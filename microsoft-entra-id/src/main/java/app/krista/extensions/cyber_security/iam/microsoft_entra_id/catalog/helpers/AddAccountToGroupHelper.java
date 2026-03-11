/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.helpers;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.MicrosoftEntraIDResources;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util.SubCatalogConstants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers.ExtensionResponseGenerator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.errorhandlers.ErrorHandlingStateManager;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.Validator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.AddAccountUserMailOrIdValidator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.validators.AddAccountGroupObjectIdValidator;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;

import org.jvnet.hk2.annotations.Service;

import java.util.*;

/**
 * Helper class for Add Account To Group operations with validation support.
 */
@Service
public class AddAccountToGroupHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAccountToGroupHelper.class);

    private final AccountImpl accountService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public AddAccountToGroupHelper(AccountImpl accountService,
                                   ValidationOrchestrator validationOrchestrator,
                                   ErrorHandlingStateManager internalStateManager,
                                   ExtensionResponseGenerator responseGenerator) {
        this.accountService = accountService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("AddAccountToGroupHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register User Mail or Id validator for add account to group
            AddAccountUserMailOrIdValidator userMailOrIdValidator = new AddAccountUserMailOrIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.USER_MAIL_OR_ID_ADD_GROUP, userMailOrIdValidator);
            LOGGER.info("Registered AddAccountUserMailOrIdValidator for USER_MAIL_OR_ID_ADD_GROUP");

            // Register Group Object Id validator for add account to group
            AddAccountGroupObjectIdValidator groupObjectIdValidator = new AddAccountGroupObjectIdValidator(accountService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.GROUP_OBJECT_ID_ADD_GROUP, groupObjectIdValidator);
            LOGGER.info("Registered AddAccountGroupObjectIdValidator for GROUP_OBJECT_ID_ADD_GROUP");

            LOGGER.info("AddAccountToGroupHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse addAccountToGroupWithValidation(String userMailOrId, String groupObjectId) {
        try {
            LOGGER.info("AddAccountToGroupHelper.addAccountToGroupWithValidation() called with userMailOrId: {}, groupObjectId: {}", userMailOrId, groupObjectId);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.USER_MAIL_OR_ID_ADD_GROUP, userMailOrId != null ? userMailOrId : "",
                            Validator.ValidationResource.GROUP_OBJECT_ID_ADD_GROUP, groupObjectId != null ? groupObjectId : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, userMailOrId, groupObjectId);
            }

            // If validation passes, proceed with adding the account to group
            LOGGER.debug("Validation passed, adding account to group");
            Map<String, Object> result = accountService.addAccountToGroup(userMailOrId, groupObjectId);

            LOGGER.info("Successfully added account to group for userMailOrId: {}, groupObjectId: {}", userMailOrId, groupObjectId);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Account Info", result))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while adding account to group: {}", cause.getMessage(), cause);
            return new ExtensionResponseBuilder()
                    .failure(new ExtensionResponse.Error("Error occurred while adding account to group",
                            System.currentTimeMillis(), ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, ""))
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String userMailOrId, String groupObjectId) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("Validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        Map<String, Object> stateMap = Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
        internalStateManager.put(stateId, Constants.GSON.toJson(stateMap));
        LOGGER.debug("Validation failed, generating confirmation response");

        // Include all fields in the confirmation response to preserve valid values
        Map<String, Object> map = new HashMap<>();
        map.put(SubCatalogConstants.STATE_ID, stateId);

        // Always include both fields to preserve valid values during re-entry flow
        map.put(MicrosoftEntraIDResources.USER_MAIL_OR_ID_ADD_GROUP, userMailOrId != null ? userMailOrId : "");
        map.put(MicrosoftEntraIDResources.GROUP_OBJECT_ID_ADD_GROUP, groupObjectId != null ? groupObjectId : "");

        LOGGER.debug("Validation failed, generated confirmation response map: {}", map);

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_ADD_ACCOUNT_TO_GROUP_VALIDATION,
                map);
    }

}
