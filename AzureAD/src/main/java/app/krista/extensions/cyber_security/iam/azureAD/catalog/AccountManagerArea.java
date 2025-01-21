package app.krista.extensions.cyber_security.iam.azureAD.catalog;


import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.impl.anno.Attribute;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            id = "localDomainRequest_e42ee430-a2ea-47ff-afcf-00d8ab303e5b",
            name = "Activate or Deactivate an Account",
            description = "Required User ID or User Mail to Activate or Deactivate an Account",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Account Status", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Map<String, Object> activateOrDeactivateAnAccount(
            @Field.Boolean(name = "Deactivate", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) Boolean deactivate,
            @Field.Text(name = "User Mail or ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String userMailOrID){
        try {
            boolean accountStatus = account.activateOrDeactivateAccount(userMailOrID, deactivate);
            String statusMessage =  deactivate ? "Deactivated" : "Activated";
            return Map.of(
                    "Account Status", accountStatus,
                    "Message" , "Account "+statusMessage+ " Successfully");
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e){
            logger.error("Error Occurred during Activate or Deactivate an Account : {}", e.getMessage(), e);
            return Map.of(
                    "Account Status", false,
                    "Message" , e.getMessage());
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_9945dea1-a854-4871-ad09-85f335bea91a",
            name = "Get List of Groups",
            description = "Get List of Groups",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Group List", type = "[ { Group Mail Id: Text, Group Id: Text } ]",required = false)
    public List<Map<String, String>> getListOfGroups(){
        try {
            return account.getAllGroups();
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e){
            logger.error("Error Occurred during Get List of Groups : {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_30716250-b09b-4706-b565-61c113715a31",
            name = "Add an Account to Group",
            description = "Requires User Id or User Mail and Group Object Id",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Account Info", type = "{ User Mail Id: Text, User Name: Text, Group List: [ Text ], User Id: Text }",required = false)
    public Map<String, Object> addAnAccountToGroup(
            @Field.Text(name = "User Mail or Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String userMailOrId,
            @Field.Text(name = "Group Object Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupObjectId) {
        try {
            return account.addAccountToGroup(userMailOrId, groupObjectId);
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e) {
            logger.error("Error Occurred during Add an Account to Group : {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_d29d95b8-315f-438b-988d-6e70722fe889",
            name = "Remove an Account From Group",
            description = "Required User Mail or ID and Group Object Id",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Removed", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean removeGroupFromAccount(
            @Field.Text(name = "User Mail or Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String userMailOrId,
            @Field.Text(name = "Group Object Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupObjectId) {
        try {
            return account.removeAccountFromGroup(userMailOrId, groupObjectId);
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e) {
            logger.error("Error Occurred during Add an Account to Group : {}", e.getMessage(), e);
            return false;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_1079ad63-576a-4aca-9897-230c2ed51f87",
            name = "Create an Account",
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
        try {
            return account.createUser(name, email, mobileNumber, extraFields);
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e){
            logger.error("Error Occurred during Create an Account : {}", e.getMessage(), e);
            return Map.of("User Id", "", "Default Password", "");
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_05582946-848b-466c-bc41-75f14413dedf",
            name = "Update an Account",
            description = "Update an Account in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Updated Entity", type = "Entity(Account)", required = false)
    public app.krista.extensions.cyber_security.iam.azureAD.catalog.entities.Account updateAccount(
            @Field(name = "Name", type = "Text", required = false, attributes = {}, options = {}) String name,
            @Field(name = "Mobile Number", type = "Text", required = false, attributes = {}, options = {}) String mobileNumber,
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email,
            @Field.Desc(name = "Extra Field", type = "[ { key: Text, value: Text } ]", required = false) List<Map<String, Object>> extraField) {
        try {
            return account.updateUser(
                    Objects.requireNonNullElse(name,""), Objects.requireNonNullElse(mobileNumber,""), email, extraField);
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e){
            logger.error("Error Occurred during Update an Account : {}", e.getMessage(), e);
            return new Account();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_a8cf8efe-56f8-40a7-be91-da8aa945195f",
            name = "Delete an Account",
            description = "Delete an Account in Azure Active Directory",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Deleted", type = "Switch", required = false, attributes = {}, options = {})
    public Boolean deleteAccount(
            @Field(name = "Email", type = "Text", required = true, attributes = {}, options = {}) String email) {
        try {
            return account.deleteUser(email);
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e){
            logger.error("Error Occurred during Delete an Account : {}", e.getMessage(), e);
            return false;
        }
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
        try {
            return account.getUser(id);
        } catch (MustAuthorizeException authorizeException) {
            throw authorizeException;
        } catch (Exception e){
            logger.error("Error Occurred during Get User Details by ID or Email : {}", e.getMessage(), e);
            return new Account();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_57ac8a99-063f-4391-bd6b-25b52f94137e",
            name = "Reset Password",
            description = "Reset Account Password",
            area = "Account Manager",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field(name = "Password", type = "Text", required = false, attributes = {}, options = {})
    public String resetPassword(
            @Field(name = "Id", type = "Text", required = true, attributes = {}, options = {}) String id) {
        int maxAttempts = 3;
        int attempts = 0;
        int backoffTime = 3000; // initial backoff time in milliseconds

        while (attempts < maxAttempts) {
            try {
                return account.resetPassword(id);
            } catch (ClientException cause) {
                Throwable rootCause = cause.getCause();
                if (rootCause instanceof GraphServiceException) {
                    GraphServiceException graphEx = (GraphServiceException) rootCause;
                    int statusCode = graphEx.getResponseCode();

                    if (statusCode == 403) {
                        attempts++;
                        if (attempts < maxAttempts) {
                            try {
                                Thread.sleep(backoffTime);
                                backoffTime *= 2; // exponential backoff
                            } catch (InterruptedException interruptedException) {
                                Thread.currentThread().interrupt();
                                logger.error("Thread interrupted during sleep", interruptedException);
                                throw new RuntimeException("Thread interrupted during sleep", interruptedException);
                            }
                        } else {
                            logger.error("Max attempts reached. Error occurred while resetting password", cause);
                            throw new RuntimeException("Max attempts reached. Error occurred while resetting password", cause);
                        }
                    } else {
                        logger.error("Error occurred while resetting password", cause);
                        throw new RuntimeException("Error occurred while resetting password", cause);
                    }
                } else {
                    logger.error("Unexpected error occurred while resetting password", cause);
                    throw new RuntimeException("Unexpected error occurred while resetting password", cause);
                }
            }
        }
        return null;
    }
}