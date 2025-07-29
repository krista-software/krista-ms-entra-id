package app.krista.extensions.cyber_security.iam.azureAD.catalog.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base class for validators that provides common functionality.
 * Extend this class to create validators for specific input types.
 */
public abstract class BaseValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseValidator.class);

    /**
     * The field name for this validator.
     */
    protected final String fieldName;

    /**
     * The field type for this validator.
     */
    protected final String fieldType;

    /**
     * Creates a new BaseValidator with the given field name and type.
     *
     * @param fieldName The field name
     * @param fieldType The field type
     */
    protected BaseValidator(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @Override
    public String getFetchFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldType() {
        return fieldType;
    }

    @Override
    public String getFetchStepMessage() {
        return String.format("Please enter a valid %s.", fieldName);
    }

    @Override
    public String getConfirmStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided %s '%s' is invalid.", fieldName, resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid %s: %s", fieldName, resourceId);
    }

    /**
     * Logs an exception and returns false.
     *
     * @param e The exception to log
     * @return false
     */
    protected boolean logExceptionAndReturnFalse(Exception e) {
        LOGGER.error("Exception in validator: {}", e.getMessage(), e);
        return false;
    }
}
