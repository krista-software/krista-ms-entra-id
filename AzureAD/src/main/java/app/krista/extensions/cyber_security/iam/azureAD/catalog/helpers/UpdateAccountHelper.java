package app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.UpdateAccountNameValidator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.UpdateAccountMobileNumberValidator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.UpdateAccountEmailValidator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
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
 * Helper class for Update Account operations with validation support.
 */
@Service
public class UpdateAccountHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAccountHelper.class);

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public UpdateAccountHelper(AccountImpl accountService,
                               ValidationOrchestrator validationOrchestrator,
                               ErrorHandlingStateManager internalStateManager,
                               ExtensionResponseGenerator responseGenerator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("UpdateAccountHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register Name validator for update account
            UpdateAccountNameValidator nameValidator = new UpdateAccountNameValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.NAME_UPDATE_ACCOUNT, nameValidator);
            LOGGER.info("Registered UpdateAccountNameValidator for NAME_UPDATE_ACCOUNT");

            // Register Mobile Number validator for update account
            UpdateAccountMobileNumberValidator mobileNumberValidator = new UpdateAccountMobileNumberValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.MOBILE_NUMBER_UPDATE_ACCOUNT, mobileNumberValidator);
            LOGGER.info("Registered UpdateAccountMobileNumberValidator for MOBILE_NUMBER_UPDATE_ACCOUNT");

            // Register Email validator for update account
            UpdateAccountEmailValidator emailValidator = new UpdateAccountEmailValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.EMAIL_UPDATE_ACCOUNT, emailValidator);
            LOGGER.info("Registered UpdateAccountEmailValidator for EMAIL_UPDATE_ACCOUNT");

            LOGGER.info("UpdateAccountHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse updateAccountWithValidation(String name, String mobileNumber, String email) {
        try {
            LOGGER.info("UpdateAccountHelper.updateAccountWithValidation() called with name: {}, mobileNumber: {}, email: {}", name, mobileNumber, email);

            // Validate input parameters (all three fields)
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.NAME_UPDATE_ACCOUNT, name != null ? name : "",
                            Validator.ValidationResource.MOBILE_NUMBER_UPDATE_ACCOUNT, mobileNumber != null ? mobileNumber : "",
                            Validator.ValidationResource.EMAIL_UPDATE_ACCOUNT, email != null ? email : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, name, mobileNumber, email);
            }

            // If validation passes, proceed with updating the account
            LOGGER.debug("Validation passed, updating account");
            Account updatedAccount = accountService.updateUser(
                    name != null ? name : "",
                    mobileNumber != null ? mobileNumber : "",
                    email,
                    null); // extraField is not part of validation, so passing null

            LOGGER.info("Successfully updated account with name: {}, mobileNumber: {}, email: {}", name, mobileNumber, email);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Updated Entity", updatedAccount))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while updating account: {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder()
                    .failure(new ExtensionResponse.Error("Error occurred while updating account",
                            System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, ""))
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String name, String mobileNumber, String email) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("Validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("Validation failed, generating confirmation response");

        // Create a map with the necessary data for the confirmation step
        Map<String, Object> map = Map.of(
                SubCatalogConstants.STATE_ID, stateId,
                AzureResources.NAME_UPDATE_ACCOUNT, name != null ? name : "",
                AzureResources.MOBILE_NUMBER_UPDATE_ACCOUNT, mobileNumber != null ? mobileNumber : "",
                AzureResources.EMAIL_UPDATE_ACCOUNT, email != null ? email : ""
        );
        LOGGER.debug("Validation failed, generated confirmation response map: {}", map);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_UPDATE_ACCOUNT_VALIDATION,
                map);
    }

}
