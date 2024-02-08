package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileCreateService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsAreaListImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsAreaSiteImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.http.GraphServiceException;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Class for creating,deleting and updating SharePoint lists & Site using Microsoft Graph API.
 *
 * @author Vaibhav Choudhary
 */
@Service
public class OperationsAreaImpl {

    private final GraphServiceClientProvider clientProvider;

    private final OperationsAreaListImpl operationsAreaListImpl;

    private final OperationsAreaSiteImpl operationsAreaSiteImpl;


    @Inject
    public OperationsAreaImpl(GraphServiceClientProviderFactory clientProviderFactory,
                              OperationsAreaListImpl operationsAreaListImpl,
                              OperationsAreaSiteImpl operationsAreaSiteImpl) {
        this.clientProvider = clientProviderFactory.create();
        this.operationsAreaListImpl = operationsAreaListImpl;
        this.operationsAreaSiteImpl = operationsAreaSiteImpl;
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
        FileCreateService fileCreateService = new FileCreateService(clientProvider.
                getUserRequestBuilder(null, null));
        return fileCreateService.createGroup(groupName, mailNickName);
    }

}
