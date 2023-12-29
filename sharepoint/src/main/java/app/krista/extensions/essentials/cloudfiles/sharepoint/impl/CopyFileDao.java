package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.RestClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.google.gson.JsonObject;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.*;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@Service
public class CopyFileDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyFileDao.class);
    private final GraphServiceClientProvider provider;
    private final OperationsManagementDao operationsManagementDao;
    private final SharedFileDao sharedFileDao;

    @Inject
    public CopyFileDao(GraphServiceClientProviderFactory factory, OperationsManagementDao operationsManagementDao, SharedFileDao sharedFileDao) {
        this(factory.create(), operationsManagementDao, sharedFileDao);
    }

    public CopyFileDao(GraphServiceClientProvider provider, OperationsManagementDao operationsManagementDao, SharedFileDao sharedFileDao) {
        this.provider = provider;
        this.operationsManagementDao = operationsManagementDao;
        this.sharedFileDao = sharedFileDao;
    }

    public Map<String, Object> copyFile(String source, String target, Boolean replace, Boolean createFolder) {
        Map<String, Object> payload = new HashMap<>();
        try {
            DriveItem driveItemSource = operationsManagementDao.checkIfFileExist(source, payload);
            DriveItem driveItemTarget;
            if ((boolean) payload.get(Constants.SUCCESS)) {
                Map<String, Object> response = new HashMap<>();
                if (createFolder) {
                    driveItemTarget = createFolderInPath(target, response);
                } else {
                    driveItemTarget = checkIfTargetFileExist(target, response);
                }
                if ((boolean) response.get(Constants.IS_FILE_EXIST)) {
                    if (driveItemSource != null) {

                        String endpoint = driveItemSource.id + "/copy";
                        if (Boolean.TRUE.equals(replace)) {
                            endpoint += "?@microsoft.graph.conflictBehavior=replace";
                        }
                        Response copyResponse = getRestClient(driveItemTarget.parentReference.driveId).
                                executeHttpMethods(endpoint, Constants.POST_METHOD,
                                        createRequestBody(driveItemTarget.id));
                        processResponse(copyResponse, payload, driveItemSource);
                    }
                } else {
                    payload.put(Constants.MESSAGE, "Provide Valid target Path");
                    payload.put(Constants.SUCCESS, false);
                }
            }
            return payload;
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    public DriveItem checkIfTargetFolderExist(String target, Map<String, Object> response) {
        try {
            if (target != null) {
                DriveItem driveItem = getFolderByAbsolutePath(target);
                if (driveItem != null && null != driveItem.parentReference) {
                    response.put("isFileExist", true);
                    return driveItem;
                } else {
                    response.put("isFileExist", false);
                    return new DriveItem();
                }
            } else {
                response.put("isFileExist", false);
                return new DriveItem();
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            response.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            response.put("isFileExist", false);
            return new DriveItem();
        }
    }

    public DriveItem checkIfTargetFileExist(String target, Map<String, Object> response) {
        try {
            if (target != null) {
                DriveItem driveItem = getFileByAbsolutePath(target);
                if (driveItem != null && null != driveItem.parentReference) {
                    response.put("isFileExist", true);
                    return driveItem;
                } else {
                    response.put("isFileExist", false);
                    return new DriveItem();
                }
            } else {
                response.put("isFileExist", false);
                return new DriveItem();
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            response.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            response.put("isFileExist", false);
            return new DriveItem();
        }
    }


    private RestClient getRestClient(String driveId) {
        return provider.getRestClient(driveId);
    }

    public RequestBody createRequestBody(String folderId) {
        String payload = getParentReferenceJson(folderId);
        MediaType jsonMediaType = MediaType.parse(Constants.APPLICATION_JSON);
        return RequestBody.create(jsonMediaType, payload);
    }

    private String getParentReferenceJson(String folderId) {
        JsonObject jsonObject = new JsonObject();
        JsonObject parentReference = new JsonObject();
        parentReference.addProperty("id", folderId);
        jsonObject.add("parentReference", parentReference);
        return jsonObject.toString();
    }

    private void processResponse(Response statusResponse, Map<String, Object> payload, DriveItem driveItemSource) throws IOException {
        if (statusResponse.code() == 200 || statusResponse.code() == 202) {
            DriveItem driveItem = provider.getUserRequestBuilder(null, null)
                    .drives(driveItemSource.parentReference.driveId).items(driveItemSource.id)
                    .buildRequest().get();

            payload.put(Constants.FILE, CatalogTypes.fromFile(new FileImpl(driveItem, null, null)));
            payload.put(Constants.MESSAGE, "File has been copied successfully");
            payload.put(Constants.SUCCESS, true);
        } else if (statusResponse.code() == 409) {
            payload.put(Constants.MESSAGE, "A file with this name already exists. Please choose a different file name.");
            payload.put(Constants.SUCCESS, false);
        } else {
            payload.put(Constants.MESSAGE, "There is some issue with File , Please choose a different file name.");
            payload.put(Constants.SUCCESS, false);
        }
    }

    public Map<String, Object> moveFile(String source, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            DriveItem driveItemSource = operationsManagementDao.checkIfFileExist(source, payload);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                Map<String, Object> response = new HashMap<>();
                DriveItem driveItemTarget = checkIfTargetFolderExist(target, response);
                if ((boolean) response.get("isFileExist")) {
                    if (driveItemSource != null) {
                        String endpoint = driveItemSource.id + "?@microsoft.graph.conflictBehavior=replace";

                        Response moveResponse = getRestClient(driveItemTarget.parentReference.driveId).
                                executeHttpMethods(endpoint, Constants.PATCH_METHOD,
                                        createRequestBody(driveItemTarget.id));
                        processResponse(moveResponse, payload, driveItemSource);
                        return payload;
                    }
                } else {
                    payload.put(Constants.MESSAGE, "Provide Valid target Path");
                    payload.put(Constants.SUCCESS, false);
                    return payload;
                }
            } else {
                payload.put(Constants.MESSAGE, "Provide Valid source Path");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }
            return payload;
        } catch (GraphServiceException | IOException | IllegalArgumentException cause) {
            cause.printStackTrace();
            payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put(Constants.SUCCESS, false);
            cause.printStackTrace();
        }
        return payload;
    }


    private DriveItem getFileByAbsolutePath(String filePath) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", filePath);
        if (filePath != null) {
            filePath = filePath.replace("%20", " ");
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(true, null);

                LinkedList<SearchRequest> requestsList = new LinkedList<>();
                SearchRequest requests = new SearchRequest();
                requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.LIST_ITEM,
                        EntityType.DRIVE, EntityType.DRIVE_ITEM);

                LinkedList<SortProperty> sortProperties = new LinkedList<>();
                SortProperty sort = new SortProperty();
                sort.name = "LastModifiedTime";
                sort.isDescending = false;
                sortProperties.add(sort);
                requests.sortProperties = sortProperties;

                SearchQuery query = new SearchQuery();
                query.queryString = "path:\"" + filePath + "\"";

                requests.query = query;
                requests.from = 0;
                requests.size = 1;
                requestsList.add(requests);

                SearchEntityQueryCollectionPage page = graphClient.search().query(SearchEntityQueryParameterSet
                        .newBuilder().withRequests(requestsList).build()).buildRequest().post();

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    for (SearchResponse driveInPage : page.getCurrentPage()) {
                        if (driveInPage.hitsContainers != null) {
                            for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                                if (hitContainer.hits != null) {
                                    for (SearchHit hits : hitContainer.hits) {
                                        if (hits != null && hits.resource instanceof DriveItem) {
                                            driveItem = (DriveItem) hits.resource;
                                        }
                                    }
                                }
                            }
                        }
                    }
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


    private DriveItem getFolderByAbsolutePath(String filePath) {
        LOGGER.info("Searching for files with name: {}, selector: {}, page: {}, page size: {}", filePath);
        if (filePath != null) {
            filePath = filePath.replace("%20", " ");
            DriveItem driveItem = new DriveItem();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(true, null);

                LinkedList<SearchRequest> requestsList = new LinkedList<>();
                SearchRequest requests = new SearchRequest();
                requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.LIST_ITEM,
                        EntityType.DRIVE, EntityType.DRIVE_ITEM);

                LinkedList<SortProperty> sortProperties = new LinkedList<>();
                SortProperty sort = new SortProperty();
                sort.name = "LastModifiedTime";
                sort.isDescending = false;
                sortProperties.add(sort);
                requests.sortProperties = sortProperties;

                SearchQuery query = new SearchQuery();
                query.queryString = "path:\"" + filePath + "\"";

                requests.query = query;
                requests.from = 0;
                requests.size = 1;
                requestsList.add(requests);

                SearchEntityQueryCollectionPage page = graphClient.search().query(SearchEntityQueryParameterSet
                        .newBuilder().withRequests(requestsList).build()).buildRequest().post();

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    for (SearchResponse driveInPage : page.getCurrentPage()) {
                        if (driveInPage.hitsContainers != null) {
                            for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                                if (hitContainer.hits != null) {
                                    for (SearchHit hits : hitContainer.hits) {
                                        if (hits != null && hits.resource instanceof DriveItem) {
                                            DriveItem drive = (DriveItem) hits.resource;
                                            if(drive.webUrl != null && drive.webUrl.equals(filePath)){
                                                driveItem = (DriveItem) hits.resource;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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

    private DriveItem createFolderInPath(String target, Map<String, Object> response) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (target != null) {
                String absolutePathBuilder = DriveUtil.getRootUrl(target);
                DriveItem driveItemTarget = checkIfTargetFolderExist(absolutePathBuilder, payload);
                if ((boolean) payload.get("isFileExist")) {
                    target = DriveUtil.removeFirstSlash(DriveUtil.getRelativePath(target));
                    String[] folders = target.split("/");
                    //StringBuilder absolutePathBuilder = new StringBuilder("sites/"+driveItemTarget.parentReference.siteId+"/drive/root:");
                    StringBuilder relativePathBuilder = new StringBuilder();

                    for (String folderName : folders) {
                        Map<String, Object> check = new HashMap<>();
                        absolutePathBuilder = absolutePathBuilder + "/" + folderName;
                        relativePathBuilder.append("/").append(folderName);

                        // Check if the folder exists
                        checkIfTargetFileExist(absolutePathBuilder.toString(), check);

                        if (!(boolean) check.get("isFileExist")) {
                            // Create the folder
                            DriveItem driveItem = provider.getUserRequestBuilder(null, null).drives(driveItemTarget.parentReference.driveId).root().itemWithPath(FileUtil.getParentPath(relativePathBuilder.toString())).children().buildRequest().post(DriveUtil.getDriveItemForItem(folderName));
                            response.put("isFileExist", true);
                            return driveItem;
                        }
                    }
                } else {
                    payload.put(Constants.MESSAGE, "Provide Valid target Path");
                    payload.put(Constants.SUCCESS, false);
                    return new DriveItem();
                }
                return new DriveItem();
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            return new DriveItem();
        }
        return new DriveItem();
    }

}
