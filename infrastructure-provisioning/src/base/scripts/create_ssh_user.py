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
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--initial_user', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def ensure_ssh_user(initial_user, os_user):
    sudo('useradd -m -G sudo -s /bin/bash {}'.format(os_user))
    sudo('mkdir /home/{}/.ssh'.format(os_user))
    sudo('chown -R {0}:{0} /home/{1}/.ssh/'.format(initial_user, os_user))
    sudo('cat /home/{0}/.ssh/authorized_keys > /home/{1}/.ssh/authorized_keys'.format(initial_user, os_user))
    sudo('chown -R {0}:{0} /home/{0}/.ssh/'.format(os_user))
    sudo('chmod 700 /home/{0}/.ssh'.format(os_user))
    sudo('chmod 600 /home/{0}/.ssh/authorized_keys'.format(os_user))
    sudo('echo "{} ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers.d/90-cloud-init-users'.format(os_user))


if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = '{}@{}'.format(args.initial_user, args.hostname)

    print "Creating ssh user: {}".format(args.os_user)
    try:
        ensure_ssh_user(args.initial_user, args.os_user)
    except:
        sys.exit(1)

