### Release Notes - Microsoft Entra ID Extension

## Version 2.0.6

- **Developer Name** : Samruddhi Pisal
- **Krista Service APIs Java** : 1.0.115
- **Global Catalog Version** : 1.0.58
- **Appliance Release Version** : 3.5.0

## BREAKING CHANGES for UI/UX 

**Extension Name Change: Azure AD → Microsoft Entra ID**

With this version change, we have removed all references to "Azure AD" and changed it to "Microsoft Entra ID". Once you upgrade to this version, you will need to:

1. **Re-authenticate** the extension with your Microsoft Entra ID credentials
2. **Update callback URLs** in your Azure portal application registration
3. **Verify authentication configuration** to ensure proper connectivity

This change aligns with Microsoft's official rebranding of Azure Active Directory to Microsoft Entra ID.

## Enhancement Highlights

#### Key Highlights of Version 2.0.6

* **Activate or Deactivate an Account** This request allows admin to activate or deactivate account.
* **Add an Account to Group** Allows admin to add user/account to respective group using group object id and user id or user mail.
* **Remove an Account From Group** Allows admin to remove user/account from respective group using group object id and user id or user mail.
* **Get List of Groups** Used to Fetch List of Group Available.

## Note:
* Get All Groups request does not include or support **Security Groups** in its response.

## Resolved Bugs
NA

## Known Issues
NA