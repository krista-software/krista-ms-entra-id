package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileEmailSearchDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FilePermissionDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.FileSearchFilterByRange;
import app.krista.model.base.DateRange;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class SharepointAreaImpl {

    private final FilePermissionDao filePermissionDao;
    private final FileEmailSearchDao fileEmailSearchDao;
    private final FileSearchFilterByRange fileSearchFilterByRange;

    @Inject
    public SharepointAreaImpl(FilePermissionDao filePermissionDao,
                              FileEmailSearchDao fileEmailSearchDao, FileSearchFilterByRange fileSearchFilterByRange) {
        this.filePermissionDao = filePermissionDao;
        this.fileEmailSearchDao = fileEmailSearchDao;
        this.fileSearchFilterByRange = fileSearchFilterByRange;
    }

    public FreeForm getSharedPermission(String sourcePath, boolean useEmail, String accountID) {
        try {
            return filePermissionDao.getSharedPermission(sourcePath, useEmail, accountID);
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
