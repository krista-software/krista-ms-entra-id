package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.RemoveAccountUserMailOrIdValidator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.RemoveAccountGroupObjectIdValidator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;

import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Remove Account From Group operations with validation support.
 */
@Service
public class RemoveAccountFromGroupHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAccountFromGroupHelper.class);

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public RemoveAccountFromGroupHelper(AccountImpl accountService,
                                        ValidationOrchestrator validationOrchestrator,
                                        ErrorHandlingStateManager internalStateManager,
                                        ExtensionResponseGenerator responseGenerator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("RemoveAccountFromGroupHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register User Mail or Id validator for remove account from group
            RemoveAccountUserMailOrIdValidator userMailOrIdValidator = new RemoveAccountUserMailOrIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.USER_MAIL_OR_ID_REMOVE_GROUP, userMailOrIdValidator);
            LOGGER.info("Registered RemoveAccountUserMailOrIdValidator for USER_MAIL_OR_ID_REMOVE_GROUP");

            // Register Group Object Id validator for remove account from group
            RemoveAccountGroupObjectIdValidator groupObjectIdValidator = new RemoveAccountGroupObjectIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.GROUP_OBJECT_ID, groupObjectIdValidator);
            LOGGER.info("Registered RemoveAccountGroupObjectIdValidator for GROUP_OBJECT_ID");

            LOGGER.info("RemoveAccountFromGroupHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse removeAccountFromGroupWithValidation(String userMailOrId, String groupObjectId) {
        try {
            LOGGER.info("RemoveAccountFromGroupHelper.removeAccountFromGroupWithValidation() called with userMailOrId: {}, groupObjectId: {}", userMailOrId, groupObjectId);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.USER_MAIL_OR_ID_REMOVE_GROUP, userMailOrId != null ? userMailOrId : "",
                            Validator.ValidationResource.GROUP_OBJECT_ID, groupObjectId != null ? groupObjectId : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, userMailOrId, groupObjectId);
            }

            // If validation passes, proceed with removing the account from group
            LOGGER.debug("Validation passed, removing account from group");
            Boolean removed = accountService.removeAccountFromGroup(userMailOrId, groupObjectId);

            LOGGER.info("Successfully removed account from group for userMailOrId: {}, groupObjectId: {}", userMailOrId, groupObjectId);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Removed", removed))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while removing account from group: {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder()
                    .failure(new ExtensionResponse.Error("Error occurred while removing account from group",
                            System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, ""))
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String userMailOrId, String groupObjectId) {
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

        // Always include both fields to preserve valid values during re-entry flow
        map.put(MicrosoftEntraIDResources.USER_MAIL_OR_ID_REMOVE_GROUP, userMailOrId != null ? userMailOrId : "");
        map.put(MicrosoftEntraIDResources.GROUP_OBJECT_ID, groupObjectId != null ? groupObjectId : "");

        LOGGER.debug("Validation failed, generated confirmation response map: {}", map);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_REMOVE_ACCOUNT_FROM_GROUP_VALIDATION,
                map);
    }

}
