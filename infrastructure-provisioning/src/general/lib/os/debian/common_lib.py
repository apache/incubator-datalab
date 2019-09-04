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
    count = 0
    check = False
    while not check:
        if count > 60:
            print("Repositories are not available. Please, try again later.")
            sys.exit(1)
        else:
            try:
                if not exists('/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
                    print("Updating repositories "
                          "and installing requested tools: {}".format(requisites))
                    print("Attempt number " + str(count) + " to install requested tools. Max 60 tries.")
                    sudo('apt-get update')
                    sudo('apt-get -y install ' + requisites)
                    sudo('unattended-upgrades -v')
                    sudo('export LC_ALL=C')
                    sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
                    sudo('systemctl enable haveged')
                    sudo('systemctl start haveged')
                    if os.environ['conf_cloud_provider'] == 'aws':
                        sudo('apt-get -y install --install-recommends linux-aws-hwe')
                    check = True
            except:
                count += 1
                time.sleep(50)


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


def ensure_ntpd(user, edge_private_ip=''):
    try:
        if not exists('/home/{}/.ensure_dir/ntpd_ensured'.format(user)):
            sudo('timedatectl set-ntp no')
            sudo('apt-get -y install ntp ntpdate')
            sudo('echo "tinker panic 0" >> /etc/ntp.conf')
            if os.environ['conf_resource'] != 'ssn' and os.environ['conf_resource'] != 'edge':
                sudo('echo "server {} prefer iburst" >> /etc/ntp.conf'.format(edge_private_ip))
            sudo('systemctl restart ntp')
            sudo('systemctl enable ntp')
            sudo('touch /home/{}/.ensure_dir/ntpd_ensured'.format(user))
    except:
        sys.exit(1)
