package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.models.Site;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Choudhary
 */

public class SiteImpl implements FileService {

    private final Site site;

    public SiteImpl(Site site) {
        this.site = site;
    }

    @Override
    public String getName() {
        return site.displayName;
    }

    @Override
    public String getBaseName() {
        return FileUtil.getBaseName(site.name != null ? site.name : null).get(0);
    }

    @Override
    public String getExtension() {
        return FileUtil.getBaseName(site.name != null ? site.name : null).get(1);
    }

    @Override
    public String getParentFolder() {
        return site.parentReference != null ? site.parentReference.name : null;
    }

    @Override
    public String getAbsolutePath() {
        return site.webUrl != null ? site.webUrl : null;
    }

    @Override
    public String getOwner() {
        if (site.createdBy != null) {
            assert site.createdBy.user != null;
            return site.createdBy.user.displayName;
        } else {
            return null;
        }
    }

    @Override
    public Long getCreated() {
        return site.createdDateTime != null ? Timestamp.valueOf(site.createdDateTime.toLocalDateTime()).getTime() : null;
    }

    @Override
    public Long getModified() {
        return site.lastModifiedDateTime != null ? Timestamp.valueOf(site.lastModifiedDateTime.toLocalDateTime()).getTime() : null;
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
    public List<String> getTags() {
        return List.of();
    }

    @Override
    public Boolean getIsFolder() {
        return null;
    }

    @Override
    public List<Map<String, Object>> getSharedWith() {
        return List.of();
    }

    @Override
    public String getCreatedBy() {
        if (site.createdBy == null) {
            return null;
        }
        return site.createdBy.user != null ? site.createdBy.user.displayName : null;
    }

    @Override
    public String getModifiedBy() {
        if (site.lastModifiedBy == null) {
            return null;
        }
        return site.lastModifiedBy.user != null ? site.lastModifiedBy.user.displayName : null;
    }

}
