package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.ManagementDaoImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.base.DateRange;
import app.krista.model.base.FreeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Domain(id = "catEntryDomain_b1bfe7ff-3b3c-4d2c-a9e4-74acd5dae8e3",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "b434c9e2-24f6-4b49-860e-887fd92df6cb")
public class ManagementArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementArea.class);
    private final EventHandler eventHandler;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ManagementDaoImpl managementDao;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;

    @Inject
    public ManagementArea(EventHandler eventHandler,
                          ManagementDaoImpl managementDao, RequestContext requestContext,
                          AuthorizationContext authorizationContext) {
        this.eventHandler = eventHandler;
        this.managementDao = managementDao;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
    }

    @CatalogRequest(
            id = "localDomainRequest_10e4eff1-3185-4c2e-9f2b-77a51c79c363",
            name = "Copy",
            description = "Copy the provide file represented by the entity to the target location",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    public Map<String, Object> copy(
            @Field.Text(name = "Source", attributes = {@Attribute(name = "visualWidth", value = "S")}) String source,
            @Field.Text(name = "Target", attributes = {@Attribute(name = "visualWidth", value = "S")}) String target,
            @Field.Boolean(name = "Replace", attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean replace,
            @Field.Boolean(name = "Create Folder", attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean createFolder) {
        return managementDao.copyFile(source, target, replace, createFolder);
    }

    @CatalogRequest(
            id = "localDomainRequest_4cde43ab-7cc3-4262-84cb-80f8cd44a70f",
            name = "Delete",
            description = "Delete the given file from the cloud storge provider.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> delete(
            @Field.Text(name = "Target", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String target) {
        return managementDao.deleteFile(target);
    }

    @CatalogRequest(
            id = "localDomainRequest_01d4d9c4-43bf-4a6a-8a1e-2ce7365ec9e2",
            name = "Download",
            description = "Retrieves the file from the cloud storage and provides a reference as link, Krista File or both based on provided inputs.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Download Link", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.File(name = "Krista File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> download(
            @Field.Boolean(name = "Download As Link", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean downloadAsLink,
            @Field.Boolean(name = "Download As Krista File", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean downloadAsKristaFile,
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source) {
        return managementDao.downloadFile(downloadAsLink, downloadAsKristaFile, source);
    }

    @CatalogRequest(
            id = "localDomainRequest_2541904d-736c-4bc5-8252-43ebab276639",
            name = "fetch By File Size Range",
            description = "Accepts File Size , Returns Task ID",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String fetchByFileSizeRange(
            @Field(name = "From Size", type = "Number", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Double fromSize,
            @Field(name = "To Size", type = "Number", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Double toSize) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getFileBySizeRange(fromSize, toSize, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_4f052b70-1a7e-4441-9c92-8da5567bd773",
            name = "fetch File By Modification Date Range",
            description = "Accepts Modification Date Range and returns Task ID",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String fetchFileByModificationDateRange(
            @Field.DateRange(name = "Modification Date Range", required = true, showHowManyDaysInViewer = 60) DateRange modificationDateRange) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getFileByDateRange(modificationDateRange, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_22701bdb-9388-42e6-8922-350a018b7cc4",
            name = "Get Info",
            description = "Accepts Source of File and return Task ID which will futher used in Get Info Result",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String getInfo(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sourcePath) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getInfo(sourcePath, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_7615e985-edff-40ef-9fb7-1b3d4d8fe88e",
            name = "Get Info Result",
            description = "Accepts Task ID and Retrieves key information around a single folder in the cloud storage.",
            area = "Management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field(name = "Files", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Folders", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Oldest File", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Oldest File Create Date", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Newest File", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Newest File Date", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Last Modified File Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Last Modified File Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Last Modified File Create Date", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> getInfoResult(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Task ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String taskID) {
        Map<String, Object> payload = new HashMap<>();
        if (eventName.equals(taskID)) {
            payload.put("Files", eventData.get("Files"));
            payload.put("Folders", eventData.get("Folders"));
            payload.put("Size", eventData.get("Size"));
            payload.put("Oldest File", eventData.get("Oldest File"));
            payload.put("Oldest File Create Date", eventData.get("Oldest File Create Date"));
            payload.put("Newest File", eventData.get("Newest File"));
            payload.put("Newest File Date", eventData.get("Newest File Date"));
            payload.put("Last Modified File Id", eventData.get("Last Modified File Id"));
            payload.put("Last Modified File Name", eventData.get("Last Modified File Name"));
            payload.put("Last Modified File Create Date", eventData.get("Last Modified File Create Date"));
            payload.put(Constants.MESSAGE, eventData.get(Constants.MESSAGE));
            payload.put(Constants.SUCCESS, eventData.get(Constants.SUCCESS));
            return payload;
        }
        throw new IllegalStateException("Invalid Input");
    }

    @SuppressWarnings("unchecked")
    @CatalogRequest(
            id = "localDomainRequest_e2407a41-9cc7-4f4b-8c84-51b0983f0fed",
            name = "Get Result",
            description = "Accept task ID as input and return List of Files",
            area = "Management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "Files", type = "[ Entity(File) ]", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> getResult(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Task ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String taskID) {
        if (eventName.equals(taskID)) {
            LOGGER.info(" In ");
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.FILES, eventData.get(Constants.FILES));
            map.put(Constants.MESSAGE, eventData.get(Constants.MESSAGE));
            map.put(Constants.SUCCESS, eventData.get(Constants.SUCCESS));
            return map;
        }
        throw new IllegalStateException("Invalid Input");
    }

    @CatalogRequest(
            id = "localDomainRequest_87f39e6a-4743-4b0b-9958-db94338b2299",
            name = "List Files",
            description = "Produces a non-recursive list of files that is present within the folder denoted by the source path",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String listFiles(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sourcePath,
            @Field.Text(name = "Search", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String search) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getFileByPath(sourcePath, search, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_7695297d-cd0c-4dad-bad9-edca0cacd477",
            name = "List Folders",
            description = "Produces a non-recursive list of folders that is present within the folder denoted by the source path",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String listFolders(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sourcePath,
            @Field.Text(name = "Search", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String search) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getFoldersByPath(sourcePath, search, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_cb6a1310-80e7-4a63-84e2-a1bffe11687e",
            name = "Move",
            description = "Move a file on the drive from source to target where both denote a full file path.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> move(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source,
            @Field.Text(name = "Target", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String target) {
        return managementDao.moveFile(source, target);
    }

    @CatalogRequest(
            id = "localDomainRequest_d13cb137-a4ef-4d1e-8d83-a19ebb2bebea",
            name = "Read",
            description = "Read the content of the file as plain text.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Content", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> read(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source) {
        return managementDao.readTxtFile(source);
    }

    @CatalogRequest(
            id = "localDomainRequest_a2a9201e-af41-46d3-a4aa-b23044eece80",
            name = "Rename",
            description = "Renames the file given at the Source File Path to the name Given",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> rename(
            @Field.Desc(name = "Rename File", type = "Entity(File)", required = true) File renameFile,
            @Field.Text(name = "Target File Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String targetFileName) {
        return managementDao.renameFile(renameFile, targetFileName);
    }

    @CatalogRequest(
            id = "localDomainRequest_da1e91d5-0636-4c31-bbd2-56cf50a381a1",
            name = "Share File",
            description = "Share the file denoted by source with the given users identified by their email address applying the provided Permissions",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> shareFile(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source,
            @Field.Desc(name = "Share", type = "[ { User: Email, Permissions: PickOne(Read|Write|Read and Write) } ]", required = true) List<Map<String, Object>> share) {
        return managementDao.shareFile(source, share);
    }

    @CatalogRequest(
            id = "localDomainRequest_1060751d-fb89-48da-8cb6-5d9232d48a21",
            name = "Upload",
            description = "Upload the content of the file from the media server to the cloud storage at target path.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> upload(
            @Field.File(name = "Krista File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) app.krista.model.base.File kristaFile,
            @Field.Text(name = "Target Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String targetPath) {
        return managementDao.uploadFile(kristaFile, targetPath);
    }

    @CatalogRequest(
            id = "localDomainRequest_9a5dbf33-2803-4979-bb1b-736230bf7d3c",
            name = "Upload Text",
            description = "Uploads the file content to the cloud storage at the given target location as a plain text file.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> uploadText(
            @Field(name = "File Content", type = "Paragraph", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String fileContent,
            @Field.Text(name = "Target", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String target) {
        return managementDao.uploadFileAsTxt(fileContent, target);
    }

}