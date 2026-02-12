# Microsoft Entra ID Extension

## Overview

The Microsoft Entra ID Extension enables seamless integration with Microsoft's cloud-based identity and access management service (formerly Azure Active Directory). This extension provides comprehensive user account management capabilities, allowing you to create, update, delete, and manage user accounts and group memberships directly from your Krista workflows.

**Version**: 3.0.3
**Domain**: Cyber Security / IAM (Identity and Access Management)
**Area**: Account Manager

## Key Features

✅ **Complete User Lifecycle Management**
- Create new user accounts with customizable profiles
- Update existing user information and attributes
- Delete user accounts when needed
- Activate or deactivate user accounts

✅ **Group Management**
- Add users to security and distribution groups
- Remove users from groups
- Retrieve list of all available groups

✅ **User Information Retrieval**
- Get detailed user information by ID or email
- Search and query user accounts

✅ **Security Operations**
- Reset user passwords with temporary credentials
- Force password change on next login

✅ **Flexible Authentication**
- Service Account mode for automated workflows
- User Account mode for individual user context
- OAuth 2.0 with secure token management

✅ **Retry Mechanism**
- Configurable error handling with retry support
- Input validation with user-friendly prompts

## Quick Start Guide

### 1. Prerequisites
- Microsoft Entra ID tenant with administrative access
- Microsoft Entra ID app registration (see [Creating Microsoft Entra ID App](pages/CreatingMicrosoftEntraIDApp.md))
- Required OAuth 2.0 scopes configured

### 2. Configure the Extension
Follow the [Extension Configuration](pages/ExtensionConfiguration.md) guide to set up:
- Client ID, Client Secret, and Tenant ID
- Administrator email for service account
- Authentication type (Service Account or User Account)
- Redirect URI configuration

### 3. Authenticate
Complete the OAuth 2.0 authentication flow as described in [Authentication](pages/Authentication.md)

### 4. Start Using Catalog Requests
Explore the available catalog requests to manage your Microsoft Entra ID users and groups

## Documentation Structure

### 📋 Getting Started
- [Extension Configuration](pages/ExtensionConfiguration.md) - Complete setup guide
- [Authentication](pages/Authentication.md) - OAuth 2.0 authentication details
- [Creating Microsoft Entra ID App](pages/CreatingMicrosoftEntraIDApp.md) - App registration guide

### 👤 Account Management
- [Create an Account](pages/CreateAnAccount.md) - Create new user accounts
- [Update an Account](pages/UpdateAnAccount.md) - Modify existing user information
- [Delete an Account](pages/DeleteAnAccount.md) - Remove user accounts
- [Activate or Deactivate an Account](pages/ActivateOrDeactivateAnAccount.md) - Enable/disable accounts
- [Get User Details by ID or Email](pages/GetUserDetailsByIDOrEmail.md) - Retrieve user information
- [Reset Password](pages/ResetPassword.md) - Generate temporary passwords

### 👥 Group Management
- [Get List of Groups](pages/GetListOfGroups.md) - Retrieve all available groups
- [Add an Account to Group](pages/AddAnAccountToGroup.md) - Add users to groups
- [Remove an Account From Group](pages/RemoveAnAccountFromGroup.md) - Remove users from groups

### 📚 Additional Resources
- [Release Notes](pages/release-notes.md) - Version history and updates

## Support & Resources

- **Extension Version**: 3.0.3
- **API**: Microsoft Graph API
- **OAuth 2.0**: Authorization Code Grant with Refresh Tokens
- **Required Scopes**:
  - `openid` - Basic authentication
  - `offline_access` - Refresh token support
  - `User.ReadWrite.All` - User management operations
  - `UserAuthenticationMethod.ReadWrite.All` - Password reset operations

## Common Use Cases

1. **Automated User Onboarding**: Create new user accounts automatically when employees join
2. **User Offboarding**: Deactivate or delete accounts when employees leave
3. **Group-Based Access Control**: Manage group memberships for role-based access
4. **Password Reset Workflows**: Automate password reset requests
5. **User Information Sync**: Keep user data synchronized across systems
6. **Bulk User Operations**: Process multiple user account changes efficiently

## Security Considerations

- All credentials are securely stored and encrypted
- OAuth 2.0 tokens are automatically refreshed
- Client secrets should be rotated regularly
- Use Service Account mode for automated workflows
- Use User Account mode when user context is required
- Monitor authentication logs for unusual activity

---

**Ready to get started?** Begin with [Extension Configuration](pages/ExtensionConfiguration.md) to set up your Microsoft Entra ID connection.
