# Configuring SharePoint Extension in Krista

### Steps for connection


* Client ID and Client Secret:
  * To make connection, you have to provide client id and client secret in setup parameters from your registered web application in azure portal. You can get these parameters from **Credentials** tab of the project selected.
    ![Client Id](../_media/clientId.png)
    ![Client Secret](../_media/clientSecretId.png)


* Once you have provided the client id and client secret you must authorize yourself with the same account which is used to register the application in azure portal.


* To authorize yourself you are required to follow these steps:
   * Go to the Details page of the extension and copy the Routing id from there. After that, append **"/rest/sharepoint/callback"** at the end of the routing id (This is you redirect URI). Now add this URI in Authorized redirect URI in credentials of your registered app in azure portal.
     ![Routing Id Reference](../_media/routing_id.png)
     ![Authorized redirect uri Reference](../_media/redirectUrl.png)
   * Run one of the request of the extension through conversation in the client. Once you run the request in the client, an authentication will appear. Click on that tab and authorize yourself using the same account which is used to register your application in azure portal.


* Once you are authorized, you can see if the connection is established by clicking test connection on setup page.


* After the connection is established, you are good to go with the requests of the extension.


### Setup SharePoint extension


* Adding SharePoint extension
  * Catalogs > Essentials > Cloud Files > SharePoint    
    ![SetupPage](../_media/setup1.jpg)


* Add SharePoint extension by clicking on **Add to Workspace**
  * Setup Share Point > **Name** > Save  
    ![SetupPage](../_media/setup2.jpg)


* Now you can use this same Client id , Client secret, Tenant id and Email as Setup Parameters in Setup Page and Click on Validate Attributes.
  ![SetupPage](../_media/setup3.jpg)



