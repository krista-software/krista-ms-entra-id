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
    public static final String PUBLIC_CALLBACK_PATH = "/rest/azuread/callback";
    public static final String DEFAULT_CALLBACK_PATH = "/extension/api/rest/v3/oauth/callback";
    public static final String EXTENSION_FORWARD_PATH = "/rest/azuread/v3/oauth/callback";
    public static final String HASH = "#";

    // Authentication constants
    public static final String AUTH_CONTEXT_ID = "authContextId";
    public static final String INVALID_STATE_PARAMETERS = "Invalid state parameters!";
    public static final String UNAUTHORISED_USER = "Unauthorised User.";
    public static final String USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES = "User Authenticated Successfully. Save the changes.";
    public static final String ERROR_OCCURRED_DURING_AUTHORIZATION = "Error occurred during authorization: ";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String AUTH_URL_QUERY_PARAMS = "&prompt=select_account";
    public static final String UNDER_SCORE = "_";
    public static final String WS_CONTACT = "ws_contact";

    private Constants() {
    }
}
