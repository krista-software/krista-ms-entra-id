package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This service class provides methods for reading and uploading text files in SharePoint using Microsoft Graph API.
 * It includes functionality to check file existence, read text files, and upload text files.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class DriveTextFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveTextFileManager.class);
    private final GraphServiceClientProvider provider;
    private final ManagementUtilityService managementUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;

    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public DriveTextFileManager(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService,
                                FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Reads the contents of a TXT file at the specified path, handling path validation, file existence checks, and potential errors.
     *
     * @return A Map containing status and result details, including success/failure indicators, file contents, or error messages.
     */
    public Map<String, Object> readTxtFile(String source) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {
                //source = managementUtilityService.getUpdatedFilePath(source);
                searchFileExecutor.getFileInfo(source, payload, true);
                DriveItem driveItem = (payload.containsKey(Constants.DRIVE_ITEM) ? (DriveItem) payload.get(Constants.DRIVE_ITEM) : null);

                //String path = DriveUtil.getRelativePath(source);
                //    getFileInfo(driveItem.parentReference.driveId, path, payload);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    readTxtFileFromSource(payload, driveItem);
                } else {
                    invalidPath(payload);
                }
            } else {
                invalidFileName(payload);
            }

            return payload;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
            failedToReadTextFile(payload);
        }
        return payload;
    }

    private void failedToReadTextFile(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_READ_TEXTFILE);
        payload.put(Constants.SUCCESS, false);
    }

    private void readTxtFileFromSource(Map<String, Object> payload, DriveItem driveItem) {
//        DriveItem driveItem = (DriveItem) payload.get(Constants.DRIVE_ITEM);
        if (driveItem.name != null && FileUtil.getBaseName(driveItem.name).get(1) != null &&
                FileUtil.getBaseName(driveItem.name).get(1).equalsIgnoreCase("txt")) {

            InputStream inputStream = provider.getUserRequestBuilder(null, null)
                    .drives(driveItem.parentReference.driveId).items(driveItem.id).content().buildRequest().get();

            payload.put(Constants.CONTENT, FileUtil.readFile(inputStream));
            payload.put(Constants.MESSAGE, Constants.FILE_READ_SUCCESS);
            payload.put(Constants.SUCCESS, true);
        } else {
            invalidPath(payload);
        }

    }


    /**
     * Uploads a TXT file with specified content to the target path, handling path validation, folder existence checks, potential errors, and file creation.
     *
     * @return A Map containing status and result details, including success/failure indicators and error messages.
     */
    public Map<String, Object> uploadTxtFile(String fileContent, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (target != null && FileUtil.hasFileExtension(target) && target.endsWith(".txt")) {

                String path = DriveUtil.getRelativePath(target);
                String parentPath = FileUtil.getParentPath(path);
                Map<String, Object> response = new HashMap<>();
                searchFileExecutor.getFileInfo(parentPath, payload, true);

                DriveItem driveItem = (payload.containsKey(Constants.DRIVE_ITEM) ? (DriveItem) payload.get(Constants.DRIVE_ITEM) : null);

                if ((boolean) payload.get(Constants.SUCCESS)) {
//                        DriveItem driveItem = (DriveItem) response.get(Constants.DRIVE_ITEM);
                    if (driveItem.name != null && driveItem.folder != null) {
                        uploadTxtFileFromTarget(target, fileContent, payload, driveItem);
                    }
                } else {
                    invalidPath(payload);
                }
            } else {
                invalidPath(payload);
            }

        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
            failedToUploadTxtFile(payload);
        }
        return payload;
    }

    private void uploadTxtFileFromTarget(String path, String fileContent, Map<String, Object> payload,
                                         DriveItem driveItem) throws IOException {
        String[] stream = path.split("/");

        String fileName = stream[stream.length - 1].contains(".txt") ? stream[stream.length - 1] : null;
        if (fileName != null) {
            java.io.File javaFile = FileUtil.createTxtFile(fileName, fileContent);
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
            DriveItem uploadFile = graphClient.drives(driveItem.parentReference.driveId).items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile));
            if (uploadFile != null) {
                File fileEntity = CatalogTypes.fromFile(new DrivePermissionImpl(uploadFile, graphClient, fileSearchUtilityService));
                payload.put(Constants.FILE, fileEntity);
                payload.put(Constants.MESSAGE, Constants.FILE_UPLOADED_SUCCESSFULLY);
                payload.put(Constants.SUCCESS, true);
            }
        } else {
            invalidFileName(payload);
        }
    }

    private void failedToUploadTxtFile(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_UPLOAD_TEXTFILE);
        payload.put(Constants.SUCCESS, false);
    }

    public void invalidPath(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH);
        payload.put(Constants.SUCCESS, false);
    }

    public void invalidFileName(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.INVALID_FILENAME);
        payload.put(Constants.SUCCESS, false);
    }

    public void getFileInfo(String driveId, String path, Map<String, Object> response) {
        try {
            DriveItem driveItem = driveId != null ?
                    provider.getUserRequestBuilder(null, null).drives(driveId).root()
                            .itemWithPath(path).buildRequest().get() :
                    provider.getUserRequestBuilder(null, null).drive().root()
                            .itemWithPath(path).buildRequest().get();
            response.put(Constants.DRIVE_ITEM, driveItem);
            response.put(Constants.SUCCESS, true);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            response.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                    .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : cause.getMessage());
            response.put(Constants.SUCCESS, false);
        }
    }
}
