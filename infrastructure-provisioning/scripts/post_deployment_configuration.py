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

from fabric.api import *
import argparse
import os
import requests

if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument('--keycloak_realm_name', type=str, default='dlab', help='Keycloak Realm name')
    parser.add_argument('--keycloak_auth_server_url', type=str, default='dlab', help='Keycloak auth server URL')
    parser.add_argument('--keycloak_client_name', type=str, default='dlab', help='Keycloak client name')
    parser.add_argument('--keycloak_client_secret', type=str, default='dlab', help='Keycloak client secret')
    parser.add_argument('--keycloak_user', type=str, default='dlab', help='Keycloak user')
    parser.add_argument('--keycloak_user_password', type=str, default='keycloak-user-password',
                        help='Keycloak user password')
    args = parser.parse_args()

    headers = {
        'Metadata-Flavor': 'Google',
    }

    server_external_ip = requests.post('http://metadata/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip', headers=headers).text
    dlab_sbn = requests.post('http://metadata/computeMetadata/v1/instance/name', headers=headers).text
    dlab_zone = requests.post('http://metadata/computeMetadata/v1/instance/zone', headers=headers).text
    dlab_region = '-'.join(dlab_zone.split('-', 2)[:2])
    deployment_vpcId = local("sudo gcloud compute instances describe {0} --zone {1} --format 'value(networkInterfaces.network)' | sed 's|.*/||'".format(dlab_sbn, dlab_zone), capture=True)
    deployment_subnetId = local("sudo gcloud compute instances describe {0} --zone {1} --format 'value(networkInterfaces.subnetwork)' | sed 's|.*/||'".format(dlab_sbn, dlab_zone), capture=True)
    gcp_projectId = requests.get('http://metadata/computeMetadata/v1/project/project-id', headers=headers).text
    keycloak_redirectUri = 'http://{}'.format(server_external_ip)

    local('sed -i "s|DLAB_SBN|{}|g" /opt/dlab/conf/self-service.yml'.format(dlab_sbn))
    local('sed - i "s|KEYCLOAK_REDIRECTURI|{}|g" /opt/dlab/conf/self-service.yml'.format(keycloak_redirectUri))
    local('sed - i "s|KEYCLOAK_REALM_NAME|{}|g" /opt/dlab/conf/self-service.yml'.format(args.keycloak_realm_name))
    local('sed - i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" /opt/dlab/conf/self-service.yml'.format(args.keycloak_auth_server_url))
    local('sed - i "s|KEYCLOAK_CLIENT_NAME|{}|g" /opt/dlab/conf/self-service.yml'.format(args.keycloak_client_name))
    local('sed - i "s|KEYCLOAK_CLIENT_SECRET|{}|g" /opt/dlab/conf/self-service.yml'.format(args.keycloak_client_secret))

    local('sed - i "s|KEYCLOAK_REALM_NAME|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_realm_name))
    local('sed - i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_auth_server_url))
    local('sed - i "s|KEYCLOAK_CLIENT_NAME|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_client_name))
    local('sed - i "s|KEYCLOAK_CLIENT_SECRET|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_client_secret))
    local('sed - i "s|DLAB_SBN|{}|g" /opt/dlab/conf/provisioning.yml'.format(dlab_sbn))
    local('sed - i "s|SUBNET_ID|{}|g" /opt/dlab/conf/provisioning.yml'.format(deployment_subnetId))
    local('sed - i "s|DLAB_REGION|{}|g" /opt/dlab/conf/provisioning.yml'.format(dlab_region))
    local('sed - i "s|DLAB_ZONE|{}|g" /opt/dlab/conf/provisioning.yml'.format(dlab_zone))
    local('sed - i "s|SSN_VPC_ID|{}|g" /opt/dlab/conf/provisioning.yml'.format(deployment_vpcId))
    local('sed - i "s|GCP_PROJECT_ID|{}|g" /opt/dlab/conf/provisioning.yml'.format(gcp_projectId))
    local('sed - i "s|KEYCLOAK_USER|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_user))
    local('sed - i "s|KEYCLOAK_USER_PASSWORD|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_user_password))

    local('sed - i "s|DLAB_SBN|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(dlab_sbn))
    local('sed - i "s|GCP_PROJECT_ID|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(gcp_projectId))
    local('sed - i "s|DLAB_REGION|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(dlab_region))
    local('sed - i "s|DLAB_ZONE|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(dlab_zone))
    local('sed - i "s|KEYCLOAK_REALM_NAME|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.keycloak_realm_name))
    local('sed - i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.keycloak_auth_server_url))
    local('sed - i "s|KEYCLOAK_CLIENT_NAME|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.keycloak_client_name))
    local('sed - i "s|KEYCLOAK_CLIENT_SECRET|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.keycloak_client_secret))
    local('sed - i "s|KEYCLOAK_USER|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.keycloak_user))
    local('sed - i "s|KEYCLOAK_USER_PASSWORD|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(args.keycloak_user_password))

