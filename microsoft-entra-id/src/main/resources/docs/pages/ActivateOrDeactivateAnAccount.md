# Activate or Deactivate an Account

## Overview

The **Activate or Deactivate an Account** catalog request enables or disables a user account in Microsoft Entra ID without deleting it.

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
| Account Status | Pick One | Yes | Enable or Disable | `Disable` |
| Allow Retry | Boolean | No | Enable error handling | `true` |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| Status | Text | Operation status message |

## Usage Examples

### Disable Account
```
User Mail or Id: "temp.contractor@company.com"
Account Status: "Disable"
Allow Retry: true
```

### Enable Account
```
User Mail or Id: "returning.employee@company.com"
Account Status: "Enable"
Allow Retry: true
```

## Business Rules

1. **Reversible**: Can enable/disable multiple times
2. **Immediate Effect**: Takes effect immediately
3. **Preserves Data**: All user data retained

## Related Catalog Requests

- [Delete an Account](pages/DeleteAnAccount.md)
- [Create an Account](pages/CreateAnAccount.md)

## See Also

- [Microsoft Graph Update User API](https://learn.microsoft.com/en-us/graph/api/user-update)
