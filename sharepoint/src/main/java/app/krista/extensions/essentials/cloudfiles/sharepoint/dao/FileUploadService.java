package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.JavaFileHandleHelper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for uploading files to Microsoft Graph using the Graph API.
 * Provides methods to validate and upload files, handling exceptions and response messages.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadService.class);
    private final GraphServiceClientProvider provider;
    private final ManagementUtilityService managementUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;

    private final JavaFileHandleHelper javaFileHandleHelper;

    @Inject
    public FileUploadService(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService,
                             FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor, JavaFileHandleHelper javaFileHandleHelper) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
        this.javaFileHandleHelper = javaFileHandleHelper;
    }

    /**
     * Uploads files to the specified target path, handling validation and potential errors.
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> uploadFiles(app.krista.model.base.File kristaFile, String targetPath) {
        Map<String, Object> payload = new HashMap<>();
        try {
            validateAndUploadFile(kristaFile, targetPath, payload);
        } catch (Exception cause) {
            handleUploadFilesException(cause, payload);
        }
        return payload;
    }

    private void validateAndUploadFile(app.krista.model.base.File kristaFile, String targetPath,
                                       Map<String, Object> payload) {
        if (targetPath != null && !FileUtil.hasFileExtension(targetPath)) {
     //       targetPath = managementUtilityService.getUpdatedFilePath(targetPath);
            searchFileExecutor.getFileInfo(targetPath,payload,true);
            DriveItem driveItem = (payload.containsKey(Constants.DRIVE_ITEM) ? (DriveItem) payload.get(Constants.DRIVE_ITEM) : null);

            if ((boolean) payload.get(Constants.SUCCESS)) {
                targetPath = DriveUtil.getRelativePath(targetPath);
                uploadFileByPathByDriveID(driveItem.parentReference.driveId, targetPath, kristaFile, payload,driveItem);
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

    private void uploadFileByPathByDriveID(String driveID, String targetPath, app.krista.model.base.File kristaFile,
                                           Map<String, Object> payload, DriveItem driveItem) {
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
//            DriveItem driveItem = driveID != null ? graphClient.drives(driveID).root().itemWithPath(targetPath).buildRequest().get()
//                    : graphClient.drive().root().itemWithPath(targetPath).buildRequest().get();
            if (driveItem != null && driveItem.folder != null && driveItem.id != null) {
                java.io.File javaFile = javaFileHandleHelper.convertKristaFileToJavaFile(kristaFile);
                DriveUtil.listValidator(List.of(javaFile), Constants.FAILED_TO_UPLOAD_FILE);

                DriveItem uploadedDriveItem = driveID != null ? graphClient.drives(driveID).items(driveItem.id)
                                .itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile))
                        : graphClient.drive().items(driveItem.id)
                        .itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile));
                if (uploadedDriveItem != null) {
                    File fileEntity = CatalogTypes.fromFile(new DrivePermissionImpl(uploadedDriveItem, graphClient, fileSearchUtilityService));
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
