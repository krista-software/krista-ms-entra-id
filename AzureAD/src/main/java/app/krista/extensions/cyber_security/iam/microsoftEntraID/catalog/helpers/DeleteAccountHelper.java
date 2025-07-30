package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators.EmailValidator;
import app.krista.extensions.cyber_security.iam.microsoftEntraID.impl.AccountImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;

/**
 * Helper class for Delete Account operations with validation support.
 */
@Service
public class DeleteAccountHelper extends BaseValidationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteAccountHelper.class);

    private final AccountImpl accountService;

    @Inject
    public DeleteAccountHelper(AccountImpl accountService,
                               ValidationOrchestrator validationOrchestrator,
                               ErrorHandlingStateManager internalStateManager,
                               ExtensionResponseGenerator responseGenerator) {
        super(validationOrchestrator, internalStateManager, responseGenerator);
        this.accountService = accountService;
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

    @Override
    protected Map<String, Object> createParameterMap(String stateId, Object... parameters) {
        // Extract parameters for Delete Account operation
        String email = parameters.length > 0 ? safeParameterToString(parameters[0]) : "";

        Map<String, Object> map = createBaseParameterMap(stateId);
        map.put(MicrosoftEntraIDResources.EMAIL, email);

        return map;
    }

    @Override
    protected String getConfirmationSubCatalogRequestName() {
        return SubCatalogConstants.CONFIRM_DELETE_ACCOUNT_VALIDATION;
    }

}
