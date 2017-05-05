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

import sys
import argparse
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *
import json
import xmlrpclib

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def get_available_pip_pkgs(version):
    pip_pkgs = dict()
    client = xmlrpclib.ServerProxy('https://pypi.python.org/pypi')
    all_pkgs = client.browse(["Programming Language :: Python :: " + version + ""])
    for pkg in all_pkgs:
        pip_pkgs[pkg] = "N/A"

    return pip_pkgs


# def get_available_r_pkgs():
#     r_pkgs = dict()
#     status, output = commands.getstatusoutput("sudo R -e 'available.packages(contriburl=\"http://cran.us.r-project.org/src/contrib\")'")


if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = '{}@{}'.format(args.os_user, args.hostname)

    all_pkgs = dict()
    all_pkgs['os_pkg'] = get_available_os_pkgs() # from notebook_lib
    all_pkgs['pip2'] = get_available_pip_pkgs("2.7")
    all_pkgs['pip3'] = get_available_pip_pkgs("3.5")

    with open("/root/all_pkgs.json", 'w') as result:
        result.write(json.dumps(all_pkgs))
