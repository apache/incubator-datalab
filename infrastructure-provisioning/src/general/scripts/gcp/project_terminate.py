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

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import requests
import sys
import traceback


def terminate_edge_node(endpoint_name, project_name, service_base_name, region, zone):
    logging.info("Terminating Dataengine-service clusters")
    try:
        labels = [
            {'sbn': service_base_name},
            {'project_tag': project_name}
        ]
        clusters_list = GCPMeta.get_dataproc_list(labels)
        if clusters_list:
            for cluster_name in clusters_list:
                GCPActions.delete_dataproc_cluster(cluster_name, region)
                logging.info('The Dataproc cluster {} has been terminated successfully'.format(cluster_name))
        else:
            logging.info("There are no Dataproc clusters to terminate.")
    except Exception as err:
        datalab.fab.append_result("Failed to terminate dataengine-service", str(err))
        sys.exit(1)

    logging.info("Terminating EDGE and notebook instances")
    base = '{}-{}-{}'.format(service_base_name, project_name, endpoint_name)
    keys = ['edge', 'ps', 'static-ip', 'bucket', 'subnet']
    targets = ['{}-{}'.format(base, k) for k in keys]
    try:
        instances = GCPMeta.get_list_instances(zone, base)
        if 'items' in instances:
            for i in instances['items']:
                if 'project_tag' in i['labels'] and project_name == i['labels']['project_tag']:
                    GCPActions.remove_instance(i['name'], zone)
    except Exception as err:
        datalab.fab.append_result("Failed to terminate instances", str(err))
        sys.exit(1)

    logging.info("Removing static addresses")
    try:
        static_addresses = GCPMeta.get_list_static_addresses(region, base)
        if 'items' in static_addresses:
            for i in static_addresses['items']:
                if bool(set(targets) & set([i['name']])):
                    GCPActions.remove_static_address(i['name'], region)
    except Exception as err:
        datalab.fab.append_result("Failed to remove static addresses", str(err))
        sys.exit(1)

    logging.info("Removing storage bucket")
    try:
        buckets = GCPMeta.get_list_buckets(base)
        if 'items' in buckets:
            for i in buckets['items']:
                if bool(set(targets) & set([i['name']])):
                    GCPActions.remove_bucket(i['name'])
    except Exception as err:
        datalab.fab.append_result("Failed to remove storage buckets", str(err))
        sys.exit(1)

    logging.info("Removing project specific images")
    try:
        project_image_name_beginning = '{}-{}-{}'.format(service_base_name, project_name, endpoint_name)
        images = GCPMeta.get_list_images(project_image_name_beginning)
        if 'items' in images:
            for i in images['items']:
                GCPActions.remove_image(i['name'])
    except Exception as err:
        logging.info('Error: {0}'.format(err))
        sys.exit(1)

    logging.info("Removing firewalls")
    try:
        firewalls = GCPMeta.get_list_firewalls(base)
        if 'items' in firewalls:
            for i in firewalls['items']:
                if bool(set(targets) & set(i['targetTags'])):
                    GCPActions.remove_firewall(i['name'])
    except Exception as err:
        datalab.fab.append_result("Failed to remove security groups", str(err))
        sys.exit(1)

    logging.info("Removing Service accounts and roles")
    try:
        list_service_accounts = GCPMeta.get_list_service_accounts()
        sa_keys = ['edge-sa', 'ps-sa']
        role_keys = ['edge-role', 'ps-role']
        sa_target = ['{}-{}'.format(base, k) for k in sa_keys]
        indexes = [GCPMeta.get_index_by_service_account_name('{}-{}'.format(base, k)) for k in sa_keys]
        role_targets = ['{}-{}-{}'.format(base, i, k) for k in role_keys for i in indexes]
        for service_account in (set(sa_target) & set(list_service_accounts)):
            GCPActions.remove_service_account(service_account, service_base_name)
        list_roles_names = GCPMeta.get_list_roles()
        for role in (set(role_targets) & set(list_roles_names)):
            GCPActions.remove_role(role)
    except Exception as err:
        datalab.fab.append_result("Failed to remove service accounts and roles", str(err))
        sys.exit(1)

    logging.info("Removing subnets")
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
        datalab.fab.append_result("Failed to remove subnets", str(err))
        sys.exit(1)

    logging.info("Removing nat route")
    try:
        nat_route_name = '{0}-{1}-{2}-nat-route'.format(service_base_name, project_name, endpoint_name)
        route = GCPMeta.get_route(nat_route_name)
        if route:
            GCPActions.delete_nat_route(nat_route_name)
    except Exception as err:
        datalab.fab.append_result("Failed to remove nat route", str(err))
        sys.exit(1)


if __name__ == "__main__":
    # generating variables dictionary
    GCPMeta = datalab.meta_lib.GCPMeta()
    GCPActions = datalab.actions_lib.GCPActions()
    logging.info('Generating infrastructure names and tags')
    project_conf = dict()
    project_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    project_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    project_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
    project_conf['project_tag'] = project_conf['project_name']
    project_conf['region'] = os.environ['gcp_region']
    project_conf['zone'] = os.environ['gcp_zone']

    try:
        logging.info('[TERMINATE EDGE]')
        try:
            terminate_edge_node(project_conf['endpoint_name'], project_conf['project_name'],
                                project_conf['service_base_name'],
                                project_conf['region'], project_conf['zone'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to terminate edge.", str(err))
    except Exception as err:
        logging.info('Error: {0}'.format(err))
        sys.exit(1)

    try:
        logging.info('[KEYCLOAK PROJECT CLIENT DELETE]')
        keycloak_auth_server_url = '{}/realms/master/protocol/openid-connect/token'.format(
            os.environ['keycloak_auth_server_url'])
        keycloak_client_url = '{0}/admin/realms/{1}/clients'.format(os.environ['keycloak_auth_server_url'],
                                                                    os.environ['keycloak_realm_name'])

        keycloak_auth_data = {
            "username": os.environ['keycloak_user'],
            "password": os.environ['keycloak_user_password'],
            "grant_type": "password",
            "client_id": "admin-cli",
        }

        client_params = {
            "clientId": "{}-{}-{}".format(project_conf['service_base_name'], project_conf['project_name'],
                                          project_conf['endpoint_name'])
        }

        keycloak_token = requests.post(keycloak_auth_server_url, data=keycloak_auth_data).json()

        keycloak_get_id_client = requests.get(keycloak_client_url, data=keycloak_auth_data, params=client_params,
                                              headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                       "Content-Type": "application/json"})
        json_keycloak_client_id = json.loads(keycloak_get_id_client.text)
        keycloak_id_client = json_keycloak_client_id[0]['id']

        keycloak_client_delete_url = '{0}/admin/realms/{1}/clients/{2}'.format(os.environ['keycloak_auth_server_url'],
                                                                               os.environ['keycloak_realm_name'],
                                                                               keycloak_id_client)

        keycloak_client = requests.delete(keycloak_client_delete_url,
                                          headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                   "Content-Type": "application/json"})
    except Exception as err:
        logging.error("Failed to remove project client from Keycloak", str(err))

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": project_conf['service_base_name'],
                   "project_name": project_conf['project_name'],
                   "Action": "Terminate project"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
