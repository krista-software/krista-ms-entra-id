package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SearchEntityQueryCollectionPage;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

@Service
public class SharedFileDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedFileDao.class);

    private final GraphServiceClientProvider provider;

    private final OperationsManagementDao operationsManagementDao;

    @Inject
    public SharedFileDao(GraphServiceClientProviderFactory factory, OperationsManagementDao operationsManagementDao) {
        this(factory.create(), operationsManagementDao);
    }

    public SharedFileDao(GraphServiceClientProvider provider, OperationsManagementDao operationsManagementDao) {
        this.provider = provider;
        this.operationsManagementDao = operationsManagementDao;
    }

    public Map<String, Object> shareFile(String source, List<Map<String, Object>> share) {
        Map<String, Object> payload = new HashMap<>();
        try {
            if (source != null) {

                DriveItem drive = operationsManagementDao.checkIfFileExist(source, payload);
                if ((boolean) payload.get("Success")) {
                    String sharingLink = "";
                    DriveItemCreateLinkParameterSet parameterSet = DriveItemCreateLinkParameterSet.newBuilder().withType("edit").withScope("organization").withExpirationDateTime(null).withPassword(null).withMessage(null).withRetainInheritedPermissions(null).build();
                    String sourcePath = DriveUtil.getRelativePath(source);
                    sharingLink = getSharingLink(drive.parentReference.driveId, sourcePath, parameterSet);

                    if (sharingLink != null) {

                        String encodedSharingUrl = getEncodedSharingURL(sharingLink);
                        List<String> usersWithReadPermission = FileUtil.getUsersWithPermission(share, "Read");
                        List<String> usersWithWritePermission = FileUtil.getUsersWithPermission(share, "Write");
                        usersWithWritePermission.addAll(FileUtil.getUsersWithPermission(share, "Read and Write"));
                        Map<String, List<String>> usersWithPermission = new LinkedHashMap<>();
                        usersWithPermission.put("view", !usersWithReadPermission.isEmpty() ? usersWithReadPermission : null);
                        usersWithPermission.put("edit", !usersWithWritePermission.isEmpty() ? usersWithWritePermission : null);

                        for (Map.Entry<String, List<String>> users : usersWithPermission.entrySet()) {
                            grantPermissions(users.getKey(), users.getValue(), encodedSharingUrl);
                        }

                        DriveItem driveItem = getFileInfo(drive.parentReference.driveId, sourcePath);
                        payload.put("File", CatalogTypes.fromFile(new FileImpl(driveItem, null, null)));

                        payload.put("Message", "File Has been shared Successfully");
                        return payload;
                    }
                } else {
                    payload.put("Success", false);
                    payload.put("Message", "Provide Valid Path");
                    return payload;
                }
            } else {
                payload.put("Success", false);
                payload.put("Message", "Provide Valid Path");
                return payload;
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            payload.put("Message", Objects.requireNonNull(cause.getMessage()).contains("itemNotFound") ? "File Not Found" : cause.getMessage());
            payload.put("Success", false);
            cause.printStackTrace();
        }
        return Map.of();
    }

    public DriveItem getFileInfo(String driveId, String path) {
        return driveId != null ?
                provider.getUserRequestBuilder(null, null).drives(driveId).root().itemWithPath(path).buildRequest().get() :
                provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(path).buildRequest().get();
    }

    private void grantPermissions(String role, List<String> usersList, String encodedSharingUrl) {
        try {
            List<DriveRecipient> recipientsList = new LinkedList<>();
            if(usersList != null) {
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
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }

    }

    private String getEncodedSharingURL(String sharingLink) {
        String base64Value = Base64.getEncoder().encodeToString(sharingLink.getBytes(StandardCharsets.UTF_8));
        return "u!" + base64Value.replaceAll("[=/+]", "").replace('/', '_').replace('+', '-');
    }

    private String getSharingLink(String driveID, String path, DriveItemCreateLinkParameterSet parameterSet) {
        try {
            Permission sharedFile = driveID != null ? provider.getUserRequestBuilder(null, null).drives(driveID).root().itemWithPath(path).createLink(parameterSet).buildRequest().post()
                    : provider.getUserRequestBuilder(null, null).drive().root().itemWithPath(path).createLink(parameterSet).buildRequest().post();
            return (sharedFile != null && sharedFile.link != null) ? sharedFile.link.webUrl : null;
        } catch (GraphServiceException cause) {
            if (cause.getError() != null && cause.getError().error != null) {
                return cause.getError().error.message;
            } else {
                return cause.getMessage();
            }
        }
    }

}
