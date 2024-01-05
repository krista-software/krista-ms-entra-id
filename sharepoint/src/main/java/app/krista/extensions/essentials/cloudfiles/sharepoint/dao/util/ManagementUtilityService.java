package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileSearchImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Vaibhav Choudhary
 *
 */

@Service
public class ManagementUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementUtilityService.class);
    private final GraphServiceClientProvider provider;

    private final FileSearchImpl fileSearchImpl;

    @Inject
    public ManagementUtilityService(GraphServiceClientProviderFactory factory, FileSearchImpl fileSearchImpl) {
        this.provider = factory.create();
        this.fileSearchImpl = fileSearchImpl;
    }

    public DriveItem checkIfFileExist(String sourcePath, Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        if (sourcePath != null) {
            try {
                driveItem = getFileByAbsolutePath(sourcePath, true, null);
                boolean isValidFile = driveItem != null && driveItem.parentReference != null;

                if (isValidFile) {
                    payload.put(Constants.SUCCESS, true);
                    return driveItem;
                } else {
                    handleInvalidFile(payload);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                handleFileExistenceCheckException(cause, payload);
            }
        } else {
            handleInvalidFile(payload);
        }
        return driveItem;
    }

    private void handleInvalidFile(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_SOURCE_PATH);
        payload.put(Constants.SUCCESS, false);
    }

    private void handleFileExistenceCheckException(Exception cause, Map<String, Object> payload) {
        LOGGER.error("{} : {}", Constants.ERROR_MESSAGE, cause.getMessage());
        payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
        payload.put(Constants.SUCCESS, false);
    }

    public DriveItem getFileByAbsolutePath(String filePath, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with filePath: {}", filePath);
        if (filePath != null) {
            filePath = filePath.replace("%20", " ");
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);
                LinkedList<SearchRequest> requestsList = fileSearchImpl.buildSearchRequestList(filePath);
                SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    driveItem = fileSearchImpl.findDriveItemByFilePath(page, filePath, driveItem, false);
                }
                if (driveItem != null) {
                    return driveItem;
                } else {
                    return null;
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
                throw new IllegalArgumentException(cause.getMessage(), cause);
            }
        } else {
            return null;
        }
    }

    protected DriveItem getRootFolderByAbsolutePath(String filePath) {
        LOGGER.info("Searching for files with name: {}", filePath);
        if (filePath != null) {
            filePath = filePath.replace("%20", " ");
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(true, null);
                LinkedList<SearchRequest> requestsList = fileSearchImpl.buildSearchRequestList(filePath);
                SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    driveItem = fileSearchImpl.findDriveItemByFilePath(page, filePath, driveItem, true);
                }
                if (driveItem != null) {
                    return driveItem;
                } else {
                    return null;
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
                throw new IllegalArgumentException("Error during file search : ", cause);
            }
        } else {
            return null;
        }
    }

    public void handleDeleteFileException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
        payload.put(Constants.MESSAGE, "Invalid Input provided");
        payload.put(Constants.SUCCESS, false);
    }


}