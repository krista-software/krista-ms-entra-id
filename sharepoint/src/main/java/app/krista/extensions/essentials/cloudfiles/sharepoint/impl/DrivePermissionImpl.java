package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.File;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Vaibhav Choudhary
 *
 */

public class DrivePermissionImpl implements FileService {

    private final DriveItem driveItem;
    private final DriveItem driveMetaData;

    private final GraphServiceClientProvider clientProvider;

    public DrivePermissionImpl(DriveItem driveItem, GraphServiceClientProvider clientProvider) {
        this.driveItem = driveItem;
        this.clientProvider = clientProvider;
        this.driveMetaData = FileSearchUtilityService.getDriveMetaData(driveItem, clientProvider);
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
        return FileSearchUtilityService.getExtension(driveMetaData,driveItem);
        //return FileUtil.getBaseName(driveItem.name != null ? driveItem.name : null).get(1);
    }

    @Override
    public String getParentFolder() {
        return FileSearchUtilityService.getParentFolder(driveMetaData,driveItem);
    }

    @Override
    public String getAbsolutePath() {
        return FileSearchUtilityService.getAbsolutePath(driveMetaData,driveItem);
    }

    @Override
    public String getPath() {
        return driveItem != null && driveItem.webUrl != null ? driveItem.webUrl : null;
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
        return driveItem.createdDateTime != null ? Timestamp.valueOf(driveItem.createdDateTime.toLocalDateTime()).getTime() : null;
    }

    @Override
    public Long getModified() {
        return driveItem.lastModifiedDateTime != null ? Timestamp.valueOf(driveItem.lastModifiedDateTime.toLocalDateTime()).getTime() : null;
    }

    @Override
    public Double getSize() {
        return driveItem.size != null ? (double) driveItem.size : null;
    }

    @Override
    public String getPermissions() {
        return null;
    }

    @Override
    public List<String> getTags() {
        return List.of();
    }

    @Override
    public Boolean getIsFolder() {
        return driveMetaData.folder != null;
    }

    @Override
    public List<Map<String, Object>> getSharedWith() {
        return driveMetaData != null && driveMetaData.permissions != null ? FileDaoImpl.getSharedWithPermissions(driveMetaData.permissions) : null;
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

}
