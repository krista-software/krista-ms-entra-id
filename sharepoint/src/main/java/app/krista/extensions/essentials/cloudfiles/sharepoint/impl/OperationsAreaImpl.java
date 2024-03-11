package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileCreateService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsAreaListImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsAreaSiteImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.RestClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.ksdk.context.RequestContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.http.GraphServiceException;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;


/**
 * Class for creating,deleting and updating SharePoint lists & Site using Microsoft Graph API.
 *
 * @author Vaibhav Choudhary
 */
@Service
public class OperationsAreaImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsAreaImpl.class);
    private final GraphServiceClientProvider clientProvider;
    private final OperationsAreaListImpl operationsAreaListImpl;
    private final OperationsAreaSiteImpl operationsAreaSiteImpl;
    private final SharePointAttributes sharePointAttributes;

    private final RequestContext requestContext;


    @Inject
    public OperationsAreaImpl(GraphServiceClientProviderFactory clientProviderFactory,
                              OperationsAreaListImpl operationsAreaListImpl,
                              OperationsAreaSiteImpl operationsAreaSiteImpl, SharePointAttributes sharePointAttributes, RequestContext requestContext) {
        this.clientProvider = clientProviderFactory.create();
        this.operationsAreaListImpl = operationsAreaListImpl;
        this.operationsAreaSiteImpl = operationsAreaSiteImpl;
        this.sharePointAttributes = sharePointAttributes;
        this.requestContext = requestContext;
    }


    public Map<String, Object> createList(String siteName, String listName) {
        try {
            return operationsAreaListImpl.createList(siteName, listName);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> deleteList(String siteName, String listName) {
        try {
            return operationsAreaListImpl.deleteList(siteName, listName);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> updateList(String siteName, String listName, String displayName, String description) {
        try {
            return operationsAreaListImpl.updateList(siteName, listName, displayName, description);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> updateGroup(String groupName, String newGroupName) {
        try {
            return operationsAreaSiteImpl.updateGroup(groupName, newGroupName);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        } catch (Exception cause) {
            throw new RuntimeException(cause);
        }
    }

    public Map<String, Object> deleteGroup(String groupName) {
        try {
            return operationsAreaSiteImpl.deleteGroup(groupName);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    /**
     * Creates a new group using the {@link FileCreateService}.
     * This method retrieves the Graph client and calls the `FileCreateService`'s `createGroup` method
     * to handle the group creation logic with dedicated functionality.
     *
     * @param groupName,@param mailNickName A map containing information necessary for group creation, such as name and base name.
     * @return A GroupImpl object representing the newly created group, or null if creation fails.
     * @throws GraphServiceException If an error occurs during group creation.
     */

    public Map<String, Object> createGroup(String groupName, String mailNickName) {
        String ownerMail = null;

        try {
            RestClient restClient = clientProvider.getRestClient(null);

            if (requestContext.invokeAsUser()) {
                ownerMail = getOwnerMail(restClient);
            } else {
                ownerMail = sharePointAttributes.getMailId();
            }
            Response response = restClient.
                    executeHttpMethodsForGroup(Constants.GROUP_URL, createGroupPayload(groupName, mailNickName, ownerMail));

            return prepareResponse(response);
        } catch (Exception cause) {
            LOGGER.error("Failed to create group - {}", cause.getMessage());
            return ResponseUtil.preparePayloadResponse(null, false, "Failed to create group - " + cause.getMessage());
        }
    }

    private Map<String, Object> prepareResponse(Response response) {

        String responseBody = response.readEntity(String.class);
        if (responseBody != null) {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonObject.has(Constants.id) &&                // Success 201 created Group
                    jsonObject.has(Constants.createdDateTime) &&
                    jsonObject.has(Constants.displayName) &&
                    jsonObject.has(Constants.mail)) {

                File file = new File();
                if (jsonObject.has(Constants.displayName) && jsonObject.has(Constants.mailNickname)) {
                    file.name = jsonObject.get(Constants.displayName).getAsString();
                    file.baseName = jsonObject.get(Constants.mailNickname).getAsString();
                }
                return ResponseUtil.preparePayloadResponse(file, true, Constants.GROUP_CREATED);
            } else if (jsonObject.has(Constants.error)) {
                Map<String, Object> groupMessageForConflict = getErrorResponse(jsonObject);
                if (groupMessageForConflict != null) return groupMessageForConflict;
            } else {
                throw new IllegalArgumentException("Failed to create group");
            }
        }
        return Collections.emptyMap();
    }

    @Nullable
    private Map<String, Object> getErrorResponse(JsonObject jsonObject) {
        JsonObject errorJsonBody = jsonObject.getAsJsonObject(Constants.error);
        if (errorJsonBody.get(Constants.CODE).getAsString().equals(Constants.Request_BadRequest) &&
                errorJsonBody.get(Constants.message).getAsString().equals(Constants.groupConflictMessage)) {
            return ResponseUtil.preparePayloadResponse(null, false, Constants.groupMessageForConflict);
        }
        if (errorJsonBody.get(Constants.CODE).getAsString().equals(Constants.Request_BadRequest)
                && errorJsonBody.get(Constants.message).getAsString().equals(Constants.InvalidMailNickName)) {
            return ResponseUtil.preparePayloadResponse(null, false, Constants.ValidMailNickName);
        }
        return null;
    }


    private String createGroupPayload(String groupName, String mailNickName, String ownerMail) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty(Constants.displayName, groupName);
        jsonBody.addProperty(Constants.mailEnabled, true);
        jsonBody.addProperty(Constants.mailNickname, mailNickName);
        jsonBody.addProperty(Constants.securityEnabled, false);

        JsonArray groupTypes = new JsonArray();
        groupTypes.add(Constants.Unified);
        jsonBody.add(Constants.groupTypes, groupTypes);

        JsonArray owners = new JsonArray();
        owners.add(Constants.groupBaseUrl + ownerMail);
        jsonBody.add(Constants.ownersDataBind, owners);
        return new Gson().toJson(jsonBody);
    }

    private String getOwnerMail(RestClient restClient) throws IOException {
        String ownerMail = null;
        okhttp3.Response response = restClient.getWithHeader(Constants.ownerMailUrl);
        String responseBody = response.body().string();
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        if (jsonObject != null && jsonObject.has(Constants.mail)) {
            ownerMail = jsonObject.get(Constants.mail).getAsString();
        }
        return ownerMail;
    }

}
