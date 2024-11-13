package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class provides operations for managing files in SharePoint, specifically deleting and renaming files.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class OperationsManagementDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsManagementDao.class);
    private final GraphServiceClientProvider provider;
    private final SearchFileExecutor searchFileExecutor;


    @Inject
    public OperationsManagementDao(GraphServiceClientProviderFactory factory, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Deletes a file at the specified path, handling path validation, file existence checks, and potential errors.
     *
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> deleteFile(String target) {
        try {
            Map<String, Object> payload = new HashMap<>();

            if (target != null && !FileUtil.hasFileExtension(target)) {
                return ResponseUtil.buildPathValidationResult(false, Constants.PROVIDE_VALID_PATH_FILE_NOT_FOUND);
            }
            searchFileExecutor.getFileInfo(target, payload, true);
            DriveItem driveItem = (payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null);

            if ((boolean) payload.get(Constants.SUCCESS)) {
                assert Objects.requireNonNull(driveItem).parentReference != null;
                assert driveItem.id != null;
                provider.getUserRequestBuilder(null, null).
                        drives(Objects.requireNonNull(driveItem.parentReference.driveId)).
                        items(driveItem.id)
                        .buildRequest().delete();
                ResponseUtil.errorResponse(true, Constants.FILE_DELETED_SUCCESSFULLY, payload);
            }
            if (payload.isEmpty()) {
                ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH, payload);
            }
            return payload;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
        }
        return Map.of();
    }

    /**
     * Renames a file to the specified target name, handling validation and potential errors.
     *
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> renameFile(String sourcePath, String targetFileName) {
        Map<String, Object> payload = new HashMap<>();
        try {
            validateAndRenameFile(sourcePath, targetFileName, payload);
        } catch (Exception cause) {
            handleRenameFileException(cause, payload);
        }
        return payload;
    }

    private void validateAndRenameFile(String sourcePath, String targetFileName, Map<String, Object> payload) {
        if (sourcePath != null) {
            searchFileExecutor.getFileInfo(sourcePath, payload, true);
            if (payload.get(Constants.SUCCESS).equals(Boolean.TRUE)) {
                DriveItem driveItem = (payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null);
                if (driveItem != null) {
                    renameDriveItem(targetFileName, driveItem, payload);
                }
            } else {
                setFileNotFoundMessage(payload);
            }
        }
    }

    private void renameDriveItem(String targetFileName, DriveItem drive, Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        driveItem.name = targetFileName;
        provider.getUserRequestBuilder(null, null).drives(Objects.requireNonNull(drive.parentReference.driveId)).items(Objects.requireNonNull(drive.id)).buildRequest().patch(driveItem);
        setFileRenamedSuccessfullyMessage(payload);
    }

    private void setFileNotFoundMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.PLEASE_PROVIDE_A_VALID_PATH_THE_FILE_FOLDER_WAS_NOT_FOUND);
        payload.put(Constants.SUCCESS, false);
    }

    private void handleRenameFileException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_RENAME_FILE);
        payload.put(Constants.SUCCESS, false);
    }

    private void setFileRenamedSuccessfullyMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FILE_RENAMED_SUCCESSFULLY);
        payload.put(Constants.SUCCESS, true);
    }

}
