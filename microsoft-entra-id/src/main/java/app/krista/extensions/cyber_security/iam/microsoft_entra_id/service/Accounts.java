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
