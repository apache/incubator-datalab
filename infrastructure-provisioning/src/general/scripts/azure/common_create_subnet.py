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
import ipaddress
import sys
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--subnet_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--vpc_name', type=str, default='')
parser.add_argument('--prefix', type=str, default='')
parser.add_argument('--vpc_cidr', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    try:
        empty_vpc = False
        private_subnet_size = ipaddress.ip_network(u'0.0.0.0/{}'.format(args.prefix)).num_addresses
        first_vpc_ip = int(ipaddress.IPv4Address(args.vpc_cidr.split('/')[0]))
        subnets_cidr = []
        subnets = AzureMeta().list_subnets(args.resource_group_name, args.vpc_name)
        for subnet in subnets:
            subnets_cidr.append(subnet.address_prefix)
        sorted_subnets_cidr = sorted(subnets_cidr)
        if not subnets_cidr:
            empty_vpc = True

        last_ip = first_vpc_ip
        for cidr in sorted_subnets_cidr:
            first_ip = int(ipaddress.IPv4Address(cidr.split('/')[0]))
            if first_ip - last_ip < private_subnet_size:
                subnet_size = ipaddress.ip_network(u'{}'.format(cidr)).num_addresses
                last_ip = first_ip + subnet_size - 1
            else:
                break
        if empty_vpc:
            datalab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip), args.prefix)
        else:
            datalab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip + 1), args.prefix)
        if args.subnet_name != '':
            if AzureMeta().get_subnet(args.resource_group_name, args.vpc_name, args.subnet_name):
                logging.info("REQUESTED SUBNET {} ALREADY EXISTS".format(args.subnet_name))
            else:
                logging.info("Creating Subnet {}".format(args.subnet_name))
                AzureActions().create_subnet(args.resource_group_name, args.vpc_name, args.subnet_name,
                                             datalab_subnet_cidr)
        else:
            logging.info("Subnet name can't be empty")
            sys.exit(1)
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        sys.exit(1)
