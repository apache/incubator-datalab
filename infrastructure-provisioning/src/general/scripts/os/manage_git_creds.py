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

import argparse
import ast
import os
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)

    gitlab_certfile = os.environ['conf_gitlab_certfile']
    if exists(conn, '/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)

    git_creds = dict()
    try:
        if exists(conn, '/home/{}/.netrc'.format(args.os_user)):
            conn.run('rm .netrc')
        if exists(conn, '/home/{}/.gitcreds'.format(args.os_user)):
            conn.run('rm .gitcreds')
        git_creds = os.environ['git_creds']
    except KeyError as err:
        print('Error: {0}'.format(err))
        print("Parameter git_creds does not exist. Skipping.")
        sys.exit(0)

    try:
        data = ast.literal_eval(git_creds)
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to parse git credentials.", str(err))
        sys.exit(1)

    try:
        new_config = list()
        for i in range(len(data)):
            if data[i]['hostname'] == "":
                new_config.append('default login {0} password {1}'.format(data[i]['login'], data[i]['password']))
            else:
                new_config.append('machine {0} login {1} password {2}'.format(data[i]['hostname'], data[i]['login'], data[i]['password']))
        with open("new_netrc", "w+") as f:
            for conf in sorted(new_config, reverse=True):
                f.writelines(conf + "\n")
        conn.put('new_netrc', '/home/{}/.netrc'.format(args.os_user))

        creds = dict()
        with open("new_gitcreds", 'w') as gitcreds:
            for i in range(len(data)):
                creds.update({data[i]['hostname']: [data[i]['username'], data[i]['email']]})
            gitcreds.write(json.dumps(creds))
        conn.put('new_gitcreds', '/home/{}/.gitcreds'.format(args.os_user))

    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to add host/login/(password/token) to config.", str(err))
        sys.exit(1)

    conn.close()