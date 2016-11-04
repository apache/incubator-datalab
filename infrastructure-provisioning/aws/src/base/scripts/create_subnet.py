#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

import argparse
import json
from dlab.aws_actions import *
from dlab.aws_meta import *
import sys
import os
import socket
import boto3


parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--username', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    success = False
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

        subnet_cidr = '{}.{}.{}.0/24'.format(cidr.split('.')[0], cidr.split('.')[1], position)
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
        print "Associating route_table with the subnet"
        ec2 = boto3.resource('ec2')
        rt = get_route_table_by_tag(args.infra_tag_name, args.infra_tag_value)
        route_table = ec2.RouteTable(rt)
        route_table.associate_with_subnet(SubnetId=subnet_id)
        success = True
    except:
        success = False

    if success:
        sys.exit(0)
    else:
        sys.exit(1)
