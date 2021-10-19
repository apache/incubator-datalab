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
import boto3
import ipaddress
import json
import sys
from botocore import exceptions
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--username', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--prefix', type=str, default='')
parser.add_argument('--ssn', type=bool, default=False)
parser.add_argument('--user_subnets_range', type=str, default='')
parser.add_argument('--zone', type=str, default='')
parser.add_argument('--subnet_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    tag = {"Key": args.infra_tag_name, "Value": args.subnet_name}
    tag_name = {"Key": "Name", "Value": args.subnet_name}

    #defining subnet cidr
    try:
        if args.user_subnets_range == '' or args.ssn:
            ec2 = boto3.resource('ec2')
            private_subnet_size = ipaddress.ip_network(u'0.0.0.0/{}'.format(args.prefix)).num_addresses
            vpc = ec2.Vpc(args.vpc_id)
            vpc_cidr = vpc.cidr_block
            first_vpc_ip = int(ipaddress.IPv4Address(vpc_cidr.split('/')[0]))
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
                first_ip = int(ipaddress.IPv4Address(cidr.split('/')[0]))
                if first_ip - last_ip < private_subnet_size or previous_subnet_size < private_subnet_size:
                    subnet_size = ipaddress.ip_network(u'{}'.format(cidr)).num_addresses
                    last_ip = first_ip + subnet_size - 1
                    previous_subnet_size = subnet_size
                else:
                    break
            datalab_subnet_cidr = ''
            if previous_subnet_size < private_subnet_size:
                while True:
                    try:
                        datalab_subnet_cidr = '{0}/{1}'.format(ipaddress.ip_address(last_ip + 1), args.prefix)
                        ipaddress.ip_network(datalab_subnet_cidr.decode('utf-8'))
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
            client = boto3.client('ec2')
            response = client.describe_subnets(Filters=[{'Name': 'vpc-id', 'Values': [args.vpc_id]}]).get('Subnets')
            for i in response:
                existed_subnet_list.append(i.get('CidrBlock'))
            available_subnets = list(set(pre_defined_subnet_list) - set(existed_subnet_list))
            if not available_subnets:
                logging.error("There is no available subnet to create. Aborting...")
                sys.exit(1)
            else:
                datalab_subnet_cidr = available_subnets[0]

        #checking existing subnets
        if args.ssn:
            subnet_id = get_subnet_by_cidr(datalab_subnet_cidr, args.vpc_id)
            subnet_check = get_subnet_by_tag(tag, False, args.vpc_id)
        else:
            subnet_id = get_subnet_by_cidr(datalab_subnet_cidr, args.vpc_id)
            subnet_check = get_subnet_by_tag(tag, args.vpc_id)

        #creating subnet
        if not subnet_check:
            if subnet_id == '':
                logging.info("Creating subnet {0} in vpc {1} with tag {2}".
                      format(datalab_subnet_cidr, args.vpc_id, json.dumps(tag)))
                subnet_id = create_subnet(args.vpc_id, datalab_subnet_cidr, tag, args.zone)
                create_tag(subnet_id, tag_name)
        else:
            logging.info("REQUESTED SUBNET ALREADY EXISTS. USING CIDR {}".format(subnet_check))
            subnet_id = get_subnet_by_cidr(subnet_check)
        logging.info("SUBNET_ID: {}".format(subnet_id))

        #associating subnet with route table
        if not args.ssn:
            if os.environ['edge_is_nat'] == 'true':
                logging.info('Subnet will be associted with route table for NAT')
            else:
                logging.info("Associating route_table with the subnet")
                ec2 = boto3.resource('ec2')
                if os.environ['conf_duo_vpc_enable'] == 'true':
                    rt = get_route_table_by_tag(args.infra_tag_value + '-secondary-tag', args.infra_tag_value)
                else:
                    rt = get_route_table_by_tag(args.infra_tag_name, args.infra_tag_value)
                route_table = ec2.RouteTable(rt)
                try:
                    route_table.associate_with_subnet(SubnetId=subnet_id)
                    if os.environ['conf_duo_vpc_enable'] == 'true':
                        create_peer_routes(os.environ['aws_peering_id'], args.infra_tag_value)
                except exceptions.ClientError as err:
                    if 'Resource.AlreadyAssociated' in str(err):
                        logging.info('Other route table is already associated with this subnet. Skipping...')
        else:
            logging.info("Associating route_table with the subnet")
            ec2 = boto3.resource('ec2')
            rt = get_route_table_by_tag(args.infra_tag_name, args.infra_tag_value)
            route_table = ec2.RouteTable(rt)
            route_table.associate_with_subnet(SubnetId=subnet_id)
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        sys.exit(1)
