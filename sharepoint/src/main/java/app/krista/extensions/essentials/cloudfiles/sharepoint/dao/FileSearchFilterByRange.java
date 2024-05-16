package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DrivePermissionImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.model.base.DateRange;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for searching files based on date and size ranges using Microsoft Graph API.
 * Provides methods to retrieve files modified within a specific date range or within a specific size range.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class FileSearchFilterByRange {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSearchFilterByRange.class);
    private final GraphServiceClientProvider clientProvider;
    private final FileSearchImpl fileSearchImpl;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SharePointAttributes sharePointAttributes;


    @Inject
    public FileSearchFilterByRange(GraphServiceClientProviderFactory factory, FileSearchImpl fileSearchImpl, FileSearchUtilityService fileSearchUtilityService, SharePointAttributes sharePointAttributes) {
        this.clientProvider = factory.create();
        this.fileSearchImpl = fileSearchImpl;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.sharePointAttributes = sharePointAttributes;
    }

    /**
     * Retrieves files modified within a specified date range, handling validation, potential errors, and returning matching files.
     *
     * @return A FreeForm object containing the list of matching files or error messages.
     */
    public FreeForm getFilesByModificationDate(DateRange modificationDateRange, boolean useEmail, String accountID, Double pageIndex, Double pageSize) {
        FreeForm payload = new FreeForm();
        try {
            if (modificationDateRange == null) {
                handleInvalidError(Constants.DATE_RANGE_CANT_BE_EMPTY, payload);
                return payload;
            }
            String fromDate = DriveUtil.getModifiedDate(modificationDateRange.getFrom());
            String toDate = DriveUtil.getModifiedDate(modificationDateRange.getTo());
            if (fromDate == null && toDate == null) {
                handleInvalidError(Constants.DATE_RANGE_CANT_BE_EMPTY, payload);
                return payload;
            }
            int from = pageIndex != null && pageIndex >= 0 ? pageIndex.intValue() : 0;
            int size = pageSize != null && pageSize <= 500 ? pageSize.intValue() : 500;

            List<DrivePermissionImpl> drivePermissions = getFileByDateRange(fromDate, toDate, payload, useEmail, accountID, from, size);
            if (drivePermissions != null) {
                ResponseUtil.listOfFileEntityResponse(drivePermissions.stream().map(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File::fromFile).collect(Collectors.toList()), true, "Files and Folders within specified date range is fetched successfully.", payload);
            } else {
                handleInvalidError(Constants.NO_FILE_FOUND_IN_DATE_RANGE, payload);
                return payload;
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            handleInvalidError(Constants.FILE_NOT_FOUND, payload);
            return payload;
        }
        return payload;
    }

    public List<DrivePermissionImpl> getFileByDateRange(String fromDate, String toDate, FreeForm payload, boolean useEmail, String accountID, int from, int size) {
        LOGGER.info("Searching for files with File Range: {} {}", fromDate, toDate);
        List<DrivePermissionImpl> resultPage = new ArrayList<>();
        try {
            GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(useEmail, accountID);
            List<SearchRequest> requestsList = buildSearchRequestListForDate(fromDate, toDate, from, size);
            SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

            if (page != null && !page.getCurrentPage().isEmpty()) {
                extractDrivePermissionsFromSearchResults(resultPage, page, graphClient);
            }

        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
            ResponseUtil.successResponse(false, Constants.PROVIDE_VALID_SIZE_RANGE, payload);
        }
        return resultPage;
    }

    /**
     * Retrieves files within a specified size range, handling validation, potential errors, and returning matching files.
     *
     * @return A FreeForm object containing the list of matching files or error messages.
     */
    public FreeForm getFilesBySizeRange(Double fromSize, Double toSize, boolean useEmail, String accountID, Double pageIndex, Double pageSize) {
        FreeForm payload = new FreeForm();
        try {
            if (!isValidDoubleRange(fromSize, toSize)) {
                handleInvalidError(Constants.FILE_SIZE_CANNOT_BE_NULL, payload);
            }
            int from = pageIndex != null && pageIndex >= 0 ? pageIndex.intValue() : 0;
            int size = pageSize != null && pageSize <= 500 ? pageSize.intValue() : 500;

            List<DrivePermissionImpl> drivePermissions = getFileBySizeRange(fromSize, toSize, payload, useEmail, accountID, from, size);
            if (drivePermissions != null && !drivePermissions.isEmpty()) {
                ResponseUtil.listOfFileEntityResponse(drivePermissions.stream().map(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File::fromFile).collect(Collectors.toList()), true, "Files and Folders within specified size range is fetched successfully.", payload);
            } else {
                handleInvalidError(Constants.NO_FILE_FOUND_IN_SIZE_RANGE, payload);
                return payload;
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            handleInvalidError(Constants.FILE_NOT_FOUND, payload);
            return payload;
        }
        return payload;
    }

    private void handleInvalidError(String message, FreeForm payload) {
        ResponseUtil.getInfoResponse(false, message, payload);
    }

    private boolean isValidDoubleRange(Double fromSize, Double toSize) {
        int from = (int) Math.round(fromSize);
        int to = (int) Math.round(toSize);
        return from != 0 && to != 0 && from < to;
    }

    public List<DrivePermissionImpl> getFileBySizeRange(Double fromSize, Double toSize, FreeForm payload, boolean useEmail, String accountID, int pageIndex, int pageSize) {
        LOGGER.info("Searching for files with File Range: {} {}", fromSize, toSize);
        List<DrivePermissionImpl> resultPage = new ArrayList<>();
        if (fromSize != null && toSize != null && fromSize != 0 && toSize != 0 && toSize > fromSize) {
            try {
                GraphServiceClient<Request> graphClient = clientProvider.getUserRequestBuilder(useEmail, accountID);
                List<SearchRequest> requestsList = buildSearchRequestList(fromSize, toSize, pageIndex, pageSize);
                SearchEntityQueryCollectionPage page = fileSearchImpl.buildSearchQuery(graphClient, requestsList);

                if (page != null && !page.getCurrentPage().isEmpty()) {
                    extractDrivePermissionsFromSearchResults(resultPage, page, graphClient);
                }

            } catch (GraphServiceException | IllegalArgumentException cause) {
                LOGGER.error("Error during file search: {}", cause.getMessage(), cause);
                ResponseUtil.successResponse(false, Constants.PROVIDE_VALID_SIZE_RANGE, payload);
            }
        } else {
            ResponseUtil.successResponse(false, Constants.PROVIDE_VALID_SIZE_RANGE, payload);
        }
        return resultPage;
    }

    public List<SearchRequest> buildSearchRequestList(Double fromSize, Double toSize, int pageIndex, int pageSize) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForRest(fromSize, toSize, requests, pageIndex, pageSize);
        requestsList.add(requests);
        return requestsList;
    }

    public List<SearchRequest> buildSearchRequestListForDate(String fromDate, String toDate, int pageIndex, int pageSize) {
        LinkedList<SearchRequest> requestsList = new LinkedList<>();
        SearchRequest requests = buildSearchRequest();
        buildSearchRequestForDate(fromDate, toDate, requests, pageIndex, pageSize);
        requestsList.add(requests);
        return requestsList;
    }

    public SearchRequest buildSearchRequest() {
        SearchRequest requests = new SearchRequest();
        requests.entityTypes = Arrays.asList(EntityType.SITE, EntityType.LIST, EntityType.DRIVE, EntityType.DRIVE_ITEM);

        LinkedList<SortProperty> sortProperties = new LinkedList<>();
        SortProperty sort = new SortProperty();
        sort.name = "LastModifiedTime";
        sort.isDescending = false;
        sortProperties.add(sort);
        requests.sortProperties = sortProperties;
        return requests;
    }

    public void buildSearchRequestForRest(Double fromSize, Double toSize, SearchRequest requests, int pageIndex, int pageSize) {
        SearchQuery query = new SearchQuery();
        int from = (int) Math.round(fromSize);
        int to = (int) Math.round(toSize);
        query.queryString = "size >= " + from + " AND size <= " + to;
        requests.query = query;
        requests.from = pageIndex;
        requests.size = pageSize;
    }

    public void buildSearchRequestForDate(String fromDate, String toDate, SearchRequest requests, int pageIndex, int pageSize) {
        String domainName = fileSearchImpl.getDomainName();
        String personalDrivePath = fileSearchImpl.getDomainPath(domainName, false);
        SearchQuery query = new SearchQuery();
        query.queryString = "LastModifiedTime >= " + fromDate + " AND LastModifiedTime <= " + toDate + " AND NOT path:\"" + personalDrivePath + "\"";
        requests.query = query;
        requests.from = pageIndex;
        requests.size = pageSize;
    }

    public void extractDrivePermissionsFromSearchResults(List<DrivePermissionImpl> resultPage, SearchEntityQueryCollectionPage page, GraphServiceClient<Request> graphClient) {
        page.getCurrentPage().stream()
                .filter(response -> response.hitsContainers != null)
                .flatMap(response -> response.hitsContainers.stream())
                .filter(container -> container.hits != null)
                .flatMap(container -> container.hits.stream())
                .filter(hits -> hits != null && hits.resource instanceof DriveItem)
                .map(hits -> new DrivePermissionImpl((DriveItem) hits.resource, graphClient, fileSearchUtilityService, sharePointAttributes, null))
                .forEach(resultPage::add);
    }

}



