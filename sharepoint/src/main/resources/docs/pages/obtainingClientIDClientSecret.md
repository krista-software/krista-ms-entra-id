# Obtaining Client ID, Tenant ID, and Client Secret


For the SharePoint Extension, you would need to obtain Client ID, Tenant ID, and Client Secret from the Azure Portal.


### Steps to get Client ID, Tenant ID, and Client Secret

* Log in to the Azure portal from [here](https://portal.azure.com/).
  ![Home Page](../_media/homePage.png)


* Under **Azure services**, click **Azure Active Directory**. If not found readily click **More services** to find Azure Active Directory.
  ![Azure Active Directory](../_media/azureActiveDirectory.png)


* On the following Overview page, click **App registrations** under the **Manage** column menu.
  ![App Registration](../_media/appRegistration.png)


* At the top of the page, click **New Registration**. Alternatively, open your pre-existing application.
  ![New registration](../_media/newRegistration.png)


* **Register an application** page is displayed. Enter the relevant details on this application page. Enter the **Name** that would be display name. Select the **Supported account types**. Out of the available options, select accounts in the single tenant only (first option). Under Redirect URI, select the Web platform. Add the authorized redirect URI corresponding to your SharePoint extension. Click **Register**.
  * Go to the Details page of the extension and copy the Routing id from there. After that, append **"/rest/sharepoint/callback"** at the end of the routing id (This is you redirect URI). Now add this URI in Authorized redirect URI in credentials of your registered app in azure portal.
      ![Routing Id Reference](../_media/routing_id.png)
      ![Register](../_media/register.png)


* After registration, in **Overview**, under **Essentials**, you will find the Client ID and Tenant ID.
  ![Client Id and Tenant Id](../_media/clientIDtenantID.png)


* Click **API Permissions**. Under **Configured permissions**, click **Add a permission**.
  ![Add a Permission](../_media/addAPermission.png)


* On the **Request API permissions** page, select **Microsoft Graph**.
  ![Microsoft Graph](../_media/microsoftGraph.png)


* Select **Delegated Permissions**.
  ![Delegated Permissions](../_media/delegatedPermissions.png)


* Start typing permission name and select the following permissions after you see.
  * Files.ReadWrite.All
  * offline_access
  * openid
  * profile
  * User.read

  ![Select Permission](../_media/selectPermissions.png)


* Select **Certificates & secrets**, go to **Client secrets** tab, and click **New client secret**.
  ![Certificates & Secrets](../_media/certificates&Secrets.png)


* On the **Add a client secret** panel, enter the **Description**, and set up the time span of expiry in the **Expires** field. Click **Add**.
  ![Add Client Secret](../_media/addClientSecret.png)


* On the following page, you will see the Client Secret under **Value**.
  ![Client Secret](../_media/clientSecret.png)


* Save the Client ID, Tenant ID, and Client Secret for future reference.


* You must provide these values in your extension to proceed with the operations such as catalog requests and invoker request.
