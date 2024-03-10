package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemDeltaCollectionPage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileChangeResourceUtil {

    private FileChangeResourceUtil() {
    }

//    private static Map<String, DriveItemDeltaCollectionPage> FILE_CHANGE_RESOURCE = new HashMap<>();
//
//    private static Map<String, List<DriveItem>> OLD_FILE_RESOURCE = new HashMap<>();
//
//    public static void addFileMetaData(String routingURL, DriveItemDeltaCollectionPage page) {
//        FILE_CHANGE_RESOURCE.put(routingURL, page);
//    }
//
//    public static DriveItemDeltaCollectionPage getFileChangeResource(String routingURL) {
//        return FILE_CHANGE_RESOURCE.get(routingURL);
//    }
//
//    public static void addOldFileMetaData(String routingURL, List<DriveItem> driveItemList) {
//        OLD_FILE_RESOURCE.put(routingURL, driveItemList);
//    }
//
//    public static List<DriveItem> getOldFileChangeResource(String routingURL) {
//        return OLD_FILE_RESOURCE.get(routingURL);
//    }


    private static Map<String, DriveItemDeltaCollectionPage> FILE_CHANGE_RESOURCE = new HashMap<>();
    private static Map<String,String> SUBSCRIPTION_STORE = new HashMap<>();

    public static void addFileMetaData(String subscriptionId,DriveItemDeltaCollectionPage page){
        FILE_CHANGE_RESOURCE.put(subscriptionId,page);
    }

    public static DriveItemDeltaCollectionPage getFileChangeResource(String subscriptionId) {
        return FILE_CHANGE_RESOURCE.get(subscriptionId);
    }

    public static void addSubscription(String path, String subscriptionId){
        SUBSCRIPTION_STORE.put(path,subscriptionId);
    }

    public static String getSubscriptionByPath(String path){
        return SUBSCRIPTION_STORE.get(path);
    }

    public static String getPathBySubscription(String subscriptionId){
        return SUBSCRIPTION_STORE.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(subscriptionId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

}