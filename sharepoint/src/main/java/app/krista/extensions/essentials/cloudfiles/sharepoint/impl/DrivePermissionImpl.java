package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util.FileSearchUtilityService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Choudhary
 */

public class DrivePermissionImpl implements FileService {

    private final DriveItem driveItem;
    private final DriveItem driveMetaData;
    private final GraphServiceClient<Request> graphClient;
    private final FileSearchUtilityService fileSearchUtilityService;
    private final String absolutePath;

    public DrivePermissionImpl(DriveItem driveItem, GraphServiceClient<Request> graphClient, FileSearchUtilityService fileSearchUtilityService, String sitePath) {
        this.driveItem = driveItem;
        this.graphClient = graphClient;
        this.fileSearchUtilityService = fileSearchUtilityService;
        this.driveMetaData = this.fileSearchUtilityService.getDriveMetaData(driveItem, graphClient);
        this.absolutePath =  sitePath!=null ? getFilePath(driveItem,sitePath) : null;

    }

    private String getFilePath(DriveItem driveItem, String sitePath) {
        if(driveItem.parentReference != null && driveItem.parentReference.path != null){
            String relativePath = FileUtil.getRootRelativeFilePath(driveItem.parentReference.path);
            String modifiedRelativePath = relativePath.isBlank() ? "" : relativePath +"/";
            return sitePath+"/"+modifiedRelativePath+driveItem.name;
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
        return FileSearchUtilityService.getExtension(driveMetaData, driveItem);
    }

    @Override
    public String getParentFolder() {
        return absolutePath == null
                ? FileSearchUtilityService.getParentFolder(driveMetaData, driveItem)
                : FileUtil.getParentPath(absolutePath);
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath == null
                ? FileSearchUtilityService.getAbsolutePath(driveMetaData, driveItem)
                : absolutePath;
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
        if (driveItem.permissions != null) {
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

}
