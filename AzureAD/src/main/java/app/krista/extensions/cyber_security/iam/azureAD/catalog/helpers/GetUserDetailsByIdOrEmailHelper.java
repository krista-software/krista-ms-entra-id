package app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.ErrorBuilder;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.UserIdValidator;
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
 * Helper class for Get User Details by ID or Email operations with validation support.
 */
@Service
public class GetUserDetailsByIdOrEmailHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserDetailsByIdOrEmailHelper.class);

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public GetUserDetailsByIdOrEmailHelper(AccountImpl accountService,
                                           ValidationOrchestrator validationOrchestrator,
                                           ErrorHandlingStateManager internalStateManager,
                                           ExtensionResponseGenerator responseGenerator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("GetUserDetailsByIdOrEmailHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register User ID/Email validator
            UserIdValidator userIdValidator = new UserIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.ID, userIdValidator);
            LOGGER.info("Registered UserIdValidator for USER_ID");

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
                    Map.of(Validator.ValidationResource.ID, userId != null ? userId : "")
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

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String userId) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("User ID validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("User ID validation failed, generating confirmation response");

        // Create a map with the necessary data for the confirmation step
        Map<String, Object> map = Map.of(
                SubCatalogConstants.STATE_ID, stateId,
                "Id", userId != null ? userId : ""
        );

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
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
