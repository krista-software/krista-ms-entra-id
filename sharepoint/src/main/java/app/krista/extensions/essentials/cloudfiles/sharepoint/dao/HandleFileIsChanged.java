package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileChangeResourceUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemDeltaParameterSet;
import com.microsoft.graph.models.SearchQuery;
import com.microsoft.graph.models.SearchRequest;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

@Service
public class HandleFileIsChanged {

    private final GraphServiceClientProvider provider;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HandleFileIsChanged.class);
    private final String routingUrl;
    private final FileSearchImpl fileSearchImpl;
    private final ManagementUtilityService managementUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;


    @Inject
    public HandleFileIsChanged(GraphServiceClientProviderFactory factory, Invoker invoker, FileSearchImpl fileSearchImpl,
                               ManagementUtilityService managementUtilityService, FileSearchUtilityService fileSearchUtilityService) {
        this(factory.create(), invoker, fileSearchImpl, managementUtilityService, fileSearchUtilityService);
    }

    public HandleFileIsChanged(GraphServiceClientProvider provider, Invoker invoker, FileSearchImpl fileSearchImpl,
                               ManagementUtilityService managementUtilityService, FileSearchUtilityService fileSearchUtilityService) {
        this.provider = provider;
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.fileSearchImpl = fileSearchImpl;
        this.managementUtilityService = managementUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    /**
     * Retrieves information about changes made to a file at the specified path, handling path validation, potential errors, and returning file change details.
     *
     * @return A Map containing status and result details, including success/failure indicators, file change information, or error messages.
     */
    public Map<String, Object> getChangeFileEvent(String source, String search) {
        System.out.println(" getChangeFileEvent started " + source + " " + search + " " + routingUrl);
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
                String domainName = fileSearchImpl.getDomainName();
                String siteDomainPath = fileSearchImpl.getDomainPath(domainName, true);
                String absolutPath = fileSearchImpl.handleAbsolutePath(source, siteDomainPath);

                getWhichChangesMadeInFile(absolutPath, search, graphClient, payload, source);
                System.out.print(" Iteration is done ");
            } else {
                ResponseUtil.successResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            ResponseUtil.successResponse(false, Constants.FILE_NOT_FOUND, payload);
        }
        return payload;
    }

    private Map<String, Object> getWhichChangesMadeInFile(String absolutPath, String search, GraphServiceClient<Request> graphClient,
                                                          Map<String, Object> payload, String source) {
        try {
            DriveItem deltaDriveItem = getDriveItemThroughAbsolutePath(absolutPath);
            DriveItemDeltaCollectionPage deltaCollectionPage = FileChangeResourceUtil.getFileChangeResource(absolutPath);
            DriveItem notificationDriveItem = null;

            DriveItemDeltaCollectionPage page = getDeltaQueryThroughDriveItemAndAbsolutePath(deltaDriveItem, graphClient);

            for (DriveItem newDrive : page.getCurrentPage()) {
                if (isSearchAvailable(search, newDrive)) {
                    notificationDriveItem = newDrive;
                    break;
                } else {
                    notificationDriveItem = newDrive;
                }
            }

            // handle in case event notification is already sent
            List<DriveItem> newDriveItemList = getFileByAbsolutePath(absolutPath, graphClient);
            if (notificationDriveItem != null) {

                if (notificationDriveItem.deleted != null && notificationDriveItem.deleted.state.equals("deleted")) {
                    payload.put(Constants.FILE, File.fromFile(new DrivePermissionImpl(new DriveItem(), graphClient, fileSearchUtilityService)));
                    payload.put(Constants.EVENT, Constants.DELETED);
                    return payload;
                }

                List<DriveItem> driveItemList = FileChangeResourceUtil.getOldFileChangeResource(source);
                boolean flag = false;
                if (driveItemList != null) {
                    for (DriveItem driveItem : driveItemList) {
                        if (driveItem.id != null && driveItem.id.equals(notificationDriveItem.id)
                                && notificationDriveItem.oDataType.equals(Constants.MICROSOFT_GRAPH_DRIVE_ITEM)) {
                            addUpdatedFile(notificationDriveItem, payload, graphClient);
                            flag = false;
                            break;
                        }
                        flag = true;
                    }
                }
                if (flag && notificationDriveItem.id != null && notificationDriveItem.oDataType.equals(Constants.MICROSOFT_GRAPH_DRIVE_ITEM)) {
                    addCreatedFile(notificationDriveItem, payload, graphClient);
                }
            }
            FileChangeResourceUtil.addFileMetaData(absolutPath, deltaCollectionPage);
            FileChangeResourceUtil.addOldFileMetaData(source, newDriveItemList);
        } catch (Exception cause) {
            throw new IllegalArgumentException();
        }
        return payload;
    }

    private DriveItemDeltaCollectionPage getNewNotificationThroughDriveId(DriveItem deltaDriveItem, GraphServiceClient<Request> graphClient, DriveItemDeltaCollectionPage deltaCollectionPage) {
        DriveItemDeltaCollectionPage newDeltaCollectionPage = null;
        try {
            String token = DriveUtil.getTokenExtractor(deltaCollectionPage.deltaLink);

            newDeltaCollectionPage = graphClient
                    .drives(deltaDriveItem.parentReference.id).items(deltaDriveItem.id)
                    .delta(DriveItemDeltaParameterSet.newBuilder().withToken(token).build())
                    .buildRequest()
                    .get();
        } catch (Exception cause) {
            System.out.println(" Exception " + cause);
        }
        return newDeltaCollectionPage;
    }

    private boolean isSearchAvailable(String search, DriveItem newDrive) {
        return search != null && newDrive != null && newDrive.name != null && newDrive.name.contains(search);
    }

    private DriveItemDeltaCollectionPage getDeltaQueryThroughDriveItemAndAbsolutePath(DriveItem driveItem, GraphServiceClient<Request> graphClient) {
        try {
            return graphClient.drives(driveItem.parentReference.driveId)
                    .items(driveItem.id).delta().buildRequest().get();
        } catch (Exception cause) {
            throw new IllegalArgumentException(" ");
        }
    }

    public List<DriveItem> getFileByAbsolutePath(String filePath, GraphServiceClient<Request> graphClient) {
        LOGGER.info("Searching for files with filePath: {} ", filePath);
        List<DriveItem> resultPage = new ArrayList<>();  // Initialize the result page
        if (filePath != null) {
            filePath = filePath.replace(Constants.WHITE_SPACE, " ");

            try {
                LinkedList<SearchRequest> requestsList = buildSearchRequestList(filePath);
                SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    fileSearchImpl.extractDriveItemListFromSearchResults(resultPage, page);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {} ", cause.getMessage(), cause);
                throw new IllegalArgumentException(cause.getMessage(), cause);
            }
        }
        return resultPage;
    }

    public LinkedList<SearchRequest> buildSearchRequestList(String filePath) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = fileSearchImpl.buildSearchRequest();
        buildSearchRequestForRest(filePath, requests);
        requestsList.add(requests);
        return requestsList;
    }

    public void buildSearchRequestForRest(String filePath, SearchRequest requests) {
        SearchQuery query = new SearchQuery();
        query.queryString = "path:\"" + filePath + "\"";
        requests.query = query;
        requests.from = 0;
        requests.size = 500;
    }

    private void addUpdatedFile(DriveItem driveItem, Map<String, Object> payload, GraphServiceClient<Request> graphClient) {
        app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = File.fromFile(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService));
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.EVENT, Constants.UPDATED);
    }

    private void addCreatedFile(DriveItem driveItem, Map<String, Object> payload, GraphServiceClient<Request> graphClient) {
        File fileEntity = File.fromFile(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService));
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.EVENT, Constants.CREATED);
    }

    public DriveItem getDriveItemThroughAbsolutePath(String source) {
        DriveItem driveItem = null;
        if (source == null || source.isBlank()) {
            return driveItem;
        }
        driveItem = managementUtilityService.getRootFolderByAbsolutePath(source, false);
        return driveItem;
    }

}
