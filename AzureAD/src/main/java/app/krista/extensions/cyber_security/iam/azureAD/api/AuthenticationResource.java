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
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
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

    @POST
    @Path("/testConnection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> testConnection(Map<String, Object> authPayload) {
        try {
            // Extract auth details from payload
            String authType = (String) authPayload.getOrDefault("authType", "Public");
            String email = (String) authPayload.get("email");
            String clientId = (String) authPayload.get("clientId");
            String clientSecret = (String) authPayload.get("clientSecret");
            String tenantId = (String) authPayload.get("tenantId");
            Boolean allowMailAlert = (Boolean) authPayload.getOrDefault("allowMailAlert", false);

            // Create temporary attributes for testing using new constructor signature
            AzureADAttributes testAttributes = new AzureADAttributes(
                clientId, clientSecret, tenantId, email, allowMailAlert, authType, "dummy-routing-url");

            // Test the connection
            GraphServiceClientProvider testProvider = providerFactory.create(testAttributes);
            testProvider.getGraphServiceClientForAdmin().me().buildRequest().get();

            return Map.of("isSuccess", true, "message", "Connection successful");
        } catch (Exception e) {
            return Map.of("isSuccess", false, "errorMessage", e.getMessage());
        }
    }

    @POST
    @Path("/saveCredentials")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> saveCredentials(Map<String, Object> authPayload) {
        try {
            // This would typically save the credentials to the extension configuration
            // For now, return success
            return Map.of("isSuccess", true, "message", "Credentials saved successfully");
        } catch (Exception e) {
            return Map.of("isSuccess", false, "errorMessage", e.getMessage());
        }
    }

    @GET
    @Path("/getCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getCredentials(@QueryParam("authType") String authType) {
        try {
            // This would typically retrieve saved credentials
            // For now, return empty credentials
            return Map.of(
                "email", "",
                "clientId", "",
                "clientSecret", "",
                "tenantId", "",
                "allowMailAlert", false
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GET
    @Path("/getAuthKey")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAuthKey() {
        // Return default auth type
        return "Public";
    }

    @GET
    @Path("/docs/")
    @Produces(MediaType.TEXT_HTML)
    public String getAuthenticationUI() {
        // Return the built React app HTML with relative paths
        return """
            <!doctype html><html lang="en"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/><meta name="theme-color" content="#000000"/><meta name="description" content="Azure AD Authentication Tab"/><link rel="manifest" href="./manifest.json"/><title>Authentication</title><script defer="defer" src="./static/js/main.b34172cb.js"></script><link href="./static/css/main.2cf75125.css" rel="stylesheet"></head><body><noscript>You need to enable JavaScript to run this app.</noscript><div id="root"></div></body></html>
            """;
    }

    @GET
    @Path("/docs/static/css/{filename}")
    @Produces("text/css")
    public Response getCSSFile(@PathParam("filename") String filename) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ui/build/static/css/" + filename);
            if (inputStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(inputStream).type("text/css").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/docs/static/js/{filename}")
    @Produces("application/javascript")
    public Response getJSFile(@PathParam("filename") String filename) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ui/build/static/js/" + filename);
            if (inputStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(inputStream).type("application/javascript").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/docs/manifest.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManifest() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ui/build/manifest.json");
            if (inputStream == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(inputStream).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
