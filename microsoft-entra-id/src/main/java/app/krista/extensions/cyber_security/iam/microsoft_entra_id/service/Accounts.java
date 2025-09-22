package app.krista.extensions.cyber_security.iam.microsoft_entra_id.service;

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account;

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

    Boolean activateOrDeactivateAccount(String userMailOrID, Boolean deactivate);

    Map<String ,Object> addAccountToGroup(String userMailOrId, String groupMail);

    Boolean removeAccountFromGroup(String userMailOrId, String groupMail);

    List<Map<String, String>> getAllGroups();
}
