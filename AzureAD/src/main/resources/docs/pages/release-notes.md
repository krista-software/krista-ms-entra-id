### Release Notes - Microsoft Entra ID Extension

## Version 2.0.6

- **Developer Name** : Samruddhi Pisal
- **Krista Service APIs Java** : 1.0.115
- **Global Catalog Version** : 1.0.58
- **Appliance Release Version** : 3.5.0

## Enhancement Highlights

#### Key Highlights of Version 2.0.6

* **Activate or Deactivate an Account** This request allows admin to activate or deactivate account.
* **Add an Account to Group** Allows admin to add user/account to respective group using group object id and user id or user mail.
* **Remove an Account From Group** Allows admin to remove user/account from respective group using group object id and user id or user mail.
* **Get List of Groups** Used to Fetch List of Group Available.

## Note:
* Get All Groups request does not include or support **Security Groups** in its response.

## Resolved Bugs
* [KE-1901](https://antbrains.atlassian.net/browse/KE-1901) Microsoft Entra ID: Invalid Error Message When Re-adding a User to a Group.
* [KE-1903](https://antbrains.atlassian.net/browse/KE-1903) Microsoft Entra ID: Error is displayed when an invalid email ID is entered in the Reset Password request.
* [KE-1904](https://antbrains.atlassian.net/browse/KE-1904) Microsoft Entra ID: Extension Logs Not Displayed in Diagnostics.
* [KE-1897](https://antbrains.atlassian.net/browse/KE-1897) Microsoft Entra ID: The response does not indicate whether the account has been successfully deactivated or not.
* [KE-1899](https://antbrains.atlassian.net/browse/KE-1899) Microsoft Entra ID: The response does not confirm if the account has been successfully added to the group.

## UI/UX Improvements
* **Authentication UI Cleanup**: Removed unused "Allow Mail Alert" option from authentication tab
* **Simplified Authentication Flow**: Streamlined authentication process by removing unnecessary fields
* **Backend Cleanup**: Removed allowMailAlert field from AzureADAttributes class and related components
* **UI Build Cleanup**: Rebuilt and cleaned UI build files to remove legacy allowMailAlert references
* **Authentication Tab Fix**: Fixed authentication tab not displaying by updating JavaScript file references and ensuring all UI assets are properly deployed

## Known Issues
NA