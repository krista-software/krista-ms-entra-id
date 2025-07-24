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
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.EmailValidator;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Delete Account operations with validation support.
 */
@Service
public class DeleteAccountHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteAccountHelper.class);

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public DeleteAccountHelper(AccountImpl accountService,
                               ValidationOrchestrator validationOrchestrator,
                               ErrorHandlingStateManager internalStateManager,
                               ExtensionResponseGenerator responseGenerator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("DeleteAccountHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register Email validator
            EmailValidator emailValidator = new EmailValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL, emailValidator);
            LOGGER.info("Registered EmailValidator for EMAIL");

        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse deleteAccountWithValidation(String email) {
        try {
            LOGGER.info("DeleteAccountHelper.deleteAccountWithValidation() called with email: {}", email);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.EMAIL, email != null ? email : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Email validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, email);
            }

            // If validation passes, proceed with deleting the account
            LOGGER.debug("Validation passed, deleting account");
            Boolean deleted = accountService.deleteUser(email);

            LOGGER.info("Successfully deleted account for email: {}", email);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Deleted", deleted))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while deleting account: {}", cause.getMessage(), cause);
            return generateErrorResponse("Error occurred while deleting account");
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String email) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("Email validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("Email validation failed, generating confirmation response");

        // Create a map with the necessary data for the confirmation step
        Map<String, Object> map = Map.of(
                SubCatalogConstants.STATE_ID, stateId,
                AzureResources.EMAIL, email != null ? email : ""
        );
        LOGGER.debug("Email validation failed, generated confirmation response map: {}", map);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_DELETE_ACCOUNT_VALIDATION,
                map);
    }

    private ExtensionResponse generateErrorResponse(String errorMessage) {
        LOGGER.error("Generating error response: {}", errorMessage);
        ExtensionResponse.Error error = new ErrorBuilder()
                .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                .errorMessage(errorMessage)
                .build();

        return new ExtensionResponseBuilder()
                .failure(error)
                .build();
    }
}
