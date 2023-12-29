package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import com.microsoft.graph.models.List;

import java.sql.Timestamp;
import java.util.Map;

public class ListImpl implements FileService {

    private final List list;

    public ListImpl(List list) {
        this.list = list;
    }

    @Override
    public String getName() {
        return list.displayName;
    }

    @Override
    public String getBaseName() {
        return FileUtil.getBaseName(list.name!=null ? list.name : null).get(0);
    }

    @Override
    public String getExtension() {
        return FileUtil.getBaseName(list.name!=null ? list.name : null).get(1);
    }

    @Override
    public String getParentFolder() {
        return list.parentReference != null ? list.parentReference.name : null;
    }

    @Override
    public String getAbsolutePath() {
        return list.parentReference != null ? list.parentReference.path : null;
    }

    @Override
    public String getPath() {
        return list.parentReference != null ? list.parentReference.path : null;
    }

    @Override
    public String getOwner() {
        if (list.createdBy != null) {
            assert list.createdBy.user != null;
            return list.createdBy.user.displayName;
        } else {
            return null;
        }
    }

    @Override
    public Long getCreated() {
        return list.createdDateTime != null ? Timestamp.valueOf(list.createdDateTime.toLocalDateTime()).getTime() : null;
    }

    @Override
    public Long getModified() {
        return list.lastModifiedDateTime != null ? Timestamp.valueOf(list.lastModifiedDateTime.toLocalDateTime()).getTime() : null;
    }

    @Override
    public Double getSize() {
        return null;
    }

    @Override
    public String getPermissions() {
        return null;
    }

    @Override
    public java.util.List<String> getTags() {
        return java.util.List.of();
    }

    @Override
    public Boolean getIsFolder() {
        return true;
    }

    @Override
    public java.util.List<Map<String, Object>> getSharedWith() {
        return java.util.List.of();
    }

    @Override
    public String getCreatedBy() {
        if (list.createdBy == null) {
            return null;
        }
        return list.createdBy.user != null ? list.createdBy.user.displayName : null;
    }

    @Override
    public String getModifiedBy() {
        if (list.lastModifiedBy == null) {
            return null;
        }
        return list.lastModifiedBy.user != null ? list.lastModifiedBy.user.displayName : null;
    }
}
