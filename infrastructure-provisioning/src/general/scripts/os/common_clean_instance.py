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

import argparse
import os
import sys
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--application', type=str, default='')
args = parser.parse_args()


def general_clean():
    try:
        conn.sudo('systemctl stop ungit')
        conn.sudo('systemctl stop inactive.timer')
        conn.sudo('rm -f /etc/systemd/system/inactive.service')
        conn.sudo('rm -f /etc/systemd/system/inactive.timer')
        conn.sudo('rm -rf /opt/inactivity')
        conn.sudo('npm -g uninstall ungit')
        conn.sudo('rm -f /etc/systemd/system/ungit.service')
        conn.sudo('systemctl daemon-reload')
        remove_os_pkg(['nodejs', 'npm'])
        conn.sudo('sed -i "/spark.*.memory/d" /opt/spark/conf/spark-defaults.conf')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def clean_jupyter():
    try:
        conn.sudo('systemctl stop jupyter-notebook')
        conn.sudo('pip3 uninstall -y notebook jupyter')
        conn.sudo('rm -rf /usr/local/share/jupyter/')
        conn.sudo('rm -rf /home/{}/.jupyter/'.format(args.os_user))
        conn.sudo('rm -rf /home/{}/.ipython/'.format(args.os_user))
        conn.sudo('rm -rf /home/{}/.ipynb_checkpoints/'.format(args.os_user))
        conn.sudo('rm -rf /home/{}/.local/share/jupyter/'.format(args.os_user))
        conn.sudo('rm -f /etc/systemd/system/jupyter-notebook.service')
        conn.sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

def clean_jupyterlab():
    try:
        conn.sudo('systemctl stop jupyterlab-notebook')
        conn.sudo('pip3 uninstall -y jupyterlab')
        #conn.sudo('rm -rf /usr/local/share/jupyter/')
        conn.sudo('rm -rf /home/{}/.jupyter/'.format(args.os_user))
        conn.sudo('rm -rf /home/{}/.ipython/'.format(args.os_user))
        conn.sudo('rm -rf /home/{}/.ipynb_checkpoints/'.format(args.os_user))
        conn.sudo('rm -rf /home/{}/.local/share/jupyter/'.format(args.os_user))
        conn.sudo('rm -f /etc/systemd/system/jupyterlab-notebook.service')
        conn.sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

def clean_zeppelin():
    try:
        conn.sudo('systemctl stop zeppelin-notebook')
        conn.sudo('rm -rf /opt/zeppelin* /var/log/zeppelin /var/run/zeppelin')
        if os.environ['notebook_multiple_clusters'] == 'true':
            conn.sudo('systemctl stop livy-server')
            conn.sudo('rm -rf /opt/livy* /var/run/livy')
            conn.sudo('rm -f /etc/systemd/system/livy-server.service')
        conn.sudo('rm -f /etc/systemd/system/zeppelin-notebook.service')
        conn.sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def clean_rstudio():
    try:
        remove_os_pkg(['rstudio-server'])
        conn.sudo('rm -f /home/{}/.Rprofile'.format(args.os_user))
        conn.sudo('rm -f /home/{}/.Renviron'.format(args.os_user))
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)


def clean_tensor():
    try:
        clean_jupyter()
        conn.sudo('systemctl stop tensorboard')
        conn.sudo('systemctl disable tensorboard')
        conn.sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def clean_tensor_rstudio():
    try:
        clean_rstudio()
        conn.sudo('systemctl stop tensorboard')
        conn.sudo('systemctl disable tensorboard')
        conn.sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

def clean_tensor_jupyterlab():
    try:
        clean_jupyterlab()
        conn.sudo('systemctl stop tensorboard')
        conn.sudo('systemctl disable tensorboard')
        conn.sudo('systemctl daemon-reload')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

def clean_deeplearning():
    try:
        conn.sudo('systemctl stop ungit')
        conn.sudo('systemctl stop inactive.timer')
        conn.sudo('rm -f /etc/systemd/system/inactive.service')
        conn.sudo('rm -f /etc/systemd/system/inactive.timer')
        conn.sudo('rm -rf /opt/inactivity')
        conn.sudo('npm -g uninstall ungit')
        conn.sudo('rm -f /etc/systemd/system/ungit.service')
        conn.sudo('systemctl daemon-reload')
        remove_os_pkg(['nodejs', 'npm'])
        conn.sudo('sed -i "/spark.*.memory/d" /opt/spark/conf/spark-defaults.conf')
        # conn.sudo('systemctl stop tensorboard')
        # conn.sudo('systemctl disable tensorboard')
        # conn.sudo('systemctl daemon-reload')
        clean_jupyter()
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


if __name__ == "__main__":
    print('Configure connections')
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)

    if os.environ['conf_cloud_provider'] == 'azure':
        from datalab.actions_lib import ensure_right_mount_paths
        ensure_right_mount_paths()
        de_master_name = '{}-{}-{}-de-{}-m'.format(
            os.environ['conf_service_base_name'],
            os.environ['project_name'],
            os.environ['endpoint_name'],
            os.environ['computational_name'])
        de_ami_id = AzureMeta().get_instance_image(os.environ['azure_resource_group_name'],
                                                   de_master_name)
        default_ami_id = 'default'
    else:
        de_master_name = '{}-{}-{}-de-{}-m'.format(
            os.environ['conf_service_base_name'],
            os.environ['project_name'],
            os.environ['endpoint_name'],
            os.environ['computational_name'])
        de_ami_id = get_ami_id_by_instance_name(de_master_name)
        default_ami_id = get_ami_id(
            os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])])


    if de_ami_id != default_ami_id:
        if args.application in os.environ['dataengine_image_notebooks'].split(','):
            if args.application == 'deeplearning':
                clean_deeplearning()
            else:
                general_clean()
                if args.application == 'jupyter':
                    clean_jupyter()
                elif args.application == 'zeppelin':
                    clean_zeppelin()
                elif args.application == 'rstudio':
                    clean_rstudio()
                elif args.application == 'tensor':
                    clean_tensor()
                elif args.application == 'tensor-rstudio':
                    clean_tensor_rstudio()
                elif args.application == 'tensor-jupyterlab':
                    clean_tensor_jupyterlab()
    else:
        print('Found default ami, do not make clean')
    #conn.close()
    sys.exit(0)