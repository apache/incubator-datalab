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
from fabric.contrib.files import exists
import sys


def ensure_pkg(user, requisites='linux-headers-generic python-pip python-dev '
                                'groff gcc vim less git wget sysv-rc-conf '
                                'libssl-dev unattended-upgrades nmap '
                                'libffi-dev unzip libxml2-dev'):
    try:
        if not exists('/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
            print("Updating repositories "
                  "and installing requested tools: {}".format(requisites))
            sudo('apt-get update')
            sudo('apt-get -y install ' + requisites)
            sudo('unattended-upgrades -v')
            sudo('export LC_ALL=C')
            sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
        return True
    except:
        return False


def renew_gpg_key():
    try:
        sudo('mv /etc/apt/trusted.gpg /etc/apt/trusted.bkp')
        sudo('apt-key update')
    except:
        sys.exit(1)


def change_pkg_repos():
    if not exists('/tmp/pkg_china_ensured'):
        put('/root/files/sources.list', '/tmp/sources.list')
        sudo('mv /tmp/sources.list /etc/apt/sources.list')
        sudo('apt-get update')
        sudo('touch /tmp/pkg_china_ensured')


def find_java_path_remote():
    java_path = sudo("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"")
    return java_path


def find_java_path_local():
    java_path = local("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"", capture=True)
    return java_path
