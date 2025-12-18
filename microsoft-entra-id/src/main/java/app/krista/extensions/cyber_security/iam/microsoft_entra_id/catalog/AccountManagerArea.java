package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog;


import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.GetUserDetailsByIdOrEmailHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.DeleteAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.ActivateDeactivateAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.ResetPasswordHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.RemoveAccountFromGroupHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.AddAccountToGroupHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.UpdateAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers.CreateAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.TelemetryHelper;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.service.Accounts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Domain(id = "catEntryDomain_5fd12694-66b1-4cd2-8141-c41f02716c3a",
        name = "IAM",
        ecosystemId = "catEntryEcosystem_5516afe8-3809-4918-b911-d89597fc96e0",
        ecosystemName = "Cyber Security",
        ecosystemVersion = "3cf343a9-3df2-4036-9e0f-7b84930b8ef7")
public
class AccountManagerArea {

    public static final String ACCOUNT_STATUS = "Account Status";
    public static final String MESSAGE = "Message";
    private final Accounts account;
    private final GetUserDetailsByIdOrEmailHelper getUserDetailsHelper;
    private final DeleteAccountHelper deleteAccountHelper;
    private final ActivateDeactivateAccountHelper activateDeactivateAccountHelper;
    private final ResetPasswordHelper resetPasswordHelper;
    private final RemoveAccountFromGroupHelper removeAccountFromGroupHelper;
    private final AddAccountToGroupHelper addAccountToGroupHelper;
    private final UpdateAccountHelper updateAccountHelper;
    private final CreateAccountHelper createAccountHelper;
    private final TelemetryHelper telemetryHelper;
    private final Invoker invoker;
    private final AccountImpl accountService;

    private static final Logger logger = LoggerFactory.getLogger(AccountManagerArea.class);

    @Inject
    public AccountManagerArea(AccountImpl account, GetUserDetailsByIdOrEmailHelper getUserDetailsHelper,
                              DeleteAccountHelper deleteAccountHelper, ActivateDeactivateAccountHelper activateDeactivateAccountHelper,
                              ResetPasswordHelper resetPasswordHelper, RemoveAccountFromGroupHelper removeAccountFromGroupHelper,
                              AddAccountToGroupHelper addAccountToGroupHelper, UpdateAccountHelper updateAccountHelper,
                              CreateAccountHelper createAccountHelper, TelemetryHelper telemetryHelper, Invoker invoker, AccountImpl accountService) {
        this.account = account;
        this.getUserDetailsHelper = getUserDetailsHelper;
        this.deleteAccountHelper = deleteAccountHelper;
        this.activateDeactivateAccountHelper = activateDeactivateAccountHelper;
        this.resetPasswordHelper = resetPasswordHelper;
        this.removeAccountFromGroupHelper = removeAccountFromGroupHelper;
        this.addAccountToGroupHelper = addAccountToGroupHelper;
        this.updateAccountHelper = updateAccountHelper;
        this.createAccountHelper = createAccountHelper;
        this.telemetryHelper = telemetryHelper;
        this.invoker = invoker;
        this.accountService = accountService;
    }


