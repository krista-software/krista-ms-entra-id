package app.krista.extensions.cyber_security.iam.azureAD;

import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants.*;
import static app.krista.extensions.cyber_security.iam.azureAD.impl.util.Validators.addAttributeIfNotNull;

public final class AzureADAttributes {
    private static final Logger LOG = LogManager.getLogger(AzureADAttributes.class);

    @SerializedName(EMAIL)
    private final String email;
    @SerializedName(CLIENT_ID)
    private final String clientId;
    @SerializedName(CLIENT_SECRET)
    private final String clientSecret;
    @SerializedName(AUTH_TYPE)
    private final String authType;
    @SerializedName(TENANT_ID)
    private final String tenantId;

    private final String routingUrl;
    private String publicClientId;
    private String publicClientSecret;

    public AzureADAttributes(String clientId, String clientSecret, String tenantId, String email,
                             String authType, String routingUrl) {
        loadPublicConfig();
        this.clientId = clientId == null ? publicClientId : clientId;
        this.clientSecret = clientSecret == null ? publicClientSecret : clientSecret;
        this.tenantId = tenantId;
        this.email = email;
        this.authType = authType;
        this.routingUrl = routingUrl.replace(LOCAL_EXTN_URL, LOCAL_EXTN_REPLACE_URL);
    }

    public static AzureADAttributes create(JsonObject authPayload, String baseurl) {
        String authType = authPayload.get("authType").getAsString();

        if (Constants.PUBLIC.equals(authType)) {
            return new AzureADAttributes(null, null, null, authPayload.get(EMAIL).getAsString(),
                    authType, baseurl);
        } else if (Constants.PRIVATE.equals(authType)) {
            return new AzureADAttributes(authPayload.get(CLIENT_ID).getAsString(), authPayload.get(CLIENT_SECRET).getAsString(),
                    authPayload.get(TENANT_ID).getAsString(), authPayload.get(EMAIL).getAsString(),
                    authType, baseurl);
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_AUTH);
        }
    }

    private void loadPublicConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(inputStream);
            this.publicClientId = properties.getProperty("public.clientId");
            this.publicClientSecret = properties.getProperty("public.clientSecret");
        } catch (IOException cause) {
            throw new RuntimeException("Failed to load public configuration file.", cause);
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCallbackEndPoint() {
        if (Constants.PUBLIC.equals(authType)) {
             return routingUrl.replaceAll("/extension/api/.*", DEFAULT_CALLBACK_PATH);
        } else if (Constants.PRIVATE.equals(authType)) {
            return routingUrl + "/rest/azuread/callback";
        } else {
            return EMPTY_STRING;
        }
    }

    public String getForwardPath() {
        if (Constants.PUBLIC.equals(authType)) {
            return routingUrl + EXTENSION_FORWARD_PATH;
        } else {
            return EMPTY_STRING;
        }
    }

    public String getEmail() {
        return email;
    }

    public String getAuthType() {
        return authType;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> attributesMap = new HashMap<>();
        addAttributeIfNotNull(attributesMap, CLIENT_ID, getClientId());
        addAttributeIfNotNull(attributesMap, CLIENT_SECRET, getClientSecret());
        addAttributeIfNotNull(attributesMap, TENANT_ID, getTenantId());
        addAttributeIfNotNull(attributesMap, EMAIL, getEmail());
        addAttributeIfNotNull(attributesMap, AUTH_TYPE, getAuthType());
        return attributesMap;
    }

}
