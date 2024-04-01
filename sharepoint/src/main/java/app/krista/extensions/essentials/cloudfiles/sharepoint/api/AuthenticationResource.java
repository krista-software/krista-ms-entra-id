package app.krista.extensions.essentials.cloudfiles.sharepoint.api;

import app.krista.extension.executor.Invoker;
import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileChangeInfoHandler;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.Notification;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.NotificationProcessQueue;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.SharePointAttributeStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.microsoft.graph.http.GraphServiceException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import app.krista.model.base.FreeForm;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public final class AuthenticationResource {

    public static final String USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST = "User Authenticated Successfully. Please proceed with the request.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    public static final int STATUS_200 = 200;
    private final SharePointAttributeStore sharePointAttributeStore;
    private final RefreshTokenStore refreshTokenStore;
    private final GraphServiceClientProviderFactory providerFactory;
    private final EventHandler eventHandler;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final NotificationProcessQueue notificationProcessQueue;
    private final FileChangeInfoHandler fileChangeInfoHandler;
    private static final int FILE_STORE_LIMIT = 1000;
    private static final List<Map<String, Object>> fileStore = new ArrayList<>(FILE_STORE_LIMIT);


    @Inject
    public AuthenticationResource(SharePointAttributeStore sharePointAttributeStore, RefreshTokenStore refreshTokenStore,
                                  GraphServiceClientProviderFactory providerFactory, EventHandler eventHandler,
                                  AuthorizationContext context, AuthorizationListener authorizationListener,
                                  Invoker invoker, FileChangeInfoHandler fileChangeInfoHandler) {
        this.sharePointAttributeStore = sharePointAttributeStore;
        this.refreshTokenStore = refreshTokenStore;
        this.providerFactory = providerFactory;
        this.eventHandler = eventHandler;
        this.context = context;
        this.authorizationListener = authorizationListener;
        this.fileChangeInfoHandler = fileChangeInfoHandler;
        this.notificationProcessQueue = new NotificationProcessQueue(providerFactory, invoker);
    }

    @GET
    @Path("/callback")
    public String getCallBack(@QueryParam(Constants.CODE) String code, @QueryParam(Constants.STATE) String state) {
        Objects.requireNonNull(code);
        String[] parts = state.split(Constants.HASH);
        if (parts[0].isBlank() || parts.length > 2) {
            throw new BadRequestException(Constants.INVALID_STATE_PARAMETERS);
        }
        String key = parts[0];
        String authContextId = parts.length == 2 ? parts[1] : null;
        try {
            GraphServiceClientProvider clientProvider = getGraphServiceClientProvider(authContextId);
            OAuth2AccessToken accessToken = clientProvider.getSharePointAttributes().getOAuth20Service().getAccessToken(code);
            refreshTokenStore.put(key, accessToken.getRefreshToken());
            if (!key.startsWith(Constants.WS_CONTACT) && !hasUserAccess(clientProvider)) {
                refreshTokenStore.remove(key);
                return Constants.UNAUTHORISED_USER + " User email '" + key + "' configured in the setup does not match with authenticated user.";
            }
            if (context.isAuthenticated()) {
                authorizationListener.authorized();
                return USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST;
            }
            return Constants.USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES;
        } catch (OAuthException cause) {
            throw new IllegalStateException(getErrorDescription(cause), cause.getCause());
        } catch (IOException | ExecutionException cause) {
            throw new IllegalStateException(Constants.ERROR_OCCURRED_DURING_AUTHORIZATION, cause.getCause());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(Constants.ERROR_OCCURRED_DURING_AUTHORIZATION, interruptedException.getCause());
        } finally {
            if (authContextId != null) {
                sharePointAttributeStore.remove(authContextId);
            }
        }
    }

    private GraphServiceClientProvider getGraphServiceClientProvider(String authContextId) {
        GraphServiceClientProvider clientProvider;
        if (authContextId == null) {
            clientProvider = providerFactory.create();
        } else {
            SharePointAttributes effectiveSharePointAttributes = sharePointAttributeStore.load(authContextId);
            clientProvider = providerFactory.create(effectiveSharePointAttributes);
        }
        return clientProvider;
    }

    private String getErrorDescription(OAuthException cause) {
        String errorDescription = Constants.ERROR_OCCURRED_DURING_AUTHORIZATION;
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            var exceptionMap = Constants.GSON.fromJson(cause.getMessage(), Map.class);
            if (exceptionMap.containsKey(Constants.ERROR_DESCRIPTION)) {
                errorDescription = errorDescription + exceptionMap.get(Constants.ERROR_DESCRIPTION);
            }
        }
        return errorDescription;
    }

    private boolean hasUserAccess(GraphServiceClientProvider clientProvider) {
        try {
            clientProvider.getUserRequestBuilder(null, null).me().mailFolders().buildRequest().get();
            return true;
        } catch (GraphServiceException ignore) {
            LOGGER.debug(Constants.IGNORED_EXCEPTION);
            return false;
        }
    }


    /**
     * This method finds duplicate file entity from recent 1000 file entity received and add new file entity to set and Remove the oldest 100 file entities to make space for new ones.
     *
     * @param payload This is file entity sent by FileChangeInfoHandler
     * @return true if duplicate is found
     */
    private boolean isDuplicateFile(Map<String, Object> payload) {
        if (!fileStore.isEmpty() && isFileExist(payload)) {
            return true;
        }

        if (fileStore.size() >= FILE_STORE_LIMIT) {
            removeOldestEvents((FILE_STORE_LIMIT + 100) - fileStore.size());
        }

        fileStore.add(payload);
        return false;
    }

    private boolean isFileExist(Map<String, Object> payload) {
        for(Map<String, Object> file : fileStore){
            app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileStoreEntity = (app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File)file.get(Constants.FILE);
            String fileStoreEvent = file.get(Constants.EVENT).toString();
            app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = (app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File)payload.get(Constants.FILE);
            String fileEvent = payload.get(Constants.EVENT).toString();
            if(fileEntity.absolutePath.equals(fileStoreEntity.absolutePath) && fileEvent.equals(fileStoreEvent) && fileEntity.modified.equals(fileStoreEntity.modified)){
                return true;
            }
        }
        return false;
    }

    private void removeOldestEvents(int count) {
        fileStore.subList(0, count).clear();
    }

    @POST
    @Path("/sharePointNotification")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response subscriptionValidation(@QueryParam(Constants.VALIDATION_TOKEN) String validationToken) {
        return Response.status(200).type(MediaType.TEXT_PLAIN).entity(validationToken.trim()).build();
    }

    @POST
    @Path("/sharePointNotification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response subscriptionNotification(@NotNull JsonObject notification) {
        JsonArray array = notification.get(Constants.VALUE).getAsJsonArray();
        if(!array.isEmpty()) {
            String subscriptionId = array.get(0).getAsJsonObject().get("subscriptionId").getAsString();
            List<Map<String, Object>> payloadList = fileChangeInfoHandler.getChangeFileEvent(subscriptionId);
            for(Map<String, Object> payload : payloadList) {
                if (payload.containsKey(Constants.MESSAGE) && payload.get(Constants.MESSAGE).equals(Constants.EMPTY)) {
                    FreeForm freeForm = new FreeForm();
                    freeForm.put(Constants.SHAREPOINT_FILE_ID, Constants.SWITCH, false);
                    eventHandler.handleEvent(Constants.FILE_RECEIVED, freeForm);
                } else {
                    if (isDuplicateFile(payload)) {
                        return Response.status(STATUS_200).build();
                    }
                    FreeForm freeForm = new FreeForm();
                    ResponseUtil.addEventToFreeForm(payload, freeForm);
                    freeForm.put(Constants.SHAREPOINT_FILE_ID, Constants.SWITCH, true);
                    eventHandler.handleEvent(Constants.FILE_RECEIVED, freeForm);
                }
            }
        }
        return Response.status(STATUS_200).build();
    }

    @POST
    @Path("/lifecycleNotification")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response lifecycleValidation(@QueryParam(Constants.VALIDATION_TOKEN) String validationToken) {
        return Response.status(200).type(MediaType.TEXT_PLAIN).entity(validationToken.trim()).build();
    }

    @POST
    @Path("/lifecycleNotification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response lifecycleNotification(JsonObject notification) {
        this.notificationProcessQueue.add(new Notification(Notification.NotificationType.LIFECYCLE, notification));
        return Response.status(202).build();
    }

}
