package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DriveSharedPermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
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
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilePermissionDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionDao.class);
    private final GraphServiceClientProvider clientProvider;
    private final FileSearchImpl fileSearchImpl;
    private final SharePointAttributes sharePointAttributes;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public FilePermissionDao(GraphServiceClientProviderFactory clientProviderFactory, FileSearchImpl fileSearchImpl,
                             SharePointAttributes sharePointAttributes, FileSearchUtilityService fileSearchUtilityService) {
        this.clientProvider = clientProviderFactory.create();
        this.fileSearchImpl = fileSearchImpl;
        this.sharePointAttributes = sharePointAttributes;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    /**
     * Retrieves shared permissions for a file at the specified path, handling path validation, potential errors, and returning shared permission details.
     * @return A FreeForm object containing shared permission information or error messages.
     */
    public FreeForm getSharedPermission(String filePath, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with filePath: {} ", filePath);
        FreeForm payload = new FreeForm();
        if (filePath != null) {
            filePath = filePath.replace(Constants.WHITE_SPACE, " ");
            List<DriveSharedPermissionImpl> resultPage = new ArrayList<>();  // Initialize the result page
            try {
                GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(useEmail, accountID);
                LinkedList<SearchRequest> requestsList = buildSearchRequestListWithAbsolutePath(filePath);
                SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    extractDriveFromSearchResultsUsingPath(resultPage, page, filePath, graphClient);
                }

                if (resultPage.size() > 0) {
                    ResponseUtil.listOfFileEntityResponse(resultPage.stream().map(CatalogTypes::fromFile)
                            .collect(Collectors.toList()), true, Constants.FILE_FOUND, payload);
                } else {
                    ResponseUtil.getInfoResponse(false , Constants.PROVIDE_VALID_SOURCE_PATH,  payload);
                }
            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {} {} ", cause.getMessage(), cause);
                ResponseUtil.getInfoResponse(false , Constants.FAILED_GET_FILE_FROM_INPUT_STREAM_FILE_NAME_EMPTY,  payload);
            }
        }
        return payload;
    }

    public LinkedList<SearchRequest> buildSearchRequestListWithAbsolutePath(String filePath) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = fileSearchImpl.buildSearchRequest();
        buildSearchRequestForRest(filePath, requests);
        requestsList.add(requests);
        return requestsList;
    }

    public void extractDriveFromSearchResultsUsingPath(List<DriveSharedPermissionImpl> files, SearchEntityQueryCollectionPage page, String filePath, GraphServiceClient<Request> graphClient) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                if (driveItem != null && driveItem.webUrl != null && !driveItem.webUrl.contains("sharepoint.com/personal/")
                                        && driveItem.webUrl.contains(filePath)) {
                                    files.add(new DriveSharedPermissionImpl(driveItem, graphClient, sharePointAttributes , fileSearchUtilityService));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void buildSearchRequestForRest(String filePath, SearchRequest requests) {
        String domainName = fileSearchImpl.getDomainName();
        String siteDomainPath = fileSearchImpl.getDomainPath(domainName, true);
        filePath = fileSearchImpl.handleAbsolutePath(filePath, siteDomainPath);
        SearchQuery query = new SearchQuery();
        query.queryString = "path:\"" + filePath + "\"";
        requests.query = query;
        requests.from = 0;
        requests.size = 25;
    }



}
