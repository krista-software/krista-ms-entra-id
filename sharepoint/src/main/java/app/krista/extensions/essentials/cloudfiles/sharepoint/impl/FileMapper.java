package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.models.DriveItem;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Choudhary
 */

public class FileMapper implements FileService {

    private final DriveItem driveItem;
    private final String absolutePath;
    private final String permissions;

    public FileMapper(DriveItem driveItem, String sitePath, String permissions) {
        this.driveItem = driveItem;
        this.permissions = permissions;
        this.absolutePath = sitePath != null ? getFilePath(driveItem, sitePath) : null;
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
        return FileUtil.getParentPath(absolutePath);
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
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
        return permissions;
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
        return Collections.emptyList();
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


    private String getFilePath(DriveItem driveItem, String sitePath) {
        if (driveItem.parentReference != null && driveItem.parentReference.path != null) {
            String relativePath = FileUtil.getRootRelativeFilePath(driveItem.parentReference.path);
            String modifiedRelativePath = relativePath.isBlank() ? "" : relativePath + "/";
            return sitePath + "/" + modifiedRelativePath + driveItem.name;
        }
        return null;
    }
}
