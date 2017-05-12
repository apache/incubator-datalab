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


def install_libs(libraries):
    try:
        os_pkg_libs = list()
        pip2_libs = list()
        pip3_libs = list()
        r_pkg_libs = list()

        pkgs = json.loads(libraries.replace("'", "\""))

        if "os_pkg" in pkgs['libraries'].keys():
            for pkg in pkgs['libraries']['os_pkg']:
                os_pkg_libs.append(pkg)
        if "pip2" in pkgs['libraries'].keys():
            for pkg in pkgs['libraries']['pip2']:
                pip2_libs.append(pkg)
        if "pip3" in pkgs['libraries'].keys():
            for pkg in pkgs['libraries']['pip3']:
                pip3_libs.append(pkg)
        if "r_pkg" in pkgs['libraries'].keys():
            for pkg in pkgs['libraries']['r_pkg']:
                r_pkg_libs.append(pkg)

        if os_pkg_libs:
            print 'Installing os packages:', os_pkg_libs
            if not install_os_pkg(os_pkg_libs):
                sys.exit(1)

        if pip2_libs:
            print 'Installing pip2 packages:', pip2_libs
            if not install_pip2_pkg(pip2_libs):
                sys.exit(1)

        if pip3_libs:
            print 'Installing pip3 packages:', pip3_libs
            if not install_pip3_pkg(pip3_libs):
                sys.exit(1)

        if os.environ['application'] in ['jupyter', 'rstudio', 'zeppelin', 'deeplearning']:
            if r_pkg_libs:
                print 'Installing R packages:', r_pkg_libs
                if not install_r_pkg(r_pkg_libs):
                    sys.exit(1)

    except:
        sys.exit(1)

if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    print 'Installing libraries:' + args.libs
    install_libs(args.libs)

    with open("/root/result.json", 'w') as result:
        res = {"Action": "Install additional libs",
               "Libs": args.libs}
        result.write(json.dumps(res))
