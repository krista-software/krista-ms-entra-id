package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.EntityType;
import com.microsoft.graph.models.SearchQuery;
import com.microsoft.graph.models.SearchRequest;
import com.microsoft.graph.models.SortProperty;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileEmailSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileEmailSearchDao.class);
    private final GraphServiceClientProvider provider;
    private final FileSearchImpl fileSearchImpl;


    @Inject
    public FileEmailSearchDao(GraphServiceClientProviderFactory factory, FileSearchImpl fileSearchImpl) {
        this.provider = factory.create();
        this.fileSearchImpl = fileSearchImpl;
    }

    public FreeForm getSearchByMail(String email, String user, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with filePath: {} {} ", email, user);
        FreeForm payload = new FreeForm();
        if (email == null || email.isEmpty()) {
            ResponseUtil.getInfoResponse(false, Constants.MAILID_CANNOT_BE_EMPTY, payload);
            return payload;
        }
        List<DrivePermissionImpl> resultPage = new ArrayList<>();  // Initialize the result page
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);
            LinkedList<SearchRequest> requestsList = buildSearchRequestList();
            SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

            if (page != null && !page.getCurrentPage().isEmpty()) {
                fileSearchImpl.extractDrivePermissionsForSearchByMail(resultPage, page, graphClient, email, user);
            }
            if (resultPage.size() > 0) {
                List<File> fileDetails = resultPage.stream().map(CatalogTypes::fromFile).collect(Collectors.toList());
                ResponseUtil.listOfFileEntityResponse(fileDetails, true, Constants.FILE_FOUND, payload);
            } else {
                ResponseUtil.getInfoResponse(false, Constants.FILE_NOT_FOUND, payload);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
            ResponseUtil.getInfoResponse(false, Constants.FAILED_TO_GET_BY_MAILID, payload);
        }
        return payload;
    }

    public LinkedList<SearchRequest> buildSearchRequestList() {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(requests);
        requestsList.add(requests);
        return requestsList;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.LIST, EntityType.DRIVE, EntityType.DRIVE_ITEM);

        // Fill in the rest of the request setup logic
        LinkedList<SortProperty> sortProperties = new LinkedList<>();
        SortProperty sort = new SortProperty();
        sort.name = "LastModifiedTime";
        sort.isDescending = false;
        sortProperties.add(sort);
        requests.sortProperties = sortProperties;
        return requests;
    }

    public void buildSearchRequestForRest(SearchRequest requests) {
        String domainName = fileSearchImpl.getDomainName();
        String siteDomainPath = fileSearchImpl.getDomainPath(domainName, false);
        SearchQuery query = new SearchQuery();
        query.queryString = domainName + " AND NOT path:\"" + siteDomainPath + "\"";
        requests.query = query;
        requests.from = 0;
        requests.size = 500;
    }
}
