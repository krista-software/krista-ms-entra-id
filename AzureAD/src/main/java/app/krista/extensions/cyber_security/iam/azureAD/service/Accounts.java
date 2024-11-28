package app.krista.extensions.cyber_security.iam.azureAD.service;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;

import java.util.List;
import java.util.Map;

/**
 * This is outlook user account
 */
public interface Accounts {

    Boolean deleteUser(String email);

    Map<String, Object> createUser(String name, String email, String mobileNumber, List<Map<String, Object>> extraFields);

    Account updateUser(String name, String mobileNumber, String email, List<Map<String, Object>> extraField);

    Account getUser(String id);

    String resetPassword(String id);

    Account activateOrDeactivateAccount(String userMailOrID, Boolean deactivate);

    Account addGroupToAccount(String userMailOrId, String groupMail);

    Boolean removeGroupFromAccount(String userMailOrId, String groupMail);
}
