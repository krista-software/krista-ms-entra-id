package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.ManagementDaoImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.ResponseUtil;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.base.FreeForm;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "dbcdd7cf-a461-402f-9459-3bc205bf6dce")
public class ManagementArea {

    private final EventHandler eventHandler;
    private final ManagementDaoImpl managementDao;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final GraphServiceClientProvider provider;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public ManagementArea(EventHandler eventHandler,
                          ManagementDaoImpl managementDao, RequestContext requestContext,
                          AuthorizationContext authorizationContext, GraphServiceClientProvider provider) {
        this.eventHandler = eventHandler;
        this.managementDao = managementDao;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.provider = provider;
    }

    @CatalogRequest(
            id = "localDomainRequest_b277227d-746c-469e-8bed-75233b6b6133",
            name = "Copy",
            description = "Copy the provide file represented by the entity to the target location",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> copy(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source,
            @Field.Text(name = "Target", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String target,
            @Field.Boolean(name = "Replace", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean replace,
            @Field.Boolean(name = "Create Folder", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean createFolder) {
        return managementDao.copyFile(source, target, replace, createFolder);
    }

    @CatalogRequest(
            id = "localDomainRequest_6518b866-ac60-46f7-b77a-fb7f31af257d",
            name = "Delete",
            description = "Delete the given file from the cloud storage provider.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> delete(
            @Field.Text(name = "Target", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String target) {
        return managementDao.deleteFile(target);
    }

    @CatalogRequest(
            id = "localDomainRequest_d0aea3ac-2059-4ce4-85e8-626b42e64c0c",
            name = "Download as File",
            description = "Retrieves the File from the cloud storage and provides a reference as Krista File based on provided source location.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Krista File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Message", type = "RichText", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public Map<String, Object> downloadAsFile(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source) {
        return managementDao.downloadFileAsFile(source);
    }

    @CatalogRequest(
            id = "localDomainRequest_37d526ce-6741-4523-b61f-d77451005159",
            name = "Download as Link",
            description = "Retrieves the File from the cloud storage as a hyperlink based on provided source location.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Link", type = "RichText", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public Map<String, Object> downloadAsLink(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source) {
        return managementDao.downloadFileAsLink(source);
    }

    @CatalogRequest(
            id = "localDomainRequest_8d92b36c-792d-4cfe-b63d-c1dededbf075",
            name = "Get Info",
            description = "Accepts the source of the file and returns a Task ID, which will be further used to retrieve result information.",
            area = "Management",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String getInfo(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sourcePath) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        if (!useEmail) {
            provider.validateToken(accountID);
        }
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getInfo(sourcePath, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_be76f28f-ffca-46ee-9b27-6a8045cb7177",
            name = "Get Info Result",
            description = "Accepts Task ID and Retrieves key information around a single folder in the cloud storage.",
            area = "Management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field(name = "Files", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Folders", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Oldest File", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Newest File", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Last Modified File Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Last Modified File Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Oldest File Create Date", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Newest File Date", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Last Modified File Create Date", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> getInfoResult(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Task ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String taskID) {
        Map<String, Object> payload = new HashMap<>();
        if (eventName.equals(taskID)) {
            ResponseUtil.getFileEventData(eventData, payload);
            return payload;
        }
        return Map.of(Constants.MESSAGE, eventData.get(Constants.MESSAGE), Constants.SUCCESS, eventData.get(Constants.SUCCESS));
    }

    @CatalogRequest(
            id = "localDomainRequest_39c4d468-6f5f-4176-a241-a01dcadad9a0",
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
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.FILES, eventData.get(Constants.FILES));
            map.put(Constants.MESSAGE, eventData.get(Constants.MESSAGE));
            map.put(Constants.SUCCESS, eventData.get(Constants.SUCCESS));
            return map;
        }
        throw new IllegalStateException("Invalid Input");
    }

    @CatalogRequest(
            id = "localDomainRequest_f5f0a870-d4ab-4194-b673-1142f21c005a",
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
        if (!useEmail) {
            provider.validateToken(accountID);
        }
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getFileByPath(sourcePath, FileUtil.validateTextInput(search), useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_3f16a853-5d28-4af7-8d4c-6f8eab2d341a",
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
        if (!useEmail) {
            provider.validateToken(accountID);
        }
        executorService.submit(() -> {
            FreeForm freeForm = managementDao.getFoldersByPath(sourcePath, FileUtil.validateTextInput(search), useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }


    @CatalogRequest(
            id = "localDomainRequest_691267f6-205e-4143-9032-88b2a81abdd9",
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
            id = "localDomainRequest_4c6bb990-da44-473b-b697-4527494484fd",
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
            id = "localDomainRequest_7c5d230a-12eb-4eee-82cf-0b6c0a5256cf",
            name = "Rename",
            description = "Renames the file given at the Source File Path to the name Given",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> rename(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source,
            @Field.Text(name = "Target File Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String targetFileName) {
        return managementDao.renameFile(source, targetFileName);
    }

    @CatalogRequest(
            id = "localDomainRequest_d66a7825-5d45-4486-8a00-1de62dd1ddd3",
            name = "Share File",
            description = "Share the file denoted by source with the given users identified by their email address applying the provided Permissions",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> shareFile(
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source,
            @Field.Desc(name = "Share", type = "[ { User: Email, Permissions: PickOne(Read|Write|Read and Write|Own) } ]", required = true) List<Map<String, Object>> share) {
        return managementDao.shareFile(source, share);
    }

    @CatalogRequest(
            id = "localDomainRequest_7842cdaf-513b-4b52-a70a-1d7c1c1c5fc6",
            name = "Write",
            description = "Upload the content of the file from the media server to the cloud storage at target path.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> write(
            @Field.File(name = "Krista File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) app.krista.model.base.File kristaFile,
            @Field.Text(name = "Target Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String targetPath) {
        return managementDao.uploadFile(kristaFile, targetPath);
    }

    @CatalogRequest(
            id = "localDomainRequest_dcaada19-6e50-4b2d-95e6-6ec99a8d738d",
            name = "Write Text",
            description = "Uploads the file content to the cloud storage at the given target location as a plain text file.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> writeText(
            @Field(name = "File Content", type = "Paragraph", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String fileContent,
            @Field.Text(name = "Target", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String target) {
        return managementDao.uploadFileAsTxt(fileContent, target);
    }

    @CatalogRequest(
            id = "localDomainRequest_799daa46-a5c5-47ad-9b39-094ee2b9d48d",
            name = "Changes Since",
            description = "This endpoint produces a list of recent updates within a specified folder since a given date. The updates include information about the path, file status (e.g., Created, Updated, Deleted), and can be filtered using an optional regex in the search variable.",
            area = "Management",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Result", type = "[ { Path: Text, Date: Date, Update: PickOne(Created|Updated|Deleted) } ]", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Date(name = "Date1", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> changesSince(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sourcePath,
            @Field.Text(name = "Search", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String search,
            @Field.Date(name = "Since", required = true, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = false, allowFuture = false, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Long since) {
        return managementDao.getVersionHistory(sourcePath, FileUtil.validateTextInput(search), since);
    }

    @CatalogRequest(
            id = "localDomainRequest_d6ab2648-df6b-4429-aeef-a56c75fde191",
            name = "When a File is Changed",
            description = "Monitors the Cloud Storage for changes. When a change arrives and the affected files/folders match the source path and event type, the event will be triggered.",
            area = "Management",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.PickOne(name = "Event", values = {"Created", "Updated", "Deleted", "Shared"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> whenAFileIsChanged(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Source", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String source,
            @Field.PickOne(name = "Event", values = {"Created", "Updated", "Deleted", "Shared"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String event,
            @Field.Text(name = "Search", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String search) {

        if (eventName.equalsIgnoreCase(Constants.FILE_RECEIVED)
                && eventData.get(Constants.EVENT) != null && !eventData.get(Constants.EVENT).equals(Constants.NO_CHANGE)
                && isInfoValid(eventData, search, source, event)) {
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.FILE, eventData.get(Constants.FILE));
            map.put(Constants.EVENT, eventData.get(Constants.EVENT));
            return map;
        }
        throw new IllegalStateException(Constants.INVALID_INPUT);
    }


    private boolean isInfoValid(FreeForm eventData, String search, String source, String event) {
        if (event != null && !eventData.get(Constants.EVENT).equals(event)) {
            return false;
        }
        app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity = (app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File) eventData.get(Constants.FILE);
        if (isContainsSource(fileEntity, source) && isContainsSearch(fileEntity, search)) {
            return true;

        } else return isContainsSource(fileEntity, source) && isContainsSearch(fileEntity, search);
    }

    private boolean isContainsSource(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File fileEntity, String source) {
        return source != null && fileEntity.absolutePath != null && FileUtil.isPathContainsInput(fileEntity.absolutePath, source);
    }

    private boolean isContainsSearch(app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File eventData, String search) {
        if (search != null && !search.isEmpty() && eventData.name != null && eventData.name.contains(search)) {
            return true;
        }
        return search == null;
    }


}