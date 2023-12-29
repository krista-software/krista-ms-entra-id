package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Permission;
import com.microsoft.graph.requests.PermissionCollectionPage;

import java.util.*;

public class FilePermissionService {

    public List<Map<String, Object>> getSharedWithPermissions(PermissionCollectionPage permissionCollectionPage) {
        try {
            List<Map<String, Object>> mapList = new ArrayList<>();
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null) {
                    for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                        if (permission.grantedTo != null) {
                            Map<String, Object> sharedUser = new HashMap<>();
                            sharedUser.put("User Email", permission.grantedTo.user != null ? permission.grantedTo.user.displayName : "");
                            sharedUser.put("Permission", (permission.roles != null && !permission.roles.isEmpty()) ?
                                    ("owner".equals(permission.roles.get(0)) ? "Read and Write" : permission.roles.get(0).toString()) : "");
                            mapList.add(sharedUser);
                        }
                    }
                }
                if (permissionCollectionPage.getNextPage() != null) {
                    permissionCollectionPage = permissionCollectionPage.getNextPage().buildRequest().get();
                } else {
                    break;
                }
            }
            return mapList;
        } catch (GraphServiceException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }
}
