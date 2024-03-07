package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.GroupImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.ListImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.List;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.SiteCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsAreaUtilityService.class);


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

    public static Group getGroup(GraphServiceClient<Request> graphClient, String groupName) {
        try {
            LinkedList<Option> requestOptions = new LinkedList<>();
         //   requestOptions.add(new QueryOption("search", groupName));
            GroupCollectionPage groups = graphClient.groups().buildRequest().get();

            Group existingSite = null;
            if (groups != null) {
                for (Group group : groups.getCurrentPage()) {
                    if (group.displayName != null && group.displayName.equals(groupName)) {
                        existingSite = group;
                        break;
                    }
                }
            }
            return existingSite;
        } catch (Exception cause) {
            return null;
        }
    }

    public static Group findMatchingGroup(GraphServiceClient<Request> graphClient, String siteName) {
        try {
            GroupCollectionPage groups = graphClient.groups().buildRequest().get();

            while (groups != null && !groups.getCurrentPage().isEmpty()) {
                for (Group group : groups.getCurrentPage()) {

                        if (group.displayName != null && group.displayName.equals(siteName)) {
                            return group;
                        }

                }
                if (groups.getNextPage() != null) {
                    groups = groups.getNextPage().buildRequest().get();
                } else {
                    break;
                }
            }
        } catch (Exception cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            return null;
        }
        return null; // Group not found
    }

    public static void processResponse(File file, Map<String, Object> payload) {
        if (file != null) {
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.GROUP_UPDATED_SUCCESSFULLY);
            payload.put(Constants.FILE, file); // Include updated site information
        } else {
            payload.put(Constants.SUCCESS, false);
            payload.put(Constants.MESSAGE, Constants.FAILED_TO_UPDATE_GROUP_PLEASE_TRY_AGAIN_LATER);
        }
    }

    public static void processResponseGroup(Group group, Map<String, Object> payload) {
        if (group != null) {
            payload.put(Constants.SUCCESS, true);
            payload.put(Constants.MESSAGE, Constants.GROUP_UPDATED_SUCCESSFULLY);
            payload.put(Constants.FILE, File.fromFile(new GroupImpl(group,null))); // Include updated site information
        } else {
            payload.put(Constants.SUCCESS, false);
            payload.put(Constants.MESSAGE, Constants.FAILED_TO_UPDATE_GROUP_PLEASE_TRY_AGAIN_LATER);
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
