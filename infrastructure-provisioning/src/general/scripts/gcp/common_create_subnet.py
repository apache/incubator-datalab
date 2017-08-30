#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

import argparse
import json
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys
import boto3
import ipaddress


parser = argparse.ArgumentParser()
parser.add_argument('--subnet_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--vpc_selflink', type=str, default='')
parser.add_argument('--prefix', type=str, default='')
parser.add_argument('--vpc_cidr', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    empty_vpc = False
    private_subnet_size = ipaddress.ip_network(u'0.0.0.0/{}'.format(args.prefix)).num_addresses
    subnets_cidr = []
    try:
        subnets = GCPMeta().get_vpc(args.vpc_selflink.split('/')[-1])['subnetworks']
    except KeyError:
        empty_vpc = True
        subnets = []
    for subnet in subnets:
        subnets_cidr.append(GCPMeta().get_subnet(subnet.split('/')[-1], args.region)['ipCidrRange'])
    sortkey = lambda addr: \
        (int(addr.split("/")[0].split(".")[0]),
         int(addr.split("/")[0].split(".")[1]),
         int(addr.split("/")[0].split(".")[2]),
         int(addr.split("/")[0].split(".")[3]),
         int(addr.split("/")[1]))
    sorted_subnets_cidr = sorted(subnets_cidr, key=sortkey)

    if not empty_vpc:
        last_ip = int(ipaddress.IPv4Address(sorted_subnets_cidr[0].split('/')[0].decode("utf-8")))
    else:
        last_ip = int(ipaddress.IPv4Address(args.vpc_cidr.split('/')[0].decode("utf-8")))
    previous_subnet_size = private_subnet_size
    for cidr in sorted_subnets_cidr:
        first_ip = int(ipaddress.IPv4Address(cidr.split('/')[0].decode("utf-8")))
        if first_ip - last_ip < private_subnet_size or previous_subnet_size < private_subnet_size:
            subnet_size = ipaddress.ip_network(u'{}'.format(cidr)).num_addresses
            last_ip = first_ip + subnet_size - 1
            previous_subnet_size = subnet_size
        else:
            break

    dlab_subnet_cidr = ''
    if empty_vpc:
        dlab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip), args.prefix)
    else:
        if previous_subnet_size < private_subnet_size:
            while True:
                try:
                    dlab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip + 1), args.prefix)
                    ipaddress.ip_network(dlab_subnet_cidr.decode('utf-8'))
                    break
                except ValueError:
                    last_ip = last_ip + 2
                    continue
        else:
            dlab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip + 1), args.prefix)

    if args.subnet_name != '':
        if GCPMeta().get_subnet(args.subnet_name, args.region):
            print "REQUESTED SUBNET {} ALREADY EXISTS".format(args.subnet_name)
        else:
            print "Creating Subnet {}".format(args.subnet_name)
            GCPActions().create_subnet(args.subnet_name, dlab_subnet_cidr, args.vpc_selflink, args.region)
    else:
        sys.exit(1)
