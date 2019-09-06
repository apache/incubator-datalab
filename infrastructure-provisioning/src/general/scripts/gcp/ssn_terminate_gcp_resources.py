#!/usr/bin/python

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
    print("Terminating Dataengine-service clusters")
    try:
        labels = [
            {'sbn': args.service_base_name}
        ]
        clusters_list = meta_lib.GCPMeta().get_dataproc_list(labels)
        if clusters_list:
            for cluster_name in clusters_list:
                actions_lib.GCPActions().delete_dataproc_cluster(cluster_name, args.region)
                print('The Dataproc cluster {} has been terminated successfully'.format(cluster_name))
        else:
            print("There are no Dataproc clusters to terminate.")
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Terminating instances")
    try:
        instances = GCPMeta().get_list_instances(args.zone, args.service_base_name)
        if 'items' in instances:
            for i in instances['items']:
                GCPActions().remove_instance(i['name'], args.zone)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing images")
    try:
        images = GCPMeta().get_list_images(args.service_base_name)
        if 'items' in images:
            for i in images['items']:
                GCPActions().remove_image(i['name'])
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing static addresses")
    try:
        static_addresses = GCPMeta().get_list_static_addresses(args.region, args.service_base_name)
        if 'items' in static_addresses:
            for i in static_addresses['items']:
                GCPActions().remove_static_address(i['name'], args.region)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing firewalls")
    try:
        firewalls = GCPMeta().get_list_firewalls(args.service_base_name)
        if 'items' in firewalls:
            for i in firewalls['items']:
                GCPActions().remove_firewall(i['name'])
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing Service accounts and roles")
    try:
        list_service_accounts = GCPMeta().get_list_service_accounts()
        for service_account in list_service_accounts:
            if service_account.startswith(args.service_base_name):
                GCPActions().remove_service_account(service_account)
        list_roles_names = GCPMeta().get_list_roles()
        for role in list_roles_names:
            if role.startswith(args.service_base_name):
                GCPActions().remove_role(role)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing subnets")
    try:
        list_subnets = GCPMeta().get_list_subnetworks(args.region, '', args.service_base_name)
        if 'items' in list_subnets:
            vpc_selflink = list_subnets['items'][0]['network']
            vpc_name = vpc_selflink.split('/')[-1]
            subnets = GCPMeta().get_list_subnetworks(args.region, vpc_name, args.service_base_name)
            for i in subnets['items']:
                GCPActions().remove_subnet(i['name'], args.region)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing s3 buckets")
    try:
        buckets = GCPMeta().get_list_buckets(args.service_base_name)
        if 'items' in buckets:
            for i in buckets['items']:
                GCPActions().remove_bucket(i['name'])
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Removing SSN VPC")
    try:
        GCPActions().remove_vpc(args.service_base_name + '-ssn-vpc')
    except Exception as err:
        print('Error: {0}'.format(err))
        print("No such VPC")
        sys.exit(1)
