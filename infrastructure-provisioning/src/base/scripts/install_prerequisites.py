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
from dlab.common_lib import ensure_pkg
from dlab.common_lib import change_pkg_repos
from fabric.contrib.files import exists
import sys
import os


parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--pip_packages', type=str, default='boto3 argparse fabric awscli google-api-python-client google-auth-httplib2 google-cloud-storage pycrypto azure==2.0.0')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--user', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


def create_china_pip_conf_file():
    if not exists('/home/{}/pip_china_ensured'.format(args.user)):
        sudo('touch /etc/pip.conf')
        sudo('echo "[global]" >> /etc/pip.conf')
        sudo('echo "timeout = 600" >> /etc/pip.conf')
        sudo('echo "index-url = https://{}/simple/" >> /etc/pip.conf'.format(os.environ['conf_pypi_mirror']))
        sudo('echo "trusted-host = {}" >> /etc/pip.conf'.format(os.environ['conf_pypi_mirror']))
        sudo('touch /home/{}/pip_china_ensured'.format(args.user))


if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = '{}@{}'.format(args.user, args.hostname)
    deeper_config = json.loads(args.additional_config)

    if args.region == 'cn-north-1':
        change_pkg_repos()
        create_china_pip_conf_file()

    print("Updating repositories and installing requested tools.")
    if not ensure_pkg(args.user):
        sys.exit(1)

    print("Installing python packages: {}".format(args.pip_packages))
    if not ensure_pip(args.pip_packages):
        sys.exit(1)

    sys.exit(0)

