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
import boto3
import requests


def terminate_edge_node(tag_name, project_name, tag_value, nb_sg, edge_sg, de_sg, emr_sg, endpoint_name):
    print('Terminating EMR cluster')
    try:
        clusters_list = dlab.meta_lib.get_emr_list(tag_name)
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                if '{}'.format(tag_value[:-1]) in emr_name:
                    dlab.actions_lib.terminate_emr(cluster_id)
                    print("The EMR cluster {} has been terminated successfully".format(emr_name))
        else:
            print("There are no EMR clusters to terminate.")
    except Exception as err:
        dlab.fab.append_result("Failed to terminate EMR cluster.", str(err))
        sys.exit(1)

    print("Terminating EDGE and notebook instances")
    try:
        dlab.actions_lib.remove_ec2(tag_name, tag_value)
    except Exception as err:
        dlab.fab.append_result("Failed to terminate instances.", str(err))
        sys.exit(1)

    print("Removing s3 bucket")
    try:
        dlab.actions_lib.remove_s3('edge', project_name)
    except Exception as err:
        dlab.fab.append_result("Failed to remove buckets.", str(err))
        sys.exit(1)

    print("Removing IAM roles and profiles")
    try:
        dlab.actions_lib.remove_all_iam_resources('notebook', project_name, endpoint_name)
        dlab.actions_lib.remove_all_iam_resources('edge', project_name, endpoint_name)
    except Exception as err:
        dlab.fab.append_result("Failed to remove IAM roles and profiles.", str(err))
        sys.exit(1)

    print("Removing security groups")
    try:
        dlab.actions_lib.remove_sgroups(emr_sg)
        dlab.actions_lib.remove_sgroups(de_sg)
        dlab.actions_lib.remove_sgroups(nb_sg)
        dlab.actions_lib.remove_sgroups(edge_sg)
    except Exception as err:
        dlab.fab.append_result("Failed to remove Security Groups.", str(err))
        sys.exit(1)

    print("Removing private subnet")
    try:
        dlab.actions_lib.remove_subnets(tag_value)
    except Exception as err:
        dlab.fab.append_result("Failed to remove subnets.", str(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    dlab.actions_lib.create_aws_config_files()
    print('Generating infrastructure names and tags')
    project_conf = dict()
    project_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    project_conf['project_name'] = os.environ['project_name']
    project_conf['endpoint_name'] = os.environ['endpoint_name']
    project_conf['endpoint_instance_name'] = '{}-{}-endpoint'.format(project_conf['service_base_name'],
                                                                     project_conf['endpoint_name'])
    project_conf['tag_name'] = project_conf['service_base_name'] + '-tag'
    project_conf['tag_value'] = '{}-{}-{}-*'.format(project_conf['service_base_name'], project_conf['project_name'],
                                                    project_conf['endpoint_name'])
    project_conf['edge_sg'] = '{}-{}-{}-edge'.format(project_conf['service_base_name'], project_conf['project_name'],
                                                     project_conf['endpoint_name'])
    project_conf['nb_sg'] = '{}-{}-{}-nb'.format(project_conf['service_base_name'], project_conf['project_name'],
                                                 project_conf['endpoint_name'])
    project_conf['edge_instance_name'] = '{}-{}-{}-edge'.format(project_conf['service_base_name'],
                                                                project_conf['project_name'],
                                                                project_conf['endpoint_name'])
    project_conf['de_sg'] = '{}-{}-{}-de*'.format(project_conf['service_base_name'],
                                                  project_conf['project_name'],
                                                  project_conf['endpoint_name'])
    project_conf['emr_sg'] = '{}-{}-{}-des-*'.format(project_conf['service_base_name'],
                                                     project_conf['project_name'],
                                                     project_conf['endpoint_name'])

    try:
        logging.info('[TERMINATE PROJECT]')
        print('[TERMINATE PROJECT]')
        try:
            terminate_edge_node(project_conf['tag_name'], project_conf['project_name'], project_conf['tag_value'],
                                project_conf['nb_sg'], project_conf['edge_sg'], project_conf['de_sg'],
                                project_conf['emr_sg'], project_conf['endpoint_name'])
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to terminate project.", str(err))
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    try:
        endpoint_id = dlab.meta_lib.get_instance_by_name(project_conf['tag_name'],
                                                         project_conf['endpoint_instance_name'])
        print("Endpoint id: " + endpoint_id)
        ec2 = boto3.client('ec2')
        ec2.delete_tags(Resources=[endpoint_id], Tags=[{'Key': 'project_tag'}, {'Key': 'endpoint_tag'}])
    except Exception as err:
        print("Failed to remove Project tag from Enpoint", str(err))
#        traceback.print_exc()
#        sys.exit(1)

    try:
        print('[KEYCLOAK PROJECT CLIENT DELETE]')
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
            "clientId": '{}-{}-{}'.format(project_conf['service_base_name'], project_conf['project_name'],
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

        keycloak_client = requests.delete(
            keycloak_client_delete_url,
            headers={"Authorization": "Bearer {}".format(keycloak_token.get("access_token")),
                     "Content-Type": "application/json"})
    except Exception as err:
        print("Failed to remove project client from Keycloak", str(err))

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": project_conf['service_base_name'],
                   "project_name": project_conf['project_name'],
                   "Action": "Terminate edge node"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
