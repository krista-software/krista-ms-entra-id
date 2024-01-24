package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.dao.*;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Choudhary
 */

@Service
public class ManagementDaoImpl {

    private final GraphServiceClientProvider provider;
    private final DriveFileService fileManagementDao;
    private final FolderManagementDao folderManagementDao;
    private final OperationsManagementDao operationsManagementDao;
    private final FileDownloadService fileDownloadService;
    private final FileUploadService fileUploadService;
    private final SharedFileDao sharedFileDao;
    private final FileTransferService copyFileDao;
    private final DriveTextFileManager driveTextFileService;
    private final FileSearchFilterByRange fileSearchFilterByRange;
    private final FileVersionSearchDao fileVersionSearchDao;
    private final HandleFileIsChanged handleFileIsChanged;


    @Inject
    public ManagementDaoImpl(GraphServiceClientProviderFactory factory, DriveFileService fileManagementDao,
                             FolderManagementDao folderManagementDao, OperationsManagementDao operationsManagementDao,
                             SharedFileDao sharedFileDao, FileTransferService copyFileDao, FileDownloadService fileDownloadService,
                             FileUploadService fileUploadService, DriveTextFileManager driveTextFileService,
                             FileSearchFilterByRange fileSearchFilterByRange,FileVersionSearchDao fileVersionSearchDao,
                             HandleFileIsChanged handleFileIsChanged) {
        this.provider = factory.create();
        this.fileManagementDao = fileManagementDao;
        this.folderManagementDao = folderManagementDao;
        this.operationsManagementDao = operationsManagementDao;
        this.sharedFileDao = sharedFileDao;
        this.copyFileDao = copyFileDao;
        this.fileDownloadService = fileDownloadService;
        this.fileUploadService = fileUploadService;
        this.driveTextFileService = driveTextFileService;
        this.fileSearchFilterByRange = fileSearchFilterByRange;
        this.fileVersionSearchDao = fileVersionSearchDao;
        this.handleFileIsChanged = handleFileIsChanged;
    }


    public Map<String, Object> copyFile(String source, String target, Boolean replace, Boolean createFolder) {
        try {
            return copyFileDao.copyFile(source, target, replace, createFolder);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> deleteFile(String target) {
        try {
            return operationsManagementDao.deleteFile(target);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> downloadFileAsLink(String source) {
        try {
            // return "<a href =" + fileDownloadService.downloadFiles(true, false, source) + ">Click here to download " ;
           return fileDownloadService.downloadFiles(true, false, source);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> downloadFileAsFile(String source) {
        try {
            return fileDownloadService.downloadFiles(false, true, source);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFileByPath(String relativePath, String search, boolean useEmail, String accountID) {
        try {
            return fileManagementDao.getListOfFiles(relativePath, search, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFoldersByPath(String relativePath, String search, boolean useEmail, String accountID) {
        try {
            return folderManagementDao.getListOfFolders(relativePath, search, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> shareFile(String source, List<Map<String, Object>> share) {
        try {
            return sharedFileDao.shareFile(source, share);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> moveFile(String source, String target) {
        try {
            return copyFileDao.moveFile(source, target);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> renameFile(String sourcePath, String targetFileName) {
        try {
            return operationsManagementDao.renameFile(sourcePath, targetFileName);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getInfo(String sourcePath, boolean useEmail, String accountID) {
        try {
            return fileManagementDao.getFileInfo(sourcePath, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> readTxtFile(String source) {
        try {
            return driveTextFileService.readTxtFile(source);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> uploadFile(app.krista.model.base.File kristaFile, String targetPath) {
        try {
            return fileUploadService.uploadFiles(kristaFile, targetPath);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> uploadFileAsTxt(String fileContent, String target) {
        try {
            return driveTextFileService.uploadTxtFile(fileContent, target);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> getVersionHistory(String sourcePath, String search, Long since) {
        try {
            return fileVersionSearchDao.getVersionHistory(sourcePath, search, since);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> whenAFileIsChanged(String source, String search) {
        try {
            return handleFileIsChanged.getChangeFileEvent(source, search);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

}
