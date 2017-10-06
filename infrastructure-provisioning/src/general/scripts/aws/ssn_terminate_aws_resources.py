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
parser.add_argument('--tag_name', type=str)
parser.add_argument('--nb_sg', type=str)
parser.add_argument('--edge_sg', type=str)
parser.add_argument('--service_base_name', type=str)
args = parser.parse_args()


##############
# Run script #
##############

if __name__ == "__main__":
    print('Terminating EMR cluster')
    try:
        clusters_list = get_emr_list(args.tag_name)
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                terminate_emr(cluster_id)
                print("The EMR cluster {} has been terminated successfully".format(emr_name))
        else:
            print("There are no EMR clusters to terminate.")
    except:
        sys.exit(1)

    print("Deregistering notebook's AMI")
    try:
        deregister_image('*')
    except:
        sys.exit(1)

    print("Terminating EC2 instances")
    try:
        remove_ec2(args.tag_name, '*')
    except:
        sys.exit(1)

    print("Removing security groups")
    try:
        remove_sgroups(args.nb_sg)
        remove_sgroups(args.edge_sg)
        try:
            remove_sgroups(args.tag_name)
        except:
            print("There is no pre-defined SSN SG")
    except:
        sys.exit(1)

    print("Removing private subnet")
    try:
        remove_subnets('*')
    except:
        sys.exit(1)

    print("Removing s3 buckets")
    try:
        remove_s3()
    except:
        sys.exit(1)

    print("Removing IAM roles, profiles and policies")
    try:
        remove_all_iam_resources('all')
    except:
        sys.exit(1)

    print("Removing route tables")
    try:
        remove_route_tables(args.tag_name)
    except:
        sys.exit(1)

    print("Removing SSN subnet")
    try:
        remove_subnets(args.service_base_name + '-subnet')
    except:
        print("There is no pre-defined SSN Subnet")

    print"Removing SSN VPC"
    try:
        vpc_id = get_vpc_by_tag(args.tag_name, args.service_base_name)
        if vpc_id != '':
            try:
                remove_vpc_endpoints(vpc_id)
            except:
                print "There is no such VPC Endpoint"
            try:
                remove_internet_gateways(vpc_id, args.tag_name, args.service_base_name)
            except:
                print "There is no such Internet gateway"
            remove_route_tables(args.tag_name, True)
            remove_vpc(vpc_id)
        else:
            print "There is no pre-defined SSN VPC"
    except:
        sys.exit(1)
