#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

import argparse
import sys
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *
import json
import ast


parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--instance_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--libs', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    env.hosts = "{}".format(args.instance_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    print('Installing libraries: {}'.format(args.libs))
    general_status = list()
    data = ast.literal_eval(args.libs)
    pkgs = {"libraries": {}}

    try:
        for row in range(len(data)):
            if not data[row]['group'] in pkgs['libraries'].keys():
                pkgs["libraries"].update({data[row]['group']: []})
            pkgs['libraries'][data[row]['group']].append(data[row]['name'])
            if data[row]['group'] == "java":
                pkgs['libraries'][data[row]['group']].append(data[row]['version'])
    except Exception as err:
        append_result("Failed to parse libs list.", str(err))
        sys.exit(1)

    try:
        print('Installing os packages: {}'.format(pkgs['libraries']['os_pkg']))
        status = install_os_pkg(pkgs['libraries']['os_pkg'])
        general_status = general_status + status
    except KeyError:
        pass

    if os.environ['application'] in ['jupyter', 'zeppelin', 'deeplearning', 'tensor', 'tensor-rstudio', 'rstudio']:
        try:
            print('Installing java dependencies: {}'.format(pkgs['libraries']['java']))
            status = install_java_pkg(pkgs['libraries']['java'])
            general_status = general_status + status
        except KeyError:
            pass

        try:
            print('Installing pip2 packages: {}'.format(pkgs['libraries']['pip2']))
            status = install_pip_pkg(pkgs['libraries']['pip2'], 'pip2', 'pip2')
            general_status = general_status + status
        except KeyError:
            pass

        try:
            print('Installing pip3 packages: {}'.format(pkgs['libraries']['pip3']))
            status = install_pip_pkg(pkgs['libraries']['pip3'], 'pip3', 'pip3')
            general_status = general_status + status
        except KeyError:
            pass

        try:
            print('Installing other packages: {}'.format(pkgs['libraries']['others']))
            for pkg in pkgs['libraries']['others']:
                status_pip2 = install_pip_pkg([pkg], 'pip2', 'others')
                status_pip3 = install_pip_pkg([pkg], 'pip3', 'others')
                if status_pip2[0]['status'] == 'installed':
                    general_status = general_status + status_pip2
                else:
                    general_status = general_status + status_pip3
        except KeyError:
            pass

    if (os.environ['application'] in ('jupyter', 'zeppelin')
        and os.environ['notebook_r_enabled'] == 'true')\
            or os.environ['application'] in ('rstudio', 'tensor-rstudio'):
        try:
            print('Installing R packages: {}'.format(pkgs['libraries']['r_pkg']))
            status = install_r_pkg(pkgs['libraries']['r_pkg'])
            general_status = general_status + status
        except KeyError:
            pass

    with open("/root/result.json", 'w') as result:
        res = {"Action": "Install additional libs",
               "Libs": general_status}
        result.write(json.dumps(res))
