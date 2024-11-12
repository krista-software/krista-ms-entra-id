package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.impl;


import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntitySearchMapper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.SearchFileExecutor;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

@Service
public class FileEntitySearchImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileEntitySearchImpl.class);
    private final GraphServiceClientProvider provider;
    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public FileEntitySearchImpl(GraphServiceClientProvider provider, SearchFileExecutor searchFileExecutor) {
        this.provider = provider;
        this.searchFileExecutor = searchFileExecutor;
    }


    /**
     * This function returns file from given relativePath or returns null for exception
     *
     * @param absolutePath id of a file to be search
     * @return {@link List <File>}
     */
    public List<FileService> getFileByPath(String absolutePath) {

        return getFilesListByAbsolutePath(absolutePath);
    }

    public List<FileService> getFilesByParentFolder(String parentFolder) {
        return getFilesListByParentPath(parentFolder);
    }


    public List<FileService> getFilesListByParentPath(String path) {
        return getFilesListByAbsolutePath(path);
    }


    public List<FileService> getFilesListByAbsolutePath(String path) {
        Map<String, Object> payload = new HashMap<>();
        searchFileExecutor.getFileInfo(path, payload, true);
        DriveItem driveItem = payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null;
        if (driveItem != null) {
            String sitePath = payload.containsKey(Constants.SOURCE_ROOT_PATH) ? payload.get(Constants.SOURCE_ROOT_PATH).toString() : " ";
            return getDriveItemCollectionPageByPath(driveItem, FileUtil.getUserRequestBuilder(provider), sitePath);
        }
        return Collections.emptyList();
    }


    private List<FileService> getDriveItemCollectionPageByPath(DriveItem driveItem, GraphServiceClient<Request> graphClient, String sitePath) {
        try {
            List<FileService> files = new ArrayList<>();
            if (driveItem.folder != null && driveItem.folder.childCount != 0) {
                getListofFileService(driveItem, graphClient, sitePath, files);
            } else {
                files.add(new FileEntitySearchMapper(driveItem, sitePath, graphClient));
            }
            return files;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return Collections.emptyList();
    }

    private static void getListofFileService(DriveItem driveItem, GraphServiceClient<Request> graphClient, String sitePath, List<FileService> files) {
        DriveItemCollectionPage page = getDriveItemCollectionPage(driveItem, graphClient);
        while (page != null && !page.getCurrentPage().isEmpty()) {
            for (DriveItem driveInPage : page.getCurrentPage()) {
                files.add(new FileEntitySearchMapper(driveInPage, sitePath, graphClient));
            }
            if (page.getNextPage() != null) {
                page = page.getNextPage().buildRequest().get();
            } else {
                break;
            }
        }
    }

    @Nullable
    private static DriveItemCollectionPage getDriveItemCollectionPage(DriveItem driveItem, GraphServiceClient<Request> graphClient) {
        try {
            return Objects.requireNonNull(driveItem.name).equals("root")
                    ? graphClient.drives(Objects.requireNonNull(driveItem.parentReference.driveId)).root().children().buildRequest().get()
                    : graphClient.drives(Objects.requireNonNull(driveItem.parentReference.driveId)).items(Objects.requireNonNull(driveItem.id)).children().buildRequest().get();
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return null;
    }
}
