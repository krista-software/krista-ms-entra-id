package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.SharePointAttributes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import com.microsoft.graph.models.DriveItem;

import java.sql.Timestamp;
import java.util.List;

import com.microsoft.graph.requests.PermissionCollectionPage;

import java.util.Map;

/**
 *
 * @author Vaibhav Choudhary
 *
 */

public class FileImpl implements FileService {

    private final DriveItem driveItem;

    private final PermissionCollectionPage permissionCollectionPage;

    private final SharePointAttributes sharePointAttributes;

    public FileImpl(DriveItem driveItem, PermissionCollectionPage permissionCollectionPage, SharePointAttributes sharePointAttributes) {
        this.driveItem = driveItem;
        this.permissionCollectionPage = permissionCollectionPage;
        this.sharePointAttributes = sharePointAttributes;
    }

    @Override
    public String getName() {
        return driveItem.name;
    }

    @Override
    public String getBaseName() {
        return FileUtil.getBaseName(driveItem.name!=null ? driveItem.name : null).get(0);
    }

    @Override
    public String getExtension() {
        return FileUtil.getBaseName(driveItem.name!=null ? driveItem.name : null).get(1);
    }

    @Override
    public String getParentFolder() {
        return driveItem.parentReference != null ? driveItem.parentReference.name : null;
    }

    @Override
    public String getAbsolutePath() {
        return null;
    }

    @Override
    public String getPath() {
        return FileUtil.getFilePath(
                (driveItem.remoteItem != null)
                ? driveItem.remoteItem.webDavUrl
                : (driveItem.parentReference != null ? driveItem.parentReference.path : null), driveItem);
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
        return driveItem.size != null ? Double.valueOf(driveItem.size) : null;
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
        return driveItem.folder != null;
    }

    @Override
    public List<Map<String, Object>> getSharedWith() {
        return null;
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
