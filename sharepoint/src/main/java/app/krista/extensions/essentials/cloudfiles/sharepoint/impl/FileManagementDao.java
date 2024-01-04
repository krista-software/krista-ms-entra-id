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

/**
 *
 * @author Vaibhav Choudhary
 *
 */

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
            if (sourcePath.equalsIgnoreCase(null) || sourcePath.trim().isEmpty()) { // Check for null or empty string
                handleInvalidPathError(Constants.INVALID_FOLDER_PATH, payload);
                return payload; // Exit the method early
            }
            DriveItem driveItem = checkIfFileExist(sourcePath, payload, useEmail, accountID);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                fetchFileInfo(sourcePath, payload, useEmail, accountID, driveItem);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleErrorResponse("An error occurred. Please provide a valid path.", payload);
        }
        return payload;
    }

    private void handleErrorResponse(String errorMessage, FreeForm payload) {
        payload.put(Constants.MESSAGE, errorMessage);
        payload.put(Constants.SUCCESS, false);
        LOGGER.error("Error during processing: {}", errorMessage);
    }

    private DriveItem checkIfFileExist(String sourcePath, FreeForm payload, boolean useEmail, String accountID) {
        try {
            if (shouldSkipFileCheck(sourcePath, payload, useEmail, accountID)) return new DriveItem();

            DriveItem driveItem = getFileByAbsolutePath(sourcePath);
            if (isInvalidDriveItem(driveItem,payload)) return new DriveItem();

            sourcePath = DriveUtil.getRelativePath(sourcePath);
            DriveItem driveItemCollectionPage = getDriveItemCollectionPage(driveItem, sourcePath, useEmail, accountID);

            if (isFolderValid(driveItemCollectionPage)) {
                ResponseUtil.addFileSize(driveItem.size != null ? driveItem.size : 0L, payload);
                ResponseUtil.getInfoResponse(true, "File Found", payload);
            } else {
                ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
            }

            return driveItem;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleException(cause, payload);
            return new DriveItem();
        }
    }

    private boolean shouldSkipFileCheck(String sourcePath, FreeForm payload, boolean useEmail, String accountID) {
        if (sourcePath == null) {
            ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
            return true;
        }
        return false;
    }

    private boolean isInvalidDriveItem(DriveItem driveItem, FreeForm payload) {
        if (driveItem == null || driveItem.parentReference == null || driveItem.parentReference.driveId == null) {
            ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
            return true;
        }
        return false;
    }

    private DriveItem getDriveItemCollectionPage(DriveItem driveItem, String sourcePath, boolean useEmail, String accountID) {
        return provider.getUserRequestBuilder(useEmail, accountID)
                .drives(driveItem.parentReference.driveId)
                .root()
                .itemWithPath(sourcePath)
                .buildRequest()
                .get();
    }

    private void handleException(Exception cause, FreeForm payload) {
        ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
    }

    private boolean isFolderValid(DriveItem driveItemCollectionPage) {
        return driveItemCollectionPage != null && driveItemCollectionPage.folder != null;
    }

    private DriveItem getFileByAbsolutePath(String filePath) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", filePath);

        if (filePath == null) {
            return null;
        }

        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(true, null);
            LinkedList<SearchRequest> requestsList = buildSearchRequests(filePath);
            SearchEntityQueryCollectionPage page = executeSearchQuery(graphClient, requestsList);

            return extractDriveItemFromSearchPage(page);

        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleSearchError(cause);
            return null;
        }
    }

    private LinkedList<SearchRequest> buildSearchRequests(String filePath) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();

        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.LIST_ITEM,
                EntityType.DRIVE, EntityType.DRIVE_ITEM);

        LinkedList<SortProperty> sortProperties = buildSortProperties();
        requests.sortProperties = sortProperties;

        SearchQuery query = buildSearchQuery(filePath);
        requests.query = query;
        requests.from = 0;
        requests.size = 1;

        requestsList.add(requests);
        return requestsList;
    }

    private LinkedList<SortProperty> buildSortProperties() {
        LinkedList<SortProperty> sortProperties = new LinkedList<>();

        SortProperty sort = new SortProperty();
        sort.name = "LastModifiedTime";
        sort.isDescending = false;

        sortProperties.add(sort);
        return sortProperties;
    }

    private SearchQuery buildSearchQuery(String filePath) {
        SearchQuery query = new SearchQuery();
        query.queryString = "path:\"" + filePath + "\"";
        return query;
    }

    private SearchEntityQueryCollectionPage executeSearchQuery(GraphServiceClient<Request> graphClient, LinkedList<SearchRequest> requestsList) {
        return graphClient.search().query(SearchEntityQueryParameterSet
                .newBuilder().withRequests(requestsList).build()).buildRequest().post();
    }

    private DriveItem extractDriveItemFromSearchPage(SearchEntityQueryCollectionPage page) {
        if (page != null && !page.getCurrentPage().isEmpty()) {
            for (SearchResponse driveInPage : page.getCurrentPage()) {
                if (driveInPage.hitsContainers != null) {
                    for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                        if (hitContainer.hits != null) {
                            for (SearchHit hits : hitContainer.hits) {
                                if (hits != null && hits.resource instanceof DriveItem) {
                                    return (DriveItem) hits.resource;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void handleSearchError(Exception cause) {
        LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
        throw new IllegalArgumentException(cause.getMessage(), cause);
    }

    private void fetchFileInfo(String sourcePath, FreeForm payload, boolean useEmail, String accountID, DriveItem driveItem) {
        try {
            sourcePath = DriveUtil.getRelativePath(sourcePath);
            DriveItemCollectionPage page = provider.getUserRequestBuilder(useEmail, accountID)
                    .drives(driveItem.parentReference.driveId)
                    .root()
                    .itemWithPath(sourcePath)
                    .children()
                    .buildRequest()
                    .get();

            if (page != null && !page.getCurrentPage().isEmpty()) {
                processResponse(page, payload);
            } else {
                payload.put(Constants.MESSAGE, "No information available for the provided path.");
                payload.put(Constants.SUCCESS, false);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleErrorResponse("An error occurred while fetching file information.", payload);
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
            if (relativePath.equalsIgnoreCase(null) || relativePath.trim().isEmpty()) {
                handleInvalidPathError(Constants.INVALID_FOLDER_PATH, payload);
                return payload;
            }

            DriveItem driveItem = checkIfFileExistAndGetParentDriveId(relativePath, payload, useEmail, accountID);
            if (payload.get(Constants.SUCCESS) == Boolean.TRUE) {
                String refinedRelativePath = DriveUtil.getRelativePath(relativePath);
                getRootFilesByDriveIDPath(driveItem.parentReference.driveId, refinedRelativePath, files, useEmail, accountID, search, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException e) {
            handleServiceException(e, payload);
        }
        return payload;
    }

    private void handleInvalidPathError(String message, FreeForm payload) {
        ResponseUtil.getInfoResponse(false, message, payload);
    }

    private DriveItem checkIfFileExistAndGetParentDriveId(String relativePath, FreeForm payload, boolean useEmail, String accountID) {
        DriveItem driveItem = checkIfFileExist(relativePath, payload, useEmail, accountID);
        if (payload.get(Constants.SUCCESS) == Boolean.TRUE) { // Explicit boolean check
            return driveItem;
        } else {
            handleInvalidPathError("Invalid Folder Path", payload);
            return null; // Directly return null to indicate failure
        }
    }

    private void handleServiceException(Exception cause, FreeForm payload) {
        String errorMessage = Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage();
        ResponseUtil.getInfoResponse(false, errorMessage, payload);
    }

    public void getRootFilesByDriveIDPath(String driveID, String relativePath, List<FileService> files, boolean useEmail, String accountID, String search, FreeForm payload) {
        try {
            DriveItem driveItem = provider.getUserRequestBuilder(useEmail, accountID)
                    .drives(driveID)
                    .root()
                    .itemWithPath(relativePath)
                    .buildRequest()
                    .get();

            if (driveItem != null && driveItem.id != null && driveItem.folder != null) {
                iterateAndAddFilesToCollection(files, search, driveID, relativePath, useEmail, accountID, payload);
            } else {
                files.add(new DrivePermissionImpl(driveItem, provider));
                ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
    }

    private void iterateAndAddFilesToCollection(List<FileService> files, String search, String driveID, String relativePath, boolean useEmail, String accountID, FreeForm payload) {
        DriveItemCollectionPage page = provider.getUserRequestBuilder(useEmail, accountID)
                .drives(driveID)
                .root()
                .itemWithPath(relativePath)
                .children()
                .buildRequest()
                .get();

        while (page != null && !page.getCurrentPage().isEmpty()) {
            for (DriveItem driveInPage : page.getCurrentPage()) {
                if (isSearchCriteriaMatched(search, driveInPage)) {
                    files.add(new DrivePermissionImpl(driveInPage, provider));
                }
            }

            if (page.getNextPage() != null) {
                page = page.getNextPage().buildRequest().get();
            } else {
                ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
                break;
            }
        }
    }

    private boolean isSearchCriteriaMatched(String search, DriveItem driveItem) {
        return search != null && !search.isEmpty() && driveItem.name != null && driveItem.name.contains(search)
                || search != null && search.isBlank();
    }


}
