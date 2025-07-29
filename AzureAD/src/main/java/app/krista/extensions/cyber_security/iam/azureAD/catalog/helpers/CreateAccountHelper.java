package app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.CreateAccountNameValidator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.CreateAccountMobileNumberValidator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.CreateAccountEmailValidator;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Create Account operation with validation support.
 * Handles validation flow and error handling for account creation.
 */
@Service
public class CreateAccountHelper extends BaseValidationHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccountHelper.class);

    private final AccountImpl accountService;

    @Inject
    public CreateAccountHelper(AccountImpl accountService, ValidationOrchestrator validationOrchestrator,
                               ErrorHandlingStateManager internalStateManager, ExtensionResponseGenerator responseGenerator,
                               CreateAccountNameValidator nameValidator, CreateAccountMobileNumberValidator mobileNumberValidator,
                               CreateAccountEmailValidator emailValidator) {
        super(validationOrchestrator, internalStateManager, responseGenerator);
        this.accountService = accountService;

        // Register validators
        validationOrchestrator.registerValidator(Validator.ValidationResource.NAME_CREATE_ACCOUNT, nameValidator);
        validationOrchestrator.registerValidator(Validator.ValidationResource.MOBILE_NUMBER_CREATE_ACCOUNT, mobileNumberValidator);
        validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL_CREATE_ACCOUNT, emailValidator);
    }

    /**
     * Creates an account with validation.
     *
     * @param name The name for the new account
     * @param email The email for the new account
     * @param mobileNumber The mobile number for the new account
     * @param extraFields Additional fields for the account
     * @return ExtensionResponse with the result
     */
    public ExtensionResponse createAccountWithValidation(String name, String email, String mobileNumber, List<Map<String, Object>> extraFields) {
        try {
            LOGGER.info("CreateAccountHelper.createAccountWithValidation() called with name: {}, email: {}, mobileNumber: {}", name, email, mobileNumber);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.NAME_CREATE_ACCOUNT, name != null ? name : "",
                            Validator.ValidationResource.EMAIL_CREATE_ACCOUNT, email != null ? email : "",
                            Validator.ValidationResource.MOBILE_NUMBER_CREATE_ACCOUNT, mobileNumber != null ? mobileNumber : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, name, email, mobileNumber);
            }

            // If validation passes, proceed with creating the account
            LOGGER.debug("Validation passed, creating account");
            Map<String, Object> result = accountService.createUser(name, email, mobileNumber, extraFields);
            LOGGER.info("Account created successfully for email: {}", email);
            return new ExtensionResponseBuilder().success(result).build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while creating account: {}", cause.getMessage(), cause);
            // Uses centralized error response generation via BaseValidationHelper
            return generateErrorResponse("Error occurred while creating account");
        }
    }

    @Override
    protected Map<String, Object> createParameterMap(String stateId, Object... parameters) {
        // Extract parameters for Create Account operation
        String name = parameters.length > 0 ? safeParameterToString(parameters[0]) : "";
        String email = parameters.length > 1 ? safeParameterToString(parameters[1]) : "";
        String mobileNumber = parameters.length > 2 ? safeParameterToString(parameters[2]) : "";

        Map<String, Object> map = createBaseParameterMap(stateId);

        // Always include all fields to preserve valid values during re-entry flow
        map.put(AzureResources.NAME_CREATE_ACCOUNT, name);
        map.put(AzureResources.EMAIL_CREATE_ACCOUNT, email);
        map.put(AzureResources.MOBILE_NUMBER_CREATE_ACCOUNT, mobileNumber);

        return map;
    }

    @Override
    protected String getConfirmationSubCatalogRequestName() {
        return SubCatalogConstants.CONFIRM_CREATE_ACCOUNT_VALIDATION;
    }

    @Override
    protected Map<String, Object> getCustomErrorResult() {
        // Create Account uses a custom success response with empty fields for errors
        return Map.of("User Id", "", "Default Password", "");
    }
}
