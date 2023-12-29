package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;

import java.sql.Timestamp;
import java.util.Map;

public class GroupImpl implements FileService {

    private final Group group;

    private final GraphServiceException exception;

    public GroupImpl(Group group, GraphServiceException exception) {
        this.group = group;
        this.exception = exception;
    }

    @Override
    public String getName() {
        return group.displayName;
    }

    @Override
    public String getBaseName() {
        return FileUtil.getBaseName(group.mail!=null ? group.mail : null).get(0);
    }

    @Override
    public String getExtension() {
        return null;
    }

    @Override
    public String getParentFolder() {
        return null;
    }

    @Override
    public String getAbsolutePath() {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public Long getCreated() {
        return group.createdDateTime != null ? Timestamp.valueOf(group.createdDateTime.toLocalDateTime()).getTime() : null;
    }

    @Override
    public Long getModified() {
        return null;
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
        return null;
    }

    @Override
    public java.util.List<Map<String, Object>> getSharedWith() {
        return java.util.List.of();
    }

    @Override
    public String getCreatedBy() {
        return null;
    }

    @Override
    public String getModifiedBy() {
        return null;
    }

}
