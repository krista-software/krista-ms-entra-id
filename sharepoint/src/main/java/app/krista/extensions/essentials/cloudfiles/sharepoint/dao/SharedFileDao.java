package app.krista.extensions.essentials.cloudfiles.sharepoint.dao;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.ManagementUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.FileImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

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
    private final ManagementUtilityService managementUtilityService;
    private final FileSearchUtilityService fileSearchUtilityService;

    @Inject
    public SharedFileDao(GraphServiceClientProviderFactory factory, ManagementUtilityService managementUtilityService,
                         FileSearchUtilityService fileSearchUtilityService) {
        this.provider = factory.create();
        this.managementUtilityService = managementUtilityService;
        this.fileSearchUtilityService = fileSearchUtilityService;
    }


    public Map<String, Object> shareFile(String source, List<Map<String, Object>> share) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (isNullOrEmpty(source)) {
                return handleInvalidPath(payload);
            }
            source = managementUtilityService.getUpdatedFilePath(source);
            DriveItem drive = managementUtilityService.checkIfFileExist(source, payload);
            if (!(boolean) payload.get(Constants.SUCCESS)) {
                return handleInvalidPath(payload);
            }

            String sharingLink = getSharingLink(drive.parentReference.driveId, DriveUtil.getRelativePath(source), createLinkParameterSet());
            if (sharingLink != null) {
                String encodedSharingUrl = getEncodedSharingURL(sharingLink);
                Map<String, List<String>> usersWithPermission = getUsersWithPermissionMap(share);

                for (Map.Entry<String, List<String>> users : usersWithPermission.entrySet()) {
                    grantPermissions(users.getKey(), users.getValue(), encodedSharingUrl);
                }

                GraphServiceClient<Request> graphClient = provider.getUserRequestBuilder(null, null);
                DriveItem driveItem = getFileInfo(drive.parentReference.driveId, DriveUtil.getRelativePath(source), graphClient);
                payload.put(Constants.FILE, CatalogTypes.fromFile(new FileImpl(driveItem, graphClient, source, null, fileSearchUtilityService)));
                payload.put(Constants.MESSAGE, Constants.FILE_SHARED_SUCCESS);
                return payload;
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            handleGraphServiceException(cause, payload);
        }
        return payload;
    }

    private DriveItemCreateLinkParameterSet createLinkParameterSet() {
        return DriveItemCreateLinkParameterSet.newBuilder()
                .withType(Constants.EDIT)
                .withScope(Constants.ORGANIZATION)
                .withExpirationDateTime(null)
                .withPassword(null)
                .withMessage(null)
                .withRetainInheritedPermissions(null)
                .build();
    }

    private Map<String, List<String>> getUsersWithPermissionMap(List<Map<String, Object>> share) {
        List<String> usersWithReadPermission = FileUtil.getUsersWithPermission(share, Constants.READ);
        List<String> usersWithWritePermission = FileUtil.getUsersWithPermission(share, Constants.WRITE);
        usersWithWritePermission.addAll(FileUtil.getUsersWithPermission(share, Constants.READ_AND_WRITE));

        Map<String, List<String>> usersWithPermission = new LinkedHashMap<>();
        usersWithPermission.put(Constants.VIEW, !usersWithReadPermission.isEmpty() ? usersWithReadPermission : null);
        usersWithPermission.put(Constants.EDIT, !usersWithWritePermission.isEmpty() ? usersWithWritePermission : null);

        return usersWithPermission;
    }

    private Map<String, Object> handleInvalidPath(Map<String, Object> payload) {
        payload.put(Constants.SUCCESS, false);
        payload.put(Constants.MESSAGE, Constants.PROVIDE_VALID_PATH);
        return payload;
    }

    private void handleGraphServiceException(Exception cause, Map<String, Object> payload) {
        LOGGER.error(Constants.ERROR_MESSAGE + " : " + cause.getMessage());
        payload.put(Constants.MESSAGE, Constants.FAILED_TO_SHARE_FILE);
        payload.put(Constants.SUCCESS, false);
    }

    public DriveItem getFileInfo(String driveId, String path, GraphServiceClient<Request> graphClient) {
        return driveId != null ?
                graphClient.drives(driveId).root().itemWithPath(path).buildRequest().get() :
                graphClient.drive().root().itemWithPath(path).buildRequest().get();
    }

    private void grantPermissions(String role, List<String> usersList, String encodedSharingUrl) {
        try {
            List<DriveRecipient> recipientsList = new LinkedList<>();
            if (usersList != null) {
                for (String user : usersList) {
                    DriveRecipient driveRecipient = new DriveRecipient();
                    driveRecipient.email = user;
                    recipientsList.add(driveRecipient);
                }
                provider.getGraphServiceClientForAdmin().shares(encodedSharingUrl).permission().grant(
                        PermissionGrantParameterSet
                                .newBuilder()
                                .withRoles(List.of(role))
                                .withRecipients(recipientsList)
                                .build()).buildRequest().post();
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    private String getEncodedSharingURL(String sharingLink) {
        String base64Value = Base64.getEncoder().encodeToString(sharingLink.getBytes(StandardCharsets.UTF_8));
        return "u!" + base64Value.replaceAll("[=/+]", "").replace('/', '_')
                .replace('+', '-');
    }

    private String getSharingLink(String driveID, String path, DriveItemCreateLinkParameterSet parameterSet) {
        try {
            Permission sharedFile = driveID != null ? provider.getUserRequestBuilder(null, null)
                    .drives(driveID).root().itemWithPath(path).createLink(parameterSet).buildRequest().post()
                    : provider.getUserRequestBuilder(null, null).drive().root()
                    .itemWithPath(path).createLink(parameterSet).buildRequest().post();
            return (sharedFile != null && sharedFile.link != null) ? sharedFile.link.webUrl : null;
        } catch (GraphServiceException cause) {
            if (cause.getError() != null && cause.getError().error != null) {
                return cause.getError().error.message;
            } else {
                return cause.getMessage();
            }
        }
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

}
