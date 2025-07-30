package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog;

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.GetUserDetailsByIdOrEmailHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.DeleteAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.ActivateDeactivateAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.ResetPasswordHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.RemoveAccountFromGroupHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.AddAccountToGroupHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.UpdateAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers.CreateAccountHelper;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Service
public class AccountManagerAreaSubcatalogRequests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountManagerAreaSubcatalogRequests.class);

    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ValidationOrchestrator validationOrchestrator;
    private final GetUserDetailsByIdOrEmailHelper getUserDetailsHelper;
    private final DeleteAccountHelper deleteAccountHelper;
    private final ActivateDeactivateAccountHelper activateDeactivateAccountHelper;
    private final ResetPasswordHelper resetPasswordHelper;
    private final RemoveAccountFromGroupHelper removeAccountFromGroupHelper;
    private final AddAccountToGroupHelper addAccountToGroupHelper;
    private final UpdateAccountHelper updateAccountHelper;
    private final CreateAccountHelper createAccountHelper;

    @Inject
    public AccountManagerAreaSubcatalogRequests(ExtensionResponseGenerator responseGenerator,
                                                ErrorHandlingStateManager internalStateManager,
                                                ValidationOrchestrator validationOrchestrator,
                                                GetUserDetailsByIdOrEmailHelper getUserDetailsHelper,
                                                DeleteAccountHelper deleteAccountHelper,
                                                ActivateDeactivateAccountHelper activateDeactivateAccountHelper,
                                                ResetPasswordHelper resetPasswordHelper,
                                                RemoveAccountFromGroupHelper removeAccountFromGroupHelper,
                                                AddAccountToGroupHelper addAccountToGroupHelper,
                                                UpdateAccountHelper updateAccountHelper,
                                                CreateAccountHelper createAccountHelper) {
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.validationOrchestrator = validationOrchestrator;
        this.getUserDetailsHelper = getUserDetailsHelper;
        this.deleteAccountHelper = deleteAccountHelper;
        this.activateDeactivateAccountHelper = activateDeactivateAccountHelper;
        this.resetPasswordHelper = resetPasswordHelper;
        this.removeAccountFromGroupHelper = removeAccountFromGroupHelper;
        this.addAccountToGroupHelper = addAccountToGroupHelper;
        this.updateAccountHelper = updateAccountHelper;
        this.createAccountHelper = createAccountHelper;

        LOGGER.info("AccountManagerArea Sub catalog Requests constructor completed");
    }


    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
            description = "Checks if user wants to re-enter input and validates it",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmReenterGetUserDetailsByIDOrEmail(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Id: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmReenterGetUserDetailsByIDOrEmail() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmReenterGetUserDetailsByIDOrEmail() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
            description = "Handle reenter Get User Details by ID or Email",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Result", type = "Entity", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterGetUserDetailsByIDOrEmail(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Id: Text }") Map<String, Object> map) {

        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Full input map contents: {}", map);

        String userId = (String) map.get(MicrosoftEntraIDResources.ID);
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Extracted parameters: USER_ID={}", userId);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.ID_GET_USER_DETAILS, userId != null ? userId : "")
        );
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Validation results: {} errors", validationResults.size());
        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Validation failed again after re-entry for USER_ID: {}", userId);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided user ID is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Validation passed, retrieving user details");
        ExtensionResponse response = getUserDetailsHelper.getUserDetailsByIdOrEmailWithValidation(userId);
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Completed successfully");
        return response;

    }

    @NotNull
    @SuppressWarnings("unchecked")
    private List<ValidationOrchestrator.ValidationResult> getValidationResults(String stateId) {
        LOGGER.debug("getValidationResults() - Starting with stateId: {}", stateId);

        List<ValidationOrchestrator.ValidationResult> results = new java.util.ArrayList<>();

        try {
            // Try to get the raw stored data as a string first
            String storedData = internalStateManager.getRaw(stateId);
            LOGGER.debug("getValidationResults() - Retrieved raw data: {}", storedData != null ? "not null" : "null");

            if (storedData != null) {
                // Try to parse as direct ValidationResult list first (from GetChannelIDHelper)
                try {
                    com.google.gson.reflect.TypeToken<List<ValidationOrchestrator.ValidationResult>> typeToken =
                            new com.google.gson.reflect.TypeToken<List<ValidationOrchestrator.ValidationResult>>() {
                            };
                    results = Constants.GSON.fromJson(storedData, typeToken.getType());
                    LOGGER.debug("getValidationResults() - Successfully parsed as direct ValidationResult list, size: {}", results.size());
                    return results;
                } catch (Exception directParseError) {
                    LOGGER.debug("Direct parse failed, trying wrapped format: {}", directParseError.getMessage());

                    // Try to parse as wrapped format (from GetChatIDHelper with VALIDATION_RESULTS key)
                    try {
                        com.google.gson.reflect.TypeToken<Map<String, List<ValidationOrchestrator.ValidationResult>>> wrappedTypeToken =
                                new com.google.gson.reflect.TypeToken<Map<String, List<ValidationOrchestrator.ValidationResult>>>() {
                                };
                        Map<String, List<ValidationOrchestrator.ValidationResult>> wrappedData = Constants.GSON.fromJson(storedData, wrappedTypeToken.getType());

                        if (wrappedData != null && wrappedData.containsKey(SubCatalogConstants.VALIDATION_RESULTS)) {
                            results = wrappedData.get(SubCatalogConstants.VALIDATION_RESULTS);
                            LOGGER.debug("getValidationResults() - Successfully parsed as wrapped format, size: {}", results.size());
                            return results;
                        }
                    } catch (Exception wrappedParseError) {
                        LOGGER.debug("Wrapped parse also failed: {}", wrappedParseError.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving validation results for stateId: {}, error: {}", stateId, e.getMessage());
        }

        LOGGER.debug("getValidationResults() retrieved {} validation results", results.size());
        return results;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_DELETE_ACCOUNT_VALIDATION,
            description = "Checks if user wants to re-enter email for delete account and validates it",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmDeleteAccountValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Email: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmDeleteAccountValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmDeleteAccountValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_DELETE_ACCOUNT_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_DELETE_ACCOUNT_VALIDATION,
            description = "Handle reenter Delete Account validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field(name = "Deleted", type = "Switch", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleDeleteAccountValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Email: Text }") Map<String, Object> map) {

        LOGGER.debug("handleDeleteAccountValidation() - Full input map contents: {}", map);

        String email = (String) map.get(MicrosoftEntraIDResources.EMAIL);
        LOGGER.debug("handleDeleteAccountValidation() - Extracted parameters: EMAIL={}", email);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.EMAIL, email != null ? email : "")
        );
        LOGGER.debug("handleDeleteAccountValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleDeleteAccountValidation() - Validation failed again after re-entry for EMAIL: {}", email);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided email is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        LOGGER.debug("handleDeleteAccountValidation() - Validation passed, deleting account");
        ExtensionResponse response = deleteAccountHelper.deleteAccountWithValidation(email);
        LOGGER.debug("handleDeleteAccountValidation() - Completed successfully");
        return response;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_ACTIVATE_DEACTIVATE_ACCOUNT_VALIDATION,
            description = "Checks if user wants to re-enter User Mail or ID for activate/deactivate account and validates it",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmActivateDeactivateAccountValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, User Mail or ID: Text, Deactivate: Boolean }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmActivateDeactivateAccountValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmActivateDeactivateAccountValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_ACTIVATE_DEACTIVATE_ACCOUNT_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_ACTIVATE_DEACTIVATE_ACCOUNT_VALIDATION,
            description = "Handle reenter Activate or Deactivate Account validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Boolean(name = "Account Status", required = false)
    @Field.Text(name = "Message", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleActivateDeactivateAccountValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, User Mail or ID: Text, Deactivate: Boolean }") Map<String, Object> map) {

        LOGGER.debug("handleActivateDeactivateAccountValidation() - Full input map contents: {}", map);

        String userMailOrID = (String) map.get(MicrosoftEntraIDResources.USER_MAIL_OR_ID);
        Boolean deactivate = (Boolean) map.get("Deactivate");
        LOGGER.debug("handleActivateDeactivateAccountValidation() - Extracted parameters: USER_MAIL_OR_ID={}, Deactivate={}", userMailOrID, deactivate);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.USER_MAIL_OR_ID, userMailOrID != null ? userMailOrID : "")
        );
        LOGGER.debug("handleActivateDeactivateAccountValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleActivateDeactivateAccountValidation() - Validation failed again after re-entry for USER_MAIL_OR_ID: {}", userMailOrID);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided User Mail or ID is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        LOGGER.debug("handleActivateDeactivateAccountValidation() - Validation passed, activating/deactivating account");
        ExtensionResponse response = activateDeactivateAccountHelper.activateOrDeactivateAccountWithValidation(userMailOrID, deactivate);
        LOGGER.debug("handleActivateDeactivateAccountValidation() - Completed successfully");
        return response;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_RESET_PASSWORD_VALIDATION,
            description = "Checks if user wants to re-enter ID for reset password and validates it",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmResetPasswordValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Id: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmResetPasswordValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmResetPasswordValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_RESET_PASSWORD_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_RESET_PASSWORD_VALIDATION,
            description = "Handle reenter Reset Password validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field(name = "Password", type = "Text", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleResetPasswordValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Id: Text }") Map<String, Object> map) {

        LOGGER.debug("handleResetPasswordValidation() - Full input map contents: {}", map);

        String id = (String) map.get(MicrosoftEntraIDResources.ID);
        LOGGER.debug("handleResetPasswordValidation() - Extracted parameters: ID={}", id);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.ID, id != null ? id : "")
        );
        LOGGER.debug("handleResetPasswordValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleResetPasswordValidation() - Validation failed again after re-entry for ID: {}", id);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided ID is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        LOGGER.debug("handleResetPasswordValidation() - Validation passed, resetting password");
        ExtensionResponse response = resetPasswordHelper.resetPasswordWithValidation(id);
        LOGGER.debug("handleResetPasswordValidation() - Completed successfully");
        return response;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REMOVE_ACCOUNT_FROM_GROUP_VALIDATION,
            description = "Checks if user wants to re-enter User Mail or Id and Group Object Id for remove account from group and validates them",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmRemoveAccountFromGroupValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, User Mail or Id: Text, Group Object Id : Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmRemoveAccountFromGroupValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmRemoveAccountFromGroupValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_REMOVE_ACCOUNT_FROM_GROUP_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_REMOVE_ACCOUNT_FROM_GROUP_VALIDATION,
            description = "Handle reenter Remove Account From Group validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Boolean(name = "Removed", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleRemoveAccountFromGroupValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, User Mail or Id: Text, Group Object Id : Text }") Map<String, Object> map) {

        LOGGER.debug("handleRemoveAccountFromGroupValidation() - Full input map contents: {}", map);

        String userMailOrId = (String) map.get(MicrosoftEntraIDResources.USER_MAIL_OR_ID_REMOVE_GROUP);
        String groupObjectId = (String) map.get(MicrosoftEntraIDResources.GROUP_OBJECT_ID);
        LOGGER.debug("handleRemoveAccountFromGroupValidation() - Extracted parameters: USER_MAIL_OR_ID={}, GROUP_OBJECT_ID={}", userMailOrId, groupObjectId);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.USER_MAIL_OR_ID_REMOVE_GROUP, userMailOrId != null ? userMailOrId : "",
                        Validator.ValidationResource.GROUP_OBJECT_ID, groupObjectId != null ? groupObjectId : ""
                )
        );
        LOGGER.debug("handleRemoveAccountFromGroupValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleRemoveAccountFromGroupValidation() - Validation failed again after re-entry for USER_MAIL_OR_ID: {}, GROUP_OBJECT_ID: {}", userMailOrId, groupObjectId);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided User Mail or Id and/or Group Object Id are still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        LOGGER.debug("handleRemoveAccountFromGroupValidation() - Validation passed, removing account from group");
        ExtensionResponse response = removeAccountFromGroupHelper.removeAccountFromGroupWithValidation(userMailOrId, groupObjectId);
        LOGGER.debug("handleRemoveAccountFromGroupValidation() - Completed successfully");
        return response;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_ADD_ACCOUNT_TO_GROUP_VALIDATION,
            description = "Checks if user wants to re-enter User Mail or Id and Group Object Id for add account to group and validates them",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmAddAccountToGroupValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, User Mail or Id : Text,Group Object Id: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmAddAccountToGroupValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmAddAccountToGroupValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_ADD_ACCOUNT_TO_GROUP_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_ADD_ACCOUNT_TO_GROUP_VALIDATION,
            description = "Handle reenter Add Account To Group validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Account Info", type = "{ User Mail Id: Text, User Name: Text, Group List: [ { Group Mail Id: Text, Group Id: Text } ], User Id: Text }", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleAddAccountToGroupValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, User Mail or Id: Text, Group Object Id: Text }") Map<String, Object> map) {

        LOGGER.debug("handleAddAccountToGroupValidation() - Full input map contents: {}", map);

        String userMailOrId = (String) map.get(MicrosoftEntraIDResources.USER_MAIL_OR_ID_ADD_GROUP);
        String groupObjectId = (String) map.get(MicrosoftEntraIDResources.GROUP_OBJECT_ID_ADD_GROUP);
        LOGGER.debug("handleAddAccountToGroupValidation() - Extracted parameters: USER_MAIL_OR_ID={}, GROUP_OBJECT_ID={}", userMailOrId, groupObjectId);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.USER_MAIL_OR_ID_ADD_GROUP, userMailOrId != null ? userMailOrId : "",
                        Validator.ValidationResource.GROUP_OBJECT_ID_ADD_GROUP, groupObjectId != null ? groupObjectId : ""
                )
        );
        LOGGER.debug("handleAddAccountToGroupValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleAddAccountToGroupValidation() - Validation failed again after re-entry for USER_MAIL_OR_ID: {}, GROUP_OBJECT_ID: {}", userMailOrId, groupObjectId);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided User Mail or Id and/or Group Object Id are still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        LOGGER.debug("handleAddAccountToGroupValidation() - Validation passed, adding account to group");
        ExtensionResponse response = addAccountToGroupHelper.addAccountToGroupWithValidation(userMailOrId, groupObjectId);
        LOGGER.debug("handleAddAccountToGroupValidation() - Completed successfully");
        return response;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_UPDATE_ACCOUNT_VALIDATION,
            description = "Checks if user wants to re-enter Name, Mobile Number, and Email for update account and validates them",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmUpdateAccountValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Name: Text, Mobile Number: Text, Email: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmUpdateAccountValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmUpdateAccountValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_UPDATE_ACCOUNT_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_UPDATE_ACCOUNT_VALIDATION,
            description = "Handle reenter Update Account validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Updated Entity", type = "Entity(Account)", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleUpdateAccountValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Name: Text,Mobile Number : Text, Email: Text }") Map<String, Object> map) {

        LOGGER.debug("handleUpdateAccountValidation() - Full input map contents: {}", map);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            LOGGER.debug("Key: '{}', Value: '{}', Value type: {}",
                    entry.getKey(), entry.getValue(),
                    entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
        }

        String name = (String) map.get(MicrosoftEntraIDResources.NAME_UPDATE_ACCOUNT);
        String mobileNumber = (String) map.get(MicrosoftEntraIDResources.MOBILE_NUMBER_UPDATE_ACCOUNT);
        String email = (String) map.get(MicrosoftEntraIDResources.EMAIL_UPDATE_ACCOUNT);
        LOGGER.debug("handleUpdateAccountValidation() - Extracted parameters: NAME={}, MOBILE_NUMBER={}, EMAIL={}", name, mobileNumber, email);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.NAME_UPDATE_ACCOUNT, name != null ? name : "",
                        Validator.ValidationResource.MOBILE_NUMBER_UPDATE_ACCOUNT, mobileNumber != null ? mobileNumber : "",
                        Validator.ValidationResource.EMAIL_UPDATE_ACCOUNT, email != null ? email : ""
                )
        );
        LOGGER.debug("handleUpdateAccountValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleUpdateAccountValidation() - Validation failed again after re-entry for NAME: {}, MOBILE_NUMBER: {}, EMAIL: {}", name, mobileNumber, email);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided Name, Mobile Number, and/or Email are still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        LOGGER.debug("handleUpdateAccountValidation() - Validation passed, updating account");
        ExtensionResponse response = updateAccountHelper.updateAccountWithValidation(name, mobileNumber, email);
        LOGGER.debug("handleUpdateAccountValidation() - Completed successfully");
        return response;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_CREATE_ACCOUNT_VALIDATION,
            description = "Checks if user wants to re-enter Name, Email, and Mobile Number for create account and validates them",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmCreateAccountValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Name: Text, Email: Text, Mobile Number: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmCreateAccountValidation() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmCreateAccountValidation() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_CREATE_ACCOUNT_VALIDATION,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_CREATE_ACCOUNT_VALIDATION,
            description = "Handle reenter Create Account validation",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field(name = "User Id", type = "Text", required = false)
    @Field(name = "Default Password", type = "Text", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleCreateAccountValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Name: Text, Email: Text, Mobile Number: Text }") Map<String, Object> map) {

        LOGGER.debug("handleCreateAccountValidation() - Full input map contents: {}", map);

        String name = (String) map.get(MicrosoftEntraIDResources.NAME_CREATE_ACCOUNT);
        String email = (String) map.get(MicrosoftEntraIDResources.EMAIL_CREATE_ACCOUNT);
        String mobileNumber = (String) map.get(MicrosoftEntraIDResources.MOBILE_NUMBER_CREATE_ACCOUNT);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.NAME_CREATE_ACCOUNT, name != null ? name : "",
                        Validator.ValidationResource.EMAIL_CREATE_ACCOUNT, email != null ? email : "",
                        Validator.ValidationResource.MOBILE_NUMBER_CREATE_ACCOUNT, mobileNumber != null ? mobileNumber : ""
                )
        );
        LOGGER.debug("handleCreateAccountValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails, continue the validation flow by asking for correct values
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleCreateAccountValidation() - Validation failed again after re-entry for NAME: {}, EMAIL: {}, MOBILE_NUMBER: {}", name, email, mobileNumber);
            return createAccountHelper.handleValidationFailure(validationResults, name, email, mobileNumber);
        }

        ExtensionResponse response = createAccountHelper.createAccountWithValidation(name, email, mobileNumber, null);
        LOGGER.debug("handleCreateAccountValidation() - Completed successfully");
        return response;
    }
}

