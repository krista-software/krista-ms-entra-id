package app.krista.extensions.cyber_security.iam.azureAD.catalog.extresp;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.RemediationActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExtensionResponseFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionResponseFactory.class);

    private ExtensionResponseFactory() {
    }

    public static ExtensionResponse create(Map<String, Object> values) {
        return new ExtensionResponseBuilder().success(values).build();
    }

    public static ExtensionResponse create(Exception cause,
                                           String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType,
                                           List<RemediationAction> actions,
                                           String subCatalogRequestName,
                                           Map<String, Object> state) {
        RemediationActions remediationActions = new RemediationActions(actions, subCatalogRequestName);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, Arrays.toString(cause.getStackTrace()));
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                remediationActions, state);
    }

    public static ExtensionResponse create(String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType,
                                           List<RemediationAction> actions,
                                           String subCatalogRequestName,
                                           Map<String, Object> state) {
        RemediationActions remediationActions = new RemediationActions(actions, subCatalogRequestName);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(), exceptionType, "");
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, remediationActions, state);
    }

    public static ExtensionResponse create(String message, ExtensionResponse.Error.ExceptionType exceptionType,
                                           List<RemediationAction> actions, String subCatalogRequestName,
                                           Map<String, Object> state, ExtensionResponse.Result result) {
        RemediationActions remediationActions = new RemediationActions(actions, subCatalogRequestName);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(), exceptionType, "");
        return new ExtensionResponse(result, null, error, remediationActions, state);
    }

    /**
     * Generates a standardized error response for logic errors.
     * This method consolidates the common error response pattern used across all helper classes.
     *
     * @param errorMessage The error message to include in the response
     * @return ExtensionResponse with failure result and logic error type
     */
    public static ExtensionResponse generateErrorResponse(String errorMessage) {
        LOGGER.error("Generating error response: {}", errorMessage);
        ExtensionResponse.Error error = new ErrorBuilder()
                .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                .errorMessage(errorMessage)
                .build();

        return new ExtensionResponseBuilder()
                .failure(error)
                .build();
    }

    /**
     * Generates a standardized error response for logic errors with additional detailed message.
     * This method provides an overloaded version for cases where more detailed error information is needed.
     *
     * @param errorMessage The primary error message
     * @param detailedMessage Additional detailed error information (currently not used in response but logged)
     * @return ExtensionResponse with failure result and logic error type
     */
    public static ExtensionResponse generateErrorResponse(String errorMessage, String detailedMessage) {
        LOGGER.error("Generating error response: {} - Details: {}", errorMessage, detailedMessage);
        ExtensionResponse.Error error = new ErrorBuilder()
                .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                .errorMessage(errorMessage)
                .build();

        return new ExtensionResponseBuilder()
                .failure(error)
                .build();
    }
}
