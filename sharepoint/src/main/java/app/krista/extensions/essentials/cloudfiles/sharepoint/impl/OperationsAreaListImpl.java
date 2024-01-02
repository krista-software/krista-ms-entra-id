package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsAreaImpl.class);

    @Inject
    public OperationsAreaListImpl(GraphServiceClientProviderFactory clientProviderFactory,
                                  SharePointAttributes sharePointAttributes,OperationsAreaUtilityService operationsAreaUtilityService) {
        this(clientProviderFactory.create(sharePointAttributes),operationsAreaUtilityService);
    }

    public OperationsAreaListImpl(GraphServiceClientProvider clientProvider,OperationsAreaUtilityService operationsAreaUtilityService) {
        this.clientProvider = clientProvider;
        this.operationsAreaUtilityService = operationsAreaUtilityService;
    }

    /**
     * Creates a new list with default columns within a specified SharePoint site.
     *
     * @param siteName Name of the site where the list should be created.
     * @param listName Desired name for the new list.
     * @return A Map containing details of the operation's success or failure:
     *         - Success: true if list creation succeeded, false otherwise.
     *         - Message: Informative message about the operation's outcome.
     *         - File (optional): Details of the created list if successful.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> createList(String siteName, String listName) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndListNames(siteName, listName, payload)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);    // Check if site exists
            if (existingSite == null) {
                payload.put(Constants.MESSAGE, "Site '" + siteName + "' not found while creating list");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }

            List microsoftGraphList = new List();                           // Create the Microsoft Graph List object
            microsoftGraphList.displayName = listName;

            // Set "Modified" and "Modified By" columns ... these are the default column which we are adding
            LinkedList<ColumnDefinition> columnsList = new LinkedList<>();

            ColumnDefinition columnDefinition = new ColumnDefinition();
            columnDefinition.name = Constants.MODIFIED;
            columnDefinition.dateTime = new DateTimeColumn();
            columnsList.add(columnDefinition);

            columnDefinition = new ColumnDefinition();
            columnDefinition.name = Constants.MODIFIED_BY;
            columnDefinition.text = new TextColumn();

            columnsList.add(columnDefinition);

            // Set columns for the Microsoft Graph List
            ColumnDefinitionCollectionResponse columnDefinitionCollectionResponse = new ColumnDefinitionCollectionResponse();
            columnDefinitionCollectionResponse.value = columnsList;
            ColumnDefinitionCollectionPage columnDefinitionCollectionPage =
                    new ColumnDefinitionCollectionPage(columnDefinitionCollectionResponse, null);
            microsoftGraphList.columns = columnDefinitionCollectionPage;

            // Make the request to create the list & Return a success message or handle the response accordingly
            List list = graphClient.sites(existingSite.id).lists().buildRequest().post(microsoftGraphList);
            OperationsAreaUtilityService.processResponse(list,payload,"created");
        } catch (Exception cause) {
            LOGGER.error("Error creating list : ", cause.getMessage());
            payload.put(Constants.MESSAGE, Constants.ERROR_LIST_CREATION);
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    /**
     * Deletes a specified list from a SharePoint site.
     *
     * @param siteName Name of the site containing the list.
     * @param listName Name of the list to be deleted.
     * @return A Map containing details of the operation's success or failure:
     *         - Success: true if list deletion succeeded, false otherwise.
     *         - Message: Informative message about the operation's outcome.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> deleteList(String siteName, String listName) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndListNames(siteName,listName,payload)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                payload.put(Constants.MESSAGE, "Site '" + siteName + "' not found while deleting list");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }

            List existingList = OperationsAreaUtilityService.getList(graphClient, existingSite, listName);
            if (existingList == null) {
                payload.put(Constants.MESSAGE, "List '" + listName + "' not found while deleting list");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }

            graphClient.sites(existingSite.id).lists(existingList.id).buildRequest().delete();

            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, existingList.displayName + Constants.LIST_DELETED_SUCCESSFULLY);
        } catch (Exception cause) {
            LOGGER.error("Error deleting list : ", cause.getMessage());
            payload.put(Constants.MESSAGE, Constants.FAILED_TO_MODIFY_LIST);
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    /**
     * Updates the display name and description of a specified list within a SharePoint site.
     *
     * @param siteName Name of the site containing the list.
     * @param listName Name of the list to be updated.
     * @param displayName New display name for the list.
     * @param description New description for the list.
     * @return A Map containing details of the operation's success or failure:
     *         - Success: true if list update succeeded, false otherwise.
     *         - Message: Informative message about the operation's outcome.
     *         - File (optional): Details of the updated list if successful.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> updateList(String siteName, String listName, String displayName, String description) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndListAndUpdatedNames(siteName, listName ,payload, displayName)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                payload.put(Constants.MESSAGE, "Site '" + siteName + "' not found while updating list");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }

            List existingList = OperationsAreaUtilityService.getList(graphClient, existingSite, listName);
            if (existingList == null) {
                payload.put(Constants.MESSAGE, "List '" + listName + "' not found while updating list");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }

            existingList.displayName = displayName;
            existingList.description = description;
            List updatedList = graphClient.sites(existingSite.id).lists(existingList.id)
                    .buildRequest()
                    .patch(existingList);

            OperationsAreaUtilityService.processResponse(updatedList, payload, "modified"); // Use the processResponse method for clarity
        } catch (Exception cause) {
            LOGGER.error("Error updating list : ", cause.getMessage());
            payload.put(Constants.MESSAGE, Constants.FAILED_TO_MODIFY_LIST);
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

}
