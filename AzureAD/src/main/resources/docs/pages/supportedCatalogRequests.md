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

|                    |                    |                   |
| **Parameter Name** | **Parameter Type** | **Example**       |
|--------------------|--------------------|-------------------|
| User Id            | Text               | 2bhjsdcnlks-ksmdc |
| Default Password   | Text               | wamhatever        |

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

## Entity Requests

### The Azure AD Extension supports the following entity requests

#### Search Account

- **Description**: Returns list of labels, and you can search by Name, Email, Mobile Number or ID.
- **Input Parameters**: NA
- **Output Parameters**:

| **Parameter Name** | **Parameter Type** | 
|--------------------|--------------------|
| Account            | List<Account>      | 

#### Get Account

- **Description**: Selects Account from searched result, and it will return the Account Details.
- **Input Parameters**: NA
- **Output Parameters**: NA
