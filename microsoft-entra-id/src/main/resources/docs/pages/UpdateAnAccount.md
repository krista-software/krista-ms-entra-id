# Update an Account

## Overview

The **Update an Account** catalog request modifies an existing user account in Microsoft Entra ID. You can update the user's name, mobile number, and other profile attributes.

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Account Manager |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes (via Allow Retry parameter) |

## Input Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| Email | Text | Yes | User's email to identify the account | `john.smith@company.com` |
| Name | Text | No | Updated display name | `John A. Smith` |
| Mobile Number | Text | No | Updated mobile phone number | `+1-555-999-8888` |
| Extra Field | List of Key-Value Pairs | No | Additional attributes to update | `[{"key": "jobTitle", "value": "Senior Manager"}]` |
| Allow Retry | Boolean | No | Enable error handling with user prompts | `true` |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| Updated Entity | Entity(Account) | Complete updated user account object |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Email is required | "Email is required" | Provide user's email address |
| Email format | "Invalid email format" | Use valid email format |
| User not found | "User does not exist" | Verify email address is correct |
| Mobile number format | "Invalid mobile number" | Use valid phone format |

## Error Handling

### INPUT_ERROR
- Invalid email format
- User not found
- Invalid mobile number format

### LOGIC_ERROR
- Update validation failed
- Extra fields contain invalid attributes

### SYSTEM_ERROR
- Microsoft Graph API unavailable
- Network connectivity issues

### Authorization Errors
- Insufficient permissions (requires User.ReadWrite.All)
- Token expired (automatic retry)

## Usage Examples

### Example 1: Update User Name
```
Email: "jane.doe@company.com"
Name: "Jane Marie Doe"
Mobile Number: null
Extra Field: []
Allow Retry: true
```

### Example 2: Update Mobile Number and Job Title
```
Email: "bob.johnson@company.com"
Name: null
Mobile Number: "+1-555-222-3333"
Extra Field: [{"key": "jobTitle", "value": "Director of Engineering"}]
Allow Retry: true
```

## Business Rules

1. **Email Immutable**: Cannot change user's email/UPN through this request
2. **Partial Updates**: Only provided fields are updated
3. **Null Values**: Null parameters are ignored (not cleared)
4. **Account Must Exist**: User must exist in Microsoft Entra ID

## Limitations

- Cannot update email/User Principal Name
- Cannot update password (use Reset Password instead)
- Cannot update account enabled/disabled status (use Activate/Deactivate instead)
- Some attributes require additional permissions

## Best Practices

1. **Use Retry Mode**: Enable "Allow Retry" for validation
2. **Verify User Exists**: Check user exists before updating
3. **Update Only Changed Fields**: Don't send unchanged values
4. **Audit Changes**: Log all account modifications

## Common Use Cases

1. **Profile Updates**: Update user information when details change
2. **Job Changes**: Update job title, department when employee changes roles
3. **Contact Updates**: Update phone numbers and contact information
4. **Bulk Updates**: Update multiple user attributes from external system

## Related Catalog Requests

- [Create an Account](pages/CreateAnAccount.md) - Create new users
- [Delete an Account](pages/DeleteAnAccount.md) - Remove users
- [Get User Details by ID or Email](pages/GetUserDetailsByIDOrEmail.md) - Retrieve user info
- [Reset Password](pages/ResetPassword.md) - Change user password

## Technical Implementation

**Helper Class**: `UpdateAccountHelper`
- Validates email and mobile number format
- Handles retry logic when enabled

**Telemetry Metrics**:
- Operation: `updateAccount`
- Tracks update success/failure rates

## Troubleshooting

### "User does not exist" Error
**Cause**: Email not found in Microsoft Entra ID
**Resolution**: Verify email address is correct

### "Invalid mobile number" Error
**Cause**: Phone number format invalid
**Resolution**: Use international format (+1-555-123-4567)

### Update Succeeds but Changes Not Visible
**Cause**: Caching or replication delay
**Resolution**: Wait a few seconds and refresh

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md)
- [Authentication](pages/Authentication.md)
- [Microsoft Graph Update User API](https://learn.microsoft.com/en-us/graph/api/user-update)
