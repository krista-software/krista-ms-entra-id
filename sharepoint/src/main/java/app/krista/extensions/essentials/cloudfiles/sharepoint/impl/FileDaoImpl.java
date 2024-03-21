package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntitySearchMapper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.strategy.CustomFilterStrategy;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FilePermissionService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileSearchImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.SearchFileExecutor;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.SearchQuery;
import com.microsoft.graph.models.SearchRequest;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.PermissionCollectionPage;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * Implementation of FileDao interface for Microsoft Graph API file operations, including creation, retrieval, and search.
 *
 * @author Vaibhav Choudhary
 */

@Service
@ContractsProvided(FileDao.class)
public class FileDaoImpl implements FileDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDaoImpl.class);
    private final GraphServiceClientProvider clientProvider;
    private final FileSearchImpl fileSearchImpl;
    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public FileDaoImpl(GraphServiceClientProviderFactory clientProviderFactory, FileSearchImpl fileSearchImpl, SearchFileExecutor searchFileExecutor) {
        this.clientProvider = clientProviderFactory.create();
        this.fileSearchImpl = fileSearchImpl;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Retrieves a list of shared permissions from a PermissionCollectionPage, including SharePoint attributes.
     *
     * @param permissionCollectionPage The PermissionCollectionPage containing the permissions to process.
     * @return A list of maps, where each map represents a shared permission and its details. The maps contain keys for:
     * - "email": The email address of the user or group with the permission.
     * - "roles": A list of permission roles (e.g., "write", "read").
     * - "link": The sharing link URL (if applicable).
     * - "sharepointAttributes": A map containing SharePoint-specific attributes (if applicable).
     * - ... Other permission details ...
     */
    public static List<Map<String, Object>> getSharedWithPermissions(PermissionCollectionPage permissionCollectionPage) {
        LOGGER.info("Getting shared permissions for PermissionCollectionPage and SharePointAttributes.");
        return new FilePermissionService().getSharedWithPermissions(permissionCollectionPage);
    }

    /**
     * Retrieves a page of files filtered by name, selector, and pagination parameters.
     * Caches results for efficient retrieval of subsequent pages.
     *
     * @param name      The name of the file to search for.
     * @param selector  The type of filter to apply: "Extension", "Owner", "Absolute Path", or general search.
     * @param pageIndex The index of the page to retrieve (1-based).
     * @param pageSize  The number of files per page.
     * @return A list of {@link DriveImpl} objects representing the found files, or an empty list if no files match the criteria or an error occurs.
     * @throws IllegalArgumentException If an invalid selector is provided.
     */
    @Override
    public List<FileService> getFilesByFilter(List<SearchCondition> conditions, String name, String selector, long pageIndex, int pageSize) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", name, selector, pageIndex, pageSize);
        try {
            final List<FileService> newCachedFiles = new ArrayList<>();
            return getFileServices(conditions, name, selector, pageIndex, pageSize, newCachedFiles);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error during file search: {} {} ", cause.getMessage(), cause);
            return Collections.emptyList();
        }
    }

    @NotNull
    private List<FileService> getFileServices(List<SearchCondition> conditions, String name, String selector, long pageIndex, int pageSize, List<FileService> newCachedFiles) {
        GraphServiceClient<Request> graphClient = getUserRequestBuilder();

        while ((newCachedFiles.size()+((pageIndex - 1) * pageSize)) < (pageIndex - 1) * pageSize + pageSize) {

            LinkedList<SearchRequest> requestsList = new LinkedList<>();
            prepareRequestList(conditions, name, selector, (int) pageIndex, pageSize, requestsList);

            Map<String ,Boolean> moreResultAvailable = new HashMap<>();

            SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);
            moreResultAvailable.put(Constants.MORE_RESULT_AVAILABLE,Boolean.TRUE);
            if (page != null && !page.getCurrentPage().isEmpty()) {
                fileSearchImpl.extractDriveFromSearchResults(newCachedFiles, page, moreResultAvailable, pageSize);
            } else {
                break;
            }
            boolean iSMoreResultAvailable = moreResultAvailable.get(Constants.MORE_RESULT_AVAILABLE);
            if(!iSMoreResultAvailable){
                return newCachedFiles;
            }
            if (newCachedFiles.isEmpty() || newCachedFiles.size() < pageSize) {
                pageIndex++;
            }
        }
        return newCachedFiles;
    }

    private void prepareRequestList(List<SearchCondition> conditions, String name, String selector, int pageIndex, int pageSize, LinkedList<SearchRequest> requestsList) {
        SearchRequest requests = fileSearchImpl.buildSearchRequest();
        requests.query = buildQueryString(conditions, name, selector);
        requests.from = (pageIndex - 1) * pageSize;
        requests.size = 50;//(int)pageIndex * pageSize;  // Adjust for partial page
        JsonPrimitive trimDuplicatesJson = new JsonPrimitive(true);
        requests.additionalDataManager().put("trimDuplicates", trimDuplicatesJson);
        requestsList.add(requests);
    }

    private SearchQuery buildQueryString(List<SearchCondition> conditions, String name, String selector) {
        String domainName = fileSearchImpl.getDomainName();
        String siteContoso = "https://" + domainName + ".sharepoint.com";
        String path = "path:\"" + siteContoso + "\"";
        SearchQuery query = new SearchQuery();
        if (conditions != null && !conditions.isEmpty()) {
            query.queryString = path;
            for (SearchCondition condition : conditions) {
                if (CustomFilterStrategy.excludeConditions(condition)) {
                    continue;
                }
                CustomFilterStrategy.prepareSearchQuery(condition, query);
            }
        } else {
            query.queryString = path;
            if(name.equals(Constants.LOGICAL_NAME)) {
                query.queryString = (query.queryString + " AND " + Constants.ALL);
                return query;
            }

            CustomFilterStrategy.prepareSearchQuery(name, selector, query);
        }
        return query;
    }

    @Override
    public FileService getFileByAbsolutePath(String path) {
        Map<String, Object> payload = new HashMap<>();
        searchFileExecutor.getFileInfo(path, payload, true);
        String sitePath = payload.containsKey(Constants.SOURCE_ROOT_PATH) ? payload.get(Constants.SOURCE_ROOT_PATH).toString() : null;
        return new FileEntitySearchMapper(payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null, sitePath, null);
    }


    private GraphServiceClient<Request> getUserRequestBuilder() {
        return clientProvider.getUserRequestBuilder(null, null);
    }

}