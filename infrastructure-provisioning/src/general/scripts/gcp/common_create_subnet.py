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
parser.add_argument('--subnet_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--vpc_selflink', type=str, default='')
parser.add_argument('--prefix', type=str, default='')
parser.add_argument('--vpc_cidr', type=str, default='')
parser.add_argument('--ssn', type=bool, default=False)
parser.add_argument('--user_subnets_range', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    region_list = ["asia-east1", "asia-east2", "asia-northeast1", "asia-northeast2", "asia-northeast3",
                   "asia-south1", "asia-south2", "asia-southeast1", "asia-southeast2", "australia-southeast1",
                   "australia-southeast2", "europe-central2", "europe-north1", "europe-southwest1", "europe-west1",
                   "europe-west12", "europe-west2", "europe-west3", "europe-west4", "europe-west6",
                   "europe-west8", "europe-west9", "me-west1", "northamerica-northeast1", "northamerica-northeast2",
                   "southamerica-east1", "southamerica-west1", "us-central1", "us-east1", "us-east4",
                   "us-east5", "us-south1", "us-west1", "us-west2", "us-west3", "us-west4"]
    if args.user_subnets_range == '' or args.ssn:
        empty_vpc = False
        private_subnet_size = ipaddress.ip_network(u'0.0.0.0/{}'.format(args.prefix)).num_addresses
        subnets_cidr = []
        try:
            subnets = GCPMeta().get_vpc(args.vpc_selflink.split('/')[-1])['subnetworks']
        except KeyError:
            empty_vpc = True
            subnets = []
        for subnet in subnets:
            for region in region_list:
                if region in subnet:
                    subnets_cidr.append(GCPMeta().get_subnet(subnet.split('/')[-1], region)['ipCidrRange'])
        sortkey = lambda addr: \
            (int(addr.split("/")[0].split(".")[0]),
             int(addr.split("/")[0].split(".")[1]),
             int(addr.split("/")[0].split(".")[2]),
             int(addr.split("/")[0].split(".")[3]),
             int(addr.split("/")[1]))
        sorted_subnets_cidr = sorted(subnets_cidr, key=sortkey)

        if not empty_vpc:
            last_ip = int(ipaddress.IPv4Address(sorted_subnets_cidr[0].split('/')[0]))
        else:
            last_ip = int(ipaddress.IPv4Address(args.vpc_cidr.split('/')[0]))
        previous_subnet_size = private_subnet_size
        for cidr in sorted_subnets_cidr:
            first_ip = int(ipaddress.IPv4Address(cidr.split('/')[0]))
            if first_ip - last_ip < private_subnet_size or previous_subnet_size < private_subnet_size:
                subnet_size = ipaddress.ip_network(u'{}'.format(cidr)).num_addresses
                last_ip = first_ip + subnet_size - 1
                previous_subnet_size = subnet_size
            else:
                break

        datalab_subnet_cidr = ''
        if empty_vpc:
            datalab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip), args.prefix)
        else:
            if previous_subnet_size < private_subnet_size:
                while True:
                    try:
                        datalab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip + 1), args.prefix)
                        ipaddress.ip_network(datalab_subnet_cidr)
                        break
                    except ValueError:
                        last_ip = last_ip + 2
                        continue
            else:
                datalab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip + 1), args.prefix)
    else:
        pre_defined_subnet_list = []
        subnet_cidr = args.user_subnets_range.split('-')[0].replace(' ', '')
        pre_defined_subnet_list.append(subnet_cidr)
        while str(subnet_cidr) != args.user_subnets_range.split('-')[1].replace(' ', ''):
            subnet = ipaddress.ip_network(u'{}'.format(subnet_cidr))
            num_addr = subnet.num_addresses
            first_ip = int(ipaddress.IPv4Address(u'{}'.format(subnet.network_address)))
            next_subnet = ipaddress.ip_network(u'{}/{}'.format(ipaddress.ip_address(first_ip + num_addr),
                                                               args.prefix))
            pre_defined_subnet_list.append(next_subnet.compressed)
            subnet_cidr = next_subnet
        existed_subnet_list = []
        response = GCPMeta().get_vpc(args.vpc_selflink.split('/')[-1])['subnetworks']
        for subnet in response:
            for region in region_list:
                if region in subnet:
                    existed_subnet_list.append(GCPMeta().get_subnet(subnet.split('/')[-1], region)['ipCidrRange'])
        available_subnets = list(set(pre_defined_subnet_list) - set(existed_subnet_list))
        if not available_subnets:
            logging.info("There is no available subnet to create. Aborting...")
            sys.exit(1)
        else:
            datalab_subnet_cidr = available_subnets[0]

    if args.subnet_name != '':
        if GCPMeta().get_subnet(args.subnet_name, args.region):
            logging.info("REQUESTED SUBNET {} ALREADY EXISTS".format(args.subnet_name))
        else:
            logging.info("Creating Subnet {}".format(args.subnet_name))
            GCPActions().create_subnet(args.subnet_name, datalab_subnet_cidr, args.vpc_selflink, args.region)
    else:
        logging.info("Subnet name can't be empty")
        sys.exit(1)