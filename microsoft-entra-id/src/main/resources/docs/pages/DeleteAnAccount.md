# Delete an Account

## Overview

The **Delete an Account** catalog request permanently removes a user account from Microsoft Entra ID. This is an irreversible operation.

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
| Status | Text | Deletion status message |

## Usage Examples

```
User Mail or Id: "former.employee@company.com"
Allow Retry: true
```

## Business Rules

1. **Permanent Deletion**: Cannot be undone
2. **30-Day Recovery**: Deleted users can be restored within 30 days
3. **License Release**: Licenses are automatically released

## Related Catalog Requests

- [Create an Account](pages/CreateAnAccount.md)
- [Activate or Deactivate an Account](pages/ActivateOrDeactivateAnAccount.md)

## See Also

- [Microsoft Graph Delete User API](https://learn.microsoft.com/en-us/graph/api/user-delete)
