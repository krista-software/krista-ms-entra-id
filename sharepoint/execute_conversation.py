import argparse
import inspect
import os
import time

from krista_automation_lib import KristaLibs

# Define command-line arguments
parser = argparse.ArgumentParser()
parser.add_argument('-config_file', action='store', dest='config_file', default="", help='Config file', required=True)
parsed = parser.parse_args()

# Initialize the KristaLibs instance with the provided configuration file
krista_libs = KristaLibs(config_file=parsed.config_file)

# Start executing a conversation within the extension
fields = {"Number1": 30, "Number2": 20}
krista_libs.start_execute_conversation(fields=fields)
# time.sleep(5)
#
# # Define a dictionary of field values for the conversation step
# data_file = "extension_workspace_config.json"
# data_file_path = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe()))) + "/" + data_file
# f_ext = data_file.split(".")[-1]
# media_id = krista_libs.upload_file_to_media_server(data_file_path)
# fields = {"num1": 30, "num2": 20, "File1": [{"fileName": data_file, "mediaId": media_id, "extension": f_ext}]}
#
# # Execute a conversation step with a custom message and field values
# krista_libs.execute_conversation_step(message="Enter Value for Multiplication", fields=fields)
# time.sleep(5)
#
# # Get all conversation messages
# all_msg = krista_libs.get_all_conversation_messages(exp_len=6)
#
# # Assert specific values from the conversation messages
# assert 50 == krista_libs.get_conversation_field_value_from_msg_seq(all_msg, seq=3, field="Result"), "Invalid Value found"
# assert 600 == krista_libs.get_conversation_field_value_from_msg_seq(all_msg, seq=6, field="Result"), "Invalid Value found"
#
# # Print a success message if the assertions pass
# print("Smoke test has been done successfully")
