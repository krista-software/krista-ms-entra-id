package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * This class provides operations for managing files in SharePoint, specifically deleting and renaming files.
 *
 * ***************************************************************
 * WARNING: DO NOT CHANGE ANYTHING IN THIS CLASS.
 * ***************************************************************
 *
 * @author Vaibhav Choudhary
 */

@Service
public class OperationsManagementDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsManagementDao.class);

    private final GraphServiceClientProvider provider;

    private final ManagementUtilityService managementUtilityService;

    @Inject
    public OperationsManagementDao(GraphServiceClientProviderFactory factory,ManagementUtilityService managementUtilityService) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
    }


    public Map<String, Object> deleteFile(String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            DriveItem driveItem = managementUtilityService.checkIfFileExist(target, payload);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                target = DriveUtil.getRelativePath(target);
                provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId)
                        .root().itemWithPath(target).buildRequest().delete();
                payload.put(Constants.MESSAGE, Constants.FILE_DELETED_SUCCESSFULLY);
                payload.put(Constants.SUCCESS, true);
            } else {
                payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH);
                payload.put(Constants.SUCCESS, false);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            managementUtilityService.handleDeleteFileException(cause, payload);
        }
        return payload;
    }

    public Map<String, Object> renameFile(File renameFile, String targetFileName) {
        Map<String, Object> payload = new HashMap<>();
        try {
            validateAndRenameFile(renameFile, targetFileName, payload);
        } catch (Exception cause) {
            handleRenameFileException(cause, payload);
        }
        return payload;
    }

    private void validateAndRenameFile(File renameFile, String targetFileName, Map<String, Object> payload) {
        if (renameFile != null) {
            if (!renameFile.extension.isBlank() && (FileUtil.getBaseName(targetFileName).get(1).equalsIgnoreCase(null) ||
                    FileUtil.getBaseName(targetFileName).get(1).isEmpty())) {
                setFileExtensionNotAvailableMessage(payload);
            } else {
                DriveItem drive = managementUtilityService.checkIfFileExist(renameFile.absolutePath, payload);

                if(payload.get(Constants.SUCCESS) == Boolean.TRUE) {
                    renameDriveItem(renameFile, targetFileName, drive, payload);
                } else {
                    setFileNotFoundMessage(payload);
                }
            }
        } else {
            setInvalidFileMessage(payload);
        }
    }

    private void renameDriveItem(File renameFile, String targetFileName, DriveItem drive, Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        driveItem.name = targetFileName;
        String relativePath = DriveUtil.getRelativePath(renameFile.absolutePath);
        provider.getUserRequestBuilder(null, null)
                .drives(drive.parentReference.driveId)
                .root()
                .itemWithPath(relativePath)
                .buildRequest()
                .patch(driveItem);

        setFileRenamedSuccessfullyMessage(payload);
    }

    private void setFileExtensionNotAvailableMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FILE_EXTENSION_NOT_AVAILABLE);
        payload.put(Constants.SUCCESS, false);
    }

    private void setFileNotFoundMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FILE_NOT_FOUND);
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
