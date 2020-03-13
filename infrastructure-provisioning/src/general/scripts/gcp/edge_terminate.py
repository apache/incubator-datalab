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

import json
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import sys
import time
import os
import traceback
import logging


def terminate_edge_node(user_name, service_base_name, region, zone, project_name, endpoint_name):
    print("Terminating Dataengine-service clusters")
    try:
        labels = [
            {'sbn': service_base_name},
            {'user': user_name}
        ]
        clusters_list = GCPMeta.get_dataproc_list(labels)
        if clusters_list:
            for cluster_name in clusters_list:
                GCPActions.delete_dataproc_cluster(cluster_name, region)
                print('The Dataproc cluster {} has been terminated successfully'.format(cluster_name))
        else:
            print("There are no Dataproc clusters to terminate.")
    except Exception as err:
        dlab.fab.append_result("Failed to terminate dataproc", str(err))
        sys.exit(1)

    print("Terminating EDGE and notebook instances")
    base = '{}-{}-{}'.format(service_base_name, project_name, endpoint_name)
    keys = ['edge', 'ps', 'static-ip', 'bucket', 'subnet']
    targets = ['{}-{}'.format(base, k) for k in keys]
    try:
        instances = GCPMeta.get_list_instances(zone, base)
        if 'items' in instances:
            for i in instances['items']:
                if 'user' in i['labels'] and user_name == i['labels']['user']:
                    GCPActions.remove_instance(i['name'], zone)
    except Exception as err:
        dlab.fab.append_result("Failed to terminate instances", str(err))
        sys.exit(1)

    print("Removing static addresses")
    try:
        static_addresses = GCPMeta.get_list_static_addresses(region, base)
        if 'items' in static_addresses:
            for i in static_addresses['items']:
                if bool(set(targets) & set([i['name']])):
                    GCPActions.remove_static_address(i['name'], region)
    except Exception as err:
        dlab.fab.append_result("Failed to remove static IPs", str(err))
        sys.exit(1)

    print("Removing storage bucket")
    try:
        buckets = GCPMeta.get_list_buckets(base)
        if 'items' in buckets:
            for i in buckets['items']:
                if bool(set(targets) & set([i['name']])):
                    GCPActions.remove_bucket(i['name'])
    except Exception as err:
        dlab.fab.append_result("Failed to remove buckets", str(err))
        sys.exit(1)

    print("Removing firewalls")
    try:
        firewalls = GCPMeta.get_list_firewalls(base)
        if 'items' in firewalls:
            for i in firewalls['items']:
                if bool(set(targets) & set(i['targetTags'])):
                    GCPActions.remove_firewall(i['name'])
    except Exception as err:
        dlab.fab.append_result("Failed to remove security groups", str(err))
        sys.exit(1)

    print("Removing Service accounts and roles")
    try:
        list_service_accounts = GCPMeta.get_list_service_accounts()
        for service_account in (set(targets) & set(list_service_accounts)):
            if service_account.startswith(service_base_name):
                GCPActions.remove_service_account(service_account, service_base_name)
        list_roles_names = GCPMeta.get_list_roles()
        for role in (set(targets) & set(list_roles_names)):
            if role.startswith(service_base_name):
                GCPActions.remove_role(role)
    except Exception as err:
        dlab.fab.append_result("Failed to remove service accounts and roles", str(err))
        sys.exit(1)

    print("Removing subnets")
    try:
        list_subnets = GCPMeta.get_list_subnetworks(region, '', base)
        if 'items' in list_subnets:
            vpc_selflink = list_subnets['items'][0]['network']
            vpc_name = vpc_selflink.split('/')[-1]
            subnets = GCPMeta.get_list_subnetworks(region, vpc_name, base)
            for i in subnets['items']:
                if bool(set(targets) & set([i['name']])):
                    GCPActions.remove_subnet(i['name'], region)
    except Exception as err:
        dlab.fab.append_result("Failed to remove subnets", str(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    GCPMeta = dlab.meta_lib.GCPMeta()
    GCPActions = dlab.actions_lib.GCPActions()
    print('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    edge_conf['edge_user_name'] = (os.environ['edge_user_name'])
    edge_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    edge_conf['endpoint_name'] = os.environ['endpoint_name'].replace('_', '-').lower()
    edge_conf['region'] = os.environ['gcp_region']
    edge_conf['zone'] = os.environ['gcp_zone']

    try:
        logging.info('[TERMINATE EDGE]')
        print('[TERMINATE EDGE]')
        try:
            terminate_edge_node(edge_conf['edge_user_name'], edge_conf['service_base_name'],
                                edge_conf['region'], edge_conf['zone'], edge_conf['project_name'],
                                edge_conf['endpoint_name'])
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to terminate edge.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": edge_conf['service_base_name'],
                   "user_name": edge_conf['edge_user_name'],
                   "Action": "Terminate edge node"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
