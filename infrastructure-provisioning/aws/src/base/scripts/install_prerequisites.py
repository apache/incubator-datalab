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

from fabric.api import *
import argparse
import json
from dlab.fab import *
import sys


parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='edge')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--apt_packages', type=str, default='linux-headers-generic python-pip python-dev groff vim less git wget sysv-rc-conf')
parser.add_argument('--pip_packages', type=str, default='boto3 boto argparse fabric jupyter awscli')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = 'ubuntu@' + args.hostname
    deeper_config = json.loads(args.additional_config)

    print "Updating repositories and installing requested tools: " + args.apt_packages
    if not ensure_apt(args.apt_packages):
        sys.exit(1)

    print "Installing python packages: " + args.pip_packages
    if not ensure_pip(args.pip_packages):
        sys.exit(1)

    sys.exit(0)

