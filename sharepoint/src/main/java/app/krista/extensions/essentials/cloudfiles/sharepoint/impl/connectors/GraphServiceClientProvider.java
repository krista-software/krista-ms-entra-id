package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.*;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GraphServiceClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphServiceClientProvider.class);
    private final RefreshTokenStore refreshTokenStore;
    private final SharePointAttributes sharePointAttributes;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final String authContextId;

    @Inject
    public GraphServiceClientProvider(RefreshTokenStore refreshTokenStore, SharePointAttributes sharePointAttributes,
                                      RequestContext requestContext, AuthorizationContext authorizationContext) {
        this(refreshTokenStore, sharePointAttributes, requestContext, authorizationContext, null);
    }

    public GraphServiceClientProvider(RefreshTokenStore refreshTokenStore, SharePointAttributes sharePointAttributes,
                                      RequestContext requestContext, AuthorizationContext authorizationContext, String authContextId) {
        this.refreshTokenStore = refreshTokenStore;
        this.sharePointAttributes = sharePointAttributes;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.authContextId = authContextId;
    }

    public SharePointAttributes getSharePointAttributes() {
        return sharePointAttributes;
    }

    public RestClient getRestClient(String driveId) {
        return getRestClient(driveId, !requestContext.invokeAsUser());
    }

    public GraphServiceClient<Request> getGraphServiceClientForUser(Boolean useEmail, String accountID) throws IOException {
        if (useEmail != null) {
            return getGraphServiceClient(useEmail, useEmail ? null : accountID);
        } else {
            return getGraphServiceClient(!requestContext.invokeAsUser(), null);
        }
    }

    public GraphServiceClient<Request> getGraphServiceClientForAdmin() {
        return getGraphServiceClient(true, null);
    }

    private GraphServiceClient<Request> getGraphServiceClient(boolean useEmail, String accountID) {
        try {
            String userId = getUserId(useEmail, accountID);
            String refreshToken = refreshTokenStore.get(userId);
            if (refreshToken == null) {
                throw createMustAuthorizationException(userId, false);
            }
            return getGraphServiceClient(userId, refreshToken);
        } catch (RuntimeException cause) {
            throw cause;
        }
    }

    /**
     * Creates a new GraphServiceClient instance using the provided user ID and refresh token.
     * This method is used internally to obtain a GraphServiceClient object for authorized access to SharePoint
     * resources on behalf of the specified user.
     *
     * @param userId       The unique identifier of the user for whom the GraphServiceClient should be created.
     * @param refreshToken The refresh token associated with the user's account.
     * @return A GraphServiceClient object authorized to access SharePoint resources on behalf of the specified user.
     * @throws MustAuthorizeException If authorization fails due to invalid refresh token or other issues.
     */
    private GraphServiceClient<Request> getGraphServiceClient(String userId, String refreshToken) {
        try {
            String[] scopes = Constants.REQUIRED_SCOPE.split(Constants.SCOPE_SEPARATOR);
            Set<String> scopeSet = Arrays.stream(scopes).collect(Collectors.toSet());
            RefreshTokenParameters refreshTokenParameters = RefreshTokenParameters.builder(scopeSet, refreshToken).build();
            IClientCredential clientCredential = ClientCredentialFactory.createFromSecret(sharePointAttributes.getClientSecret());
            String authority = Constants.AUTHORITY + sharePointAttributes.getTenantId();
            ConfidentialClientApplication confidentialClientApplication = ConfidentialClientApplication.builder
                    (sharePointAttributes.getClientId(), clientCredential).authority(authority).build();
            IAuthenticationResult authenticationResult = confidentialClientApplication.acquireToken(refreshTokenParameters).get();
            final String cachedTokenContent = confidentialClientApplication.tokenCache().serialize();
            updateRefreshToken(userId, Constants.GSON.fromJson(cachedTokenContent, JsonObject.class));
            return GraphServiceClient.builder()
                    .authenticationProvider(new GraphServiceClientAuthenticationProvider(authenticationResult.accessToken()))
                    .buildClient();
        } catch (ClientException | ExecutionException | MalformedURLException cause) {
            throw createMustAuthorizationException(userId, true);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw createMustAuthorizationException(userId, true);
        }
    }

    private RestClient getRestClient(String driveId, boolean useEmail) {
        try {
            String userId = getUserId(useEmail, null);
            String refreshToken = refreshTokenStore.get(userId);
            if (refreshToken == null) {
                throw createMustAuthorizationException(userId, false);
            }
            return getRestClient(driveId, userId, refreshToken);
        } catch (RuntimeException cause) {
            System.err.println("Failed to get graph client " + cause.getClass().getName());
            throw cause;
        }
    }

    private RestClient getRestClient(String driveId, String userId, String refreshToken) {
        try {
            String[] scopes = Constants.REQUIRED_SCOPE.split(Constants.SCOPE_SEPARATOR);
            Set<String> scopeSet = Arrays.stream(scopes).collect(Collectors.toSet());
            RefreshTokenParameters refreshTokenParameters = RefreshTokenParameters.builder(scopeSet, refreshToken).build();
            IClientCredential clientCredential = ClientCredentialFactory.createFromSecret(sharePointAttributes.getClientSecret());
            String authority = Constants.AUTHORITY + sharePointAttributes.getTenantId();
            ConfidentialClientApplication confidentialClientApplication = ConfidentialClientApplication.builder
                    (sharePointAttributes.getClientId(), clientCredential).authority(authority).build();
            IAuthenticationResult authenticationResult = confidentialClientApplication.acquireToken(refreshTokenParameters).get();
            return new RestClient(authenticationResult.accessToken(), driveId);
        } catch (IOException | ExecutionException | InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw createMustAuthorizationException(userId, true);
        }
    }

    private void updateRefreshToken(String userId, JsonObject jsonObject) {
        JsonObject refreshToken = jsonObject.get("RefreshToken").getAsJsonObject();
        String firstKey = refreshToken.keySet().iterator().next();
        JsonObject tokenData = refreshToken.getAsJsonObject(firstKey);
        String secret = tokenData.get("secret").getAsString();
        refreshTokenStore.put(userId, secret);
        System.out.println("Updated Refresh Token.");
    }

    /**
     * Builds a GraphServiceClient object for performing operations on behalf of the current user or a specific user.
     * <p>
     * This method provides multiple options for obtaining a GraphServiceClient:
     * - If `useEmail` is true and `accountID` is provided, the method retrieves the user by email and creates a client for them.
     * - If `useEmail` is true and `accountID` is not provided, the method uses the currently logged-in user's email to create a client.
     * - If `useEmail` is null, the method checks `RequestContext.invokeAsUser`:
     * - If `invokeAsUser` is true, it creates a client for the currently logged-in user.
     * - If `invokeAsUser` is false, it throws an IllegalStateException because a user context is required.
     *
     * @param useEmail  Whether to use the provided email (if true) or the currently logged-in user (if null).
     * @param accountID The email address of the specific user (relevant only if `useEmail` is true).
     * @return A GraphServiceClient object authorized to access SharePoint resources on behalf of the specified user.
     * @throws IOException           If any I/O error occurs while retrieving user information.
     * @throws IllegalStateException If a user context is required but not available.
     */
    public GraphServiceClient<Request> getUserRequestBuilder(Boolean useEmail, String accountID) {
        try {
            if (useEmail != null) {
                return getGraphServiceClientForUser(useEmail, accountID);
            }
            return getGraphServiceClientForUser(null, null);
        } catch (IOException cause) {
            throw new IllegalStateException(cause);
        }
    }


    private MustAuthorizeException createMustAuthorizationException(String userId, boolean reAuthentication) {
        LOGGER.debug(reAuthentication ? Constants.GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_RE_AUTHENTICATION :
                Constants.GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_AUTHENTICATION);
        List<NamedValuedField> details = new ArrayList<>();
        NamedValuedField userIdField = new NamedValuedField(Constants.USER_ID, Constants.TEXT, userId, new HashMap<>(), new HashMap<>());
        details.add(userIdField);
        if (authContextId != null) {
            NamedValuedField contextIdField = new NamedValuedField(Constants.AUTH_CONTEXT_ID, Constants.TEXT,
                    authContextId, new HashMap<>(), new HashMap<>());
            details.add(contextIdField);
        }
        return new MustAuthorizeException(reAuthentication ?
                Constants.REFRESH_TOKEN_IS_NO_LONGER_VALID_PLEASE_REAUTHORIZE_YOURSELF :
                Constants.YOU_ARE_NOT_AUTHORIZED_PLEASE_AUTHORIZE_YOURSELF, details);
    }

    private String getUserId(boolean calledFromValidateAttributes, String accountID) {
        String userId;
        if (calledFromValidateAttributes) {
            userId = sharePointAttributes.getMailId();
        } else {
            userId = (accountID != null) ? accountID : authorizationContext.getAuthorizedAccount().getAccountId();
        }
        if (userId == null) {
            throw new IllegalStateException(Constants.FAILED_TO_GET_ACCOUNT);
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
