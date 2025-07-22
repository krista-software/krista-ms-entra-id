package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import java.util.Map;

/**
 * Interface for all validators in the Office365Excel extension.
 * Each validator is responsible for validating a specific type of resource.
 */
public interface Validator {

    enum ValidationResource {
        USER_ID
    }

    /**
     * Validates the given resource against validation rules
     * @param resourceValue The resource value to validate
     * @param context Additional context for validation
     * @return true if valid, false otherwise
     */
    Boolean validate(String resourceValue, Map<ValidationResource, String> context);

    /**
     * Gets the field name for fetch operations
     */
    String getFetchFieldName();

    /**
     * Gets the field type for the UI
     */
    String getFieldType();

    /**
     * Gets the message to display during fetch step
     */
    String getFetchStepMessage();

    /**
     * Gets the confirmation message after validation
     */
    String getConfirmStepMessage(String resourceValue, Map<ValidationResource, String> context);

    /**
     * Gets the error message for invalid resources
     */
    String getErrMessage(String resourceValue);
}
