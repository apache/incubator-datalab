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
import subprocess
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from datalab.logger import logging
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--conf_resource', type=str, default='')
parser.add_argument('--instance_id', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    list_private_ips = GCPMeta().get_list_private_ip_by_conf_type_and_id(
        args.conf_resource, args.instance_id)
    
    for ip in list_private_ips:
        params = "--user {} --hostname {} --keyfile '{}' --additional_config '{}'".format(
            args.os_user, ip, args.keyfile, args.additional_config)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)
