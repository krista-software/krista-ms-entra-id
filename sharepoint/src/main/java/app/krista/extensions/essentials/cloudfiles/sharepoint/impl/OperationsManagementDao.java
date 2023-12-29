package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

@Service
public class OperationsManagementDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsManagementDao.class);

    private final GraphServiceClientProvider provider;

    @Inject
    public OperationsManagementDao(GraphServiceClientProviderFactory factory) {
        this(factory.create());
    }

    public OperationsManagementDao(GraphServiceClientProvider provider) {
        this.provider = provider;
    }

    public Map<String, Object> deleteFile(String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            DriveItem driveItem = checkIfFileExist(target, payload);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                target = DriveUtil.getRelativePath(target);
                provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId)
                        .root().itemWithPath(target).buildRequest().delete();
                payload.put(Constants.MESSAGE, "File Deleted Successfully");
                payload.put(Constants.SUCCESS, true);
            } else {
                payload.put(Constants.MESSAGE, "Provide Valid Path");
                payload.put(Constants.SUCCESS, false);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            payload.put(Constants.MESSAGE, "Invalid input provided: " + cause.getMessage());
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    protected DriveItem checkIfFileExist(String sourcePath, Map<String, Object> payload) {
        try {
            if (sourcePath != null) {
                DriveItem driveItem = getFileByAbsolutePath(sourcePath);
                if (driveItem != null && null != driveItem.parentReference) {
                    payload.put(Constants.SUCCESS, true);
                    return driveItem;
                } else {
                    payload.put(Constants.MESSAGE, "Provide Valid Path");
                    payload.put(Constants.SUCCESS, false);
                    return new DriveItem();
                }
            } else {
                payload.put(Constants.MESSAGE, "Provide Valid Path");
                payload.put(Constants.SUCCESS, false);
                return new DriveItem();
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put(Constants.SUCCESS, false);
            return new DriveItem();
        }
    }

    public Map<String, Object> downloadFiles(Boolean downloadAsLink, Boolean downloadAsKristaFile, String relativePath) {
        Map<String, Object> payload = new HashMap<>();
        if (relativePath != null) {
            List<File> files = new ArrayList<>();
            DriveItem driveItem = checkIfFileExist(relativePath, payload);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                relativePath = DriveUtil.getRelativePath(relativePath);
                downloadByDriveIDByPath(driveItem.parentReference.driveId, relativePath, downloadAsLink, downloadAsKristaFile, files, payload);
                if (payload.isEmpty()) {
                    payload.put(Constants.MESSAGE, "Provide Valid Path");
                    payload.put(Constants.SUCCESS, false);
                }
            } else {
                payload.put(Constants.MESSAGE, "Failed to download files due to an error. Please Provide a valid relative path.");
                payload.put(Constants.SUCCESS, false);
            }
        } else {
            payload.put(Constants.MESSAGE, "Provide a valid relative path.");
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    private DriveItem checkIfFileExist(String sourcePath, FreeForm payload) {
        try {
            if (sourcePath != null) {
                DriveItem driveItem = getFileByAbsolutePath(sourcePath);
                if (driveItem != null) {
                    ResponseUtil.addFileSize(driveItem.size != null ? driveItem.size : 0L, payload);
                    ResponseUtil.getInfoResponse(true, "File Found", payload);
                    return driveItem;
                } else {
                    ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                }
            } else {
                ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                return new DriveItem();
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
            return new DriveItem();
        }
        return new DriveItem();
    }

    private void downloadByDriveIDByPath(String driveID, String path, Boolean downloadAsLink, Boolean downloadAsKristaFile, List<File> files, Map<String, Object> payload) {
        try {
            DriveItem driveItem = (driveID != null) ? provider.getUserRequestBuilder(null, null).drives(driveID).root().itemWithPath(path).buildRequest().get()
                    : provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(path).buildRequest().get();
            if (driveItem != null && driveItem.folder == null) {
                if (downloadAsLink != null && downloadAsLink) {
                    payload.put(Constants.SUCCESS, true);
                    payload.put(Constants.MESSAGE, "To Download file Please Click on URL");
                    payload.put("Download Link", driveItem.additionalDataManager().get("@microsoft.graph.downloadUrl").getAsString());
                } else if (downloadAsKristaFile != null && downloadAsKristaFile && driveItem.size != null && driveItem.size > (1024 * 1024 * 30)) {
                    payload.put(Constants.SUCCESS, true);
                    payload.put(Constants.MESSAGE, "File Size is Greater than 30MB, Please Download file By URL");
                    payload.put("Download Link", driveItem.additionalDataManager().get("@microsoft.graph.downloadUrl").getAsString());

                } else {
                    if (driveItem.size != null && driveItem.size > (1024 * 1024 * 30)) { // 30MB
                        payload.put(Constants.SUCCESS, true);
                        payload.put(Constants.MESSAGE, "To Download file Please Click on URL");
                        payload.put("Download Link", "");
                    } else if (driveItem.size != null && driveItem.size < (1024 * 1024 * 30) && driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
                        assert driveItem.id != null;
                        InputStream inputStream = provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId).items(driveItem.id).content().buildRequest().get();
                        java.io.File content = DriveUtil.getFileFromInputStream(inputStream, driveItem.name);
                        app.krista.model.base.File kristaFile = CatalogTypes.toFile(content.getName(), content);
                        payload.put("Krista File", kristaFile);
                        payload.put(Constants.SUCCESS, true);
                        payload.put(Constants.MESSAGE, "To Download file Please Click on URL");
                    }
                }
            }
        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> renameFile(File renameFile, String targetFileName) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (renameFile != null) {
                if (!renameFile.extension.isBlank() && FileUtil.getBaseName(targetFileName).get(1) == null) {
                    payload.put(Constants.MESSAGE, "File Extension is Not Available");
                    payload.put(Constants.SUCCESS, false);
                    return payload;
                }
                DriveItem drive = checkIfFileExist(renameFile.absolutePath, payload);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    DriveItem driveItem = new DriveItem();
                    driveItem.name = targetFileName;
                    String relativePath = DriveUtil.getRelativePath(renameFile.absolutePath);
                    provider.getUserRequestBuilder(null, null).drives(drive.parentReference.driveId).root().itemWithPath(relativePath).buildRequest().patch(driveItem);
                    payload.put(Constants.MESSAGE, "Rename Successful");
                    payload.put(Constants.SUCCESS, true);
                } else {
                    payload.put(Constants.MESSAGE, "File Not Found");
                    payload.put(Constants.SUCCESS, false);
                }
            }
            return payload;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    public Map<String, Object> readTxtFile(String source) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {
                DriveItem drive = checkIfFileExist(source, payload);
                Map<String, Object> response = new HashMap<>();
                String path = DriveUtil.getRelativePath(source);
                getFileInfo(drive.parentReference.driveId, path, response);
                if ((boolean) response.get(Constants.SUCCESS)) {
                    DriveItem driveItem = (DriveItem) response.get("DriveItem");
                    if (driveItem.name != null && FileUtil.getBaseName(driveItem.name).get(1) != null && FileUtil.getBaseName(driveItem.name).get(1).equalsIgnoreCase("txt")) {

                        InputStream inputStream = provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId).items(driveItem.id).content().buildRequest().get();

                        payload.put("Content", FileUtil.readFile(inputStream));
                        payload.put(Constants.MESSAGE, "");
                        payload.put(Constants.SUCCESS, true);
                    } else {
                        payload.put(Constants.MESSAGE, "Invalid File Name or Invalid File Extension");
                        payload.put(Constants.SUCCESS, false);
                    }
                }
            }
            return payload;
        } catch (GraphServiceException | IllegalArgumentException cause) {
            payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put(Constants.SUCCESS, false);
        }
        return payload;
    }

    public void getFileInfo(String driveId, String path, Map<String, Object> response) {
        try {
            DriveItem driveItem = driveId != null ?
                    provider.getUserRequestBuilder(null, null).drives(driveId).root().itemWithPath(path).buildRequest().get() :
                    provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(path).buildRequest().get();
            response.put("DriveItem", driveItem);
            response.put(Constants.SUCCESS, true);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            response.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            response.put(Constants.SUCCESS, false);
        }
    }

    public Map<String, Object> uploadFiles(app.krista.model.base.File kristaFile, String targetPath) {
        Map<String, Object> payload = new HashMap<>();
        if (targetPath != null) {
            DriveItem drive = checkIfFileExist(targetPath, payload);
            if ((boolean) payload.get(Constants.SUCCESS)) {
                targetPath = DriveUtil.getRelativePath(targetPath);
                uploadFileByPathByDriveID(drive.parentReference.driveId, targetPath, kristaFile, payload);
                return payload;
            } else {
                payload.put(Constants.MESSAGE, "Provide Valid Path");
                payload.put(Constants.SUCCESS, false);
                return payload;
            }
        } else {
            payload.put(Constants.MESSAGE, "Provide Valid Path");
            payload.put(Constants.SUCCESS, false);
            return payload;
        }
    }

    private void uploadFileByPathByDriveID(String driveID, String targetPath, app.krista.model.base.File kristaFile, Map<String, Object> payload) {
        try {
            DriveItem driveItem = driveID != null ? provider.getUserRequestBuilder(null, null).drives(driveID).root().itemWithPath(targetPath).buildRequest().get()
                    : provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(targetPath).buildRequest().get();
            if (driveItem != null && driveItem.folder != null && driveItem.id != null) {
                java.io.File javaFile = CatalogTypes.fromFile(kristaFile);
                DriveUtil.listValidator(List.of(javaFile), "Failed to upload file. File is empty.");

                DriveItem uploadedDriveItem = driveID != null ?
                        provider.getUserRequestBuilder(null, null).drives(driveID).items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile))
                        : provider.getUserRequestBuilder(null, null).drive().items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile));
                if (uploadedDriveItem != null) {
                    app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(driveItem, null, null));
                    payload.put(Constants.FILE, fileEntity);
                    payload.put(Constants.MESSAGE, "File Uploaded Successfully");
                    payload.put(Constants.SUCCESS, true);
                }
            }
        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }

    }

    public Map<String, Object> uploadTxtFile(String fileContent, String target) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (target != null) {
                DriveItem drive = checkIfFileExist(DriveUtil.removeLastSegment(target), payload);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    String path = DriveUtil.getRelativePath(target);
                    String parentPath = FileUtil.getParentPath(path);
                    Map<String, Object> response = new HashMap<>();
                    getFileInfo(drive.parentReference.driveId, parentPath, response);
                    if ((boolean) response.get(Constants.SUCCESS)) {
                        DriveItem driveItem = (DriveItem) response.get("DriveItem");
                        if (driveItem.name != null && driveItem.folder != null) {

                            String[] stream = path.split("/");

                            String fileName = stream[stream.length - 1].contains(".txt") ? stream[stream.length - 1] : null;
                            if (fileName != null) {
                                java.io.File javaFile = FileUtil.createTxtFile(fileName, fileContent);
                                DriveItem uploadFile = provider.getUserRequestBuilder(null, null).drives(driveItem.parentReference.driveId).items(driveItem.id).itemWithPath(javaFile.getName()).content().buildRequest().put(DriveUtil.getByteArray(javaFile));
                                if (uploadFile != null) {
                                    app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = CatalogTypes.fromFile(new FileImpl(uploadFile, null, null));
                                    payload.put(Constants.FILE, fileEntity);
                                    payload.put(Constants.MESSAGE, "File id Uploaded Successfully");
                                    payload.put(Constants.SUCCESS, true);
                                }
                            } else {
                                payload.put(Constants.MESSAGE, "Invalid FileName");
                                payload.put(Constants.SUCCESS, false);
                            }
                        }
                    } else {
                        payload.put(Constants.MESSAGE, "Invalid Folder Path");
                        payload.put(Constants.SUCCESS, false);
                    }
                } else {
                    payload.put(Constants.MESSAGE, "Invalid Folder Path");
                    payload.put(Constants.SUCCESS, false);
                }
            } else {
                payload.put(Constants.MESSAGE, "Invalid Folder Path");
                payload.put(Constants.SUCCESS, false);
            }
            return payload;

        } catch (GraphServiceException | IllegalArgumentException | IOException cause) {
            payload.put(Constants.MESSAGE, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put(Constants.SUCCESS, false);
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

}
