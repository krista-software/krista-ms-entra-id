package app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp.ExtensionResponseFactory;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.UserMailOrIdValidator;
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
 * Helper class for Activate or Deactivate Account operations with validation support.
 */
@Service
public class ActivateDeactivateAccountHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateDeactivateAccountHelper.class);

    public static final String ACCOUNT_STATUS = "Account Status";
    public static final String MESSAGE = "Message";

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public ActivateDeactivateAccountHelper(AccountImpl accountService,
                                           ValidationOrchestrator validationOrchestrator,
                                           ErrorHandlingStateManager internalStateManager,
                                           ExtensionResponseGenerator responseGenerator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("ActivateDeactivateAccountHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register User Mail or ID validator
            UserMailOrIdValidator userMailOrIdValidator = new UserMailOrIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.USER_MAIL_OR_ID, userMailOrIdValidator);
            LOGGER.info("Registered UserMailOrIdValidator for USER_MAIL_OR_ID");

            LOGGER.info("ActivateDeactivateAccountHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse activateOrDeactivateAccountWithValidation(String userMailOrID, Boolean deactivate) {
        try {
            LOGGER.info("ActivateDeactivateAccountHelper.activateOrDeactivateAccountWithValidation() called with userMailOrID: {}, deactivate: {}", userMailOrID, deactivate);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.USER_MAIL_OR_ID, userMailOrID != null ? userMailOrID : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("User Mail or ID validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, userMailOrID, deactivate);
            }

            // If validation passes, proceed with activating/deactivating the account
            LOGGER.debug("Validation passed, activating/deactivating account");
            boolean accountStatus = accountService.activateOrDeactivateAccount(userMailOrID, deactivate);
            String statusMessage = deactivate ? "Deactivated" : "Activated";

            Map<String, Object> result = Map.of(
                    ACCOUNT_STATUS, accountStatus,
                    MESSAGE, "Account " + statusMessage + " Successfully");

            LOGGER.info("Successfully {} account for userMailOrID: {}", statusMessage.toLowerCase(), userMailOrID);

            return ExtensionResponseFactory.create(result);

        } catch (Exception cause) {
            LOGGER.error("Error occurred while activating/deactivating account: {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder()
                    .failure(new ExtensionResponse.Error("Error occurred while activating/deactivating account",
                            System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, ""))
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String userMailOrID, Boolean deactivate) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("User Mail or ID validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("User Mail or ID validation failed, generating confirmation response");

        // Create a map with the necessary data for the confirmation step
        Map<String, Object> map = Map.of(
                SubCatalogConstants.STATE_ID, stateId,
                AzureResources.USER_MAIL_OR_ID, userMailOrID != null ? userMailOrID : "",
                "Deactivate", deactivate != null ? deactivate : false
        );
        LOGGER.debug("User Mail or ID validation failed, generated confirmation response map: {}", map);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_ACTIVATE_DEACTIVATE_ACCOUNT_VALIDATION,
                map);
    }

}
