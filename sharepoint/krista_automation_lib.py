import base64
import json
import time
import urllib
import uuid
import requests
import os
import sys
import argparse


class KristaLibs:
    """
        A class providing utility methods for interacting with Krista Studio and related functionalities.
    """

    def __init__(self, config_file):
        """
        Initialize a KristaLibs instance with configuration parameters from a JSON file.

        Parameters:
        - config_file (str): The path to a JSON configuration file containing the necessary parameters
          for interacting with Krista Studio and other services.
        """
        self.conf = json.load(open(config_file))
        self.studio_url = self.conf["studio_url"]
        self.client_url = self.conf["client_url"]
        self.extension_url = self.conf["extension_url"]
        self.username = self.conf["username"]
        self.password = self.conf["password"]
        self.workspace_id = self.conf["workspace_id"]
        self.defn_ws_id = "defnWs_" + self.workspace_id.split("_")[1]
        self.studio_client_session_id = self.get_client_session_id(self.username, self.password, "studio")
        self.client_client_session_id = self.get_client_session_id(self.username, self.password, "client")
        self.list_of_invokers = self.get_all_invoker()
        self.furl = ""
        self.execution_id = ""

    def get_dict_from_list_of_dict(self, listOfDict, keyName, keyValue):
        found_values = []
        for dictionary in listOfDict:
            if dictionary[keyName] == keyValue:
                found_values.append(dictionary)
        return found_values

    def base64encode(self, passstr):
        """
        Encode a given ASCII string in base64 format.
        Parameters:
        - passstr (str): The ASCII string to be encoded in base64 format.
        Returns:
        - str: The base64-encoded representation of the input string.
        """
        sample_string_bytes = passstr.encode("ascii")
        base64_bytes = base64.b64encode(sample_string_bytes)
        base64_string = base64_bytes.decode("ascii")
        return base64_string

    def api_call(self, pod_url, method, data=None, request_headers=None, basic_auth=None):
        """
        Perform an HTTP request to a specified URL using the given HTTP method and parameters.
        Parameters:
        - pod_url (str): The URL to which the HTTP request will be sent.
        - method (str): The HTTP method to use for the request (e.g., "GET," "POST," "PUT," "DELETE").
        - data (dict or None): The data to include in the request body for methods that support it.
          If None, no data will be included in the request body.
        - request_headers (dict or None): Additional headers to include in the request. If None, no
          additional headers will be added.
        - basic_auth (tuple or None): A tuple containing the username and password for HTTP basic
          authentication, if required. If None, no basic authentication will be used.
        Returns:
        - Response: An HTTP response object containing the server's response to the request.
        """
        requests.packages.urllib3.disable_warnings()
        url = pod_url
        basic_auth = basic_auth
        method = method
        params = data
        headers = {'X-Requested-With': 'curl'}
        if request_headers is not None:
            headers.update(request_headers)
        _types = ["GET", "POST", "DELETE", "PUT", "PATCH"]
        if method not in _types:
            raise ValueError("Wrong method")
        response = None
        if method == "GET":
            response = requests.get(url=url, headers=headers, params=params, auth=basic_auth, verify=False)
        elif method == "POST":
            response = requests.post(url=url, headers=headers, data=params, auth=basic_auth, verify=False)
        elif method == "DELETE":
            response = requests.delete(url=url, headers=headers, data=params, auth=basic_auth, verify=False)
        elif method == "PUT":
            response = requests.put(url=url, headers=headers, data=params, auth=basic_auth, verify=False)
        elif method == "PATCH":
            response = requests.patch(url=url, headers=headers, data=params, auth=basic_auth, verify=False)

        return response

    def get_login_id(self, username, application="client"):
        """
        Gets a login code for the specified user and application.
        Args:
            username (str): The username to get the login code for.
            application (str, optional): The name of the application to get the login code for. Defaults to "client".
        Returns:
            str: The login code, or None if the login code could not be retrieved.
        """
        url = self.studio_url + "api/rpc/v1/auth/verifyEmail"
        payload = {
            "payload": {
                "identificationToken": {"tokenType": "email", "value": username},
                "sendEmail": False,
                "appVersion": {"name": "Krista", "version": "0.1", "build": "qa"},
                "application": application
            }
        }
        payload = json.dumps(payload)
        headers = {'Content-Type': 'application/json'}
        response = self.api_call(url, "POST", request_headers=headers, data=payload)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        response = json.loads(response.text)
        if response.get("isError"):
            raise Exception(str(response.get("payload").get("validationErrors")))
        login_code = response["payload"]["loginCode"]
        return login_code

    def get_client_session_id(self, username, password, app_name="studio", is_appliance=False):
        """
        Logs in to Studio or Studio Client and returns the client session ID.
        Args:
            username (str): The username to use for login.
            password (str): The password to use for login.
            app_name (str, optional): The name of the application to log in to. Defaults to "studio".
            is_appliance (bool, optional): Whether to log in to Studio Appliance. Defaults to False.
        Returns:
            str: The client session ID, or None if the login fails.
        """
        body_login = {
            "context": {"transactionId": None},
            "payload": {
                "identificationToken": {"tokenType": "email", "value": username},
                "forgotPassword": True,
                "workspaceId": self.workspace_id,
                "password": self.base64encode(password),
                "appVersion": {"name": "Krista", "version": "0.1", "build": "dev"},
                "application": app_name
            }
        }
        if not is_appliance:
            l_login_code = self.get_login_id(username, app_name)
            body_login["payload"]["loginCode"] = l_login_code
        url = self.studio_url + "api/rpc/v1/auth/login"
        response = self.api_call(url, "POST", data=json.dumps(body_login))
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        return json.loads(response.text).get('payload').get('clientSessionId')

    def get_header_client_session_id(self, client_session_id):
        """
        Generates a header containing the client session ID.
        Args:
            client_session_id (str): The client session ID.
        Returns:
            dict: A dictionary containing the following headers:
                * X-Krista-Context: A header containing the client session ID.
                * Content-Type: A header indicating that the request body is in JSON format.
        """
        tempdumpheader = json.dumps({'clientSessionId': client_session_id})
        parseheader = urllib.parse.quote(tempdumpheader)
        return {"X-Krista-Context": parseheader, "Content-Type": "application/json; charset=UTF-8"}

    def upload_file_to_media_server(self, file_name):
        """
        Uploads a file to the media server.
        Args:
            file_name (str): The path to the file to upload.
        Returns:
            str: The ID of the uploaded file.
        """
        ext_map = {"xlsx": "excel", "xls": "excel", "csv": "csv", "docx": "word", "pdf": "pdf", "jar": "jar", "json": "binary"}
        f_name = file_name.split("/")[-1]
        f_ext = f_name.split(".")[-1]
        if f_ext in ["jpg", "jpeg", "png"]:
            media_id = "image_" + self.generate_uuid() + "." + f_ext
        else:
            media_id = "doc_" + ext_map[f_ext] + "_" + self.generate_uuid() + "." + f_ext
        head = {'Content-Type': 'application/zip'}
        file_data = open(file_name, 'rb')
        size = os.path.getsize(file_name)
        url = self.studio_url + "api/media/public/" + media_id + '?len=' + str(size)
        response = self.api_call(url, "POST", request_headers=head, data=file_data)
        print(response.text)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        return media_id

    def generate_uuid(self):
        """
        Generates a universally unique identifier (UUID).
        Returns:
            str: A UUID string.
        """
        return str(uuid.uuid4())

    def get_catalogs(self):
        """
        Retrieves a list of all catalogs in the workspace.
        Returns:
            list: A list of catalog objects, where each catalog object is a dictionary with the following keys:
                * id: The ID of the catalog.
                * title: The name of the catalog.
                * type: The type of the catalog.
                * description: A description of the catalog.
        """
        uri = "appliance/" + self.workspace_id + "/catalogs"
        url = self.studio_url + uri
        head = self.get_header_client_session_id(self.studio_client_session_id)
        response = self.api_call(url, "GET", request_headers=head)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        return res["payload"]

    def get_private_catalog_id(self):
        """
        Retrieves the ID of the private catalog appliance.
        Returns:
            str: The ID of the private catalog appliance, or None if the private catalog appliance is not found.
        """
        uri = "appliance/" + self.workspace_id + "/catalogs"
        url = self.studio_url + uri
        head = self.get_header_client_session_id(self.studio_client_session_id)
        response = self.api_call(url, "GET", request_headers=head)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        for catalog in res["payload"]:
            if catalog["type"] == "PRIVATE":
                return catalog["id"]

    def get_global_catalog_id(self):
        """
       Retrieves the ID of the global catalog appliance.
       Returns:
           str: The ID of the global catalog appliance, or None if the global catalog appliance is not found.
       """
        uri = "appliance/" + self.workspace_id + "/catalogs"
        url = self.studio_url + uri
        head = self.get_header_client_session_id(self.studio_client_session_id)
        response = self.api_call(url, "GET", request_headers=head)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        for catalog in res["payload"]:
            if catalog["type"] == "APPLIANCE":
                return catalog["id"]

    def get_ecosystems_id(self, ecosystem_name):
        """
        Retrieves the ID of an ecosystem, given the ecosystem name.
        Args:
            ecosystem_name (str): The name of the ecosystem to search for.
        Returns:
            str: The ID of the ecosystem, or None if the ecosystem is not found.
        """
        uri = "appliance/" + self.workspace_id + "/catalog/ecosystems?"
        for ext in self.get_catalogs():
            uri += "&catalogs=" + ext["id"]
        url = self.studio_url + uri
        head = self.get_header_client_session_id(self.studio_client_session_id)
        response = self.api_call(url, "GET", request_headers=head)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        for ecosystem in res["payload"]["ecosystems"]:
            if ecosystem_name == ecosystem["title"]:
                return ecosystem["id"]

    def get_domains_id(self, ecosystem_id, domain_name):
        """
        Retrieves the ID of a domain in the specified ecosystem, given the domain name.
        Args:
            ecosystem_id (str): The ID of the ecosystem to search.
            domain_name (str): The name of the domain to search for.
        Returns:
            str: The ID of the domain, or None if the domain is not found.
        """
        uri = "appliance/" + self.workspace_id + "/catalog/ecosystems/" + ecosystem_id + "/domains?"
        for ext in self.get_catalogs():
            uri += "&catalogs=" + ext["id"]
        url = self.studio_url + uri
        head = self.get_header_client_session_id(self.studio_client_session_id)
        response = self.api_call(url, "GET", request_headers=head)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        for domain in res["payload"]["domains"]:
            if domain_name == domain["title"]:
                return domain["id"]

    def get_extension_details(self, ecosystem_id, domain_id, extension_name):
        """
            Retrieve details of a specific extension within a domain of an ecosystem.
            Parameters:
            - ecosystem_id (str): The unique identifier of the ecosystem containing the domain.
            - domain_id (str): The unique identifier of the domain where the extension is located.
            - extension_name (str): The name of the extension whose details you want to retrieve.
            Returns:
            - dict or None: A dictionary containing details of the specified extension if it is found.
              The dictionary includes keys: "id," "payload," and subkeys "title," "description," and "extensionBinaryId."
              If the extension is not found, the method returns None.
        """
        uri = "appliance/" + self.workspace_id + "/catalog/ecosystems/" + ecosystem_id + "/domains/" + domain_id + "/extensions?"
        for ext in self.get_catalogs():
            uri += "&catalogs=" + ext["id"]
        url = self.studio_url + uri
        head = self.get_header_client_session_id(self.studio_client_session_id)
        response = self.api_call(url, "GET", request_headers=head)
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        for extension in res["payload"]["extensions"]:
            if extension_name == extension["title"]:
                return {"id": extension["id"], "payload": {"title": extension["title"], "description": extension["description"], "extensionBinaryId": ""}}

    def upload_extension(self, jar_file):
        """
           Upload an extension JAR file and update its details in the ecosystem.
           Parameters:
           - jar_file (str): The path to the extension JAR file to be uploaded.
           Note: This method is used to upload an extension JAR file and associate it with the specified ecosystem and domain.
        """
        if os.path.exists(jar_file):
            ecosystem = self.conf["ecosystem"]
            domain = self.conf["domain"]
            extension_name = self.conf["extension_name"]
            ecosystem_id = self.get_ecosystems_id(ecosystem)
            domain_id = self.get_domains_id(ecosystem_id, domain)
            ext_details = self.get_extension_details(ecosystem_id, domain_id, extension_name)
            media_id = self.upload_file_to_media_server(jar_file)
            ext_details["payload"]["extensionBinaryId"] = media_id
            uri = "appliance/" + self.workspace_id + "/catalog/ecosystems/" + ecosystem_id + "/domains/" + domain_id + "/extensions/" + ext_details["id"]
            url = self.studio_url + uri
            head = self.get_header_client_session_id(self.studio_client_session_id)
            response = self.api_call(url, "PUT", request_headers=head, data=json.dumps(ext_details["payload"]))
            assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
            res = json.loads(response.text)
            assert not res["isError"], "Error Found" + str(res)
            print("Successfully Uploaded Extension:", extension_name)
        else:
            print("File Not Found:", jar_file)

    def get_all_invoker(self):
        """
           Retrieve a list of all invokers associated with the current workspace.
           Returns:
           - list: A list of dictionaries, each containing information about an invoker.
        """
        url = self.studio_url + "api/rpc/v1/studio/invokerGetAll"
        body = {"context": {"clientSessionId": self.studio_client_session_id}, "payload": {"defnWsId": self.defn_ws_id}}
        response = self.api_call(url, "POST", data=json.dumps(body))
        return json.loads(response.text).get('payload').get('invokers')

    def get_invoker_id(self, invoker_name):
        for inv in self.list_of_invokers:
            if inv["name"] == invoker_name:
                return inv["id"]

    def get_routing_id(self, invoker_name):
        """
           Retrieve the ID of a specific invoker by its name.
           Parameters:
           - invoker_name (str): The name of the invoker whose ID you want to retrieve.
           Returns:
            - str or None: If the specified invoker is found, the method returns its ID as a string.
              If the invoker is not found, the method returns None.
        """
        try:
            found_values = self.get_dict_from_list_of_dict(self.list_of_invokers, "name", invoker_name)
            routing_id = (found_values[0].get('routingId'))
            return routing_id
        except Exception as e:
            print("This exception is from getRoutingId() in utils.py" + str(e))

    def extension_upgrade(self):
        """
        Upgrade the extension associated with the current workspace.
        Implementation Details:
            This method initiates the upgrade of the extension specified in the configuration by making an HTTP
            POST request to the 'invokerUpgrade' API endpoint. It includes the workspace ID and invoker ID in
            the request payload for the upgrade.
            The upgrade process is initiated, and no response data is expected.
        Note: This method is used to trigger the upgrade of a specific extension within the workspace.
        """
        invoker_name = self.conf["extension_name"]
        url = "api/rpc/v1/studio/invokerUpgrade"
        print("Upgrading Extension: " + invoker_name)
        payload = {"context": {"clientSessionId": self.studio_client_session_id},
                   "payload": {"workspaceId": self.workspace_id, "invokerId": self.get_invoker_id(invoker_name)}}
        self.api_call(self.studio_url + url, "POST", data=json.dumps(payload))

    def start_execute_conversation(self, conversation_name=None, fields=None, is_path=False, path_name=None):
        """
        Start the execution of a conversation within the extension.
        Parameters:
        - conversation_name (str, optional): The name of the conversation to start. If not provided, it uses
          the conversation name from the configuration.
        - fields (dict, optional): A dictionary of field values to be used during the conversation execution.
        """
        conversation_name = conversation_name if conversation_name is not None else self.conf["conversation_name"]
        routing_id_ext_api_auth = self.get_routing_id(self.conf["client_api_ext_name"])
        url = "/extension/api/{}/rest/conversations/"
        f_url = self.extension_url + str(url.format(routing_id_ext_api_auth))
        f_url += self.get_conversation_id_for_name(conversation_name) + "/executions/"
        execution_id = self.get_execution_id(f_url)
        for i in range(0, 5):
            time.sleep(1)
            response = self.api_call(f_url + execution_id + "/messages/", 'GET')
            if response.status_code == 200:
                f_url += execution_id + "/messages/"
                print("execution_id:", execution_id)
                break
            else:
                print("Failed:", response.status_code, " - ", response.text)
            time.sleep(1)
            execution_id = self.get_execution_id(f_url)

        self.furl = f_url
        self.execution_id = execution_id
        self.execute_conversation_step(fields=fields, is_path=is_path, path_name=path_name)

    def get_conversation_id_for_name(self, conversation_name):
        """
        Retrieve the ID of a conversation by its name.
        Parameters:
        - conversation_name (str): The name of the conversation whose ID you want to retrieve.
        Returns:
        - str: The ID of the conversation with the specified name.
        """
        ws_d = self.get_workspace_details()
        ws = dict(map(lambda x: (x["title"], x["id"]), ws_d["collectedDefnWs"]["conversationMap"].values()))
        return ws[conversation_name]

    def get_workspace_details(self):
        """
        Retrieve details of the current workspace.
        Returns:
        - dict: A dictionary containing details of the current workspace.
        """
        payload = {"context": {"clientSessionId": self.studio_client_session_id}, "payload": self.workspace_id}
        uri = "api/rpc/v1/studio/wsGet"
        url = self.studio_url + uri
        response = self.api_call(url, "POST", data=json.dumps(payload))
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        res = json.loads(response.text)
        return res["payload"]

    def get_execution_id(self, url):
        """
        Retrieve the execution ID associated with a conversation.
        Parameters:
        - url (str): The URL to make the request to.
        Returns:
        - str: The execution ID of the conversation.
        """
        response = self.api_call(url, 'POST')
        assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
        return json.loads(response.text.replace("'", '"'))["executionId"]

    def get_message_id(self, message, is_substring=False, timeout=10):
        """
        Retrieve the message ID based on a message string within a conversation.
        Parameters:
            - message (str): The message string to search for.
            - is_substring (bool, optional): If True, search for a substring match of the message.
            - timeout (int, optional): The maximum number of seconds to wait for the message.
        Returns:
            - str or None: The message ID if the message is found. None if the message is not found.
        """
        url = self.furl
        for i in range(0, timeout):
            response = self.api_call(url, 'GET')
            assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
            response_t = json.loads(response.text.replace("'", '"'))
            if len(response_t) >= 2 and message is None:
                return response_t[-1]["messageId"]

            for res in response_t[-2:]:
                if not is_substring:
                    if message in res['userMessage']:
                        return res["messageId"]
                else:
                    if message == res['userMessage']:
                        return res["messageId"]
            time.sleep(1)
        print("Message not found with string:", message)

    def execute_conversation_step(self, message=None, fields=None, is_msg_substring=False, is_path=False, path_name=None):
        """
        Execute a step in a conversation within the extension.
        - message: A message string that represents the step to be executed. If provided, this message is used as the content of the conversation step.
        - fields: A dictionary of field values that can be used during the conversation step. These fields provide additional data for the step.
        - is_msg_substring: A boolean flag. If set to True, the step is executed based on a substring match of the message. If False, the message must match exactly.
        - is_path: A boolean flag. If set to True, the step is executed as a path in the conversation. In this case, the 'path_name' parameter must also be provided.
        - path_name: The name of the path to be executed. This parameter is required when 'is_path' is True.

        Example:
            # Execute a step with a specific message
            execute_conversation_step(message="Hello, how can I help you?")

            # Execute a step as a path
            execute_conversation_step(is_path=True, path_name="Path1")

            # Execute a step with field values
            field_data = {"name": "John", "age": 30}
            execute_conversation_step(message="Provide personal info", fields=field_data)

        """
        url = self.furl + self.get_message_id(message, is_substring=is_msg_substring)
        head = {"Content-Type": "application/json; charset=UTF-8"}
        if is_path:
            payload = {"submitValue": path_name, "submitType": "path"}
        else:
            fields = {} if fields is None else fields
            payload = {"values": fields, "submitType": "SUBMIT"}
        response = self.api_call(url, "PUT", request_headers=head, data=json.dumps(payload))
        assert response.status_code == 202, "Invalid Code Found:" + str(response.text)

    def get_all_conversation_messages(self, exp_len=1, wait_loop=30):
        """
            Retrieve all messages in a conversation with the extension.
            Parameters:
            - exp_len (int, optional): The expected number of messages to retrieve.
            - wait_loop (int, optional): The maximum number of iterations to wait for messages.
            Returns:
            - dict: A dictionary containing all messages in the conversation.
        """
        exec_id = self.execution_id
        print("get_all_conversation_messages:", exec_id)
        conversation_url = "/api/rpc/v1/client/workspaces/{}/executions/"
        conversation_url = self.client_url + str(conversation_url.format(self.workspace_id)) + exec_id + "?&pageSize=-500"
        res = None
        head = self.get_header_client_session_id(self.client_client_session_id)
        for _ in range(0, wait_loop):
            response = self.api_call(conversation_url, "GET", request_headers=head)
            res = json.loads(response.text)
            print("Len:", len(res["payload"]["messages"]["main"]))
            assert response.status_code == 200, "Invalid Code Found:" + str(response.text)
            if len(res["payload"]["messages"]["main"]) >= exp_len:
                return res
            time.sleep(2)
        return res

    def get_conversation_field_value_from_msg_seq(self, res, seq, field):
        """
        Retrieve the value of a conversation field from a specific message sequence.
        Parameters:
           - res (dict): The dictionary containing conversation details.
           - seq (int): The index of the message sequence from which to retrieve the field value.
           - field (str): The label of the field whose value you want to retrieve.
        Returns:
           - str or None: The value of the specified field from the message sequence. If the field is not found,
             the method returns None.
        """
        try:
            page_fields = res["payload"]["messages"]["main"][seq]["attributes"]["form"]["pages"][0]["pageFields"]
            field_values = res["payload"]["messages"]["main"][seq]["attributes"]["formResponse"]["fieldValues"]
            for fields in page_fields:
                if fields['label'] == field:
                    return field_values[fields['localId']]
            return None
        except Exception as e:
            print("This exception is from get_conversation_field_value_from_msg_seq(): " + str(e))
