package app.krista.extensions.cyber_security.iam.azureAD.impl;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.azureAD.impl.util.Constants;
import app.krista.extensions.cyber_security.iam.azureAD.service.Accounts;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.AuthenticationMethodResetPasswordParameterSet;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.DirectoryObjectCollectionReferenceRequestBuilder;
import com.microsoft.graph.requests.DirectoryObjectReferenceRequestBuilder;
import com.microsoft.graph.requests.UserCollectionPage;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public Account activateOrDeactivateAccount(String userMailOrID, Boolean deactivate) {
        try {
            User user = new User();
            user.accountEnabled = !deactivate; //Deactivate Input is True, hence we are setting it as False
            log.info("User Account Deactivating Initialised: {}", user.accountEnabled);

            provider.getGraphServiceClient().users(userMailOrID).buildRequest().patch(user);
            log.info("User Account Deactivating Completed");

            return getUser(userMailOrID);
        } catch (ClientException cause) {
            log.error("Failed to Deactivate User: {}", cause.getMessage());
            throw new ClientException("Failed to Deactivate User, Please provide a valid email or a valid ID", cause);
        }
    }

    @Override
    public Account addAccountToGroup(String userMailOrId, String groupId) {
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

            return getUser(userMailOrId);
        } catch (ClientException cause) {
            log.error("Failed to add an Account to Group: {}", cause.getMessage());
            throw new ClientException("Failed to add an Account to Group, Please provide a valid email, valid userId or valid group objectId ", cause);
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
            throw new ClientException("Failed to Remove an Account from Group, Please provide a valid email or a valid Id", cause);
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
