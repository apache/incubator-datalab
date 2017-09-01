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
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--username', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--prefix', type=str, default='')
parser.add_argument('--ssn', type=bool, default=False)
args = parser.parse_args()


if __name__ == "__main__":
    success = False
    if args.ssn:
        tag = {"Key": args.infra_tag_name, "Value": "{}-subnet".format(args.infra_tag_value)}
    else:
        tag = {"Key": args.infra_tag_name, "Value": "{}-{}-subnet".format(args.infra_tag_value, args.username)}
    try:
        ec2 = boto3.resource('ec2')
        private_subnet_size = ipaddress.ip_network(u'0.0.0.0/{}'.format(args.prefix)).num_addresses
        vpc = ec2.Vpc(args.vpc_id)
        vpc_cidr = vpc.cidr_block
        first_vpc_ip = int(ipaddress.IPv4Address(vpc_cidr.split('/')[0].decode("utf-8")))
        subnets = list(vpc.subnets.all())
        subnets_cidr = []
        for subnet in subnets:
            subnets_cidr.append(subnet.cidr_block)
        sortkey = lambda addr:\
            (int(addr.split("/")[0].split(".")[0]),
             int(addr.split("/")[0].split(".")[1]),
             int(addr.split("/")[0].split(".")[2]),
             int(addr.split("/")[0].split(".")[3]),
             int(addr.split("/")[1]))
        sorted_subnets_cidr = sorted(subnets_cidr, key=sortkey)

        last_ip = first_vpc_ip
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

        if args.ssn:
            subnet_id = get_subnet_by_cidr(dlab_subnet_cidr, args.vpc_id)
            subnet_check = get_subnet_by_tag(tag, False, args.vpc_id)
        else:
            subnet_id = get_subnet_by_cidr(dlab_subnet_cidr)
            subnet_check = get_subnet_by_tag(tag)
        if not subnet_check:
            if subnet_id == '':
                print "Creating subnet %s in vpc %s with tag %s." % \
                      (dlab_subnet_cidr, args.vpc_id, json.dumps(tag))
                subnet_id = create_subnet(args.vpc_id, dlab_subnet_cidr, tag)
        else:
            print "REQUESTED SUBNET ALREADY EXISTS. USING CIDR {}".format(subnet_check)
            subnet_id = get_subnet_by_cidr(subnet_check)
        print "SUBNET_ID: " + subnet_id
        if not args.ssn:
            print "Associating route_table with the subnet"
            ec2 = boto3.resource('ec2')
            rt = get_route_table_by_tag(args.infra_tag_name, args.infra_tag_value)
            route_table = ec2.RouteTable(rt)
            route_table.associate_with_subnet(SubnetId=subnet_id)
        else:
            print "Associating route_table with the subnet"
            ec2 = boto3.resource('ec2')
            rt = get_route_table_by_tag(args.infra_tag_name, args.infra_tag_value)
            route_table = ec2.RouteTable(rt)
            route_table.associate_with_subnet(SubnetId=subnet_id)
            with open('/tmp/ssn_subnet_id', 'w') as f:
                f.write(subnet_id)
        success = True
    except:
        success = False

    if success:
        sys.exit(0)
    else:
        sys.exit(1)
