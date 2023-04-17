package app.krista.extensions.cyber_security.iam.azureAD.catalog.entities;

import app.krista.extension.impl.anno.*;
import com.microsoft.graph.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Searchable
@Domain(id = "catEntryDomain_5fd12694-66b1-4cd2-8141-c41f02716c3a",
        name = "IAM",
        ecosystemId = "catEntryEcosystem_5516afe8-3809-4918-b911-d89597fc96e0",
        ecosystemName = "Cyber Security",
        ecosystemVersion = "168c3ea6-2104-40a9-9d9a-b235128dfa91")
@Entity(name = "Account", id = "localDomainEntity_e8c2f0ae-6db2-4361-8404-0b866554e218", primaryKey = "Id", options = {})
public class Account {

    @Searchable
    @ToString
    @Field(name = "Name", type = "Text")
    public String name;

    @Searchable
    @ToString
    @Field(name = "Email", type = "Text")
    public String email;

    @Field(name = "Id", type = "Text", required = false)
    public String id;

    @Searchable
    @ToString
    @Field(name = "Mobile Number", type = "Text")
    public String mobileNumber;

    @Field.Desc(name = "Extra Fields", type = "[ { key: Text, value: Text } ]", required = false)
    public List<Map<String, Object>> extraFields;


    public static Account from(User user) {
        Account updatedAccount = new Account();
        List<Map<String, Object>> extraFields1 = new ArrayList<>();
        if (user != null) {
            updatedAccount.name = user.displayName;
            updatedAccount.email = user.mail;
            updatedAccount.mobileNumber = user.mobilePhone;
            updatedAccount.id = user.id;
            if (user.givenName != null) {
                extraFields1.add(Map.of("key", "Given Name", "value", user.givenName));
            }
            if (user.jobTitle != null) {
                extraFields1.add(Map.of("key", "Job Title", "value", user.jobTitle));
            }
            if (user.officeLocation != null) {
                extraFields1.add(Map.of("Office Location", user.officeLocation));
            }
            if (user.surname != null) {
                extraFields1.add(Map.of("key", "Surname", "value", user.surname));
            }
            if (user.userPrincipalName != null) {
                extraFields1.add(Map.of("key", "User Principal Name", "value", user.userPrincipalName));
            }
        }
        updatedAccount.extraFields = extraFields1;
        return updatedAccount;
    }

}