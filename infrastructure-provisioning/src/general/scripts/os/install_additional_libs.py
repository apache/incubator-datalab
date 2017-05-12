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
import os
from dlab.actions_lib import *
from dlab.common_lib import *
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *
import json


parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--libs', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    print 'Installing libraries:' + args.libs
    fail_on = ""

    pkgs = json.loads(args.libs.replace("'", "\""))

    try:
        print 'Installing os packages:', pkgs['libraries']['os_pkg']
        status, fail_pkg = install_os_pkg(pkgs['libraries']['os_pkg'])
        print status, fail_pkg
        if not status:
            fail_on += " os_pkg:" + fail_pkg
            # sys.exit(1)
    except KeyError:
        pass

    try:
        print 'Installing pip2 packages:', pkgs['libraries']['pip2']
        status, fail_pkg = install_pip2_pkg(pkgs['libraries']['pip2'])
        print status, fail_pkg
        if not status:
            fail_on += " pip2:" + fail_pkg
            # sys.exit(1)
    except KeyError:
        pass

    try:
        print 'Installing pip3 packages:', pkgs['libraries']['pip3']
        status, fail_pkg = install_pip3_pkg(pkgs['libraries']['pip3'])
        print status, fail_pkg
        if not status:
            fail_on += " pip3:" + fail_pkg
            # sys.exit(1)
    except KeyError:
        pass

    if os.environ['application'] in ['jupyter', 'rstudio', 'zeppelin', 'deeplearning']:
        try:
            print 'Installing R packages:', pkgs['libraries']['r_pkg']
            status, fail_pkg = install_r_pkg(pkgs['libraries']['r_pkg'])
            print status, fail_pkg
            print status, fail_pkg
            if not status:
                fail_on += " r_pkg:" + fail_pkg
                # sys.exit(1)
        except KeyError:
            pass


    with open("/root/result.json", 'w') as result:
        res = {"Action": "Install additional libs",
               "Libs": args.libs,
               "Fail": fail_on}
        result.write(json.dumps(res))
