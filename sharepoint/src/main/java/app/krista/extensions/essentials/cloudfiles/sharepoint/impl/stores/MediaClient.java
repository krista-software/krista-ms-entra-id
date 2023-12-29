package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

public class MediaClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaClient.class);
    private final WebTarget webTarget;
    private static MediaClient mediaClient = null;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public MediaClient() {
        String mediaUrl = "http://localhost:8081";
        webTarget = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class).build()
                .target(mediaUrl);
        new File(Constants.TEMP_DIR).mkdirs();
    }

    public static MediaClient create() {
        if (mediaClient == null) {
            mediaClient = new MediaClient();
        }
        return mediaClient;
    }

    public static String getMediaID(String key) {
        String fileExtension = key.substring(key.lastIndexOf(".") + 1);

        // when file does not contain any extension at the end
        if (fileExtension.equals(key)) {
            fileExtension = "txt";
        }
        return "doc_binary_" + UUID.randomUUID() + "." + fileExtension;
    }

    public static String createMediaUrl(String mediaId) {
        return "/media/public/" + mediaId;
    }

    private static boolean handleResponse(Response response) {
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)
            return true;
        else if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            LOGGER.debug(Constants.GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_RE_AUTHENTICATION);
            throw new MustAuthorizeException("AuthException: responseStatus: " + response.getStatus() + Constants.SPACE + response.getStatusInfo().getReasonPhrase());
        } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            return false;
        } else {
            throw new ClientErrorException("ClientError: responseStatus: " + response.getStatus() + Constants.SPACE + response.getStatusInfo().getReasonPhrase(), response.getStatus());
        }
    }

    private static File handleFileDownload(Response response, String fileName) throws IOException {
        InputStream is = response.readEntity(InputStream.class);
        File file = new File(Constants.TEMP_DIR + fileName);
        Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return file;
    }

    // region Internal

    public void upload(String url, File file) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Failed to upload file. URL not found.");
        }
        if (file == null) {
            throw new IllegalArgumentException("Failed to upload file. File not found.");
        }
        WebTarget resourceTarget = webTarget.path(url);
        resourceTarget = resourceTarget.queryParam("len", file.length());
        handleResponse(resourceTarget.request().post(Entity.entity(file, APPLICATION_OCTET_STREAM_TYPE)));
    }

    public File download(String url, String fileName) throws IOException {
        Response response = webTarget.path(url).request(MediaType.APPLICATION_OCTET_STREAM).get();
        boolean result = handleResponse(response);
        return result ? handleFileDownload(response, fileName) : null;
    }

    // endregion
}
