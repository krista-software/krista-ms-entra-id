# Supported Requests

# Catalog Requests

Krista SharePoint extension supports the following Catalog Requests:

## Management Area

> #### Notes :
>  - Specify the SharePoint site name in the path.
>  - The path should start with /Sites/ListName and include the folder.
>  - In the File Entity Response, the absolute path retrieved includes the complete file path.
>  - If we include only Documents in the absolute path instead of Shared Documents or use the complete absolute path, it will still function.


#### 1. Copy

- **Description**: Copy the provide file represented by the entity to the target location and Replace file or create folder in path if selected Yes.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                              |
|--------------------|--------------------|---------------|------------------------------------------|
| Source             | Text               | Yes           | /siteName /Shared Documents/fileName     |
| Target             | Text               | Yes           | /siteName /Shared Documents/TargetFolder |
| Replace If Exists  | Boolean            | No            | True/False                               |
| Create Folder      | Boolean            | No            | True/False                               |

> #### Note :
>    - Copy the specified file on the provided target location.

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Message            | Text               | File has been copied successfully  |
| Success            | Yes/No             | Yes                                |

#### 2. Delete

- **Description**: Delete the given file from the cloud storage provider.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                               |
|--------------------|--------------------|---------------|-------------------------------------------|
| Target             | Text               | Yes           | /siteName /Shared Documents/fileName.txt  |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**               |
|--------------------|--------------------|---------------------------|
| Message            | Text               | File Deleted successfully |
| Success            | Yes/No             | Yes                       |

#### 3. Download as Link

- **Description**: Retrieves the file from the cloud storage and provides a reference as link.
- **Input Parameters**:

| **Parameter Name**      | **Parameter Type** | **Mandatory** | **Example**                               |
|-------------------------|--------------------|---------------|-------------------------------------------|
| Source                  | Text               | Yes           | /siteName /Shared Documents/fileName.txt  |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                          |
|--------------------|--------------------|--------------------------------------|
| Download Link      | Text               | {link}                               |
| Message            | Text               | To Download file Please Click on URL |
| Success            | Yes/No             | Yes                                  |

#### 4. Download as Krista File

- **Description**: Retrieves the file from the cloud storage and provides a Krista File.
- **Input Parameters**:

| **Parameter Name**      | **Parameter Type** | **Mandatory** | **Example**                               |
|-------------------------|--------------------|---------------|-------------------------------------------|
| Source                  | Text               | Yes           | /siteName /Shared Documents/fileName.txt  |

> #### Note :
> - Download as Krista File supports Files Up to 50 MB.

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**     |
|--------------------|--------------------|-----------------|
| Krista File        | Krista File        | File            |
| Message            | Text               | File Downloaded |
| Success            | Yes/No             | Yes             |

#### 5. Get Info

- **Description**: Accepts Source of File and return Task ID which will further used in Get Info Result .
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                        |
|--------------------|--------------------|---------------|------------------------------------|
| Source Path        | Text               | Yes           | /siteName /Shared Documents/folder |


> #### Note :
>  - Get Info Supports Only For SharePoint Folders

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

#### 6. Get Info Result

- **Description**: Accepts Task ID and Retrieves key information around a single folder in the cloud storage.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**              |
|--------------------|--------------------|---------------|--------------------------|
| Task ID            | Text               | Yes           | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>  - This **Task ID** is exclusively supported for Get Info catalog requests.

- **Output**:

| **Parameter Name**             | **Parameter Type** | **Example**              |
|--------------------------------|--------------------|--------------------------|
| Files                          | Number             | 5                        |
| Folders                        | Number             | 2                        |
| Size                           | Number             | 21324                    |
| Oldest File                    | Text               | SharePoint               |
| Oldest File Create Date        | Date               | 03/04/2023               |
| Newest File                    | Text               | SharePoint 2.0           |
| Newest File Date               | Date               | 03/04/2023               |
| Last Modified File Id          | Text               | bdcqwveb84yt4y43y84yt284 |
| Last Modified File Name        | Text               | Extension                |
| Last Modified File Create Date | Date               | 03/04/2023               |
| Message                        | Text               | File Information         |
| Success                        | Yes/No             | Yes                      |


