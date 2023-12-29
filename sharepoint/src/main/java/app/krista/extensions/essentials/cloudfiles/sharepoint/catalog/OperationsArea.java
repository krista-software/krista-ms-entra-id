package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.OperationsAreaImpl;

import javax.inject.Inject;
import java.util.Map;

@Domain(id = "catEntryDomain_b1bfe7ff-3b3c-4d2c-a9e4-74acd5dae8e3",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "f61a9799-2fd1-47d0-b9b2-b017b6245867")
public class OperationsArea {

    private final OperationsAreaImpl operationsAreaImpl;

    @Inject
    public OperationsArea(OperationsAreaImpl operationsAreaImpl) {
        this.operationsAreaImpl = operationsAreaImpl;
    }

    @CatalogRequest(
            id = "localDomainRequest_257b5e27-e375-40e4-b72b-a071e501df4d",
            name = "Create List",
            description = "Creates a new list in the specified SharePoint site.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)",required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> createList(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName,
            @Field.Text(name = "List Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String listName) {
        return operationsAreaImpl.createList(siteName, listName);
    }

    @CatalogRequest(
            id = "localDomainRequest_7ddb2418-b628-430b-a32a-3a8fb998ab33",
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
            id = "localDomainRequest_8abcc65c-962c-4631-aa0f-9e1d5de57967",
            name = "Delete Site",
            description = "Deletes a SharePoint Site/Group with the specified name.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> deleteSite(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName) {
        return operationsAreaImpl.deleteSite(siteName);
    }

    @CatalogRequest(
            id = "localDomainRequest_aa60758b-906c-40e0-827a-bfdc2d72f94e",
            name = "Update List",
            description = "Updates an existing SharePoint list with the provided display name and description.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)",required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> updateList(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName,
            @Field.Text(name = "List Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String listName,
            @Field.Text(name = "Display Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String displayName,
            @Field(name = "Description", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String description) {
        return operationsAreaImpl.updateList(siteName, listName, displayName, description);
    }

    @CatalogRequest(
            id = "localDomainRequest_6d5f19fb-8b13-4870-a089-7a3e13905ff0",
            name = "Update Site",
            description = "Updates the display name of an existing SharePoint site.",
            area = "Operations",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "File", type = "Entity(File)",required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> updateSite(
            @Field.Text(name = "Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String siteName,
            @Field.Text(name = "New Site Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String newSiteName) {
        return operationsAreaImpl.updateSite(siteName, newSiteName);
    }

}