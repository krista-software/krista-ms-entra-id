# Get User Details by ID or Email

## Overview

The **Get User Details by ID or Email** catalog request retrieves detailed information about a user account from Microsoft Entra ID.

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
| User Details | Entity(Account) | Complete user account object with all attributes |

## Usage Examples

### Example 1: Get User by Email
```
User Mail or Id: "john.smith@company.com"
Allow Retry: true

Output:
User Details: {
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "displayName": "John Smith",
  "mail": "john.smith@company.com",
  "mobilePhone": "+1-555-123-4567",
  "accountEnabled": true
}
```

### Example 2: Get User by Object ID
```
User Mail or Id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Allow Retry: true
```

## Business Rules

1. **Flexible Lookup**: Accepts email or object ID
2. **Complete Profile**: Returns all available user attributes
3. **User Must Exist**: Returns error if user not found

## Related Catalog Requests

- [Create an Account](pages/CreateAnAccount.md)
- [Update an Account](pages/UpdateAnAccount.md)
- [Delete an Account](pages/DeleteAnAccount.md)

## See Also

- [Microsoft Graph Get User API](https://learn.microsoft.com/en-us/graph/api/user-get)