#### 7. Get Result

- **Description**: Accept task ID as input and return List of Files.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**              |
|--------------------|--------------------|---------------|--------------------------|
| Task ID            | Text               | Yes           | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>  - This task ID is exclusively supported for List Files & List Folders catalog requests.

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                              |
|--------------------|--------------------|------------------------------------------|
| Files              | List&lt;Entity>    | [{name : "ABC", extension:".txt" , ...}] |
| Message            | Text               | File Information                         |
| Success            | Yes/No             | Yes                                      |


#### 8. List Files

- **Description**: Produces a non-recursive list of files that is present within the folder denoted by the source path.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                        |
|--------------------|--------------------|---------------|------------------------------------|
| Source Path        | Text               | Yes           | /siteName /Shared Documents/folder |
| Search             | Text               | No            | Put.json                           |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

#### 9. List Folders

- **Description**: Produces a non-recursive list of folders that is present within the folder denoted by the source path.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                        |
|--------------------|--------------------|---------------|------------------------------------|
| Source Path        | Text               | Yes           | /siteName /Shared Documents/folder |
| Search             | Text               | No            | FolderName                         |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |


#### 10. Move

- **Description**: Move a file on the drive from source to target where both denote a full file path.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                     |
|--------------------|--------------------|---------------|-------------------------------------------------|
| Source             | Text               | Yes           | /siteName /Shared Documents/folder/fileName     |
| Target             | Text               | Yes           | /siteName /Shared Documents/folder/TargetFolder |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Message            | Text               | File Moved                         |
| Success            | Yes/No             | Yes                                |


#### 11. Read

- **Description**: Read the content of the file as plain text. If the file is too big or cannot be accessed this should be reflected in the success and message variables produced. Content in that case should be empty.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                 |
|--------------------|--------------------|---------------|---------------------------------------------|
| Source             | Text               | Yes           | /siteName /Shared Documents/folder/fileName |

> #### Note :
>   - Read File Supports Only For .txt File

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**  |
|--------------------|--------------------|--------------|
| Content            | Paragraph          | Text Content |
| Message            | Text               |              |
| Success            | Yes/No             | Yes          |


#### 12. Rename

- **Description**: Renames the file given at the Source File Path to the name Given.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                       |
|--------------------|--------------------|---------------|---------------------------------------------------|
| Source             | Text               | Yes           | /siteName /Shared Documents/folder/fileName.xlsx  |
| Target File Name   | Text               | Yes           | NewExcel.xlsx                                     |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| Message            | Text               |                                    |
| Success            | Yes/No             | Yes                                |

#### 13. Share File

- **Description**: Share the file denoted by source with the given users identified by their email address applying the provided Permissions.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type**   | **Mandatory** | **Example**                                       |
|--------------------|----------------------|---------------|---------------------------------------------------|
| Source             | Text                 | Yes           | /siteName /Shared Documents/folder/fileName       |
| Share              | List&lt;Multi field> | Yes           | [{Email : testmail@mail.com, Permissions : Read}] |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Message            | Text               | File has been shared successfully  |
| Success            | Yes/No             | Yes                                |


#### 14. Write

- **Description**: Upload the content of the file from the media server to the cloud storage at target path. If Target path does not include a file name, the file name of the uploaded file is use. Produced File Entity represent the file in the cloud storage.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                 |
|--------------------|--------------------|---------------|---------------------------------------------|
| Krista File        | File               | Yes           | Test.pdf                                    |
| Target Path        | Text               | Yes           | /siteName /Shared Documents/folder/fileName |

> #### Note :
>  - Upload File Supports file size up to 50 MB.

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Message            | Text               | File Uploaded Successfully         |
| Success            | Yes/No             | Yes                                |


#### 15. Write Text

- **Description**: Uploads the file content to the cloud storage at the given target location as a plain text file.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                     |
|--------------------|--------------------|---------------|-------------------------------------------------|
| File Content       | Text               | Yes           | Demo File Content                               |
| Target             | Text               | Yes           | /siteName /Shared Documents/folder/fileName.txt |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |
| Message            | Text               | File Uploaded Successfully             |
| Success            | Yes/No             | Yes                                    |


