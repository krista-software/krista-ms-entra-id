/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.api;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.MicrosoftEntraIDAttributes;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.OAuthService;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores.MicrosoftEntraIDAttributesStore;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.stores.RefreshTokenStore;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.AuthenticationResponse;
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
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants.*;
import static com.github.scribejava.core.model.OAuthConstants.CODE;
import static com.github.scribejava.core.model.OAuthConstants.STATE;

@Path("/")
public final class AuthenticationResource {

    public static final String USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST = "User Authenticated Successfully. Please proceed with the request.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    private static final String AUTHORIZATION_PROMPT = "Authorize yourself and click 'Validate Attributes' before saving changes to proceed further.";
    private static final String FAILED_TO_SAVE_ATTRIBUTES = "Failed to save attributes";

    private final MicrosoftEntraIDAttributesStore azureADAttributesStore;
    private final RefreshTokenStore refreshTokenStore;
    private final GraphServiceClientProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final String baseRoutingUrl;
    private final String invokerId;

    @Inject
    public AuthenticationResource(MicrosoftEntraIDAttributesStore azureADAttributesStore, RefreshTokenStore refreshTokenStore,
                                  GraphServiceClientProviderFactory providerFactory, Invoker invoker,
                                  AuthorizationContext context, AuthorizationListener authorizationListener) {
        this.azureADAttributesStore = azureADAttributesStore;
        this.refreshTokenStore = refreshTokenStore;
        this.providerFactory = providerFactory;
        this.context = context;
        this.authorizationListener = authorizationListener;
        this.baseRoutingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.invokerId = invoker.getInvokerId();
    }

    @GET
    @Path("/callback")
    public String getCallBack(@QueryParam(CODE) String code, @QueryParam(STATE) String state) {
        LOGGER.info("Private auth callback triggered with Code: [{}], State: [{}]", code, state);
        return getAuthenticationResponseMessage(code, state);
    }

    @GET
    @Path("/v3/oauth/callback")
    public String getCallbackForV3Auth(@QueryParam(CODE) String code, @QueryParam(STATE) String state) {
        LOGGER.info("Public auth callback triggered with Code: [{}], State: [{}]", code, state);
        return getAuthenticationResponseMessage(code, state);
    }

    @NotNull
    private String getAuthenticationResponseMessage(String code, String state) {
        LOGGER.info("Processing authentication callback with Code: [{}], State: [{}]", code, state);

        if (code == null) {
            LOGGER.error("Missing authorization code in callback. Rejecting authentication.");
            return "Authentication Failed. Please re-authorize.";
        }

        Objects.requireNonNull(state);
        String[] parts = state.split(HASH);
        if (parts[0].isBlank()) {
            LOGGER.error("State parameter is invalid or empty.");
            throw new BadRequestException("Invalid state parameters!");
        }

        String key = parts[0];
        String authContextId = getAuthContextId(state);

        LOGGER.debug("Parsed key: [{}] from state. AuthContextId resolved: [{}]", key, authContextId);

        try {
            LOGGER.info("Retrieving GraphServiceClientProvider using AuthContextId: {}", authContextId);
            GraphServiceClientProvider clientProvider = getClientProvider(authContextId);

            LOGGER.debug("Fetching Microsoft Entra ID attributes for authentication...");
            MicrosoftEntraIDAttributes azureADAttributes = clientProvider.getAzureADAttributes();

            LOGGER.debug("Initializing OAuth20Service for token exchange...");
            OAuth20Service oAuth20Service = new OAuthService(azureADAttributes).getOAuth20Service();


            LOGGER.info("Requesting access token from Microsoft Graph...");
            OAuth2AccessToken accessToken = oAuth20Service.getAccessToken(code);
            LOGGER.info("Access token retrieved successfully.");

            refreshTokenStore.put(key, accessToken.getRefreshToken());
            LOGGER.debug("Refresh token stored for key: {}", key);

            if (!key.startsWith("wsContact") && !hasUserAccess(clientProvider)) {
                LOGGER.warn("User access check failed. Authenticated user does not match setup user.");
                refreshTokenStore.remove(key);
                return "Unauthorised user. User email '" + key.split("\\$")[0] + "' configured in the setup does not match with authenticated user.";
            }

            if (context.isAuthenticated()) {
                LOGGER.info("User is authenticated. Notifying authorization listener.");
                authorizationListener.authorized();
                return USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST;
            }

            LOGGER.info("User authenticated successfully but not yet in active context.");
            return "User authenticated successfully. Save the changes.";

        } catch (OAuthException cause) {
            LOGGER.error("OAuthException occurred during token exchange: {}", cause.getMessage(), cause);
            throw new IllegalStateException(getErrorDescription(cause), cause.getCause());
        } catch (Exception cause) {
            LOGGER.error("Exception occurred during authorization: {}", cause.getMessage(), cause);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error occurred during authorization", cause.getCause());
        } finally {
            LOGGER.debug("Cleaning up azureADAttributesStore for authContextId: {}", authContextId);
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
            MicrosoftEntraIDAttributes effectiveOneDriveAttributes = azureADAttributesStore.load(authContextId);
            clientProvider = providerFactory.create(effectiveOneDriveAttributes);
        }
        return clientProvider;
    }

    private String getErrorDescription(OAuthException cause) {
        String errorDescription = ERROR_OCCURRED_DURING_AUTHORIZATION;
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> exceptionMap = (Map<String, Object>) Constants.GSON.fromJson(cause.getMessage(), Map.class);
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
            LOGGER.debug("Failed to validate user access: {}", cause.getMessage(), cause);
            return false;
        }
    }

    private String getAuthContextId(String state) {
        String[] parts = state.split(HASH);
        return parts.length > 1 ? parts[1] : null;
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

    private String createStateParameter(MustAuthorizeException cause, MicrosoftEntraIDAttributes azureADAttributes) {
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
        MicrosoftEntraIDAttributes azureADAttributes = MicrosoftEntraIDAttributes.create(authPayload, baseRoutingUrl);
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
            OAuth20Service oAuth20Service = new OAuthService(azureADAttributes).getOAuth20Service();
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
        MicrosoftEntraIDAttributes attributes = MicrosoftEntraIDAttributes.create(authPayload, baseRoutingUrl);
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
        final MicrosoftEntraIDAttributes loadedAttributes = azureADAttributesStore.load(invokerId);
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
        final MicrosoftEntraIDAttributes attributes = azureADAttributesStore.load(invokerId);
        if (attributes != null) {
            final String authType = attributes.getAuthType();
            return Constants.GSON.toJson(Objects.requireNonNullElse(authType, PUBLIC));
        } else {
            return "";
        }
    }

    @GET
    @Path("/docs/{subPath:.*}")
    public InputStream customTabs(@PathParam("subPath") String subPath) {
        String filePath = "ui/" + (subPath.isEmpty() ? "index.html" : subPath);
        return getClass().getClassLoader().getResourceAsStream(filePath);
    }
}
