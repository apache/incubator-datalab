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
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--initial_user', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--sudo_group', type=str, default='')
args = parser.parse_args()


def resolving_hosts(initial_user):
    if not exists('/home/{}/.hosts_resolved'.format(initial_user)):
        host = sudo('curl http://169.254.169.254/latest/meta-data/local-hostname').split('\n')
        if len(host) > 1:
            host = host[1]
        else:
            host = host[0]
        host_short = host.split('.')[0]
        private_ip = sudo('curl http://169.254.169.254/latest/meta-data/local-ipv4').split('\n')
        if len(private_ip) > 1:
            private_ip = private_ip[1]
        else:
            private_ip = private_ip[0]
        sudo('echo "{} {} {}" >> /etc/hosts'.format(private_ip, host, host_short))
        sudo('touch /home/{}/.hosts_resolved'.format(initial_user))

def ensure_ssh_user(initial_user, os_user, sudo_group):
    if not exists('/home/{}/.ssh_user_ensured'.format(initial_user)):
        sudo('useradd -m -G {1} -s /bin/bash {0}'.format(os_user, sudo_group))
        sudo('echo "{} ALL = NOPASSWD:ALL" >> /etc/sudoers'.format(os_user))
        sudo('mkdir /home/{}/.ssh'.format(os_user))
        sudo('chown -R {0}:{0} /home/{1}/.ssh/'.format(initial_user, os_user))
        sudo('cat /home/{0}/.ssh/authorized_keys > /home/{1}/.ssh/authorized_keys'.format(initial_user, os_user))
        sudo('chown -R {0}:{0} /home/{0}/.ssh/'.format(os_user))
        sudo('chmod 700 /home/{0}/.ssh'.format(os_user))
        sudo('chmod 600 /home/{0}/.ssh/authorized_keys'.format(os_user))
        sudo('mkdir /home/{}/.ensure_dir'.format(os_user))
        sudo('touch /home/{}/.ssh_user_ensured'.format(initial_user))

if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = '{}@{}'.format(args.initial_user, args.hostname)

    print("Resolving hosts")
    try:
        resolving_hosts(args.initial_user)
    except Exception as err:
        print('Failed to resolve hosts', str(err))
        sys.exit(1)

    print("Creating ssh user: {}".format(args.os_user))
    try:
        ensure_ssh_user(args.initial_user, args.os_user, args.sudo_group)
    except Exception as err:
        print('Failed to create ssh user', str(err))
        sys.exit(1)

