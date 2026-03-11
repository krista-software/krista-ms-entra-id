/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id;

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants.*;
import static app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Validators.addAttributeIfNotNull;

public final class MicrosoftEntraIDAttributes {
    private static final Logger LOG = LogManager.getLogger(MicrosoftEntraIDAttributes.class);

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

    public MicrosoftEntraIDAttributes(String clientId, String clientSecret, String tenantId, String email,
                                      String authType, String routingUrl) {
        loadPublicConfig();
        this.clientId = clientId == null ? publicClientId : clientId;
        this.clientSecret = clientSecret == null ? publicClientSecret : clientSecret;
        this.tenantId = tenantId;
        this.email = email;
        this.authType = authType;
        this.routingUrl = routingUrl.replace(LOCAL_EXTN_URL, LOCAL_EXTN_REPLACE_URL);
    }

    public static MicrosoftEntraIDAttributes create(JsonObject authPayload, String baseurl) {
        String authType = authPayload.get("authType").getAsString();

        if (Constants.PUBLIC.equals(authType)) {
            return new MicrosoftEntraIDAttributes(null, null, null, authPayload.get(EMAIL).getAsString(),
                    authType, baseurl);
        } else if (Constants.PRIVATE.equals(authType)) {
            return new MicrosoftEntraIDAttributes(authPayload.get(CLIENT_ID).getAsString(), authPayload.get(CLIENT_SECRET).getAsString(),
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
            return routingUrl + "/rest/microsoftentraid/callback";
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
