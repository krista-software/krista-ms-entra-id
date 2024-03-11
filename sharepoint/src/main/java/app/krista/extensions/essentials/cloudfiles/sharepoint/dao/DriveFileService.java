package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
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

    private static final String EMPTY_FOLDER = "The provided folder is empty";
    private final GraphServiceClientProvider provider;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;


    @Inject
    public DriveFileService(GraphServiceClientProviderFactory factory,
                            FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Retrieves file information for the specified path, handling path validation, file existence checks, and potential errors.
     *
     * @return A FreeForm object containing file details or error messages.
     */
    public FreeForm getFileInfo(String sourcePath, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        Map<String, Object> mapPayload = new HashMap<>();
        try {
            if (sourcePath == null || sourcePath.trim().isEmpty()) {
                handleInvalidPathError(payload);
                return payload; // Exit the method early
            }
            searchFileExecutor.getFileInfo(sourcePath, mapPayload, true, useEmail, accountID);
            if (mapPayload.get(Constants.SUCCESS).equals(Boolean.TRUE) && mapPayload.get(Constants.IS_SOURCE_FILE_EXIST).equals(Boolean.TRUE)) {
                DriveItem driveItem = (DriveItem) mapPayload.get(Constants.SOURCE_DRIVE_ITEM);
                payload.put(Constants.SIZE, Constants.NUMBER, driveItem.size != null ? driveItem.size : 0);

                if(driveItem.size != 0){
                    fetchFileInfo(sourcePath, payload, useEmail, accountID, driveItem);
                }
                else{
                  handleEmptyFolder(payload,mapPayload);
                }
            }else{
                payload.put(Constants.SUCCESS,Constants.SWITCH,false);
                payload.put(Constants.MESSAGE,Constants.TEXT,mapPayload.getOrDefault(Constants.MESSAGE,Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH));
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleErrorResponse("An error occurred. Please provide a valid path.", payload);
        }
        return payload;
    }

    private void handleEmptyFolder(FreeForm payload, Map<String, Object> mapPayload) {
        payload.put(Constants.SUCCESS, Constants.SWITCH, true);
        payload.put(Constants.MESSAGE, Constants.TEXT, mapPayload.getOrDefault(Constants.MESSAGE, EMPTY_FOLDER));
    }

    private void handleErrorResponse(String errorMessage, FreeForm payload) {
        payload.put(Constants.MESSAGE, errorMessage);
        payload.put(Constants.SUCCESS, false);
        LOGGER.error("Error during processing: {}", errorMessage);
    }

    private DriveItemCollectionPage driveItemCollectionPageProvider(String sourcePath, boolean useEmail, String accountID, DriveItem driveItem) {
        return provider.getUserRequestBuilder(useEmail, accountID)
                .drives(driveItem.parentReference.driveId)
                .root()
                .itemWithPath(sourcePath)
                .children()
                .buildRequest()
                .get();
    }


    private void fetchFileInfo(String sourcePath, FreeForm payload, boolean useEmail, String accountID, DriveItem driveItem) {
        try {
            sourcePath = FolderManagementDao.getRelativePath(sourcePath);
            DriveItemCollectionPage page = driveItemCollectionPageProvider(sourcePath, useEmail, accountID, driveItem);

            if (page != null && !page.getCurrentPage().isEmpty()) {
                processResponse(page, payload);
            } else {
                payload.put(Constants.MESSAGE, "No information available for the provided path.");
                payload.put(Constants.SUCCESS, false);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
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
                    if (driveInPage.file != null) {
                        filesCount++;
                    }
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
    public FreeForm getListOfFiles(String sourcePath, String search, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        Map<String, Object> mapPayload = new HashMap<>();
        try {
            List<FileService> files = new ArrayList<>();
            if (isNullOrEmpty(sourcePath)) {
                handleInvalidPathError(payload);
                return payload;
            }
            searchFileExecutor.getFileInfo(sourcePath, mapPayload, true, useEmail, accountID);
            if (mapPayload.get(Constants.SUCCESS).equals(Boolean.TRUE) && mapPayload.get(Constants.IS_SOURCE_FILE_EXIST).equals(Boolean.TRUE)) {
                DriveItem driveItem = (DriveItem) mapPayload.get(Constants.SOURCE_DRIVE_ITEM);
                String refinedRelativePath = FolderManagementDao.getRelativePath(sourcePath);
                String sitePath = mapPayload.get(Constants.SOURCE_ROOT_PATH).toString();
                getRootFilesByDriveIDPath(driveItem.parentReference.driveId, refinedRelativePath,
                        files, useEmail, accountID, search, payload, sourcePath,sitePath);
            }else{
                payload.put(Constants.SUCCESS,Constants.SWITCH,false);
                payload.put(Constants.MESSAGE,Constants.TEXT,mapPayload.getOrDefault(Constants.MESSAGE,Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH));
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(cause.getMessage());
            handleServiceException(cause, payload);
        }
        return payload;
    }

    private boolean isNullOrEmpty(String relativePath) {
        return relativePath == null || relativePath.trim().isEmpty();
    }

    private void handleInvalidPathError(FreeForm payload) {
        ResponseUtil.getInfoResponse(false, Constants.INVALID_FOLDER_PATH, payload);
    }

    private void handleServiceException(Exception cause, FreeForm payload) {
        LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
        String errorMessage = Objects.requireNonNull(cause.getMessage()).contains(Constants.ITEM_NOT_FOUND) ? Constants.FILE_NOT_FOUND : Constants.PLEASE_PROVIDE_A_VALID_PATH_THE_FILE_FOLDER_WAS_NOT_FOUND;
        ResponseUtil.getInfoResponse(false, errorMessage, payload);
    }

    public void getRootFilesByDriveIDPath(String driveID, String relativePath, List<FileService> files, boolean useEmail, String accountID, String search, FreeForm payload, String absolutePath, String sitePath) {
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);

            DriveItem driveItem = graphClient.drives(driveID).root().itemWithPath(relativePath).buildRequest().get();
            if (driveItem != null && driveItem.id != null && driveItem.folder != null) {
                iterateAndAddFilesToCollection(files, search, driveID, relativePath, useEmail, accountID, payload,sitePath);
            } else {
                files.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService, sitePath));
                ResponseUtil.listOfFileEntityResponse(files.stream().map(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File::fromFile).collect(Collectors.toList()), true, " ", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains(Constants.ITEM_NOT_FOUND) ? Constants.FILE_NOT_FOUND : cause.getMessage(), payload);
        }
    }

    private void iterateAndAddFilesToCollection(List<FileService> files, String search, String driveID, String relativePath, boolean useEmail, String accountID, FreeForm payload, String sitePath) {
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);

            DriveItemCollectionPage page = graphClient.drives(driveID).root().itemWithPath(relativePath).children()
                    .buildRequest().get();
            while (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem driveInPage : page.getCurrentPage()) {
                    if (isSearchCriteriaMatched(search, driveInPage)) {
                        files.add(new FileImpl(driveInPage, graphClient, sitePath, fileSearchUtilityService));
                    }
                }

                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.listOfFileEntityResponse(files.stream().map(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File::fromFile).collect(Collectors.toList()), true, Constants.FILE_FETCHED_SUCCESSFULLY, payload);
                    break;
                }
            }
        } catch (Exception cause) {
            LOGGER.error(" {} : {} ", Constants.ERROR_DESCRIPTION, cause);
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains(Constants.ITEM_NOT_FOUND) ? Constants.FILE_NOT_FOUND : cause.getMessage(), payload);
        }
    }

    private boolean isSearchCriteriaMatched(String search, DriveItem driveItem) {
        return search != null && !search.isEmpty() && driveItem.name != null && driveItem.name.contains(search)
                || search != null && search.isBlank();
    }
}
