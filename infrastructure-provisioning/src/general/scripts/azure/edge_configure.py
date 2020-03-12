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
import sys
import time
import os
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import logging
import traceback
import uuid
from fabric.api import *

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    def clear_resources():
        AzureActions.remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions.remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                   edge_conf['private_subnet_name'])
        AzureActions.remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions.remove_security_group(edge_conf['resource_group_name'],
                                           edge_conf['notebook_security_group_name'])
        AzureActions.remove_security_group(edge_conf['resource_group_name'],
                                           edge_conf['master_security_group_name'])
        AzureActions.remove_security_group(edge_conf['resource_group_name'],
                                           edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta.list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta.list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions.remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])

    try:
        print('Generating infrastructure names and tags')
        AzureMeta = dlab.meta_lib.AzureMeta()
        AzureActions = dlab.actions_lib.AzureActions()
        edge_conf = dict()
        edge_conf['service_base_name'] = os.environ['conf_service_base_name'] = dlab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        edge_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        edge_conf['key_name'] = os.environ['conf_key_name']
        edge_conf['vpc_name'] = os.environ['azure_vpc_name']
        edge_conf['region'] = os.environ['azure_region']
        edge_conf['subnet_name'] = os.environ['azure_subnet_name']
        edge_conf['project_name'] = (os.environ['project_name'])
        edge_conf['endpoint_name'] = (os.environ['endpoint_name'])
        edge_conf['user_keyname'] = edge_conf['project_name']
        edge_conf['private_subnet_name'] = '{}-{}-{}-subnet'.format(edge_conf['service_base_name'],
                                                                    edge_conf['project_name'],
                                                                    edge_conf['endpoint_name'])
        edge_conf['instance_name'] = '{0}-{1}-{2}-edge'.format(edge_conf['service_base_name'],
                                                               edge_conf['project_name'], edge_conf['endpoint_name'])
        edge_conf['instance_dns_name'] = 'host-{}.{}.cloudapp.azure.com'.format(edge_conf['instance_name'],
                                                                                edge_conf['region'])
        edge_conf['user_storage_account_name'] = '{0}-{1}-{2}-bucket'.format(edge_conf['service_base_name'],
                                                                             edge_conf['project_name'],
                                                                             edge_conf['endpoint_name'])
        edge_conf['user_container_name'] = '{0}-{1}-{2}-bucket'.format(edge_conf['service_base_name'],
                                                                       edge_conf['project_name'],
                                                                       edge_conf['endpoint_name'])
        edge_conf['shared_storage_account_name'] = '{0}-{1}-shared-bucket'.format(edge_conf['service_base_name'],
                                                                                  edge_conf['endpoint_name'])
        edge_conf['shared_container_name'] = '{0}-{1}-shared-bucket'.format(edge_conf['service_base_name'],
                                                                            edge_conf['endpoint_name'])
        edge_conf['datalake_store_name'] = '{}-ssn-datalake'.format(edge_conf['service_base_name'])
        edge_conf['datalake_shared_directory_name'] = '{}-shared-folder'.format(edge_conf['service_base_name'])
        edge_conf['datalake_user_directory_name'] = '{0}-{1}-{2}-folder'.format(edge_conf['service_base_name'],
                                                                                edge_conf['project_name'],
                                                                                edge_conf['endpoint_name'])
        edge_conf['edge_security_group_name'] = '{}-sg'.format(edge_conf['instance_name'])
        edge_conf['notebook_security_group_name'] = '{}-{}-{}-nb-sg'.format(edge_conf['service_base_name'],
                                                                            edge_conf['project_name'],
                                                                            edge_conf['endpoint_name'])
        edge_conf['master_security_group_name'] = '{}-{}-{}-de-master-sg'.format(edge_conf['service_base_name'],
                                                                                 edge_conf['project_name'],
                                                                                 edge_conf['endpoint_name'])
        edge_conf['slave_security_group_name'] = '{}-{}-{}-de-slave-sg'.format(edge_conf['service_base_name'],
                                                                               edge_conf['project_name'],
                                                                               edge_conf['endpoint_name'])
        edge_conf['dlab_ssh_user'] = os.environ['conf_os_user']
        edge_conf['keyfile_name'] = "{}{}.pem".format(os.environ['conf_key_dir'], edge_conf['key_name'])
        edge_conf['private_subnet_cidr'] = AzureMeta.get_subnet(edge_conf['resource_group_name'],
                                                                edge_conf['vpc_name'],
                                                                edge_conf['private_subnet_name']).address_prefix
        if os.environ['conf_network_type'] == 'private':
            edge_conf['edge_private_ip'] = AzureMeta.get_private_ip_address(edge_conf['resource_group_name'],
                                                                            edge_conf['instance_name'])
            edge_conf['edge_public_ip'] = edge_conf['edge_private_ip']
            edge_conf['instance_hostname'] = edge_conf['edge_private_ip']
        else:
            edge_conf['edge_public_ip'] = AzureMeta.get_instance_public_ip_address(edge_conf['resource_group_name'],
                                                                                   edge_conf['instance_name'])
            edge_conf['edge_private_ip'] = AzureMeta.get_private_ip_address(edge_conf['resource_group_name'],
                                                                            edge_conf['instance_name'])
            edge_conf['instance_hostname'] = edge_conf['instance_dns_name']
        edge_conf['vpc_cidrs'] = AzureMeta.get_vpc(edge_conf['resource_group_name'],
                                                   edge_conf['vpc_name']).address_space.address_prefixes

        if os.environ['conf_stepcerts_enabled'] == 'true':
            edge_conf['step_cert_sans'] = ' --san {0} '.format(AzureMeta.get_private_ip_address(
                edge_conf['resource_group_name'], edge_conf['instance_name']))
            if os.environ['conf_network_type'] == 'public':
                edge_conf['step_cert_sans'] += ' --san {0} --san {1} '.format(
                    AzureMeta.get_instance_public_ip_address(edge_conf['resource_group_name'],
                                                             edge_conf['instance_name']),
                    edge_conf['instance_dns_name'])
        else:
            edge_conf['step_cert_sans'] = ''

    except Exception as err:
        dlab.fab.append_result("Failed to generate infrastructure names", str(err))
        clear_resources()
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            edge_conf['initial_user'] = 'ubuntu'
            edge_conf['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            edge_conf['initial_user'] = 'ec2-user'
            edge_conf['sudo_group'] = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            edge_conf['instance_hostname'], os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem",
            edge_conf['initial_user'], edge_conf['dlab_ssh_user'], edge_conf['sudo_group'])

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed creating ssh user 'dlab'.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        print('[INSTALLING PREREQUISITES]')
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {} --region {}".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], edge_conf['dlab_ssh_user'],
            os.environ['azure_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing apps: apt & pip.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        print('[INSTALLING HTTP PROXY]')
        logging.info('[INSTALLING HTTP PROXY]')
        additional_config = {"exploratory_subnet": edge_conf['private_subnet_cidr'],
                             "template_file": "/root/templates/squid.conf",
                             "project_name": edge_conf['project_name'],
                             "ldap_host": os.environ['ldap_hostname'],
                             "ldap_dn": os.environ['ldap_dn'],
                             "ldap_user": os.environ['ldap_service_username'],
                             "ldap_password": os.environ['ldap_service_password'],
                             "vpc_cidrs": edge_conf['vpc_cidrs'],
                             "allowed_ip_cidr": ['0.0.0.0/0']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], json.dumps(additional_config),
            edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_http_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing http proxy.", str(err))
        clear_resources()
        sys.exit(1)


    try:
        print('[INSTALLING USERs KEY]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": edge_conf['user_keyname'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], json.dumps(additional_config),
            edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing users key. Excpeption: " + str(err))
        clear_resources()
        sys.exit(1)

    try:
        print('[INSTALLING NGINX REVERSE PROXY]')
        logging.info('[INSTALLING NGINX REVERSE PROXY]')
        edge_conf['keycloak_client_secret'] = str(uuid.uuid4())
        params = "--hostname {} --keyfile {} --user {} --keycloak_client_id {} --keycloak_client_secret {} " \
                 "--step_cert_sans '{}'".format(
                  edge_conf['instance_hostname'], edge_conf['keyfile_name'], edge_conf['dlab_ssh_user'],
                  edge_conf['service_base_name'] + '-' + edge_conf['project_name'] + '-' + edge_conf['endpoint_name'],
                  edge_conf['keycloak_client_secret'], edge_conf['step_cert_sans'])

        try:
            local("~/scripts/{}.py {}".format('configure_nginx_reverse_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
        keycloak_params = "--service_base_name {} --keycloak_auth_server_url {} --keycloak_realm_name {} " \
                          "--keycloak_user {} --keycloak_user_password {} --keycloak_client_secret {} " \
                          "--edge_public_ip {} --project_name {} --endpoint_name {} ".format(
                           edge_conf['service_base_name'], os.environ['keycloak_auth_server_url'],
                           os.environ['keycloak_realm_name'], os.environ['keycloak_user'],
                           os.environ['keycloak_user_password'],
                           edge_conf['keycloak_client_secret'], edge_conf['instance_hostname'], edge_conf['project_name'],
                           edge_conf['endpoint_name'])
        try:
            local("~/scripts/{}.py {}".format('configure_keycloak', keycloak_params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing Nginx reverse proxy. Excpeption: " + str(err))
        clear_resources()
        sys.exit(1)

    try:
        for storage_account in AzureMeta.list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                edge_conf['shared_storage_account_name'] = storage_account.name
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                edge_conf['user_storage_account_name'] = storage_account.name

        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(edge_conf['instance_name']))
        print("Hostname: {}".format(edge_conf['instance_dns_name']))
        print("Public IP: {}".format(edge_conf['edge_public_ip']))
        print("Private IP: {}".format(edge_conf['edge_private_ip']))
        print("Key name: {}".format(edge_conf['key_name']))
        print("User storage account name: {}".format(edge_conf['user_storage_account_name']))
        print("User container name: {}".format(edge_conf['user_container_name']))
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta.list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    edge_conf['datalake_id'] = datalake.name
            print("Data Lake name: {}".format(edge_conf['datalake_id']))
            print("Data Lake tag name: {}".format(edge_conf['datalake_store_name']))
            print("Data Lake Store user directory name: {}".format(edge_conf['datalake_user_directory_name']))
        print("Notebook SG: {}".format(edge_conf['notebook_security_group_name']))
        print("Edge SG: {}".format(edge_conf['edge_security_group_name']))
        print("Notebook subnet: {}".format(edge_conf['private_subnet_cidr']))
        with open("/root/result.json", 'w') as result:
            if os.environ['azure_datalake_enable'] == 'false':
                res = {"hostname": edge_conf['instance_dns_name'],
                       "public_ip": edge_conf['edge_public_ip'],
                       "ip": edge_conf['edge_private_ip'],
                       "key_name": edge_conf['key_name'],
                       "user_storage_account_name": edge_conf['user_storage_account_name'],
                       "user_container_name": edge_conf['user_container_name'],
                       "shared_storage_account_name": edge_conf['shared_storage_account_name'],
                       "shared_container_name": edge_conf['shared_container_name'],
                       "user_storage_account_tag_name": edge_conf['user_storage_account_name'],
                       "tunnel_port": "22",
                       "socks_port": "1080",
                       "notebook_sg": edge_conf['notebook_security_group_name'],
                       "edge_sg": edge_conf['edge_security_group_name'],
                       "notebook_subnet": edge_conf['private_subnet_cidr'],
                       "instance_id": edge_conf['instance_name'],
                       "full_edge_conf": edge_conf,
                       "project_name": edge_conf['project_name'],
                       "@class": "com.epam.dlab.dto.azure.edge.EdgeInfoAzure",
                       "Action": "Create new EDGE server"}
            else:
                res = {"hostname": edge_conf['instance_dns_name'],
                       "public_ip": edge_conf['edge_public_ip'],
                       "ip": edge_conf['edge_private_ip'],
                       "key_name": edge_conf['key_name'],
                       "user_storage_account_name": edge_conf['user_storage_account_name'],
                       "user_container_name": edge_conf['user_container_name'],
                       "shared_storage_account_name": edge_conf['shared_storage_account_name'],
                       "shared_container_name": edge_conf['shared_container_name'],
                       "user_storage_account_tag_name": edge_conf['user_storage_account_name'],
                       "datalake_name": edge_conf['datalake_id'],
                       "datalake_tag_name": edge_conf['datalake_store_name'],
                       "datalake_shared_directory_name": edge_conf['datalake_shared_directory_name'],
                       "datalake_user_directory_name": edge_conf['datalake_user_directory_name'],
                       "tunnel_port": "22",
                       "socks_port": "1080",
                       "notebook_sg": edge_conf['notebook_security_group_name'],
                       "edge_sg": edge_conf['edge_security_group_name'],
                       "notebook_subnet": edge_conf['private_subnet_cidr'],
                       "instance_id": edge_conf['instance_name'],
                       "full_edge_conf": edge_conf,
                       "project_name": edge_conf['project_name'],
                       "@class": "com.epam.dlab.dto.azure.edge.EdgeInfoAzure",
                       "Action": "Create new EDGE server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results.", str(err))
        clear_resources()
        sys.exit(1)