#### 16. Changes Since

- **Description**: This endpoint produces a list of recent updates within a specified folder since a given date. The updates include information about the path, file status (e.g., Created, Updated, Deleted), and can be filtered using an optional regex in the search variable.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                        |
|--------------------|--------------------|---------------|------------------------------------|
| Source Path        | Text               | Yes           | /siteName /Shared Documents/folder |
| Since              | Date               | Yes           | 2023-12-29 T03 \: 54 \: 26Z        |
| Search             | Text               | Yes           | WeeklyDemo.txt                     |

- **Observation**: It works for files having version history.

- **Output**:

| **Parameter Name** | **Parameter Type**   | **Example**                                 |
|--------------------|----------------------|---------------------------------------------|
| Result             | List&lt;Multi field> | [{Path: Text, Date: Date, Update: Created}] |
| Message            | Text                 | File Content                                |
| Success            | Yes/No               | Yes                                         |

> #### Note :
>   - Changes Since Supports For SharePoint Folders and File.


#### 17. When a File is Changed

- **Description**: Monitors the Cloud Storage for changes. When a change arrives and the affected files/folders match the source path and event type, the event will be triggered.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                        |
|--------------------|--------------------|---------------|------------------------------------|
| Source             | Text               | Yes           | /siteName /Shared Documents/folder |
| Event              | Pick One           | Yes           | {Created,Updated,Deleted}          |
| Search             | Text               | No            | ChildFolder                        |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Event              | Text               | Updated                            |

> #### Note :
>   - File Entity in response does not contain Permission and Shared With & it's for Update & Create.
>   - Initially, upon creating a subscription, the system provides notifications for updates. Subsequently, you will receive notifications for both new creations and updates.
>   - Searching with Contains ChildFolder implies seeking instances where the provided path includes any subfolders.

## SharePoint Area

#### 1. Fetch By File Size Range

- **Description**: Accepts File Size , Returns Task ID.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| From Size          | Number             | Yes           | 123         |
| To Size            | Number             | Yes           | 3423        |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>   - File Size should be specified in bytes for conducting size range searches.


#### 2. Fetch File By Modification Date Range

- **Description**: Accepts File Modification date , Returns Task ID.
- **Input Parameters**:

| **Parameter Name**      | **Parameter Type** | **Mandatory** | **Example**             |
|-------------------------|--------------------|---------------|-------------------------|
| Modification Date Range | DateRange          | Yes           | 01/01/2023 - 02/04/2023 |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

#### 3. Get Shared Permission

- **Description**: Retrieve a list of users who have been granted access to a specific file, including those with shared access and those who possess permission to interact with the file.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                      |
|--------------------|--------------------|---------------|----------------------------------|
| Source Path        | Text               | Yes           | /siteName /Shared Documents/file |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>   - The File Entity in the response of Get Shared Permission catalog offers information on permissions specific to SharePoint Groups. It's essential to note that this functionality is limited to SharePoint Groups and does not encompass Sites.

#### 4. Search By Mail

- **Description**: Searches for files in SharePoint based on the email addresses of the users who created and last modified the files.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**               |
|--------------------|--------------------|---------------|---------------------------|
| Email              | Text               | Yes           | abc@contoso.com           |
| User               | Pick one           | Yes           | Created By OR Modified By |

- **Output**:


| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>   - File Search operates on data constraints associated with SharePoint Sites and Groups, focusing on criteria related to either the Created By or Modified By email addresses.

#### 5. Search By Permission

- **Description**: Retrieve a list of files based on different permission levels such as read, write, and owner access.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                   |
|--------------------|--------------------|---------------|-------------------------------|
| Permission         | Pick one           | Yes           | Read / Write / Read and Write |

- **Output**:


| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task ID            | Text               | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>   - File Search based on permission roles has size limitations in its scope.

#### 6. Get Response

- **Description**: Accept task ID as input and return List of Files.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**              |
|--------------------|--------------------|---------------|--------------------------|
| Task ID            | Text               | Yes           | bdcqwveb84yt4y43y84yt284 |

