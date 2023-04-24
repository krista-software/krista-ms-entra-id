package app.krista.extensions.cyber_security.iam.azureAD.impl;

import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.cyber_security.iam.azureAD.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.cyber_security.iam.azureAD.service.Accounts;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.AuthenticationMethodResetPasswordParameterSet;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.UserCollectionPage;
import org.jvnet.hk2.annotations.Service;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import javax.inject.Inject;
import java.util.*;

@Service
public class AccountImpl implements Accounts {
    public static final String AUTHENTICATION_METHOD_ID = "28c10230-6103-485e-b985-444c60001490";
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
        System.out.println(passwordProfile.password);
        passwordProfile.forceChangePasswordNextSignIn = true;
        return passwordProfile;
    }

    public Map<String, Object> createUser(String name, String email, String mobileNumber, List<Map<String, Object>> extraFields) {
        User user = new User();
        user.displayName = name;
        user.accountEnabled = true;
        user.userPrincipalName = email;
        user.mobilePhone = mobileNumber;
        String mailNickname = name.split(" ")[0];
        user.mailNickname = mailNickname;
        if (extraFields != null) {
            updateExtraFields(user, extraFields);
        }
        // Generate and set password profile
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.password = generateRandomPassword();
        System.out.println("here is the pass - "+passwordProfile.password);
        passwordProfile.forceChangePasswordNextSignIn = true;
        user.passwordProfile = passwordProfile;

        // Create user using GraphServiceClient
        User createdUser = provider.getGraphServiceClient().users().buildRequest().post(user);

        // Prepare and return result map
        Map<String, Object> result = new HashMap<>();
        result.put("User Id", createdUser.id);
        result.put("Default Password", user.passwordProfile.password);
        return result;
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
            User updatedUser = provider.getGraphServiceClient().users(email).buildRequest().get();
            updatedUser.mail = email;
            return Account.from(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Account getUser(String id) {
        User userDetails = provider.getGraphServiceClient().users(id).buildRequest().get();
        return Account.from(userDetails);
    }

    public String resetPassword(String id) {
        String randomPassword = generateRandomPassword();
        try {
            provider.getGraphServiceClient().users(id).authentication()
                    .methods(AUTHENTICATION_METHOD_ID)
                    .resetPassword(AuthenticationMethodResetPasswordParameterSet
                            .newBuilder()
                            .withNewPassword(randomPassword)
                            .build())
                    .buildRequest()
                    .post();
        } catch (ClientException exception) {
            throw new IllegalStateException(exception.getMessage(), exception.getCause());
        }
        return randomPassword;
    }

    private String generateRandomPassword() {
        // Define the allowed characters for each position in the password
        String allowedCharsAtPosition1 = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String allowedCharsAtPosition2To8 = "0123456789";
        String allowedCharsAtPosition9To12 = "abcdefghijklmnopqrstuvwxyz0123456789";

        // Generate the password by randomly selecting a character from each allowed character set
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
        return provider.getGraphServiceClient().users().buildRequest().get();
    }
}
