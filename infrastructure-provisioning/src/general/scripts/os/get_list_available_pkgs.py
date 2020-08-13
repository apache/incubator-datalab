#!/usr/bin/python

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

import sys
import argparse
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *
import json
import xmlrpclib


parser = argparse.ArgumentParser()
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--instance_ip', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--group', type=str, default='')
args = parser.parse_args()


def get_available_pip_pkgs(version):
    try:
        for _ in range(100):
            pip_pkgs = dict()
            client = xmlrpclib.ServerProxy('https://pypi.python.org/pypi')
            raw_pkgs = client.browse(["Programming Language :: Python :: " + version + ""])
            all_pkgs = [i[0] for i in raw_pkgs]
            if len(all_pkgs) != 0:
                for pkg in all_pkgs:
                    pip_pkgs[pkg] = "N/A"
                return pip_pkgs
            else:
                local('sleep 5')
                continue
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def get_uncategorised_pip_pkgs(all_pkgs_pip2, all_pkgs_pip3):
    try:
        pip_pkgs = dict()
        client = xmlrpclib.ServerProxy('https://pypi.python.org/pypi')
        raw_pkgs = client.list_packages()
        all_pkgs_other = []
        for pkg in raw_pkgs:
            if pkg not in all_pkgs_pip2 and pkg not in all_pkgs_pip3:
                all_pkgs_other.append(pkg)
        for pkg in all_pkgs_other:
            pip_pkgs[pkg] = "N/A"
        return pip_pkgs
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


if __name__ == "__main__":
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = '{}@{}'.format(args.os_user, args.instance_ip)
    all_pkgs = dict()
    if args.group == 'os_pkg':
        all_pkgs['os_pkg'] = get_available_os_pkgs()
    elif args.group == 'java':
        all_pkgs['java'] = {}
    elif args.group == 'pip2':
        all_pkgs['pip2'] = get_available_pip_pkgs("2.7")
    elif args.group == 'pip3':
        all_pkgs['pip3'] = get_available_pip_pkgs("3.6")
    elif args.group == 'others':
        all_pkgs['pip2'] = get_available_pip_pkgs("2.7")
        all_pkgs['pip3'] = get_available_pip_pkgs("3.6")
        all_pkgs['others'] = get_uncategorised_pip_pkgs(all_pkgs['pip2'], all_pkgs['pip3'])
    elif args.group == 'r_pkg':
        all_pkgs['r_pkg'] = get_available_r_pkgs()

    # Writing response file & json file with all pkgs
    with open("/root/result.json", 'w') as result:
        res = {"Action": "Get list of all available libraries"}
        result.write(json.dumps(res))

    with open("/root/all_pkgs.json", 'w') as result:
        result.write(json.dumps(all_pkgs))
