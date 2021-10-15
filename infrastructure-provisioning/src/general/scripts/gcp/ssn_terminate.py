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

import datalab.ssn_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import subprocess
import requests
from fabric import *

if __name__ == "__main__":
    # generating variables dictionary
    logging.info('Generating infrastructure names and tags')
    ssn_conf = dict()
    ssn_conf['service_base_name'] = datalab.fab.replace_multi_symbols(
        os.environ['conf_service_base_name'].replace('_', '-').lower()[:20], '-', True)
    ssn_conf['region'] = os.environ['gcp_region']
    ssn_conf['zone'] = os.environ['gcp_zone']
    pre_defined_vpc = False
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            pre_defined_vpc = True
            ssn_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        ssn_conf['vpc_name'] = '{}-vpc'.format(ssn_conf['service_base_name'])

    try:
        logging.info('[TERMINATE SSN]')
        params = "--service_base_name {} --region {} --zone {} --pre_defined_vpc {} --vpc_name {}".format(
            ssn_conf['service_base_name'], ssn_conf['region'], ssn_conf['zone'], pre_defined_vpc, ssn_conf['vpc_name'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('ssn_terminate_gcp_resources', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to terminate ssn.", str(err))
        sys.exit(1)

    try:
        logging.info('[KEYCLOAK SSN CLIENT DELETE]')
        keycloak_auth_server_url = '{}/realms/master/protocol/openid-connect/token'.format(os.environ['keycloak_auth_server_url'])
        keycloak_client_url = '{0}/admin/realms/{1}/clients'.format(os.environ['keycloak_auth_server_url'],
                                                                    os.environ['keycloak_realm_name'])
        keycloak_auth_data = {"username": os.environ['keycloak_user'], "password": os.environ['keycloak_user_password'],
                              "grant_type": "password", "client_id": "admin-cli"}
        client_params = {"clientId": '{}-ui'.format(ssn_conf['service_base_name'])}
        keycloak_token = requests.post(keycloak_auth_server_url, data=keycloak_auth_data).json()
        keycloak_get_id_client = requests.get(keycloak_client_url, data=keycloak_auth_data, params=client_params,
                                              headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                       "Content-Type": "application/json"})
        json_keycloak_client_id = json.loads(keycloak_get_id_client.text)
        keycloak_id_client = json_keycloak_client_id[0]['id']
        keycloak_client_delete_url = '{0}/admin/realms/{1}/clients/{2}'.format(os.environ['keycloak_auth_server_url'],
                                                                               os.environ['keycloak_realm_name'], keycloak_id_client)
        keycloak_client = requests.delete(keycloak_client_delete_url, headers={"Authorization": "Bearer {}"
                                          .format(keycloak_token.get("access_token")), "Content-Type": "application/json"})
    except Exception as err:
        logging.error("Failed to remove ssn client from Keycloak", str(err))

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "Action": "Terminate ssn with all service_base_name environment"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
