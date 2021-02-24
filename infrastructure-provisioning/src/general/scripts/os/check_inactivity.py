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
import json
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--instance_ip', type=str, default='')
parser.add_argument('--resource_type', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--dataengine_ip', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.instance_ip, args.os_user, args.keyfile)

    inactivity_dir = '/opt/inactivity/'
    if args.resource_type == 'dataengine':
        reworked_ip=args.dataengine_ip.replace('.','-')
        inactivity_file = '{}_inactivity'.format(reworked_ip)
    else:
        inactivity_file = 'local_inactivity'

    if exists(conn, '{}{}'.format(inactivity_dir, inactivity_file)):
        timestamp = conn.sudo('cat {}{}'.format(inactivity_dir, inactivity_file)).stdout.replace('\n','')
    else:
        timestamp = '0000000000'


    with open('/root/result.json', 'w') as outfile:
        json.dump(timestamp, outfile)

    conn.close()
