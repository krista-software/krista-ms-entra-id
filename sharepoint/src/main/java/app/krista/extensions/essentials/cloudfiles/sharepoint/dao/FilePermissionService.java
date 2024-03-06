package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.IdentitySet;
import com.microsoft.graph.models.Permission;
import com.microsoft.graph.requests.PermissionCollectionPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Service class for retrieving shared permissions on files using Microsoft Graph API.
 * Provides a method to extract and format shared permission information from a PermissionCollectionPage.
 *
 * @author Vaibhav Choudhary
 */

public class FilePermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionService.class);

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
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage(), cause);
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> findUsersInMap(List<Map<String, Object>> mapList, Permission permission) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (permission.grantedTo != null) {
            result.add(findOrCreateUser(mapList, permission.grantedTo.user.displayName));
        } else if (permission.grantedToIdentities != null) {
            for (IdentitySet identitySet : permission.grantedToIdentities) {
                if (identitySet.user != null) {
                    result.add(findOrCreateUser(mapList, String.valueOf(identitySet.user.additionalDataManager().get("email"))));
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
            return "owner".equals(permission.roles.get(0)) ? Constants.READ_AND_WRITE : toSentenceCase(permission.roles.get(0));
        }
        return "";
    }

    private static String toSentenceCase(String role) {
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }

}
