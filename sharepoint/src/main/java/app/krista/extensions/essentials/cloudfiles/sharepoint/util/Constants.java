package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CODE = "code";
    public static final String STATE = "state";
    public static final String INVALID_STATE_PARAMETERS = "Invalid state parameters!";
    public static final String WS_CONTACT = "wsContact";
    public static final String IGNORED_EXCEPTION = "IGNORED EXCEPTION";
    public static final String SUCCESS = "Success";
    public static final String SPACE = " ";
    public static final String UNAUTHORISED_USER = "Unauthorised user.";
    public static final String USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES = "User authenticated successfully. Save the changes.";
    public static final String ERROR_OCCURRED_DURING_AUTHORIZATION = "Error occurred during authorization";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String ACCESS_TYPE_OFFLINE_APPROVAL_PROMPT_FORCE = "&access_type=offline&approval_prompt=force";
    public static final String TEXT = "Text";
    public static final String HASH = "#";
    public static final String REQUIRED_SCOPE = "openid offline_access Mail.Send Mail.ReadWrite Mail.Send.Shared Mail.ReadWrite.Shared";
    public static final String SCOPE_SEPARATOR = " ";
    public static final String AUTHORITY = "https://login.microsoftonline.com/";
    public static final String USER_ID = "userId";
    public static final String AUTH_CONTEXT_ID = "authContextId";
    public static final String YOU_ARE_NOT_AUTHORIZED_PLEASE_AUTHORIZE_YOURSELF = "You are not authorized. Please authorize yourself";
    public static final String REFRESH_TOKEN_IS_NO_LONGER_VALID_PLEASE_REAUTHORIZE_YOURSELF = "Refresh Token is no longer valid. Please reauthorize yourself";
    public static final String FAILED_TO_GET_ACCOUNT = "Failed to get account";
    public static final String REST_SHAREPOINT_CALLBACK = "/rest/sharepoint/callback";
    public static final String CREATED = "created";
    public static final String REST_OUTLOOK_MAIL_NOTIFICATION = "/rest/outlook/mailNotification";
    public static final String YYYY_MM_DD_T_HH_MM_SS_SSSSSSS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'";
    public static final String ME_MAIL_FOLDERS_INBOX_MESSAGES = "/mailFolders('Inbox')/messages";
    public static final String REST_OUTLOOK_LIFECYCLE_NOTIFICATION = "/rest/outlook/lifecycleNotification";
    public static final String TEMP_DIR = "/tmp/krista/extension/outlook/";
    public static final String GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_RE_AUTHENTICATION = "Got error for Authentication. So sending for re authentication.";
    public static final String GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_AUTHENTICATION = "Got error for Authentication. So sending for authentication";
    public static final String SHAREPOINT_FILE_ID = "sharePointFileId";
    public static final String FILE_RECEIVED = "fileReceived";
    public static final String POST_METHOD = "POST";
    public static final String GET_REQUEST = "GET";
    public static final String PATCH_METHOD = "PATCH";
    public static final String APPLICATION_JSON = "application/json";
    public static final String MESSAGE = "Message";
    public static final String FILE = "File";
    public static final String FILES = "Files";
    public static final String IS_FILE_EXIST = "isFileExist";

    private Constants() {
    }
}
