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
import os


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
    except:
        sys.exit(1)


def renew_gpg_key():
    try:
        sudo('mv /etc/apt/trusted.gpg /etc/apt/trusted.bkp')
        sudo('apt-key update')
    except:
        sys.exit(1)


def update_apt_repository_configuration(repository_host):
    if not exists('/tmp/apt_conf_update_ensured'):
        put('/root/files/sources.list', '/tmp/sources.list')
        sudo('mv /tmp/sources.list /etc/apt/sources.list')
        if 'conf_dlab_repository_host' in os.environ:
            sudo('sed -i "s|REPOSITORY_UBUNTU|{}/repository/apt-ubuntu/|g" /etc/apt/sources.list'.format(
                repository_host))
            sudo('sed -i "s|REPOSITORY_SECURITY_UBUNTU|{}/repository/apt-security/|g" /etc/apt/sources.list'.format(
                repository_host))
        else:
            sudo('sed -i "s|REPOSITORY_UBUNTU|{}|g" /etc/apt/sources.list'.format(repository_host))
            sudo('sed -i "s|REPOSITORY_SECURITY_UBUNTU|{}|g" /etc/apt/sources.list'.format(repository_host))
        # sudo('touch /etc/apt/apt.conf.d/98ssl-exceptions')
        # sudo("""echo 'Acquire::https::{}::Verify-Peer "false";' > /etc/apt/apt.conf.d/98ssl-exceptions""".format(
        #     repository_host))
        # sudo("""echo 'Acquire::https::{}::Verify-Host "false";' >> /etc/apt/apt.conf.d/98ssl-exceptions""".format(
        #     repository_host))
        sudo('apt-get update')
        sudo('touch /tmp/apt_conf_update_ensured')


def add_repository_cert():
    try:
        if not exists('/tmp/repository_cert_added'):
            sudo('mkdir -p /usr/local/share/ca-certificates/repository')
            local('ls -l /root/certs')
            put('/root/certs/repository.crt', '/usr/local/share/ca-certificates/repository/repository.crt', use_sudo=True)
            sudo('update-ca-certificates')
            sudo('touch /tmp/repository_cert_added')
    except Exception as err:
        print('Error ---->')
        print(str(err))


def find_java_path_remote():
    java_path = sudo("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"")
    return java_path


def find_java_path_local():
    java_path = local("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"", capture=True)
    return java_path
