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

jupyter_dir = '/home/' + args.os_user + '/.jupyter/'

def start_jupyter_container():
    try:
        with cd('{}'.format(jupyter_dir))
            run('docker volume create -d local-persist -o mountpoint=/opt --name=jup_volume')
            run('docker build --file /home/dlab-user/jupyter/Dockerfile.jupyter -t jupyter-notebook .')
            run('docker run -d -p 8888:8888 -v jup_volume:/opt/ jupyter-notebook:latest')

if __name__ == "__main__":
    print("Starting Jupyter container")
    try:
        start_jupyter_container()
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

