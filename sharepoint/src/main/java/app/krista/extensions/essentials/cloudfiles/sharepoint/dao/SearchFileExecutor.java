package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.*;

@Service
public class SearchFileExecutor {

    private static final String ERROR_DURING_FILE_SEARCH = "Error during file search: {}";
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchFileExecutor.class);
    private static final String PLEASE_PROVIDE_VALID_PATH = "Please, provide valid path";
    private final FileSearchImpl fileSearchImpl;
    private final GraphServiceClientProvider provider;

    @Inject
    public SearchFileExecutor(FileSearchImpl fileSearchImpl, GraphServiceClientProvider provider) {
        this.fileSearchImpl = fileSearchImpl;
        this.provider = provider;
    }

    public void getFileInfo(String path, Map<String, Object> payload, boolean isSourcePath, Boolean useMail, String accountID) {
        try {
            String relativeFilePath = FileUtil.relativeFilePath(path);
            String driveIdPath = FileUtil.driveIdPath(path);
            String updatedTarget = getUpdatedFilePath(driveIdPath);

            if (isSourcePath) {
                payload.put(Constants.SOURCE_ROOT_PATH, driveIdPath.replace("%20", " "));
            } else {
                payload.put(Constants.TARGET_ROOT_PATH, driveIdPath.replace("%20", " "));
            }
            setPayloadIfInitialPathValidOrNot(updatedTarget, payload, useMail, accountID);
            String driveId = payload.get(Constants.DRIVE_ID) != null ? payload.get(Constants.DRIVE_ID).toString() : null;
            fetchFileDetailsByDriveID(driveId, relativeFilePath, isSourcePath, payload, useMail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(ERROR_DURING_FILE_SEARCH, cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_A_VALID_PATH_THE_FILE_FOLDER_WAS_NOT_FOUND, payload);
        }
    }

    public void getFileInfo(String path, Map<String, Object> payload, boolean isSourcePath) {
        getFileInfo(path, payload, isSourcePath, null, null);
    }

    public void fetchFileDetailsByDriveID(String driveId, String relativeFilePath, boolean isSourcePath, Map<String, Object> payload, Boolean useMail, String accountID) {

        if (driveId != null) {
            String path = relativeFilePath != null ? DriveUtil.validatePath(relativeFilePath) : "/";
            DriveItem driveItem = getDriveItem(driveId, path, payload, useMail, accountID);
            if (driveItem != null) {
                if (isSourcePath) {
                    payload.put(Constants.SOURCE_RELATIVE_PATH, relativeFilePath);
                    ResponseUtil.addSourceDriveItem(true, driveItem, payload);
                } else {
                    payload.put(Constants.TARGET_RELATIVE_PATH, relativeFilePath);
                    ResponseUtil.addTargetDriveItem(true, driveItem, payload);
                }
                payload.put(Constants.SUCCESS, true);
            } else {
                if (isSourcePath) {
                    payload.put(Constants.IS_SOURCE_FILE_EXIST, false);
                } else {
                    payload.put(Constants.IS_TARGET_FILE_EXIST, false);
                }
                payload.put(Constants.SUCCESS, false);
            }
        }
    }

    @Nullable
    private DriveItem getDriveItem(String driveId, String path, Map<String, Object> payload, Boolean useMail, String accountID) {
        try {
            return path != null ?
                    provider.getUserRequestBuilder(useMail, accountID).
                            drives(driveId)
                            .root().itemWithPath(path).buildRequest().get()
                    : provider.getUserRequestBuilder(useMail, accountID).
                    drives(driveId)
                    .root().buildRequest().get()
                    ;

        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(ERROR_DURING_FILE_SEARCH, cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_A_VALID_PATH_THE_FILE_FOLDER_WAS_NOT_FOUND, payload);
        }
        return null;
    }

    public String getUpdatedFilePath(String path) {
        String domainName = fileSearchImpl.getDomainName();
        String siteDomainPath = fileSearchImpl.getDomainPath(domainName, true);
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        return fileSearchImpl.handleAbsolutePath(path, siteDomainPath);
    }

    public void setPayloadIfInitialPathValidOrNot(String sourcePath, Map<String, Object> payload, Boolean useMail, String accountID) {
        if (sourcePath != null) {
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useMail, accountID);
                getFileByAbsolutePath(sourcePath, graphClient, payload);
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error(ERROR_DURING_FILE_SEARCH, cause.getMessage(), cause);
                handleFileExistenceCheckException(cause, payload);
            }
        }
    }

    private void handleFileExistenceCheckException(Exception cause, Map<String, Object> payload) {
        LOGGER.error("{} : {}", Constants.ERROR_MESSAGE, cause.getMessage());
        payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                .contains(Constants.ITEM_NOT_FOUND) ? Constants.FILE_NOT_FOUND : Constants.PLEASE_PROVIDE_A_VALID_PATH_THE_FILE_FOLDER_WAS_NOT_FOUND);
        payload.put(Constants.SUCCESS, false);
    }

    public List<SearchRequest> buildSearchRequestsFromFile(String filePath) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(filePath, requests);
        requestsList.add(requests);
        return requestsList;
    }

    public void buildSearchRequestForRest(String filePath, SearchRequest requests) {
        SearchQuery query = new SearchQuery();
        query.queryString = "path:\"" + filePath + "\"";
        requests.query = query;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.LIST, EntityType.LIST_ITEM,
                EntityType.DRIVE, EntityType.DRIVE_ITEM);
        return requests;
    }

    public void getFileByAbsolutePath(String filePath, GraphServiceClient<Request> graphClient, Map<String, Object> payload) {
        LOGGER.info("Searching for files with filePath: {}", filePath);
        if (filePath != null) {
            try {
                List<SearchRequest> requestsList = buildSearchRequestsFromFile(filePath);
                SearchEntityQueryCollectionPage page = buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    findDriveItemByFilePath(page, payload);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error(ERROR_DURING_FILE_SEARCH, cause.getMessage(), cause);
                ResponseUtil.errorResponse(false, Objects.requireNonNull(cause.getMessage())
                        .contains(Constants.ITEM_NOT_FOUND)
                        ? Constants.FILE_NOT_FOUND
                        : PLEASE_PROVIDE_VALID_PATH, payload);
            }
        }

    }

    public SearchEntityQueryCollectionPage buildSearchQuery(GraphServiceClient<Request> graphClient, List<SearchRequest> requestsList) {
        return graphClient
                .search()
                .query(SearchEntityQueryParameterSet.newBuilder().withRequests(requestsList).build())
                .buildRequest()
                .post();
    }

    public void findDriveItemByFilePath(SearchEntityQueryCollectionPage page, Map<String, Object> payload) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                if (driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
                                    payload.put(Constants.DRIVE_ID, driveItem.parentReference.driveId);
                                    payload.put(Constants.DRIVE_ITEM, driveItem);
                                    payload.put(Constants.SUCCESS, true);
                                } else {
                                    ResponseUtil.errorResponse(false, PLEASE_PROVIDE_VALID_PATH, payload);
                                }
                            } else {
                                ResponseUtil.errorResponse(false, PLEASE_PROVIDE_VALID_PATH, payload);
                            }
                            if (!payload.isEmpty()) {
                                return;
                            }
                        }
                    } else {
                        ResponseUtil.errorResponse(false, PLEASE_PROVIDE_VALID_PATH, payload);
                        return;
                    }
                }
            }
        }
    }


}

