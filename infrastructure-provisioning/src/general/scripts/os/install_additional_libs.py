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


def parse_json_libs(libs):
    try:
        libs_list = dict()
        data = json.loads(libs.replace("'","\""))
        node = data['libraries']
        for n in node:
            key = n['Name']
            value = n['Type']
            libs_list[key] = value
        return libs_list
    except:
        traceback.print_exc()
        raise Exception

def install_libs(libraries):
    try:
        print "Installing additional libraries."

        os_pkg_libs = ''
        pip2_libs = ''
        pip3_libs = ''
        r_pkg_libs = list()

        for lib in libraries:
            if libraries[lib] == "os_pkg":
                os_pkg_libs += " " + lib
            elif libraries[lib] == "pip2":
                pip2_libs += " " + lib
            elif libraries[lib] == "pip3":
                pip3_libs += " " + lib
            elif libraries[lib] == "r_pkg":
                r_pkg_libs.append(lib)

        if os_pkg_libs:
            logging.info('Installing os packages.')
            if not install_os_pkg(os_pkg_libs):
                sys.exit(1)

        if pip2_libs:
            logging.info('Installing pip2 packages.')
            if not install_pip2_pkg(pip2_libs):
                sys.exit(1)

        if pip3_libs:
            logging.info('Installing pip3 packages.')
            if not install_pip3_pkg(pip3_libs):
                sys.exit(1)

        if os.environ['application'] in ['jupyter', 'rstudio', 'zeppelin', 'deeplearning']:
            if r_pkg_libs:
                logging.info('Installing R packages.')
                if not install_r_pkg(r_pkg_libs):
                    sys.exit(1)

    except:
        traceback.print_exc()
        raise Exception

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    env.hosts = "{}".format(args.notebook_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    libraries = parse_json_libs(args.libs)

    print "Installing libraries: " + args.libs
    logging.info('Installing libraries:' + args.libs)
    install_libs(libraries)

    with open("/root/result.json", 'w') as result:
        res = {"Action": "Install additional libs",
               "Libs": str(libraries)}
        result.write(json.dumps(res))
