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
import sys
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--instance_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--zone', type=str, default='')
parser.add_argument('--vpc_name', type=str, default='')
parser.add_argument('--subnet_name', type=str, default='')
parser.add_argument('--instance_size', type=str, default='')
parser.add_argument('--ssh_key_path', type=str, default='')
parser.add_argument('--initial_user', type=str, default='')
parser.add_argument('--service_account_name', type=str, default='')
parser.add_argument('--image_name', type=str, default='')
parser.add_argument('--secondary_image_name', type=str, default='')
parser.add_argument('--primary_disk_size', type=str, default='20')
parser.add_argument('--secondary_disk_size', type=str, default='30')
parser.add_argument('--instance_class', type=str, default='')
parser.add_argument('--static_ip', type=str, default='')
parser.add_argument('--labels', type=str, default='{"empty":"string"}')
parser.add_argument('--gpu_accelerator_type', type=str, default='None')
parser.add_argument('--gpu_accelerator_count', type=str, default='None')
parser.add_argument('--network_tag', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--os_login_enabled', type=str, default='FALSE')
parser.add_argument('--block_project_ssh_keys', type=str, default='FALSE')
parser.add_argument('--rsa_encrypted_csek', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.instance_name:
        if GCPMeta().get_instance(args.instance_name):
            logging.info("REQUESTED INSTANCE {} ALREADY EXISTS".format(args.instance_name))
        else:
            logging.info("Creating Instance {}".format(args.instance_name))
            GCPActions().create_instance(args.instance_name, args.service_base_name, args.cluster_name, args.region, args.zone,
                                         args.vpc_name, args.subnet_name,
                                         args.instance_size, args.ssh_key_path, args.initial_user, args.image_name,
                                         args.secondary_image_name, args.service_account_name, args.instance_class,
                                         args.network_tag, json.loads(args.labels), args.static_ip,
                                         args.primary_disk_size, args.secondary_disk_size, args.gpu_accelerator_type,
                                         args.gpu_accelerator_count, args.os_login_enabled, args.block_project_ssh_keys,
                                         args.rsa_encrypted_csek)
    else:
        parser.print_help()
        sys.exit(2)
