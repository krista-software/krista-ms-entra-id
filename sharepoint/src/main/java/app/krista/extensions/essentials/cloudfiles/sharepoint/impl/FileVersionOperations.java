package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.SearchFileExecutor;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemVersion;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.DriveItemVersionCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileVersionOperations {
    public static final String UPDATED = "Updated";
    public static final String CREATED = "Created";
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVersionOperations.class);
    private final GraphServiceClientProvider provider;
    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public FileVersionOperations(GraphServiceClientProviderFactory factory, SearchFileExecutor searchFileExecutor) {
        this(factory.create(), searchFileExecutor);
    }

    public FileVersionOperations(GraphServiceClientProvider provider, SearchFileExecutor searchFileExecutor) {
        this.provider = provider;
        this.searchFileExecutor = searchFileExecutor;
    }

    @NotNull
    private static String getParentPathToCompare(DriveItem driveItem) {
        return driveItem.parentReference.path != null
                ? driveItem.parentReference.path + "/" + driveItem.name
                : "/drives/" + driveItem.parentReference.driveId + "/root:";
    }

    private static boolean isSearchMatch(String search, DriveItem driveInPage) {
        return (!search.isEmpty() && driveInPage.name.contains(search)) || search.isBlank();
    }

    private static String getRootRelativeFilePath(DriveItem driveInPage) {
        String path = FileUtil.getRootRelativeFilePath(driveInPage.parentReference.path);
        return path != null && !path.isBlank()
                ? "/" + FileUtil.getRootRelativeFilePath(driveInPage.parentReference.path)
                : "";
    }

    public Map<String, Object> getVersionHistory(String sourcePath, String search, Long since) {
        Map<String, Object> payload = new HashMap<>();
        searchFileExecutor.getFileInfo(sourcePath, payload, true);
        if (payload.get(Constants.IS_SOURCE_FILE_EXIST) != null && payload.get(Constants.IS_SOURCE_FILE_EXIST).equals(Boolean.TRUE)) {
            fetchFileVersionChange(search, since, payload);
        } else {
            ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_VALID_FOLDER_PATH, payload);
        }
        return payload;
    }

    private void fetchFileVersionChange(String search, Long since, Map<String, Object> payload) {
        try {
            String driveID = payload.get(Constants.DRIVE_ID).toString();
            DriveItem driveItem = (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM);
            DriveItemDeltaCollectionPage page = getUserRequestBuilder().drives(driveID).items(driveItem.id).delta().buildRequest().get();
            List<Map<String, Object>> result = new ArrayList<>();
            while (page != null && !page.getCurrentPage().isEmpty()) {
                String rootPath = payload.get(Constants.SOURCE_ROOT_PATH).toString();
                processFileVersion(driveItem, search, since, rootPath, page, result);
                if (page.getNextPage() != null) {
                    page = page.getNextPage().buildRequest().get();
                } else {
                    ResponseUtil.changesSinceResponse(true, "Files Content", result, payload);
                    break;
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Constants.FILE_NOT_FOUND, payload);
        }
    }

    private GraphServiceClient<Request> getUserRequestBuilder() {
        return provider.getUserRequestBuilder(null, null);
    }

    private void processFileVersion(DriveItem driveItem, String search, Long since, String rootPath, DriveItemDeltaCollectionPage page, List<Map<String, Object>> result) {
        for (DriveItem driveInPage : page.getCurrentPage()) {
            if (driveInPage.name != null && !driveInPage.name.equals("root")
                    && driveInPage.parentReference != null
                    && driveInPage.parentReference.path != null
                    && driveInPage.parentReference.path.equals(getParentPathToCompare(driveItem))
                    && driveInPage.folder == null && driveInPage.id != null && getFileDate(driveInPage.lastModifiedDateTime) > (since != null ? since : 0)) {
                if (isSearchMatch(search, driveInPage)) {
                    processDriveItem(result, driveInPage, rootPath);
                }
            }

        }
    }

    private void processDriveItem(List<Map<String, Object>> result, DriveItem driveInPage, String rootPath) {
        DriveItemVersionCollectionPage driveItemVersionCollectionPage = getUserRequestBuilder().drives(driveInPage.parentReference.driveId).items(driveInPage.id).versions().buildRequest().get();
        if (driveItemVersionCollectionPage != null
                && !driveItemVersionCollectionPage.getCurrentPage().isEmpty()
                && driveItemVersionCollectionPage.getCurrentPage().size() > 1) {

            DriveItemVersion version = driveItemVersionCollectionPage.getCurrentPage().get(0);
            if (version.lastModifiedDateTime != null) {
                Map<String, Object> resultContent = ResponseUtil
                        .createResultContent(rootPath + getRootRelativeFilePath(driveInPage) + "/" + driveInPage.name
                                , getFileDate(driveInPage.lastModifiedDateTime)
                                , UPDATED);
                result.add(resultContent);
            }
        } else {
            Map<String, Object> resultContent = ResponseUtil
                    .createResultContent(rootPath + getRootRelativeFilePath(driveInPage) + "/" + driveInPage.name
                            , getFileDate(driveInPage.lastModifiedDateTime)
                            , CREATED);
            result.add(resultContent);
        }
    }

    private long getFileDate(OffsetDateTime dateTime) {
        return dateTime != null ? Timestamp.valueOf(dateTime.toLocalDateTime()).getTime() : 0L;
    }
}
