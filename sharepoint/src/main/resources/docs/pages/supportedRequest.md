# Supported Requests

## Catalog Requests

Krista SharePoint extension supports the following Catalog Requests:

### Management Area

> ** Note :
>  - Specify the SharePoint site name in the path.
>  - The path should start with "Shared Documents" and include the folder.


#### Copy

- **Description**: Copy the provide file represented by the entity to the target location and Replace file or create folder in path if selected 'Yes' .
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                 |
|--------------------|--------------------|---------------|-----------------------------------------------------------------------------|
| Source             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/fileName     |
| Target             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/TargetFolder |
| Replace If Exists  | Boolean            | No            | True/False                                                                  |
| Create Folder      | Boolean            | No            | True/False                                                                  |

> ** Note :
> - Copy the specified file on specified location

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**   |
|--------------------|--------------------|---------------|
| File               | File               | Test.xlsx     |
| Message            | Text               | "File Copied" |
| Success            | Yes/No             | Yes           |

#### Delete

- **Description**: Delete the given file from the cloud storage provider.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                 |
|--------------------|--------------------|---------------|-----------------------------------------------------------------------------|
| Target             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/fileName.txt |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**    |
|--------------------|--------------------|----------------|
| Message            | Text               | "File Deleted" |
| Success            | Yes/No             | Yes            |

#### Download

- **Description**: Retrieves the file from the cloud storage and provides a reference as link, Krista File or both based on provided inputs.
- **Input Parameters**:

| **Parameter Name**      | **Parameter Type** | **Mandatory** | **Example**                                                                 |
|-------------------------|--------------------|---------------|-----------------------------------------------------------------------------|
| Download As Link        | Yes/No             | No            | No                                                                          |
| Download as Krista File | Yes/No             | No            | Yes                                                                         |
| Source                  | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/fileName.txt |

> ** Note :
> - Download as Krista File supports Files Unto 50 MB. 

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**       |
|--------------------|--------------------|-------------------|
| Download Link      | Text               | {link}            |
| Krista File        | Krista File        | File              |
| Message            | Text               | "File Downloaded" |
| Success            | Yes/No             | Yes               |

#### Get Info

- **Description**: Accepts Source of File and return Task ID which will further used in Get Info Result .
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                           |
|--------------------|--------------------|---------------|-----------------------------------------------------------------------|
| Source Path        | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder |


> ** Note :
>  - Get Info Supports Only For SharePoint Folders

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task UD            | Text               | bdcqwveb84yt4y43y84yt284 |

#### Get Info Result

- **Description**: Accepts Task ID and Retrieves key information around a single folder in the cloud storage.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**              |
|--------------------|--------------------|---------------|--------------------------|
| Task ID            | Text               | Yes           | bdcqwveb84yt4y43y84yt284 |

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
| Message                        | Text               | "File Information"       |
| Success                        | Yes/No             | Yes                      |


#### Get Result

- **Description**: Accept task ID as input and return List of Files.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**              |
|--------------------|--------------------|---------------|--------------------------|
| Task ID            | Text               | Yes           | bdcqwveb84yt4y43y84yt284 |

- **Output**:

| **Parameter Name**             | **Parameter Type** | **Example**                              |
|--------------------------------|--------------------|------------------------------------------|
| Files                          | List<File Entity>  | [{name : "ABC", extension:".txt" , ...}] |
| Message                        | Text               | "File Information"                       |
| Success                        | Yes/No             | Yes                                      |


#### List Files

- **Description**: Produces a non-recursive list of files that is present within the folder denoted by the source path.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                           |
|--------------------|--------------------|---------------|-----------------------------------------------------------------------|
| Source Path        | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder |
| Search             | Text               | No            | "Put.json"                                                            |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task UD            | Text               | bdcqwveb84yt4y43y84yt284 |

#### List Folders

