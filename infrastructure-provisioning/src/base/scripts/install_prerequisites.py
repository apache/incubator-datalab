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
import os
from datalab.common_lib import *
from datalab.fab import *
from fabric import *
from patchwork.files import exists
from patchwork import files
import traceback

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--pip_packages', type=str,
                    default='boto3 argparse fabric awscli google-api-python-client google-auth-httplib2 google-cloud-storage pycryptodome azure==2.0.0')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--user', type=str, default='')
parser.add_argument('--edge_private_ip', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = init_datalab_connection(args.hostname, args.user, args.keyfile)
    deeper_config = json.loads(args.additional_config)

    print("Updating hosts file")
    update_hosts_file(args.user)

    print("Updating repositories and installing requested tools.")
    try:
        ensure_pkg(args.user)
    except:
        traceback.print_exc()
        sys.exit(1)

    print("Installing python packages: {}".format(args.pip_packages))
    ensure_pip(args.pip_packages)

    print("Installing NTPd")
    ensure_ntpd(args.user, args.edge_private_ip)

    conn.close()
