package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.IdentitySet;
import com.microsoft.graph.models.Permission;
import com.microsoft.graph.requests.PermissionCollectionPage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service class for retrieving shared permissions on files using Microsoft Graph API.
 * Provides a method to extract and format shared permission information from a PermissionCollectionPage.
 *
 * @author Vaibhav Choudhary
 */

public class FilePermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionService.class);

    private static String toSentenceCase(String role) {
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }

    public static String getOwnerPermissions(PermissionCollectionPage permissionCollectionPage, SharePointAttributes sharePointAttributes) {
        try {
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                Permission permissionGranted = permissionCollectionPage.getCurrentPage().get(permissionCollectionPage.getCurrentPage().size() - 1);
                String grantedToPermission = fetchPermissionFromGrantedTo(sharePointAttributes, permissionGranted);
                if (grantedToPermission != null) return grantedToPermission;
                String identityPermission = fetchPermissionFromIdentities(permissionCollectionPage, sharePointAttributes);
                if (identityPermission != null) return identityPermission;

                if (permissionCollectionPage.getNextPage() != null) {
                    permissionCollectionPage = permissionCollectionPage.getNextPage().buildRequest().get();
                } else {
                    break;
                }
            }
        } catch (GraphServiceException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
        return null;
    }

    @Nullable
    private static String fetchPermissionFromGrantedTo(SharePointAttributes sharePointAttributes, Permission permissionGranted) {
        if (permissionGranted != null && permissionGranted.grantedTo != null && permissionGranted.grantedTo.user != null) {
            String email = permissionGranted.grantedTo.user.additionalDataManager().get(Constants.EMAIL) != null
                    ? permissionGranted.grantedTo.user.additionalDataManager().get(Constants.EMAIL).getAsString() : permissionGranted.grantedTo.user.displayName;

            return Objects.requireNonNull(email).equals(sharePointAttributes.getMailId()) ? Constants.OWN : Constants.WRITE;
        }
        return null;
    }

    @Nullable
    private static String fetchPermissionFromIdentities(PermissionCollectionPage permissionCollectionPage, SharePointAttributes sharePointAttributes) {
        for (Permission permission : permissionCollectionPage.getCurrentPage()) {
            if (permission != null && permission.grantedToIdentities != null && permission.roles != null) {
                for (IdentitySet identity : permission.grantedToIdentities) {
                    if (identity.user != null) {
                        String email = identity.user.additionalDataManager().get(Constants.EMAIL) != null
                                ? identity.user.additionalDataManager().get(Constants.EMAIL).getAsString() : "";
                        return email.equals(sharePointAttributes.getMailId()) ? toSentenceCase(permission.roles.toString()) : " ";
                    }
                }
            }
        }
        return null;
    }

    public List<Map<String, Object>> getSharedWithPermissions(PermissionCollectionPage permissionCollectionPage) {
        try {
            List<Map<String, Object>> mapList = new ArrayList<>();
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null) {
                    permissionCollectionPage.getCurrentPage().stream()
                            .filter(permission -> permission.grantedTo != null || permission.grantedToIdentities != null)
                            .forEach(permission -> {
                                List<Map<String, Object>> sharedUsers = findUsersInMap(mapList, permission);
                                sharedUsers.forEach(sharedUser -> sharedUser.put("Permission", getPermissionString(permission)));
                            });
                }
                permissionCollectionPage = (permissionCollectionPage.getNextPage() != null) ?
                        permissionCollectionPage.getNextPage().buildRequest().get() : null;
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
            result.add(findOrCreateUser(mapList, Objects.requireNonNull(permission.grantedTo.user).displayName));
        } else if (permission.grantedToIdentities != null) {
            for (IdentitySet identitySet : permission.grantedToIdentities) {
                if (identitySet.user != null) {
                    result.add(findOrCreateUser(mapList, String.valueOf(identitySet.user.additionalDataManager().get("email")).replace("\"", "")));
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
            return "owner".equals(permission.roles.get(0)) ? Constants.OWN : toSentenceCase(permission.roles.get(0));
        }
        return "";
    }

}
