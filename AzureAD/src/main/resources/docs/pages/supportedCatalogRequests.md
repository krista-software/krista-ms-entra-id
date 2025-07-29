## Catalog Requests

### The Microsoft Entra ID Extension supports the following catalog requests

#### 1. Create Account

- **Description**: This request will help you in creating an account, and it returns the details of the newly created account.
- **Input Parameters**: In extra fields following fields are supported - Job Title, Given Name, Office Location, Surname.

| **Parameter Name** | **Parameter Type**                                                | **Mandatory** | **Example**                      |
|--------------------|-------------------------------------------------------------------|---------------|----------------------------------|
| Name               | Text                                                              | Yes           | user1                            |
| Email              | Text                                                              | Yes           | user1@kristasoft.info            |
| Mobile Number      | Text                                                              | Yes           | 7777696969                       |
| Extra Fields       | List of Multi-field[ key, value fields(Text) inside Multi-field ] | No            | key - Job Title, value - Manager |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| User Id            | Text               |
| Default Password   | Text               |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| Name | Empty or invalid format | Please enter a valid name for the new account (2-50 characters, letters, spaces, hyphens, apostrophes, and dots allowed). |
| Name | Invalid characters or length | The provided name is not valid for account creation. Please enter a valid name. |
| Email | Empty or invalid format | Please enter a valid email address for the new account that doesn't already exist in Microsoft Entra ID. |
| Email | Already exists in Microsoft Entra ID | The email address already exists in Microsoft Entra ID. Please enter a different email address. |
| Email | Invalid format | The provided email address is not in valid format. Please enter a valid email address. |
| Mobile Number | Empty or invalid format | Please enter a valid mobile number for the new account (10-15 digits, international format supported with + prefix). |
| Mobile Number | Invalid format | The provided mobile number is not valid for account creation. Please enter a valid mobile number. |

#### 2. Update Account

- **Description**: With this request you can update an account, and it returns Account entity of the updated account.
- **Input Parameters**: In extra fields following fields are supported - Job Title, Given Name, Office Location, Surname.

| **Parameter Name** | **Parameter Type**                                                | **Mandatory** | **Example**                      |
|--------------------|-------------------------------------------------------------------|---------------|----------------------------------|
| Name               | Text                                                              | Yes           | user1                            |
| Email              | Text                                                              | Yes           | user1@kristasoft.info            |
| Mobile Number      | Text                                                              | Yes           | 7777696969                       |
| Extra Fields       | List of Multi-field[ key, value fields(Text) inside Multi-field ] | No            | key - Job Title, value - Manager |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| Updated Entity     | Account Entity     |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| Name | Empty or invalid format | Please enter a valid name for account update (2-50 characters, letters, spaces, hyphens, apostrophes, and dots allowed). |
| Name | Invalid characters or length | The provided name is not valid for account update. Please enter a valid name. |
| Email | Empty or invalid format | Please enter a valid email address that exists in Microsoft Entra ID for account update. |
| Email | Not found in Microsoft Entra ID | Email not found in Microsoft Entra ID. |
| Mobile Number | Empty or invalid format | Please enter a valid mobile number for account update (10-15 digits, international format supported with + prefix). |
| Mobile Number | Invalid format | The provided mobile number is not valid for account update. Please enter a valid mobile number. |

#### 3. Delete Account

- **Description**: This request can delete an account by ID or UserPrincipalName, and it returns true or false according to the status of delete.
- **Input Parameters**:

| **Parameter Name**        | **Parameter Type**                                                | **Mandatory** | **Example**                                                   |
|---------------------------|-------------------------------------------------------------------|---------------|---------------------------------------------------------------|
| ID or User Principal Name | Text                                                              | Yes           | user1@kristasoft.info or 2b451f76-96d1-4f62-aef1-a056584bb791 |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| Deleted            | Switch             |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| Email | Empty or invalid format | Please enter a valid email address that exists in Microsoft Entra ID. |
| Email | Not found in Microsoft Entra ID | Email not found in Microsoft Entra ID. |
| Email | Invalid format | Invalid email format. Please enter a valid email address. |

#### 4. Reset Password

- **Description**: This request returns the one time login password for user after login through this password, User have to set a new password.
- **Input Parameters**:

| **Parameter Name**         | **Parameter Type**                                                | **Mandatory** | **Example**                                                    |
|----------------------------|-------------------------------------------------------------------|---------------|----------------------------------------------------------------|
| ID or User Principal Name  | Text                                                              | Yes           | user1@kristasoft.info or 2b451f76-96d1-4f62-aef1-a056584bb791  |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| Password           | Text               |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| ID or User Principal Name | Empty | Please enter a valid user ID or email address for password reset. |
| ID or User Principal Name | Invalid format | Invalid ID format. Please enter a valid email address or user ID. |
| ID or User Principal Name | Email not found in Microsoft Entra ID | Email address not found in Microsoft Entra ID or not eligible for password reset. |
| ID or User Principal Name | User ID not found | User ID not found in Microsoft Entra ID or not eligible for password reset. |

