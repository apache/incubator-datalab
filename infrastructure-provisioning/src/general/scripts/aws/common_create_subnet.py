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
import os
import socket
import boto3


parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--username', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--prefix', type=str, default='24')
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
        vpc = ec2.Vpc(args.vpc_id)
        cidr = vpc.cidr_block
        subnets = list(vpc.subnets.all())

        cidr_blocks = []
        for i in subnets:
            cidr_blocks.append({'Net': i.cidr_block.split('/')[0], 'Mask': i.cidr_block.split('/')[1], 'NTOA': socket.inet_aton(i.cidr_block.split('/')[0])})
        list = sorted(cidr_blocks, key=lambda k: k['NTOA'])

        position = 0
        for i in list:
            start = int(i.get('Net').split('.')[2])
            #print 'start: ' + str(start)
            end = start + ((lambda k: 2**(32 - k))(int(i.get('Mask')))) // 256
            #print 'end: ' + str(end)
            if position != start:
                #return position += 1
                #position += 1
                #print 'position: ' + str(position)
                break
            else:
                position = end
            #print 'position: ' + str(position)

        subnet_cidr = '{}.{}.{}.0/{}'.format(cidr.split('.')[0], cidr.split('.')[1], position, args.prefix)
        if args.ssn:
            subnet_id = get_subnet_by_cidr(subnet_cidr, args.vpc_id)
            subnet_check = get_subnet_by_tag(tag, False, args.vpc_id)
        else:
            subnet_id = get_subnet_by_cidr(subnet_cidr)
            subnet_check = get_subnet_by_tag(tag)
        if not subnet_check:
            if subnet_id == '':
                print "Creating subnet %s in vpc %s with tag %s." % \
                      (subnet_cidr, args.vpc_id, json.dumps(tag))
                subnet_id = create_subnet(args.vpc_id, subnet_cidr, tag)
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
