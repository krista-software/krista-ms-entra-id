package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities;

import java.util.Map;
import java.util.List;

import app.krista.extension.impl.anno.*;

@Domain(id = "catEntryDomain_b1bfe7ff-3b3c-4d2c-a9e4-74acd5dae8e3",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion ="6991c074-ff58-4466-944c-d0855e9ff1df")
@Entity(name = "File", id = "localDomainEntity_f276c124-9f7a-4836-be9c-669bcd2c66ab", primaryKey = "Absolute Path", options = {})
public class File {

    @Searchable
    @ToString
    @Field.Text(name = "Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String name;

    @Field.Text(name = "Base Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String baseName;

    @Searchable
    @ToString
    @Field.Text(name = "Extension", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String extension;

    @ToString
    @Field.Text(name = "Parent Folder", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String parentFolder;

    @Searchable
    @ToString
    @Field.Text(name = "Absolute Path", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String absolutePath;

    @Searchable
    @ToString
    @Field.Text(name = "Owner", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String owner;

    @ToString
    @Field.Date(name = "Created", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long created;

    @ToString
    @Field.Date(name = "Modified", required = false, includeTimeOfDay = true, showHowManyDaysInViewer = 60, allowPast = true, allowToday = true, allowFuture = true, defaultTimeSpan = 1, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long modified;

    @ToString
    @Field(name = "Size", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Double size;

    @ToString
    @Field.PickOne(name = "Permissions", values = {"Read", "Write", "Read and Write"}, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String permissions;

    @Field.Boolean(name = "Is Folder", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean isFolder;

    @Field.Desc(name = "Tags", type = "[ Text ]", required = false)
    public List<String> tags;

    @Field.Desc(name = "Shared With", type = "[ { User Email: Email, Permission: PickOne(Read|Write|Read and Write) } ]", required = false)
    public List<Map<String, Object>> sharedWith;

    @Field.Text(name = "Created By", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String createdBy;

    @Field.Text(name = "Modified By", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String modifiedBy;

}
