package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog;

import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores.MediaClient;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class CatalogTypes {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogTypes.class);

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
       // fileDetails.isFolder = file.getIsFolder();
        fileDetails.isFolder = file.getIsFolder() != null ? file.getIsFolder() : false;
        fileDetails.tags = file.getTags();
        fileDetails.sharedWith = file.getSharedWith();
        fileDetails.createdBy = file.getCreatedBy();
        fileDetails.modifiedBy = file.getModifiedBy();
        return fileDetails;
    }

    public static java.io.File fromFile(app.krista.model.base.File file) {
        if (file == null) {
            throw new IllegalArgumentException(Constants.NO_FILE_TO_PARSE);
        }
        try {
            return MediaClient.create().download(MediaClient.createMediaUrl(file.getMediaId()), file.getFileName());
        } catch (IOException cause) {
            LOGGER.error(" {} : {} " + Constants.ERROR_MESSAGE, cause.getMessage());
            throw new IllegalArgumentException(cause);
        }
    }

    public static app.krista.model.base.File toFile(String fileName, java.io.File file) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(Constants.FILE_NAME_IS_EMPTY);
        } else if (file == null) {
            throw new IllegalArgumentException(Constants.NO_FILE_TO_PARSE);
        }
        int lastDot = fileName.lastIndexOf('.');
        String fileExtension = fileName.substring(lastDot + 1);
        String mediaId = MediaClient.create().getMediaID(fileName);
        MediaClient.create().upload(MediaClient.createMediaUrl(mediaId), file);
        return new app.krista.model.base.File(file.length(), file.getName(), fileExtension, mediaId);
    }

}
