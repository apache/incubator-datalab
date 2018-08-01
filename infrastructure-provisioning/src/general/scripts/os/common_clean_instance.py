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

import os
import sys
import argparse
from fabric.api import *
from dlab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--application', type=str, default='')
args = parser.parse_args()


def general_clean():
    try:
        sudo('systemctl stop ungit')
        sudo('npm -g uninstall ungit')
        sudo('rm -f /etc/systemd/system/ungit.service')
        sudo('systemctl daemon-reload')
        remove_os_pkg(['nodejs', 'npm'])
        sudo('sed -i "/spark.*.memory/d" /opt/spark/conf/spark-defaults.conf')
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

def clean_jupyter():
    try:
        sudo('systemctl stop jupyter-notebook')
        sudo('pip2 uninstall -y notebook jupyter')
        sudo('pip3.5 uninstall -y notebook jupyter')
        sudo('rm -rf /usr/local/share/jupyter/')
        sudo('rm -rf /home/{}/.jupyter/'.format(args.os_user))
        sudo('rm -rf /home/{}/.ipython/'.format(args.os_user))
        sudo('rm -rf /home/{}/.ipynb_checkpoints/'.format(args.os_user))
        sudo('rm -rf /home/{}/.local/share/jupyter/'.format(args.os_user))
        sudo('rm -f /etc/systemd/system/jupyter-notebook.service')
        sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

def clean_zeppelin():
    try:
        sudo('systemctl stop zeppelin-notebook')
        sudo('rm -rf /opt/zeppelin* /var/log/zeppelin /var/run/zeppelin')
        if os.environ['notebook_multiple_clusters'] == 'true':
            sudo('systemctl stop livy-server')
            sudo('rm -rf /opt/livy* /var/run/livy')
            sudo('rm -f /etc/systemd/system/livy-server.service')
        sudo('rm -f /etc/systemd/system/zeppelin-notebook.service')
        sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

def clean_rstudio():
    try:
        remove_os_pkg(['rstudio-server'])
        sudo('rm -f /home/{}/.Rprofile'.format(args.os_user))
        sudo('rm -f /home/{}/.Renviron'.format(args.os_user))
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

def clean_tensor():
    try:
        clean_jupyter()
        sudo('systemctl stop tensorboard')
        sudo('systemctl disable tensorboard')
        sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

def clean_tensor_rstudio():
    try:
        clean_rstudio()
        sudo('systemctl stop tensorboard')
        sudo('systemctl disable tensorboard')
        sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

if __name__ == "__main__":
    print('Configure connections')
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    if os.environ['conf_cloud_provider'] == 'azure':
         de_master_name = '{}-{}-de-{}-{}-m'.format(
            os.environ['conf_service_base_name'],
            os.environ['edge_user_name'].replace("_", "-"),
            os.environ['exploratory_name'].replace("_", "-"),
            os.environ['computational_name'].replace("_", "-"))
         de_ami_id = AzureMeta().get_instance_image(os.environ['azure_resource_group_name'],
            de_master_name)
         default_ami_id = 'default'
    else:
        de_master_name = '{}-{}-de-{}-{}-m'.format(
            os.environ['conf_service_base_name'],
            os.environ['edge_user_name'],
            os.environ['exploratory_name'],
            os.environ['computational_name'])
        de_ami_id = get_ami_id_by_instance_name(de_master_name)
        default_ami_id = get_ami_id(
            os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])])

    if de_ami_id != default_ami_id:
        if args.application in os.environ['dataengine_image_notebooks'].split(','):
            general_clean()
            if args.application == 'jupyter':
                clean_jupyter()
            elif args.application == 'zeppelin':
                clean_zeppelin()
            elif args.application == 'rstudio':
                clean_rstudio()
            elif args.application in ('tensor', 'deeplearning'):
                clean_tensor()
            elif args.application == ('tensor-rstudio'):
                clean_tensor_rstudio()
    else:
        print('Found default ami, do not make clean')

    sys.exit(0)