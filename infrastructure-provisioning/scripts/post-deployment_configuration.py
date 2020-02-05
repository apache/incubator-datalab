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
import requests
import uuid
from Crypto.PublicKey import RSA

if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument('--keycloak_realm_name', type=str, default='KEYCLOAK_REALM_NAME', help='Keycloak Realm name')
    parser.add_argument('--keycloak_auth_server_url', type=str, default='KEYCLOAK_AUTH_SERVER_URL', help='Keycloak auth server URL')
    parser.add_argument('--keycloak_client_name', type=str, default='KEYCLOAK_CLIENT_NAME', help='Keycloak client name')
    parser.add_argument('--keycloak_client_secret', type=str, default='KEYCLOAK_CLIENT_SECRET', help='Keycloak client secret')
    parser.add_argument('--keycloak_user', type=str, default='KEYCLOAK_USER', help='Keycloak user')
    parser.add_argument('--keycloak_admin_password', type=str, default='KEYCLOAK_ADMIN_PASSWORD',
                        help='Keycloak admin password')
    args = parser.parse_args()
    headers = {
        'Metadata-Flavor': 'Google',
    }

    print("Getting cloud and instance parameters")
    server_external_ip = requests.get('http://metadata/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip', headers=headers).text
    dlab_sbn = requests.get('http://metadata/computeMetadata/v1/instance/name', headers=headers).text
    dlab_ssn_static_ip_name = dlab_sbn + '-ip'
    dlab_zone = requests.get('http://metadata/computeMetadata/v1/instance/zone', headers=headers).text.split('/')[-1]
    dlab_region = '-'.join(dlab_zone.split('-', 2)[:2])
    deployment_vpcId = local("sudo gcloud compute instances describe {0} --zone {1} --format 'value(networkInterfaces.network)' | sed 's|.*/||'".format(dlab_sbn, dlab_zone), capture=True)
    deployment_subnetId = local("sudo gcloud compute instances describe {0} --zone {1} --format 'value(networkInterfaces.subnetwork)' | sed 's|.*/||'".format(dlab_sbn, dlab_zone), capture=True)
    gcp_projectId = requests.get('http://metadata/computeMetadata/v1/project/project-id', headers=headers).text
    keycloak_redirectUri = 'http://{}'.format(server_external_ip)

    print("Generationg SSH keyfile for dlab-user")
    key = RSA.generate(2048)
    local("sudo sh -c 'echo \"{}\" >> /home/dlab-user/keys/KEY-FILE.pem'".format(key.exportKey('PEM')))
    local("sudo chmod 600 /home/dlab-user/keys/KEY-FILE.pem")
    pubkey = key.publickey()
    local("sudo sh -c 'echo \"{}\" >> /home/dlab-user/.ssh/authorized_keys'".format(pubkey.exportKey('OpenSSH')))

    print("Generationg MongoDB password")
    mongo_pwd = uuid.uuid4().hex
    try:
        local("sudo echo -e 'db.changeUserPassword(\"admin\", \"{}\")' | mongo dlabdb --port 27017 -u admin -p MONGO_PASSWORD".format(
            mongo_pwd))
        local('sudo sed -i "s|MONGO_PASSWORD|{}|g" /opt/dlab/conf/billing.yml'.format(mongo_pwd))

        local('sudo sed -i "s|MONGO_PASSWORD|{}|g" /opt/dlab/conf/ssn.yml'.format(mongo_pwd))
    except:
        print('Mongo password was already changed')


    print('Reserving external IP')
    static_address_exist = local(
        "sudo gcloud compute addresses list --filter='address={}'".format(server_external_ip), capture=True)
    if static_address_exist:
        print('Address is already static')
    else:
        local("sudo gcloud compute addresses create {0} --addresses {1} --region {2}".format(dlab_ssn_static_ip_name,
                                                                                             server_external_ip,
                                                                                             dlab_region), capture=True)

    print("Overwriting SSN parameters")
    local('sudo sed -i "s|DLAB_SBN|{}|g" /opt/dlab/conf/self-service.yml'.format(dlab_sbn))
    local('sudo sed -i "s|KEYCLOAK_REDIRECTURI|{}|g" /opt/dlab/conf/self-service.yml'.format(keycloak_redirectUri))
    local('sudo sed -i "s|KEYCLOAK_REALM_NAME|{}|g" /opt/dlab/conf/self-service.yml'.format(args.keycloak_realm_name))
    local('sudo sed -i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" /opt/dlab/conf/self-service.yml'.format(
        args.keycloak_auth_server_url))
    local('sudo sed -i "s|KEYCLOAK_CLIENT_NAME|{}|g" /opt/dlab/conf/self-service.yml'.format(args.keycloak_client_name))
    local('sudo sed -i "s|KEYCLOAK_CLIENT_SECRET|{}|g" /opt/dlab/conf/self-service.yml'.format(
        args.keycloak_client_secret))

    local('sudo sed -i "s|KEYCLOAK_REALM_NAME|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_realm_name))
    local('sudo sed -i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" /opt/dlab/conf/provisioning.yml'.format(
        args.keycloak_auth_server_url))
    local('sudo sed -i "s|KEYCLOAK_CLIENT_NAME|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_client_name))
    local('sudo sed -i "s|KEYCLOAK_CLIENT_SECRET|{}|g" /opt/dlab/conf/provisioning.yml'.format(
        args.keycloak_client_secret))
    local('sudo sed -i "s|DLAB_SBN|{}|g" /opt/dlab/conf/provisioning.yml'.format(dlab_sbn))
    local('sudo sed -i "s|SUBNET_ID|{}|g" /opt/dlab/conf/provisioning.yml'.format(deployment_subnetId))
    local('sudo sed -i "s|DLAB_REGION|{}|g" /opt/dlab/conf/provisioning.yml'.format(dlab_region))
    local('sudo sed -i "s|DLAB_ZONE|{}|g" /opt/dlab/conf/provisioning.yml'.format(dlab_zone))
    local('sudo sed -i "s|SSN_VPC_ID|{}|g" /opt/dlab/conf/provisioning.yml'.format(deployment_vpcId))
    local('sudo sed -i "s|GCP_PROJECT_ID|{}|g" /opt/dlab/conf/provisioning.yml'.format(gcp_projectId))
    local('sudo sed -i "s|KEYCLOAK_USER|{}|g" /opt/dlab/conf/provisioning.yml'.format(args.keycloak_user))
    local('sudo sed -i "s|KEYCLOAK_ADMIN_PASSWORD|{}|g" /opt/dlab/conf/provisioning.yml'.format(
        args.keycloak_admin_password))

    local('sudo sed -i "s|DLAB_SBN|{}|g" /opt/dlab/conf/billing.yml'.format(dlab_sbn))

    local(
        'sudo sed -i "s|DLAB_SBN|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            dlab_sbn))
    local(
        'sudo sed -i "s|GCP_PROJECT_ID|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            gcp_projectId))
    local(
        'sudo sed -i "s|DLAB_REGION|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            dlab_region))
    local(
        'sudo sed -i "s|DLAB_ZONE|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            dlab_zone))
    local(
        'sudo sed -i "s|KEYCLOAK_REALM_NAME|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            args.keycloak_realm_name))
    local(
        'sudo sed -i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            args.keycloak_auth_server_url))
    local(
        'sudo sed -i "s|KEYCLOAK_CLIENT_NAME|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            args.keycloak_client_name))
    local(
        'sudo sed -i "s|KEYCLOAK_CLIENT_SECRET|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            args.keycloak_client_secret))
    local(
        'sudo sed -i "s|KEYCLOAK_USER|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            args.keycloak_user))
    local(
        'sudo sed -i "s|KEYCLOAK_ADMIN_PASSWORD|{}|g" /opt/dlab/sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
            args.keycloak_admin_password))

    local('sudo sed -i "s|SERVER_IP|{}|g" /etc/nginx/conf.d/nginx_proxy.conf'.format(server_external_ip))
    local('sudo systemctl restart nginx')
    local('sudo supervisorctl restart all')
    local('cd /opt/dlab/sources/infrastructure-provisioning/src/ && sudo docker-build all')

    print('SUMMARY')
    print('Mongo password stored in /opt/dlab/conf/ssn.yml')
    print('SSH key for dlab-user stored in /home/dlab-user/keys/KEY-FILE.pem')
    if not args:
        print('Keycloak parameters was not set, please configure Keycloak parameters manually')
