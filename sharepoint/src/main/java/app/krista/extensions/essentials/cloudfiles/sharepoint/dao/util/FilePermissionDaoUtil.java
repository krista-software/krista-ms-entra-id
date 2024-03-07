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

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FilePermissionDaoUtil {

    private FilePermissionDaoUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissionDaoUtil.class);

    public static final String DRIVE_ITEMS = "drive/items";
    public static final String ADDED_IN_SHARED = "ADDED IN SHARED";
    public static final String NOT_ADDED = "NOT ADDED";
    public static final String VISITORS = "Visitors";

    public static List<Map<String, Object>> getSharedPermissions(DriveItem driveItem,
                                                                 GraphServiceClient<Request> graphServiceClient) {
        try {
            List<Map<String, Object>> mapList = new ArrayList<>();
            PermissionCollectionPage permissionCollectionPage = driveItem.permissions;
            while (permissionCollectionPage != null && !permissionCollectionPage.getCurrentPage().isEmpty()) {
                if (permissionCollectionPage.additionalDataManager() != null
                        && Objects.requireNonNullElse(permissionCollectionPage.additionalDataManager().get(Constants.ODATA_CONTEXT), "")
                        .toString().contains(DRIVE_ITEMS)) {
                    iterateAddDataPermission(driveItem, mapList, graphServiceClient);
                } else {
                    iteratePermissionsFromDriveItem(driveItem, mapList, graphServiceClient);
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
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS,cause.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * This method collects mailId of user which is deleted from records but its mailId exist and Files is shared to respective mailID
     *
     * @param sharePointIdentitySet contains user information
     * @return email
     */

    private static String getSiteUserWithNoRecords(
            SharePointIdentitySet sharePointIdentitySet) {
        return sharePointIdentitySet.siteUser != null ? Objects.requireNonNullElse(sharePointIdentitySet.siteUser
                .additionalDataManager().get(Constants.EMAIL), sharePointIdentitySet.siteUser.displayName).toString() : "";
    }

    private static void iterateAddDataPermission(DriveItem driveItem, List<Map<String, Object>> mapList, GraphServiceClient<Request> graphServiceClient) {
        PermissionCollectionPage permissionCollectionPage = driveItem.permissions;
        String siteId = driveItem.parentReference != null ? driveItem.parentReference.siteId : null;

        permissionCollectionPage.getCurrentPage().forEach(permission -> {
            if (permission.grantedToV2 != null) {
                handlePermissionV2(permission.grantedToV2, mapList, graphServiceClient, siteId, permission, false);
            } else if (permission.grantedToIdentitiesV2 != null) {
                permission.grantedToIdentitiesV2.forEach(set -> handlePermissionV2(set, mapList, graphServiceClient, siteId, permission, true));
            }
        });
    }

    private static void handlePermissionV2(IdentitySet grantedToV2, List<Map<String, Object>> mapList, GraphServiceClient<Request> graphServiceClient, String siteId, Permission permission, boolean isIdentitySet) {
        String email = grantedToV2.user != null ? getMailFromGrantedTo(grantedToV2, mapList, graphServiceClient, siteId, permission) : "";
        if (email.equals(ADDED_IN_SHARED) || email.equals(NOT_ADDED)) {
            return;
        }
        if (email.contains(VISITORS)) {
            Map<String, Object> sharedUser = new HashMap<>();
            setSharedUser(sharedUser, email, Constants.READ);
            mapList.add(sharedUser);
            return;
        }
        if (email.isBlank()) {
            email = getSiteUserWithNoRecords(permission.grantedToV2);
        }
        Map<String, Object> sharedUser = new HashMap<>();
        if (isIdentitySet) {
            String role = permission.roles != null ? toSentenceCase(permission.roles.get(0)) : "";
            setSharedUser(sharedUser, email.replace("\"", ""), role);
        } else {
            setSharedUser(sharedUser, email.replace("\"", ""), Constants.OWN);
        }
        mapList.add(sharedUser);
    }

    private static void handlePermission(IdentitySet set, List<Map<String, Object>> mapList, GraphServiceClient<Request> graphServiceClient, String siteId, Permission permission, boolean identitySet) {
        String email = set.user != null ? getMailFromGrantedTo(set, mapList, graphServiceClient, siteId, permission) : "";
        if (email.equals(ADDED_IN_SHARED) || email.equals(NOT_ADDED)) {
            return;
        } else if (email.endsWith(VISITORS)) {
            Map<String, Object> sharedUser = new HashMap<>();
            setSharedUser(sharedUser, email, Constants.READ);
            mapList.add(sharedUser);
            return;
        }
        Map<String, Object> sharedUser = new HashMap<>();
        if (identitySet) {
            String role = permission.roles != null ? toSentenceCase(permission.roles.get(0)) : "";
            setSharedUser(sharedUser, email.replace("\"", ""), role);
        } else {
            setSharedUser(sharedUser, email.replace("\"", ""), Constants.OWN);
        }
        mapList.add(sharedUser);
    }

    private static void iteratePermissionsFromDriveItem(DriveItem driveItem, List<Map<String, Object>> mapList, GraphServiceClient<Request> graphServiceClient) {
        PermissionCollectionPage permissionCollectionPage = driveItem.permissions;
        String siteId = driveItem.parentReference != null ? driveItem.parentReference.siteId : null;

        permissionCollectionPage.getCurrentPage().forEach(permission -> {
            if (permission.grantedTo != null) {
                handlePermission(permission.grantedTo, mapList, graphServiceClient, siteId, permission, false);
            } else if (permission.grantedToIdentities != null) {
                permission.grantedToIdentities.forEach(set -> handlePermission(set, mapList, graphServiceClient, siteId, permission, true));
            }
        });
    }


    private static String getMailFromGrantedTo(IdentitySet set, List<Map<String, Object>> mapList, GraphServiceClient<Request> graphServiceClient, String siteId, Permission permission) {
        try {
            if (set.user.additionalDataManager().get(Constants.EMAIL) != null) {
                return set.user.additionalDataManager().get(Constants.EMAIL).getAsString();
            } else {
                return set.user.displayName.contains(VISITORS)
                        ? set.user.displayName
                        : setUsersInShared(permission, set.user.displayName, mapList, siteId, graphServiceClient).toString();
            }
        } catch (RuntimeException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
            return NOT_ADDED;
        }
    }

    private static Object setUsersInShared(Permission permission, String displayName, List<Map<String, Object>> listSharedUser, String siteId, GraphServiceClient<Request> graphServiceClient) {
        String siteName;
        boolean continueWithSiteId = false;
        siteName = siteId != null ? getSiteName(siteId, graphServiceClient) : extractSiteName(displayName);
        if (siteName == null) {
            continueWithSiteId = true;
        }
        Group group = findMatchingGroup(graphServiceClient, siteName, continueWithSiteId, siteId);
        if (group != null) {
            setUsersInSharedMap(displayName, group, listSharedUser, permission, graphServiceClient);
            return ADDED_IN_SHARED;
        }
        return NOT_ADDED;
    }

    private static void setUsersInSharedMap(String displayName, Group group, List<Map<String, Object>> listSharedUser, Permission permission, GraphServiceClient<Request> graphServiceClient) {
        try {
            if (displayName.endsWith("Owners")) {
                // Check owners:
                DirectoryObjectCollectionWithReferencesPage owners = graphServiceClient.groups(group.id).owners().buildRequest().get();
                setOwnersPermission(owners, listSharedUser);
            } else {
                // Check members:
                DirectoryObjectCollectionWithReferencesPage members = graphServiceClient.groups(group.id).members().buildRequest().get();
                setMembersPermission(members, listSharedUser, permission);
            }
        } catch (RuntimeException cause) {
            LOGGER.error(Constants.ERROR_OCCURRED_DURING_FETCHING_DETAILS, cause.getMessage());
        }
    }

    private static void setMembersPermission(DirectoryObjectCollectionWithReferencesPage members, List<Map<String, Object>> listSharedUser, Permission permission) {
        while (members != null && !members.getCurrentPage().isEmpty()) {
            for (DirectoryObject member : members.getCurrentPage()) {
                if (member instanceof User) {
                    Map<String, Object> sharedMembers = new HashMap<>();
                    User user = (User) member;
                    setSharedUser(sharedMembers
                            , user.mail != null ? user.mail : " "
                            , permission.roles != null ? toSentenceCase(permission.roles.get(0)) : "");
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

    private static void setOwnersPermission(DirectoryObjectCollectionWithReferencesPage owners, List<Map<String, Object>> listSharedUser) {
        while (owners != null && !owners.getCurrentPage().isEmpty()) {
            for (DirectoryObject owner : owners.getCurrentPage()) {
                if (owner instanceof User) {
                    Map<String, Object> sharedOwner = new HashMap<>();
                    User user = (User) owner;
                    setSharedUser(sharedOwner, user.mail != null ? user.mail : " ", Constants.OWN);
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

    private static void setSharedUser(Map<String, Object> sharedUser, String email, String permission) {
        sharedUser.put(Constants.USER_EMAIL, email);
        sharedUser.put(Constants.PERMISSION, permission);
    }
}
