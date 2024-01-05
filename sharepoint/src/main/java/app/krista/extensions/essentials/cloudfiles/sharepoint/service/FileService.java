package app.krista.extensions.essentials.cloudfiles.sharepoint.service;

import java.util.List;
import java.util.Map;

public interface FileService {

    String getName();

    String getBaseName();

    String getExtension();

    String getParentFolder();

    String getAbsolutePath();

    String getPath();

    String getOwner();

    Long getCreated();

    Long getModified();

    Double getSize();

    String getPermissions();

    Boolean getIsFolder();

    List<String> getTags();

    List<Map<String, Object>> getSharedWith();

    String getCreatedBy();

    String getModifiedBy();

}
