package app.krista.extensions.cyber_security.iam.azureAD.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String STATE = "state";
    public static final String REQUIRED_SCOPE = "openid offline_access User.ReadWrite.All UserAuthenticationMethod.ReadWrite.All";
    public static final String CALLBACK_URI = "/azuread/callback";
    public static final String AUTHENTICATION_METHOD_ID = "28c10230-6103-485e-b985-444c60001490";

    // Attribute constants
    public static final String EMAIL = "email";
    public static final String ALLOW_MAIL_ALERT = "allowMailAlert";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String AUTH_TYPE = "authType";
    public static final String TENANT_ID = "tenantId";

    // Auth type constants
    public static final String PUBLIC = "Public";
    public static final String PRIVATE = "Private";
    public static final String UNSUPPORTED_AUTH = "Unsupported authentication type";
    public static final String EMPTY_STRING = "";

    // URL constants
    public static final String LOCAL_EXTN_URL = "localhost:8080";
    public static final String LOCAL_EXTN_REPLACE_URL = "host.docker.internal:8080";
    public static final String DEFAULT_CALLBACK_PATH = "/extension/api/azuread/callback";
    public static final String EXTENSION_FORWARD_PATH = "/rest/azuread/forward";
    public static final String HASH = "#";

    private Constants() {
    }
}