- **Description**: Produces a non-recursive list of folders that is present within the folder denoted by the source path.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                           |
|--------------------|--------------------|---------------|-----------------------------------------------------------------------|
| Source Path        | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder |
| Search             | Text               | No            | FolderName                                                            |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**              |
|--------------------|--------------------|--------------------------|
| Task UD            | Text               | bdcqwveb84yt4y43y84yt284 |


#### Move

- **Description**: Move a file on the drive from source to target where both denote a full file path.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                        |
|--------------------|--------------------|---------------|------------------------------------------------------------------------------------|
| Source             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/fileName     |
| Target             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/TargetFolder |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |
| Message            | Text               | "File Moved"                           |
| Success            | Yes/No             | Yes                                    |


#### Read

- **Description**: Read the content of the file as plain text. If the file is too big or cannot be accessed this should be reflected in the success and message variables produced. Content in that case should be empty.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                    |
|--------------------|--------------------|---------------|--------------------------------------------------------------------------------|
| Source             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/fileName |

> ** Note :
> - Read File Supports Only For .txt File

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |
| Message            | Text               | " "                                    |
| Success            | Yes/No             | Yes                                    |


#### Rename

- **Description**: Renames the file given at the Source File Path to the name Given.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory**                          | **Example**              |
|--------------------|--------------------|----------------------------------------|--------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} | {name : "NewSheet.xlsx"} |
| Target File Name   | Text               | Yes                                    | NewExcel.xlsx            |

- **Output**:

  | **Parameter Name** | **Parameter Type** | **Example**                            |
  |--------------------|--------------------|----------------------------------------|
  | File               | Entity             | {name : "ABC", extension:".txt" , ...} |
  | Message            | Text               | " "                                    |
  | Success            | Yes/No             | Yes                                    |

#### Share File

