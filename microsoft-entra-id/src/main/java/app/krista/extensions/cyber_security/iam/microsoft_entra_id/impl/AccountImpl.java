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

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl;

import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.util.Constants;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.service.Accounts;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.*;

@Service
public class AccountImpl implements Accounts {
    private static final Logger log = LoggerFactory.getLogger(AccountImpl.class);
    private final GraphServiceClientProvider provider;

    @Inject
    public AccountImpl(GraphServiceClientProviderFactory factory) {
        this(factory.create());
    }

    public AccountImpl(GraphServiceClientProvider provider) {
        this.provider = provider;
    }

    public PasswordProfile getPasswordProfile() {
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.password = generateRandomPassword();
        passwordProfile.forceChangePasswordNextSignIn = true;
        return passwordProfile;
    }

    public Map<String, Object> createUser(String name, String email, String mobileNumber, List<Map<String, Object>> extraFields) {
        try {
            User user = new User();
            user.displayName = name;
            user.accountEnabled = true;
            user.userPrincipalName = email;
            user.mail = email;
            user.mobilePhone = mobileNumber;
            user.mailNickname = name.split(" ")[0];
            if (extraFields != null) {
                updateExtraFields(user, extraFields);
            }
            user.passwordProfile = getPasswordProfile();
            User createdUser = provider.getGraphServiceClient().users().buildRequest().post(user);
            Map<String, Object> result = new HashMap<>();
            result.put("User Id", createdUser.id);
            result.put("Default Password", Objects.requireNonNull(user.passwordProfile).password);
            return result;
        } catch (ClientException cause) {
            log.error("Failed to create user: {}", cause.getMessage(), cause);
            throw new ClientException("Failed to create user: Please provide a valid Email", cause);
        }
    }


    public Account updateUser(String name, String mobileNumber, String email, List<Map<String, Object>> extraFields) {
        User user = new User();
        if (!name.isBlank()) {
            user.displayName = name;
        }
        if (!mobileNumber.isBlank()) {
            user.mobilePhone = mobileNumber;
        }
        if (extraFields != null) {
            updateExtraFields(user, extraFields);
        }
        try {
            provider.getGraphServiceClient().users(email).buildRequest().patch(user);
            return getUser(email);
        } catch (ClientException cause) {
            log.error("Failed to update user: {}", cause.getMessage(), cause);
            throw new ClientException("Failed to update user: Please provide a valid email or a valid ID", cause);
        }
    }


    private void updateExtraFields(User user, List<Map<String, Object>> extraFields) {
        for (Map<String, Object> extraField : extraFields) {
            String key = (String) extraField.get("key");
            Object value = extraField.get("value");
            // Update fields based on the key
            if ("Given Name".equalsIgnoreCase(key)) {
                user.givenName = (String) value;
            } else if ("Job Title".equalsIgnoreCase(key)) {
                user.jobTitle = (String) value;
            } else if ("mail".equalsIgnoreCase(key)) {
                user.mail = (String) value;
            } else if ("Office Location".equalsIgnoreCase(key)) {
                user.officeLocation = (String) value;
            } else if ("Surname".equalsIgnoreCase(key)) {
                user.surname = (String) value;
            }
        }
    }

    public Boolean deleteUser(String email) {
        try {
            provider.getGraphServiceClient().users(email).buildRequest().delete();
            return true;
        } catch (ClientException cause) {
            log.error("Failed to delete user: {}", cause.getMessage());
            throw new ClientException("Failed to delete user: Please provide a valid email or a valid ID", cause);
        }
    }


    @Override
    public Boolean activateOrDeactivateAccount(String userMailOrID, Boolean deactivate) {
        try {
            User user = new User();
            user.accountEnabled = !deactivate; //Deactivate Input is True, hence we are setting it as False
            log.info("User Account Deactivating Initialised: {}", user.accountEnabled);

            provider.getGraphServiceClient().users(userMailOrID).buildRequest().patch(user);
            log.info("User Account Activation/Deactivation Completed");
            return !deactivate;

        } catch (ClientException cause) {
            log.error("Failed to Deactivate User: {}", cause.getMessage());
            throw new ClientException("Failed to Deactivate User. Please provide a valid email address or a valid ID.", cause);
        }
    }

    @Override
    public Map<String, Object> addAccountToGroup(String userMailOrId, String groupId) {
        try {

            DirectoryObject member = new DirectoryObject();
            member.id = getUser(userMailOrId).id;
            log.info("User Id  assigned to Member : {}", member.id);

            DirectoryObjectCollectionReferenceRequestBuilder directoryObjectCollectionReferenceRequestBuilder
                    = provider.getGraphServiceClient().groups(groupId).members().references();

            if (directoryObjectCollectionReferenceRequestBuilder != null) {
                directoryObjectCollectionReferenceRequestBuilder.buildRequest().post(member);
                log.info("User added to Group");
            } else {
                log.info("Group references are Null or Empty");
            }

            return getUserForGroups(userMailOrId);
        } catch (ClientException cause) {
            log.error("Failed to add an Account to Group: {}", cause.getMessage());
            throw new ClientException("Failed to add an Account to the Group. Please provide a valid email address, a valid user ID, or a valid group object ID.", cause);
        }
    }

