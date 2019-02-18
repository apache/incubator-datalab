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
from dlab.common_lib import update_apt_repository_configuration
from dlab.common_lib import add_repository_cert
from fabric.contrib.files import exists
import sys
import os


parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--pip_packages', type=str, default='boto3 argparse fabric==1.14.0 awscli google-api-python-client google-auth-httplib2 google-cloud-storage pycrypto azure==2.0.0')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--user', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


def update_pip_repository_configuration(repository_host):
    if not exists('/home/{}/pip_conf_update_ensured'.format(args.user)):
        sudo('touch /etc/pip.conf')
        sudo('echo "[global]" > /etc/pip.conf')
        sudo('echo "timeout = 600" >> /etc/pip.conf')
        sudo('echo "index-url = https://{}/simple/" >> /etc/pip.conf'.format(repository_host))
        sudo('echo "trusted-host = {}" >> /etc/pip.conf'.format(repository_host.split('/')[0]))
        sudo('touch /home/{}/pip_conf_update_ensured'.format(args.user))


if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = '{}@{}'.format(args.user, args.hostname)
    deeper_config = json.loads(args.additional_config)

    if args.region == 'cn-north-1':
        update_apt_repository_configuration('http://mirrors.aliyun.com/ubuntu/')
        update_pip_repository_configuration(os.environ['conf_pypi_mirror'])

    if 'local_repository_cert_path' in os.environ:
        add_repository_cert()
    if 'local_repository_host' in os.environ:
        update_apt_repository_configuration(os.environ['local_repository_host'])
        update_pip_repository_configuration('{}/{}/{}'.format(os.environ['local_repository_host'],
                                                              os.environ['local_repository_prefix'],
                                                              os.environ['local_repository_pypi_repo']))

    print("Updating hosts file")
    update_hosts_file(args.user)

    print("Updating repositories and installing requested tools.")
    ensure_pkg(args.user)

    print("Installing python packages: {}".format(args.pip_packages))
    ensure_pip(args.pip_packages)


