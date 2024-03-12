package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String CODE = "code";

    public static final String STATE = "state";

    public static final String INVALID_STATE_PARAMETERS = "Invalid state parameters!";

    public static final String WS_CONTACT = "wsContact";

    public static final String IGNORED_EXCEPTION = "IGNORED EXCEPTION";

    public static final String PLEASE_PROVIDE_VALID_TARGET_PATH = "Please, Provide Valid Target Path";

    public static final String PLEASE_PROVIDE_VALID_FOLDER_PATH = "Please, Provide Valid Folder Path";

    public static final String UNAUTHORISED_USER = "Unauthorised user.";

    public static final String USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES = "User authenticated successfully. Save the changes.";

    public static final String ERROR_OCCURRED_DURING_AUTHORIZATION = "Error occurred during authorization";

    public static final String ERROR_DESCRIPTION = "error_description";

    public static final String ACCESS_TYPE_OFFLINE_APPROVAL_PROMPT_FORCE = "&access_type=offline&approval_prompt=force&prompt=select_account";

    public static final String TEXT = "Text";

    public static final String ERROR_OCCURRED_DURING_COPYING_FILE = "Error occurred during copying file";

    public static final String COPY_IN_PROGRESS = "Copy operation is in progress";

    public static final String HASH = "#";

    public static final String REQUIRED_SCOPE = "openid offline_access Mail.Send Mail.ReadWrite Mail.Send.Shared Mail.ReadWrite.Shared Group.ReadWrite.All GroupMember.Read.All User.Read User.Read.All";

    public static final String FILE_ALREADY_EXIST = "A file with this name already exists, Please choose a different file name";

    public static final String SCOPE_SEPARATOR = " ";

    public static final String ODATA_CONTEXT = "@odata.context";

    public static final String USER_EMAIL = "User Email";

    public static final String AUTHORITY = "https://login.microsoftonline.com/";

    public static final String USER_ID = "userId";

    public static final String AUTH_CONTEXT_ID = "authContextId";

    public static final String REFRESH_TOKEN_IS_NO_LONGER_VALID_PLEASE_REAUTHORIZE_YOURSELF = "Refresh Token is no longer valid. Please reauthorize yourself";

    public static final String FAILED_TO_GET_ACCOUNT = "Failed to get account";

    public static final String REST_SHAREPOINT_CALLBACK = "/rest/sharepoint/callback";

    public static final String GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_RE_AUTHENTICATION = "Got error for Authentication. So sending for re authentication.";

    public static final String GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_AUTHENTICATION = "Got error for Authentication. So sending for authentication";

    public static final String POST_METHOD = "POST";

    public static final String GET_REQUEST = "GET";

    public static final String PATCH_METHOD = "PATCH";

    public static final String APPLICATION_JSON = "application/json";

    public static final String MESSAGE = "Message";

    public static final String FILE = "File";

    public static final String FILES = "Files";

    public static final String IS_FILE_EXIST = "isFileExist";

    public static final String IS_SOURCE_FILE_EXIST = "isSourceFileExist";

    public static final String IS_TARGET_FILE_EXIST = "isTargetFileExist";

    public static final String IS_FOLDER_EXIST = "isFolderExist";

    public static final String PARENT_REFERENCE = "parentReference";

    public static final String ID = "id";

    public static final String FILE_HAS_BEEN_COPIED = "File has been copied successfully";

    public static final String PROVIDE_VALID_SOURCE_PATH = "Provide Valid source Path";

    public static final String PROVIDE_VALID_TARGET_PATH = "Provide Valid target Path";

    public static final String ITEM_NOT_FOUND = "itemNotFound";

    public static final String FILE_NOT_FOUND = "File Not Found";

    public static final String FILE_FOUND = "File found successfully";

    public static final String ERROR_DURING_SERVICE = "Error occurred from microsoft graph service, please try again later";

    public static final String FILE_UPLOADED_SUCCESSFULLY = "File Uploaded Successfully";

    public static final String ERROR_MESSAGE = "ERROR MESSAGE";

    public static final String FAILED_TO_UPLOAD_FILE = "Failed to upload file. File is empty.";

    public static final String FAILED_TO_UPLOAD = "Failed to upload file. Provide valid file to upload.";

    public static final String FAILED_TO_DOWNLOAD_FILE = "Failed to download file. Provide valid file to download.";

    public static final String ERROR_IN_UPLOAD_FILE = "Error uploading files. Please try again later.";

    public static final String FAILED_TO_RENAME_FILE = "Failed to rename file. Please try again later.";

    public static final String ERROR_OCCURRED_DURING_FETCHING_DETAILS = "Error occurred during fetching Details: {}";

    public static final String FILE_RENAMED_SUCCESSFULLY = "File Renamed Successfully";

    public static final String FAILED_TO_READ_TEXTFILE = "Failed to read text file. Please try again later.";

    public static final String FAILED_TO_UPLOAD_TEXTFILE = "Failed to upload text file. Please try again later.";

    public static final String CONTENT = "Content";

    public static final String DRIVE_ITEM = "DriveItem";

    public static final String SOURCE_DRIVE_ITEM = "SourceDriveItem";

    public static final String TARGET_DRIVE_ITEM = "TargetDriveItem";

    public static final String FILE_READ_SUCCESS = "File read successfully";

    public static final String SUCCESS = "Success";

    public static final String READ = "Read";

    public static final String WRITE = "Write";

    public static final String GROUP_DELETED_SUCCESSFULLY = " Group deleted successfully";

    public static final String TO_DELETE_GROUP_PLEASE_TRY_AGAIN_LATER = "Failed to delete group. Please try again later.";

    public static final String ERROR_LIST_CREATION = "Error creating list. Please try again later.";

    public static final String FAILED_LIST_CREATION = "Failed to create list. Please try again later.";

    public static final String GROUP_NAME_EXIST = "This group name is already in use. Please choose another unique group name";

    public static final String FAILED_TO_MODIFY_LIST = "Failed to modify list. Please try again later.";

    public static final String FAILED_TO_UPDATE_GROUP_PLEASE_TRY_AGAIN_LATER = "Failed to update group. Please try again later.";

    public static final String GROUP_UPDATED_SUCCESSFULLY = "Group updated successfully";

    public static final String SITE_NAME_CANNOT_BE_NULL = "Site name cannot be null or empty";

    public static final String LIST_NAME_CANNOT_BE_NULL = "List name cannot be null or empty";

    public static final String UPDATED_LIST_NAME_CANNOT_BE_NULL = "Updated List name cannot be null or empty";

    public static final String NO_CHANGE = "No Change";

    public static final String MODIFIED_BY = "Modified By";

    public static final String LIST_DELETED_SUCCESSFULLY = " List deleted successfully";

    public static final String UPDATED_SITE_NAME_CANNOT_BE_NULL = "Updated Site name cannot be null or empty";

    public static final String PERMISSIONS = "Permissions";

    public static final String PERMISSION = "Permission";

    public static final String OWN = "Own";

    public static final String QUERY_PERMISSIONS = "permissions";

    public static final String FILE_SIZE_CANNOT_BE_NULL = "File Size can't be null or 0";

    public static final String PROVIDE_VALID_SIZE_RANGE = "Provide valid size range";

    public static final String NO_FILE_FOUND_IN_SIZE_RANGE = "No File found within size range";

    public static final String DATE_RANGE_CANT_BE_EMPTY = "Modified date range can't be null OR empty";

    public static final String NO_FILE_FOUND_IN_DATE_RANGE = "No File found within date range";

    public static final String INVALID_FOLDER_PATH = "Please provide a valid path, the folder was not found";

    public static final String SOURCE_OR_TARGET_PATHS_CANT_BE_NULL = "Source OR Target paths cannot be null or empty";

    public static final String LIST_CREATED = "created";

    public static final String CREATED = "Created";

    public static final String UPDATED = "Updated";

    public static final String CHANGETYPE = "updated";

    public static final String SITE_RESOURCE = "/sites/";

    public static final String LIST_RESOURCE = "/lists/";

    public static final String TO_DOWNLOAD_FILE_PLEASE_CLICK_ON_URL = "File downloaded successfully. Please click on the following URL to access the file";

    public static final String DRIVE_ID = "DriveId";

    public static final String DOWNLOAD_LINK = "Link";

    public static final String SOURCE_RELATIVE_PATH = "Source Relative Path";

    public static final String TARGET_RELATIVE_PATH = "Target Relative Path";

    public static final String SOURCE_ROOT_PATH = "Source Root Path";

    public static final String TARGET_ROOT_PATH = "Target Root Path";

    public static final String KRISTA_FILE = "Krista File";

    public static final String FAILED_GET_FILE_FROM_INPUT_STREAM_INPUT_STREAM_EMPTY = "Failed get file from input stream. Input stream is empty.";

    public static final String FAILED_GET_FILE_FROM_INPUT_STREAM_FILE_NAME_EMPTY = "Failed get file from input stream. File name is empty.";

    public static final String FAILED_TO_GET_DRIVE_ITEM_FOLDER_NAME_IS_EMPTY = "Failed to get drive item. Folder name is empty.";

    public static final String WHITE_SPACE = "%20";

    public static final String SHARED_DOCUMENTS = "Shared Documents";

    public static final String DOCUMENTS = "Documents";

    public static final String USER = "User";

    public static final String SHARED_DOCUMENT_PATH = "/Shared Documents/";

    public static final String DOCUMENT_PATH = "/Documents/";

    public static final String REST_SHAREPOINT_MAIL_NOTIFICATION = "/rest/sharepoint/sharePointNotification";

    public static final String YYYY_MM_DD_T_HH_MM_SS_SSSSSSS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'";

    public static final String REST_SHAREPOINT_LIFECYCLE_NOTIFICATION = "/rest/sharepoint/lifecycleNotification";

    public static final String VALIDATION_TOKEN = "validationToken";

    public static final String VALUE = "value";

    public static final String SUBSCRIPTION_ID = "subscriptionId";

    public static final String LIFECYCLE_EVENT = "lifecycleEvent";

    public static final String REAUTHORIZATION_REQUIRED = "reauthorizationRequired";

    public static final String FILE_RECEIVED = "fileReceived";

    public static final String EVENT = "Event";

    public static final String SHAREPOINT_FILE_ID = "sharePointFileId";

    public static final String SWITCH = "Switch";

    public static final String FAILED_TO_GET_BY_MAILID = "Failed to get file from this mail Id, Please try again later.";

    public static final String MAILID_CANNOT_BE_EMPTY = "E-Mail Id Cannot be null or empty";

    public static final String INVALID_EMAIL = "Please provide a valid email";

    public static final String CREATED_BY = "Created By";

    public static final String CLIENT_ID = "clientId";

    public static final String CLIENT_SECRET = "clientSecret";

    public static final String TENANT_ID = "tenantId";

    public static final String EMAIL = "email";

    public static final int OK = 200;

    public static final int CREATED_RES = 201;

    public static final int ACCEPTED = 202;

    public static final int NO_CONTENT = 204;

    public static final int CONFLICT = 409;

    public static final int UNAUTHORIZED = 401;

    public static final int BAD_REQUEST = 400;

    public static final int INTERNAL_SERVER_ERROR = 500;

    public static final String FOLDER_NOT_FOUND = "Folder Not Found";

    public static final String INVALID_INPUT = "Invalid Input";

    public static final String FOLDERS_FETCHED_SUCCESSFULLY = "Folders fetched successfully";

    public static final String FILE_FETCHED_SUCCESSFULLY = "Files and Folders fetched successfully";

    public static final String EMPTY = "EMPTY";

    public static final String GROUP_CREATED = "Group created successfully";

    public static final String PLEASE_PROVIDE_VALID_SOURCE_PATH = "Please provide valid source path";

    public static final String FILE_SIZE_IS_GREATER_THAN_50_MB_PLEASE_DOWNLOAD_FILE_BY_URL = "File size is greater than 50MB, Please Download file by URL";

    public static final String PLEASE_PROVIDE_VALID_FILE_SOURCE_PATH = "Please, Provide Valid File Source Path";

    public static final String ACCESS_DENIED = "accessDenied";

    public static final String FILE_DELETED_SUCCESSFULLY = "File deleted successfully";

    public static final String PROVIDE_VALID_PATH_FILE_NOT_FOUND = "Please provide valid path, file was not found";

    public static final String RESULT = "Result";

    public static final String UPDATE = "Update";

    public static final String PATH = "Path";

    public static final String NUMBER = "Number";

    public static final String DATE = "Date";

    public static final String FILE_HAS_BEEN_MOVED = "File has been moved successfully";

    public static final String FILE_IS_TO_SHARE_CURRENTLY_RESTRICTED_FOR_ASSISTANCE_CONTACT_YOUR_ADMINISTRATOR_OR_SUPPORT = "Access to share this file or folder is currently restricted. For assistance, contact your administrator or support.";

    public static final String ERROR_OCCURRED_DURING_MOVING_FILE = "Error occurred during moving file";

    public static final String FOLDERS = "Folders";

    public static final String SIZE = "Size";

    public static final String OLDEST_FILE = "Oldest File";

    public static final String OLDEST_FILE_CREATE_DATE = "Oldest File Create Date";

    public static final String NEWEST_FILE = "Newest File";

    public static final String NEWEST_FILE_DATE = "Newest File Date";

    public static final String LAST_MODIFIED_FILE_ID = "Last Modified File Id";

    public static final String LAST_MODIFIED_FILE_NAME = "Last Modified File Name";

    public static final String LAST_MODIFIED_FILE_CREATE_DATE = "Last Modified File Create Date";

    public static final String PLEASE_PROVIDE_VALID_PATH_FOLDER_WAS_NOT_FOUND = "Please provide valid path, folder was not found";

    public static final String PLEASE_PROVIDE_A_VALID_PATH_THE_FILE_FOLDER_WAS_NOT_FOUND = "Please provide a valid path, the file/folder was not found";

    public static final String FILE_DOWNLOADED_SUCCESSFULLY = "File downloaded successfully";

    public static final String groupConflictMessage = "Another object with the same value for property mailNickname already exists.";

    public static final String groupMessageForConflict = "There is already another group with the same email nickname. Please choose a different nickname.";

    public static final String InvalidMailNickName = "Invalid value specified for property 'mailNickname' of resource 'Group'.";

    public static final String ValidMailNickName = "Please provide valid mail nick name ";

    public static final String GROUP_URL = "https://graph.microsoft.com/v1.0/groups";

    public static final String error = "error";

    public static final String id = "id";

    public static final String createdDateTime = "createdDateTime";

    public static final String displayName = "displayName";

    public static final String mail = "mail";

    public static final String message = "message";

    public static final String mailNickname = "mailNickname";

    public static final String Request_BadRequest = "Request_BadRequest";

    public static final String mailEnabled = "mailEnabled";

    public static final String securityEnabled = "securityEnabled";

    public static final String Unified = "Unified";

    public static final String groupTypes = "groupTypes";

    public static final String ownersDataBind = "owners@odata.bind";

    public static final String groupBaseUrl = "https://graph.microsoft.com/v1.0/users/";

    public static final String ownerMailUrl = "https://graph.microsoft.com/v1.0/me";

    public static final String DOCUMENT_LIBRARY = "documentLibrary";


    private Constants() {
    }
}

