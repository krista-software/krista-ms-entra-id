package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This service class provides methods for reading and uploading text files in SharePoint using Microsoft Graph API.
 * It includes functionality to check file existence, read text files, and upload text files.
 * <p>
 * ***************************************************************
 * WARNING: DO NOT CHANGE ANYTHING IN THIS CLASS.
 * ***************************************************************
 *
 * @author Vaibhav Choudhary
 */

@Service
public class DriveTextFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveTextFileManager.class);

    private final GraphServiceClientProvider provider;

    private final ManagementUtilityService managementUtilityService;

    @Inject
    public DriveTextFileManager(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
    }

    public Map<String, Object> readTxtFile(String source) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {
                DriveItem drive = managementUtilityService.checkIfFileExist(source, payload);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    String path = DriveUtil.getRelativePath(source);
                    getFileInfo(drive.parentReference.driveId, path, payload);
                    if ((boolean) payload.get(Constants.SUCCESS)) {
                        readTxtFileFromSource(payload);
                    }
                } else {
                    invalidFileName(payload);
                }
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

    private void readTxtFileFromSource(Map<String, Object> payload) {
        DriveItem driveItem = (DriveItem) payload.get(Constants.DRIVE_ITEM);
        if (driveItem.name != null && FileUtil.getBaseName(driveItem.name).get(1) != null && FileUtil.getBaseName(driveItem.name).get(1).equalsIgnoreCase("txt")) {

            InputStream inputStream = provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId).items(driveItem.id).content().buildRequest().get();

            payload.put(Constants.CONTENT, FileUtil.readFile(inputStream));
            payload.put(Constants.MESSAGE, Constants.FILE_READ_SUCCESS);
            payload.put(Constants.SUCCESS, true);
        } else {
            invalidPath(payload);
        }

    }

    public Map<String, Object> uploadTxtFile(String fileContent, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (target != null && !FileUtil.hasFileExtension(fileContent) && target.endsWith(".txt")) {
                DriveItem drive = managementUtilityService.checkIfFileExist(DriveUtil.removeLastSegment(target), payload);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    String path = DriveUtil.getRelativePath(target);
                    String parentPath = FileUtil.getParentPath(path);
                    Map<String, Object> response = new HashMap<>();
                    getFileInfo(drive.parentReference.driveId, parentPath, response);
                    if ((boolean) response.get(Constants.SUCCESS)) {
                        DriveItem driveItem = (DriveItem) response.get(Constants.DRIVE_ITEM);
                        if (driveItem.name != null && driveItem.folder != null) {
                            uploadTxtFileFromTarget(path,fileContent,payload,driveItem);
                        }
                    } else {
                        invalidPath(payload);
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

    private void uploadTxtFileFromTarget(String path, String fileContent, Map<String, Object> payload, DriveItem driveItem) throws IOException {
        String[] stream = path.split("/");

        String fileName = stream[stream.length - 1].contains(".txt") ? stream[stream.length - 1] : null;
        if (fileName != null) {
            java.io.File javaFile = FileUtil.createTxtFile(fileName, fileContent);
            DriveItem uploadFile = provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId).items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile));
            if (uploadFile != null) {
                app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new DrivePermissionImpl(uploadFile, provider));
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
                    provider.getUserRequestBuilder(null, null).drives(driveId).root().itemWithPath(path).buildRequest().get() :
                    provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(path).buildRequest().get();
            response.put(Constants.DRIVE_ITEM, driveItem);
            response.put(Constants.SUCCESS, true);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            response.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            response.put(Constants.SUCCESS, false);
        }
    }
}
