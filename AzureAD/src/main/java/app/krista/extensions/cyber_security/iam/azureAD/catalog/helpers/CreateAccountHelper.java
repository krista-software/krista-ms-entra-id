package app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.ErrorBuilder;
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
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
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
public class CreateAccountHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccountHelper.class);

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public CreateAccountHelper(AccountImpl accountService, ValidationOrchestrator validationOrchestrator,
                               ErrorHandlingStateManager internalStateManager, ExtensionResponseGenerator responseGenerator,
                               CreateAccountNameValidator nameValidator, CreateAccountMobileNumberValidator mobileNumberValidator,
                               CreateAccountEmailValidator emailValidator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;

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
            return generateErrorResponse("Error occurred while creating account");
        }
    }

    public ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                     String name, String email, String mobileNumber) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("Validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("Validation failed, generating confirmation response");

        // Include all fields in the confirmation response to preserve valid values
        Map<String, Object> map = new HashMap<>();
        map.put(SubCatalogConstants.STATE_ID, stateId);
        
        // Always include all fields to preserve valid values during re-entry flow
        map.put(AzureResources.NAME_CREATE_ACCOUNT, name != null ? name : "");
        map.put(AzureResources.EMAIL_CREATE_ACCOUNT, email != null ? email : "");
        map.put(AzureResources.MOBILE_NUMBER_CREATE_ACCOUNT, mobileNumber != null ? mobileNumber : "");

        LOGGER.debug("Validation failed, generated confirmation response map: {}", map);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_CREATE_ACCOUNT_VALIDATION,
                map);
    }

    private ExtensionResponse generateErrorResponse(String message) {
        Map<String, Object> errorResult = Map.of("User Id", "", "Default Password", "");
        return new ExtensionResponseBuilder().success(errorResult).build();
    }
}
