package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.PermissionCollectionPage;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilePermissionDaoUtil {

    // Permission For File Entity
    public static String getPermissions(PermissionCollectionPage permissionCollectionPage, SharePointAttributes sharePointAttributes,
                                        GraphServiceClient<Request> graphServiceClient) {

        String permissionResponse = null;

        if (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
            for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                // 1. Handle permissions granted to groups:
                if (permission.grantedTo != null && permission.grantedTo.user != null) {
                    String siteName = extractSiteName(permission.grantedTo.user.displayName);
                    if (siteName != null) {
                        try {
                            Group group = findMatchingGroup(graphServiceClient, siteName);

                            if (group != null) {
                                permissionResponse = checkUserMembership(graphServiceClient, group, sharePointAttributes.getMailId());
                                if (permissionResponse != null) {
                                    return permissionResponse; // Early return if found
                                }
                            }
                        } catch (Exception cause) {
                            return permissionResponse;
                        }
                    }
                }
            }
            for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                // 2. Check for permissions granted directly to the user:
                if (permission.grantedToIdentities != null) {
                    for (IdentitySet identitySet : permission.grantedToIdentities) {
                        if (identitySet != null && matchesEmailForIdentitySet(identitySet, sharePointAttributes.getMailId())) {
                            permissionResponse = permission.roles.get(0);
                        }
                    }
                }
            }
        }
        return permissionResponse; // Default to null if not found
    }


    private static boolean matchesEmail(DirectoryObject user, String email) {
        return user != null && ((User) user).mail != null &&
                ((User) user).mail.equals(email);
    }

    private static boolean matchesEmailForIdentitySet(IdentitySet user, String email) {
        return user != null && user.additionalDataManager().get("email") != null &&
                user.additionalDataManager().get("email").equals(email);
    }

    private static String extractSiteName(String displayName) {
        return displayName != null ? displayName.split("\\s+")[0] : null;
    }

    public static Group findMatchingGroup(GraphServiceClient<Request> graphClient, String siteName) {
        try {
            GroupCollectionPage groups = graphClient.groups().buildRequest().get();

            if (groups != null && !groups.getCurrentPage().isEmpty()) {
                for (Group group : groups.getCurrentPage()) {
                    if (group.displayName != null && group.displayName.equals(siteName)) {
                        return group;
                    }
                }
            }
        } catch (Exception cause) {
            return null;
        }
        return null; // Group not found
    }

    private static String checkUserMembership(GraphServiceClient<Request> graphClient, Group group, String email) {
        try {
            // Check owners:
            DirectoryObjectCollectionWithReferencesPage owners = graphClient.groups(group.id).owners().buildRequest().get();
            for (DirectoryObject owner : owners.getCurrentPage()) {
                if (matchesEmail(owner, email)) {
                    return "Read & Write"; // Adjust the role name if needed
                }
            }

            // Check members:
            DirectoryObjectCollectionWithReferencesPage members = graphClient.groups(group.id).members().buildRequest().get();
            for (DirectoryObject member : members.getCurrentPage()) {
                if (matchesEmail(member, email)) {
                    return "write"; // Adjust the role name if needed
                }
            }
        } catch (Exception cause) {
            throw new IllegalArgumentException();
        }
        return null; // User not found in group
    }


    // Shared With For File Entity
    public static List<Map<String, Object>> getSharedWithPermissions(PermissionCollectionPage permissionCollectionPage,
                                                                     GraphServiceClient<Request> graphClient) {
        try {
            List<Map<String, Object>> mapList = new ArrayList<>();
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                        findOwnersInMap(mapList, permission, graphClient, result);
                    }
                    for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                        findUsersInMap(mapList, permission, graphClient, result);
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

    private static List<Map<String, Object>> findUsersInMap(List<Map<String, Object>> mapList, Permission permission, GraphServiceClient<Request> graphClient, List<Map<String, Object>> result) {
        if (permission.grantedToIdentities != null) {
            for (IdentitySet identitySet : permission.grantedToIdentities) {
                if (identitySet.user != null) {
                    result.add(findOrCreateUser(mapList, String.valueOf(identitySet.user.additionalDataManager().get("email")), permission.roles.get(0)));
                }
            }
        }
        return result;
    }


    private static List<Map<String, Object>> findOwnersInMap(List<Map<String, Object>> mapList, Permission permission,
                                                             GraphServiceClient<Request> graphClient, List<Map<String, Object>> result) {
        if (permission.grantedTo != null) {
            addOwnersAndMembers(result, mapList, permission.grantedTo.user.displayName, graphClient);
        }
        return result;
    }

    private static void addOwnersAndMembers(List<Map<String, Object>> result, List<Map<String, Object>> mapList, String displayName, GraphServiceClient<Request> graphClient) {
        String siteName = extractSiteName(displayName);
        if (siteName != null) {
            try {
                // Add Group Owners Mail
                Group groupOwners = findMatchingGroup(graphClient, siteName);

                if (groupOwners != null) {
                    DirectoryObjectCollectionWithReferencesPage owners = graphClient.groups(groupOwners.id).owners().buildRequest().get();
                    for (DirectoryObject owner : owners.getCurrentPage()) {
                        result.add(findOrCreateUser(mapList, ((User) owner).mail, "Read And Write"));
                    }
                }

                // Add Group Members Mail
                Group groupMember = findMatchingGroup(graphClient, siteName);

                if (groupMember != null) {
                    DirectoryObjectCollectionWithReferencesPage members = graphClient.groups(groupMember.id).members().buildRequest().get();
                    for (DirectoryObject member : members.getCurrentPage()) {
                        result.add(findOrCreateUser(mapList, ((User) member).mail, "Write"));
                    }
                }
            } catch (Exception cause) {
            }
        }
    }

    private static Map<String, Object> findOrCreateUser(List<Map<String, Object>> mapList, String displayName, String role) {
        for (Map<String, Object> user : mapList) {
            String userEmail = (String) user.get("User Email");
            if (userEmail != null && userEmail.equals(displayName)) {
                return user;
            }
        }

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("User Email", displayName);
        newUser.put("Permission", role);
        mapList.add(newUser);
        return newUser;
    }

    private static String getPermissionString(Permission permission) {
        if (permission.roles != null && !permission.roles.isEmpty()) {
            return "owner".equals(permission.roles.get(0)) ? "Read and Write" : permission.roles.get(0).toString();
        }
        return "";
    }
}
