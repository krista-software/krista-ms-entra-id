package app.krista.extensions.cyber_security.iam.azureAD.api;

import app.krista.extensions.cyber_security.iam.azureAD.AzureADAttributes;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.AzureADAttributesStore;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.RefreshTokenStore;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.microsoft.graph.http.GraphServiceException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.Map;
import java.util.Objects;

@Path("/")
public final class AuthenticationResource {

    private static final String CODE = "code";
    private static final String STATE = "state";
    private final AzureADAttributesStore azureADAttributesStore;
    private final RefreshTokenStore refreshTokenStore;
    private final GraphServiceClientProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;

    @Inject
    public AuthenticationResource(AzureADAttributesStore azureADAttributesStore, RefreshTokenStore refreshTokenStore, GraphServiceClientProviderFactory providerFactory, AuthorizationContext context, AuthorizationListener authorizationListener) {
        this.azureADAttributesStore = azureADAttributesStore;
        this.refreshTokenStore = refreshTokenStore;
        this.providerFactory = providerFactory;
        this.context = context;
        this.authorizationListener = authorizationListener;
    }

    @GET
    @Path("/callback")
    public String getCallBack(@QueryParam(CODE) String code, @QueryParam(STATE) String state) {
        Objects.requireNonNull(code);
        Objects.requireNonNull(state);
        String[] parts = state.split("#");
        if (parts[0].isBlank() || parts.length > 2) {
            throw new BadRequestException("Invalid state parameters!");
        }
        String key = parts[0];
        String authContextId = parts.length == 2 ? parts[1] : null;
        try {
            GraphServiceClientProvider clientProvider = getClientProvider(authContextId);
            OAuth2AccessToken accessToken = clientProvider.getAzureADAttributes().getOAuth20Service().getAccessToken(code);
            refreshTokenStore.put(key, accessToken.getRefreshToken());
            if (!key.startsWith("wsContact") && !hasUserAccess(clientProvider)) {
                refreshTokenStore.remove(key);
                return "Unauthorised user. User email '" + key.split("\\$")[0] + "' configured in the setup does not match with authenticated user.";
            }
            if (context.isAuthenticated()) {
                authorizationListener.authorized();
                return "User authenticated successfully";
            }
            return "User authenticated successfully. Save the changes.";
        } catch (OAuthException cause) {
            throw new IllegalStateException(getErrorDescription(cause), cause.getCause());
        } catch (Exception cause) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error occurred during authorization", cause.getCause());
        } finally {
            if (authContextId != null) {
                azureADAttributesStore.remove(authContextId);
            }
        }
    }

    private GraphServiceClientProvider getClientProvider(String authContextId) {
        GraphServiceClientProvider clientProvider;
        if (authContextId == null) {
            clientProvider = providerFactory.create();
        } else {
            AzureADAttributes effectiveOneDriveAttributes = azureADAttributesStore.load(authContextId);
            clientProvider = providerFactory.create(effectiveOneDriveAttributes);
        }
        return clientProvider;
    }

    private String getErrorDescription(OAuthException cause) {
        String errorDescription = "Error occurred during authorization. ";
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            Map exceptionMap = Constants.GSON.fromJson(cause.getMessage(), Map.class);
            if (exceptionMap.containsKey("error_description")) {
                errorDescription = errorDescription + exceptionMap.get("error_description");
            }
        }
        return errorDescription;
    }

    private boolean hasUserAccess(GraphServiceClientProvider clientProvider) {
        try {
            clientProvider.getUserRequestBuilder().buildRequest().get();
            return true;
        } catch (GraphServiceException ignore) {
            System.err.println("-------IGNORED EXCEPTION-----");
            ignore.printStackTrace();
            return false;
        }
    }

}
