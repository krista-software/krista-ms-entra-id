package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FilePermissionDaoUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DriveSharedPermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileMapper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilePermissionSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionSearchDao.class);
    private final GraphServiceClientProvider clientProvider;
    private final FileSearchImpl fileSearchImpl;
    private final SharePointAttributes sharePointAttributes;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;
    private final GraphServiceClientProviderFactory clientProviderFactory;


    @Inject
    public FilePermissionSearchDao(GraphServiceClientProviderFactory clientProviderFactory, FileSearchImpl fileSearchImpl,
                                   SharePointAttributes sharePointAttributes, FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor, GraphServiceClientProviderFactory clientProviderFactory1) {
        this.clientProvider = clientProviderFactory.create();
        this.fileSearchImpl = fileSearchImpl;
        this.sharePointAttributes = sharePointAttributes;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
        this.clientProviderFactory = clientProviderFactory1;
    }

    /**
     * Searches for files with a specified permission, handling potential errors and returning matching files.
     *
     * @return A FreeForm object containing the list of files with the specified permission or error messages.
     */
    public FreeForm getSearchByPermission(String site, String permission, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with permission: {} ", permission);
        FreeForm payload = new FreeForm();
        Map<String, Object> mapPayload = new HashMap<>();
        if (permission != null) {
            try {
                searchFileExecutor.getFileInfo(site, mapPayload, true, useEmail, accountID);
                DriveItem driveItem = (mapPayload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) mapPayload.get(Constants.SOURCE_DRIVE_ITEM) : null);
                if (driveItem == null) {
                    payload.put(Constants.MESSAGE, Constants.TEXT, Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH);
                    return payload;
                }
                return fetchFilesWithPermissions(permission, useEmail, accountID, driveItem, mapPayload, payload);
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {} {} ", cause.getMessage(), cause);
                ResponseUtil.getInfoResponse(false, Constants.FAILED_GET_FILE_FROM_INPUT_STREAM_FILE_NAME_EMPTY, payload);
            }
        }
        return payload;
    }


    private SearchEntityQueryCollectionPage getSearchResult(GraphServiceClient<Request> graphClient, int from, int size) {
        List<SearchRequest> requestsList = buildSearchRequestListWithAbsolutePath(from, size);
        return fileSearchImpl.buildSearchQuery(graphClient, requestsList);
    }

    private boolean hasPermission(File fileEntity, String permission) {
        return fileEntity.permissions != null && fileEntity.permissions.equalsIgnoreCase(permission);
    }

    public List<SearchRequest> buildSearchRequestListWithAbsolutePath(int from, int size) {
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
        page.getCurrentPage().stream()
                .filter(response -> response.hitsContainers != null)
                .flatMap(response -> response.hitsContainers.stream())
                .filter(container -> container.hits != null)
                .flatMap(container -> container.hits.stream())
                .filter(hits -> hits != null && hits.resource instanceof DriveItem)
                .map(hits -> (DriveItem) hits.resource)
                .filter(driveItem -> driveItem.webUrl != null && !driveItem.webUrl.contains("sharepoint.com/personal/"))
                .map(driveItem -> new DriveSharedPermissionImpl(driveItem, graphClient, sharePointAttributes, fileSearchUtilityService, null))
                .forEach(files::add);
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


    public FreeForm fetchFilesWithPermissions(String permission, boolean useEmail, String accountID, DriveItem driveItem, Map<String, Object> mapPayLoad, FreeForm payload) {
        try {
            List<FileService> files = new ArrayList<>();
            boolean unUsedPermission = permission.equalsIgnoreCase(Constants.READ_AND_WRITE);
            if (!unUsedPermission) {

                DriveItemDeltaCollectionPage page = clientProvider.getUserRequestBuilder(useEmail, accountID)
                        .drives(driveItem.parentReference.driveId).
                        items(driveItem.id).delta().buildRequest().get();

                String sitePath = mapPayLoad.get(Constants.SOURCE_ROOT_PATH).toString();

                while (page != null && !page.getCurrentPage().isEmpty()) {
                    for (DriveItem driveInPage : page.getCurrentPage()) {
                        if (driveInPage.name != null && !driveInPage.name.equals("root")) {
                            iterateDriveItemWithPermission(permission, driveInPage, useEmail, accountID, files, sitePath);
                        }
                    }
                    if (page.getNextPage() != null) {
                        page = page.getNextPage().buildRequest().get();
                    } else {
                        ResponseUtil.listOfFileEntityResponse(files.stream()
                                .map(File::fromFile)
                                .collect(Collectors.toList()), true, "File With [ " + permission + " ] permission Fetched Successfully", payload);
                        break;
                    }
                }

                return payload;
            } else {
                ResponseUtil.listOfFileEntityResponse(files.stream()
                        .map(File::fromFile)
                        .collect(Collectors.toList()), false, "Permission [ " + permission + " ] is Not Supported in Sharepoint", payload);
                return payload;
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                    .contains(Constants.ITEM_NOT_FOUND)
                    ? Constants.FILE_NOT_FOUND
                    : Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH, payload);
        }
        return payload;
    }


    private void iterateDriveItemWithPermission(String permission, DriveItem driveInPage, boolean useEmail, String accountID, List<FileService> files, String sitePath) {
        try {
            DriveItem item = clientProvider.getUserRequestBuilder(useEmail, accountID)
                    .drives(driveInPage.parentReference.driveId).items(driveInPage.id).buildRequest().expand(Constants.QUERY_PERMISSIONS).get();

            List<Map<String, Object>> mapList = FilePermissionDaoUtil.getSharedPermissions(item, clientProvider.getUserRequestBuilder(useEmail, accountID));

            for (Map<String, Object> map : mapList) {
                if (map.get(Constants.USER_EMAIL).equals(sharePointAttributes.getMailId()) && map.get(Constants.PERMISSION).equals(permission)) {
                    files.add(new FileMapper(item, sitePath, permission));
                }
            }
        } catch (RuntimeException cause) {
            LOGGER.error(cause.getMessage());
        }

    }

}