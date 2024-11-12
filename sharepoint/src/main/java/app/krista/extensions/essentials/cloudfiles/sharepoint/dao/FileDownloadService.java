package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.KristaMediaClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
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
    private static final String MICROSOFT_GRAPH_DOWNLOAD_URL = "@microsoft.graph.downloadUrl";
    private final GraphServiceClientProvider provider;
    private final KristaMediaClient kristaMediaClient;
    private final SearchFileExecutor searchFileExecutor;


    @Inject
    public FileDownloadService(GraphServiceClientProviderFactory factory, KristaMediaClient kristaMediaClient, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.kristaMediaClient = kristaMediaClient;
        this.searchFileExecutor = searchFileExecutor;
    }

    /**
     * Downloads files from the specified path, supporting link generation and Krista file format, with path validation, download mode handling, and error management.
     *
     * @return A Map containing status and result details, including success/failure indicators, download links or files, and error messages.
     */
    public Map<String, Object> downloadFiles(Boolean downloadAsLink, Boolean downloadAsKristaFile, String absolutePath) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (absolutePath != null && FileUtil.hasFileExtension(absolutePath)) {
                searchFileExecutor.getFileInfo(absolutePath, payload, true);
                DriveItem driveItem = (payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null);

                if ((boolean) payload.get(Constants.SUCCESS)) {
                    String relativePath = DriveUtil.validatePath(absolutePath);
                    downloadByDriveIDByPath(relativePath, downloadAsLink,
                            downloadAsKristaFile, payload, absolutePath, driveItem);
                    handleDownloadResult(payload);
                } else {
                    setInvalidPathMessage(payload);
                }
            } else {
                setInvalidPathMessage(payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_VALID_FILE_SOURCE_PATH, payload);
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
        payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH_FILE_NOT_FOUND);
        payload.put(Constants.SUCCESS, false);
    }


    private void downloadByDriveIDByPath(String path, Boolean downloadAsLink,
                                         Boolean downloadAsKristaFile, Map<String, Object> payload, String absolutePath, DriveItem driveItem) {
        try {
            if (driveItem != null && driveItem.folder == null) {
                if (downloadAsLink != null && downloadAsLink) {
                    handleDownloadLink(payload, driveItem, absolutePath);
                } else if (downloadAsKristaFile != null && downloadAsKristaFile && driveItem.size != null &&
                        driveItem.size > (1024 * 1024 * 50L)) { // This line indicated that if file size is more than 50 MB then should be able to download as link
                    handleDownloadAsKristaFile(payload, driveItem, path);
                } else {
                    handleDownload(payload, driveItem);
                }
            }
        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    private void handleDownloadLink(Map<String, Object> payload, DriveItem driveItem, String path) {
        payload.put(Constants.SUCCESS, true);
        payload.put(Constants.MESSAGE, Constants.TO_DOWNLOAD_FILE_PLEASE_CLICK_ON_URL);
        payload.put(Constants.DOWNLOAD_LINK, "<a href =" + driveItem.additionalDataManager().get(MICROSOFT_GRAPH_DOWNLOAD_URL).getAsString() + ">" + path);
    }

    private void handleDownloadAsKristaFile(Map<String, Object> payload, DriveItem driveItem, String path) {
        payload.put(Constants.SUCCESS, true);
        payload.put(Constants.MESSAGE, Constants.FILE_SIZE_IS_GREATER_THAN_50_MB_PLEASE_DOWNLOAD_FILE_BY_URL + " <a href =" + driveItem.additionalDataManager().get(MICROSOFT_GRAPH_DOWNLOAD_URL).getAsString() + ">" + path);
    }

    private void handleDownload(Map<String, Object> payload, DriveItem driveItem) throws IOException {
        if (driveItem.size != null && driveItem.size > (1024 * 1024 * 50)) {
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.TO_DOWNLOAD_FILE_PLEASE_CLICK_ON_URL);
            payload.put(Constants.DOWNLOAD_LINK, "");
        } else if (driveItem.size != null && driveItem.size < (1024 * 1024 * 50) &&
                driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
            assert driveItem.id != null;
            InputStream inputStream = provider.getUserRequestBuilder(null, null)
                    .drives(driveItem.parentReference.driveId).items(driveItem.id).content().buildRequest().get();
            java.io.File content = DriveUtil.getFileFromInputStream(inputStream, driveItem.name);
            app.krista.model.base.File kristaFile = kristaMediaClient.toKristaFile(content);
            payload.put(Constants.KRISTA_FILE, kristaFile);
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.FILE_DOWNLOADED_SUCCESSFULLY);
        }
    }

}
