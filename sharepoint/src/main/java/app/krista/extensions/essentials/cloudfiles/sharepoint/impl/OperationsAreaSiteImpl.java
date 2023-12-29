package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OperationsAreaSiteImpl {

    private final GraphServiceClientProvider clientProvider;

    private static final Logger log = LoggerFactory.getLogger(OperationsAreaImpl.class);

    @Inject
    public OperationsAreaSiteImpl(GraphServiceClientProviderFactory clientProviderFactory, SharePointAttributes sharePointAttributes) {
        this(clientProviderFactory.create(sharePointAttributes));
    }

    public OperationsAreaSiteImpl(GraphServiceClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    /**
     * Updates the display name of a specified SharePoint site.
     *
     * @param siteName Name of the site to be updated.
     * @param newSiteName New display name for the site.
     * @return A Map containing details of the operation's success or failure:
     *         - Success: true if site update succeeded, false otherwise.
     *         - Message: Informative message about the operation's outcome.
     *         - File (optional): Details of the updated site if successful.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> updateSite(String siteName, String newSiteName) {
        Map<String, Object> payload = new HashMap<>();

        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            Site existingSite = OperationsAreaUtilityService.getSite(graphClient, siteName);
            if (existingSite == null) {
                payload.put("Message", "Site '" + siteName + "' not found while updating site");
                payload.put("Success", false);
                return payload;
            }

            existingSite.displayName = newSiteName;

            Site updatedSite = graphClient.sites(existingSite.id).buildRequest().patch(existingSite);

            OperationsAreaUtilityService.processResponse(updatedSite, payload); // Use the processResponse method
        } catch (Exception cause) {
            log.error("Error updating site", cause);
            payload.put("Message", Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "Site Not Found" : cause.getMessage());
            payload.put("Success", false);
        }
        return payload;
    }

    /**
     * Deletes a specified SharePoint site.
     *
     * @param siteName Name of the site to be deleted.
     * @return A Map containing details of the operation's success or failure:
     *         - Success: true if site deletion succeeded, false otherwise.
     *         - Message: Informative message about the operation's outcome.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> deleteSite(String siteName) {
        Map<String, Object> payload = new HashMap<>();
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

            GroupCollectionPage existingGroups = graphClient.groups().buildRequest().get();

            Group groupToDelete = null;

            if (existingGroups != null) {
                for (Group group : existingGroups.getCurrentPage()) {
                    if (group.displayName.equals(siteName)) {
                        groupToDelete = group;
                        break;
                    }
                }
            }

            if (groupToDelete != null) {
                graphClient.groups(groupToDelete.id).buildRequest().delete();
                payload.put("Success", true);
                payload.put("Message", groupToDelete.displayName + " Site deleted successfully");
            } else {
                payload.put("Success", false);
                payload.put("Message", "Site with name " + siteName + " not found");
            }
        } catch (Exception cause) {
            log.error("Error deleting site", cause);
            payload.put("Success", false);
            payload.put("Message", Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "Site Not Found" : cause.getMessage());
        }
        return payload;
    }

}
