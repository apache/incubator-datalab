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
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--cluster_ip', type=str, default='none')
args = parser.parse_args()


if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)

    if args.cluster_ip == "none":
        kernel = 'local'
    else:
        kernel = args.cluster_ip.replace('.', '-')

    if os.environ['conf_cloud_provider'] == 'azure':
        datalab.actions_lib.ensure_right_mount_paths()
        if exists(conn, '/etc/systemd/system/zeppelin-notebook.service'):
            conn.sudo('systemctl restart zeppelin-notebook.service')
    conn.sudo('''bash -c -l "date +%s > /opt/inactivity/{}_inactivity" '''.format(kernel))

    conn.close()
