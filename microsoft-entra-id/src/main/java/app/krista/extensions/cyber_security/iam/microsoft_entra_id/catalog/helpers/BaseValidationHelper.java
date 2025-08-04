package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all validation helpers that provides common functionality
 * for handling validation failures and generating error responses.
 * 
 * This class implements the Template Method pattern to eliminate code duplication
 * across all helper classes while allowing customization of specific behaviors.
 */
public abstract class BaseValidationHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseValidationHelper.class);
    
    protected final ValidationOrchestrator validationOrchestrator;
    protected final ErrorHandlingStateManager internalStateManager;
    protected final ExtensionResponseGenerator responseGenerator;

    /**
     * Constructor for base validation helper.
     * 
     * @param validationOrchestrator The validation orchestrator
     * @param internalStateManager The state manager for error handling
     * @param responseGenerator The response generator for creating responses
     */
    protected BaseValidationHelper(ValidationOrchestrator validationOrchestrator,
                                   ErrorHandlingStateManager internalStateManager,
                                   ExtensionResponseGenerator responseGenerator) {
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
    }

    /**
     * Template method for handling validation failures.
     * This method provides the common structure while allowing subclasses
     * to customize specific parts through abstract methods.
     *
     * @param validationResults The validation results containing errors
     * @param parameters The parameters that failed validation
     * @return ExtensionResponse for the validation failure
     */
    public final ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            Object... parameters) {
        
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("Validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("Validation failed, generating confirmation response");

        // Create the parameter map - delegate to subclass for customization
        Map<String, Object> parameterMap = createParameterMap(stateId, parameters);
        LOGGER.debug("Validation failed, generated confirmation response map: {}", parameterMap);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                getConfirmationSubCatalogRequestName(),
                parameterMap);
    }

    /**
     * Template method for generating error responses.
     * Provides a consistent error response structure across all helpers.
     * Uses the existing ExtensionResponseBuilder for consistency.
     *
     * @param errorMessage The error message to include
     * @return ExtensionResponse for the error
     */
    public final ExtensionResponse generateErrorResponse(String errorMessage) {
        LOGGER.error("Generating error response: {}", errorMessage);

        // Check if this helper needs a custom error response
        Map<String, Object> customErrorResult = getCustomErrorResult();
        if (customErrorResult != null) {
            return new ExtensionResponseBuilder().success(customErrorResult).build();
        }

        // Use standard ExtensionResponseBuilder for error responses
        return new ExtensionResponseBuilder()
                .failure(new ExtensionResponse.Error(errorMessage, System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, ""))
                .build();
    }

    /**
     * Abstract method that subclasses must implement to create the parameter map
     * for validation failure responses. This allows each helper to customize
     * which parameters are included in the confirmation response.
     * 
     * @param stateId The unique state ID for this validation session
     * @param parameters The parameters that failed validation
     * @return Map containing the parameters for the confirmation response
     */
    protected abstract Map<String, Object> createParameterMap(String stateId, Object... parameters);

    /**
     * Abstract method that subclasses must implement to provide the name
     * of the confirmation subcatalog request for this helper.
     * 
     * @return The name of the confirmation subcatalog request
     */
    protected abstract String getConfirmationSubCatalogRequestName();

    /**
     * Hook method that subclasses can override to provide custom error results.
     * By default, returns null which means use the standard failure response.
     * 
     * @return Custom error result map, or null for standard failure response
     */
    protected Map<String, Object> getCustomErrorResult() {
        return null; // Default: use standard failure response
    }

    /**
     * Utility method to create a basic parameter map with state ID.
     * Subclasses can use this as a starting point and add their specific parameters.
     * 
     * @param stateId The unique state ID
     * @return Basic parameter map with state ID
     */
    protected final Map<String, Object> createBaseParameterMap(String stateId) {
        Map<String, Object> map = new HashMap<>();
        map.put(SubCatalogConstants.STATE_ID, stateId);
        return map;
    }

    /**
     * Utility method to safely convert parameters to strings.
     * Handles null values by converting them to empty strings.
     * 
     * @param parameter The parameter to convert
     * @return String representation of the parameter, or empty string if null
     */
    protected final String safeParameterToString(Object parameter) {
        return parameter != null ? parameter.toString() : "";
    }
}
