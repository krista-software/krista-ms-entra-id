package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileSearchImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.EntityType;
import com.microsoft.graph.models.SearchRequest;
import com.microsoft.graph.models.SortProperty;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

/**
 * Utility service class for management operations using Microsoft Graph API.
 * This class provides methods for checking file existence, handling exceptions,
 * and performing search operations for files in a SharePoint drive.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class ManagementUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementUtilityService.class);
    private final GraphServiceClientProvider provider;
    private final FileSearchImpl fileSearchImpl;

    @Inject
    public ManagementUtilityService(GraphServiceClientProviderFactory clientProviderFactory, FileSearchImpl fileSearchImpl) {
        this(clientProviderFactory.create(), fileSearchImpl);
    }

    public ManagementUtilityService(GraphServiceClientProvider provider, FileSearchImpl fileSearchImpl) {
        this.provider = provider;
        this.fileSearchImpl = fileSearchImpl;
    }

    public DriveItem checkIfFileExist(String sourcePath, Map<String, Object> payload) {
        DriveItem driveItem = new DriveItem();
        if (sourcePath != null) {
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
                driveItem = getFileByAbsolutePath(sourcePath, graphClient);
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
        payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : cause.getMessage());
        payload.put(Constants.SUCCESS, false);
    }

    public DriveItem getFileByAbsolutePath(String filePath, GraphServiceClient<Request> graphClient) {
        LOGGER.info("Searching for files with filePath: {}", filePath);
        if (filePath != null) {
            String siteDomainPath = fileSearchImpl.getDomainPath(fileSearchImpl.getDomainName(), true);
            filePath = fileSearchImpl.handleAbsolutePath(filePath, siteDomainPath);
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                LinkedList<SearchRequest> requestsList = buildSearchRequestList(filePath);
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

    public DriveItem getRootFolderByAbsolutePath(String filePath, Boolean flag) {
        LOGGER.info("Searching for files with name: {}", filePath);
        if (filePath != null) {
            filePath = filePath.replace(Constants.WHITE_SPACE, " ");
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
                LinkedList<SearchRequest> requestsList = fileSearchImpl.buildSearchRequestList(filePath);
                SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    driveItem = fileSearchImpl.findDriveItemByFilePath(page, filePath, driveItem, flag);
                }
                if (driveItem != null) {
                    return driveItem;
                } else {
                    return null;
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {} {} ", cause.getMessage(), cause);
                throw new IllegalArgumentException("Error during file search : {} ", cause);
            }
        } else {
            return null;
        }
    }

    public void handleDeleteFileException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(" {} : {} " + Constants.ERROR_MESSAGE, cause.getMessage());
        payload.put(Constants.MESSAGE, Constants.INVALID_INPUT_PROVIDED);
        payload.put(Constants.SUCCESS, false);
    }

    public String getUpdatedFilePath(String path) {
        String domainName = fileSearchImpl.getDomainName();
        String siteDomainPath = fileSearchImpl.getDomainPath(domainName, true);
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        return fileSearchImpl.handleAbsolutePath(path, siteDomainPath);
    }

    public LinkedList<SearchRequest> buildSearchRequestList(String filePath) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        fileSearchImpl.buildSearchRequestForRest(filePath, requests);
        requestsList.add(requests);
        return requestsList;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.LIST, EntityType.LIST_ITEM,
                EntityType.DRIVE, EntityType.DRIVE_ITEM);
        return requests;
    }
}