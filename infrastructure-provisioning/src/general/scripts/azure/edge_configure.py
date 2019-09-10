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
from dlab.fab import *
from dlab.meta_lib import *
import sys, time, os
from dlab.actions_lib import *

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        print('Generating infrastructure names and tags')
        edge_conf = dict()

        edge_conf['service_base_name'] = os.environ['conf_service_base_name']
        edge_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        edge_conf['key_name'] = os.environ['conf_key_name']
        edge_conf['vpc_name'] = os.environ['azure_vpc_name']
        edge_conf['region'] = os.environ['azure_region']
        edge_conf['subnet_name'] = os.environ['azure_subnet_name']
        edge_conf['project_name'] = os.environ['project_name'].lower().replace('_', '-')
        edge_conf['user_keyname'] = os.environ['project_name']
        edge_conf['private_subnet_name'] = edge_conf['service_base_name'] + '-' + edge_conf['project_name'] + '-subnet'
        edge_conf['instance_name'] = edge_conf['service_base_name'] + "-" + edge_conf['project_name'] + '-edge'
        edge_conf['network_interface_name'] = edge_conf['service_base_name'] + "-" + edge_conf['project_name'] + \
                                              '-edge-nif'
        edge_conf['static_public_ip_name'] = edge_conf['service_base_name'] + "-" + edge_conf['project_name'] + \
                                             '-edge-ip'
        edge_conf['primary_disk_name'] = edge_conf['instance_name'] + '-disk0'
        edge_conf['instance_dns_name'] = 'host-' + edge_conf['instance_name'] + '.' + edge_conf['region'] + \
                                         '.cloudapp.azure.com'
        edge_conf['user_storage_account_name'] = edge_conf['service_base_name'] + '-' + edge_conf[
            'project_name'] + '-storage'
        edge_conf['user_container_name'] = (edge_conf['service_base_name'] + '-' + edge_conf['project_name'] +
                                            '-container').lower()
        edge_conf['shared_storage_account_name'] = edge_conf['service_base_name'] + '-shared-storage'
        edge_conf['shared_container_name'] = (edge_conf['service_base_name'] + '-shared-container').lower()
        edge_conf['datalake_store_name'] = edge_conf['service_base_name'] + '-ssn-datalake'
        edge_conf['datalake_shared_directory_name'] = edge_conf['service_base_name'] + '-shared-folder'
        edge_conf['datalake_user_directory_name'] = '{0}-{1}-folder'.format(edge_conf['service_base_name'],
                                                                            edge_conf['project_name'])
        edge_conf['edge_security_group_name'] = edge_conf['instance_name'] + '-sg'
        edge_conf['notebook_security_group_name'] = edge_conf['service_base_name'] + "-" + edge_conf['project_name'] + \
                                                    '-nb-sg'
        edge_conf['master_security_group_name'] = edge_conf['service_base_name'] + '-' \
                                                    + edge_conf['project_name'] + '-dataengine-master-sg'
        edge_conf['slave_security_group_name'] = edge_conf['service_base_name'] + '-' \
                                                   + edge_conf['project_name'] + '-dataengine-slave-sg'
        edge_conf['dlab_ssh_user'] = os.environ['conf_os_user']
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], edge_conf['key_name'])
        edge_conf['private_subnet_cidr'] = AzureMeta().get_subnet(edge_conf['resource_group_name'],
                                                                  edge_conf['vpc_name'],
                                                                  edge_conf['private_subnet_name']).address_prefix
        if os.environ['conf_network_type'] == 'private':
            edge_conf['edge_private_ip'] = AzureMeta().get_private_ip_address(edge_conf['resource_group_name'],
                                                                              edge_conf['instance_name'])
            edge_conf['edge_public_ip'] =  edge_conf['edge_private_ip']
        else:
            edge_conf['edge_public_ip'] = AzureMeta().get_instance_public_ip_address(edge_conf['resource_group_name'],
                                                                        edge_conf['instance_name'])
            edge_conf['edge_private_ip'] = AzureMeta().get_private_ip_address(edge_conf['resource_group_name'],
                                                                                    edge_conf['instance_name'])
        instance_hostname = AzureMeta().get_private_ip_address(edge_conf['resource_group_name'],
                                                                        edge_conf['instance_name'])
        edge_conf['vpc_cidrs'] = AzureMeta().get_vpc(edge_conf['resource_group_name'],
                                                      edge_conf['vpc_name']).address_space.address_prefixes
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to generate infrastructure names", str(err))
        AzureActions().remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions().remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['notebook_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['master_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                                 edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions().remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (instance_hostname, os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem", initial_user,
             edge_conf['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed creating ssh user 'dlab'.", str(err))
        AzureActions().remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions().remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['notebook_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['master_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                                 edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions().remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])
        sys.exit(1)

    try:
        print('[INSTALLING PREREQUISITES]')
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(instance_hostname, keyfile_name, edge_conf['dlab_ssh_user'], os.environ['azure_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing apps: apt & pip.", str(err))
        AzureActions().remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions().remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['notebook_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['master_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                                 edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions().remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])
        sys.exit(1)

    try:
        print('[INSTALLING HTTP PROXY]')
        logging.info('[INSTALLING HTTP PROXY]')
        additional_config = {"exploratory_subnet": edge_conf['private_subnet_cidr'],
                             "template_file": "/root/templates/squid.conf",
                             "project_name": os.environ['project_name'],
                             "ldap_host": os.environ['ldap_hostname'],
                             "ldap_dn": os.environ['ldap_dn'],
                             "ldap_user": os.environ['ldap_service_username'],
                             "ldap_password": os.environ['ldap_service_password'],
                             "vpc_cidrs": edge_conf['vpc_cidrs'],
                             "allowed_ip_cidr": ['0.0.0.0/0']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}" \
                 .format(instance_hostname, keyfile_name, json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_http_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing http proxy.", str(err))
        AzureActions().remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions().remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['notebook_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['master_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                                 edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions().remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])
        sys.exit(1)


    try:
        print('[INSTALLING USERs KEY]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": edge_conf['user_keyname'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing users key. Excpeption: " + str(err))
        AzureActions().remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions().remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['notebook_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['master_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                                 edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions().remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])
        sys.exit(1)

    try:
        print('[INSTALLING NGINX REVERSE PROXY]')
        logging.info('[INSTALLING NGINX REVERSE PROXY]')
        params = "--hostname {} --keyfile {} --user {}" \
            .format(instance_hostname, keyfile_name, edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_nginx_reverse_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing Nginx reverse proxy. Excpeption: " + str(err))
        AzureActions().remove_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
        AzureActions().remove_subnet(edge_conf['resource_group_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['notebook_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                             edge_conf['master_security_group_name'])
        AzureActions().remove_security_group(edge_conf['resource_group_name'],
                                                 edge_conf['slave_security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(edge_conf['resource_group_name'], storage_account.name)
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    AzureActions().remove_datalake_directory(datalake.name, edge_conf['datalake_user_directory_name'])
        sys.exit(1)

    try:
        for storage_account in AzureMeta().list_storage_accounts(edge_conf['resource_group_name']):
            if edge_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                shared_storage_account_name = storage_account.name
            if edge_conf['user_storage_account_name'] == storage_account.tags["Name"]:
                user_storage_account_name = storage_account.name

        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(edge_conf['instance_name']))
        print("Hostname: {}".format(edge_conf['instance_dns_name']))
        print("Public IP: {}".format(edge_conf['edge_public_ip']))
        print("Private IP: {}".format(edge_conf['edge_private_ip']))
        print("Key name: {}".format(edge_conf['key_name']))
        print("User storage account name: {}".format(user_storage_account_name))
        print("User container name: {}".format(edge_conf['user_container_name']))
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(edge_conf['resource_group_name']):
                if edge_conf['datalake_store_name'] == datalake.tags["Name"]:
                    datalake_id = datalake.name
            print("Data Lake name: {}".format(datalake_id))
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
                       "user_storage_account_name": user_storage_account_name,
                       "user_container_name": edge_conf['user_container_name'],
                       "shared_storage_account_name": shared_storage_account_name,
                       "shared_container_name": edge_conf['shared_container_name'],
                       "user_storage_account_tag_name": edge_conf['user_storage_account_name'],
                       "tunnel_port": "22",
                       "socks_port": "1080",
                       "notebook_sg": edge_conf['notebook_security_group_name'],
                       "edge_sg": edge_conf['edge_security_group_name'],
                       "notebook_subnet": edge_conf['private_subnet_cidr'],
                       "instance_id": edge_conf['instance_name'],
                       "full_edge_conf": edge_conf,
                       "project_name": os.environ['project_name'],
                       "@class": "com.epam.dlab.dto.azure.edge.EdgeInfoAzure",
                       "Action": "Create new EDGE server"}
            else:
                res = {"hostname": edge_conf['instance_dns_name'],
                       "public_ip": edge_conf['edge_public_ip'],
                       "ip": edge_conf['edge_private_ip'],
                       "key_name": edge_conf['key_name'],
                       "user_storage_account_name": user_storage_account_name,
                       "user_container_name": edge_conf['user_container_name'],
                       "shared_storage_account_name": shared_storage_account_name,
                       "shared_container_name": edge_conf['shared_container_name'],
                       "user_storage_account_tag_name": edge_conf['user_storage_account_name'],
                       "datalake_name": datalake_id,
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
                       "project_name": os.environ['project_name'],
                       "@class": "com.epam.dlab.dto.azure.edge.EdgeInfoAzure",
                       "Action": "Create new EDGE server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)
