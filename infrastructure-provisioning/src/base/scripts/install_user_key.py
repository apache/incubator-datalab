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
from dlab.fab import *
import argparse
import json
import sys
import os

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--user', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


def copy_key(config):
    admin_key_pub = local('ssh-keygen -y -f {}'.format(args.keyfile),
                          capture=True)
    sudo('rm -f /home/{}/.ssh/authorized_keys'.format(args.user))
    sudo('echo "{0}" >> /home/{1}/.ssh/authorized_keys'.format(admin_key_pub, args.user))
    try:
        user_key = '{}{}.pub'.format(
            config.get('user_keydir'),
            config.get('user_keyname'))
        print(user_key)
        key = open('{0}'.format(user_key)).read()
        sudo('echo "{0}" >> /home/{1}/.ssh/authorized_keys'.format(key, args.user))
    except:
        print('No user key')

##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = '{}@{}'.format(args.user, args.hostname)
        deeper_config = json.loads(args.additional_config)
    except:
        print('Fail connection')
        sys.exit(2)

    print("Ensuring safest ssh ciphers")
    ensure_ciphers()

    print("Installing users key...")
    try:
        copy_key(deeper_config)
    except:
        print("Users keyfile {0} could not be found at {1}/{0}".format(args.keyfile, deeper_config['user_keydir']))
        sys.exit(1)

