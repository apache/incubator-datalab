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
import sys, os
import boto3, botocore


parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    success = False
    tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    if args.vpc_id:
        print "Creating Endpoint in vpc {}, region {} with tag {}.".format(args.vpc_id, args.region, json.dumps(tag))
        ec2 = boto3.client('ec2')
        route_table = []
        endpoint = ''
        service_name = 'com.amazonaws.{}.s3'.format(args.region)
        print 'Vars are: {}, {}, {}'.format(args.vpc_id, service_name, json.dumps(tag))
        try:
            route_table = get_route_tables(args.vpc_id, json.dumps(tag))
            if not route_table:
                route_table.append(ec2.create_route_table(
                    VpcId = args.vpc_id
                )['RouteTable']['RouteTableId'])
                print 'Created Route-Table with ID: {}'.format(route_table)
                create_tag(route_table, json.dumps(tag))
            endpoints = get_vpc_endpoints(args.vpc_id)
            if not endpoints:
                print 'Creating EP'
                endpoint = ec2.create_vpc_endpoint(
                    VpcId=args.vpc_id,
                    ServiceName=service_name,
                    RouteTableIds=route_table
                    #   ClientToken='string'
                )
                endpoint = endpoint['VpcEndpoint']['VpcEndpointId']
            else:
                print 'For current VPC {} endpoint already exists. ID: {}. Route table list will be modified'.format(args.vpc_id, endpoints[0].get('VpcEndpointId'))
                endpoint_id = endpoints[0].get('VpcEndpointId')
                result = ec2.modify_vpc_endpoint(
                    VpcEndpointId=endpoint_id,
                    AddRouteTableIds=route_table
                )
                if result:
                    endpoint = endpoint_id
            print "ENDPOINT: " + endpoint
            success = True
        except botocore.exceptions.ClientError as err:
            print err.response['Error']['Message']
            print 'Failed to create endpoint. Removing RT'
            ec2.delete_route_table(
                RouteTableId=route_table[0]
            )
            success = False
    else:
        parser.print_help()
        sys.exit(2)

    if success:
        sys.exit(0)
    else:
        sys.exit(1)