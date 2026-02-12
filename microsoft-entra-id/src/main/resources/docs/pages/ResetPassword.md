# Reset Password

## Overview

The **Reset Password** catalog request generates a new temporary password for a user account in Microsoft Entra ID. The user must change this password on their next login.

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Account Manager |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes |

## Input Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| User Mail or Id | Text | Yes | User's email or object ID | `user@company.com` |
| Allow Retry | Boolean | No | Enable error handling | `true` |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| Temporary Password | Text | New temporary password (user must change on next login) |

## Usage Examples

```
User Mail or Id: "john.smith@company.com"
Allow Retry: true

Output:
Temporary Password: "Xk9#mP2qL@5n"
```

## Business Rules

1. **Temporary Password**: System generates secure random password
2. **Force Change**: User must change password on next login
3. **Immediate Effect**: Takes effect immediately
4. **Secure Delivery**: Transmit password securely to user

## Common Use Cases

1. **Forgotten Password**: User forgot their password
2. **Account Recovery**: Help user regain access
3. **Security Reset**: Force password change for security reasons
4. **New Employee**: Reset default password from account creation

## Related Catalog Requests

- [Create an Account](pages/CreateAnAccount.md) - Also generates temporary password
- [Update an Account](pages/UpdateAnAccount.md)
- [Activate or Deactivate an Account](pages/ActivateOrDeactivateAnAccount.md)

## See Also

- [Microsoft Graph Reset Password API](https://learn.microsoft.com/en-us/graph/api/user-update)
- [Authentication](pages/Authentication.md) - Required UserAuthenticationMethod.ReadWrite.All scope

