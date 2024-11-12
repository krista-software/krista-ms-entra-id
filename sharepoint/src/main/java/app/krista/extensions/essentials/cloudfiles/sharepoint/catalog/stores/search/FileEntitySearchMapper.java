package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search;

import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileService;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileEntitySearchMapper implements FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileEntitySearchMapper.class);
    private final DriveItem driveItem;
    private final String absolutePath;

    private final GraphServiceClient<Request> graphClient;

    public FileEntitySearchMapper(DriveItem driveItem, String sitePath, GraphServiceClient<Request> graphClient) {
        this.driveItem = driveItem;
        this.graphClient = graphClient;
        this.absolutePath = sitePath != null ? getFilePath(driveItem, sitePath) : extractPathFromDriveItem(driveItem);
    }

    @Override
    public String getName() {
        return driveItem.name;
    }

    @Override
    public String getBaseName() {
        return FileUtil.getBaseName(driveItem.name != null ? driveItem.name : null).get(0);
    }

    @Override
    public String getExtension() {
        return FileUtil.getBaseName(driveItem.name != null ? driveItem.name : null).get(1);
    }


    @Override
    public String getParentFolder() {
        return FileUtil.getParentPath(this.absolutePath);
    }

    @Override
    public String getAbsolutePath() {
        return this.absolutePath;
    }

    @Override
    public String getOwner() {
        if (driveItem.createdBy != null) {
            assert driveItem.createdBy.user != null;
            return driveItem.createdBy.user.displayName;
        } else {
            return null;
        }
    }

    @Override
    public Long getCreated() {
        return driveItem.createdDateTime != null ? Timestamp.valueOf(driveItem.createdDateTime
                .toLocalDateTime()).getTime() : null;
    }

    @Override
    public Long getModified() {
        return driveItem.lastModifiedDateTime != null ? Timestamp.valueOf(driveItem.lastModifiedDateTime
                .toLocalDateTime()).getTime() : null;
    }

    @Override
    public Double getSize() {
        return driveItem.size != null ? (double) driveItem.size : null;
    }

    @Override
    public String getPermissions() {
        return null;
    }

    @Override
    public List<String> getTags() {
        return List.of();
    }

    @Override
    public Boolean getIsFolder() {
        return driveItem.folder != null;
    }

    @Override
    public List<Map<String, Object>> getSharedWith() {
        return Collections.emptyList();
    }

    @Override
    public String getCreatedBy() {
        if (driveItem.createdBy == null) {
            return null;
        }
        return driveItem.createdBy.user != null ? driveItem.createdBy.user.displayName : null;
    }

    @Override
    public String getModifiedBy() {
        if (driveItem.lastModifiedBy == null) {
            return null;
        }
        return driveItem.lastModifiedBy.user != null ? driveItem.lastModifiedBy.user.displayName : null;
    }


    private String getFilePath(DriveItem driveItem, String sitePath) {
        if (driveItem.parentReference != null && driveItem.parentReference.path != null) {
            String relativePath = FileUtil.getRootRelativeFilePath(driveItem.parentReference.path);
            String modifiedRelativePath = relativePath.isBlank() ? "" : relativePath + Constants.SLASH;
            return sitePath + Constants.SLASH + modifiedRelativePath + driveItem.name;
        }
        return null;
    }

    public String extractPathFromDriveItem(DriveItem driveItem) {
        return driveItem.webUrl != null
                ? getFullFilePath(driveItem)
                : null;
    }

    private String getFullFilePath(DriveItem driveItem) {
        try {
            if (Objects.requireNonNull(driveItem.webUrl).contains(".aspx?")) {
                DriveItem item = graphClient.drives(Objects.requireNonNull(driveItem.parentReference.driveId)).
                        items(Objects.requireNonNull(driveItem.id)).buildRequest().get();
                return getFilePath(Objects.requireNonNull(item), extractSitePathFromURL(driveItem.webUrl));

            } else {
                return extractFullPath(driveItem.webUrl);
            }
        } catch (GraphServiceException | IllegalArgumentException cause) {
            LOGGER.error("Error while fetching path {} {} :", cause, cause.getMessage());
        }
        return null;
    }

    public static String extractSitePathFromURL(String url) {
        String extractedPath = "";
        int sitesIndex = url.indexOf(Constants.SITE_RESOURCE);
        if (sitesIndex != -1) {
            int startIndex = sitesIndex + Constants.SITE_RESOURCE.length();
            int endIndex = url.indexOf(Constants.SLASH, startIndex);
            if (endIndex != -1) {
                int secondEndIndex = url.indexOf(Constants.SLASH, endIndex + 1);
                if (secondEndIndex != -1) {
                    extractedPath = url.substring(startIndex, secondEndIndex);
                } else {
                    extractedPath = url.substring(startIndex);
                }
                extractedPath = Constants.SLASH + extractedPath.replace("%20", " "); // Replace %20 with space
            }
        }
        return extractedPath;

    }

    public static String extractFullPath(String url) {
        String extractedPath = "";
        int sitesIndex = url.indexOf("/sites");
        if (sitesIndex != -1) {
            extractedPath = url.substring(sitesIndex + "/sites".length());
        }
        return extractedPath;
    }


}

