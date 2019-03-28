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

from fabric.api import *
from fabric.contrib.files import exists
import sys
import os
import time


def ensure_pkg(user, requisites='linux-headers-generic python-pip python-dev '
                                'groff gcc vim less git wget sysv-rc-conf '
                                'libssl-dev unattended-upgrades nmap '
                                'libffi-dev unzip libxml2-dev haveged'):
    try:
        if not exists('/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
            print("Updating repositories "
                  "and installing requested tools: {}".format(requisites))
            apt_proccess = sudo('ps aux | grep apt | grep -v grep | wc -l')
            while apt_proccess != '0':
                time.sleep(10)
                apt_proccess = sudo('ps aux | grep apt | grep -v grep | wc -l')
            sudo('rm -f /var/lib/apt/lists/lock')
            sudo('apt-get update')
            sudo('apt-get -y install ' + requisites)
            sudo('unattended-upgrades -v')
            sudo('export LC_ALL=C')
            sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
            sudo('systemctl enable haveged')
            sudo('systemctl start haveged')
            if os.environ['conf_cloud_provider'] == 'aws':
                sudo('apt-get -y install --install-recommends linux-aws-hwe')
    except:
        time.sleep(3600)
        sys.exit(1)


def renew_gpg_key():
    try:
        sudo('rm -f /var/lib/apt/lists/lock')
        sudo('mv /etc/apt/trusted.gpg /etc/apt/trusted.bkp')
        sudo('apt-key update')
    except:
        sys.exit(1)


def update_apt_repository_configuration(repository_host=''):
    if not exists('/tmp/apt_conf_update_ensured'):
        sudo('echo "apt_preserve_sources_list: true" >> /etc/cloud/cloud.cfg')
        put('/root/files/sources.list', '/tmp/sources.list')
        sudo('mv /tmp/sources.list /etc/apt/sources.list')
        if os.environ['local_repository_enabled'] == 'True':
            sudo('sed -i "s|REPOSITORY_UBUNTU|{0}/|g" /etc/apt/sources.list'.format(
                 os.environ['local_repository_apt_ubuntu_repo']))
            sudo('sed -i "s|REPOSITORY_SECURITY_UBUNTU|{0}/|g" /etc/apt/sources.list'.format(
                os.environ['local_repository_apt_ubuntu_security_repo']))
        else:
            sudo('sed -i "s|REPOSITORY_UBUNTU|{}|g" /etc/apt/sources.list'.format(repository_host))
            sudo('sed -i "s|REPOSITORY_SECURITY_UBUNTU|{}|g" /etc/apt/sources.list'.format(repository_host))
        sudo('apt-get update')
        sudo('touch /tmp/apt_conf_update_ensured')


def add_repository_cert():
    if not exists('/tmp/repository_cert_added'):
        sudo('mkdir -p /usr/local/share/ca-certificates/repository')
        put('/root/certs/repository.crt', '/usr/local/share/ca-certificates/repository/repository.crt', use_sudo=True)
        sudo('update-ca-certificates')
        sudo('touch /tmp/repository_cert_added')


def find_java_path_remote():
    java_path = sudo("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"")
    return java_path


def find_java_path_local():
    java_path = local("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"", capture=True)
    return java_path
