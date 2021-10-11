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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import uuid
import subprocess
from fabric import *

if __name__ == "__main__":
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
        logging.info('Generating infrastructure names and tags')
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        edge_conf = dict()
        edge_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
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
                                                                             edge_conf['endpoint_name']).lower()
        edge_conf['user_container_name'] = '{0}-{1}-{2}-bucket'.format(edge_conf['service_base_name'],
                                                                       edge_conf['project_name'],
                                                                       edge_conf['endpoint_name']).lower()
        edge_conf['shared_storage_account_name'] = '{0}-{1}-shared-bucket'.format(edge_conf['service_base_name'],
                                                                                  edge_conf['endpoint_name']).lower()
        edge_conf['shared_container_name'] = '{0}-{1}-shared-bucket'.format(edge_conf['service_base_name'],
                                                                            edge_conf['endpoint_name']).lower()
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
        edge_conf['datalab_ssh_user'] = os.environ['conf_os_user']
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
        datalab.fab.append_result("Failed to generate infrastructure names", str(err))
        clear_resources()
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            edge_conf['initial_user'] = 'ubuntu'
            edge_conf['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            edge_conf['initial_user'] = 'ec2-user'
            edge_conf['sudo_group'] = 'wheel'

        logging.info('[CREATING DATALAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            edge_conf['instance_hostname'], os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem",
            edge_conf['initial_user'], edge_conf['datalab_ssh_user'], edge_conf['sudo_group'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed creating ssh user 'datalab'.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {} --region {}".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], edge_conf['datalab_ssh_user'],
            os.environ['azure_region'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing apps: apt & pip.", str(err))
        clear_resources()
        sys.exit(1)

    try:
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
            edge_conf['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_http_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing http proxy.", str(err))
        clear_resources()
        sys.exit(1)


    try:
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": edge_conf['user_keyname'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], json.dumps(additional_config),
            edge_conf['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing users key. Excpeption: " + str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[INSTALLING NGINX REVERSE PROXY]')
        edge_conf['keycloak_client_secret'] = str(uuid.uuid4())
        params = "--hostname {} --keyfile {} --user {} --keycloak_client_id {} --keycloak_client_secret {} " \
                 "--step_cert_sans '{}'".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], edge_conf['datalab_ssh_user'],
            edge_conf['service_base_name'] + '-' + edge_conf['project_name'] + '-' + edge_conf['endpoint_name'],
            edge_conf['keycloak_client_secret'], edge_conf['step_cert_sans'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_nginx_reverse_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
        if os.environ['conf_letsencrypt_enabled'] == 'true' and 'conf_letsencrypt_domain_name' in os.environ:
            edge_conf['edge_hostname'] = '{}.{}'.format(edge_conf['project_name'], os.environ['conf_letsencrypt_domain_name'])
        else:
            edge_conf['edge_hostname'] = "''"
        keycloak_params = "--service_base_name {} --keycloak_auth_server_url {} --keycloak_realm_name {} " \
                          "--keycloak_user {} --keycloak_user_password {} --keycloak_client_secret {} " \
                          "--instance_public_ip {} --project_name {} --endpoint_name {} --hostname {} ".format(
                           edge_conf['service_base_name'], os.environ['keycloak_auth_server_url'],
                           os.environ['keycloak_realm_name'], os.environ['keycloak_user'],
                           os.environ['keycloak_user_password'],
                           edge_conf['keycloak_client_secret'], edge_conf['instance_hostname'], edge_conf['project_name'],
                           edge_conf['endpoint_name'], edge_conf['edge_hostname'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_keycloak', keycloak_params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing Nginx reverse proxy. Excpeption: " + str(err))
        clear_resources()
        sys.exit(1)

    try:
        for storage_account in AzureMeta.list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                edge_conf['shared_storage_account_name'] = storage_account.name
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                edge_conf['user_storage_account_name'] = storage_account.name

        logging.info('[SUMMARY]')
        logging.info("Instance name: {}".format(edge_conf['instance_name']))
        logging.info("Hostname: {}".format(edge_conf['instance_dns_name']))
        logging.info("Public IP: {}".format(edge_conf['edge_public_ip']))
        logging.info("Private IP: {}".format(edge_conf['edge_private_ip']))
        logging.info("Key name: {}".format(edge_conf['key_name']))
        logging.info("User storage account name: {}".format(edge_conf['user_storage_account_name']))
        logging.info("User container name: {}".format(edge_conf['user_container_name']))
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta.list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    edge_conf['datalake_id'] = datalake.name
            logging.info("Data Lake name: {}".format(edge_conf['datalake_id']))
            logging.info("Data Lake tag name: {}".format(edge_conf['datalake_store_name']))
            logging.info("Data Lake Store user directory name: {}".format(edge_conf['datalake_user_directory_name']))
        logging.info("Notebook SG: {}".format(edge_conf['notebook_security_group_name']))
        logging.info("Edge SG: {}".format(edge_conf['edge_security_group_name']))
        logging.info("Notebook subnet: {}".format(edge_conf['private_subnet_cidr']))
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
                       "@class": "com.epam.datalab.dto.azure.edge.EdgeInfoAzure",
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
                       "@class": "com.epam.datalab.dto.azure.edge.EdgeInfoAzure",
                       "Action": "Create new EDGE server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results.", str(err))
        clear_resources()
        sys.exit(1)
