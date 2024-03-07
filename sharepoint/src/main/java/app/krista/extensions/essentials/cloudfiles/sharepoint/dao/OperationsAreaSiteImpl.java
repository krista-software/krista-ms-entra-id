package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.OperationsAreaUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides operations for managing SharePoint sites, including updating display names and deleting sites.
 * It uses Microsoft Graph API for communication with SharePoint.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class OperationsAreaSiteImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsAreaSiteImpl.class);
    private final GraphServiceClientProvider clientProvider;
    private final OperationsAreaUtilityService operationsAreaUtilityService;

    @Inject
    public OperationsAreaSiteImpl(GraphServiceClientProviderFactory clientProviderFactory,
                                  OperationsAreaUtilityService operationsAreaUtilityService) {
        this.clientProvider = clientProviderFactory.create();
        this.operationsAreaUtilityService = operationsAreaUtilityService;
    }


    /**
     * Updates the display name of a specified SharePoint site.
     *
     * @param groupName    Name of the site to be updated.
     * @param newGroupName New display name for the site.
     * @return A Map containing details of the operation's success or failure:
     * - Success: true if site update succeeded, false otherwise.
     * - Message: Informative message about the operation's outcome.
     * - File (optional): Details of the updated site if successful.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> updateGroup(String groupName, String newGroupName) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteAndUpdatedNames(groupName, newGroupName, payload)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(null, null);
            Group existingGroup = OperationsAreaUtilityService.findMatchingGroup(graphClient, groupName);

            if (existingGroup == null) {
                payload.put(Constants.MESSAGE, "Group '" + groupName + "' not found while updating group");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }
            Group updateGroup = new Group();
            updateGroup.displayName = newGroupName;
            graphClient.groups(existingGroup.id).buildRequest().patch(updateGroup);
        } catch (GraphServiceException cause) {
            throw new IllegalStateException(cause.getMessage());
        }
        return ResponseUtil.preparePayloadResponse(null, true, Constants.GROUP_UPDATED_SUCCESSFULLY);
    }

    /**
     * Deletes a specified SharePoint site.
     *
     * @param groupName Name of the site to be deleted.
     * @return A Map containing details of the operation's success or failure:
     * - Success: true if site deletion succeeded, false otherwise.
     * - Message: Informative message about the operation's outcome.
     * @throws GraphServiceException If an error occurs during communication with Microsoft Graph.
     */
    public Map<String, Object> deleteGroup(String groupName) {
        Map<String, Object> payload = new HashMap<>();
        if (!operationsAreaUtilityService.validateSiteNames(groupName, payload)) {
            return payload; // Early return if validation fails
        }
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(null, null);

            GroupCollectionPage existingGroups = graphClient.groups().buildRequest().get();

            Group groupToDelete = null;

            if (existingGroups != null) {
                for (Group group : existingGroups.getCurrentPage()) {
                    if (group.displayName.equals(groupName)) {
                        groupToDelete = group;
                        break;
                    }
                }
            }

            if (groupToDelete != null) {
                graphClient.groups(groupToDelete.id).buildRequest().delete();
                payload.put(Constants.SUCCESS, true);
                payload.put(Constants.MESSAGE, groupToDelete.displayName + Constants.GROUP_DELETED_SUCCESSFULLY);
            } else {
                payload.put(Constants.SUCCESS, false);
                payload.put(Constants.MESSAGE, "Group with name " + groupName + " not found");
            }
        } catch (Exception cause) {
            LOGGER.error("Error deleting group : {} ", cause.getMessage());
            payload.put(Constants.SUCCESS, false);
            payload.put(Constants.MESSAGE, Constants.TO_DELETE_GROUP_PLEASE_TRY_AGAIN_LATER);
        }
        return payload;
    }

}
