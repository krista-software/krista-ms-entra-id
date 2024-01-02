package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;

import javax.inject.Inject;

/**
 * Service class for constructing Microsoft Graph API search queries and performing file searches.
 *
 * [Note : This class works as Utility class for all FileSearch Implementation methods.]
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileSearchImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchImpl.class);
    private final GraphServiceClientProvider clientProvider;

    @Inject
    public FileSearchImpl(GraphServiceClientProviderFactory factory) {
        this(factory.create());
    }

    public FileSearchImpl(GraphServiceClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public LinkedList<SearchRequest> buildSearchRequestList(String filePath){
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(filePath,requests);
        requestsList.add(requests);
        return requestsList;
    }

    public SearchQuery buildQueryString(String name, String selector) {
        SearchQuery query = new SearchQuery();

        if ("Extension".equals(selector)) {
            query.queryString = "isDocument=true AND filetype:" + name;
        } else if ("Owner".equals(selector)) {
            query.queryString = "isDocument=true AND CreatedBy:" + name;
        } else if ("Absolute Path".equals(selector)) {
            //isDocument=true AND
            query.queryString = "path:\"" + name + "\"";
        } else {
            query.queryString = "isDocument=true AND " + name;
        }
        return query;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.LIST_ITEM,
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
        SearchEntityQueryCollectionPage page = graphClient
                .search()
                .query(SearchEntityQueryParameterSet.newBuilder().withRequests(requestsList).build())
                .buildRequest()
                .post();
        return page;
    }

    public void extractDriveFromSearchResults(List<DriveImpl> files, SearchEntityQueryCollectionPage page){
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                files.add(new DriveImpl(driveItem, null, null, clientProvider));
                            }
                        }
                    }
                }
            }
        }
    }

    public void extractDrivePermissionsFromSearchResults(List<DrivePermissionImpl> resultPage, SearchEntityQueryCollectionPage page, String filePath) {
        for (SearchResponse driveInPage : page.getCurrentPage()) {
            if (driveInPage.hitsContainers != null) {
                for (SearchHitsContainer hitContainer : driveInPage.hitsContainers) {
                    if (hitContainer.hits != null) {
                        for (SearchHit hits : hitContainer.hits) {
                            if (hits != null && hits.resource instanceof DriveItem) {
                                DriveItem driveItem = (DriveItem) hits.resource;
                                if (driveItem.webUrl != null && driveItem.webUrl.equals(filePath)) {
                                    resultPage.add(new DrivePermissionImpl(driveItem, clientProvider));
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


}