    @CatalogRequest(
            id = "localDomainRequest_e42ee430-a2ea-47ff-afcf-00d8ab303e5b",
            name = "Activate or Deactivate an Account",
            description = "Required User ID or User Mail to Activate or Deactivate an Account",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Account Status", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Returns true if the account operation was successful, false if it failed")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Returns a descriptive message about the operation result (e.g., 'Account Activated Successfully')")}, options = {})
    public ExtensionResponse activateOrDeactivateAnAccount(
            @Field.Boolean(name = "Deactivate", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Set to true to deactivate the account, false to activate it")}, options = {}) Boolean deactivate,
            @Field.Text(name = "User Mail or ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Enter the user's email address or unique user ID from Microsoft Entra ID")}, options = {}) String userMailOrID,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("activateOrDeactivateAnAccount");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using ActivateDeactivateAccountErrorHandler");
                response = activateDeactivateAccountHelper.activateOrDeactivateAccountWithValidation(userMailOrID, deactivate);
            } else {
                boolean accountStatus = accountService.activateOrDeactivateAccount(userMailOrID, deactivate);
                String statusMessage = deactivate ? "Deactivated" : "Activated";
                Map<String, Object> result = Map.of(
                        ACCOUNT_STATUS, accountStatus,
                        MESSAGE, "Account " + statusMessage + " Successfully");
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, result, null, null, null);
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Activate or Deactivate an Account : {}", cause.getMessage(), cause);
            Map<String, Object> errorResult = Map.of(
                    ACCOUNT_STATUS, false,
                    MESSAGE, cause.getMessage());
            return new ExtensionResponseBuilder().success(errorResult).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_9945dea1-a854-4871-ad09-85f335bea91a",
            name = "Get List of Groups",
            description = "Get List of Groups",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Group List", type = "[ { Group Mail Id: Text, Group Id: Text } ]", required = false)
    public ExtensionResponse getListOfGroups() {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getListOfGroups");
        logger.debug("Get List of Groups Initiated");
        try {
            List<Map<String, String>> groups = account.getAllGroups();
            operation.recordSuccess(groups.size());
            return new ExtensionResponseBuilder().success(Map.of("Group List", groups)).build();
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Get List of Groups : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Group List", Collections.emptyList())).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_30716250-b09b-4706-b565-61c113715a31",
            name = "Add an Account to Group",
            description = "Requires User Id or User Mail and Group Object Id",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Account Info", type = "{ User Mail Id: Text, User Name: Text, Group List: [ { Group Mail Id: Text, Group Id: Text } ], User Id: Text }", required = false)
    public ExtensionResponse addAnAccountToGroup(
            @Field.Text(name = "User Mail or Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Enter the user's email address or unique user ID to add to the group")}, options = {}) String userMailOrId,
            @Field.Text(name = "Group Object Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Enter the Microsoft Entra ID group's unique object ID (GUID format)")}, options = {}) String groupObjectId,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("addAnAccountToGroup");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using AddAccountToGroupErrorHandler");
                response = addAccountToGroupHelper.addAccountToGroupWithValidation(userMailOrId, groupObjectId);
            } else {
                Map<String, Object> result = accountService.addAccountToGroup(userMailOrId, groupObjectId);
                return new ExtensionResponseBuilder().success(Map.of("Account Info", result)).build();
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Add an Account to Group : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Account Info", Collections.emptyMap())).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_d29d95b8-315f-438b-988d-6e70722fe889",
            name = "Remove an Account From Group",
            description = "Required User Mail or ID and Group Object Id",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Removed", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Returns true if the user was successfully removed from the group, false if the operation failed")}, options = {})
    public ExtensionResponse removeGroupFromAccount(
            @Field.Text(name = "User Mail or Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Enter the user's email address or unique user ID to remove from the group")}, options = {}) String userMailOrId,
            @Field.Text(name = "Group Object Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "tooltip", value = "Enter the Microsoft Entra ID group's unique object ID (GUID format) to remove the user from")}, options = {}) String groupObjectId,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("removeGroupFromAccount");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using RemoveAccountFromGroupErrorHandler");
                response = removeAccountFromGroupHelper.removeAccountFromGroupWithValidation(userMailOrId, groupObjectId);
            } else {
                Boolean removed = accountService.removeAccountFromGroup(userMailOrId, groupObjectId);
                logger.info("Successfully removed account from group for userMailOrId: {}, groupObjectId: {}", userMailOrId, groupObjectId);
                return new ExtensionResponseBuilder()
                        .success(Map.of("Removed", removed))
                        .build();

            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Remove an Account from Group : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Removed", false)).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_1079ad63-576a-4aca-9897-230c2ed51f87",
            name = "Create an Account",
            description = "Create User in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "User Id", type = "Text", required = false, attributes = {@Attribute(name = "tooltip", value = "The unique Microsoft Entra ID object ID assigned to the newly created user account")}, options = {})
    @Field(name = "Default Password", type = "Text", required = false, attributes = {@Attribute(name = "tooltip", value = "The temporary password generated for the new user account (user must change on first login)")}, options = {})
    public ExtensionResponse createAccount(
            @Field(name = "Name", type = "Text", required = true, attributes = {}, options = {}) String name,
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email,
            @Field(name = "Mobile Number", type = "Text", required = true, attributes = {}, options = {}) String mobileNumber,
            @Field.Desc(name = "Extra Fields", type = "[ { key: Text, value: Text } ]", required = false) List<Map<String, Object>> extraFields,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("createAccount");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using CreateAccountErrorHandler");
                response = createAccountHelper.createAccountWithValidation(name, email, mobileNumber, extraFields);
            } else {
                Map<String, Object> result = accountService.createUser(name, email, mobileNumber, extraFields);
                return new ExtensionResponseBuilder().success(result).build();
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Create an Account : {}", cause.getMessage(), cause);
            Map<String, Object> errorResult = Map.of("User Id", "", "Default Password", "");
            return new ExtensionResponseBuilder().success(errorResult).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_05582946-848b-466c-bc41-75f14413dedf",
            name = "Update an Account",
            description = "Update an Account in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Updated Entity", type = "Entity(Account)", required = false)
    public ExtensionResponse updateAccount(
            @Field(name = "Name", type = "Text", required = false, attributes = {}, options = {}) String name,
            @Field(name = "Mobile Number", type = "Text", required = false, attributes = {}, options = {}) String mobileNumber,
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email,
            @Field.Desc(name = "Extra Field", type = "[ { key: Text, value: Text } ]", required = false) List<Map<String, Object>> extraField,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("updateAccount");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using UpdateAccountErrorHandler");
                response = updateAccountHelper.updateAccountWithValidation(name, mobileNumber, email);
            } else {
                Account updatedAccount = accountService.updateUser(
                        name != null ? name : "",
                        mobileNumber != null ? mobileNumber : "",
                        email,
                        null);
                return new ExtensionResponseBuilder()
                        .success(Map.of("Updated Entity", updatedAccount))
                        .build();
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Update an Account : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Updated Entity", new Account())).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_a8cf8efe-56f8-40a7-be91-da8aa945195f",
            name = "Delete an Account",
            description = "Delete an Account in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Deleted", type = "Switch", required = false, attributes = {@Attribute(name = "tooltip", value = "Returns true if the user account was successfully deleted from Microsoft Entra ID, false if the operation failed")}, options = {})
    public ExtensionResponse deleteAccount(
            @Field(name = "Email", type = "Text", required = true, attributes = {@Attribute(name = "tooltip", value = "Enter the email address of the user account to permanently delete from Microsoft Entra ID")}, options = {}) String email,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("deleteAccount");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using DeleteAccountErrorHandler");
                response = deleteAccountHelper.deleteAccountWithValidation(email);
            }
            else{
                Boolean deleted = accountService.deleteUser(email);
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("Deleted", deleted);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseData, null, null, null);
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Delete an Account : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Deleted", false)).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_720edeff-9823-4597-b208-921835e1d960",
            name = "Get User Details by ID or Email",
            description = "Get User Details by ID or Email",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Result", type = "Entity(Account)", required = false)
    public ExtensionResponse getUserDetailsByIDOrEmail(
            @Field(name = "Id", type = "Text", required = true, attributes = {@Attribute(name = "tooltip", value = "Enter the user's email address or unique user ID to retrieve their account details")}, options = {}) String id,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getUserDetailsByIDOrEmail");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using GetUserDetailsByIdOrEmailErrorHandler");
                response = getUserDetailsHelper.getUserDetailsByIdOrEmailWithValidation(id);
            } else {
                logger.debug("Error handling is disabled, using direct logic");
                Account result = accountService.getUser(id);
                if (result == null) {
                    logger.warn("No user found with ID/Email: {}", id);
                    return new ExtensionResponseBuilder()
                            .success(Map.of("Result", "No user found with the specified ID or email"))
                            .build();
                }
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("Result", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseData, null, null, null);
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Get User Details by ID or Email : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Result", new Account())).build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_57ac8a99-063f-4391-bd6b-25b52f94137e",
            name = "Reset Password",
            description = "Reset Account Password",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Password", type = "Text", required = false, attributes = {@Attribute(name = "tooltip", value = "Returns the new temporary password generated for the user (user must change this password on next login)")}, options = {})
    public ExtensionResponse resetPassword(
            @Field(name = "Id", type = "Text", required = true, attributes = {@Attribute(name = "tooltip", value = "Enter the user's email address or unique user ID to reset their password and generate a temporary password")}, options = {}) String id,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean allowRetry) {
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("resetPassword");

        try {
            ExtensionResponse response;
            boolean retryEnabled = allowRetry != null && allowRetry;
            if (retryEnabled) {
                logger.debug("Error handling is enabled, using ResetPasswordErrorHandler");
                response = resetPasswordHelper.resetPasswordWithValidation(id);
            } else {
                String password = accountService.resetPassword(id);
                response = new ExtensionResponseBuilder().success(Map.of("Password", password)).build();
            }
            return response;
        } catch (MustAuthorizeException authorizeException) {
            operation.recordFailure(authorizeException);
            throw authorizeException;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error Occurred during Reset Password : {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder().success(Map.of("Password", "")).build();
        }
    }
    public boolean isErrorHandlingEnabled() {
        final Object enableErrorHandling = invoker.getAttributes().get(Constants.ENABLE_ERROR_HANDLING);
        boolean errorHandlingFlag = enableErrorHandling != null && (Boolean) enableErrorHandling;
        logger.info("Enable Error Handling Flag:{}", errorHandlingFlag);
        return errorHandlingFlag;
    }

}
