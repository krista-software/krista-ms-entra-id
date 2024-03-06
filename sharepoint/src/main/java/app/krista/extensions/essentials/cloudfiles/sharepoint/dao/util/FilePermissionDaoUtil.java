package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.PermissionCollectionPage;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FilePermissionDaoUtil {

    private FilePermissionDaoUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionDaoUtil.class);

    public static final String DRIVE_ITEMS = "drive/items";
    public static final String ADDED_IN_SHARED = "ADDED IN SHARED";
    public static final String NOT_ADDED = "NOT ADDED";
    public static final String VISITORS = "Visitors";

    public static List<Map<String, Object>> getSharedPermissions(DriveItem driveItem, SharePointAttributes sharePointAttributes,
                                                                 GraphServiceClient<Request> graphServiceClient) {
        try {
            List<Map<String, Object>> mapList = new ArrayList<>();
            PermissionCollectionPage permissionCollectionPage = driveItem.permissions;
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null
                        && Objects.requireNonNullElse(permissionCollectionPage.additionalDataManager().get(Constants.ODATA_CONTEXT), "")
                        .toString().contains(DRIVE_ITEMS)) {
                    iterateAddDataPermission(driveItem, mapList, sharePointAttributes, graphServiceClient);
                } else {
                    iteratePermissionsFromDriveItem(driveItem, mapList, sharePointAttributes, graphServiceClient);
                }
                if (permissionCollectionPage.getNextPage() != null) {
                    permissionCollectionPage = permissionCollectionPage.getNextPage().buildRequest().get();
                } else {
                    break;
                }
            }
            return mapList.stream()
                    .distinct()
                    .collect(Collectors.toList());
        } catch (GraphServiceException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }


    private static void iterateAddDataPermission(DriveItem driveItem, List<Map<String, Object>> mapList, SharePointAttributes sharePointAttributes, GraphServiceClient<Request> graphServiceClient) {
        PermissionCollectionPage permissionCollectionPage = driveItem.permissions;
        String siteId = driveItem.parentReference != null ? driveItem.parentReference.siteId : null;
        for (Permission permission : permissionCollectionPage.getCurrentPage()) {
            if (permission.grantedToV2 != null) {
                Map<String, Object> sharedUser = new HashMap<>();
                String email = permission.grantedToV2.user != null
                        ? getMailFromGrantedTo(permission.grantedToV2, mapList, sharePointAttributes, graphServiceClient, siteId, permission) : "";
                if (email.equals(ADDED_IN_SHARED)) {
                    continue;
                } else if (email.equals(NOT_ADDED)) {
                    return;
                } else if (email.contains(VISITORS)) {
                    sharedUser.put(Constants.USER_EMAIL, email);
                    sharedUser.put(Constants.PERMISSION, Constants.READ);
                    mapList.add(sharedUser);
                    continue;
                }
                if (email.isBlank()) {
                    email = permission.grantedToV2.siteUser != null ? Objects.requireNonNullElse(permission.grantedToV2.siteUser
                            .additionalDataManager().get(Constants.EMAIL), permission.grantedToV2.siteUser.displayName).toString() : "";
                }
                sharedUser.put(Constants.USER_EMAIL, email.replace("\"", ""));
                sharedUser.put(Constants.PERMISSION, Constants.OWN);
                mapList.add(sharedUser);
            } else if (permission.grantedToIdentitiesV2 != null) {
                for (SharePointIdentitySet set : permission.grantedToIdentitiesV2) {
                    Map<String, Object> sharedUser = new HashMap<>();
                    String email = set.user != null ? getMailFromGrantedTo(set, mapList, sharePointAttributes, graphServiceClient, siteId, permission) : "";
                    if (email.equals(ADDED_IN_SHARED)) {
                        continue;
                    } else if (email.equals(NOT_ADDED)) {
                        return;
                    } else if (email.contains(VISITORS)) {
                        sharedUser.put(Constants.USER_EMAIL, email);
                        sharedUser.put(Constants.PERMISSION, Constants.READ);
                        mapList.add(sharedUser);
                        continue;
                    }
                    if (email.isBlank()) {
                        email = set.siteUser != null ? Objects.requireNonNullElse(set.siteUser.additionalDataManager().get(Constants.EMAIL), set.siteUser.displayName).toString() : "";
                    }
                    sharedUser.put(Constants.USER_EMAIL, email.replace("\"", ""));
                    sharedUser.put(Constants.PERMISSION, permission.roles != null ? toSentenceCase(permission.roles.get(0)) : "");
                    mapList.add(sharedUser);
                }
            }
        }
    }


    private static void iteratePermissionsFromDriveItem(DriveItem driveItem, List<Map<String, Object>> mapList, SharePointAttributes sharePointAttributes, GraphServiceClient<Request> graphServiceClient) {
        PermissionCollectionPage permissionCollectionPage = driveItem.permissions;
        String siteId = driveItem.parentReference != null ? driveItem.parentReference.siteId : null;
        for (Permission permission : permissionCollectionPage.getCurrentPage()) {
            if (permission.grantedTo != null) {
                Map<String, Object> sharedUser = new HashMap<>();
                String email = permission.grantedTo.user != null
                        ? getMailFromGrantedTo(permission.grantedTo, mapList, sharePointAttributes, graphServiceClient, siteId, permission)
                        : "";
                if (email.equals(ADDED_IN_SHARED)) {
                    continue;
                } else if (email.equals(NOT_ADDED)) {
                    return;
                } else if (email.endsWith(VISITORS)) {
                    sharedUser.put(Constants.USER_EMAIL, email);
                    sharedUser.put(Constants.PERMISSION, Constants.READ);
                    mapList.add(sharedUser);
                    continue;
                }
                sharedUser.put(Constants.USER_EMAIL, email.replace("\"", ""));
                sharedUser.put(Constants.PERMISSION, Constants.OWN);
                mapList.add(sharedUser);
            } else if (permission.grantedToIdentities != null) {
                for (IdentitySet set : permission.grantedToIdentities) {
                    Map<String, Object> sharedUser = new HashMap<>();
                    String email = set.user != null
                            ? getMailFromGrantedTo(set, mapList, sharePointAttributes, graphServiceClient, siteId, permission) : "";
                    if (email.equals(ADDED_IN_SHARED)) {
                        continue;
                    } else if (email.equals(NOT_ADDED)) {
                        return;
                    } else if (email.contains(VISITORS)) {
                        sharedUser.put(Constants.USER_EMAIL, email);
                        sharedUser.put(Constants.PERMISSION, Constants.READ);
                        mapList.add(sharedUser);
                        continue;
                    }
                    sharedUser.put(Constants.USER_EMAIL, email.replace("\"", ""));
                    sharedUser.put(Constants.PERMISSION, permission.roles != null ? toSentenceCase(permission.roles.get(0)) : "");
                    mapList.add(sharedUser);
                }
            }
        }
    }

    private static String getMailFromGrantedTo(IdentitySet set, List<Map<String, Object>> mapList, SharePointAttributes sharePointAttributes, GraphServiceClient<Request> graphServiceClient, String siteId, Permission permission) {
        try {
            if (set.user.additionalDataManager().get(Constants.EMAIL) != null) {
                return set.user.additionalDataManager().get(Constants.EMAIL).getAsString();
            } else {
                return set.user.displayName.contains(VISITORS)
                        ? set.user.displayName
                        : setUsersInShared(permission, set.user.displayName, mapList, siteId, sharePointAttributes, graphServiceClient).toString();
            }
        } catch (RuntimeException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            return NOT_ADDED;
        }
    }

    private static Object setUsersInShared(Permission permission, String displayName, List<Map<String, Object>> listSharedUser, String siteId, SharePointAttributes sharePointAttributes, GraphServiceClient<Request> graphServiceClient) {
        String siteName;
        boolean continueWithSiteId = false;
        siteName = siteId != null ? getSiteName(siteId, graphServiceClient) : extractSiteName(displayName);
        if (siteName == null) {
            continueWithSiteId = true;
        }
        Group group = findMatchingGroup(graphServiceClient, siteName, continueWithSiteId, siteId);
        if (group != null) {
            setUsersInSharedMap(displayName, group, listSharedUser, permission, sharePointAttributes, graphServiceClient);
            return ADDED_IN_SHARED;
        }
        return NOT_ADDED;
    }

    private static void setUsersInSharedMap(String displayName, Group group, List<Map<String, Object>> listSharedUser, Permission permission, SharePointAttributes sharePointAttributes, GraphServiceClient<Request> graphServiceClient) {
        try {
            if (displayName.endsWith("Owners")) {
                // Check owners:
                DirectoryObjectCollectionWithReferencesPage owners = graphServiceClient.groups(group.id).owners().buildRequest().get();
                setOwnersPermission(owners, listSharedUser, permission, sharePointAttributes);
            } else {
                // Check members:
                DirectoryObjectCollectionWithReferencesPage members = graphServiceClient.groups(group.id).members().buildRequest().get();
                setMembersPermission(members, listSharedUser, permission, sharePointAttributes);
            }
        } catch (RuntimeException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
    }

    private static void setMembersPermission(DirectoryObjectCollectionWithReferencesPage members, List<Map<String, Object>> listSharedUser, Permission permission, SharePointAttributes sharePointAttributes) {
        while (members != null && !members.getCurrentPage().isEmpty()) {
            for (DirectoryObject member : members.getCurrentPage()) {
                if (member instanceof User) {
                    Map<String, Object> sharedMembers = new HashMap<>();
                    User user = (User) member;
                    sharedMembers.put(Constants.USER_EMAIL, user.mail != null ? user.mail : " ");
                    sharedMembers.put(Constants.PERMISSION, permission.roles != null ? toSentenceCase(permission.roles.get(0)) : "");
                    listSharedUser.add(sharedMembers);
                }
            }
            if (members.getNextPage() != null) {
                members = members.getNextPage().buildRequest().get();
            } else {
                break;
            }
        }
    }

    private static void setOwnersPermission(DirectoryObjectCollectionWithReferencesPage owners, List<Map<String, Object>> listSharedUser, Permission permission, SharePointAttributes sharePointAttributes) {
        while (owners != null && !owners.getCurrentPage().isEmpty()) {
            for (DirectoryObject owner : owners.getCurrentPage()) {
                if (owner instanceof User) {
                    Map<String, Object> sharedOwner = new HashMap<>();
                    User user = (User) owner;
                    sharedOwner.put(Constants.USER_EMAIL, user.mail != null ? user.mail : " ");
                    sharedOwner.put(Constants.PERMISSION, Constants.OWN);
                    listSharedUser.add(sharedOwner);
                }
            }
            if (owners.getNextPage() != null) {
                owners = owners.getNextPage().buildRequest().get();
            } else {
                break;
            }
        }
    }

    public static String getOwnerPermissions(PermissionCollectionPage permissionCollectionPage, SharePointAttributes sharePointAttributes) {
        try {
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null
                        && Objects.requireNonNullElse(permissionCollectionPage.additionalDataManager()
                        .get(Constants.ODATA_CONTEXT), "").toString().contains(DRIVE_ITEMS)) {
                    Permission permission = permissionCollectionPage.getCurrentPage().get(permissionCollectionPage.getCurrentPage().size() - 1);
                    if (permission != null && permission.grantedToV2 != null && permission.grantedToV2.user != null) {
                        String email = permission.grantedToV2.user.additionalDataManager().get(Constants.EMAIL) != null
                                ? permission.grantedToV2.user.additionalDataManager().get(Constants.EMAIL).getAsString() : "";

                        return email.contains(sharePointAttributes.getMailId()) ? Constants.OWN : Constants.WRITE;
                    }
                } else {
                    Permission permissionGranted = permissionCollectionPage.getCurrentPage().get(permissionCollectionPage.getCurrentPage().size() - 1);
                    if (permissionGranted != null && permissionGranted.grantedTo != null && permissionGranted.grantedTo.user != null) {
                        String email = permissionGranted.grantedTo.user.additionalDataManager().get(Constants.EMAIL) != null
                                ? permissionGranted.grantedTo.user.additionalDataManager().get(Constants.EMAIL).getAsString() : permissionGranted.grantedTo.user.displayName;

                        return email.contains(sharePointAttributes.getMailId()) ? Constants.OWN : Constants.WRITE;
                    }
                    for (Permission permission : permissionCollectionPage.getCurrentPage()) {
                        if (permission != null && permission.grantedToIdentities != null && permission.roles != null) {
                            for (IdentitySet identity : permission.grantedToIdentities) {
                                if (identity.user != null) {
                                    String email = identity.user.additionalDataManager().get(Constants.EMAIL) != null
                                            ? identity.user.additionalDataManager().get(Constants.EMAIL).getAsString() : "";
                                    return email.contains(sharePointAttributes.getMailId()) ? permission.roles.toString() : null;
                                }
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
        } catch (GraphServiceException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
        return null;
    }

    private static String toSentenceCase(String role) {
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }


    private static String extractSiteName(String displayName) {
        return displayName != null ? displayName.split("\\s+")[0] : null;
    }

    public static Group findMatchingGroup(GraphServiceClient<Request> graphClient, String siteName, boolean continueWithSiteId, String siteId) {
        try {
            GroupCollectionPage groups = graphClient.groups().buildRequest().get();

            while (groups != null && !groups.getCurrentPage().isEmpty()) {
                for (Group group : groups.getCurrentPage()) {
                    if (!continueWithSiteId) {
                        if (group.mailNickname != null && group.mailNickname.equals(siteName)) {
                            return group;
                        }
                    } else {
                        if (group.proxyAddresses != null) {
                            for (String content : group.proxyAddresses) {
                                if (content.contains(siteId)) {
                                    return group;
                                }
                            }
                        }
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

    private static String getSiteName(String siteId, GraphServiceClient<Request> graphClient) {
        try {
            Site site = graphClient.sites(siteId).buildRequest().get();
            return site != null ? site.name : null;
        } catch (GraphServiceException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            return null;
        }
    }
}
