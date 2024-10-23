package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.OperationsAreaImpl;

import javax.inject.Inject;
import java.util.Map;

@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "fcbe44a3-40b8-45bd-866e-f28f13cee132")
public class OperationsArea {

    private final OperationsAreaImpl operationsAreaImpl;

    @Inject
    public OperationsArea(OperationsAreaImpl operationsAreaImpl) {
        this.operationsAreaImpl = operationsAreaImpl;
    }

    @CatalogRequest(
            id = "localDomainRequest_ffcd5ccf-dd66-482a-9b27-53b9dce8bd3c",
            name = "Create List",
            description = "Creates a new list in the specified SharePoint site.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> createList(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName,
            @Field.Text(name = "List Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String listName) {
        return operationsAreaImpl.createList(siteName, listName);
    }


    @CatalogRequest(
            id = "localDomainRequest_4fe8e573-df22-4c9b-81a5-bec735a687c3",
            name = "Delete List",
            description = "Deletes a SharePoint list identified by its site name and list name.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> deleteList(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName,
            @Field.Text(name = "List Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String listName) {
        return operationsAreaImpl.deleteList(siteName, listName);
    }

    @CatalogRequest(
            id = "localDomainRequest_99f0f63d-263d-4c8e-9e6a-e4eb7092fbd3",
            name = "Delete Group",
            description = "Deletes a SharePoint Group with the specified name.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> deleteGroup(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupName) {
        return operationsAreaImpl.deleteGroup(groupName);
    }

    @CatalogRequest(
            id = "localDomainRequest_b6c09a0b-4693-4e8a-b9b3-0b6c47c5ad4b",
            name = "Update List",
            description = "Updates an existing SharePoint list with the provided display name and description.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)", required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Success'")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> updateList(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName,
            @Field.Text(name = "List Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String listName,
            @Field.Text(name = "Display Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String displayName,
            @Field(name = "Description", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String description) {
        return operationsAreaImpl.updateList(siteName, listName, displayName, description);
    }


    @CatalogRequest(
            id = "localDomainRequest_92dede25-2e0e-47c4-beee-5dba787b0435",
            name = "Update Group",
            description = "Updates the display name of an existing SharePoint group.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)",required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> updateGroup(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupName,
            @Field.Text(name = "New Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String newGroupName) {
        return operationsAreaImpl.updateGroup(groupName, newGroupName);
    }

    @CatalogRequest(
            id = "localDomainRequest_ad41172b-aeac-47c0-afff-04a8cc2df1bd",
            name = "Create Group",
            description = "Create group in Sharepoint",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)",required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> createGroup(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupName,
            @Field.Text(name = "Mail Nick Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String mailNickName) {
        return operationsAreaImpl.createGroup(groupName, mailNickName);
    }
}