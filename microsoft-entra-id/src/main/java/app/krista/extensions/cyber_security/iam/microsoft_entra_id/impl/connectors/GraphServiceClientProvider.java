package app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.MicrosoftEntraIDAttributes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores.RefreshTokenStore;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.*;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants.PRIVATE;
import static app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants.PUBLIC;

public class GraphServiceClientProvider {
    public static final String REQUIRED_SCOPE = Constants.REQUIRED_SCOPE;
    public static final String SCOPE_SEPARATOR = " ";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GraphServiceClientProvider.class);
    private static final Logger LOG = LogManager.getLogger(GraphServiceClientProvider.class.getName());
    private final RefreshTokenStore refreshTokenStore;
    private final MicrosoftEntraIDAttributes azureADAttributes;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final String authContextId;


    @Inject
    public GraphServiceClientProvider(RefreshTokenStore refreshTokenStore, MicrosoftEntraIDAttributes azureADAttributes, RequestContext requestContext, AuthorizationContext authorizationContext) {
        this(refreshTokenStore, azureADAttributes, requestContext, authorizationContext, null);
    }

    public GraphServiceClientProvider(RefreshTokenStore refreshTokenStore, MicrosoftEntraIDAttributes azureADAttributes, RequestContext requestContext, AuthorizationContext authorizationContext, String authContextId) {
        this.refreshTokenStore = refreshTokenStore;
        this.azureADAttributes = azureADAttributes;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.authContextId = authContextId;
    }

    public MicrosoftEntraIDAttributes getAzureADAttributes() {
        return azureADAttributes;
    }

    public GraphServiceClient<Request> getGraphServiceClient() {
        return getGraphServiceClient(!requestContext.invokeAsUser(), false);
    }

    public GraphServiceClient<Request> getGraphServiceClientForAdmin() {
        return getGraphServiceClient(true, true);
    }

    private GraphServiceClient<Request> getGraphServiceClient(boolean useEmail, boolean handleAuthError) {
        try {
            String userId = getUserId(useEmail);
            String refreshTokenStoreKey = getRefTokenStoreKey(userId);
            String refreshToken = refreshTokenStore.get(refreshTokenStoreKey);
            if (refreshToken == null) {
                throw createMustAuthorizationException(refreshTokenStoreKey, false);
            }
            return getGraphServiceClient(refreshTokenStoreKey, refreshToken, handleAuthError);
        } catch (RuntimeException cause) {
            System.err.println("Failed to get graph client " + cause.getClass().getName());
            cause.printStackTrace();
            throw cause;
        }
    }

    private GraphServiceClient<Request> getGraphServiceClient(String refreshTokenStoreKey, String refreshToken, boolean handleAuthError) {
        try {
            String[] scopes = REQUIRED_SCOPE.split(SCOPE_SEPARATOR);
            Set<String> scopeSet = Arrays.stream(scopes).collect(Collectors.toSet());
            RefreshTokenParameters refreshTokenParameters = RefreshTokenParameters.builder(scopeSet, refreshToken).build();
            IClientCredential clientCredential = ClientCredentialFactory.createFromSecret(azureADAttributes.getClientSecret());

            String authority;
            if (PRIVATE.equals(azureADAttributes.getAuthType())) {
                authority = Constants.AUTHORITY + azureADAttributes.getTenantId();
            } else if (PUBLIC.equals(azureADAttributes.getAuthType())) {
                LOGGER.info("Using public authentication type");
                authority = Constants.ORG_AUTHORITY;
            } else {
                throw new IllegalArgumentException("No authentication type found.");
            }
            LOGGER.debug("Building client with authority: {}", authority);

            ConfidentialClientApplication confidentialClientApplication = ConfidentialClientApplication.builder
                    (azureADAttributes.getClientId(), clientCredential).authority(authority).build();
            IAuthenticationResult authenticationResult = confidentialClientApplication.acquireToken(refreshTokenParameters).get();
            final String cachedTokenContent = confidentialClientApplication.tokenCache().serialize();
            updateRefreshToken(refreshTokenStoreKey, Constants.GSON.fromJson(cachedTokenContent, JsonObject.class));

            return GraphServiceClient.builder()
                    .authenticationProvider(new GraphServiceClientAuthenticationProvider(authenticationResult.accessToken()))
                    .buildClient();
        } catch (ClientException | ExecutionException | IOException cause) {
            if (handleAuthError) {
                throw createMustAuthorizationException(refreshTokenStoreKey, true);
            } else {
                throw new IllegalArgumentException(cause.getMessage());
            }
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            if (handleAuthError) {
                throw createMustAuthorizationException(refreshTokenStoreKey, true);
            } else {
                throw new IllegalArgumentException(cause.getMessage());
            }
        }
    }

    private void updateRefreshToken(String userId, JsonObject jsonTokenCache) {
        JsonObject refreshToken = jsonTokenCache.get("RefreshToken").getAsJsonObject();
        String firstKey = refreshToken.keySet().iterator().next();
        JsonObject tokenData = refreshToken.getAsJsonObject(firstKey);
        String secret = tokenData.get("secret").getAsString();
        refreshTokenStore.put(userId, secret);
        LOG.info("Refresh token updated");
    }

    public UserRequestBuilder getUserRequestBuilder() {
        return requestContext.invokeAsUser()
                ? getGraphServiceClient().me()
                : getGraphServiceClient().users(azureADAttributes.getEmail());
    }
    @NotNull
    private String getRefTokenStoreKey(String userId) {
        if (userId.startsWith(Constants.WS_CONTACT)) {
            return userId;
        } else {
            return userId + Constants.UNDER_SCORE + azureADAttributes.getClientId() + Constants.UNDER_SCORE + azureADAttributes.getAuthType();
        }
    }

    private MustAuthorizeException createMustAuthorizationException(String userId, boolean reAuthentication) {
        System.err.println(reAuthentication ? "Got error for Authentication. So sending for re authentication" : "Got error for Authentication. So sending for authentication");
        List<NamedValuedField> details = new ArrayList<>();
        NamedValuedField userIdField = new NamedValuedField("userId", "Text", userId, new HashMap<>(), new HashMap<>());
        details.add(userIdField);
        if (authContextId != null) {
            NamedValuedField contextIdField = new NamedValuedField("authContextId", "Text", authContextId, new HashMap<>(), new HashMap<>());
            details.add(contextIdField);
        }
        return new MustAuthorizeException(reAuthentication ? "Refresh Token Expired. Please reauthorize yourself" : "Authorize yourself and click 'Validate Attributes' before saving changes to proceed further.", details);
    }

    private String getUserId(boolean calledFromValidateAttributes) {
        String userId = calledFromValidateAttributes || !requestContext.invokeAsUser()
                ? azureADAttributes.getEmail()
                : authorizationContext.getAuthorizedAccount().getAccountId();
        if (userId == null) {
            throw new IllegalStateException("Failed to get account");
        }
        return userId;
    }

    public static class GraphServiceClientAuthenticationProvider implements IAuthenticationProvider {

        private final String accessToken;

        public GraphServiceClientAuthenticationProvider(String accessToken) {
            if (accessToken == null || accessToken.isEmpty() || accessToken.isBlank()) {
                throw new IllegalArgumentException("Empty access token provided " + accessToken);
            }
            this.accessToken = accessToken;
        }

        @NotNull
        @Override
        public CompletableFuture<String> getAuthorizationTokenAsync(@NotNull URL requestUrl) {
            return CompletableFuture.completedFuture(accessToken);
        }
    }

}
