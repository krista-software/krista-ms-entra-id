package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DriveSharedPermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilePermissionSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionSearchDao.class);
    private final GraphServiceClientProvider clientProvider;
    private final FileSearchImpl fileSearchImpl;
    private final SharePointAttributes sharePointAttributes;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public FilePermissionSearchDao(GraphServiceClientProviderFactory clientProviderFactory, FileSearchImpl fileSearchImpl,
                                   SharePointAttributes sharePointAttributes, FileSearchUtilityService fileSearchUtilityService) {
        this.clientProvider = clientProviderFactory.create();
        this.fileSearchImpl = fileSearchImpl;
        this.sharePointAttributes = sharePointAttributes;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    /**
     * Searches for files with a specified permission, handling potential errors and returning matching files.
     * @return A FreeForm object containing the list of files with the specified permission or error messages.
     */
    public FreeForm getSearchByPermission(String permission, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with permission: {} ", permission);
        FreeForm payload = new FreeForm();
        if (permission != null) {
            List<DriveSharedPermissionImpl> resultPage = new ArrayList<>();  // Initialize the result page
            try {
                int index = 0;
                int from = 0;
                int size = 500;
                GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(useEmail, accountID);
                SearchEntityQueryCollectionPage page = getSearchResult(graphClient, from, size);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    do {
                        extractDriveFromSearchResultsUsingPath(resultPage, page, graphClient);
                        index++;
                        from = (index * size) + 1;
                        page = getSearchResult(graphClient, from, size);
                    } while (ResponseUtil.moreResultsAvailable(page));
                }

                if (!resultPage.isEmpty()) {
                    List<File> fileEntity = resultPage.stream().map(CatalogTypes::fromFile).filter(file -> hasPermission(file, permission)).collect(Collectors.toList());
                    ResponseUtil.listOfFileEntityResponse(fileEntity, true, Constants.FILE_FOUND, payload);
                } else {
                    ResponseUtil.getInfoResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {} {} ", cause.getMessage(), cause);
                ResponseUtil.getInfoResponse(false, Constants.FAILED_GET_FILE_FROM_INPUT_STREAM_FILE_NAME_EMPTY, payload);
            }
        }
        return payload;
    }

    private SearchEntityQueryCollectionPage getSearchResult(GraphServiceClient<Request> graphClient, int from, int size) {
        LinkedList<SearchRequest> requestsList = buildSearchRequestListWithAbsolutePath(from, size);
        return fileSearchImpl.buildSearchQuery(graphClient, requestsList);
    }

    private boolean hasPermission(File fileEntity, String permission) {
        return fileEntity.permissions != null && fileEntity.permissions.equalsIgnoreCase(permission);
    }

    public LinkedList<SearchRequest> buildSearchRequestListWithAbsolutePath(int from, int size) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(requests, from, size);
        requestsList.add(requests);
        return requestsList;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.LIST, EntityType.DRIVE, EntityType.DRIVE_ITEM);

        // Fill in the rest of the request setup logic
        LinkedList<SortProperty> sortProperties = new LinkedList<>();
        SortProperty sort = new SortProperty();
        sort.name = "LastModifiedTime";
        sort.isDescending = false;
        sortProperties.add(sort);
        requests.sortProperties = sortProperties;
        return requests;
    }

    public void extractDriveFromSearchResultsUsingPath(List<DriveSharedPermissionImpl> files, SearchEntityQueryCollectionPage page, GraphServiceClient<Request> graphClient) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                if (driveItem.webUrl != null && !driveItem.webUrl.contains("sharepoint.com/personal/")) {
                                    files.add(new DriveSharedPermissionImpl(driveItem, graphClient, sharePointAttributes, fileSearchUtilityService));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void buildSearchRequestForRest(SearchRequest requests, int from, int size) {
        String domainName = fileSearchImpl.getDomainName();
        String siteDomainPath = fileSearchImpl.getDomainPath(domainName, false);
        SearchQuery query = new SearchQuery();
        query.queryString = domainName + " AND NOT path:\"" + siteDomainPath + "\"";
        requests.query = query;
        requests.from = from;
        requests.size = size;
    }
}
