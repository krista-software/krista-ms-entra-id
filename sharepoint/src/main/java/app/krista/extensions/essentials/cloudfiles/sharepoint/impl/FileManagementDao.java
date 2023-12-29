package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveSharedWithMeCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileManagementDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileManagementDao.class);

    private final GraphServiceClientProvider provider;

    private final SharePointAttributes sharePointAttributes;

    @Inject
    public FileManagementDao(GraphServiceClientProviderFactory factory, SharePointAttributes sharePointAttributes) {
        this(factory.create(), sharePointAttributes);
    }

    public FileManagementDao(GraphServiceClientProvider provider, SharePointAttributes sharePointAttributes) {
        this.provider = provider;
        this.sharePointAttributes = sharePointAttributes;
    }

    public FreeForm getFileInfo(String sourcePath, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        try {
            DriveItem driveItem = checkIfFileExist(sourcePath, payload, useEmail, accountID);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                sourcePath = DriveUtil.getRelativePath(sourcePath);
                fetchFileInfo(sourcePath, payload, useEmail, accountID, driveItem);
            } else {
                payload.put(Constants.MESSAGE, "Please Provide valid path.");
                payload.put(Constants.SUCCESS, false);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            payload.put(Constants.MESSAGE, "An error occurred. Please Provide valid path.");
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    private DriveItem checkIfFileExist(String sourcePath, FreeForm payload, boolean useEmail, String accountID) {
        DriveItem driveItem = new DriveItem();
        try {
            if (sourcePath != null) {
                driveItem = getFileByAbsolutePath(sourcePath);
                if (driveItem != null && driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
                    sourcePath = DriveUtil.getRelativePath(sourcePath);
                    DriveItem driveItemCollectionPage = provider.getUserRequestBuilder(useEmail, accountID).drives(driveItem.parentReference.driveId).root().itemWithPath(sourcePath).buildRequest().get();
                    if(driveItemCollectionPage != null && driveItemCollectionPage.folder != null){
                        ResponseUtil.addFileSize(driveItem.size != null ? driveItem.size : 0L, payload);
                        ResponseUtil.getInfoResponse(true, "File Found", payload);
                    } else {
                        ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                    }
                } else {
                    ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                }
            } else {
                ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
            return new DriveItem();
        }
        return driveItem;
    }

    private DriveItem getFileByAbsolutePath(String filePath) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", filePath);
        if (filePath != null) {
            filePath = filePath.replace("%20", " ");
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(true, null);

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
                                            driveItem = (DriveItem) hits.resource;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (driveItem != null) {
                    return driveItem;
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

    private void fetchFileInfo(String sourcePath, FreeForm payload, boolean useEmail, String accountID, DriveItem driveItem) {
        try {
            if (driveItem != null && driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
                DriveItemCollectionPage driveItemCollectionPage = provider.getUserRequestBuilder(useEmail, accountID).drives(driveItem.parentReference.driveId).root().itemWithPath(sourcePath).children().buildRequest().get();
                if (driveItemCollectionPage != null) {
                    processResponse(driveItemCollectionPage, payload);
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
        }
    }

    private void processResponse(DriveItemCollectionPage page, FreeForm payload) {
        try {
            int filesCount = 0;
            int folderCount = 0;
            long oldestFileCreatedate = 0L;
            long newestFileCreateDate = 0L;
            long lastModifiedFileCreateDate = 0L;
            String oldestFile = null;
            String newestFile = null;
            String lastModifiedFileName = null;
            String lastModifiedFileId = null;

            while (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem driveInPage : page.getCurrentPage()) {
                    if (driveInPage.folder != null) {
                        folderCount++;
                    }
                    filesCount++;
                    newestFileCreateDate = Math.max(newestFileCreateDate, getFileDate(driveInPage.createdDateTime));
                    if (oldestFileCreatedate == 0L) {
                        oldestFileCreatedate = getFileDate(driveInPage.createdDateTime);
                    }
                    oldestFileCreatedate = Math.min(oldestFileCreatedate, getFileDate(driveInPage.createdDateTime));
                    lastModifiedFileCreateDate = Math.max(lastModifiedFileCreateDate, getFileDate(driveInPage.lastModifiedDateTime));
                }
                String oldFile = getFileNameByDate(page, oldestFileCreatedate);
                oldestFile = oldFile == null && oldestFile != null ? oldestFile : oldFile;
                String newFile = getFileNameByDate(page, newestFileCreateDate);
                newestFile = newFile == null && newestFile != null ? newestFile : newFile;
                String lastFileName = getFileNameAndIDByLastModified(page, lastModifiedFileCreateDate).get(0);
                lastModifiedFileName = lastFileName.isBlank() && lastModifiedFileName != null ? lastModifiedFileName : lastFileName;
                String lastFileID = getFileNameAndIDByLastModified(page, lastModifiedFileCreateDate).get(1);
                lastModifiedFileId = lastFileID.isBlank() && lastModifiedFileId != null ? lastModifiedFileId : lastFileID;

                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.getInfoResponse(filesCount, folderCount, (long) payload.get("Size"), oldestFile, oldestFileCreatedate, newestFile, newestFileCreateDate, lastModifiedFileId, lastModifiedFileName, lastModifiedFileCreateDate, true, "Get Info Data", payload);
                    break;
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    private long getFileDate(OffsetDateTime dateTime) {
        return dateTime != null ? Timestamp.valueOf(dateTime.toLocalDateTime()).getTime() : 0L;
    }

    private String getFileNameByDate(DriveItemCollectionPage page, long fileCreateDate) {
        if (page != null && !page.getCurrentPage().isEmpty()) {
            for (DriveItem driveInPage : page.getCurrentPage()) {
                if (getFileDate(driveInPage.createdDateTime) == fileCreateDate) {
                    return driveInPage.name;
                }
            }
        }
        return null;
    }

    private List<String> getFileNameAndIDByLastModified(DriveItemCollectionPage page, long lastModifiedFileCreateDate) {
        if (page != null && !page.getCurrentPage().isEmpty()) {
            for (DriveItem driveInPage : page.getCurrentPage()) {
                if (getFileDate(driveInPage.lastModifiedDateTime) == lastModifiedFileCreateDate) {
                    return List.of(driveInPage.name != null ? driveInPage.name : "", driveInPage.id != null ? driveInPage.id : "");
                }
            }
        }
        return List.of("", "");
    }

    @NotNull
    public FreeForm getListOfFiles(String relativePath, String search, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        try {
            List<FileService> files = new ArrayList<>();
            if (relativePath != null) {
                DriveItem driveItem = checkIfFileExist(relativePath, payload, useEmail, accountID);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    relativePath = DriveUtil.getRelativePath(relativePath);
                    getRootFilesByDriveIDPath(driveItem.parentReference.driveId, relativePath, files, useEmail, accountID, search, payload);
                    return payload;
                } else {
                    ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                    return payload;
                }
            } else {
                ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                return payload;
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
        return payload;
    }

    public void getRootFilesByDriveIDPath(String driveID, String relativePath, List<FileService> files, boolean useEmail, String accountID, String search, FreeForm payload) {
        try {
            DriveItem driveItem = provider.getUserRequestBuilder(useEmail, accountID).drives(driveID).root().itemWithPath(relativePath).buildRequest().get();
            if (driveItem != null && driveItem.id != null && driveItem.folder != null) {
                DriveItemCollectionPage page = provider.getUserRequestBuilder(useEmail, accountID).drives(driveID).root().itemWithPath(relativePath).children().buildRequest().get();
                while (page != null && !page.getCurrentPage().isEmpty()) {
                    for (DriveItem driveInPage : page.getCurrentPage()) {
                        if (search != null && !search.isEmpty() && driveInPage.name != null && driveInPage.name.contains(search)) {
                            files.add(new FileImpl(driveInPage, null, null));
                        }
                        if (search != null && search.isBlank()) {
                            files.add(new FileImpl(driveInPage, null, null));
                        }
                    }
                    if (page.getNextPage() != null) {
                        page = page.getNextPage().buildRequest().get();
                    } else {
                        ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
                        break;
                    }
                }
            } else {
                files.add(new FileImpl(driveItem, null, null));
                ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
    }

}
