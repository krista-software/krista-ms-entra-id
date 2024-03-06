package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileCreateService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsAreaListImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsAreaSiteImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.http.GraphServiceException;
import org.codehaus.jettison.json.JSONException;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
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


    @Inject
    public OperationsAreaImpl(GraphServiceClientProviderFactory clientProviderFactory,
                              OperationsAreaListImpl operationsAreaListImpl,
                              OperationsAreaSiteImpl operationsAreaSiteImpl, SharePointAttributes sharePointAttributes) {
        this.clientProvider = clientProviderFactory.create();
        this.operationsAreaListImpl = operationsAreaListImpl;
        this.operationsAreaSiteImpl = operationsAreaSiteImpl;
        this.sharePointAttributes = sharePointAttributes;
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

    public Map<String, Object> updateSite(String siteName, String newSiteName) {
        try {
            return operationsAreaSiteImpl.updateSite(siteName, newSiteName);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> deleteSite(String siteName) {
        try {
            return operationsAreaSiteImpl.deleteSite(siteName);
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
        Map<String, Object> payload = new HashMap<>();
        String ownerMail = sharePointAttributes.getMailId();

        try {
            Response response = clientProvider.getRestClient(null).
                    executeHttpMethodsForGroup(Constants.GROUP_URL, HttpMethod.POST, createPayload(groupName, mailNickName, ownerMail));

            prepareResponse(response, payload);
        } catch (IOException cause) {
            LOGGER.error("Failed to create group - " + cause.getMessage());
        } catch (JSONException cause) {
            throw new RuntimeException(cause);
        }
        return payload;
    }

    private void prepareResponse(Response response, Map<String, Object> payload) throws JSONException, JsonProcessingException {
        File file = new File();
        try (response) {
            String responseBody = response.readEntity(String.class);
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonObject.has("id") &&                // Success 201 created
                    jsonObject.has("createdDateTime") &&
                    jsonObject.has("displayName") &&
                    jsonObject.has("mail")) {

                if (jsonObject.has("displayName") && jsonObject.has("mailNickname")) {
                    file.name = jsonObject.get("displayName").getAsString();
                    file.baseName = jsonObject.get("mailNickname").getAsString();
                }
                ResponseUtil.preparePayloadResponse(file, true, Constants.GROUP_CREATED);
            } else if (jsonObject.getAsJsonObject("error") != null) {       // 400 Conflict group name
                JsonObject errorJsonBody = jsonObject.getAsJsonObject("error");
                if (errorJsonBody.get(Constants.CODE).getAsString().equals("Request_BadRequest") && errorJsonBody.get("message").getAsString().equals(Constants.groupConflictMessage)) {
                    ResponseUtil.preparePayloadResponse(null, false, Constants.groupMessageForConflict);
                }
                if (errorJsonBody.get(Constants.CODE).getAsString().equals("Request_BadRequest") && errorJsonBody.get("message").getAsString().equals(Constants.InvalidMailNickName)) {
                    ResponseUtil.preparePayloadResponse(null, false, Constants.ValidMailNickName);
                }
            }
        }
    }


    private String createPayload(String groupName, String mailNickName, String ownerMail) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("displayName", groupName);
        jsonBody.addProperty("mailEnabled", true);
        jsonBody.addProperty("mailNickname", mailNickName);
        jsonBody.addProperty("securityEnabled", false);

        JsonArray groupTypes = new JsonArray();
        groupTypes.add("Unified");
        jsonBody.add("groupTypes", groupTypes);

        JsonArray owners = new JsonArray();
        owners.add("https://graph.microsoft.com/v1.0/users/" + ownerMail);
        jsonBody.add("owners@odata.bind", owners);
        return new Gson().toJson(jsonBody);
    }


}
