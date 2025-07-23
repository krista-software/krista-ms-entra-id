package app.krista.extensions.cyber_security.iam.azureAD.catalog;

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.helpers.GetUserDetailsByIdOrEmailHelper;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.AzureResources;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Service
public class AccountManagerAreaSubcatalogRequests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountManagerAreaSubcatalogRequests.class);

    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ValidationOrchestrator validationOrchestrator;
    private final GetUserDetailsByIdOrEmailHelper getUserDetailsHelper;

    @Inject
    public AccountManagerAreaSubcatalogRequests(ExtensionResponseGenerator responseGenerator,
                                                ErrorHandlingStateManager internalStateManager,
                                                ValidationOrchestrator validationOrchestrator, GetUserDetailsByIdOrEmailHelper getUserDetailsHelper) {
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.validationOrchestrator = validationOrchestrator;
        this.getUserDetailsHelper = getUserDetailsHelper;

        LOGGER.info("AccountManagerArea Sub catalog Requests constructor completed");
    }


    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
            description = "Checks if user wants to re-enter input and validates it",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmReenterGetUserDetailsByIDOrEmail(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Id: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmReenterGetUserDetailsByIDOrEmail() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmReenterGetUserDetailsByIDOrEmail() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
                map);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_REENTER_GET_USER_DETAILS_BY_ID_OR_EMAIL,
            description = "Handle reenter Get User Details by ID or Email",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Result", type = "Entity", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterGetUserDetailsByIDOrEmail(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Id: Text }") Map<String, Object> map) {

        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Full input map contents: {}", map);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            LOGGER.debug("Key: '{}', Value: '{}', Value type: {}",
                    entry.getKey(), entry.getValue(),
                    entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
        }

        String userId = (String) map.get(AzureResources.ID);
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Extracted parameters: USER_ID={}", userId);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.ID, userId != null ? userId : "")
        );
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Validation results: {} errors", validationResults.size());
        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Validation failed again after re-entry for USER_ID: {}", userId);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided user ID is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Validation passed, retrieving user details");
        ExtensionResponse response = getUserDetailsHelper.getUserDetailsByIdOrEmailWithValidation(userId);
        LOGGER.debug("handleReenterGetUserDetailsByIDOrEmail() - Completed successfully");
        return response;

    }

    @NotNull
    @SuppressWarnings("unchecked")
    private List<ValidationOrchestrator.ValidationResult> getValidationResults(String stateId) {
        LOGGER.debug("getValidationResults() - Starting with stateId: {}", stateId);

        List<ValidationOrchestrator.ValidationResult> results = new java.util.ArrayList<>();

        try {
            // Try to get the raw stored data as a string first
            String storedData = internalStateManager.getRaw(stateId);
            LOGGER.debug("getValidationResults() - Retrieved raw data: {}", storedData != null ? "not null" : "null");

            if (storedData != null) {
                // Try to parse as direct ValidationResult list first (from GetChannelIDHelper)
                try {
                    com.google.gson.reflect.TypeToken<List<ValidationOrchestrator.ValidationResult>> typeToken =
                            new com.google.gson.reflect.TypeToken<List<ValidationOrchestrator.ValidationResult>>() {
                            };
                    results = Constants.GSON.fromJson(storedData, typeToken.getType());
                    LOGGER.debug("getValidationResults() - Successfully parsed as direct ValidationResult list, size: {}", results.size());
                    return results;
                } catch (Exception directParseError) {
                    LOGGER.debug("Direct parse failed, trying wrapped format: {}", directParseError.getMessage());

                    // Try to parse as wrapped format (from GetChatIDHelper with VALIDATION_RESULTS key)
                    try {
                        com.google.gson.reflect.TypeToken<Map<String, List<ValidationOrchestrator.ValidationResult>>> wrappedTypeToken =
                                new com.google.gson.reflect.TypeToken<Map<String, List<ValidationOrchestrator.ValidationResult>>>() {
                                };
                        Map<String, List<ValidationOrchestrator.ValidationResult>> wrappedData = Constants.GSON.fromJson(storedData, wrappedTypeToken.getType());

                        if (wrappedData != null && wrappedData.containsKey(SubCatalogConstants.VALIDATION_RESULTS)) {
                            results = wrappedData.get(SubCatalogConstants.VALIDATION_RESULTS);
                            LOGGER.debug("getValidationResults() - Successfully parsed as wrapped format, size: {}", results.size());
                            return results;
                        }
                    } catch (Exception wrappedParseError) {
                        LOGGER.debug("Wrapped parse also failed: {}", wrappedParseError.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving validation results for stateId: {}, error: {}", stateId, e.getMessage());
        }

        LOGGER.debug("getValidationResults() retrieved {} validation results", results.size());
        return results;
    }
}

