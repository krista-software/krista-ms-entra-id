import argparse
from krista_automation_lib import KristaLibs

# Define command-line arguments
parser = argparse.ArgumentParser()
parser.add_argument('-config_file', action='store', dest='config_file', default="", help='Config file', required=True)
parser.add_argument('-jar_file', action='store', dest='jar_file', default="", help='Krista extension name', required=True)
parsed = parser.parse_args()

# Initialize the KristaLibs instance with the provided configuration file
libs = KristaLibs(config_file=parsed.config_file)

# Upload the specified extension JAR file to the Krista platform
libs.upload_extension(jar_file=parsed.jar_file)

# Trigger the upgrade of the uploaded extension within the Krista platform
libs.extension_upgrade()
