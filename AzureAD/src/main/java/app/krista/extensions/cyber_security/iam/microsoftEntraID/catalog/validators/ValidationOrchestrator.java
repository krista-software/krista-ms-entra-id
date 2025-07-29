package app.krista.extensions.cyber_security.iam.microsoftEntraID.catalog.validators;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates validation of input parameters.
 * This class manages a set of validators and applies them to input parameters.
 */
@Singleton
@Service
public class ValidationOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationOrchestrator.class);
    private final Map<Validator.ValidationResource, Validator> validators = new HashMap<>();

    /*
     * Registers a validator for a validation resource.
     *
     * @param resource  The validation resource
     * @param validator The validator
     */
    public void registerValidator(Validator.ValidationResource resource, Validator validator) {
        LOGGER.debug("Registering validator for resource: {}", resource);
        validators.put(resource, validator);
    }

    /**
     * Validates the given resources using the registered validators.
     *
     * @param resources The resources to validate
     * @return A list of validation results, empty if all validations pass
     */
    public List<ValidationResult> validate(Map<Validator.ValidationResource, String> resources) {
        List<ValidationResult> results = new ArrayList<>();
        System.out.println("ValidationOrchestrator.validate() - Validating " + resources.size() + " resources");
        for (Map.Entry<Validator.ValidationResource, String> entry : resources.entrySet()) {
            Validator validator = validators.get(entry.getKey());
            if (validator == null) {
                LOGGER.warn("No validator registered for resource: {}", entry.getKey());
                continue;
            }

            if (!validator.validate(entry.getValue(), resources)) {
                results.add(new ValidationResult(
                        validator.getConfirmStepMessage(entry.getValue(), resources),
                        validator.getFetchFieldName(),
                        validator.getFetchStepMessage(),
                        validator.getErrMessage(entry.getValue()),
                        validator.getFieldType()
                ));
            }
        }
        return results;
    }

    /**
     * Result of a validation.
     */
    public static class ValidationResult {
        private final String confirmStepMessage;
        private final String fetchFieldName;
        private final String fetchStepMessage;
        private final String errMessage;
        private final String fieldType;

        /**
         * Creates a new ValidationResult.
         *
         * @param confirmStepMessage The message to display when confirming a value
         * @param fetchFieldName     The field name for fetching a new value
         * @param fetchStepMessage   The message to display when fetching a new value
         * @param errMessage         The error message to display
         * @param fieldType          The field type
         */
        public ValidationResult(String confirmStepMessage, String fetchFieldName, String fetchStepMessage, String errMessage, String fieldType) {
            this.confirmStepMessage = confirmStepMessage;
            this.fetchFieldName = fetchFieldName;
            this.fetchStepMessage = fetchStepMessage;
            this.errMessage = errMessage;
            this.fieldType = fieldType;
        }

        public String getConfirmStepMessage() {
            return confirmStepMessage;
        }

        public String getFetchFieldName() {
            return fetchFieldName;
        }

        public String getFetchStepMessage() {
            return fetchStepMessage;
        }

        public String getErrMessage() {
            return errMessage;
        }

        public String getFieldType() {
            return fieldType;
        }
    }

    /**
     * Gets the list of validators.
     *
     * @return The list of validators
     */
    public List<Validator> getValidators() {
        return new ArrayList<>(validators.values());
    }
}
