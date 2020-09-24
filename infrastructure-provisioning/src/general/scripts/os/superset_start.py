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
from datalab.notebook_lib import *
from datalab.fab import *
from fabric.api import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

superset_dir = '/home/' + args.os_user + '/incubator-superset/contrib/docker'

def start_superset(superset_dir):
    try:
        with cd('{}'.format(superset_dir)):
            sudo('docker-compose run --rm superset ./docker-init.sh')
        sudo('cp /opt/datalab/templates/superset-notebook.service /tmp/')
        sudo('sed -i \'s/OS_USER/{}/g\' /tmp/superset-notebook.service'.format(args.os_user))
        sudo('cp /tmp/superset-notebook.service /etc/systemd/system/')
        sudo('systemctl daemon-reload')
        sudo('systemctl enable superset-notebook')
        sudo('systemctl start superset-notebook')
    except: sys.exit(1)

if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname
    print("Starting Superset")
    try:
        start_superset(superset_dir)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

