# Create an Account

## Overview

The **Create an Account** catalog request creates a new user account in Microsoft Entra ID with the specified profile information. This operation generates a temporary password that the user must change on first login.

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Account Manager |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes (via Allow Retry parameter) |

## Input Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| Name | Text | Yes | Full name of the user (Display Name) | `John Smith` |
| Email | Text | Yes | User's email address (User Principal Name) | `john.smith@company.com` |
| Mobile Number | Text | Yes | User's mobile phone number | `+1-555-123-4567` |
| Extra Fields | List of Key-Value Pairs | No | Additional user attributes | `[{"key": "jobTitle", "value": "Manager"}]` |
| Allow Retry | Boolean | No | Enable error handling with user prompts | `true` |

## Output Parameters

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| User Id | Text | Microsoft Entra ID object ID of created user | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| Default Password | Text | Temporary password (user must change on first login) | `Temp@Pass123!` |

## Validation Rules

| Validation | Error Message | Resolution |
|------------|---------------|------------|
| Name is required | "Name is required" | Provide a valid display name |
| Email format | "Invalid email format" | Use valid email format (user@domain.com) |
| Email already exists | "Email already exists" | Use a different email address |
| Mobile number format | "Invalid mobile number" | Use valid phone format (+1-555-123-4567) |

## Error Handling

### INPUT_ERROR
- Invalid email format
- Missing required fields
- Email already exists in Microsoft Entra ID

### LOGIC_ERROR
- User creation validation failed
- Extra fields contain invalid attributes

### SYSTEM_ERROR
- Microsoft Graph API unavailable
- Network connectivity issues
- Service timeout

### Authorization Errors
- Insufficient permissions (requires User.ReadWrite.All)
- Token expired (automatic retry with refresh token)

## Usage Examples

### Example 1: Create Basic User Account
```
Name: "Jane Doe"
Email: "jane.doe@company.com"
Mobile Number: "+1-555-987-6543"
Extra Fields: []
Allow Retry: true

Output:
User Id: "b2c3d4e5-f6a7-8901-bcde-f23456789012"
Default Password: "Xk9#mP2qL@5n"
```

### Example 2: Create User with Job Title
```
Name: "Bob Johnson"
Email: "bob.johnson@company.com"
Mobile Number: "+1-555-111-2222"
Extra Fields: [{"key": "jobTitle", "value": "Senior Developer"}]
Allow Retry: true

Output:
User Id: "c3d4e5f6-a7b8-9012-cdef-345678901234"
Default Password: "Yz8$nQ3rM@6p"
```

## Business Rules

1. **Email Uniqueness**: Email must not already exist in Microsoft Entra ID
2. **Temporary Password**: System generates secure temporary password
3. **Force Password Change**: User must change password on first login
4. **Account Enabled**: New accounts are enabled by default
5. **License Assignment**: Licenses must be assigned separately

## Limitations

- Cannot create accounts with custom domains not verified in tenant
- Maximum 50,000 users per tenant (varies by license)
- Email must be unique across the tenant
- Cannot set permanent password during creation

## Best Practices

1. **Use Retry Mode**: Enable "Allow Retry" for better error handling
2. **Validate Email**: Ensure email doesn't exist before creation
3. **Secure Password Delivery**: Transmit temporary password securely
4. **Notify User**: Send welcome email with temporary password
5. **Monitor Creation**: Track successful/failed account creations

## Common Use Cases

1. **Employee Onboarding**: Automatically create accounts for new hires
2. **Bulk User Import**: Create multiple accounts from CSV/database
3. **Self-Service Registration**: Allow users to request account creation
4. **Partner Access**: Create external user accounts for partners

## Related Catalog Requests

- [Update an Account](pages/UpdateAnAccount.md) - Modify user information
- [Delete an Account](pages/DeleteAnAccount.md) - Remove user accounts
- [Activate or Deactivate an Account](pages/ActivateOrDeactivateAnAccount.md) - Enable/disable accounts
- [Reset Password](pages/ResetPassword.md) - Generate new temporary password

## Technical Implementation

**Helper Class**: `CreateAccountHelper`
- Handles validation and error handling when retry is enabled
- Validates email format and uniqueness
- Validates name and mobile number format

**Telemetry Metrics**:
- Operation: `createAccount`
- Tracks success/failure rates
- Records operation duration

## Troubleshooting

### "Email already exists" Error
**Cause**: Email is already registered in Microsoft Entra ID
**Resolution**: Use a different email or update existing account

### "Invalid email format" Error
**Cause**: Email doesn't match required format
**Resolution**: Use format: username@domain.com

### "Insufficient privileges" Error
**Cause**: Missing User.ReadWrite.All permission
**Resolution**: Grant required API permissions and admin consent

### Account Created but Password Not Returned
**Cause**: API response parsing issue
**Resolution**: Check logs, password is generated but may not be captured

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup guide
- [Authentication](pages/Authentication.md) - OAuth 2.0 details
- [Microsoft Graph Create User API](https://learn.microsoft.com/en-us/graph/api/user-post-users) - Official API documentation
