package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

@Service
public class SearchFileExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementUtilityService.class);

    private final FileSearchImpl fileSearchImpl;

    private final ManagementUtilityService managementUtilityService;

    private final GraphServiceClientProvider provider;

    @Inject
    public SearchFileExecutor(FileSearchImpl fileSearchImpl, ManagementUtilityService managementUtilityService, GraphServiceClientProvider provider) {
        this.fileSearchImpl = fileSearchImpl;
        this.managementUtilityService = managementUtilityService;
        this.provider = provider;
    }

    public static String driveIdPath(String fullPath) {
        String str[] = fullPath.split("/");
        String drivePath = "/" + str[1] + "/" + (str[2]);
        return drivePath;
    }

    public static String getRelativePath(String fullUrl) {
        try {
            String escapedUrl = fullUrl.replace(" ", "%20");  // Escape spaces
            URI uri = new URI(escapedUrl);
            String path = uri.getPath();

            String[] pathSegments = path.split(java.io.File.separator.equals("\\") ? "\\\\" : java.io.File.separator);

            // Find the index of the last "Documents" segment, regardless of case:
            int startIndex = -1;
            for (int i = pathSegments.length - 1; i >= 0; i--) {
                if (pathSegments[i].equalsIgnoreCase(Constants.DOCUMENTS) ||
                        pathSegments[i].equalsIgnoreCase(Constants.SHARED_DOCUMENTS)) {
                    startIndex = i;
                    break;
                }
            }

            if (startIndex != -1) {
                startIndex++;  // Start after "Documents"
                StringBuilder dynamicPath = new StringBuilder(java.io.File.separator);
                for (int i = startIndex; i < pathSegments.length; i++) {
                    dynamicPath.append(pathSegments[i]);
                    if (i < pathSegments.length - 1) {
                        dynamicPath.append(File.separator);
                    }
                }
                return dynamicPath.toString();
            } else {
                return null;  // Handle cases where "Documents" is not found
            }
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public String filePath(String fullPath) {
        String[] str = fullPath.split("/");

        if (str.length < 4) {
            return null;
        }

        StringBuilder drivePathBuilder = new StringBuilder("/");
        for (int i = 3; i < str.length; i++) {
            drivePathBuilder.append(str[i]);
            if (i < str.length - 1) {
                drivePathBuilder.append("/");
            }
        }

        return drivePathBuilder.toString();
    }

    public void getFileInfo(String target, Map<String, Object> payload, boolean isSourcePath) {
        try {
            if (target == null || target.isEmpty()) {
                payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH);
                payload.put(Constants.SUCCESS, false);
                return;
            }
            String filePath = filePath(target);
            String driveIdPath = driveIdPath(target);
            String updatedTarget = getUpdatedFilePath(driveIdPath);
            checkIfFileExist(updatedTarget, payload);
            String driveId = payload.get(Constants.Drive_Id).toString();

            if ((boolean) payload.get(Constants.SUCCESS)) {
                target = DriveUtil.getRelativePath(filePath);
                assert target != null;
                DriveItem driveItem = provider.getUserRequestBuilder(null, null).drives(driveId)
                        .root().itemWithPath(target).buildRequest().get();

                if (driveItem == null) {
                    setPayloadValues(payload, false, driveItem, Constants.File_Not_Found);
                } else {
                    setPayloadValues(payload, true, driveItem, Constants.FILE_DELETED_SUCCESSFULLY);
                }
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage());
            managementUtilityService.handleFileNotFoundException(cause, payload);
        }
    }

    private void setPayloadValues(Map<String, Object> payload, boolean success, DriveItem driveItem, String message) {
        payload.put(Constants.MESSAGE, message);
        payload.put(Constants.SUCCESS, success);
        payload.put(Constants.DRIVE_ITEM, driveItem);
    }

    public String getUpdatedFilePath(String path) {
        String domainName = fileSearchImpl.getDomainName();
        String siteDomainPath = fileSearchImpl.getDomainPath(domainName, true);
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        return fileSearchImpl.handleAbsolutePath(path, siteDomainPath);
    }

    public void checkIfFileExist(String sourcePath, Map<String, Object> payload) {
        if (sourcePath != null) {
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
                getFileByAbsolutePath(sourcePath, graphClient, payload);
                DriveItem driveItem = (DriveItem) payload.get(Constants.DRIVE_ITEM);
                boolean isValidFile = driveItem != null && driveItem.parentReference != null;
                if (isValidFile) {
                    payload.put(Constants.SUCCESS, true);
                } else {
                    handleInvalidFile(payload);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                handleFileExistenceCheckException(cause, payload);
            }
        } else {
            handleInvalidFile(payload);
        }
    }

    private void handleInvalidFile(Map<String, Object> payload) {
        payload.put(Constants.MESSAGE, "Please provide a valid path, the file was not found.");
        payload.put(Constants.SUCCESS, false);
    }

    private void handleFileExistenceCheckException(Exception cause, Map<String, Object> payload) {
        LOGGER.error("{} : {}", Constants.ERROR_MESSAGE, cause.getMessage());
        payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage())
                .contains("itemNotFound") ? Constants.FILE_NOT_FOUND : cause.getMessage());
        payload.put(Constants.SUCCESS, false);
    }

    public LinkedList<SearchRequest> buildSearchRequestList(String filePath) {
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
        requests.size = 25;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.LIST, EntityType.LIST_ITEM,
                EntityType.DRIVE, EntityType.DRIVE_ITEM);

        // Fill in the rest of the request setup logic
        LinkedList<SortProperty> sortProperties = new LinkedList<>();
        SortProperty sort = new SortProperty();
        sort.name = "LastModifiedTime";
        sort.isDescending = false;
        sortProperties.add(sort);
        requests.sortProperties = sortProperties;
        return requests;
    }

    public void getFileByAbsolutePath(String filePath, GraphServiceClient<Request> graphClient, Map<String, Object> payload) {
        LOGGER.info("Searching for files with filePath: {}", filePath);
        if (filePath != null) {
            try {
                LinkedList<SearchRequest> requestsList = buildSearchRequestList(filePath);
                SearchEntityQueryCollectionPage page = buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    findDriveItemByFilePath(page, payload);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
                throw new IllegalArgumentException(cause.getMessage(), cause);
            }
        }

    }

    public SearchEntityQueryCollectionPage buildSearchQuery(GraphServiceClient<Request> graphClient, LinkedList<SearchRequest> requestsList) {
        SearchEntityQueryCollectionPage page = graphClient
                .search()
                .query(SearchEntityQueryParameterSet.newBuilder().withRequests(requestsList).build())
                .buildRequest()
                .post();
        return page;
    }

    public void findDriveItemByFilePath(SearchEntityQueryCollectionPage page, Map<String, Object> payload) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                assert driveItem.parentReference != null;
                                String driveId = driveItem.parentReference.driveId;
                                payload.put(Constants.Drive_Id, driveId);
                                payload.put(Constants.DRIVE_ITEM, driveItem);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

}

