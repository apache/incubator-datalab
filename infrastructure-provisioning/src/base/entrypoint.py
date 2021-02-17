#!/usr/bin/python3

# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

import os
from configparser import ConfigParser
import argparse
import fabric
import json
import sys
import select
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--action', type=str, default='')
args = parser.parse_args()


def get_from_stdin():
    lines = []
    while sys.stdin in select.select([sys.stdin], [], [], 0)[0]:
        line = sys.stdin.readline()
        if line:
            lines.append(line)
        else:
            break
    if len(lines) > 0:
        return ''.join(lines)
    else:
        return "{}"


if __name__ == "__main__":
    # Get request ID as if it will need everywhere
    request_id = 'ssn'
    try:
        request_id = os.environ['request_id']
    except:
        os.environ['request_id'] = 'ssn'

    # Get config from STDIN
    stdin_contents = get_from_stdin()
    try:
        passed_as_json = json.loads(stdin_contents)
    except:
        with open("/response/{}.json".format(request_id), 'w') as response_file:
            reply = dict()
            reply['request_id'] = os.environ['request_id']
            reply['status'] = 'err'
            reply['response'] = dict()
            reply['response']['result'] = "Malformed config passed in stdin"
            reply['response']['stdin_contents'] = stdin_contents
            response_file.write(json.dumps(reply))
        sys.exit(1)

    # Get config (defaults) from files. Will not overwrite any env
    for filename in os.listdir('/root/conf'):
        if filename.endswith('.ini'):
            config = ConfigParser()
            config.read(os.path.join('/root/conf', filename))
            for section in config.sections():
                for option in config.options(section):
                    varname = "{0}_{1}".format(section, option)
                    if varname not in os.environ:
                        os.environ[varname] = config.get(section, option)

    # Overwrite config if overwrite.ini is provided
    for filename in os.listdir('/root/conf'):
        if filename.endswith('overwrite.ini'):
            config = ConfigParser()
            config.read(os.path.join('/root/conf', filename))
            for section in config.sections():
                for option in config.options(section):
                    varname = "{0}_{1}".format(section, option)
                    os.environ[varname] = config.get(section, option)

    for option in passed_as_json:
        try:
            os.environ[option] = passed_as_json[option]
        except:
            os.environ[option] = str(passed_as_json[option])

    # Pre-execution steps: checking for dry running
    dry_run = False
    try:
        if os.environ['dry_run'] == 'true':
            dry_run = True
    except:
        pass

    if args.action != 'terminate':
        subprocess.run('chmod 600 /root/keys/*.pem', shell=True, check=True)

    if dry_run:
        with open("/response/{}.json".format(request_id), 'w') as response_file:
            response = {"request_id": request_id, "action": args.action, "dry_run": "true"}
            response_file.write(json.dumps(response))

    # Run execution routines
    elif args.action == 'create':
        subprocess.run("/bin/create.py", shell=True, check=True)

    elif args.action == 'status':
        subprocess.run("/bin/status.py", shell=True, check=True)

    elif args.action == 'describe':
        with open('/root/description.json') as json_file:
            description = json.load(json_file)
            description['request_id'] = request_id
            with open("/response/{}.json".format(request_id), 'w') as response_file:
                response_file.write(json.dumps(description))

    elif args.action == 'stop':
        subprocess.run("/bin/stop.py", shell=True, check=True)

    elif args.action == 'start':
        subprocess.run("/bin/start.py", shell=True, check=True)

    elif args.action == 'terminate':
        subprocess.run("/bin/terminate.py", shell=True, check=True)

    elif args.action == 'configure':
        subprocess.run("/bin/configure.py", shell=True, check=True)

    elif args.action == 'recreate':
        subprocess.run("/bin/recreate.py", shell=True, check=True)

    elif args.action == 'reupload_key':
        subprocess.run("/bin/reupload_key.py", shell=True, check=True)

    elif args.action == 'lib_install':
        subprocess.run("/bin/install_libs.py", shell=True, check=True)

    elif args.action == 'lib_list':
        subprocess.run("/bin/list_libs.py", shell=True, check=True)

    elif args.action == 'git_creds':
        subprocess.run("/bin/git_creds.py", shell=True, check=True)

    elif args.action == 'create_image':
        subprocess.run("/bin/create_image.py", shell=True, check=True)

    elif args.action == 'terminate_image':
        subprocess.run("/bin/terminate_image.py", shell=True, check=True)

    elif args.action == 'reconfigure_spark':
        subprocess.run("/bin/reconfigure_spark.py", shell=True, check=True)

    elif args.action == 'check_inactivity':
        subprocess.run("/bin/check_inactivity.py", shell=True, check=True)