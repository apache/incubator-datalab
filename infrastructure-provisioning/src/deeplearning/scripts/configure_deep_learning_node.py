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

from dlab.actions_lib import *
from dlab.common_lib import *
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *
from fabric.contrib.files import exists
import argparse


parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


templates_dir = '/root/templates/'


def configure_tensor(args):
    sudo('mkdir /var/log/tensorboard; chown ' + args.os_user + ':' + args.os_user + ' -R /var/log/tensorboard')
    put(templates_dir + 'tensorboard.service', '/tmp/tensorboard.service')
    sudo("sed -i 's|OS_USR|" + args.os_user + "|' /tmp/tensorboard.service")
    sudo("chmod 644 /tmp/tensorboard.service")
    sudo('\cp /tmp/tensorboard.service /etc/systemd/system/')
    sudo("systemctl daemon-reload")
    sudo("systemctl enable tensorboard")
    sudo("systemctl start tensorboard")


if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    print "Configuring TensorFlow"
    configure_tensor(args)