- **Description**: Share the file denoted by source with the given users identified by their email address applying the provided Permissions.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                    |
|--------------------|--------------------|---------------|--------------------------------------------------------------------------------|
| Source             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/fileName |
| Share              | Pick One           | No            | [{Email : "testmail@mail.com, Permissions : "Read"}]                           |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |
| Message            | Text               | " "                                    |
| Success            | Yes/No             | Yes                                    |


#### Upload

- **Description**: Upload the content of the file from the media server to the cloud storage at target path. If Target path does not include a file name, the file name of the uploaded file is use. Produced File Entity represent the file in the cloud storage.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                    |
|--------------------|--------------------|---------------|--------------------------------------------------------------------------------|
| Krista File        | List&lt;File>      | Yes           | Test.pdf                                                                       |
| Target Path        | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/fileName |

> ** Note :
> - Upload File Supports file size up to 50 MB.

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |
| Message            | Text               | " "                                    |
| Success            | Yes/No             | Yes                                    |


#### Upload Text

- **Description**: Uploads the file content to the cloud storage at the given target location as a plain text file.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                        |
|--------------------|--------------------|---------------|------------------------------------------------------------------------------------|
| File Content       | Text               | Yes           | "Demo File Content"                                                                |
| Target             | Text               | Yes           | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/fileName.txt |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                            |
|--------------------|--------------------|----------------------------------------|
| File               | Entity             | {name : "ABC", extension:".txt" , ...} |
| Message            | Text               | " "                                    |
| Success            | Yes/No             | Yes                                    |

#### Fetch By File Size Range

- **Description**: Accepts File Size , Returns Task ID.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| From Size          | Number             | Yes           | 123         |
| To Size            | Number             | Yes           | 3423        |



- **Output**:

| **Parameter Name** | **Parameter Type**    | **Example**                              |
|--------------------|-----------------------|------------------------------------------|
| Files              | List&lt;File Details> | [{name : "ABC", extension:".txt" , ...}] |
| Message            | Text                  | " "                                      |
| Success            | Yes/No                | Yes                                      |

#### Fetch File By Modification Date Range

- **Description**: Accepts File Size , Returns Task ID.
- **Input Parameters**:

| **Parameter Name**      | **Parameter Type** | **Mandatory** | **Example**             |
|-------------------------|--------------------|---------------|-------------------------|
| Modification Date Range | DateRange          | Yes           | 01/01/2023 - 02/04/2023 |

- **Output**:

| **Parameter Name** | **Parameter Type**    | **Example**                              |
|--------------------|-----------------------|------------------------------------------|
| Files              | List&lt;File Details> | [{name : "ABC", extension:".txt" , ...}] |
| Message            | Text                  | " "                                      |
| Success            | Yes/No                | Yes                                      |


### Management Area

#### Create List

- **Description**: Creates a new list in the specified SharePoint site.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Site Name          | Text               | Yes           | Site Name   |
| List Name          | Text               | Yes           | List Name   |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                              |
|--------------------|--------------------|------------------------------------------|
| File               | File               | [{name : "ABC", extension:".txt" , ...}] |
| Message            | Text               | "List Created"                           |
| Success            | Yes/No             | Yes                                      |

#### Delete List

- **Description**: Deletes a SharePoint list identified by its site name and list name.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Site Name          | Text               | Yes           | Site Name   |
| List Name          | Text               | Yes           | List Name   |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**    |
|--------------------|--------------------|----------------|
| Message            | Text               | "List Deleted" |
| Success            | Yes/No             | Yes            |

#### Delete Site

- **Description**: Deletes a SharePoint Site/Group with the specified name.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example** |
|--------------------|--------------------|---------------|-------------|
| Site Name          | Text               | Yes           | Site Name   |

- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**    |
|--------------------|--------------------|----------------|
| Message            | Text               | "Site Deleted" |
| Success            | Yes/No             | Yes            |

#### Update List

- **Description**: Updates an existing SharePoint list with the provided display name and description.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**              |
|--------------------|--------------------|---------------|--------------------------|
| Site Name          | Text               | Yes           | Site Name                |
| List Name          | Text               | Yes           | List Name                |
| Display Name       | Text               | Yes           | Updated List Name        |
| Description        | Text               | Yes           | Updated List Description |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                              |
|--------------------|--------------------|------------------------------------------|
| File               | File               | [{name : "ABC", extension:".txt" , ...}] |
| Message            | Text               | "List Created"                           |
| Success            | Yes/No             | Yes                                      |

#### Update Site

- **Description**: Updates the display name of an existing SharePoint site.
- **Input Parameters**:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**       |
|--------------------|--------------------|---------------|-------------------|
| Site Name          | Text               | Yes           | Site Name         |
| Updated Site Name  | Text               | Yes           | Updated Site Name |


- **Output**:

| **Parameter Name** | **Parameter Type** | **Example**                              |
|--------------------|--------------------|------------------------------------------|
| File               | File               | [{name : "ABC", extension:".txt" , ...}] |
| Message            | Text               | "List Modified"                          |
| Success            | Yes/No             | Yes                                      |


## Entity Request

Krista SharePoint Extension supports the following Entity Request:

#### File

> ** Note :
> - File Entity Supports For SharePoint Files and Folders.

* Search By File Name
    * Description: Returns list of File Details.
    * Input Parameters:

| **Parameter Name** | **Parameter Type** | **Mandatory** | **Example**                                                                       |
|--------------------|--------------------|---------------|-----------------------------------------------------------------------------------|
| Name               | Text               | No            | Sample.xlsx                                                                       |
| Absolute Path      | Text               | No            | https://domain.sharepoint.com/sites/siteName /Shared Documents/folder/Sample.xlsx |
| Extension          | Text               | No            | xlsx                                                                              |
| Owner              | Text               | No            | Owner Name                                                                        |

* Output:

| **Parameter Name** | **Parameter Type**    |
|-------------------|-----------------------|
| File       | List&lt;File Details> |