> #### Note :
>   - This task ID is exclusively supported for Sharepoint Area catalog requests.

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                          |
|--------------------|--------------------|--------------------------------------|
| Files              | List&lt;Entity>    | [{name : ABC, extension:.txt , ...}] |
| Message            | Text               | File Information                     |
| Success            | Yes/No             | Yes                                  |


## Operation Area

#### 1. Create List

- **Description**: Creates a new list in the specified SharePoint site.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Site Name          | Text               | Yes           | Test 123    | 
| List Name          | Text               | Yes           | ABC         |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension: .txt, ...} |
| Message            | Text               | List created successfully          |
| Success            | Yes/No             | Yes                                |

#### 2. Delete List

- **Description**: Deletes a SharePoint list identified by its site name and list name.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Site Name          | Text               | Yes           | Test 123    |
| List Name          | Text               | Yes           | ABC         |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| Message            | Text               | ListName List deleted successfully |
| Success            | Yes/No             | Yes                                |

#### 3. Delete Group

- **Description**: Deletes a SharePoint Group with the specified name.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Group Name         | Text               | Yes           | Test 123    |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                |
|--------------------|--------------------|----------------------------|
| Message            | Text               | Group deleted successfully |
| Success            | Yes/No             | Yes                        |



#### 4. Update List

- **Description**: Updates an existing SharePoint list with the provided display name and description.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**          |
|--------------------|--------------------|---------------|----------------------|
| Site Name          | Text               | Yes           | Test 123             |
| List Name          | Text               | Yes           | ABC                  |
| Display Name       | Text               | Yes           | ABC 123              |
| Description        | Text               | Yes           | ABC 123 Description  |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Message            | Text               | List modified successfully         |
| Success            | Yes/No             | Yes                                |

#### 5. Update Group

- **Description**: Updates the display name of an existing SharePoint group.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Group Name         | Text               | Yes           | Test 123    |
| Updated Group Name | Text               | Yes           | Test 124    |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                        |
|--------------------|--------------------|------------------------------------|
| File               | Entity             | {name : ABC, extension:.txt , ...} |
| Message            | Text               | Group updated successfully         |
| Success            | Yes/No             | Yes                                |


## Entity Request

Krista SharePoint Extension supports the following Entity Request:

### File

> #### Note :
>   - File Entity Supports For SharePoint Files and Folders.

#### 1. Create Group

- **Description**: Create Group in SharePoint Site.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Site Name          | Text               | Yes           | Test 123    |
| Site E-Mail        | Text               | Yes           | Test123     |

> #### Note :
>   - In the Create Group function, which is implemented under the File Entity, we use the variable Name for the site name and Base Name for the Mail ID, allowing user input.
>   - The Create Group function in File Entity only supports SharePoint Groups and not individual sites, as group are also considered site.


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |


* Search By File
    * Description: Returns list of File Details.
    * Input Parameters:

| **Parameter Name** | **Parameter Type**                           | **Mandatory** | **Example**                     |
|--------------------|----------------------------------------------|---------------|---------------------------------|
| Name               | Text                                         | No            | Sample.xlsx                     |
| Base Name          | Text                                         | No            | Sample                          |
| Extension          | Text                                         | No            | xlsx                            |
| Absolute Path      | Text                                         | No            | /siteName/Shared Documents/file |
| Owner              | Text                                         | No            | John Doe                        |
| Created            | Date                                         | No            | 2023-12-29 T03 \: 43 \: 26Z     |
| Modified           | Date                                         | No            | 2023-12-29 T03 \: 54 \: 26Z     |
| Modified By        | Text                                         | No            | John Doe                        |

> #### Note :
>  - Files can also be searched by using relative path. For example, you can use this -- /siteName/list Name/file
>  - Entity Search Using Permission is not Supported.
>  - Response of Entity search does not support Permission and Shared With.
>  - Tags are not supported in SharePoint Files Extension.
>  - Parent Folder search are not supported in SharePoint Files Extension.
>  - Size search are not supported in SharePoint Files Extension & for same have added catalog.
>  - Is Folder search are not supported in SharePoint Files Extension.

* Output:

| **Parameter Name** | **Parameter Type** |
|--------------------|--------------------|
| File               | List&lt;Entity>    |



