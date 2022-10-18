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

import boto3
import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
import os
import sys
import traceback
from datalab.logger import logging


def terminate_nb(nb_tag_value, bucket_name, tag_name):
    logging.info('Terminating EMR cluster and cleaning EMR config from S3 bucket')
    try:
        clusters_list = datalab.meta_lib.get_emr_list(nb_tag_value, 'Value')
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                logging.info('Cleaning bucket from configs for cluster {}'.format(emr_name))
                datalab.actions_lib.s3_cleanup(bucket_name, emr_name, os.environ['project_name'])
                print("The bucket {} has been cleaned successfully".format(bucket_name))
                print('Terminating cluster {}'.format(emr_name))
                datalab.actions_lib.terminate_emr(cluster_id)
                logging.info("The EMR cluster {} has been terminated successfully".format(emr_name))
        else:
            logging.info("There are no EMR clusters to terminate.")
    except:
        sys.exit(1)

    logging.info("Terminating data engine cluster")
    try:
        datalab.actions_lib.remove_ec2('dataengine_notebook_name', nb_tag_value)
    except:
        sys.exit(1)

    logging.info("Terminating notebook")
    try:
        datalab.actions_lib.remove_ec2(tag_name, nb_tag_value)
    except:
        sys.exit(1)

    if os.environ['notebook_create_keycloak_client'] == 'True':
        logging.info("Terminating notebook keycloak client")
        try:
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
                "clientId": "{}-{}-{}-{}".format(notebook_config['service_base_name'], notebook_config['project_name'],
                                                 notebook_config['endpoint_name'], notebook_config['exploratory_name'])
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

            requests.delete(keycloak_client_delete_url,
                            headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                     "Content-Type": "application/json"})
        except Exception as err:
            logging.error("Failed to remove project client from Keycloak", str(err))


if __name__ == "__main__":
    # generating variables dictionary
    datalab.actions_lib.create_aws_config_files()
    logging.info('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['project_name'] = os.environ['project_name']
    notebook_config['endpoint_name'] = os.environ['endpoint_name']
    notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                  notebook_config['project_name'],
                                                                  notebook_config['endpoint_name']
                                                                 ).lower().replace('_', '-')

    try:
        notebook_config['exploratory_name'] = os.environ['exploratory_name'].lower()
    except:
        notebook_config['exploratory_name'] = ''

    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'

    try:
        logging.info('[TERMINATE NOTEBOOK]')
        try:
            terminate_nb(notebook_config['notebook_name'], notebook_config['bucket_name'], notebook_config['tag_name'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to terminate notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "user_own_bucket_name": notebook_config['bucket_name'],
                   "Action": "Terminate notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
