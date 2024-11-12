package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntitySearchMapper;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FilePermissionService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vaibhav Choudhary
 */

public class DrivePermissionImpl implements FileService {

    private final DriveItem driveItem;
    private final DriveItem driveMetaData;
    private final GraphServiceClient<Request> graphClient;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final SharePointAttributes sharePointAttributes;
    private final String absolutePath;
    private static final Logger LOGGER = LoggerFactory.getLogger(DrivePermissionImpl.class);

    public DrivePermissionImpl(DriveItem driveItem, GraphServiceClient<Request> graphClient, FileSearchUtilityService fileSearchUtilityService, SharePointAttributes sharePointAttributes, String sitePath) {
        this.driveItem = driveItem;
        this.graphClient = graphClient;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.sharePointAttributes = sharePointAttributes;
        this.driveMetaData = this.fileSearchUtilityService.getDriveMetaData(driveItem, graphClient);
        this.absolutePath = sitePath != null ? getFilePath(driveItem, sitePath) : extractPathFromDriveItem(driveItem);

    }

    private String getFilePath(DriveItem driveItem, String sitePath) {
        if (driveItem.parentReference != null && driveItem.parentReference.path != null) {
            String relativePath = FileUtil.getRootRelativeFilePath(driveItem.parentReference.path);
            String modifiedRelativePath = relativePath.isBlank() ? "" : relativePath + "/";
            return sitePath + "/" + modifiedRelativePath + driveItem.name;
        }
        return null;
    }

    @Override
    public String getName() {
        return driveItem.name;
    }

    @Override
    public String getBaseName() {
        return FileUtil.getBaseName(driveItem.name != null ? driveItem.name : null).get(0);
    }

    @Override
    public String getExtension() {
        return FileUtil.getBaseName(driveItem.name != null ? driveItem.name : null).get(1);
    }

    @Override
    public String getParentFolder() {
        return FileUtil.getParentPath(this.absolutePath);
    }

    @Override
    public String getAbsolutePath() {
        return this.absolutePath;
    }

    @Override
    public String getOwner() {
        if (driveItem.createdBy != null) {
            assert driveItem.createdBy.user != null;
            return driveItem.createdBy.user.displayName;
        } else {
            return null;
        }
    }

    @Override
    public Long getCreated() {
        return driveItem.createdDateTime != null ? Timestamp.valueOf(driveItem.createdDateTime
                .toLocalDateTime()).getTime() : null;
    }

    @Override
    public Long getModified() {
        return driveItem.lastModifiedDateTime != null ? Timestamp.valueOf(driveItem.lastModifiedDateTime
                .toLocalDateTime()).getTime() : null;
    }

    @Override
    public Double getSize() {
        return driveItem.size != null ? (double) driveItem.size : null;
    }

    @Override
    public String getPermissions() {
        if(driveMetaData.permissions != null && sharePointAttributes != null){
            return FilePermissionService.getOwnerPermissions(driveMetaData.permissions, sharePointAttributes);
        } else if (driveItem.permissions != null) {
            return driveItem.permissions.toString();
        }
        return null;
    }

    @Override
    public List<String> getTags() {
        return List.of();
    }

    @Override
    public Boolean getIsFolder() {
        return driveMetaData != null && driveMetaData.folder != null;
    }

    @Override
    public List<Map<String, Object>> getSharedWith() {
        return driveMetaData != null && driveMetaData.permissions != null ?
                FileDaoImpl.getSharedWithPermissions(driveMetaData.permissions) : null;
    }

    @Override
    public String getCreatedBy() {
        if (driveItem.createdBy == null) {
            return null;
        }
        return driveItem.createdBy.user != null ? driveItem.createdBy.user.displayName : null;
    }

    @Override
    public String getModifiedBy() {
        if (driveItem.lastModifiedBy == null) {
            return null;
        }
        return driveItem.lastModifiedBy.user != null ? driveItem.lastModifiedBy.user.displayName : null;
    }

    public String extractPathFromDriveItem(DriveItem driveItem) {
        return driveItem.webUrl != null
                ? getFullFilePath(driveItem)
                : null;
    }

    private String getFullFilePath(DriveItem driveItem) {
        try {
            if (Objects.requireNonNull(driveItem.webUrl).contains(".aspx?")) {
                DriveItem item = graphClient.drives(Objects.requireNonNull(driveItem.parentReference.driveId)).
                        items(Objects.requireNonNull(driveItem.id)).buildRequest().expand(Constants.QUERY_PERMISSIONS).get();
                return getFilePath(Objects.requireNonNull(item), FileEntitySearchMapper.extractSitePathFromURL(driveItem.webUrl));

            } else {
                return FileEntitySearchMapper.extractFullPath(driveItem.webUrl);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error while fetching path {} {} :", cause, cause.getMessage());
        }
        return null;
    }

}
