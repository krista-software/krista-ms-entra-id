package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.ColumnDefinitionCollectionPage;
import com.microsoft.graph.requests.ColumnDefinitionCollectionResponse;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class OperationsAreaListImpl {

    private final GraphServiceClientProvider clientProvider;

    private static final Logger log = LoggerFactory.getLogger(OperationsAreaImpl.class);

    @Inject
    public OperationsAreaListImpl(GraphServiceClientProviderFactory clientProviderFactory, SharePointAttributes sharePointAttributes) {
        this(clientProviderFactory.create(sharePointAttributes));
    }

    public OperationsAreaListImpl(GraphServiceClientProvider clientProvider) {
        this.clientProvider = clientProvider;
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
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);    // Check if site exists
            if (existingSite == null) {
                payload.put("Message", "Site '" + siteName + "' not found while creating list");
                payload.put("Success", false);
                return payload;
            }

            List microsoftGraphList = new List();                           // Create the Microsoft Graph List object
            microsoftGraphList.displayName = listName;

            // Set "Modified" and "Modified By" columns ... these are the default column which we are adding
            LinkedList<ColumnDefinition> columnsList = new LinkedList<>();

            ColumnDefinition columnDefinition = new ColumnDefinition();
            columnDefinition.name = "Modified";
            columnDefinition.dateTime = new DateTimeColumn();
            columnsList.add(columnDefinition);

            columnDefinition = new ColumnDefinition();
            columnDefinition.name = "Modified By";
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
            OperationsAreaUtilityService.processResponse(list,payload);
        } catch (Exception cause) {
            log.error("Error creating list", cause);
            payload.put("Message", Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "Site Not Found" : cause.getMessage());
            payload.put("Success", false);
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
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                payload.put("Message", "Site '" + siteName + "' not found while deleting list");
                payload.put("Success", false);
                return payload;
            }

            List existingList = OperationsAreaUtilityService.getList(graphClient, existingSite, listName);
            if (existingList == null) {
                payload.put("Message", "List '" + listName + "' not found while deleting list");
                payload.put("Success", false);
                return payload;
            }

            graphClient.sites(existingSite.id).lists(existingList.id).buildRequest().delete();

            payload.put("Success", true);
            payload.put("Message", existingList.displayName + " List deleted successfully");
        } catch (Exception cause) {
            log.error("Error deleting list", cause);
            payload.put("Message", Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "List Not Found" : cause.getMessage());
            payload.put("Success", false);
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
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                payload.put("Message", "Site '" + siteName + "' not found while updating list");
                payload.put("Success", false);
                return payload;
            }

            List existingList = OperationsAreaUtilityService.getList(graphClient, existingSite, listName);
            if (existingList == null) {
                payload.put("Message", "List '" + listName + "' not found while updating list");
                payload.put("Success", false);
                return payload;
            }

            existingList.displayName = displayName;
            existingList.description = description;
            List updatedList = graphClient.sites(existingSite.id).lists(existingList.id)
                    .buildRequest()
                    .patch(existingList);

            OperationsAreaUtilityService.processResponse(updatedList, payload); // Use the processResponse method for clarity
        } catch (Exception cause) {
            log.error("Error updating list", cause);
            payload.put("Message", Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "List Not Found" : cause.getMessage());
            payload.put("Success", false);
        }
        return payload;
    }

}
