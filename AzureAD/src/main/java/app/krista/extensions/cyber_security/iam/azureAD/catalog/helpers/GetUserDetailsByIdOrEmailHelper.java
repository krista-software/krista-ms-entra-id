package app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.GetUserDetailsIdValidator;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;

/**
 * Helper class for Get User Details by ID or Email operations with validation support.
 */
@Service
public class GetUserDetailsByIdOrEmailHelper extends BaseValidationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserDetailsByIdOrEmailHelper.class);

    private final AccountImpl accountService;

    @Inject
    public GetUserDetailsByIdOrEmailHelper(AccountImpl accountService,
                                           ValidationOrchestrator validationOrchestrator,
                                           ErrorHandlingStateManager internalStateManager,
                                           ExtensionResponseGenerator responseGenerator) {
        super(validationOrchestrator, internalStateManager, responseGenerator);
        this.accountService = accountService;
        LOGGER.info("GetUserDetailsByIdOrEmailHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register Get User Details specific ID validator
            GetUserDetailsIdValidator getUserDetailsIdValidator = new GetUserDetailsIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.ID_GET_USER_DETAILS, getUserDetailsIdValidator);
            LOGGER.info("Registered GetUserDetailsIdValidator for ID_GET_USER_DETAILS");

            LOGGER.info("GetUserDetailsByIdOrEmailHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getUserDetailsByIdOrEmailWithValidation(String userId) {
        try {
            LOGGER.info("GetUserDetailsByIdOrEmailHelper.getUserDetailsByIdOrEmailWithValidation() called with userId: {}", userId);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.ID_GET_USER_DETAILS, userId != null ? userId : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("User ID validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, userId);
            }

            // If validation passes, proceed with retrieving user details
            LOGGER.debug("Validation passed, retrieving user details");
            Account result = accountService.getUser(userId);

            if (result == null) {
                LOGGER.warn("No user found with ID/Email: {}", userId);
                
                return new ExtensionResponseBuilder()
                        .success(Map.of("Result", "No user found with the specified ID or email"))
                        .build();
            }

            LOGGER.info("Successfully retrieved user details for: {}", userId);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Result", result))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while retrieving user details by ID or email: {}", cause.getMessage(), cause);
            return generateErrorResponse("Error occurred while retrieving user details by ID or email");
        }
    }

    @Override
    protected Map<String, Object> createParameterMap(String stateId, Object... parameters) {
        // Extract parameters for Get User Details operation
        String userId = parameters.length > 0 ? safeParameterToString(parameters[0]) : "";

        Map<String, Object> map = createBaseParameterMap(stateId);
        map.put(AzureResources.ID, userId);

        return map;
    }

    @Override
    protected String getConfirmationSubCatalogRequestName() {
        return SubCatalogConstants.CONFIRM_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL;
    }
}
