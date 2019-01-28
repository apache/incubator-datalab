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
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys
import boto3, botocore
from dlab.ssn_lib import *
import time
import os
import json

parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--subnet_id', type=str, default='')
parser.add_argument('--duo_vpc_enable', type=str, default='false')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    waiter = time.sleep(10)
    if args.vpc_id:
        print("Creating S3 Endpoint in vpc {}, region {} with tag {}.".format(args.vpc_id, args.region,
                                                                              json.dumps(tag)))
        ec2 = boto3.client('ec2')
        route_table = []
        endpoint = ''
        service_name = 'com.amazonaws.{}.s3'.format(args.region)
        print('Vars are: {}, {}, {}'.format(args.vpc_id, service_name, json.dumps(tag)))
        try:
            # Creating Route table and S3 endpoint
            route_table = get_route_tables(args.vpc_id, json.dumps(tag))
            if not route_table:
                route_table.append(ec2.create_route_table(
                    VpcId = args.vpc_id
                )['RouteTable']['RouteTableId'])
                while route_table != '':
                    waiter
                    if route_table == '':
                        waiter
                    else:
                        break
                print('Created Route-Table with ID: {}'.format(route_table))
                create_tag(route_table, json.dumps(tag))
            endpoints = get_vpc_endpoints(args.vpc_id, args.region, 's3')
            if not endpoints:
                print('Creating S3 Endpoint')
                endpoint = ec2.create_vpc_endpoint(
                    VpcId=args.vpc_id,
                    ServiceName=service_name,
                    RouteTableIds=route_table
                )
                endpoint = endpoint['VpcEndpoint']['VpcEndpointId']
            else:
                print('For current VPC {} S3 endpoint already exists. ID: {}. Route table list will be modified'.format(
                    args.vpc_id, endpoints[0].get('VpcEndpointId')))
                endpoint_id = endpoints[0].get('VpcEndpointId')
                result = ec2.modify_vpc_endpoint(
                    VpcEndpointId=endpoint_id,
                    AddRouteTableIds=route_table
                )
                if result:
                    endpoint = endpoint_id
            print("S3 ENDPOINT: {}".format(endpoint))

            if 'conf_dlab_repository_host' in os.environ and args.duo_vpc_enable == 'false':
                # Creating Security Group and EC2 endpoint
                sg_tag = {"Key": args.infra_tag_value, "Value": args.infra_tag_name}
                allowed_vpc_cidr_ip_ranges = list()
                for cidr in get_vpc_cidr_by_id(args.vpc_id):
                    allowed_vpc_cidr_ip_ranges.append({"CidrIp": cidr})
                all_ip_cidr = '0.0.0.0/0'
                sg_name = '{}-ec2-endpoint-SG'.format(args.infra_tag_value)
                ingress_sg_rules = [
                    {
                        "IpProtocol": "-1",
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "UserIdGroupPairs": [], "PrefixListIds": []
                    }
                ]
                egress_sg_rules = [
                    {"IpProtocol": "-1", "IpRanges": [{"CidrIp": all_ip_cidr}], "UserIdGroupPairs": [],
                     "PrefixListIds": []}
                ]
                security_group_id = get_security_group_by_name(sg_name)
                if security_group_id == '':
                    print("Creating security group {0} for vpc {1} with tag {2}.".format(sg_name, args.vpc_id,
                                                                                         json.dumps(sg_tag)))
                    security_group_id = create_security_group(sg_name, args.vpc_id, ingress_sg_rules, egress_sg_rules,
                                                              sg_tag)
                else:
                    print("REQUESTED SECURITY GROUP WITH NAME {} ALREADY EXISTS".format(sg_name))
                print("SECURITY_GROUP_ID: {}".format(security_group_id))
                ec2_endpoint = get_vpc_endpoints(args.vpc_id, args.region, 'ec2')
                if not ec2_endpoint:
                    print('Creating EC2 Endpoint')
                    service_name = 'com.amazonaws.{}.ec2'.format(args.region)
                    ec2_endpoint = ec2.create_vpc_endpoint(
                        VpcEndpointType='Interface',
                        VpcId=args.vpc_id,
                        ServiceName=service_name,
                        SecurityGroupIds=[security_group_id],
                        SubnetIds=[args.subnet_id]
                    )
                    ec2_endpoint_id = ec2_endpoint['VpcEndpoint']['VpcEndpointId']
                else:
                    print('For current VPC {} EC2 endpoint already exists. ID: {}. SG and Subnet lists will be '
                          'modified'.format(args.vpc_id, ec2_endpoint[0].get('VpcEndpointId')))
                    ec2_endpoint_id = ec2_endpoint[0].get('VpcEndpointId')
                    result = ec2.modify_vpc_endpoint(
                        VpcEndpointId=ec2_endpoint_id,
                        AddSecurityGroupIds=[security_group_id],
                        AddSubnetIds=[args.subnet_id]
                    )
                print("EC2 ENDPOINT: {}".format(ec2_endpoint_id))
        except botocore.exceptions.ClientError as err:
            print(err.response['Error']['Message'])
            print('Failed to create endpoint. Removing RT')
            ec2.delete_route_table(
                RouteTableId=route_table[0]
            )
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
