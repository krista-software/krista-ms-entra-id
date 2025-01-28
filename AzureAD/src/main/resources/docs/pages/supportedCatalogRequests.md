## Catalog Requests

### The Azure AD Extension supports the following catalog requests

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

#### 9. Get List of Groups

- **Description**: Allows Admin to Fetch List of Group.

- **Output Parameters**:

| **Parameter Name** | **Parameter Type** | **Example**                                              |
|--------------------|--------------------|----------------------------------------------------------|
| Group List         | MultiField         | [{group1@kristasoft.info, dhcbw84gbbv29g9292bbvw}, ....] |


## Entity Requests

### The Azure AD Extension supports the following entity requests

### 1. Account

#### Search

- **Description**: Returns list of Accounts, and you can search by Name, Email, Mobile Number.
- **Input Parameters**: NA
- **Output Parameters**:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------| 
| Account            | List<Account>      | 

