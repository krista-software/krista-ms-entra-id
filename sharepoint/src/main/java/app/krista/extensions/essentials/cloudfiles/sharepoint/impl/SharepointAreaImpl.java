package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.*;
import app.krista.model.base.DateRange;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SharepointAreaImpl {

    private final FilePermissionDao filePermissionDao;
    private final FilePermissionSearchDao filePermissionSearchDao;
    private final FileEmailSearchDao fileEmailSearchDao;
    private final FileSearchFilterByRange fileSearchFilterByRange;

    @Inject
    public SharepointAreaImpl(FilePermissionDao filePermissionDao,
                              FilePermissionSearchDao filePermissionSearchDao, FileEmailSearchDao fileEmailSearchDao, FileSearchFilterByRange fileSearchFilterByRange) {
        this.filePermissionDao = filePermissionDao;
        this.filePermissionSearchDao = filePermissionSearchDao;
        this.fileEmailSearchDao = fileEmailSearchDao;
        this.fileSearchFilterByRange = fileSearchFilterByRange;
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

    public FreeForm getSearchByMail(String email, String user, boolean useEmail, String accountID) {
        try {
            return fileEmailSearchDao.getSearchByMail(email, user, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFileByDateRange(DateRange modificationDateRange, boolean useEmail, String accountID) {
        try {
            return fileSearchFilterByRange.getFilesByModificationDate(modificationDateRange, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFileBySizeRange(Double fromSize, Double toSize, boolean useEmail, String accountID) {
        try {
            return fileSearchFilterByRange.getFilesBySizeRange(fromSize, toSize, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

}
