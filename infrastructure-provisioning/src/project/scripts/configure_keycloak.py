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

import logging
import json
import sys
import requests
import argparse
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os

parser = argparse.ArgumentParser()
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--keycloak_auth_server_url', type=str, default='')
parser.add_argument('--keycloak_realm_name', type=str, default='')
parser.add_argument('--keycloak_user', type=str, default='')
parser.add_argument('--keycloak_user_password', type=str, default='')
parser.add_argument('--keycloak_client_secret', type=str, default='')
parser.add_argument('--edge_public_ip', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
args = parser.parse_args()

##############
# Run script #
##############
if __name__ == "__main__":
    try:
        print('[CONFIGURE KEYCLOAK]')
        logging.info('[CONFIGURE KEYCLOAK]')
        keycloak_auth_server_url = '{}/realms/master/protocol/openid-connect/token'.format(
            args.keycloak_auth_server_url)
        keycloak_auth_data = {
            "username": args.keycloak_user,
            "password": args.keycloak_user_password,
            "grant_type": "password",
            "client_id": "admin-cli",
        }

        keycloak_client_create_url = '{0}/admin/realms/{1}/clients'.format(args.keycloak_auth_server_url,
                                                                           args.keycloak_realm_name)
        keycloak_client_name = "{0}-{1}".format(args.service_base_name, args.project_name)
        keycloak_client_id = str(uuid.uuid4())
        keycloak_client_data = {
            "clientId": keycloak_client_name,
            "id": keycloak_client_id,
            "enabled": "true",
            "redirectUris": ["https://{}/*".format(args.edge_public_ip)],
            "publicClient": "false",
            "secret": args.keycloak_client_secret,
            "protocol": "openid-connect",
        }

        try:
            keycloak_token = requests.post(keycloak_auth_server_url, data=keycloak_auth_data, verify=False).json()

            keycloak_client = requests.post(keycloak_client_create_url, json=keycloak_client_data,
                                            headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                     "Content-Type": "application/json"}, verify=False)

        except Exception as err:
            append_result("Failed to configure keycloak.")
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to configure keycloak.", str(err))