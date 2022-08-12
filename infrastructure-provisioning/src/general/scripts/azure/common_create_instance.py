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
parser.add_argument('--instance_size', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--vpc_name', type=str, default='')
parser.add_argument('--network_interface_name', type=str, default='')
parser.add_argument('--subnet_name', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--datalab_ssh_user_name', type=str, default='')
parser.add_argument('--public_ip_name', type=str, default='')
parser.add_argument('--public_key', type=str, default='')
parser.add_argument('--primary_disk_size', type=str, default='')
parser.add_argument('--security_group_name', type=str, default='')
parser.add_argument('--instance_type', type=str, default='')
parser.add_argument('--tags', type=str, default='{"empty":"string"}')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--image_name', type=str, default='')
parser.add_argument('--image_type', type=str, default='default')
parser.add_argument('--instance_storage_account_type', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    disk_id = ''
    create_option = 'fromImage'
    if args.instance_name != '':
        try:
            if AzureMeta().get_instance(args.resource_group_name, args.instance_name):
                logging.info("REQUESTED INSTANCE {} ALREADY EXISTS".format(args.instance_name))
            else:
                if args.public_ip_name != 'None':
                    if AzureMeta().get_static_ip(args.resource_group_name, args.public_ip_name):
                        logging.info("REQUESTED PUBLIC IP ADDRESS {} ALREADY EXISTS.".format(args.public_ip_name))
                        static_public_ip_address = AzureMeta().get_static_ip(
                            args.resource_group_name, args.public_ip_name).ip_address
                    else:
                        logging.info("Creating Static IP address {}".format(args.public_ip_name))
                        static_public_ip_address = \
                            AzureActions().create_static_public_ip(args.resource_group_name, args.public_ip_name,
                                                                   args.region, args.instance_name,
                                                                   json.loads(args.tags))
                if AzureMeta().get_network_interface(args.resource_group_name, args.network_interface_name):
                    logging.info("REQUESTED NETWORK INTERFACE {} ALREADY EXISTS.".format(args.network_interface_name))
                    network_interface_id = AzureMeta().get_network_interface(args.resource_group_name,
                                                                             args.network_interface_name).id
                else:
                    logging.info("Creating Network Interface {}".format(args.network_interface_name))
                    network_interface_id = AzureActions().create_network_if(args.resource_group_name, args.vpc_name,
                                                                            args.subnet_name,
                                                                            args.network_interface_name, args.region,
                                                                            args.security_group_name,
                                                                            json.loads(args.tags),
                                                                            args.public_ip_name)
                disk = AzureMeta().get_disk(args.resource_group_name, '{}-volume-primary'.format(
                    args.instance_name))
                if disk:
                    create_option = 'attach'
                    disk_id = disk.id
                logging.info("Creating instance {}".format(args.instance_name))
                AzureActions().create_instance(args.region, args.instance_size, args.service_base_name,
                                               args.instance_name, args.datalab_ssh_user_name, args.public_key,
                                               network_interface_id, args.resource_group_name, args.primary_disk_size,
                                               args.instance_type, args.image_name, json.loads(args.tags),
                                               args.project_name,
                                               create_option, disk_id, args.instance_storage_account_type,
                                               args.image_type)
                for disk in AzureMeta().list_disks(args.resource_group_name):
                    if "SBN" in disk.tags and args.service_base_name == disk.tags["SBN"]:
                        AzureActions().update_disk_access(resource_group_name=args.resource_group_name, disk_name=disk.name)
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
