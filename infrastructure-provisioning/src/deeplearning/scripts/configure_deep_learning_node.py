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
parser.add_argument('--jupyter_version', type=str, default='')
args = parser.parse_args()


jupyter_conf_file = '/home/' + args.os_user + '/.local/share/jupyter/jupyter_notebook_config.py'
templates_dir = '/root/templates/'


def configure_tensor(args):
    tensor_board_started = False
    sudo('apt-add-repository -y ppa:pi-rho/security')
    sudo('apt-get update')
    sudo('apt-get install -y nmap')
    sudo('mkdir /var/log/tensorboard; chown ' + args.os_user + ':' + args.os_user + ' -R /var/log/tensorboard')
    put(templates_dir + 'tensorboard.conf', '/tmp/tensorboard.conf')
    sudo("sed -i 's|OS_USR|" + args.os_user + "|' /tmp/tensorboard.conf")
    sudo("chmod 644 /tmp/tensorboard.conf")
    sudo('\cp /tmp/tensorboard.conf /etc/init/')
    sudo('\cp /tmp/tensorboard.conf /etc/init.d/tensorboard')
    sudo('update-rc.d tensorboard defaults')
    sudo('update-rc.d tensorboard enable')
    sudo('service tensorboard start')
    while not tensor_board_started:
        tensor_port = sudo('nmap -p 6006 localhost | grep "closed" > /dev/null; echo $?')
        tensor_port = tensor_port[:1]
        if tensor_port == '1':
            tensor_board_started = True
        else:
            print "Tensor Board is still starting."
            sudo('sleep 5')


if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    print "Configuring Deep Learning node."
    try:
        if not exists('/home/' + args.os_user + '/.ensure_dir'):
            sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)

    print "Configuring TensorFlow"
    configure_tensor(args)

    print "Configuring Jupyter"
    configure_jupyter(args.os_user, jupyter_conf_file, templates_dir, args.jupyter_version)
