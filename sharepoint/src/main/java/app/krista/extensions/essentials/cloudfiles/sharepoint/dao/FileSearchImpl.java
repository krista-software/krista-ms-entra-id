package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntitySearchMapper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.List;

/**
 * Service class for constructing Microsoft Graph API search queries and performing file searches.
 * [Note : This class works as Utility class for all FileSearch Implementation methods.]
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileSearchImpl {

    public static final String ADDED_DRIVE_PERMISSION_FOR_CREATED_BY_USER_WITH_EMAIL = "Added DrivePermission for created by user with email: {}";
    public static final String ADDED_DRIVE_PERMISSION_FOR_MODIFIED_BY_USER_WITH_EMAIL = "Added DrivePermission for modified by user with email: {}";
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchImpl.class);
    public static final String SHAREPOINT_COM_PERSONAL = "sharepoint.com/personal/";
    private final GraphServiceClientProvider clientProvider;
    private final SharePointAttributes sharePointAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final FileSearchUtilityService fileSearchUtilityService;


    @Inject
    public FileSearchImpl(SharePointAttributes sharePointAttributes, GraphServiceClientProviderFactory factory,
                          GraphServiceClientProviderFactory clientProviderFactory, FileSearchUtilityService fileSearchUtilityService) {
        this.clientProvider = factory.create();
        this.sharePointAttributes = sharePointAttributes;
        this.clientProviderFactory = clientProviderFactory;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    public String handleAbsolutePath(String name, String siteDomainPath) {
        name = name.replace(Constants.WHITE_SPACE, " ");
        if (!name.startsWith(siteDomainPath)) {
            name = siteDomainPath + name;
        }
        if (!name.contains(Constants.SHARED_DOCUMENT_PATH)) {
            name = name.replace(Constants.DOCUMENT_PATH, Constants.SHARED_DOCUMENT_PATH);
        }
        return name;
    }

    public String getDomainPath(String domain, boolean flag) {
        String domainPath;
        if (flag) {
            domainPath = "https://" + domain + ".sharepoint.com/sites";
        } else {
            domainPath = "https://" + domain + "-my.sharepoint.com/personal/";
        }
        return domainPath;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = List.of(EntityType.DRIVE_ITEM);
        return requests;
    }

    public SearchEntityQueryCollectionPage buildSearchQuery(GraphServiceClient<Request> graphClient, List<SearchRequest> requestsList) {
        return graphClient
                .search()
                .query(SearchEntityQueryParameterSet.newBuilder().withRequests(requestsList).build())
                .buildRequest()
                .post();
    }

    public void extractDriveFromSearchResults(List<FileService> files, SearchEntityQueryCollectionPage page, Map<String, Boolean> moreResult, int pageSize) {
        try {
            page.getCurrentPage().stream()
                    .filter(driveInPage -> driveInPage.hitsContainers != null)
                    .flatMap(driveInPage -> driveInPage.hitsContainers.stream())
                    .forEach(hitContainer -> {
                        if (hitContainer.hits != null) {
                            if (iterateSearchHits(files, moreResult, pageSize, hitContainer)) {
                            }
                        } else {
                            moreResult.put(Constants.MORE_RESULT_AVAILABLE, Boolean.FALSE);
                        }
                    });
        } catch (RuntimeException cause) {
            LOGGER.error("Search Error : {}", cause.getMessage());
        }
    }

    private boolean iterateSearchHits(List<FileService> files, Map<String, Boolean> moreResult, int pageSize, SearchHitsContainer hitContainer) {
        Set<String> fileIdSet = new HashSet<>();
        for (SearchHit hits : Objects.requireNonNull(hitContainer.hits)) {
            if (hits != null && hits.resource instanceof DriveItem) {
                DriveItem driveItem = (DriveItem) hits.resource;
                if (driveItem.webUrl != null && !driveItem.webUrl.contains(SHAREPOINT_COM_PERSONAL) && (!fileIdSet.contains(driveItem.id))) {
                    fileIdSet.add(driveItem.id);
                    if (files.size() == pageSize) {
                        return true;
                    }
                    files.add(new FileEntitySearchMapper(driveItem, null, getUserRequestBuilder()));
                }
            }
        }
        if (files.size() < pageSize && Boolean.FALSE.equals(hitContainer.moreResultsAvailable)) {
            moreResult.put(Constants.MORE_RESULT_AVAILABLE, Boolean.FALSE);
            return true;
        }
        return false;
    }

    public String getDomainName() {
        String domainName = null;
        if (sharePointAttributes.getMailId() != null && !sharePointAttributes.getMailId().isBlank()) {
            DriveItemCollectionPage page = clientProviderFactory.create(sharePointAttributes)
                    .getGraphServiceClientForAdmin()
                    .me().drive().root().children().buildRequest().get();

            if (page != null && !page.getCurrentPage().isEmpty()) {
                for (DriveItem driveInPage : page.getCurrentPage()) {
                    if (driveInPage != null && driveInPage.webUrl != null) {
                        domainName = FileUtil.getExtractBaseUrl(driveInPage.webUrl);
                        break;
                    }
                }
            }
        }
        return domainName;
    }


    public void extractDrivePermissionsForSearchByMail(List<DrivePermissionImpl> resultPage, SearchEntityQueryCollectionPage page,
                                                       GraphServiceClient<Request> graphClient, String email, String user) {
        try {
            page.getCurrentPage().stream()
                    .filter(driveInPage -> driveInPage.hitsContainers != null)
                    .flatMap(driveInPage -> driveInPage.hitsContainers.stream())
                    .filter(hitContainer -> hitContainer.hits != null)
                    .flatMap(hitContainer -> hitContainer.hits.stream())
                    .forEach(hit -> getResultSearchByMail(resultPage, graphClient, email, user, hit));
        } catch (RuntimeException cause) {
            LOGGER.error("An unexpected error occurred: {}", cause.getMessage());
        }
    }


    private void getResultSearchByMail(List<DrivePermissionImpl> resultPage, GraphServiceClient<Request> graphClient, String email, String user, SearchHit hits) {
        try {
            if (hits != null && hits.resource instanceof DriveItem) {
                DriveItem driveItem = (DriveItem) hits.resource;
                if (driveItem.webUrl != null && user.equals(Constants.CREATED_BY) && driveItem.createdBy != null && driveItem.createdBy.user != null && driveItem.createdBy.user.additionalDataManager().get(Constants.EMAIL) != null && driveItem.createdBy.user.additionalDataManager().get(Constants.EMAIL).getAsString().equals(email)) {
                    resultPage.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService, sharePointAttributes, null));
                    LOGGER.info(ADDED_DRIVE_PERMISSION_FOR_CREATED_BY_USER_WITH_EMAIL, email);
                } else if (driveItem.webUrl != null && user.equals(Constants.MODIFIED_BY) && driveItem.lastModifiedBy != null && driveItem.lastModifiedBy.user != null && driveItem.lastModifiedBy.user.additionalDataManager().get(Constants.EMAIL) != null && driveItem.lastModifiedBy.user.additionalDataManager().get(Constants.EMAIL).getAsString().equals(email)) {
                    resultPage.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService, sharePointAttributes, null));
                    LOGGER.info(ADDED_DRIVE_PERMISSION_FOR_MODIFIED_BY_USER_WITH_EMAIL, email);
                }
            }
        } catch (IllegalStateException cause) {
            LOGGER.error("Error processing DriveItem: {}", cause.getMessage());
        }
    }

    private GraphServiceClient<Request> getUserRequestBuilder() {
        return clientProvider.getUserRequestBuilder(null, null);
    }

}
