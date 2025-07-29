package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util;

/**
 * Constants for sub catalog request names and common fields used in Office365Excel extension.
 */
public class SubCatalogConstants {


    private SubCatalogConstants() {
    }

    // Validation constants
    public static final String VALIDATION_RESULTS = "validationResults";
    public static final String REENTER = "Reenter";
    public static final String STATE_ID = "stateId";

    // Get User Details by ID or Email validation constants
    public static final String CONFIRM_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL = "confirmReenterGetUserDetailsByIDOrEmail";
    public static final String HANDLE_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL = "handleReenterGetUserDetailsByIDOrEmail";

    // Delete Account validation constants
    public static final String CONFIRM_DELETE_ACCOUNT_VALIDATION = "confirmDeleteAccountValidation";
    public static final String HANDLE_DELETE_ACCOUNT_VALIDATION = "handleDeleteAccountValidation";

    // Activate or Deactivate Account validation constants
    public static final String CONFIRM_ACTIVATE_DEACTIVATE_ACCOUNT_VALIDATION = "confirmActivateDeactivateAccountValidation";
    public static final String HANDLE_ACTIVATE_DEACTIVATE_ACCOUNT_VALIDATION = "handleActivateDeactivateAccountValidation";

    // Reset Password validation constants
    public static final String CONFIRM_RESET_PASSWORD_VALIDATION = "confirmResetPasswordValidation";
    public static final String HANDLE_RESET_PASSWORD_VALIDATION = "handleResetPasswordValidation";

    // Remove Account From Group validation constants
    public static final String CONFIRM_REMOVE_ACCOUNT_FROM_GROUP_VALIDATION = "confirmRemoveAccountFromGroupValidation";
    public static final String HANDLE_REMOVE_ACCOUNT_FROM_GROUP_VALIDATION = "handleRemoveAccountFromGroupValidation";

    // Add Account To Group validation constants
    public static final String CONFIRM_ADD_ACCOUNT_TO_GROUP_VALIDATION = "confirmAddAccountToGroupValidation";
    public static final String HANDLE_ADD_ACCOUNT_TO_GROUP_VALIDATION = "handleAddAccountToGroupValidation";

    // Update Account validation constants
    public static final String CONFIRM_UPDATE_ACCOUNT_VALIDATION = "confirmUpdateAccountValidation";
    public static final String HANDLE_UPDATE_ACCOUNT_VALIDATION = "handleUpdateAccountValidation";

    // Create Account validation constants
    public static final String CONFIRM_CREATE_ACCOUNT_VALIDATION = "confirmCreateAccountValidation";
    public static final String HANDLE_CREATE_ACCOUNT_VALIDATION = "handleCreateAccountValidation";

}
