# Add an Account to Group

## Overview

The **Add an Account to Group** catalog request adds a user to a security or distribution group in Microsoft Entra ID.

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
| Group Object Id | Text | Yes | Group's object ID | `abc123-def456...` |
| Allow Retry | Boolean | No | Enable error handling | `true` |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| Status | Text | Operation status message |

## Usage Examples

```
User Mail or Id: "john.smith@company.com"
Group Object Id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Allow Retry: true
```

## Business Rules

1. **User Must Exist**: User must be in Microsoft Entra ID
2. **Group Must Exist**: Group must be valid
3. **Idempotent**: Adding existing member succeeds

## Related Catalog Requests

- [Remove an Account From Group](pages/RemoveAnAccountFromGroup.md)
- [Get List of Groups](pages/GetListOfGroups.md)

## See Also

- [Microsoft Graph Add Group Member API](https://learn.microsoft.com/en-us/graph/api/group-post-members)
