package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.IdentitySet;
import com.microsoft.graph.models.Permission;
import com.microsoft.graph.requests.PermissionCollectionPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for retrieving shared permissions on files using Microsoft Graph API.
 * Provides a method to extract and format shared permission information from a PermissionCollectionPage.
 *
 * @author Vaibhav Choudhary
 */

public class FilePermissionService {

    public List<Map<String, Object>> getSharedWithPermissions(PermissionCollectionPage permissionCollectionPage) {
        try {
            List<Map<String, Object>> mapList = new ArrayList<>();
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null) {
                    for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                        if (permission.grantedTo != null || permission.grantedToIdentities != null) {
                            List<Map<String, Object>> sharedUsers = findUsersInMap(mapList, permission);

                            for (Map<String, Object> sharedUser : sharedUsers) {
                                sharedUser.put("Permission", getPermissionString(permission));
                            }
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
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    private List<Map<String, Object>> findUsersInMap(List<Map<String, Object>> mapList, Permission permission) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (permission.grantedTo != null) {
            result.add(findOrCreateUser(mapList, permission.grantedTo.user.displayName));
        } else if (permission.grantedToIdentities != null) {
            for (IdentitySet identitySet : permission.grantedToIdentities) {
                if (identitySet.user != null) {
                    result.add(findOrCreateUser(mapList, identitySet.user.displayName));
                }
            }
        }
        return result;
    }

    private Map<String, Object> findOrCreateUser(List<Map<String, Object>> mapList, String displayName) {
        for (Map<String, Object> user : mapList) {
            String userEmail = (String) user.get("User Email");
            if (userEmail != null && userEmail.equals(displayName)) {
                return user;
            }
        }

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("User Email", displayName);
        mapList.add(newUser);
        return newUser;
    }

    private String getPermissionString(Permission permission) {
        if (permission.roles != null && !permission.roles.isEmpty()) {
            return "owner".equals(permission.roles.get(0)) ? "Read and Write" : permission.roles.get(0).toString();
        }
        return "";
    }

}
