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
parser.add_argument('--node_name', type=str, default='')
parser.add_argument('--ami_id', type=str, default='')
parser.add_argument('--instance_type', type=str, default='')
parser.add_argument('--key_name', type=str, default='')
parser.add_argument('--security_group_ids', type=str, default='')
parser.add_argument('--subnet_id', type=str, default='')
parser.add_argument('--iam_profile', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--user_data_file', type=str, default='')
parser.add_argument('--instance_class', type=str, default='')
parser.add_argument('--instance_disk_size', type=str, default='')
parser.add_argument('--primary_disk_size', type=str, default='12')
args = parser.parse_args()


if __name__ == "__main__":
    instance_tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    if args.node_name != '':
        try:
            instance_id = get_instance_by_name(args.infra_tag_name, args.node_name)
            if instance_id == '':
                logging.info("Creating instance {0} of type {1} in subnet {2} with tag {3}.".
                      format(args.node_name, args.instance_type, args.subnet_id, json.dumps(instance_tag)))
                instance_id = create_instance(args, instance_tag, args.primary_disk_size)
            else:
                logging.info("REQUESTED INSTANCE ALREADY EXISTS AND RUNNING")
            logging.info("Instance_id {}".format(instance_id))
            logging.info("Public_hostname {}".format(get_instance_attr(instance_id, 'public_dns_name')))
            logging.info("Private_hostname {}".format(get_instance_attr(instance_id, 'private_dns_name')))
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
