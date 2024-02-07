package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.SharepointAreaImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileDao;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "fcbe44a3-40b8-45bd-866e-f28f13cee132")
public class SharepointArea {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharepointArea.class);
    private final SharepointAreaImpl sharepointAreaImpl;
    private final EventHandler eventHandler;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final FileDao fileDaoImpl;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public SharepointArea(SharepointAreaImpl sharepointAreaImpl, EventHandler eventHandler,
                          RequestContext requestContext, AuthorizationContext authorizationContext, FileDao fileDaoImpl) {
        this.sharepointAreaImpl = sharepointAreaImpl;
        this.eventHandler = eventHandler;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.fileDaoImpl = fileDaoImpl;
    }

    @CatalogRequest(
            id = "localDomainRequest_db82d4fb-90d2-4da3-ab33-10ee1ff73d66",
            name = "Get Shared Permission",
            description = "Retrieve a list of users who have been granted access to a specific file, including those with shared access and those who possess permission to interact with the file.",
            area = "Sharepoint",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String getSharedPermission(
            @Field.Text(name = "Source Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")},
                    options = {}) String sourcePath) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = sharepointAreaImpl.getSharedPermission(sourcePath, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }


    @CatalogRequest(
            id = "localDomainRequest_940bb32f-25ac-499b-b52f-816c8600ec49",
            name = "Get Response",
            description = "Accept task ID as input and return List of Files",
            area = "Sharepoint",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "Files", type = "[ Entity(File) ]", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> getResponse(
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
            id = "localDomainRequest_b14e9456-34df-4011-a120-91c6597723a0",
            name = "Search By Mail",
            description = "Searches for files in SharePoint based on the email addresses of the users who created and last modified the files.",
            area = "Sharepoint",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String searchByMail(
            @Field.Text(name = "Email", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String email,
            @Field.PickOne(name = "User", values = {"Created By", "Modified By"}, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String user) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = sharepointAreaImpl.getSearchByMail(email, user, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_36159d86-8ae9-4b4c-80a9-2dfc9aa330e5",
            name = "fetch By File Size Range",
            description = "Accepts File Size , Returns Task ID",
            area = "Sharepoint",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String fetchByFileSizeRange(
            @Field(name = "From Size", type = "Number", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Double fromSize,
            @Field(name = "To Size", type = "Number", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Double toSize) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = sharepointAreaImpl.getFileBySizeRange(fromSize, toSize, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_040b6201-84a2-40ea-9b3f-f98530b5d302",
            name = "fetch File By Modification Date Range",
            description = "Accepts Modification Date Range and returns Task ID",
            area = "Sharepoint",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Text(name = "Task ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String fetchFileByModificationDateRange(
            @Field.DateRange(name = "Modification Date Range", required = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {}, options = {}) DateRange modificationDateRange) {
        final String taskId = UUID.randomUUID().toString();
        boolean useEmail = !requestContext.invokeAsUser();
        String accountID = authorizationContext.getAuthorizedAccount().getAccountId();
        executorService.submit(() -> {
            FreeForm freeForm = sharepointAreaImpl.getFileByDateRange(modificationDateRange, useEmail, accountID);
            eventHandler.handleEvent(taskId, freeForm);
        });
        return taskId;
    }

    @CatalogRequest(
            id = "localDomainRequest_8ca7ad64-b226-401e-9520-e36bb40c0a8d",
            name = "Create Group",
            description = "Create group at Sharepoint .",
            area = "Sharepoint",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Desc(name = "File", type = "Entity", required = false)
    public Map<String, Object> createGroup(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupName,
            @Field.Text(name = "Mail Nick Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String mailNickName) {
        return fileDaoImpl.createGroup(groupName, mailNickName);
    }

}
