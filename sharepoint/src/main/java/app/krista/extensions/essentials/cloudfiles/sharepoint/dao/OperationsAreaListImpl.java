package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.OperationsAreaUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.ColumnDefinitionCollectionPage;
import com.microsoft.graph.requests.ColumnDefinitionCollectionResponse;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class provides operations for managing SharePoint lists, including creating, deleting, and updating lists.
 * It utilizes Microsoft Graph API for communication with SharePoint.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class OperationsAreaListImpl {

    private final GraphServiceClientProvider clientProvider;
    private final OperationsAreaUtilityService operationsAreaUtilityService;
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsAreaListImpl.class);

    @Inject
    public OperationsAreaListImpl(GraphServiceClientProviderFactory clientProviderFactory,
                                  OperationsAreaUtilityService operationsAreaUtilityService) {
        this.clientProvider = clientProviderFactory.create();
        this.operationsAreaUtilityService = operationsAreaUtilityService;
    }

    /**
     * Creates a new list with default columns within a specified SharePoint site.
     */
    public Map<String, Object> createList(String siteName, String listName) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndListNames(siteName, listName, payload)) {
            return payload; // Early return if validation fails
        }
        if(listName.equals(Constants.Shared_Documents)){
            LOGGER.info("This list name is not allowed to create");
            return payload;
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(null, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);    // Check if site exists

            if (existingSite == null) {
                return handleIfNotFound(payload, "Site '" + siteName + "' not found while creating list");
            }

            List microsoftGraphList = buildMicrosoftGraphList(listName);

            List createdList = graphClient.sites(existingSite.id).lists().buildRequest().post(microsoftGraphList);

            OperationsAreaUtilityService.processResponse(createdList, payload, Constants.LIST_CREATED);
        } catch (Exception cause) {
            LOGGER.error("Error creating list : {} ", cause.getMessage());
            handleIfNotFound(payload, Constants.ERROR_LIST_CREATION);
        }
        return payload;
    }

    private List buildMicrosoftGraphList(String listName) {
        List microsoftGraphList = new List();                           // Create the Microsoft Graph List object
        microsoftGraphList.displayName = listName;
        ListInfo list = new ListInfo();
        list.template = Constants.documentLibrary;
        list.contentTypesEnabled = true;
        list.hidden = false;
        microsoftGraphList.list = list;

        return microsoftGraphList;
    }

    /**
     * Deletes a specified list from a SharePoint site.
     */
    public Map<String, Object> deleteList(String siteName, String listName) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndListNames(siteName, listName, payload)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(null, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                return handleIfNotFound(payload, "Site '" + siteName + "' not found while deleting list");
            }

            List existingList = OperationsAreaUtilityService.getList(graphClient, existingSite, listName);
            if (existingList == null) {
                return handleIfNotFound(payload, "List '" + listName + "' not found while deleting list");
            }

            graphClient.sites(existingSite.id).lists(existingList.id).buildRequest().delete();

            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, existingList.displayName + Constants.LIST_DELETED_SUCCESSFULLY);
        } catch (Exception cause) {
            LOGGER.error("Error deleting list : {} ", cause.getMessage());
            handleIfNotFound(payload, Constants.FAILED_TO_MODIFY_LIST);
        }
        return payload;
    }

    /**
     * Updates the display name and description of a specified list within a SharePoint site.
     */
    public Map<String, Object> updateList(String siteName, String listName, String displayName, String description) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndListAndUpdatedNames(siteName, listName, payload, displayName)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(null, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                return handleIfNotFound(payload, "Site '" + siteName + "' not found while updating list");
            }

            List existingList = OperationsAreaUtilityService.getList(graphClient, existingSite, listName);
            if (existingList == null) {
                return handleIfNotFound(payload, "List '" + listName + "' not found while updating list");
            }

            existingList.displayName = displayName;
            existingList.description = description;
            List updatedList = graphClient.sites(existingSite.id).lists(existingList.id).buildRequest().patch(existingList);

            OperationsAreaUtilityService.processResponse(updatedList, payload, "modified"); // Use the processResponse method for clarity
        } catch (Exception cause) {
            LOGGER.error("Error updating list : {} ", cause.getMessage());
            handleIfNotFound(payload, Constants.FAILED_TO_MODIFY_LIST);
        }
        return payload;
    }

    private Map<String, Object> handleIfNotFound(Map<String, Object> payload, String Message) {
        payload.put(Constants.MESSAGE, Message);
        payload.put(Constants.SUCCESS, false);
        return payload;
    }

}
