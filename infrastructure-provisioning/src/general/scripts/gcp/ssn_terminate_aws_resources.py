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

from dlab.meta_lib import *
from dlab.actions_lib import *
import boto3
import argparse
import sys
from dlab.ssn_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--zone', type=str)
parser.add_argument('--service_base_name', type=str)
parser.add_argument('--region', type=str)
args = parser.parse_args()


##############
# Run script #
##############

if __name__ == "__main__":

    print "Terminating instances"
    try:
        instances = GCPMeta().get_list_instances(args.zone, args.service_base_name)
        print "INSTANCES-----"
        print str(instances)
        if instances:
            for i in instances['items']:
                GCPActions().remove_instance(i['name'], args.zone)
    except:
        sys.exit(1)

    print "Removing static addresses"
    try:
        static_addresses = GCPMeta().get_list_static_addresses(args.region, args.service_base_name)
        if static_addresses:
            for i in static_addresses['items']:
                GCPActions().remove_static_address(i['name'], args.region)
    except:
        sys.exit(1)
    print "Removing firewalls"
    try:
        firewalls = GCPMeta().get_list_firewalls(args.service_base_name)
        if firewalls:
            for i in firewalls['items']:
                GCPActions().remove_firewall(i['name'])
    except:
        sys.exit(1)

    print "Removing subnets"
    try:
        list_subnets = GCPMeta().get_list_subnetworks(args.region, filter_string=args.service_base_name)
        print str(list_subnets)
        if list_subnets:
            vpc_selflink = list_subnets['items'][0]['network']
            print "SELFLINK -> " + vpc_selflink
            vpc_name = vpc_selflink.split('/')[-1]
            print "VPC_NAME -> " + vpc_name
            subnets = GCPMeta().get_list_subnetworks(args.region, vpc_name, args.service_base_name)
            for i in subnets['items']:
                GCPActions().remove_subnet(i['name'], args.region)
    except:
        sys.exit(1)

    print "Removing s3 buckets"
    try:
        buckets = GCPMeta().get_list_buckets(args.service_base_name)
        if buckets:
            for i in buckets['items']:
                GCPActions().remove_bucket(i['name'])
    except:
        sys.exit(1)

    print "Removing SSN VPC"
    try:
        GCPActions().remove_vpc(args.service_base_name + '-ssn-vpc')
    except:
        print "There is no pre-defined VPC"
