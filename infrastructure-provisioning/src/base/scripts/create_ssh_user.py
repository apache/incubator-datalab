#!/usr/bin/python3

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

from fabric import *
from patchwork.files import exists
from patchwork import files
from datalab.fab import *
import argparse
import sys
import time
import traceback
from patchwork.files import exists
from patchwork import files

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--initial_user', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--sudo_group', type=str, default='')
args = parser.parse_args()


def ensure_ssh_user(initial_user, os_user, sudo_group):
    if not exists(conn, '/home/{}/.ssh_user_ensured'.format(initial_user)):
        conn.sudo('useradd -m -G {1} -s /bin/bash {0}'.format(os_user, sudo_group))
        conn.sudo('bash -c "echo \'{} ALL = NOPASSWD:ALL\' >> /etc/sudoers"'.format(os_user))
        conn.sudo('mkdir /home/{}/.ssh'.format(os_user))
        conn.sudo('chown -R {0}:{0} /home/{1}/.ssh/'.format(initial_user, os_user))
        conn.sudo('''bash -c 'cat /home/{0}/.ssh/authorized_keys > /home/{1}/.ssh/authorized_keys' '''.format(initial_user, os_user))
        conn.sudo('chown -R {0}:{0} /home/{0}/.ssh/'.format(os_user))
        conn.sudo('chmod 700 /home/{0}/.ssh'.format(os_user))
        conn.sudo('chmod 600 /home/{0}/.ssh/authorized_keys'.format(os_user))
        conn.sudo('mkdir /home/{}/.ensure_dir'.format(os_user))
        conn.sudo('touch /home/{}/.ssh_user_ensured'.format(initial_user))

if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.initial_user, args.keyfile, args.os_user)
    print("Creating ssh user: {}".format(args.os_user))
    try:
        ensure_ssh_user(args.initial_user, args.os_user, args.sudo_group)
    except Exception as err:
        print('Failed to create ssh user', str(err))
        sys.exit(1)
    conn.close()
