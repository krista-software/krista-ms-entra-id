package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.model.base.DateRange;
import app.krista.model.base.FreeForm;
import com.microsoft.graph.http.GraphServiceException;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Service
public class ManagementDaoImpl {

    private final GraphServiceClientProvider provider;
    private final FileManagementDao fileManagementDao;
    private final FolderManagementDao folderManagementDao;
    private final OperationsManagementDao operationsManagementDao;
    private final FileDownloadService fileDownloadService;
    private final FileUploadService fileUploadService;
    private final SharedFileDao sharedFileDao;
    private final CopyFileDao copyFileDao;
    private final DriveTextFileService driveTextFileService;


    @Inject
    public ManagementDaoImpl(GraphServiceClientProviderFactory factory, FileManagementDao fileManagementDao,FolderManagementDao folderManagementDao,
                             OperationsManagementDao operationsManagementDao,SharedFileDao sharedFileDao,CopyFileDao copyFileDao,
                             FileDownloadService fileDownloadService,FileUploadService fileUploadService, DriveTextFileService driveTextFileService) {
        this(factory.create(), fileManagementDao, folderManagementDao,operationsManagementDao,sharedFileDao,copyFileDao,
                fileDownloadService,fileUploadService,driveTextFileService);
    }

    public ManagementDaoImpl(GraphServiceClientProvider provider, FileManagementDao fileManagementDao,
                             FolderManagementDao folderManagementDao ,OperationsManagementDao operationsManagementDao,
                             SharedFileDao sharedFileDao ,CopyFileDao copyFileDao, FileDownloadService fileDownloadService,
                             FileUploadService fileUploadService,DriveTextFileService driveTextFileService){
        this.provider = provider;
        this.fileManagementDao = fileManagementDao;
        this.folderManagementDao = folderManagementDao;
        this.operationsManagementDao = operationsManagementDao;
        this.sharedFileDao = sharedFileDao;
        this.copyFileDao = copyFileDao;
        this.fileDownloadService = fileDownloadService;
        this.fileUploadService = fileUploadService;
        this.driveTextFileService = driveTextFileService;
    }


    public Map<String, Object> copyFile(String source, String target, Boolean replace, Boolean createFolder) {
        try {
            return copyFileDao.copyFile(source, target, replace, createFolder);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> deleteFile(String target) {
        try {
            return operationsManagementDao.deleteFile(target);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> downloadFile(Boolean downloadAsLink, Boolean downloadAsKristaFile, String source) {
        try {
            return fileDownloadService.downloadFiles(downloadAsLink, downloadAsKristaFile, source);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFileByPath(String relativePath, String search, boolean useEmail, String accountID) {
        try {
            return fileManagementDao.getListOfFiles(relativePath, search, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFoldersByPath(String relativePath, String search, boolean useEmail, String accountID) {
        try {
            return folderManagementDao.getListOfFolders(relativePath, search, useEmail, accountID);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> shareFile(String source, List<Map<String, Object>> share) {
        try {
            return sharedFileDao.shareFile(source, share);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> moveFile(String source, String target) {
        try {
            return copyFileDao.moveFile(source, target);
        } catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> renameFile(File renameFile, String targetFileName) {
        try{
            return operationsManagementDao.renameFile(renameFile,targetFileName);
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getInfo(String sourcePath, boolean useEmail, String accountID) {
        try{
            return fileManagementDao.getFileInfo(sourcePath,useEmail,accountID);
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> readTxtFile(String source) {
        try{
            return driveTextFileService.readTxtFile(source);
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> uploadFile(app.krista.model.base.File kristaFile, String targetPath) {
        try{
            return fileUploadService.uploadFiles(kristaFile,targetPath);
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> uploadFileAsTxt(String fileContent, String target) {
        try{
            return driveTextFileService.uploadTxtFile(fileContent,target);
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> getChangeFileEvent(String changeFile, String source, String search) {
        return Map.of();
    }

    public FreeForm getFileByDateRange(DateRange modificationDateRange, boolean useEmail, String accountID) {
        try{
            return new FreeForm();
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }

    public FreeForm getFileBySizeRange(Double fromSize, Double toSize, boolean useEmail, String accountID) {
        try{
            return new FreeForm();
        }catch (GraphServiceException | IllegalArgumentException cause) {
            cause.printStackTrace();
            throw new IllegalArgumentException(cause.getMessage(), cause);
        }
    }
}
