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

from dlab.fab import *
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys, os, json
from fabric.api import *
from dlab.ssn_lib import *
import traceback

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        instance = 'ssn'
        
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')

        billing_enabled = True

        ssn_conf = dict()
        # We need to cut service_base_name to 12 symbols do to the Azure Name length limitation
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].replace('_', '-')[:12], '-', True)
        # Check azure predefined resources
        ssn_conf['resource_group_name'] = os.environ.get('azure_resource_group_name', ssn_conf['service_base_name'])
        ssn_conf['vpc_name'] = os.environ.get('azure_vpc_name', '{}-vpc'.format(ssn_conf['service_base_name']))
        ssn_conf['subnet_name'] = os.environ.get('azure_subnet_name', '{}-ssn-subnet'.format(ssn_conf['service_base_name']))
        ssn_conf['security_group_name'] = os.environ.get('azure_security_group_name', '{}-sg'.format(ssn_conf['service_base_name']))
        # Default variables
        ssn_conf['region'] = os.environ['azure_region']
        ssn_conf['ssn_storage_account_name'] = '{}-ssn-storage'.format(ssn_conf['service_base_name'])
        ssn_conf['ssn_container_name'] = '{}-ssn-container'.format(ssn_conf['service_base_name']).lower()
        ssn_conf['shared_storage_account_name'] = '{}-shared-storage'.format(ssn_conf['service_base_name'])
        ssn_conf['shared_container_name'] = '{}-shared-container'.format(ssn_conf['service_base_name']).lower()
        ssn_conf['datalake_store_name'] = '{}-ssn-datalake'.format(ssn_conf['service_base_name'])
        ssn_conf['datalake_shared_directory_name'] = '{}-shared-folder'.format(ssn_conf['service_base_name'])
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        
        ssn_conf['ssh_key_path'] = os.environ['conf_key_dir'] + os.environ['conf_key_name'] + '.pem'
        ssn_conf['dlab_ssh_user'] = os.environ['conf_os_user']
        if os.environ['conf_network_type'] == 'private':
            ssn_conf['instnace_ip'] = AzureMeta().get_private_ip_address(ssn_conf['resource_group_name'],
                                                                        ssn_conf['instance_name'])
        else:
            ssn_conf['instnace_ip'] = AzureMeta().get_instance_public_ip_address(ssn_conf['resource_group_name'],
                                                                        ssn_conf['instance_name'])
        ssn_conf['instance_dns_name'] = 'host-{}.{}.cloudapp.azure.com'.format(ssn_conf['instance_name'], ssn_conf['region'])

        try:
            if os.environ['azure_offer_number'] == '':
                raise KeyError
            if os.environ['azure_currency'] == '':
                raise KeyError
            if os.environ['azure_locale'] == '':
                raise KeyError
            if os.environ['azure_region_info'] == '':
                raise KeyError
        except KeyError:
            billing_enabled = False
        if not billing_enabled:
            os.environ['azure_offer_number'] = 'None'
            os.environ['azure_currency'] = 'None'
            os.environ['azure_locale'] = 'None'
            os.environ['azure_region_info'] = 'None'
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'
    except Exception as err:
        print("Failed to generate variables dictionary." + str(err))
        sys.exit(1)

    def clear_resources():
        if 'azure_resource_group_name' not in os.environ:
            AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
        if 'azure_vpc_name' not in os.environ:
            AzureActions().remove_vpc(ssn_conf['resource_group_name'], ssn_conf['vpc_name'])
        if 'azure_subnet_name' not in os.environ:
            AzureActions().remove_subnet(ssn_conf['resource_group_name'], ssn_conf['vpc_name'],
                                            ssn_conf['subnet_name'])
        if 'azure_security_group_name' not in os.environ:
            AzureActions().remove_security_group(ssn_conf['resource_group_name'], ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(ssn_conf['resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(ssn_conf['resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(ssn_conf['resource_group_name'], storage_account.name)
        for datalake in AzureMeta().list_datalakes(ssn_conf['resource_group_name']):
            if ssn_conf['datalake_store_name'] == datalake.tags["Name"]:
                AzureActions().delete_datalake_store(ssn_conf['resource_group_name'], datalake.name)
        AzureActions().remove_instance(ssn_conf['resource_group_name'], ssn_conf['instance_name'])

    try:
        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (ssn_conf['instnace_ip'], ssn_conf['ssh_key_path'], initial_user, ssn_conf['dlab_ssh_user'], sudo_group)
        local("~/scripts/{}.py {}".format('create_ssh_user', params))
    except Exception as err:
        #print('Error: {0}'.format(err))
        traceback.print_exc()
        clear_resources()
        append_result("Failed creating ssh user 'dlab-user'.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        print('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages 'backoff argparse fabric==1.14.0 pymongo pyyaml pycrypto azure==2.0.0' \
            --user {} --region {}".format(ssn_conf['instnace_ip'], ssn_conf['ssh_key_path'],
                                          ssn_conf['dlab_ssh_user'], ssn_conf['region'])
        local("~/scripts/{}.py {}".format('install_prerequisites', params))
    except Exception as err:
        #print('Error: {0}'.format(err))
        traceback.print_exc()
        clear_resources()
        append_result("Failed installing software: pip, packages.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        print('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/",
                             "service_base_name": ssn_conf['service_base_name'],
                             "security_group_id": ssn_conf['security_group_name'], "vpc_id": ssn_conf['vpc_name'],
                             "subnet_id": ssn_conf['subnet_name'], "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --dlab_path {} --tag_resource_id {}". \
            format(ssn_conf['instnace_ip'], ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'], ssn_conf['service_base_name'])
        local("~/scripts/{}.py {}".format('configure_ssn_node', params))
    except Exception as err:
        #print('Error: {0}'.format(err))
        traceback.print_exc()
        clear_resources()
        append_result("Failed configuring ssn.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        print('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "project", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --dlab_path {} --cloud_provider {} --region {}". \
            format(ssn_conf['instnace_ip'], ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   os.environ['conf_os_family'], ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'],
                   os.environ['conf_cloud_provider'], ssn_conf['region'])
        local("~/scripts/{}.py {}".format('configure_docker', params))
    except Exception as err:
        #print('Error: {0}'.format(err))
        traceback.print_exc()
        clear_resources()
        append_result("Unable to configure docker.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')
        azure_auth_path = '/home/{}/keys/azure_auth.json'.format(ssn_conf['dlab_ssh_user'])
        ldap_login = 'false'

        cloud_params = [
            {
                'key': 'KEYCLOAK_REALM_NAME',
                'value': os.environ['keycloak_realm_name']
            },
            {
                'key': 'KEYCLOAK_AUTH_SERVER_URL',
                'value': os.environ['keycloak_auth_server_url']
            },
            {
                'key': 'KEYCLOAK_CLIENT_NAME',
                'value': os.environ['keycloak_client_name']
            },
            {
                'key': 'KEYCLOAK_CLIENT_SECRET',
                'value': os.environ['keycloak_client_secret']
            },
            {
                'key': 'CONF_OS',
                'value': os.environ['conf_os_family']
            },
            {
                'key': 'SERVICE_BASE_NAME',
                'value': ssn_conf['service_base_name']
            },
            {
                'key': 'EDGE_INSTANCE_SIZE',
                'value': os.environ['azure_edge_instance_size']
            },
            {
                'key': 'SUBNET_ID',
                'value': ssn_conf['subnet_name']
            },
            {
                'key': 'REGION',
                'value': ssn_conf['region']
            },
            {
                'key': 'ZONE',
                'value': ''
            },
            {
                'key': 'TAG_RESOURCE_ID',
                'value': ''
            },
            {
                'key': 'SG_IDS',
                'value': ssn_conf['security_group_name']
            },
            {
                'key': 'SSN_INSTANCE_SIZE',
                'value': os.environ['azure_ssn_instance_size']
            },
            {
                'key': 'VPC_ID',
                'value': ssn_conf['vpc_name']
            },
            {
                'key': 'CONF_KEY_DIR',
                'value': os.environ['conf_key_dir']
            },
            {
                'key': 'LDAP_HOST',
                'value': os.environ['ldap_hostname']
            },
            {
                'key': 'LDAP_DN',
                'value': os.environ['ldap_dn']
            },
            {
                'key': 'LDAP_OU',
                'value': os.environ['ldap_ou']
            },
            {
                'key': 'LDAP_USER_NAME',
                'value': os.environ['ldap_service_username']
            },
            {
                'key': 'LDAP_USER_PASSWORD',
                'value': os.environ['ldap_service_password']
            },
            {
                'key': 'AZURE_RESOURCE_GROUP_NAME',
                'value': ssn_conf['resource_group_name']
            },
            {
                'key': 'AZURE_SSN_STORAGE_ACCOUNT_TAG',
                'value': ssn_conf['ssn_storage_account_name']
            },
            {
                'key': 'AZURE_SHARED_STORAGE_ACCOUNT_TAG',
                'value': ssn_conf['shared_storage_account_name']
            },
            {
                'key': 'SUBNET2_ID',
                'value': ''
            },
            {
                'key': 'VPC2_ID',
                'value': ''
            },
            {
                'key': 'PEERING_ID',
                'value': ''
            }
        ]

        if os.environ['azure_datalake_enable'] == 'false':
            cloud_params.append(
                {
                    'key': 'AZURE_DATALAKE_TAG',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'AZURE_CLIENT_ID',
                    'value': ''
                })
            if os.environ['azure_oauth2_enabled'] == 'false':
                ldap_login = 'true'
            tenant_id = json.dumps(AzureMeta().sp_creds['tenantId']).replace('"', '')
            subscription_id = json.dumps(AzureMeta().sp_creds['subscriptionId']).replace('"', '')
            datalake_application_id = os.environ['azure_application_id']
            datalake_store_name = None
        else:
            cloud_params.append(
                {
                    'key': 'AZURE_DATALAKE_TAG',
                    'value': ssn_conf['datalake_store_name']
                })
            cloud_params.append(
                {
                    'key': 'AZURE_CLIENT_ID',
                    'value': os.environ['azure_application_id']
                })
            tenant_id = json.dumps(AzureMeta().sp_creds['tenantId']).replace('"', '')
            subscription_id = json.dumps(AzureMeta().sp_creds['subscriptionId']).replace('"', '')
            datalake_application_id = os.environ['azure_application_id']
            for datalake in AzureMeta().list_datalakes(ssn_conf['resource_group_name']):
                if ssn_conf['datalake_store_name'] == datalake.tags["Name"]:
                    datalake_store_name = datalake.name
        params = "--hostname {} --keyfile {} --dlab_path {} --os_user {} --os_family {} --request_id {} \
                 --resource {} --service_base_name {} --cloud_provider {} --billing_enabled {} --authentication_file {} \
                 --offer_number {} --currency {} --locale {} --region_info {}  --ldap_login {} --tenant_id {} \
                 --application_id {} --datalake_store_name {} --cloud_params '{}' --subscription_id {}  \
                 --validate_permission_scope {}". \
            format(ssn_conf['instnace_ip'], ssn_conf['ssh_key_path'], os.environ['ssn_dlab_path'],
                   ssn_conf['dlab_ssh_user'], os.environ['conf_os_family'], os.environ['request_id'],
                   os.environ['conf_resource'], ssn_conf['service_base_name'], os.environ['conf_cloud_provider'],
                   billing_enabled, azure_auth_path, os.environ['azure_offer_number'],
                   os.environ['azure_currency'], os.environ['azure_locale'], os.environ['azure_region_info'],
                   ldap_login, tenant_id, datalake_application_id, datalake_store_name, json.dumps(cloud_params),
                   subscription_id, os.environ['azure_validate_permission_scope'])
        local("~/scripts/{}.py {}".format('configure_ui', params))
    except Exception as err:
        #print('Error: {0}'.format(err))
        traceback.print_exc()
        clear_resources()
        append_result("Unable to configure UI.", str(err))
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        for storage_account in AzureMeta().list_storage_accounts(ssn_conf['resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                ssn_storage_account_name = storage_account.name
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                shared_storage_account_name = storage_account.name

        print('[SUMMARY]')
        print("Service base name: {}".format(ssn_conf['service_base_name']))
        print("SSN Name: {}".format(ssn_conf['instance_name']))
        if os.environ['conf_network_type'] == 'public':
            print("SSN Public IP address: {}".format(ssn_conf['instnace_ip']))
            print("SSN Hostname: {}".format(ssn_conf['instance_dns_name']))
        else:
            print("SSN Private IP address: {}".format(ssn_conf['instnace_ip']))
        print("Key name: {}".format(os.environ['conf_key_name']))
        print("VPC Name: {}".format(ssn_conf['vpc_name']))
        print("Subnet Name: {}".format(ssn_conf['subnet_name']))
        print("Firewall Names: {}".format(ssn_conf['security_group_name']))
        print("SSN instance size: {}".format(os.environ['azure_ssn_instance_size']))
        print("SSN storage account name: {}".format(ssn_storage_account_name))
        print("SSN container name: {}".format(ssn_conf['ssn_container_name']))
        print("Shared storage account name: {}".format(shared_storage_account_name))
        print("Shared container name: {}".format(ssn_conf['shared_container_name']))
        if os.environ['azure_datalake_enable'] == 'true':
            for datalake in AzureMeta().list_datalakes(ssn_conf['resource_group_name']):
                if ssn_conf['datalake_store_name'] == datalake.tags["Name"]:
                    datalake_store_name = datalake.name
            print("DataLake store name: {}".format(datalake_store_name))
            print("DataLake shared directory name: {}".format(ssn_conf['datalake_shared_directory_name']))
        print("Region: {}".format(ssn_conf['region']))
        jenkins_url = "http://{}/jenkins".format(ssn_conf['instnace_ip'])
        jenkins_url_https = "https://{}/jenkins".format(ssn_conf['instnace_ip'])
        print("Jenkins URL: {}".format(jenkins_url))
        print("Jenkins URL HTTPS: {}".format(jenkins_url_https))
        print("DLab UI HTTP URL: http://{}".format(ssn_conf['instnace_ip']))
        print("DLab UI HTTPS URL: https://{}".format(ssn_conf['instnace_ip']))

        try:
            with open('jenkins_creds.txt') as f:
                print(f.read())
        except Exception as err:
            print('Error: {0}'.format(err))
            print("Jenkins is either configured already or have issues in configuration routine.")

        with open("/root/result.json", 'w') as f:
            if os.environ['azure_datalake_enable'] == 'false':
                res = {"service_base_name": ssn_conf['service_base_name'],
                       "instance_name": ssn_conf['instance_name'],
                       "instance_hostname": ssn_conf['instnace_ip'],
                       "master_keyname": os.environ['conf_key_name'],
                       "vpc_id": ssn_conf['vpc_name'],
                       "subnet_id": ssn_conf['subnet_name'],
                       "security_id": ssn_conf['security_group_name'],
                       "instance_shape": os.environ['azure_ssn_instance_size'],
                       "ssn_storage_account_name": ssn_storage_account_name,
                       "ssn_container_name": ssn_conf['ssn_container_name'],
                       "shared_storage_account_name": shared_storage_account_name,
                       "shared_container_name": ssn_conf['shared_container_name'],
                       "region": ssn_conf['region'],
                       "action": "Create SSN instance"}
            else:
                res = {"service_base_name": ssn_conf['service_base_name'],
                       "instance_name": ssn_conf['instance_name'],
                       "instance_hostname": ssn_conf['instnace_ip'],
                       "master_keyname": os.environ['conf_key_name'],
                       "vpc_id": ssn_conf['vpc_name'],
                       "subnet_id": ssn_conf['subnet_name'],
                       "security_id": ssn_conf['security_group_name'],
                       "instance_shape": os.environ['azure_ssn_instance_size'],
                       "ssn_storage_account_name": ssn_storage_account_name,
                       "ssn_container_name": ssn_conf['ssn_container_name'],
                       "shared_storage_account_name": shared_storage_account_name,
                       "shared_container_name": ssn_conf['shared_container_name'],
                       "datalake_name": datalake_store_name,
                       "datalake_shared_directory_name": ssn_conf['datalake_shared_directory_name'],
                       "region": ssn_conf['region'],
                       "action": "Create SSN instance"}
            f.write(json.dumps(res))

        print('Upload response file')
        params = "--instance_name {} --local_log_filepath {} --os_user {} --instance_hostname {}".\
            format(ssn_conf['instance_name'], local_log_filepath, ssn_conf['dlab_ssh_user'], ssn_conf['instnace_ip'])
        local("~/scripts/{}.py {}".format('upload_response_file', params))
    except:
        sys.exit(1)
