package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities;

import java.util.Map;
import java.util.List;
import app.krista.extension.impl.anno.*;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;

@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion ="a8626fe2-4d91-4667-b6ae-4f60bb914ccf")
@Entity(name = "File", id = "localDomainEntity_292c2a1d-0d6c-4307-a4d1-04176dd5a315", primaryKey = "Absolute Path", options = {})
@Searchable
public class File {

    @Searchable
    @ToString
    @Field.Text(name = "Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String name;

    @Searchable
    @Field.Text(name = "Base Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String baseName;

    @Searchable
    @Field.Text(name = "Extension", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String extension;

    @Searchable
    @ToString
    @Field.Text(name = "Parent Folder", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String parentFolder;

    @Searchable
    @Field.Text(name = "Absolute Path", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String absolutePath;

    @Searchable
    @Field.Text(name = "Owner", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String owner;

    @Searchable
    @ToString
    @Field.Date(name = "Created", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long created;

    @Searchable
    @ToString
    @Field.Date(name = "Modified", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long modified;

    @Searchable
    @ToString
    @Field(name = "Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Double size;

    @Field.PickOne(name = "Permissions", values = {"Read","Write","Read and Write","Own"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String permissions;

    @Searchable
    @ToString
    @Field.Boolean(name = "Is Folder", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean isFolder;

    @Field.Desc(name = "Tags", type = "[ Text ]",required = false)
    public List<String> tags;

    @Field.Desc(name = "Shared With", type = "[ { User Email: Email, Permission: PickOne(Read|Write|Read and Write|Own) } ]",required = false)
    public List<Map<String, Object>> sharedWith;

    @Field.Text(name = "Created By", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String createdBy;

    @Field.Text(name = "Modified By", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String modifiedBy;

    public static File fromFile(FileService file) {
        if (file == null) {
            return new File();
        }
        File fileDetails = new File();
        fileDetails.name = file.getName();
        fileDetails.baseName = file.getBaseName();
        fileDetails.extension = file.getExtension();
        fileDetails.parentFolder = file.getParentFolder();
        fileDetails.absolutePath = file.getAbsolutePath();
        fileDetails.owner = file.getOwner();
        fileDetails.created = file.getCreated();
        fileDetails.modified = file.getModified();
        fileDetails.size = file.getSize();
        fileDetails.permissions = file.getPermissions();
        fileDetails.isFolder = file.getIsFolder() != null && file.getIsFolder();
        fileDetails.tags = file.getTags();
        fileDetails.sharedWith = file.getSharedWith();
        fileDetails.createdBy = file.getCreatedBy();
        fileDetails.modifiedBy = file.getModifiedBy();
        return fileDetails;
    }

}