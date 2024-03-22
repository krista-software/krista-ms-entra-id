package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FilePermissionDaoUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileMapper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FilePermissionSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionSearchDao.class);
    private final GraphServiceClientProvider clientProvider;
    private final SharePointAttributes sharePointAttributes;
    private final SearchFileExecutor searchFileExecutor;


    @Inject
    public FilePermissionSearchDao(GraphServiceClientProviderFactory clientProviderFactory,
                                   SharePointAttributes sharePointAttributes, SearchFileExecutor searchFileExecutor) {
        this.clientProvider = clientProviderFactory.create();
        this.sharePointAttributes = sharePointAttributes;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Searches for files with a specified permission, handling potential errors and returning matching files.
     *
     * @return A FreeForm object containing the list of files with the specified permission or error messages.
     */
    public FreeForm getSearchByPermission(String folderPath, String permission, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with permission: {} ", permission);
        FreeForm payload = new FreeForm();
        Map<String, Object> mapPayload = new HashMap<>();
        if (permission != null) {
            try {
                searchFileExecutor.getFileInfo(folderPath, mapPayload, true, useEmail, accountID);
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

    public FreeForm fetchFilesWithPermissions(String permission, boolean useEmail, String accountID, DriveItem driveItem, Map<String, Object> mapPayLoad, FreeForm payload) {
        try {
            List<FileService> files =  fetchFiles(permission, useEmail, accountID, driveItem, mapPayLoad);
            if(!files.isEmpty()){
                ResponseUtil.listOfFileEntityResponse(files.stream()
                        .map(File::fromFile)
                        .collect(Collectors.toList()), true, "Files with [ " + permission + " ] permission Fetched Successfully", payload);
            }else{
                ResponseUtil.getInfoResponse(false,"Files with [ " + permission + " ] permission are not available",payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleException(cause, payload);
        }
        return payload;
    }

    private List<FileService> fetchFiles(String permission, boolean useEmail, String accountID, DriveItem driveItem, Map<String, Object> mapPayLoad) {
        List<FileService> files = new ArrayList<>();
        DriveItemCollectionPage page = clientProvider.getUserRequestBuilder(useEmail, accountID)
                .drives(driveItem.parentReference.driveId)
                .items(driveItem.id)
                .children()
                .buildRequest()
                .get();
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
                break;
            }
        }
        return files;
    }

    private void handleException(Exception cause, FreeForm payload) {
        LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
        ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage())
                .contains(Constants.ITEM_NOT_FOUND)
                ? Constants.FILE_NOT_FOUND
                : Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH, payload);
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
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }

    }

}