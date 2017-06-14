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
from fabric.contrib.files import exists
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *
import json
import ast
import netrc


parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--git_creds', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    # print 'Installing libraries:' + args.git_creds
    general_status = list()
    try:
        data = ast.literal_eval(args.git_creds)
    except Exception as err:
        append_result("Failed to parse git credentials.", str(err))
        sys.exit(1)

    try:
        run('git config --global user.name \"{}\"'.format(data['username']))
        run('git config --global user.email \"{}\"'.format(data['email']))
    except Exception as err:
        append_result("Failed to add username and email to git config.", str(err))
        sys.exit(1)

    try:
        new_config = list()
        if exists('/home/{}/.netrc'.format(args.os_user)):
            get('.netrc', '.netrc')
            run('rm .netrc')
            config = netrc.netrc(".netrc")
            if data['hostname'] == "":
                new_config.append('default login {0} password {1}'.format(data['login'], data['password']))
            else:
                new_config.append('machine {0} login {1} password {2}'.format(data['hostname'], data['login'], data['password']))

            for host in config.hosts:
                if host not in ["default", data['hostname']]:
                    new_config.append('machine {0} login {1} password {2}'.format(
                        host, config.authenticators(host)[0], config.authenticators(host)[2]))
        else:
            if data['hostname'] == "":
                new_config.append('default login {0} password {1}'.format(data['login'], data['password']))
            else:
                new_config.append('machine {0} login {1} password {2}'.format(data['hostname'], data['login'], data['password']))

        with open("new_netrc", "w+") as f:
            for conf in sorted(new_config, reverse=True):
                f.writelines(conf)
        put('new_netrc', '/home/{}/.netrc'.format(args.os_user))
    except Exception as err:
        append_result("Failed to add host/login/(password/token/key) to config.", str(err))
        sys.exit(1)


    with open("/root/result.json", 'w') as result:
        res = {"Action": "Setup git credentials",
               "Git_creds": general_status}
        result.write(json.dumps(res))
