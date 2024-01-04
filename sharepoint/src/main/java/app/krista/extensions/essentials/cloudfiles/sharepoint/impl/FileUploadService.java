package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Choudhary
 */

@Service
public class FileUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadService.class);
    private final GraphServiceClientProvider provider;
    private final ManagementUtilityService managementUtilityService;

    @Inject
    public FileUploadService(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService) {
        this(factory.create(), managementUtilityService);
    }

    public FileUploadService(GraphServiceClientProvider provider, ManagementUtilityService managementUtilityService) {
        this.provider = provider;
        this.managementUtilityService = managementUtilityService;
    }

    public Map<String, Object> uploadFiles(app.krista.model.base.File kristaFile, String targetPath) {
        Map<String, Object> payload = new HashMap<>();
        try {
            validateAndUploadFile(kristaFile, targetPath, payload);
        } catch (Exception cause) {
            handleUploadFilesException(cause, payload);
        }
        return payload;
    }

    private void validateAndUploadFile(app.krista.model.base.File kristaFile, String targetPath, Map<String, Object> payload) {
        if (targetPath != null && !FileUtil.hasFileExtension(targetPath)) {
            DriveItem drive = managementUtilityService.checkIfFileExist(targetPath, payload);

            if ((boolean) payload.get(Constants.SUCCESS)) {
                targetPath = DriveUtil.getRelativePath(targetPath);
                uploadFileByPathByDriveID(drive.parentReference.driveId, targetPath, kristaFile, payload);
            } else {
                setInvalidPathMessage(payload);
            }
        } else {
            setInvalidPathMessage(payload);
        }
    }

    private void setInvalidPathMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH);
        payload.put(Constants.SUCCESS, false);
    }

    private void handleUploadFilesException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
        payload.put(Constants.MESSAGE, Constants.ERROR_IN_UPLOAD_FILE);
        payload.put(Constants.SUCCESS, false);
    }

    private void uploadFileByPathByDriveID(String driveID, String targetPath, app.krista.model.base.File kristaFile, Map<String, Object> payload) {
        try {
            DriveItem driveItem = driveID != null ? provider.getUserRequestBuilder(null, null).drives(driveID).root().itemWithPath(targetPath).buildRequest().get()
                    : provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(targetPath).buildRequest().get();
            if (driveItem != null && driveItem.folder != null && driveItem.id != null) {
                java.io.File javaFile = CatalogTypes.fromFile(kristaFile);
                DriveUtil.listValidator(List.of(javaFile), Constants.FAILED_TO_UPLOAD_FILE);

                DriveItem uploadedDriveItem = driveID != null ?
                        provider.getUserRequestBuilder(null, null).drives(driveID).items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile))
                        : provider.getUserRequestBuilder(null, null).drive().items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile));
                if (uploadedDriveItem != null) {
                    app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new DrivePermissionImpl(driveItem, provider));
                    payload.put(Constants.FILE, fileEntity);
                    payload.put(Constants.MESSAGE, Constants.FILE_UPLOADED_SUCCESSFULLY);
                    payload.put(Constants.SUCCESS, true);
                }
            }
        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            payload.put(Constants.MESSAGE, Constants.FAILED_TO_UPLOAD);
            payload.put(Constants.SUCCESS, false);
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }

    }
}
