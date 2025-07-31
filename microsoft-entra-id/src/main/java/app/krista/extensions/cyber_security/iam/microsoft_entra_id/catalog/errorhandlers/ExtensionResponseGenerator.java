package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers;

import app.krista.extension.executor.ExtensionResponse;


import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp.ExtensionResponseFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp.NamedFieldFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.extresp.RemediationActionFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.ValidationOrchestrator;
import app.krista.model.field.NamedField;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class ExtensionResponseGenerator {

    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String STEP_MESSAGE = "StepMessage";
    public static final String FIELD = "Field";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionResponseGenerator.class);

    public ExtensionResponse generateConfirmationResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("ExtensionResponseGenerator.generateConfirmationResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        List<NamedField> fields = List.of(NamedFieldFactory.createSwitchField(SubCatalogConstants.REENTER));
        Map<String, Object> stringStringMap = generateResponse(validationResults, false);

        // Use the new askAPersonToReenter method for better user experience
        String confirmationMessage = "Input validation failed. Would you like to re-enter the correct values?";
        ExtensionResponse response = ExtensionResponseFactory.create(confirmationMessage, exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), fields)),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateConfirmationResponse() completed successfully - Confirmation response generated");

        return response;
    }

    private Map<String, Object> generateResponse(List<ValidationOrchestrator.ValidationResult> validationResults, boolean fetchResponse) {
        StringBuilder stepMessage = new StringBuilder();
        StringBuilder errMessage = new StringBuilder();
        List<NamedField> fields = new ArrayList<>();
        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            stepMessage.append(!fetchResponse ? validationResult.getConfirmStepMessage() : validationResult.getFetchStepMessage()).append("\n");
            errMessage.append(validationResult.getErrMessage()).append("\n");
            if (fetchResponse) {
                fields.add(NamedFieldFactory.createField(validationResult.getFetchFieldName(), validationResult.getFieldType()));
            }
        }
        return Map.of(STEP_MESSAGE, stepMessage.toString(), ERROR_MESSAGE, errMessage.toString(), FIELD, fields);
    }

    @SuppressWarnings("unchecked")
    public ExtensionResponse generateFetchResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("ExtensionResponseGenerator.generateFetchResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        Map<String, Object> stringStringMap = generateResponse(validationResults, true);

        // Use a user-friendly message for fetch responses
        String fetchMessage = "Please provide the correct values for the highlighted fields.";
        ExtensionResponse response = ExtensionResponseFactory.create(fetchMessage, exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), (List<NamedField>) stringStringMap.get(FIELD))),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateFetchResponse() completed successfully - Fetch response generated");

        return response;
    }

    public ExtensionResponse generateFetchDenyResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state
    ) {
        LOGGER.info("ExtensionResponseGenerator.generateFetchDenyResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        StringJoiner stepMessage = new StringJoiner(", ", "Updated value for ", " were not provided.");
        StringBuilder errMessage = new StringBuilder();
        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            stepMessage.add("'" + validationResult.getFetchFieldName() + "'");
            errMessage.append(validationResult.getErrMessage()).append("\n");
        }

        // Use a user-friendly message for deny responses
        String denyMessage = "Operation cancelled. No values were updated.";
        ExtensionResponse response = ExtensionResponseFactory.create(denyMessage, exceptionType,
                List.of(RemediationActionFactory.createInformActionALLParticipants(stepMessage.toString(), List.of())),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateFetchDenyResponse() completed successfully - Deny response generated");

        return response;
    }
}

