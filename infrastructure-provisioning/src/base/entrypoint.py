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
from configparser import SafeConfigParser
import argparse
from fabric.api import *
import json
import sys
import select

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
            config = SafeConfigParser()
            config.read(os.path.join('/root/conf', filename))
            for section in config.sections():
                for option in config.options(section):
                    varname = "{0}_{1}".format(section, option)
                    if varname not in os.environ:
                        os.environ[varname] = config.get(section, option)

    # Overwrite config if overwrite.ini is provided
    for filename in os.listdir('/root/conf'):
        if filename.endswith('overwrite.ini'):
            config = SafeConfigParser()
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

    with hide('running'):
        if args.action != 'terminate':
            local('chmod 600 /root/keys/*.pem')

    if dry_run:
        with open("/response/{}.json".format(request_id), 'w') as response_file:
            response = {"request_id": request_id, "action": args.action, "dry_run": "true"}
            response_file.write(json.dumps(response))

    # Run execution routines
    elif args.action == 'create':
        with hide('running'):
            local("/bin/create.py")

    elif args.action == 'status':
        with hide('running'):
            local("/bin/status.py")

    elif args.action == 'describe':
        with open('/root/description.json') as json_file:
            description = json.load(json_file)
            description['request_id'] = request_id
            with open("/response/{}.json".format(request_id), 'w') as response_file:
                response_file.write(json.dumps(description))

    elif args.action == 'stop':
        with hide('running'):
            local("/bin/stop.py")

    elif args.action == 'start':
        with hide('running'):
            local("/bin/start.py")

    elif args.action == 'terminate':
        with hide('running'):
            local("/bin/terminate.py")

    elif args.action == 'configure':
        with hide('running'):
            local("/bin/configure.py")

    elif args.action == 'recreate':
        with hide('running'):
            local("/bin/recreate.py")

    elif args.action == 'reupload_key':
        with hide('running'):
            local("/bin/reupload_key.py")

    elif args.action == 'lib_install':
        with hide('running'):
            local("/bin/install_libs.py")

    elif args.action == 'lib_list':
        with hide('running'):
            local("/bin/list_libs.py")

    elif args.action == 'git_creds':
        with hide('running'):
            local("/bin/git_creds.py")

    elif args.action == 'create_image':
        with hide('running'):
            local("/bin/create_image.py")

    elif args.action == 'terminate_image':
        with hide('running'):
            local("/bin/terminate_image.py")

    elif args.action == 'reconfigure_spark':
        with hide('running'):
            local("/bin/reconfigure_spark.py")

    elif args.action == 'check_inactivity':
        with hide('running'):
            local("/bin/check_inactivity.py")