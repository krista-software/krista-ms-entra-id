package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.*;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SharepointAreaImpl {

    private final FilePermissionDao filePermissionDao;

    private final FilePermissionSearchDao filePermissionSearchDao;

    @Inject
    public SharepointAreaImpl(FilePermissionDao filePermissionDao,
                              FilePermissionSearchDao filePermissionSearchDao) {
        this.filePermissionDao = filePermissionDao;
        this.filePermissionSearchDao = filePermissionSearchDao;
    }

    public FreeForm getSharedPermission(String sourcePath, boolean useEmail, String accountID) {
        try {
            return filePermissionDao.getSharedPermission(sourcePath,useEmail,accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getSearchByPermission(String permission, boolean useEmail, String accountID) {
        try {
            return filePermissionSearchDao.getSearchByPermission(permission, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }
}
