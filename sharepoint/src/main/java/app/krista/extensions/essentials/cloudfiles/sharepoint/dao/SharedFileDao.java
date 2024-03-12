package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Base64;
import java.util.Objects;
import java.util.LinkedHashMap;

/**
 * This class provides functionality for sharing files in SharePoint using Microsoft Graph API.
 * It includes methods to create sharing links, grant permissions, and handle exceptions.
 *
 * @author Vaibhav Choudhary
 */

@Service
public class SharedFileDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedFileDao.class);
    private final GraphServiceClientProvider provider;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SearchFileExecutor searchFileExecutor;

    @Inject
    public SharedFileDao(GraphServiceClientProviderFactory factory, FileSearchUtilityService fileSearchUtilityService, SearchFileExecutor searchFileExecutor) {
        this.provider = factory.create();
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.searchFileExecutor = searchFileExecutor;
    }


    public Map<String, Object> shareFile(String source, List<Map<String, Object>> share) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {
                searchFileExecutor.getFileInfo(source, payload, true);
                DriveItem driveItem = (payload.containsKey(Constants.SOURCE_DRIVE_ITEM) ? (DriveItem) payload.get(Constants.SOURCE_DRIVE_ITEM) : null);
                if (driveItem != null) {
                    processSharing(driveItem, source, share, payload);
                    return payload;
                }
                ResponseUtil.errorResponse(false, Constants.PLEASE_PROVIDE_VALID_SOURCE_PATH, payload);
                return payload;
            }

        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, Objects.requireNonNull(cause.getMessage()).contains(Constants.ITEM_NOT_FOUND) ? Constants.FILE_NOT_FOUND : "Error occurred during sharing file", payload);
        }
        return payload;
    }

    private DriveItemCreateLinkParameterSet getParameterSet(String role) {
        return DriveItemCreateLinkParameterSet.newBuilder().withType(role).withScope("organization").withExpirationDateTime(null).withPassword(null).withMessage(null).withRetainInheritedPermissions(null).build();
    }

    private void processSharing(DriveItem driveItem, String source, List<Map<String, Object>> share, Map<String, Object> payload) {
        List<String> usersWithReadPermission = FileUtil.getUsersWithPermission(share, Constants.READ);
        List<String> usersWithWritePermission = FileUtil.getUsersWithPermission(share, Constants.WRITE);

        Map<String, List<String>> usersWithPermission = new LinkedHashMap<>();
        if (!usersWithReadPermission.isEmpty()) {
            usersWithPermission.put("view", usersWithReadPermission);
        }
        if (!usersWithWritePermission.isEmpty()) {
            usersWithPermission.put("edit", usersWithWritePermission);
        }

        for (Map.Entry<String, List<String>> users : usersWithPermission.entrySet()) {
            grantPermissions(users.getKey(), users.getValue(), driveItem, payload);
        }

        String message = payload.getOrDefault(Constants.MESSAGE, "File has been shared successfully").toString();
        if (message.contains("currently restricted") || message.contains("invalid email address")) {
            return;
        }
        DriveItem upadatedDriveItem = getFileWithPermissions(driveItem.parentReference.driveId, driveItem.id);

        String sitePath = payload.get(Constants.SOURCE_ROOT_PATH).toString();
        payload.put(Constants.FILE, app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File.fromFile(new FileImpl(upadatedDriveItem, provider.getUserRequestBuilder(null, null), sitePath, fileSearchUtilityService)));
        payload.put(Constants.SUCCESS, true);
    }

    public DriveItem getFileWithPermissions(String driveId, String id) {
        return provider.getUserRequestBuilder(null, null).drives(driveId).items(id).buildRequest().expand(Constants.PERMISSIONS).get();
    }

    private void grantPermissions(String role, List<String> usersList, DriveItem driveItem, Map<String, Object> payload) {
        try {
            DriveItemCreateLinkParameterSet parameterSet = getParameterSet(role);
            String sharingLink = getSharingLink(driveItem.parentReference.driveId, driveItem.id, parameterSet);
            assert sharingLink != null;
            if (sharingLink.contains(Constants.ACCESS_DENIED)) {
                ResponseUtil.errorResponse(false, Constants.FILE_IS_TO_SHARE_CURRENTLY_RESTRICTED_FOR_ASSISTANCE_CONTACT_YOUR_ADMINISTRATOR_OR_SUPPORT, payload);
                return;
            }
            String encodedSharingUrl = getEncodedSharingURL(sharingLink);
            List<DriveRecipient> recipientsList = new LinkedList<>();
            for (String user : usersList) {
                DriveRecipient driveRecipient = new DriveRecipient();
                driveRecipient.email = user;
                recipientsList.add(driveRecipient);
            }
            provider.getGraphServiceClientForAdmin().shares(encodedSharingUrl).permission().grant(PermissionGrantParameterSet.newBuilder().withRoles(List.of(role)).withRecipients(recipientsList).build()).buildRequest().post();

        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.info(cause.getMessage(), cause);
            ResponseUtil.errorResponse(false, "We couldn't share the file or folder due to an invalid email address. Please check the email address and try again.", payload);
        }

    }

    private String getEncodedSharingURL(String sharingLink) {
        String base64Value = Base64.getEncoder().encodeToString(sharingLink.getBytes(StandardCharsets.UTF_8));
        return "u!" + base64Value.replaceAll("[=/+]", "").replace('/', '_').replace('+', '-');
    }

    private String getSharingLink(String driveID, String id, DriveItemCreateLinkParameterSet parameterSet) {
        try {
            Permission sharedFile = executePermission(driveID, id, parameterSet);
            return (sharedFile != null && sharedFile.link != null) ? sharedFile.link.webUrl : null;
        } catch (GraphServiceException cause) {
            if (cause.getError() != null && cause.getError().error != null) {
                LOGGER.info(cause.getMessage(), cause);
                return cause.getMessage();
            } else {
                LOGGER.info(cause.getMessage(), cause);
                return cause.getMessage();
            }
        }
    }

    @Nullable
    private Permission executePermission(String driveID, String id, DriveItemCreateLinkParameterSet parameterSet) {
        return provider.getUserRequestBuilder(null, null).drives(driveID).items(id).createLink(parameterSet).buildRequest().post();
    }
}
