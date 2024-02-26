package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.ListImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.SiteImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.models.List;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SiteCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;

import java.util.LinkedList;
import java.util.Map;

/**
 * Utility service class for performing operations related to SharePoint sites and lists
 * using Microsoft Graph API. This class provides methods for retrieving lists, processing responses,
 * obtaining site information, and validating site and list names.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class OperationsAreaUtilityService {

    private OperationsAreaUtilityService() {
    }

    public static List getList(GraphServiceClient<Request> graphClient, Site existingSite, String listName) {
        try {
            return graphClient.sites(existingSite.id).lists(listName).buildRequest().get();
        } catch (Exception cause) {
            return null;
        }
    }

    public static void processResponse(List list, Map<String, Object> payload, String response) {
        if (list != null) {
            payload.put(Constants.FILE, File.fromFile(new ListImpl(list)));
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, "List " + response + " successfully");
        } else {
            payload.put(Constants.SUCCESS, false);
            payload.put(Constants.MESSAGE, Constants.FAILED_LIST_CREATION);
        }
    }

    public static Site getSite(GraphServiceClient<Request> graphClient, String siteName) {
        try {
            LinkedList<Option> requestOptions = new LinkedList<>();
            requestOptions.add(new QueryOption("search", siteName));
            SiteCollectionPage sites = graphClient.sites().buildRequest(requestOptions).get();

            Site existingSite = null;
            if (sites != null) {
                for (Site site : sites.getCurrentPage()) {
                    if (site.displayName != null && site.displayName.equals(siteName)) {
                        existingSite = site;
                        break;
                    }
                }
            }
            return existingSite;
        } catch (Exception cause) {
            return null;
        }
    }

    public static void processResponse(Site site, Map<String, Object> payload) {
        if (site != null) {
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.SITE_UPDATED_SUCCESSFULLY);
            payload.put(Constants.FILE, File.fromFile(new SiteImpl(site))); // Include updated site information
        } else {
            payload.put(Constants.SUCCESS, false);
            payload.put(Constants.MESSAGE, Constants.FAILED_TO_UPDATE_SITE);
        }
    }

    public boolean validateSiteAndListNames(String siteName, String listName, Map<String, Object> payload) {
        if (siteName == null || siteName.isEmpty()) {
            payload.put(Constants.MESSAGE, Constants.SITE_NAME_CANNOT_BE_NULL);
            payload.put(Constants.SUCCESS, false);
            return false;
        }

        if (listName == null || listName.isEmpty()) {
            payload.put(Constants.MESSAGE, Constants.LIST_NAME_CANNOT_BE_NULL);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true; // Validation successful
    }

    public boolean validateSiteAndListAndUpdatedNames(String siteName, String listName, Map<String, Object> payload,
                                                      String displayName) {
        if (!validateSiteAndListNames(siteName, listName, payload)) {
            return false;
        }

        if (displayName == null || displayName.isEmpty()) {
            payload.put(Constants.MESSAGE, Constants.UPDATED_LIST_NAME_CANNOT_BE_NULL);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true; // Validation successful
    }

    public boolean validateSiteNames(String siteName, Map<String, Object> payload) {
        if (siteName == null || siteName.isEmpty()) {
            payload.put(Constants.MESSAGE, Constants.SITE_NAME_CANNOT_BE_NULL);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true;
    }

    public boolean validateSiteAndUpdatedNames(String siteName, String newSiteName, Map<String, Object> payload) {
        if (!validateSiteNames(siteName, payload)) {
            return false;
        }
        if (newSiteName == null || newSiteName.isEmpty()) {
            payload.put(Constants.MESSAGE, Constants.UPDATED_SITE_NAME_CANNOT_BE_NULL);
            payload.put(Constants.SUCCESS, false);
            return false;
        }
        return true;
    }

}