#### 5. Get User Details by ID or Email

- **Description**: This request returns the details of the User.
- **Input Parameters**:

| **Parameter Name**         | **Parameter Type**                                                | **Mandatory** | **Example**                                                      |
|----------------------------|-------------------------------------------------------------------|---------------|------------------------------------------------------------------|
| ID or User Principal Name  | Text                                                              | Yes           | user1@kristasoft.info or 2b451f76-96d1-4f62-aef1-a056584bb791    |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| Result             | Account Entity     |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| ID or User Principal Name | Empty | Please enter a valid user ID or email address that exists in Microsoft Entra ID. |
| ID or User Principal Name | Email not found | Please enter a valid Email that exists in Microsoft Entra ID. |
| ID or User Principal Name | User ID not found | Please enter a valid user ID that exists in Microsoft Entra ID. |

#### 6. Activate or Deactivate an Account

- **Description**: Allows User to activate or deactivate account, Select Yes to Deactivate.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**           |
|--------------------|--------------------|---------------|-----------------------|
| Deactivate         | Switch             | Yes           | Yes                   |
| User Mail or ID    | Text               | Yes           | user1@kristasoft.info |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| Account Status     | Switch             |
| Message            | Text               |

- **Error Handling Scenarios**:

| **Field** | **Validation Error**| **Error Message** |
|-----------|---------------------|-------------------|
| User Mail or ID | Empty | Please enter a valid user email address or user ID that exists in Microsoft Entra ID. |
| User Mail or ID | Email not found | Please enter a valid email address that exists in Microsoft Entra ID. |
| User Mail or ID | User ID not found | Please enter a valid user ID that exists in Microsoft Entra ID. |
| User Mail or ID | Not found in Microsoft Entra ID | Email not found in Microsoft Entra ID or User ID not found in Microsoft Entra ID. |

#### 7. Add Group to Account

- **Description**: Allows User to Add Group to Account, requires User Id or User Mail and Group Object Id.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                          |
|--------------------|--------------------|---------------|--------------------------------------|
| Group Object Id    | Text               | Yes           | 0e831f0a-b85f-4971-80b5-34f2324b2598 |
| User Mail or ID    | Text               | Yes           | user1@kristasoft.info                |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type**                                                                                                                                 |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Account Info       | { User Mail Id: user1@kristasoft.info, User Name: user1, Group List: [ { Group Mail Id: group1, Group Id: 0e831f0a-b85f-4971-80b5-34f2324b2598 } ] |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| User Mail or ID | Empty | Please enter a valid user email address or user ID to add to the group. |
| User Mail or ID | Email not found | Please enter a valid email address that exists in Microsoft Entra ID to add to the group. |
| User Mail or ID | User ID not found | Please enter a valid user ID that exists in Microsoft Entra ID to add to the group. |
| User Mail or ID | Not found in Microsoft Entra ID | Email address not found in Microsoft Entra ID or not eligible for group addition, or User ID not found in Microsoft Entra ID or not eligible for group addition. |
| Group Object Id | Empty | Group Object Id is null or empty for user addition. |
| Group Object Id | Invalid format | The provided Group Object Id is not valid. Please enter a valid Group Object Id. |

#### 8. Remove Group from Account

- **Description**: Allows User to Remove Group to Account, Required User Mail or ID and Group Object Id.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                          |
|--------------------|--------------------|---------------|--------------------------------------|
| Group Object Id    | Text               | Yes           | 0e831f0a-b85f-4971-80b5-34f2324b2598 |
| User Mail or ID    | Text               | Yes           | user1@kristasoft.info                |

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| Removed            | Switch             |

- **Error Handling Scenarios**:

| **Field** | **Validation Error** | **Error Message** |
|-----------|---------------------|-------------------|
| User Mail or ID | Empty | Please enter a valid user email address or user ID for group removal. |
| User Mail or ID | Email not found | Please enter a valid email address that exists in Microsoft Entra ID for group removal. |
| User Mail or ID | User ID not found | Please enter a valid user ID that exists in Microsoft Entra ID for group removal. |
| User Mail or ID | Not found in Microsoft Entra ID | Email address not found in Microsoft Entra ID or not eligible for group removal, or User ID not found in Microsoft Entra ID or not eligible for group removal. |
| Group Object Id | Empty | Group Object Id is null or empty for user removal. |
| Group Object Id | Invalid format | The provided Group Object Id is not valid. Please enter a valid Group Object Id. |

#### 9. Get List of Groups

- **Description**: Allows Admin to Fetch List of Group.

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** | **Example**                                              |
|--------------------|--------------------|----------------------------------------------------------|
| Group List         | MultiField         | [{group1@kristasoft.info, dhcbw84gbbv29g9292bbvw}, ....] |

**Note**: This request does not have error handling implemented as it does not require input validation.


## Entity Requests

### The Microsoft Entra ID Extension supports the following entity requests

### 1. Account

#### Search

- **Description**: Returns list of Accounts, and you can search by Name, Email, Mobile Number.
- **Input Parameters**: NA
- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------| 
| Account            | List<Account>      | 

