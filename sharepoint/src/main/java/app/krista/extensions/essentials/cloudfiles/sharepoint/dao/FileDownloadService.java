package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for downloading files using Microsoft Graph API.
 * Provides methods to download files based on various options such as download link,
 * Krista file, and handling different scenarios.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadService.class);
    private final GraphServiceClientProvider provider;
    private final ManagementUtilityService managementUtilityService;

    @Inject
    public FileDownloadService(GraphServiceClientProviderFactory factory,
                               ManagementUtilityService managementUtilityService) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
    }

    /**
     * Downloads files from the specified path, supporting link generation and Krista file format, with path validation, download mode handling, and error management.
     * @return A Map containing status and result details, including success/failure indicators, download links or files, and error messages.
     */
    public Map<String, Object> downloadFiles(Boolean downloadAsLink, Boolean downloadAsKristaFile, String relativePath) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (relativePath != null && FileUtil.hasFileExtension(relativePath)) {
                relativePath = managementUtilityService.getUpdatedFilePath(relativePath);
                List<File> files = new ArrayList<>();
                DriveItem driveItem = managementUtilityService.checkIfFileExist(relativePath, payload);

                if ((boolean) payload.get(Constants.SUCCESS)) {
                    relativePath = DriveUtil.getRelativePath(relativePath);
                    downloadByDriveIDByPath(driveItem.parentReference.driveId, relativePath, downloadAsLink,
                            downloadAsKristaFile, files, payload);
                    handleDownloadResult(payload);
                } else {
                    setInvalidPathMessage(payload);
                }
            } else {
                setInvalidPathMessage(payload);
            }
        } catch (Exception cause) {
            handleDownloadFilesException(cause, payload);
        }
        return payload;
    }

    private void handleDownloadResult(Map<String, Object> payload) {
        if (payload.isEmpty()) {
            setDownloadFailureMessage(payload);
        }
    }

    private void setDownloadFailureMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_DOWNLOAD_FILE);
        payload.put(Constants.SUCCESS, false);
    }

    private void setInvalidPathMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH);
        payload.put(Constants.SUCCESS, false);
    }

    private void setInvalidFilePathMessage(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.INVALID_PATH_MESSAGE);
        payload.put(Constants.SUCCESS, false);
    }

    private void handleDownloadFilesException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
        payload.put(Constants.MESSAGE, Constants.ERROR_DOWNLOADING_FILES);
        payload.put(Constants.SUCCESS, false);
    }

    private void downloadByDriveIDByPath(String driveID, String path, Boolean downloadAsLink,
                                         Boolean downloadAsKristaFile, List<File> files, Map<String, Object> payload) {
        try {
            DriveItem driveItem = (driveID != null) ? provider.getUserRequestBuilder(null, null)
                    .drives(driveID).root().itemWithPath(path).buildRequest().get()
                    : provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(path)
                    .buildRequest().get();
            if (driveItem != null && driveItem.folder == null) {
                if (downloadAsLink != null && downloadAsLink) {
                    handleDownloadLink(payload, driveItem);
                } else if (downloadAsKristaFile != null && downloadAsKristaFile && driveItem.size != null &&
                        driveItem.size > (1024 * 1024 * 30)) {
                    handleDownloadAsKristaFile(payload, driveItem);
                } else {
                    handleDownload(payload, driveItem);
                }
            }
        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    private void handleDownloadLink(Map<String, Object> payload, DriveItem driveItem) {
        payload.put(Constants.SUCCESS, true);
        payload.put(Constants.MESSAGE, Constants.TO_DOWNLOAD_FILE_PLEASE_CLICK_ON_URL);
        payload.put(Constants.DOWNLOAD_LINK, driveItem.additionalDataManager().get("@microsoft.graph.downloadUrl")
                .getAsString());
    }

    private void handleDownloadAsKristaFile(Map<String, Object> payload, DriveItem driveItem) {
        payload.put(Constants.SUCCESS, true);
        payload.put(Constants.MESSAGE, Constants.FILE_SIZE_IS_GREATER_THAN_30MB);
        payload.put(Constants.DOWNLOAD_LINK, driveItem.additionalDataManager().get("@microsoft.graph.downloadUrl")
                .getAsString());
    }

    private void handleDownload(Map<String, Object> payload, DriveItem driveItem) throws IOException {
        if (driveItem.size != null && driveItem.size > (1024 * 1024 * 30)) { // 30MB
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.TO_DOWNLOAD_FILE_PLEASE_CLICK_ON_URL);
            payload.put(Constants.DOWNLOAD_LINK, "");
        } else if (driveItem.size != null && driveItem.size < (1024 * 1024 * 30) &&
                driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
            assert driveItem.id != null;
            InputStream inputStream = provider.getUserRequestBuilder(null, null)
                    .drives(driveItem.parentReference.driveId).items(driveItem.id).content().buildRequest().get();
            java.io.File content = DriveUtil.getFileFromInputStream(inputStream, driveItem.name);
            app.krista.model.base.File kristaFile = CatalogTypes.toFile(content.getName(), content);
            payload.put(Constants.KRISTA_FILE, kristaFile);
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.TO_DOWNLOAD_FILE_PLEASE_CLICK_ON_URL);
        }
    }


}
