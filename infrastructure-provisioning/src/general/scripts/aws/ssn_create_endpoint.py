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
import botocore
import sys
import time
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.ssn_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    waiter = time.sleep(10)
    if args.vpc_id:
        logging.info("Creating Endpoint in vpc {}, region {} with tag {}."
                     .format(args.vpc_id, args.region, json.dumps(tag)))
        ec2 = boto3.client('ec2')
        route_table = []
        endpoint = ''
        service_name = 'com.amazonaws.{}.s3'.format(args.region)
        logging.info('Vars are: {}, {}, {}'.format(args.vpc_id, service_name, json.dumps(tag)))
        try:
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
                logging.info('Created Route-Table with ID: {}'.format(route_table))
                create_tag(route_table, json.dumps(tag))
                create_tag(route_table, json.dumps({"Key": "Name", "Value": "{}-rt".format(args.infra_tag_value)}))
            endpoints = get_vpc_endpoints(args.vpc_id)
            if not endpoints:
                logging.info('Creating EP')
                endpoint = ec2.create_vpc_endpoint(
                    VpcId=args.vpc_id,
                    ServiceName=service_name,
                    RouteTableIds=route_table
                )
                endpoint = endpoint['VpcEndpoint']['VpcEndpointId']
            else:
                logging.info('For current VPC {} endpoint already exists. ID: {}. Route table list will be modified'
                             .format(args.vpc_id, endpoints[0].get('VpcEndpointId')))
                endpoint_id = endpoints[0].get('VpcEndpointId')
                result = ec2.modify_vpc_endpoint(
                    VpcEndpointId=endpoint_id,
                    AddRouteTableIds=route_table
                )
                if result:
                    endpoint = endpoint_id
            logging.info("ENDPOINT: {}".format(endpoint))
        except botocore.exceptions.ClientError as err:
            logging.error(err.response['Error']['Message'])
            logging.info('Failed to create endpoint. Removing RT')
            ec2.delete_route_table(
                RouteTableId=route_table[0]
            )
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
