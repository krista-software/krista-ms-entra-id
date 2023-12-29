package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import com.microsoft.graph.models.List;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SiteCollectionPage;
import okhttp3.Request;

import java.util.LinkedList;
import java.util.Map;

public class OperationsAreaUtilityService {

    private OperationsAreaUtilityService(){}

    public static List getList(GraphServiceClient<Request> graphClient, Site existingSite, String listName) {
        try {
            return graphClient.sites(existingSite.id).lists(listName).buildRequest().get();
        } catch (Exception cause) {
            return null;
        }
    }

    public static void processResponse(List list, Map<String, Object> payload) {
        if (list != null) {
            payload.put("File", CatalogTypes.fromFile(new ListImpl(list)));
            payload.put("Success", true);
            payload.put("Message", "List created successfully");
        } else {
            payload.put("Success", false);
            payload.put("Message", "Failed to create list");
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
            payload.put("Success", true);
            payload.put("Message", "Site updated successfully");
            payload.put("File", new SiteImpl(site, null)); // Include updated site information
        } else {
            payload.put("Success", false);
            payload.put("Message", "Failed to update site");
        }
    }
}
