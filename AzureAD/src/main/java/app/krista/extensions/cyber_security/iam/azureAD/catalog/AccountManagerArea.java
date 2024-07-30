package app.krista.extensions.cyber_security.iam.azureAD.catalog;


import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.azureAD.impl.AccountImpl;
import app.krista.extensions.cyber_security.iam.azureAD.service.Accounts;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Domain(id = "catEntryDomain_5fd12694-66b1-4cd2-8141-c41f02716c3a",
        name = "IAM",
        ecosystemId = "catEntryEcosystem_5516afe8-3809-4918-b911-d89597fc96e0",
        ecosystemName = "Cyber Security",
        ecosystemVersion = "3cf343a9-3df2-4036-9e0f-7b84930b8ef7")
public
class AccountManagerArea {

    private final Accounts account;
    private static final Logger logger = LoggerFactory.getLogger(AccountManagerArea.class);

    @Inject
    public AccountManagerArea(AccountImpl account) {
        this.account = account;
    }

    @CatalogRequest(
            id = "localDomainRequest_1079ad63-576a-4aca-9897-230c2ed51f87",
            name = "CreateAccount",
            description = "Create User in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "User Id", type = "Text", required = false, attributes = {}, options = {})
    @Field(name = "Default Password", type = "Text", required = false, attributes = {}, options = {})
    public Map<String, Object> createAccount(
            @Field(name = "Name", type = "Text", required = true, attributes = {}, options = {}) String name,
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email,
            @Field(name = "Mobile Number", type = "Text", required = true, attributes = {}, options = {}) String mobileNumber,
            @Field.Desc(name = "Extra Fields", type = "[ { key: Text, value: Text } ]", required = false) List<Map<String, Object>> extraFields) {
        return account.createUser(name, email, mobileNumber, extraFields);
    }

    @CatalogRequest(
            id = "localDomainRequest_05582946-848b-466c-bc41-75f14413dedf",
            name = "UpdateAccount",
            description = "Update Account in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Updated Entity", type = "Entity(Account)", required = false)
    public app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account updateAccount(
            @Field(name = "Name", type = "Text", required = false, attributes = {}, options = {}) String name,
            @Field(name = "Mobile Number", type = "Text", required = false, attributes = {}, options = {}) String mobileNumber,
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email,
            @Field.Desc(name = "Extra Field", type = "[ { key: Text, value: Text } ]", required = false) List<Map<String, Object>> extraField) {
        return account.updateUser(name, mobileNumber, email, extraField);
    }

    @CatalogRequest(
            id = "localDomainRequest_a8cf8efe-56f8-40a7-be91-da8aa945195f",
            name = "DeleteAccount",
            description = "Delete Account in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Deleted", type = "Switch", required = false, attributes = {}, options = {})
    public Boolean deleteAccount(
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email) {
        return account.deleteUser(email);

    }

    @CatalogRequest(
            id = "localDomainRequest_720edeff-9823-4597-b208-921835e1d960",
            name = "Get User Details by ID or Email",
            description = "Get User Details by ID or Email",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Result", type = "Entity(Account)", required = false)
    public Account getUserDetailsByIDOrEmail(
            @Field(name = "Id", type = "Text", required = true, attributes = {}, options = {}) String id) {
        return account.getUser(id);
    }

    @CatalogRequest(
            id = "localDomainRequest_57ac8a99-063f-4391-bd6b-25b52f94137e",
            name = "Reset Password",
            description = "Reset Account Password",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Password", type = "Text", required = false, attributes = {}, options = {})
    public String resetPassword(
            @Field(name = "Id", type = "Text", required = true, attributes = {}, options = {}) String id) throws InterruptedException {
        try {
            return account.resetPassword(id);
        } catch (ClientException cause) {
            int statusCode = ((GraphServiceException) cause.getCause()).getResponseCode();
            if (statusCode == 403) {
                Thread.sleep(3000);
                return account.resetPassword(id);
            } else {
                throw new RuntimeException("Error occurred while reset password", cause);
            }
        }

    }

}