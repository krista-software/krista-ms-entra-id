package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
public final class CatalogTypes {

    private CatalogTypes() {
    }

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
