package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;

import java.util.HashMap;
import java.util.Map;

public class FileChangeResourceUtil {

    private FileChangeResourceUtil() {
    }

    private static Map<String, DriveItemDeltaCollectionPage> FILE_CHANGE_RESOURCE = new HashMap<>();
    private static Map<String, String> SUBSCRIPTION_STORE = new HashMap<>();

    public static void addFileMetaData(String subscriptionId, DriveItemDeltaCollectionPage page) {
        FILE_CHANGE_RESOURCE.put(subscriptionId, page);
    }

    public static DriveItemDeltaCollectionPage getFileChangeResource(String subscriptionId) {
        return FILE_CHANGE_RESOURCE.get(subscriptionId);
    }

    public static void addSubscription(String path, String subscriptionId) {
        SUBSCRIPTION_STORE.put(path, subscriptionId);
    }

    public static String getSubscriptionByPath(String path) {
        return SUBSCRIPTION_STORE.get(path);
    }

    public static void removeSubscriptionByID(String subscriptionId) {
        FILE_CHANGE_RESOURCE.remove(subscriptionId);
    }

    public static void removePathFromSubscriptionStore(String path) {
        SUBSCRIPTION_STORE.remove(path);
    }

    public static String getPathBySubscription(String subscriptionId) {
        return SUBSCRIPTION_STORE.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(subscriptionId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

}