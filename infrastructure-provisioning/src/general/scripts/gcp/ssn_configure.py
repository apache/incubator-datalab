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

import argparse
import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import subprocess
import uuid
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--ssn_unique_index', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    def clear_resources():
        GCPActions.remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions.remove_static_address(ssn_conf['static_address_name'], ssn_conf['region'])
        GCPActions.remove_service_account(ssn_conf['service_account_name'], ssn_conf['service_base_name'])
        GCPActions.remove_role(ssn_conf['role_name'])
        if not ssn_conf['pre_defined_firewall']:
            GCPActions.remove_firewall('{}-ingress'.format(ssn_conf['firewall_name']))
            GCPActions.remove_firewall('{}-egress'.format(ssn_conf['firewall_name']))
        if  not ssn_conf['pre_defined_subnet']:
            GCPActions.remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if not ssn_conf['pre_defined_vpc']:
            GCPActions.remove_vpc(ssn_conf['vpc_name'])


    try:
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        logging.info('[DERIVING NAMES]')
        ssn_conf = dict()
        ssn_conf['instance'] = 'ssn'
        ssn_conf['pre_defined_vpc'] = False
        ssn_conf['pre_defined_subnet'] = False
        ssn_conf['pre_defined_firewall'] = False
        ssn_conf['billing_enabled'] = True

        ssn_conf['ssn_unique_index'] = args.ssn_unique_index
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'].replace('_', '-').lower()[:20], '-', True)
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        ssn_conf['static_address_name'] = '{}-ssn-static-ip'.format(ssn_conf['service_base_name'])
        ssn_conf['role_name'] = '{}-{}-ssn-role'.format(ssn_conf['service_base_name'], ssn_conf['ssn_unique_index'])
        ssn_conf['region'] = os.environ['gcp_region']
        ssn_conf['zone'] = os.environ['gcp_zone']
        ssn_conf['default_endpoint_name'] = os.environ['default_endpoint_name']
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        ssn_conf['instance_size'] = os.environ['gcp_ssn_instance_size']
        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
            else:
                ssn_conf['pre_defined_vpc'] = True
                ssn_conf['vpc_name'] = os.environ['gcp_vpc_name']
        except KeyError:
            ssn_conf['vpc_name'] = '{}-vpc'.format(ssn_conf['service_base_name'])

        try:
            if os.environ['gcp_subnet_name'] == '':
                raise KeyError
            else:
                ssn_conf['pre_defined_subnet'] = True
                ssn_conf['subnet_name'] = os.environ['gcp_subnet_name']
        except KeyError:
            ssn_conf['subnet_name'] = '{}-subnet'.format(ssn_conf['service_base_name'])
        try:
            if os.environ['gcp_firewall_name'] == '':
                raise KeyError
            else:
                pre_defined_firewall = True
                ssn_conf['firewall_name'] = os.environ['gcp_firewall_name']
        except KeyError:
            ssn_conf['firewall_name'] = '{}-ssn-sg'.format(ssn_conf['service_base_name'])
        ssn_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        ssn_conf['datalab_ssh_user'] = os.environ['conf_os_user']
        ssn_conf['service_account_name'] = '{}-ssn-sa'.format(ssn_conf['service_base_name']).replace('_', '-')
        ssn_conf['image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]

        try:
            if os.environ['aws_account_id'] == '':
                raise KeyError
            if os.environ['aws_billing_bucket'] == '':
                raise KeyError
        except KeyError:
            ssn_conf['billing_enabled'] = False
        if not ssn_conf['billing_enabled']:
            os.environ['aws_account_id'] = 'None'
            os.environ['aws_billing_bucket'] = 'None'
            os.environ['aws_report_path'] = 'None'

        if 'keycloak_client_name' not in os.environ:
            os.environ['keycloak_client_name'] = '{}-ui'.format(ssn_conf['service_base_name'])
        if 'keycloak_client_secret' not in os.environ:
            os.environ['keycloak_client_secret'] = str(uuid.uuid4())
    except Exception as err:
        datalab.fab.datalab.fab.append_result("Failed deriving names.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        ssn_conf['instance_hostname'] = GCPMeta.get_instance_public_ip_by_name(ssn_conf['instance_name'])
        if os.environ['conf_stepcerts_enabled'] == 'true':
            ssn_conf['step_cert_sans'] = ' --san {0} --san {1}'.format(GCPMeta.get_instance_public_ip_by_name(
                ssn_conf['instance_name']), datalab.meta_lib.get_instance_private_ip_address('ssn',
                                                                                             ssn_conf['instance_name']))
        else:
            ssn_conf['step_cert_sans'] = ''
        if os.environ['conf_os_family'] == 'debian':
            ssn_conf['initial_user'] = 'ubuntu'
            ssn_conf['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            ssn_conf['initial_user'] = 'ec2-user'
            ssn_conf['sudo_group'] = 'wheel'

        logging.info('[CREATING DATALAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            ssn_conf['instance_hostname'], ssn_conf['ssh_key_path'], ssn_conf['initial_user'],
            ssn_conf['datalab_ssh_user'], ssn_conf['sudo_group'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.datalab.fab.append_result("Failed creating ssh user 'datalab-user'.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages " \
                 "'boto3 bcrypt==3.1.7 cryptography==36.0.2 backoff argparse fabric=={} awscli pymongo pyyaml " \
                 "google-api-python-client google-cloud-storage pycryptodome' --user {} --region {}". \
            format(ssn_conf['instance_hostname'], ssn_conf['ssh_key_path'], os.environ['pip_packages_fabric'],
                   ssn_conf['datalab_ssh_user'], ssn_conf['region'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.datalab.fab.append_result("Failed installing software: pip, packages.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/",
                             "service_base_name": ssn_conf['service_base_name'],
                             "security_group_id": ssn_conf['firewall_name'], "vpc_id": ssn_conf['vpc_name'],
                             "subnet_id": ssn_conf['subnet_name'], "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --datalab_path {} " \
                 "--tag_resource_id {} --step_cert_sans '{}'". \
            format(ssn_conf['instance_hostname'], ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   ssn_conf['datalab_ssh_user'], os.environ['ssn_datalab_path'], ssn_conf['service_base_name'],
                   ssn_conf['step_cert_sans'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_ssn_node', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.datalab.fab.append_result("Failed configuring ssn.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "project", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "jupyter-gpu", "tag": "latest"},
                             {"name": "jupyterlab", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "superset", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "tensor-rstudio", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"},
                             {"name": "dataengine-service", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --datalab_path {} " \
                 "--cloud_provider {} --region {}". \
            format(ssn_conf['instance_hostname'], ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   os.environ['conf_os_family'], ssn_conf['datalab_ssh_user'], os.environ['ssn_datalab_path'],
                   os.environ['conf_cloud_provider'], ssn_conf['region'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_docker', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.datalab.fab.append_result("Unable to configure docker.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE UI]')

        cloud_params = [
            {
                'key': 'KEYCLOAK_REDIRECT_URI',
                'value': "https://{0}/".format(ssn_conf['instance_hostname'])
            },
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
                'key': 'KEYCLOAK_USER_NAME',
                'value': os.environ['keycloak_user']
            },
            {
                'key': 'KEYCLOAK_PASSWORD',
                'value': os.environ['keycloak_user_password']
            },
            {
                'key': 'CONF_OS',
                'value': os.environ['conf_os_family']
            },
            {
                'key': 'SERVICE_BASE_NAME',
                'value': os.environ['conf_service_base_name']
            },
            {
                'key': 'EDGE_INSTANCE_SIZE',
                'value': ''
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
                'value': ssn_conf['zone']
            },
            {
                'key': 'TAG_RESOURCE_ID',
                'value': ''
            },
            {
                'key': 'SG_IDS',
                'value': ''
            },
            {
                'key': 'SSN_INSTANCE_SIZE',
                'value': ''
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
                'value': ''
            },
            {
                'key': 'AZURE_SSN_STORAGE_ACCOUNT_TAG',
                'value': ''
            },
            {
                'key': 'AZURE_SHARED_STORAGE_ACCOUNT_TAG',
                'value': ''
            },
            {
                'key': 'AZURE_DATALAKE_TAG',
                'value': ''
            },
            {
                'key': 'GCP_PROJECT_ID',
                'value': os.environ['gcp_project_id']
            },
            {
                'key': 'AZURE_CLIENT_ID',
                'value': ''
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
            },
            {
                'key': 'CONF_IMAGE_ENABLED',
                'value': os.environ['conf_image_enabled']
            },
            {
                'key': "AZURE_AUTH_FILE_PATH",
                'value': ""
            }
        ]
        if os.environ['conf_stepcerts_enabled'] == 'true':
            cloud_params.append(
                {
                    'key': 'STEP_CERTS_ENABLED',
                    'value': os.environ['conf_stepcerts_enabled']
                })
            cloud_params.append(
                {
                    'key': 'STEP_ROOT_CA',
                    'value': os.environ['conf_stepcerts_root_ca']
                })
            cloud_params.append(
                {
                    'key': 'STEP_KID_ID',
                    'value': os.environ['conf_stepcerts_kid']
                })
            cloud_params.append(
                {
                    'key': 'STEP_KID_PASSWORD',
                    'value': os.environ['conf_stepcerts_kid_password']
                })
            cloud_params.append(
                {
                    'key': 'STEP_CA_URL',
                    'value': os.environ['conf_stepcerts_ca_url']
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_ENABLED',
                    'value': 'false'
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_DOMAIN_NAME',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_EMAIL',
                    'value': ''
                })
        elif os.environ['conf_letsencrypt_enabled'] == 'true':
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_ENABLED',
                    'value': os.environ['conf_letsencrypt_enabled']
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_DOMAIN_NAME',
                    'value': os.environ['conf_letsencrypt_domain_name']
                })
            if 'conf_letsencrypt_email' in os.environ:
                cloud_params.append(
                    {
                        'key': 'LETS_ENCRYPT_EMAIL',
                        'value': os.environ['conf_letsencrypt_email']
                    })
            else:
                cloud_params.append(
                    {
                        'key': 'LETS_ENCRYPT_EMAIL',
                        'value': ''
                    })
            cloud_params.append(
                {
                    'key': 'STEP_CERTS_ENABLED',
                    'value': 'false'
                })
            cloud_params.append(
                {
                    'key': 'STEP_ROOT_CA',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'STEP_KID_ID',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'STEP_KID_PASSWORD',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'STEP_CA_URL',
                    'value': ''
                })
        else:
            cloud_params.append(
                {
                    'key': 'STEP_CERTS_ENABLED',
                    'value': 'false'
                })
            cloud_params.append(
                {
                    'key': 'STEP_ROOT_CA',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'STEP_KID_ID',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'STEP_KID_PASSWORD',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'STEP_CA_URL',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_ENABLED',
                    'value': 'false'
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_DOMAIN_NAME',
                    'value': ''
                })
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_EMAIL',
                    'value': ''
                })
        params = "--hostname {} --keyfile {} --datalab_path {} --os_user {} --os_family {} --billing_enabled {} " \
                 "--request_id {} --billing_dataset_name {} \
                 --resource {} --service_base_name {} --cloud_provider {} --default_endpoint_name {} " \
                 "--cloud_params '{}' --keycloak_client_id {} --keycloak_client_secret {}" \
                 " --keycloak_auth_server_url {} --keycloak_realm_name {}". \
            format(ssn_conf['instance_hostname'], ssn_conf['ssh_key_path'], os.environ['ssn_datalab_path'],
                   ssn_conf['datalab_ssh_user'],
                   os.environ['conf_os_family'], ssn_conf['billing_enabled'], os.environ['request_id'],
                   os.environ['billing_dataset_name'], os.environ['conf_resource'],
                   ssn_conf['service_base_name'], os.environ['conf_cloud_provider'], ssn_conf['default_endpoint_name'],
                   json.dumps(cloud_params), os.environ['keycloak_client_name'], os.environ['keycloak_client_secret'],
                   os.environ['keycloak_auth_server_url'], os.environ['keycloak_realm_name'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_ui', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.datalab.fab.append_result("Unable to configure UI.", str(err))
        clear_resources()
        sys.exit(1)

    logging.info('[CREATE KEYCLOAK CLIENT]')
    keycloak_params = "--service_base_name {} --keycloak_auth_server_url {} --keycloak_realm_name {} " \
                      "--keycloak_user {} --keycloak_user_password {} --instance_public_ip {} --keycloak_client_secret {} " \
        .format(ssn_conf['service_base_name'], os.environ['keycloak_auth_server_url'],
                os.environ['keycloak_realm_name'], os.environ['keycloak_user'],
                os.environ['keycloak_user_password'], ssn_conf['instance_hostname'],
                os.environ['keycloak_client_secret'])
    try:
        subprocess.run("~/scripts/{}.py {}".format('configure_keycloak', keycloak_params), shell=True, check=True)
    except Exception as err:
        datalab.fab.append_result("Failed to create ssn keycloak client: " + str(err))
        #clear_resources()
        #sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(ssn_conf['service_base_name']))
        logging.info("SSN Name: {}".format(ssn_conf['instance_name']))
        logging.info("SSN Hostname: {}".format(ssn_conf['instance_hostname']))
        logging.info("Role name: {}".format(ssn_conf['role_name']))
        logging.info("Key name: {}".format(os.environ['conf_key_name']))
        logging.info("VPC Name: {}".format(ssn_conf['vpc_name']))
        logging.info("Subnet Name: {}".format(ssn_conf['subnet_name']))
        logging.info("Firewall Names: {}".format(ssn_conf['firewall_name']))
        logging.info("SSN instance size: {}".format(ssn_conf['instance_size']))
        logging.info("SSN AMI name: {}".format(ssn_conf['image_name']))
        logging.info("Region: {}".format(ssn_conf['region']))
        jenkins_url = "http://{}/jenkins".format(ssn_conf['instance_hostname'])
        jenkins_url_https = "https://{}/jenkins".format(ssn_conf['instance_hostname'])
        logging.info("Jenkins URL: {}".format(jenkins_url))
        logging.info("Jenkins URL HTTPS: {}".format(jenkins_url_https))
        logging.info("DataLab UI HTTP URL: http://{}".format(ssn_conf['instance_hostname']))
        logging.info("DataLab UI HTTPS URL: https://{}".format(ssn_conf['instance_hostname']))
        try:
            with open('jenkins_creds.txt') as f:
                logging.info(f.read())
        except:
            logging.info("Jenkins is either configured already or have issues in configuration routine.")

        with open("/root/result.json", 'w') as f:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "instance_name": ssn_conf['instance_name'],
                   "instance_hostname": ssn_conf['instance_hostname'],
                   "role_name": ssn_conf['role_name'],
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": ssn_conf['vpc_name'],
                   "subnet_id": ssn_conf['subnet_name'],
                   "security_id": ssn_conf['firewall_name'],
                   "instance_shape": ssn_conf['instance_size'],
                   "region": ssn_conf['region'],
                   "action": "Create SSN instance"}
            f.write(json.dumps(res))

        logging.info('Upload response file')
        local_log_filepath = "/logs/{}/{}_{}.log".format(os.environ['conf_resource'], os.environ['conf_resource'],
                                                         os.environ['request_id'])
        params = "--instance_name {} --local_log_filepath {} --os_user {} --instance_hostname {}". \
            format(ssn_conf['instance_name'], local_log_filepath, ssn_conf['datalab_ssh_user'],
                   ssn_conf['instance_hostname'])
        subprocess.run("~/scripts/{}.py {}".format('upload_response_file', params), shell=True, check=True)
    except Exception as err:
        datalab.fab.append_result("Error with writing results.", str(err))
        clear_resources()
        sys.exit(1)
