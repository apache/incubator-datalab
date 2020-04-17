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
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    odahu_conf = dict()
    odahu_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    odahu_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    odahu_conf['endpoint_name'] = (os.environ['endpoint_name']).lower().replace('_', '-')
    odahu_conf['cluster_name'] = "{}-{}".format((os.environ['conf_service_base_name']).lower().replace('_', '-'),
                                                (os.environ['odahu_cluster_name']).lower().replace('_', '-'))
    odahu_conf['tag_name'] = '{}-tag'.format(odahu_conf['service_base_name'])
    odahu_conf['endpoint_tag'] = (os.environ['endpoint_name']).lower().replace('_', '-')
    odahu_conf['project_tag'] = (os.environ['project_name']).lower().replace('_', '-')
    odahu_conf['region'] = os.environ['gcp_region']
    odahu_conf['bucket_name'] = "{}-tfstate".format(odahu_conf['cluster_name'])
    odahu_conf['static_address_name'] = "{}-nat-gw".format(odahu_conf['cluster_name'])
    odahu_conf['keycloak_auth_server_url'] = os.environ['keycloak_auth_server_url']
    odahu_conf['keycloak_realm_name'] = os.environ['keycloak_realm_name']
    odahu_conf['keycloak_client_name'] = os.environ['keycloak_client_name']
    odahu_conf['keycloak_user'] = os.environ['keycloak_user']
    odahu_conf['keycloak_user_password'] = os.environ['keycloak_user_password']
    odahu_conf['root_domain'] = os.environ['odahu_root_domain']
    if 'conf_additional_tags' in os.environ:
        odahu_conf['bucket_additional_tags'] = ';' + os.environ['conf_additional_tags']
        os.environ['conf_additional_tags'] = os.environ['conf_additional_tags'] + \
                                             ';project_tag:{0};endpoint_tag:{1};'.format(
                                                 odahu_conf['project_tag'], odahu_conf['endpoint_tag'])
    else:
        odahu_conf['bucket_additional_tags'] = ''
        os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(odahu_conf['project_tag'],
                                                                                       odahu_conf['endpoint_tag'])
    print('Additional tags will be added: {}'.format(os.environ['conf_additional_tags']))


    try:
        logging.info('[CREATE STATE BUCKETS]')
        print('[CREATE STATE BUCKETS]')

        odahu_conf['bucket_tags'] = 'endpoint_tag:{0};{1}:{2};project_tag:{3};{4}:{5}{6}'\
            .format(odahu_conf['endpoint_tag'], os.environ['conf_billing_tag_key'], os.environ['conf_billing_tag_value'],
                    odahu_conf['project_tag'], odahu_conf['tag_name'], odahu_conf['bucket_name'],
                    odahu_conf['bucket_additional_tags']).replace(';', ',')

        params = "--bucket_name {0} --bucket_tags {1} --region {2} --bucket_name_tag {0}". \
            format(odahu_conf['bucket_name'], odahu_conf['bucket_tags'], odahu_conf['region'])
        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create bucket.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE NAT GATEWAY]')
        print('[CREATE NAT GATEWAY]')
        print("Allocating Elastic IP")
        allocation_id = allocate_elastic_ip()
        tag = {"Key": odahu_conf['tag_name'], "Value": odahu_conf['static_address_name']}
        tag_name = {"Key": "Name", "Value": odahu_conf['static_address_name']}
        create_tag(allocation_id, tag)
        create_tag(allocation_id, tag_name)
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to reserve static ip.", str(err))
        remove_s3(bucket_type='odahu')
        sys.exit(1)

    try:
        print('[CONFIGURE REDIRECT URI]')
        logging.info('[CONFIGURE REDIRECT URI]')
        keycloak_auth_server_url = '{}/realms/master/protocol/openid-connect/token'.format(
            odahu_conf['keycloak_auth_server_url'])
        keycloak_auth_data = {
            "username": odahu_conf['keycloak_user'],
            "password": odahu_conf['keycloak_user_password'],
            "grant_type": "password",
            "client_id": "admin-cli",
        }
        keycloak_client_create_url = '{0}/admin/realms/{1}/clients'.format(odahu_conf['keycloak_auth_server_url'],
                                                                           odahu_conf['keycloak_realm_name'])
        odahu_redirectUris = 'https://odahu.{0}.{1}/*,http://odahu.{0}.{1}/*'.format(odahu_conf['cluster_name'],
                                                                                        odahu_conf['root_domain']).split(',')


        try:
            keycloak_token = requests.post(keycloak_auth_server_url, data=keycloak_auth_data, verify=False).json()
            keycloak_get_Uris = requests.get(keycloak_client_create_url,
                                            headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                     "Content-Type": "application/json"}, verify=False).json()
            for dict in keycloak_get_Uris:
                if dict["clientId"] == odahu_conf['keycloak_client_name']:
                    ui_redirectUris = dict["redirectUris"]
                    keycloak_client_id = dict["id"]
            keycloak_redirectUris = odahu_redirectUris + ui_redirectUris
            updated_client_data = {
                "clientId": odahu_conf['keycloak_client_name'],
                "id": keycloak_client_id,
                "enabled": "true",
                "redirectUris": keycloak_redirectUris,
                "publicClient": "false",
                "protocol": "openid-connect",
            }
            client_url = "{}/{}".format(keycloak_client_create_url, keycloak_client_id)
            keycloak_update_Uris = requests.put(client_url, json=updated_client_data,
                                            headers={"Authorization": "Bearer " + keycloak_token.get("access_token"),
                                                     "Content-Type": "application/json"}, verify=False)
        except Exception as err:
            append_result("Failed to configure keycloak.")
            sys.exit(1)
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to configure keycloak.", str(err))
        remove_s3(bucket_type='odahu')
        release_elastic_ip(allocation_id)
        sys.exit(1)