    @Override
    public Boolean removeAccountFromGroup(String userMailOrId, String groupId) {
        try {
            String userId = getUser(userMailOrId).id;
            log.info("User Id fetched: {}", userId);
            DirectoryObjectReferenceRequestBuilder objectReferenceRequestBuilder
                    = provider.getGraphServiceClient().groups(groupId).members(userId).reference();
            if (objectReferenceRequestBuilder != null) {
                objectReferenceRequestBuilder.buildRequest().delete();
                log.info("User Removed from Group");
                return true;
            } else {
                log.info("Group reference is Null or Empty");
                return false;
            }

        } catch (ClientException cause) {
            log.error("Failed to Remove an Account from Group: {}", cause.getMessage());
            throw new ClientException("Failed to remove an Account from Group. Please provide a valid email address, a valid user ID, or a valid group object ID.", cause);
        }
    }


    public Account getUser(String id) {
        try {
            User userDetails = provider.getGraphServiceClient().users(id).buildRequest().get();
            return Account.from(userDetails);
        } catch (ClientException cause) {
            throw new ClientException("Invalid user Id" + id, cause);
        }
    }

    public Map<String, Object> getUserForGroups(String id) {
        try {
            User userDetails = getUserDetails(id);
            if (userDetails != null) {
                List<Map<String, String>> userGroups = getUserGroups(id);
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("User Mail Id", userDetails.mail);
                map.put("User Name", userDetails.displayName);
                map.put("Group List", Objects.requireNonNullElse(userGroups, List.of()));
                map.put("User Id", userDetails.id);
                return map;
            }

            return Collections.emptyMap();
        } catch (ClientException cause) {
            throw new ClientException("Invalid user Id" + id, cause);
        }
    }


    @Nullable
    private User getUserDetails(String id) {
        return provider.getGraphServiceClient().users(id).buildRequest().get();
    }

    @Nullable
    public List<Map<String, String>> getUserGroups(String userId) {
        List<Map<String, String>> userGroupName = new ArrayList<>();
        try {
            DirectoryObjectCollectionWithReferencesPage directoryObjCollWithRefPage
                    = provider.getGraphServiceClient()
                    .users(userId)
                    .memberOf()
                    .buildRequest()
                    .get();

            List<DirectoryObject> memberOf = (directoryObjCollWithRefPage != null) ? directoryObjCollWithRefPage.getCurrentPage() : null;

            if (memberOf != null) {
                for (DirectoryObject directoryObject : memberOf) {
                    if (directoryObject instanceof Group) {
                        Group group = (Group) directoryObject;
                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("Group Mail Id", Objects.requireNonNullElse(group.mail, "NA"));
                        map.put("Group Id", Objects.requireNonNullElse(group.id, "NA"));
                        userGroupName.add(map);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error occurred while fetching list of group for userId : {}", e.getMessage(), e);
        }
        return userGroupName;
    }

    @Override
    public List<Map<String, String>> getAllGroups() {
        List<Map<String, String>> allGroups = new ArrayList<>();
        try {
            // Start with the initial request for groups
            GroupCollectionPage groupCollectionPage = provider.getGraphServiceClient()
                    .groups()
                    .buildRequest()
                    .get();

            // Loop through pages of results
            while (groupCollectionPage != null) {
                // Add current page's group mail IDs to the list
                List<Group> currentPage = groupCollectionPage.getCurrentPage();

                if (currentPage != null) {
                    for (Group group : currentPage) {
                        if (group.mail != null) {// Only add if mail is not null
                            allGroups.add(getGroupMap(group.mail, group.id));
                        }
                    }
                }

                // Get the next page, if available
                GroupCollectionRequestBuilder nextPage = groupCollectionPage.getNextPage();
                groupCollectionPage = nextPage != null ? nextPage.buildRequest().get() : null;
            }
        } catch (Exception e) {
            log.error("Error occurred while fetching all groups {}", e.getMessage(), e);
        }
        return allGroups;
    }

    private Map<String, String> getGroupMap(String mail, String id) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Group Mail Id", mail);
        map.put("Group Id", id);
        return map;
    }


    public String resetPassword(String id) {
        String randomPassword = generateRandomPassword();
        try {
            provider.getGraphServiceClient().users(id).authentication().methods(Constants.AUTHENTICATION_METHOD_ID).resetPassword(AuthenticationMethodResetPasswordParameterSet.newBuilder().withNewPassword(randomPassword).build()).buildRequest().post();
        } catch (ClientException cause) {
            log.error("Failed to reset the password for user: {}", cause.getMessage(), cause);
            throw new ClientException("Failed to reset the password for user: Please provide a valid Email or a valid ID", cause);
        }
        return randomPassword;
    }

    private String generateRandomPassword() {
        String allowedCharsAtPosition1 = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String allowedCharsAtPosition2To8 = "0123456789";
        String allowedCharsAtPosition9To12 = "abcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder passwordBuilder = new StringBuilder();
        passwordBuilder.append(getRandomCharFromString(allowedCharsAtPosition1));
        for (int i = 2; i <= 8; i++) {
            passwordBuilder.append(getRandomCharFromString(allowedCharsAtPosition2To8));
        }
        for (int i = 9; i <= 12; i++) {
            passwordBuilder.append(getRandomCharFromString(allowedCharsAtPosition9To12));
        }

        return passwordBuilder.toString();
    }

    private char getRandomCharFromString(String string) {
        int randomIndex = (int) (Math.random() * string.length());
        return string.charAt(randomIndex);
    }

    public UserCollectionPage getUserCollectionPage() {
        try {
            return provider.getGraphServiceClient().users().buildRequest().get();
        } catch (ClientException cause) {
            throw new ClientException("Failed to get the users ", cause);

        }
    }
}
