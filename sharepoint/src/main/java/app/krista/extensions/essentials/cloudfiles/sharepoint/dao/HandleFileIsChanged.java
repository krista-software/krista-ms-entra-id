package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileChangeResourceUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FilePermissionDaoUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class HandleFileIsChanged {

    private final GraphServiceClientProvider provider;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HandleFileIsChanged.class);
    private final String routingUrl;
    private final FileSearchImpl fileSearchImpl;
    private final ManagementUtilityService managementUtilityService;


    @Inject
    public HandleFileIsChanged(GraphServiceClientProviderFactory factory, Invoker invoker, FileSearchImpl fileSearchImpl,
                               ManagementUtilityService managementUtilityService) {
        this(factory.create(), invoker, fileSearchImpl, managementUtilityService);
    }

    public HandleFileIsChanged(GraphServiceClientProvider provider, Invoker invoker, FileSearchImpl fileSearchImpl,
                               ManagementUtilityService managementUtilityService) {
        this.provider = provider;
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.fileSearchImpl = fileSearchImpl;
        this.managementUtilityService = managementUtilityService;
    }

    public DriveItemDeltaCollectionPage createFileChangeResource(String siteName) {
        try {
            // Get Delta Through Group
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
            Group group = FilePermissionDaoUtil.findMatchingGroup(graphClient, siteName);

            return graphClient.groups(group.id).drive().root().delta().buildRequest().get();
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            throw new RuntimeException("Error Occurred During Subscription");
        }
    }


    public Map<String, Object> getChangeFileEvent(String source, String search) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {
                String siteName = getSiteName(source);
                String domainName = fileSearchImpl.getDomainName();
                String siteDomainPath = fileSearchImpl.getDomainPath(domainName, true);
                source = fileSearchImpl.handleAbsolutePath(source, siteDomainPath);

                if (isFileDeleted(source, search, payload)) {
                    FileChangeResourceUtil.addFileMetaData(routingUrl, createFileChangeResource(siteName));
                    return payload;
                }
                DriveItem driveItem = getDriveItemThroughAbsolutePath(source);
                if (driveItem != null && driveItem.folder != null) {
                    processFolder(driveItem, payload, search);
                    FileChangeResourceUtil.addFileMetaData(routingUrl, createFileChangeResource(siteName));
                    return payload;
                } else {
                    processFile(driveItem, payload, search);
                    FileChangeResourceUtil.addFileMetaData(routingUrl, createFileChangeResource(siteName));
                    return payload;
                }
            } else {
                ResponseUtil.successResponse(false, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            ResponseUtil.successResponse(false, Constants.FILE_NOT_FOUND, payload);
        }
        return payload;
    }

    private boolean isFileDeleted(String source, String search, Map<String, Object> payload) {
        if (FileChangeResourceUtil.getFileChangeResource(routingUrl) != null) {
            DriveItemDeltaCollectionPage page = FileChangeResourceUtil.getFileChangeResource(routingUrl);
            String[] stream = source.split("/");
            //Check if Source Exist In delta
            while (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem oldDrive : page.getCurrentPage()) {
                    if (oldDrive.parentReference != null && oldDrive.parentReference.path != null
                            && oldDrive.parentReference.path.equals(FileUtil.getParentPath(source))
                            && oldDrive.name != null
                            && oldDrive.name.equals(stream[stream.length - 1])) {
                        if (search == null) {
                            return fetchDeletedFiles(source, payload, oldDrive);
                        } else if (!search.isBlank() && oldDrive.name.contains(search)) {
                            return fetchDeletedFiles(source, payload, oldDrive);
                        }
                    }
                }
                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.successResponse(true, "Files Content", payload);
                    break;
                }
            }
        }
        return false;
    }

    private boolean fetchDeletedFiles(String source, Map<String, Object> payload, DriveItem oldDrive) {
        if (oldDrive.folder != null) { // if it is Folder
            if (checkIFFileDeleted(source)) {// Then Check it Exits
                app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(oldDrive, null, null, null));
                payload.put(Constants.FILE, fileEntity);
                payload.put(Constants.EVENT, Constants.DELETED);
                return true;
            } else {
                return iterateFilesForDelete(source, payload);
            }
        } else {
            if (checkIFFileDeleted(source)) {  ///
                app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(oldDrive, null, null, null));
                payload.put(Constants.FILE, fileEntity);
                payload.put(Constants.EVENT, Constants.DELETED);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean iterateFilesForDelete(String source, Map<String, Object> payload) {
        if (checkIfFileIDExistInDeltaCollection(source, payload)) {
            return true;
        }
        return false;
    }

    private boolean checkIfFileIDExistInDeltaCollection(String source, Map<String, Object> payload) {
        DriveItemDeltaCollectionPage page = FileChangeResourceUtil.getFileChangeResource(routingUrl);
        while (page != null && !page.getCurrentPage().isEmpty()) {
            for (DriveItem oldDrive : page.getCurrentPage()) {
                if (oldDrive.parentReference != null && oldDrive.parentReference.path != null
                        && oldDrive.parentReference.path.equals(source)
                        && checkIFFileDeleted(source + "/" + oldDrive.name)) {
                    app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(oldDrive, null, null, null));
                    payload.put(Constants.FILE, fileEntity);
                    payload.put(Constants.EVENT, Constants.DELETED);
                    return true;
                }
            }
            if (page.getNextPage() != null) {
                page = page.getNextPage().buildRequest().get();
            } else {
                break;
            }
        }
        return false;
    }

    private boolean checkIFFileDeleted(String source) {
        try {
            managementUtilityService.getRootFolderByAbsolutePath(source);
            return false;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            if (Objects.requireNonNull(cause.getMessage()).contains(Constants.ITEM_NOT_FOUND)) {
                return true;
            }
        }
        return false;
    }

    private void processFile(DriveItem driveItem, Map<String, Object> payload, String search) {
        if (FileChangeResourceUtil.getFileChangeResource(routingUrl) != null) {
            DriveItemDeltaCollectionPage page = FileChangeResourceUtil.getFileChangeResource(routingUrl);
            while (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem oldDrive : page.getCurrentPage()) {
                    processFileWithOldFile(oldDrive, driveItem, search, payload);
                }
                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.successResponse(true, "Files Content", payload);
                    break;
                }
            }
        }
    }

    private void processFileWithOldFile(DriveItem oldDriveItem, DriveItem driveItem, String
            search, Map<String, Object> payload) {
        if (oldDriveItem.id != null && driveItem.id != null && driveItem.id.equals(oldDriveItem.id)) {
            if (search == null) {
                if (isFileUpdated(oldDriveItem, driveItem)) {
                    addUpdatedFile(driveItem, payload);
                }
            } else if (search != null && !search.isEmpty() && driveItem.name != null && driveItem.name.equalsIgnoreCase(search)) {
                if (isFileUpdated(oldDriveItem, driveItem)) {
                    addUpdatedFile(driveItem, payload);
                }
            }
        }
    }

    private static void addUpdatedFile(DriveItem driveItem, Map<String, Object> payload) {
        app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(driveItem, null, null, null));
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.EVENT, Constants.UPDATED);
    }

    private static void addCreatedFile(DriveItem driveItem, Map<String, Object> payload) {
        app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(driveItem, null, null, null));
        payload.put(Constants.FILE, fileEntity);
        payload.put(Constants.EVENT, Constants.CREATED);
    }

    private boolean isFileUpdated(DriveItem oldDriveItem, DriveItem driveItem) {
        return !(oldDriveItem.name != null && oldDriveItem.name.equals(driveItem.name)) ||
                !(oldDriveItem.lastModifiedBy != null && oldDriveItem.lastModifiedBy.user != null
                        && oldDriveItem.lastModifiedBy.user.displayName != null
                        && driveItem.lastModifiedBy != null
                        && driveItem.lastModifiedBy.user != null
                        && oldDriveItem.lastModifiedBy.user.displayName.equals(driveItem.lastModifiedBy.user.displayName)) ||
                !(oldDriveItem.lastModifiedDateTime != null && oldDriveItem.lastModifiedDateTime.equals(driveItem.lastModifiedDateTime));
    }

    private void processFolder(DriveItem driveItem, Map<String, Object> payload, String search) {
        try {
            assert driveItem.id != null;
            DriveItemCollectionPage collectionPage = getUserRequestBuilder().drive().items(driveItem.id).children().buildRequest().get();
            while (collectionPage != null && !collectionPage.getCurrentPage().isEmpty()) {
                for (DriveItem folderItem : collectionPage.getCurrentPage()) {
                    if (folderItem.parentReference != null && isFileDeleted(folderItem.parentReference.path + "/" + folderItem.name, search, payload)) {
                        break;
                    } else if (checkIfFileIsCreated(folderItem.parentReference.path, search, payload, folderItem)) {
                        return;
                    } else {
                        processFile(folderItem, payload, search);
                    }
                }
                if (collectionPage.getNextPage() != null) {
                    collectionPage = collectionPage.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.successResponse(true, "Files Content", payload);
                    break;
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            ResponseUtil.successResponse(false, Constants.FILE_NOT_FOUND, payload);
        }
    }

    private boolean checkIfFileIsCreated(String source, String
            search, Map<String, Object> payload, DriveItem
                                                 folderItem) {
        if (FileChangeResourceUtil.getFileChangeResource(routingUrl) != null) {
            DriveItemDeltaCollectionPage page = FileChangeResourceUtil.getFileChangeResource(routingUrl);
            while (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem oldDrive : page.getCurrentPage()) {
                    if (search != null && !search.isBlank() && oldDrive.parentReference != null && oldDrive.parentReference.path != null
                            && oldDrive.parentReference.path.equals(source)
                            && oldDrive.id != null && oldDrive.id.equals(folderItem.id)) {
                        return false;
                    } else if (search == null && oldDrive.parentReference != null && oldDrive.parentReference.path != null
                            && oldDrive.parentReference.path.equals(source)
                            && oldDrive.id != null && oldDrive.id.equals(folderItem.id)) {
                        return false;
                    }
                }
                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.successResponse(true, "Files Content", payload);
                    break;
                }
            }
            addCreatedFile(folderItem, payload);
        }
        addCreatedFile(folderItem, payload);
        return true;
    }

    private UserRequestBuilder getUserRequestBuilder() {
        return provider.getUserRequestBuilder(null, null).me();
    }


    public DriveItem getDriveItemThroughAbsolutePath(String source) {
        DriveItem driveItem = null;
        if (source == null || source.isBlank()) {
            return driveItem;
        }
        GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
        driveItem = managementUtilityService.getRootFolderByAbsolutePath(source);
        return driveItem;
    }

    public String getSiteName(String path) {
        String siteName = null;
        int endIndex = path.indexOf("/", 1); // Find the index of the second "/"
        if (endIndex > 0) {
            siteName = path.substring(1, endIndex); // Extract the segment between "/"
        }
        return siteName;
    }

}
