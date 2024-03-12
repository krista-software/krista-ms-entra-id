package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DriveImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Service class for constructing Microsoft Graph API search queries and performing file searches.
 * [Note : This class works as Utility class for all FileSearch Implementation methods.]
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileSearchImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchImpl.class);
    public static final String ADDED_DRIVE_PERMISSION_FOR_CREATED_BY_USER_WITH_EMAIL = "Added DrivePermission for created by user with email: {}";
    private final GraphServiceClientProvider clientProvider;
    private final SharePointAttributes sharePointAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final FileSearchUtilityService fileSearchUtilityService;


    @Inject
    public FileSearchImpl(GraphServiceClientProviderFactory factory, SharePointAttributes sharePointAttributes,
                          GraphServiceClientProviderFactory clientProviderFactory, FileSearchUtilityService fileSearchUtilityService) {
        this.clientProvider = factory.create();
        this.sharePointAttributes = sharePointAttributes;
        this.clientProviderFactory = clientProviderFactory;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }

    public LinkedList<SearchRequest> buildSearchRequestList(String filePath) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(filePath, requests);
        requestsList.add(requests);
        return requestsList;
    }

    public SearchQuery buildQueryString(String name, String selector) {
        SearchQuery query = new SearchQuery();
        String domainName = getDomainName();
        String personalDrivePath = getDomainPath(domainName, false);

        if ("Extension".equals(selector)) {
            query.queryString = "isDocument=true AND filetype:" + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Owner".equals(selector)) {
            query.queryString = "CreatedBy:" + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Absolute Path".equals(selector)) {
            String siteDomainPath = getDomainPath(domainName, true);
            name = handleAbsolutePath(name, siteDomainPath);
            query.queryString = "path:\"" + name + "\"";
        } else if ("Created By".equals(selector)) {
            query.queryString = "CreatedBy:" + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Modified By".equals(selector)) {
            query.queryString = "ModifiedBy:" + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Created".equals(selector)) {
            name = DriveUtil.doubleToDateConversion(name);
            query.queryString = "Created = " + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Modified".equals(selector)) {
            name = DriveUtil.doubleToDateConversion(name);
            query.queryString = "LastModifiedTime = " + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Name".equals(selector) || "Base Name".equalsIgnoreCase(selector)) {
            query.queryString = "isDocument=true AND fileName:" + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else if ("Size".equalsIgnoreCase(selector)) {
            query.queryString = "isDocument=true AND Size >= 1 Size <= " + name + " AND NOT path:\"" + personalDrivePath + "\"";
        } else {
            query.queryString = "isDocument=true AND " + name + " AND NOT path:\"" + personalDrivePath + "\"";
        }
        return query;
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
        String domainPath = null;
        if (flag) {
            domainPath = "https://" + domain + ".sharepoint.com/sites";
        } else if (!flag) {
            domainPath = "https://" + domain + "-my.sharepoint.com/personal/";
        }
        return domainPath;
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

    public void buildSearchRequestForRest(String filePath, SearchRequest requests) {
        SearchQuery query = new SearchQuery();
        query.queryString = "path:\"" + filePath + "\"";
        requests.query = query;
        requests.from = 0;
        requests.size = 25;
    }

    public SearchEntityQueryCollectionPage buildSearchQuery(GraphServiceClient<Request> graphClient, LinkedList<SearchRequest> requestsList) {
        return graphClient
                .search()
                .query(SearchEntityQueryParameterSet.newBuilder().withRequests(requestsList).build())
                .buildRequest()
                .post();
    }

    public void extractDriveFromSearchResults(List<DriveImpl> files, SearchEntityQueryCollectionPage page) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                if (driveItem != null && driveItem.webUrl != null && !driveItem.webUrl.contains("sharepoint.com/personal/")) {
                                    files.add(new DriveImpl(driveItem, null, clientProvider));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void extractDrivePermissionsFromSearchResults(List<DrivePermissionImpl> resultPage, SearchEntityQueryCollectionPage page,
                                                         String filePath, GraphServiceClient<Request> graphClient) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                if (driveItem.webUrl != null && driveItem.webUrl.equals(filePath)) {
                                    resultPage.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService, null));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public DriveItem findDriveItemByFilePath(SearchEntityQueryCollectionPage page, String filePath, DriveItem driveItem, boolean rootFolder) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem drive = (DriveItem) hits.resource;
                                assert drive.parentReference != null;
                                String driveId = drive.parentReference.driveId;
                                if (drive.webUrl != null && drive.webUrl.equals(filePath) && !rootFolder) {
                                    driveItem = (DriveItem) hits.resource;
                                    break;
                                } else if (drive.webUrl != null && drive.webUrl.contains(filePath) && rootFolder) {
                                    driveItem = (DriveItem) hits.resource;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return driveItem;
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

    public void extractDriveItemListFromSearchResults(List<DriveItem> files, SearchEntityQueryCollectionPage page) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                files.add(driveItem);
                            }
                        }
                    }
                }
            }
        }
    }


    public void extractDrivePermissionsForSearchByMail(List<DrivePermissionImpl> resultPage, SearchEntityQueryCollectionPage page,
                                                       GraphServiceClient<Request> graphClient, String email, String user) {
        try {
            for (SearchResponse driveInPage : page.getCurrentPage()) {
                if (driveInPage.hitsContainers != null) {
                    for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                        if (hitContainer.hits != null) {
                            for (SearchHit hits : hitContainer.hits) {
                                try {
                                    if (hits != null && hits.resource instanceof DriveItem) {
                                        DriveItem driveItem = (DriveItem) hits.resource;
                                        if (driveItem.webUrl != null && user.equals(Constants.CREATED_BY) && driveItem.createdBy != null
                                                && driveItem.createdBy.user != null
                                                && driveItem.createdBy.user.additionalDataManager() != null
                                                && driveItem.createdBy.user.additionalDataManager().get(Constants.EMAIL) != null
                                                && driveItem.createdBy.user.additionalDataManager().get(Constants.EMAIL).getAsString().equals(email)) {
                                            resultPage.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService, null));
                                            LOGGER.info(ADDED_DRIVE_PERMISSION_FOR_CREATED_BY_USER_WITH_EMAIL, email);
                                        } else if (driveItem.webUrl != null && user.equals(Constants.MODIFIED_BY) && driveItem.lastModifiedBy != null
                                                && driveItem.lastModifiedBy.user != null
                                                && driveItem.lastModifiedBy.user.additionalDataManager() != null
                                                && driveItem.lastModifiedBy.user.additionalDataManager().get(Constants.EMAIL) != null
                                                && driveItem.lastModifiedBy.user.additionalDataManager().get(Constants.EMAIL).getAsString().equals(email)) {
                                            resultPage.add(new DrivePermissionImpl(driveItem, graphClient, fileSearchUtilityService, null));
                                            LOGGER.info(ADDED_DRIVE_PERMISSION_FOR_CREATED_BY_USER_WITH_EMAIL , email);
                                        }
                                    }
                                } catch (IllegalStateException cause) {
                                    LOGGER.error("Error processing DriveItem: {}" , cause.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException cause) {
            LOGGER.error("An unexpected error occurred: {}" , cause.getMessage());
        }
    }

}
