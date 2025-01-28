### Release Notes - Azure AD Extension

## Version 2.0.5

- **Krista Service APIs Java** : 1.0.102
- **Global Catalog Version** : 1.0.53
- **Appliance Release Version** : 3.4.2

## Enhancement Highlights

#### Key Highlights of Version 2.0.5

* **Activate or Deactivate an Account** This request allows admin to activate or deactivate account.
* **Add an Account to Group** Allows admin to add user/account to respective group using group object id and user id or user mail. 
* **Remove an Account From Group** Allows admin to remove user/account from respective group using group object id and user id or user mail.
* **Get List of Groups** Used to Fetch List of Group Available.

## Note:
* Get All Groups request does not include or support **Security Groups** in its response.

## Resolved Bugs
* [KE-1901](https://antbrains.atlassian.net/browse/KE-1901) Azure AD: Invalid Error Message When Re-adding a User to a Group.
* [KE-1903](https://antbrains.atlassian.net/browse/KE-1903) Azure AD: Error is displayed when an invalid email ID is entered in the Reset Password request.
* [KE-1904](https://antbrains.atlassian.net/browse/KE-1904) Azure AD: Extension Logs Not Displayed in Diagnostics.
* [KE-1897](https://antbrains.atlassian.net/browse/KE-1897) Azure AD: The response does not indicate whether the account has been successfully deactivated or not.
* [KE-1899](https://antbrains.atlassian.net/browse/KE-1899) Azure AD: The response does not confirm if the account has been successfully added to the group.

## Known Issues
NA