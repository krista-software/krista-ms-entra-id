package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;

import javax.inject.Inject;

@Service
@ContractsProvided(FileDao.class)
public class FileDaoImpl implements FileDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDaoImpl.class);
    private final GraphServiceClientProvider clientProvider;
    protected static List<DriveImpl> cachedFiles = new ArrayList<>();

    @Inject
    public FileDaoImpl(GraphServiceClientProviderFactory clientProviderFactory) {
        this(clientProviderFactory.create());
    }

    public FileDaoImpl(GraphServiceClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    /**
     * Creates a new group using the {@link FileCreateService}.
     * This method retrieves the Graph client and calls the `FileCreateService`'s `createGroup` method
     * to handle the group creation logic with dedicated functionality.
     *
     * @param map A map containing information necessary for group creation, such as name and base name.
     * @return A GroupImpl object representing the newly created group, or null if creation fails.
     * @throws GraphServiceException If an error occurs during group creation.
     */
    @Override
    public GroupImpl createGroup(Map<String, Object> map) {
        LOGGER.info("Creating group with details: {}", map);
        FileCreateService fileCreateService = new FileCreateService(clientProvider.getUserRequestBuilder(true, null));
        return fileCreateService.createGroup(map);
    }

    /**
     * Retrieves a file by its absolute path using Microsoft Graph search.
     *
     * @param filePath The absolute path of the file to retrieve.
     * @return The {@link FileService} object representing the found file, or null if not found.
     * @throws IllegalArgumentException If the file path is null or an error occurs during the search.
     */
    @Override
    public FileService getFileByAbsolutePath(String filePath) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", filePath);
        if(filePath != null) {
            filePath = filePath.replace("%20", " ");
            List<DrivePermissionImpl> resultPage = new ArrayList<>();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);

                LinkedList<SearchRequest> requestsList = new LinkedList<>();
                SearchRequest requests = new SearchRequest();
                requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.LIST_ITEM,
                        EntityType.DRIVE, EntityType.DRIVE_ITEM);

                LinkedList<SortProperty> sortProperties = new LinkedList<>();
                SortProperty sort = new SortProperty();
                sort.name = "LastModifiedTime";
                sort.isDescending = false;
                sortProperties.add(sort);
                requests.sortProperties = sortProperties;

                SearchQuery query = new SearchQuery();
                query.queryString = "path:\"" + filePath + "\"";

                requests.query = query;
                requests.from = 0;
                requests.size = 1;
                requestsList.add(requests);

                SearchEntityQueryCollectionPage page = graphClient.search().query(SearchEntityQueryParameterSet
                        .newBuilder().withRequests(requestsList).build()).buildRequest().post();

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    for (SearchResponse driveInPage : page.getCurrentPage()) {
                        if (driveInPage.hitsContainers != null) {
                            for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                                if (hitContainer.hits != null) {
                                    for (SearchHit hits : hitContainer.hits) {
                                        if (hits != null && hits.resource instanceof DriveItem) {
                                            DriveItem driveItem = (DriveItem) hits.resource;
                                            resultPage.add(new DrivePermissionImpl(driveItem, null, null, clientProvider));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(resultPage.size() > 0) {
                    return resultPage.get(0);
                } else {
                    return null;
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
                throw new IllegalArgumentException(cause.getMessage(), cause);
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieves a list of shared permissions from a PermissionCollectionPage, including SharePoint attributes.
     *
     * @param permissionCollectionPage The PermissionCollectionPage containing the permissions to process.
     * @return A list of maps, where each map represents a shared permission and its details. The maps contain keys for:
     *      - "email": The email address of the user or group with the permission.
     *      - "roles": A list of permission roles (e.g., "write", "read").
     *      - "link": The sharing link URL (if applicable).
     *      - "sharepointAttributes": A map containing SharePoint-specific attributes (if applicable).
     *      - ... Other permission details ...
     */
    public static List<Map<String, Object>> getSharedWithPermissions(PermissionCollectionPage permissionCollectionPage) {
        LOGGER.info("Getting shared permissions for PermissionCollectionPage and SharePointAttributes.");
        return new FilePermissionService().getSharedWithPermissions(permissionCollectionPage);
    }

    /**
     * Retrieves a page of files filtered by name, selector, and pagination parameters.
     * Caches results for efficient retrieval of subsequent pages.
     *
     * @param name The name of the file to search for.
     * @param selector The type of filter to apply: "Extension", "Owner", "Absolute Path", or general search.
     * @param pageIndex The index of the page to retrieve (1-based).
     * @param pageSize The number of files per page.
     * @return A list of {@link DriveImpl} objects representing the found files, or an empty list if no files match the criteria or an error occurs.
     * @throws IllegalArgumentException If an invalid selector is provided.
     */
    @Override
    public List<DriveImpl> getFilesByFilter(String name, String selector, long pageIndex, int pageSize) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", name, selector, pageIndex, pageSize);
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(true, null);
            if(pageIndex == 1) {
                cachedFiles = new ArrayList<>();
            }
            // Fetch more data if the cached data doesn't cover the full requested page
            if (cachedFiles.size() < (pageIndex - 1) * pageSize + pageSize) {
                int totalFetched = cachedFiles.size();  // Start from the current cached count
                while (totalFetched < (pageIndex - 1) * pageSize + pageSize && cachedFiles.size() < totalFetched + 25) {
                    LinkedList<SearchRequest> requestsList = new LinkedList<>();

                    // Create a new SearchRequest instance within the loop
                    SearchRequest requests = new SearchRequest();
                    requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.LIST_ITEM,
                            EntityType.DRIVE, EntityType.DRIVE_ITEM);

                    // Fill in the rest of the request setup logic
                    LinkedList<SortProperty> sortProperties = new LinkedList<>();
                    SortProperty sort = new SortProperty();
                    sort.name = "LastModifiedTime";
                    sort.isDescending = false;
                    sortProperties.add(sort);
                    requests.sortProperties = sortProperties;

                    SearchQuery query = new SearchQuery();
                    if ("Extension".equals(selector)) {
                        query.queryString = "isDocument=true AND filetype:" + name;
                    } else if ("Owner".equals(selector)) {
                        query.queryString = "isDocument=true AND CreatedBy:" + name;
                    } else if ("Absolute Path".equals(selector)) {
                        query.queryString = "isDocument=true AND path:\"" + name + "\"";
                    } else {
                        query.queryString = "isDocument=true AND " + name;
                    }
                    requests.query = query;
                    requests.from = ((int)pageIndex - 1) * pageSize;  // Start from 0 for the first page, 10 for the second, and so on
                    requests.size = Math.min(25, pageSize - cachedFiles.size() % pageSize);  // Adjust for partial page

                    requestsList.add(requests);

                    SearchEntityQueryCollectionPage page = graphClient.search().query(SearchEntityQueryParameterSet
                            .newBuilder().withRequests(requestsList).build()).buildRequest().post();

                    if (page != null && !page.getCurrentPage().isEmpty()) {
                        List<DriveImpl> files = new ArrayList<>();  // Create a new list for each response
                        for (SearchResponse driveInPage : page.getCurrentPage()) {
                            // Extract DriveImpl objects from the response
                            if (driveInPage.hitsContainers != null) {
                                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                                    if (hitContainer.hits != null) {
                                        for (SearchHit hits : hitContainer.hits) {
                                            if (hits != null && hits.resource instanceof DriveItem) {
                                                DriveItem driveItem = (DriveItem) hits.resource;
                                                files.add(new DriveImpl(driveItem, null, null, clientProvider));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        cachedFiles.addAll(files);  // Add fetched files to cache
                    } else {
                        break;  // No more results available
                    }
                    totalFetched += page.getCurrentPage().size();
                    if (page.getCurrentPage().size() < 25) {
                        break;
                    }
                }
            }

            // Extract the requested page from the cached results, accounting for partial data
            int startIndex = (int) ((pageIndex - 1) * pageSize);
            int endIndex = Math.min(startIndex + pageSize, cachedFiles.size());
            return cachedFiles.subList(startIndex, endIndex);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            // Log the error for debugging
            LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
            return Collections.emptyList();
        }
    }

}