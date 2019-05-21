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

import sys
import os
from dlab.notebook_lib import *
from dlab.fab import *
from fabric.api import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

jupyter_dir = '/home/' + args.os_user + '/.jupyter/'

def start_jupyter_container(jupyter_dir):
    try:
        with cd('{}'.format(jupyter_dir)):
            run('docker volume create -d local-persist -o mountpoint=/opt --name=jup_volume')
            run('docker build --file /home/{}/.jupyter/Dockerfile_jupyter -t jupyter-notebook .'.format(args.os_user))
            run('docker run -d --restart unless-stopped -p 8888:8888 -v jup_volume:/opt/repo jupyter-notebook:latest')
    except: sys.exit(1)

if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname
    print("Starting Jupyter container")
    try:
        start_jupyter_container(jupyter_dir)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

