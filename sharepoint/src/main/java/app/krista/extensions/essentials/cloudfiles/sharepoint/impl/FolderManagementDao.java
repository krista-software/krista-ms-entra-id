package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveSharedWithMeCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolderManagementDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderManagementDao.class);

    private final GraphServiceClientProvider provider;

    private final FileManagementDao fileManagementDao;

    @Inject
    public FolderManagementDao(GraphServiceClientProviderFactory factory, FileManagementDao fileManagementDao) {
        this(factory.create(), fileManagementDao);
    }

    public FolderManagementDao(GraphServiceClientProvider provider, FileManagementDao fileManagementDao) {
        this.provider = provider;
        this.fileManagementDao = fileManagementDao;
    }

    @NotNull
    public FreeForm getListOfFolders(String relativePath, String search, boolean useEmail, String accountID) {
        FreeForm payload = new FreeForm();
        try {
            List<FileService> files = new ArrayList<>();
            if (relativePath != null) {
                DriveItem driveItem = checkIfFileExist(relativePath, payload, useEmail, accountID);
                if ((boolean) payload.get(Constants.SUCCESS)) {
                    relativePath = DriveUtil.getRelativePath(relativePath);
                    getRootFilesByDriveIDPath(driveItem.parentReference.driveId, relativePath, files, useEmail, accountID, search, payload);
                } else {
                    ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                }
            } else {
                ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
        return payload;
    }

    public void getRootFilesByDriveIDPath(String driveID, String relativePath, List<FileService> files, boolean useEmail, String accountID, String search, FreeForm payload) {
        try {
            DriveItem driveItem = provider.getUserRequestBuilder(useEmail, accountID).drives(driveID).root().itemWithPath(relativePath).buildRequest().get();
            if (driveItem != null && driveItem.id != null && driveItem.folder != null) {
                DriveItemCollectionPage page = provider.getUserRequestBuilder(useEmail, accountID).drives(driveID).root().itemWithPath(relativePath).children().buildRequest().get();
                while (page != null && !page.getCurrentPage().isEmpty()) {
                    for (DriveItem driveInPage : page.getCurrentPage()) {
                        if (search != null && !search.isEmpty() && driveInPage.folder != null && driveInPage.name != null && driveInPage.name.contains(search)) {
                            files.add(new FileImpl(driveInPage, null, null));
                        }
                        if (search != null && search.isBlank() && driveInPage.folder != null) {
                            files.add(new FileImpl(driveInPage, null, null));
                        }
                    }
                    if (page.getNextPage() != null) {
                        page = page.getNextPage().buildRequest().get();
                    } else {
                        ResponseUtil.listOfFileEntityResponse(files.stream().map(CatalogTypes::fromFile).collect(Collectors.toList()), true, " ", payload);
                        break;
                    }
                }
            } else {
                ResponseUtil.getInfoResponse(false, "Provide Valid Folder Path", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
        }
    }


    private DriveItem checkIfFileExist(String sourcePath, FreeForm payload, boolean useEmail, String accountID) {
        DriveItem driveItem = new DriveItem();
        try {
            if (sourcePath != null) {
                driveItem = getFileByAbsolutePath(sourcePath);
                if (driveItem != null && driveItem.parentReference != null && driveItem.parentReference.driveId != null) {
                    ResponseUtil.addFileSize(driveItem.size != null ? driveItem.size : 0L, payload);
                    ResponseUtil.getInfoResponse(true, "File Found", payload);
                } else {
                    ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
                }
            } else {
                ResponseUtil.getInfoResponse(false, "Invalid Folder Path", payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            ResponseUtil.getInfoResponse(false, Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage(), payload);
            return new DriveItem();
        }
        return driveItem;
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
                requests.size = 50;
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
                                            DriveItem potentialFolder = (DriveItem) hits.resource;
                                            if (potentialFolder.webUrl != null && potentialFolder.webUrl.equals(filePath)) {
                                                driveItem = potentialFolder;
                                                break;  // Stop searching once the folder is found
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

}
