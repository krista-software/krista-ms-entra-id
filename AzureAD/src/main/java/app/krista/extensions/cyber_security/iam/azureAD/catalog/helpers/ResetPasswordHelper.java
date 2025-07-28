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
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ResetPasswordIdValidator;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.core.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Reset Password operations with validation support.
 */
@Service
public class ResetPasswordHelper extends BaseValidationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordHelper.class);

    private final AccountImpl accountService;

    @Inject
    public ResetPasswordHelper(AccountImpl accountService,
                               ValidationOrchestrator validationOrchestrator,
                               ErrorHandlingStateManager internalStateManager,
                               ExtensionResponseGenerator responseGenerator) {
        super(validationOrchestrator, internalStateManager, responseGenerator);
        this.accountService = accountService;
        LOGGER.info("ResetPasswordHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register Reset Password specific ID validator
            ResetPasswordIdValidator resetPasswordIdValidator = new ResetPasswordIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.ID, resetPasswordIdValidator);
            LOGGER.info("Registered ResetPasswordIdValidator for ID in ResetPasswordHelper");

            LOGGER.info("ResetPasswordHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse resetPasswordWithValidation(String id) {
        try {
            LOGGER.info("ResetPasswordHelper.resetPasswordWithValidation() called with id: {}", id);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.ID, id != null ? id : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("ID validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, id);
            }

            // If validation passes, proceed with resetting the password
            LOGGER.debug("Validation passed, resetting password");
            String password = resetPasswordWithRetry(id);

            LOGGER.info("Successfully reset password for id: {}", id);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Password", password))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while resetting password: {}", cause.getMessage(), cause);
            return ExtensionResponseFactory.generateErrorResponse("Error occurred while resetting password");
        }
    }

    private String resetPasswordWithRetry(String id) {
        final int maxAttempts = 3;
        int attempts = 0;
        int backoffTime = 3000; // initial backoff time in milliseconds

        while (attempts < maxAttempts) {
            try {
                return accountService.resetPassword(id);
            } catch (ClientException cause) {
                Throwable rootCause = cause.getCause();

                if (rootCause instanceof GraphServiceException) {
                    GraphServiceException graphEx = (GraphServiceException) rootCause;
                    int statusCode = graphEx.getResponseCode();
                    if (statusCode == Response.Status.FORBIDDEN.getStatusCode()) {
                        attempts++;
                        if (attempts < maxAttempts) {
                            sleepWithBackoff(backoffTime);
                            backoffTime *= 2; // exponential backoff
                        } else {
                            LOGGER.error("Max attempts reached. Error occurred while resetting password: {}", cause.getMessage(), cause);
                            return "";
                        }
                    } else {
                        LOGGER.error("GraphServiceException occurred while resetting password: {}", cause.getMessage(), cause);
                        return "";
                    }
                } else {
                    LOGGER.error("Unexpected error occurred while resetting password: {}", cause.getMessage(), cause);
                    return "";
                }
            }
        }
        return "";
    }

    private void sleepWithBackoff(int backoffTime) {
        try {
            Thread.sleep(backoffTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted during sleep: {}", e.getMessage(), e);
        }
    }

    @Override
    protected Map<String, Object> createParameterMap(String stateId, Object... parameters) {
        // Extract parameters for Reset Password operation
        String id = parameters.length > 0 ? safeParameterToString(parameters[0]) : "";

        Map<String, Object> map = createBaseParameterMap(stateId);
        map.put(AzureResources.ID, id);

        return map;
    }

    @Override
    protected String getConfirmationSubCatalogRequestName() {
        return SubCatalogConstants.CONFIRM_RESET_PASSWORD_VALIDATION;
    }

    // Uses default generateErrorResponse from base class (standard failure response)
}
