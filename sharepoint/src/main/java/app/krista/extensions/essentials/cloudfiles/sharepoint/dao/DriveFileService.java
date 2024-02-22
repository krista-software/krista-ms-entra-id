package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for interacting with Microsoft Graph API to retrieve information
 * about SharePoint Drive items. Provides methods to get file information,
 * check if a file exists, and list files in a specified path.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class DriveFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveFileService.class);
    private final GraphServiceClientProvider provider;
    private final ManagementUtilityService managementUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;


    @Inject
    public DriveFileService(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService,
                            FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
    }

    public static String getRelativePath(String fullUrl) {
        try {
            String escapedUrl = fullUrl.replace(" ", "%20");  // Escape spaces
            URI uri = new URI(escapedUrl);
            String path = uri.getPath();

            String[] pathSegments = path.split(java.io.File.separator.equals("\\") ? "\\\\" : java.io.File.separator);

            // Find the index of the last "Documents" segment, regardless of case:
            int startIndex = -1;
            for (int i = pathSegments.length - 1; i >= 0; i--) {
                if (pathSegments[i].equalsIgnoreCase(Constants.DOCUMENTS) ||
                        pathSegments[i].equalsIgnoreCase(Constants.SHARED_DOCUMENTS)) {
                    startIndex = i;
                    break;
                }
            }

            if (startIndex != -1) {
                startIndex++;  // Start after "Documents"
                StringBuilder dynamicPath = new StringBuilder(java.io.File.separator);
                for (int i = startIndex; i < pathSegments.length; i++) {
                    dynamicPath.append(pathSegments[i]);
                    if (i < pathSegments.length - 1) {
                        dynamicPath.append(File.separator);
                    }
                }
                return dynamicPath.toString();
            } else {
                return null;  // Handle cases where "Documents" is not found
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Retrieves file information for the specified path, handling path validation, file existence checks, and potential errors.
     *
     * @return A FreeForm object containing file details or error messages.
     */
    public FreeForm getFileInfo(String sourcePath, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        try {
            if (sourcePath.trim().isEmpty()) { // Check for null or empty string
                handleInvalidPathError(Constants.INVALID_FOLDER_PATH, payload);
                return payload; // Exit the method early
            }
            sourcePath = managementUtilityService.getUpdatedFilePath(sourcePath);
            DriveItem driveItem = checkIfFileExist(sourcePath, payload, useEmail, accountID);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                fetchFileInfo(sourcePath, payload, useEmail, accountID, driveItem);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(cause.getMessage());
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
            if (shouldSkipFileCheck(sourcePath, payload)) return new DriveItem();

            DriveItem driveItem = getFileByAbsolutePath(sourcePath, useEmail, accountID);
            if (isInvalidDriveItem(driveItem, payload)) return new DriveItem();

            sourcePath = getRelativePath(sourcePath);
            DriveItem driveItemCollectionPage = getDriveItemCollectionPage(driveItem, sourcePath, useEmail, accountID);

            if (isFolderValid(driveItemCollectionPage)) {
                ResponseUtil.addFileSize(driveItemCollectionPage.size != null ? driveItemCollectionPage.size : 0L, payload);
                ResponseUtil.getInfoResponse(true, Constants.FILE_FOUND, payload);
            } else {
                ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
            }
            return driveItem;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(cause.getMessage(), cause);
            handleException(cause, payload);
            return new DriveItem();
        }
    }

    private boolean shouldSkipFileCheck(String sourcePath, FreeForm payload) {
        if (sourcePath == null) {
            ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
            return true;
        }
        return false;
    }

    private boolean isInvalidDriveItem(DriveItem driveItem, FreeForm payload) {
        if (driveItem == null || driveItem.parentReference == null || driveItem.parentReference.driveId == null) {
            ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
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
        ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : cause.getMessage(), payload);
    }

    private boolean isFolderValid(DriveItem driveItemCollectionPage) {
        return driveItemCollectionPage != null && driveItemCollectionPage.folder != null;
    }

    private DriveItem getFileByAbsolutePath(String filePath, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with filePath : {} ", filePath);
        if (filePath == null) {
            return null;
        }
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);
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

    private SearchEntityQueryCollectionPage executeSearchQuery(GraphServiceClient<Request> graphClient,
                                                               LinkedList<SearchRequest> requestsList) {
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
            sourcePath = getRelativePath(sourcePath);
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
            LOGGER.error(cause.getMessage());
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

    /**
     * Retrieves a list of files at the specified path, optionally filtered by search criteria, handling path validation, file existence checks, and potential errors.
     *
     * @return A FreeForm object containing the list of files or error messages.
     */
    @NotNull
    public FreeForm getListOfFiles(String relativePath, String search, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        try {
            List<FileService> files = new ArrayList<>();
            if (relativePath.trim().isEmpty()) {
                handleInvalidPathError(Constants.INVALID_FOLDER_PATH, payload);
                return payload;
            }
            relativePath = managementUtilityService.getUpdatedFilePath(relativePath);
            DriveItem driveItem = checkIfFileExistAndGetParentDriveId(relativePath, payload, useEmail, accountID);
            if (payload.get(Constants.SUCCESS) == Boolean.TRUE && driveItem != null && driveItem.parentReference != null) {
                String refinedRelativePath = getRelativePath(relativePath);
                getRootFilesByDriveIDPath(driveItem.parentReference.driveId, refinedRelativePath,
                        files, useEmail, accountID, search, payload, relativePath);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(cause.getMessage());
            handleServiceException(cause, payload);
        }
        return payload;
    }

    private void handleInvalidPathError(String message, FreeForm payload) {
        ResponseUtil.getInfoResponse(false, message, payload);
    }

    private DriveItem checkIfFileExistAndGetParentDriveId(String relativePath, FreeForm payload, boolean useEmail, String accountID) {
        DriveItem driveItem = checkIfFileExist(relativePath, payload, useEmail, accountID);
        if (payload.get(Constants.SUCCESS) == Boolean.TRUE) {
            return driveItem;
        } else {
            handleInvalidPathError(Constants.INVALID_FOLDER_PATH, payload);
            return null;
        }
    }

    private void handleServiceException(Exception cause, FreeForm payload) {
        String errorMessage = Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage();
        ResponseUtil.getInfoResponse(false, errorMessage, payload);
    }

    public void getRootFilesByDriveIDPath(String driveID, String relativePath, List<FileService> files, boolean useEmail, String accountID, String search, FreeForm payload, String absolutePath) {
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);

            DriveItem driveItem = graphClient.drives(driveID).root().itemWithPath(relativePath).buildRequest().get();

            if (driveItem != null && driveItem.id != null && driveItem.folder != null) {
                iterateAndAddFilesToCollection(files, search, driveID, relativePath, useEmail, accountID, payload, absolutePath);
            } else {
                files.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService));
                ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
    }

    private void iterateAndAddFilesToCollection(List<FileService> files, String search, String driveID, String relativePath, boolean useEmail, String accountID, FreeForm payload, String absolutePath) {
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);

            DriveItemCollectionPage page = graphClient.drives(driveID).root().itemWithPath(relativePath).children()
                    .buildRequest().get();

            while (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem driveInPage : page.getCurrentPage()) {
                    if (isSearchCriteriaMatched(search, driveInPage)) {
                        files.add(new FileImpl(driveInPage, graphClient, absolutePath + "/" + driveInPage.name, absolutePath, fileSearchUtilityService));
                    }
                }

                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
                    break;
                }
            }
        } catch (Exception cause) {
            LOGGER.error(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
    }

    private boolean isSearchCriteriaMatched(String search, DriveItem driveItem) {
        return search != null && !search.isEmpty() && driveItem.name != null && driveItem.name.contains(search)
                || search != null && search.isBlank();
    }

}
