package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
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
public class FileEmailSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileEmailSearchDao.class);
    private final GraphServiceClientProvider provider;
    private final FileSearchImpl fileSearchImpl;


    @Inject
    public FileEmailSearchDao(GraphServiceClientProviderFactory factory, FileSearchImpl fileSearchImpl) {
        this.provider = factory.create();
        this.fileSearchImpl = fileSearchImpl;
    }

    /**
     * Searches for files associated with a given email or user, handling potential errors and returning matching files.
     *
     * @return A FreeForm object containing the list of matching files or error messages.
     */
    public FreeForm getSearchByMail(String email, String user, boolean useEmail, String accountID) {
        LOGGER.info("Searching for files with filePath: {} {} ", email, user);
        FreeForm payload = new FreeForm();
        if (email == null || email.isEmpty()) {
           return ResponseUtil.getInfoResponse(false, Constants.MAILID_CANNOT_BE_EMPTY, payload);
        }
        if (!ResponseUtil.isValidEmail(email)) {
            return ResponseUtil.getInfoResponse(false, Constants.INVALID_EMAIL, payload);
        }
        List<DrivePermissionImpl> resultPage = new ArrayList<>();  // Initialize the result page
        try {
            GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(useEmail, accountID);
            int index = 0;
            int from = 0;
            int size = 500;

            SearchEntityQueryCollectionPage page = getSearchResult(graphClient, email, from, size);

            if (page != null && !page.getCurrentPage().isEmpty()) {

                do {
                    fileSearchImpl.extractDrivePermissionsForSearchByMail(resultPage, page, graphClient, email, user);
                    index++;
                    from = (index * size) + 1;
                    page = getSearchResult(graphClient, email, from, size);
                } while (ResponseUtil.moreResultsAvailable(page));
            }
            if (!resultPage.isEmpty()) {
                List<File> fileDetails = resultPage.stream().map(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File::fromFile).collect(Collectors.toList());
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

    private SearchEntityQueryCollectionPage getSearchResult(GraphServiceClient<Request> graphClient, String email, int from, int size) {
        LinkedList<SearchRequest> requestsList = buildSearchRequestList(email, from, size);
        return fileSearchImpl.buildSearchQuery(graphClient, requestsList);
    }

    public LinkedList<SearchRequest> buildSearchRequestList(String email, int from, int size) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(requests, email, from, size);
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

    public void buildSearchRequestForRest(SearchRequest requests, String email, int from, int size) {
        SearchQuery query = new SearchQuery();
        query.queryString = removeDomainFromEmail(email);
        requests.query = query;
        requests.from = from;
        requests.size = size;
    }

    private String removeDomainFromEmail(String email) {
        return email.substring(0, email.indexOf("@"));
    }
}
