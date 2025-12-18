### Release Notes - Microsoft Entra ID Extension

## Version 3.0.3

- **Developer Name** : Avadh Dobaria
- **Krista Service APIs Java** : 1.0.115
- **Global Catalog Version** : GC-2025.12.3

### New Features

#### Per-Request Allow Retry Option (KE-2800)

Added an optional `Allow Retry` field to each catalog request with a default value of OFF/False. This prevents automated workflows from getting stuck in a "waiting state" when validation fails.

**Affected Catalog Requests:**
- Activate or Deactivate an Account
- Add an Account to Group
- Remove an Account From Group
- Create an Account
- Update an Account
- Delete an Account
- Get User Details by ID or Email
- Reset Password

**Behavior:**
- **Allow Retry = OFF (default)**: Direct service call without validation prompts. No waiting state for automated workflows.
- **Allow Retry = ON**: Uses validation helpers that can prompt for re-entry if validation fails.

---

## Version 3.0.2

- **Developer Name** : Samruddhi Pisal
- **Krista Service APIs Java** : 1.0.115
- **Global Catalog Version** : GC-2025.09.3
- **Appliance Release Version** : 3.5.1

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