package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
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

    private final ManagementUtilityService managementUtilityService;

    private final FileDao fileDao;

    private final SearchFileExecutor searchFileExecutor;


    @Inject
    public OperationsManagementDao(GraphServiceClientProviderFactory factory,
                                   ManagementUtilityService managementUtilityService, FileDao fileDao, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
        this.fileDao = fileDao;
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
            searchFileExecutor.getFileInfo(target, payload, true);
            DriveItem driveItem = (payload.containsKey(Constants.DRIVE_ITEM) ? (DriveItem) payload.get(Constants.DRIVE_ITEM) : null);

            if ((boolean) payload.get(Constants.SUCCESS)) {
                assert Objects.requireNonNull(driveItem).parentReference != null;
                assert driveItem.id != null;
                provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId).items(driveItem.id)
                        .buildRequest().delete();
                payload.put(Constants.MESSAGE, Constants.FILE_DELETED_SUCCESSFULLY);
                payload.put(Constants.SUCCESS, true);
            }
            if (payload.isEmpty()) {
                payload.put(Constants.MESSAGE, Constants.FILE_NOT_DELETED_SUCCESSFULLY);
                payload.put(Constants.SUCCESS, false);
            }
            return payload;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
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
            DriveItem driveItem = (payload.containsKey(Constants.DRIVE_ITEM) ? (DriveItem) payload.get(Constants.DRIVE_ITEM) : null);
            if (payload.get(Constants.SUCCESS) == Boolean.TRUE) {
                renameDriveItem(targetFileName, driveItem, payload);
            } else {
                setFileNotFoundMessage(payload);
            }
        }
    }


    private void renameDriveItem(String targetFileName, DriveItem drive, Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        driveItem.name = targetFileName;
        assert drive.parentReference != null;
        assert drive.id != null;
        provider.getUserRequestBuilder(null, null).drives(drive.parentReference.driveId).items(drive.id).buildRequest().patch(driveItem);
        setFileRenamedSuccessfullyMessage(payload);
    }

    private void setFileExtensionNotAvailableMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FILE_EXTENSION_NOT_AVAILABLE);
        payload.put(Constants.SUCCESS, false);
    }

    private void setFileNotFoundMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, "Please provide valid path, file was not found");
        payload.put(Constants.SUCCESS, false);
    }

    private void setInvalidFileMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.INVALID_PATH_MESSAGE);
        payload.put(Constants.SUCCESS, false);
    }

    private void handleRenameFileException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_RENAME_FILE);
        payload.put(Constants.SUCCESS, false);
    }

    private void setFileRenamedSuccessfullyMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FILE_RENAMED_SUCCESSFULLY);
        payload.put(Constants.SUCCESS, true);
    }

}
