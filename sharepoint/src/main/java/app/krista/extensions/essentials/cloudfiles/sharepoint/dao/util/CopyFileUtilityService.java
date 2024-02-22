package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.OperationsManagementDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;

/**
 * Service class for copying and moving files using Microsoft Graph API.
 * This class provides methods for validating source and target paths,
 * checking the existence of the target folder, handling exceptions,
 * and performing file operations.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class CopyFileUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFileUtilityService.class);
    private final GraphServiceClientProvider provider;
    private final OperationsManagementDao operationsManagementDao;
    private final ManagementUtilityService managementUtilityService;

    @Inject
    public CopyFileUtilityService(GraphServiceClientProviderFactory factory,
                                  OperationsManagementDao operationsManagementDao,
                                  ManagementUtilityService managementUtilityService) {
        this.provider = factory.create();
        this.operationsManagementDao = operationsManagementDao;
        this.managementUtilityService = managementUtilityService;
    }

    public void validateSourceAndTargetPaths(String source, String target, Map<String, Object> payload) {
        if (source == null || source.trim().isEmpty() || target == null || target.trim().isEmpty()) {
            payload.put(Constants.MESSAGE, Constants.SOURCE_OR_TARGET_PATHS_CANT_BE_NULL);
            payload.put(Constants.SUCCESS, false);
        } else {
            if (FileUtil.hasFileExtension(target)) {
                payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_TARGET_PATH);
                payload.put(Constants.SUCCESS, false);
            } else {
                payload.put(Constants.SUCCESS, true);
            }
        }
    }

    public boolean validateSourceAndTargetPathsForMove(String source, String target, Map<String, Object> payload) {
        Boolean tag = true;
        tag = validatePath(source, Constants.PROVIDE_VALID_SOURCE_PATH, payload);
        tag = validatePath(target, Constants.PROVIDE_VALID_TARGET_PATH, payload);
        return tag;
    }

    protected Boolean validatePath(String path, String errorMessage, Map<String, Object> payload) {
        if (path == null || path.trim().isEmpty()) {
            payload.put(Constants.MESSAGE, errorMessage);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true;
    }

    public void validateTargetPath(String target, Map<String, Object> payload) {
        if (target == null) {
            handleInvalidTargetPath(payload);
        }
    }

    public void handleInvalidTargetPath(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, "Please provide a valid path, the file/folder was not found");
        payload.put(Constants.SUCCESS, false);
    }

    public DriveItem checkRootTargetFolderExist(String target, Map<String, Object> response) {
        if (target != null) {
            try {
                DriveItem driveItem = managementUtilityService.getRootFolderByAbsolutePath(target, true);
                boolean isFolderExist = driveItem != null && driveItem.parentReference != null;
                response.put(Constants.IS_FOLDER_EXIST, isFolderExist);
                return isFolderExist ? driveItem : new DriveItem();
            } catch (GraphServiceException | IllegalArgumentException cause) {
                handleFolderExistenceCheckException(cause, response);
                return new DriveItem();
            }
        } else {
            response.put(Constants.IS_FOLDER_EXIST, false);
            return new DriveItem();
        }
    }

    public void handleFolderExistenceCheckException(Exception cause, Map<String, Object> response) {
        response.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                .contains("itemNotFound") ? Constants.FOLDER_NOT_FOUND : cause.getMessage());
        response.put(Constants.IS_FOLDER_EXIST, false);
    }

    public void handleCopyFileException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(" Error : " + cause.getMessage());
        payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : Constants.PROVIDE_VALID_TARGET_PATH);
        payload.put(Constants.SUCCESS, false);
    }

    public Map<String, Object> handleMoveFileException(Exception cause, Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                .contains(Constants.ITEM_NOT_FOUND) ? Constants.FILE_NOT_FOUND : cause.getMessage());
        payload.put(Constants.SUCCESS, false);
        return payload;
    }

    public Map<String, Object> handleInvalidSourcePath(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, "Please provide a valid path, the file/folder was not found");
        payload.put(Constants.SUCCESS, false);
        return payload;
    }

}
