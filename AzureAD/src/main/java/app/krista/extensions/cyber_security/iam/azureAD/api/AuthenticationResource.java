package app.krista.extensions.cyber_security.iam.azureAD.api;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.cyber_security.iam.azureAD.AzureADAttributes;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.AzureADAttributesStore;
import app.krista.extensions.cyber_security.iam.azureAD.impl.stores.RefreshTokenStore;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.AuthenticationResponse;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.model.field.NamedValuedField;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.JsonObject;
import com.microsoft.graph.http.GraphServiceException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants.*;
import static com.github.scribejava.core.model.OAuthConstants.CODE;
import static com.github.scribejava.core.model.OAuthConstants.STATE;

@Path("/")
public final class AuthenticationResource {

    public static final String USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST = "User Authenticated Successfully. Please proceed with the request.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    private static final String AUTHORIZATION_PROMPT = "Authorize yourself and click 'Validate Attributes' before saving changes to proceed further.";
    private static final String FAILED_TO_SAVE_ATTRIBUTES = "Failed to save attributes";

    private final AzureADAttributesStore azureADAttributesStore;
    private final RefreshTokenStore refreshTokenStore;
    private final GraphServiceClientProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final String baseRoutingUrl;
    private final String invokerId;
    private final Invoker invoker;

    @Inject
    public AuthenticationResource(AzureADAttributesStore azureADAttributesStore, RefreshTokenStore refreshTokenStore,
                                  GraphServiceClientProviderFactory providerFactory, Invoker invoker,
                                  AuthorizationContext context, AuthorizationListener authorizationListener) {
        this.azureADAttributesStore = azureADAttributesStore;
        this.refreshTokenStore = refreshTokenStore;
        this.providerFactory = providerFactory;
        this.context = context;
        this.authorizationListener = authorizationListener;
        this.baseRoutingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.invokerId = invoker.getInvokerId();
        this.invoker = invoker;
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
        String errorDescription = ERROR_OCCURRED_DURING_AUTHORIZATION;
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            Map<String, Object> exceptionMap = Constants.GSON.fromJson(cause.getMessage(), Map.class);
            if (exceptionMap.containsKey(ERROR_DESCRIPTION)) {
                errorDescription = errorDescription + exceptionMap.get(ERROR_DESCRIPTION);
            }
        }
        return errorDescription;
    }

    private boolean hasUserAccess(GraphServiceClientProvider clientProvider) {
        try {
            clientProvider.getUserRequestBuilder().buildRequest().get();
            return true;
        } catch (GraphServiceException cause) {
            LOGGER.debug(cause.getMessage(), cause);
            return false;
        }
    }

    private String getAuthContextId(String state) {
        String[] parts = state.split(HASH);
        return parts.length > 1 ? parts[1] : null;
    }

    private GraphServiceClientProvider getGraphServiceClientProvider(String authContextId) {
        return authContextId == null
                ? providerFactory.create()
                : providerFactory.create(authContextId);
    }

    private String createTestConnectionResponse(boolean isSuccess, String errorMessage, String url) {
        if (isSuccess) {
            return Constants.GSON.toJson(new AuthenticationResponse(true, null, null));
        } else if (url != null) {
            return Constants.GSON.toJson(new AuthenticationResponse(false, errorMessage, url));
        } else
            return Constants.GSON.toJson(new AuthenticationResponse(false,
                    Objects.requireNonNullElse(errorMessage, "Unknown Error"), null));
    }

    private String createStateParameter(MustAuthorizeException cause, AzureADAttributes azureADAttributes) {
        String userId = (String) cause.getDetails().get(0).getValue();
        Optional<NamedValuedField> authContextIdField = cause.getDetails().stream()
                .filter(namedValuedField -> Objects.equals(namedValuedField.getName(), AUTH_CONTEXT_ID))
                .findFirst();
        String state = userId;
        if (authContextIdField.isPresent()) {
            String authContextId = (String) authContextIdField.get().getValue();
            state += HASH + authContextId;
        }
        if (PUBLIC.equals(azureADAttributes.getAuthType())) {
            state += HASH + azureADAttributes.getForwardPath();
        }
        return state;
    }

    @POST
    @Path("/testConnection")
    @Produces("text/plain")
    public String testConnection(JsonObject authPayload) {
        LOGGER.info("Testing connection with Microsoft Graph API");
        AzureADAttributes azureADAttributes = AzureADAttributes.create(authPayload, baseRoutingUrl);
        String authContextId = providerFactory.createAttributes(azureADAttributes);
        String authUrl = null;
        try {
            LOGGER.info("Verifying API access for email: {}", azureADAttributes.getEmail());
            providerFactory.create(authContextId).getGraphServiceClientForAdmin().me().buildRequest().get();

            LOGGER.info("Test connection successful");
            return createTestConnectionResponse(true, null, null);
        } catch (GraphServiceException cause) {
            LOGGER.error("Graph API connection failed: {}", cause.getMessage());
            return createTestConnectionResponse(false, "An error occurred during test connection.", null);
        } catch (MustAuthorizeException cause) {
            LOGGER.info("Authorization required, generating auth URL");
            String state = createStateParameter(cause, azureADAttributes);
            OAuth20Service oAuth20Service = azureADAttributes.getOAuth20Service();
            authUrl = oAuth20Service.getAuthorizationUrl(state) + AUTH_URL_QUERY_PARAMS;
            return createTestConnectionResponse(false, AUTHORIZATION_PROMPT, authUrl);
        } finally {
            if (authUrl == null) {
                azureADAttributesStore.remove(authContextId);
            }
        }
    }

    @POST
    @Path("/saveCredentials")
    @Produces("text/plain")
    public String saveCredentials(JsonObject authPayload) {
        AzureADAttributes attributes = AzureADAttributes.create(authPayload, baseRoutingUrl);
        String authContextId = providerFactory.createAttributes(attributes);
        try {
            providerFactory.create(authContextId).getGraphServiceClientForAdmin()
                    .users(attributes.getEmail())
                    .buildRequest().get();
            boolean isSaved = azureADAttributesStore.save(attributes, invokerId);
            return isSaved
                    ? Constants.GSON.toJson(new AuthenticationResponse(true, null, null))
                    : Constants.GSON.toJson(new AuthenticationResponse(false, FAILED_TO_SAVE_ATTRIBUTES, null));
        } catch (Exception cause) {
            LOGGER.debug(FAILED_TO_SAVE_ATTRIBUTES);
            return Constants.GSON.toJson(new AuthenticationResponse(false, FAILED_TO_SAVE_ATTRIBUTES, null));
        } finally {
            azureADAttributesStore.remove(authContextId);
        }
    }

    @GET
    @Path("/getCredentials")
    @Produces("text/plain")
    public String getCredentials(@QueryParam(AUTH_TYPE) String authType) {
        LOGGER.info("Loading attributes for auth-type: {} for invoker {}", authType, invokerId);
        final AzureADAttributes loadedAttributes = azureADAttributesStore.load(invokerId);
        if (loadedAttributes != null && authType.equals(loadedAttributes.getAuthType())) {
            return Constants.GSON.toJson(loadedAttributes.toMap());
        } else {
            return "";
        }
    }

    @GET
    @Path("/getAuthKey")
    @Produces("text/plain")
    public String getAuthKey() {
        LOGGER.info("Loading stored authentication key.");
        final AzureADAttributes attributes = azureADAttributesStore.load(invokerId);
        if (attributes != null) {
            final String authType = attributes.getAuthType();
            return Constants.GSON.toJson(Objects.requireNonNullElse(authType, PUBLIC));
        } else {
            return "";
        }
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
