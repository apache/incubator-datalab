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

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import datalab.ssn_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import subprocess
import uuid
from fabric import *

def cleanup_aws_resources(tag_name, service_base_name):
    try:
        params = "--tag_name {} --service_base_name {}".format(tag_name, service_base_name)
        subprocess.run("~/scripts/{}.py {}".format('ssn_terminate_aws_resources', params), shell=True, check=True)
    except:
        traceback.print_exc()
        raise Exception

if __name__ == "__main__":
    # deriving variables for ssn node deployment
    try:
        logging.info('[DERIVING NAMES]')
        ssn_conf = dict()
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
                    os.environ['conf_service_base_name'][:20], '-', True)
        ssn_conf['role_name'] = '{}-ssn-role'.format(ssn_conf['service_base_name'])
        ssn_conf['role_profile_name'] = '{}-ssn-profile'.format(ssn_conf['service_base_name'])
        ssn_conf['policy_name'] = '{}-ssn-policy'.format(ssn_conf['service_base_name'])
        ssn_conf['tag_name'] = '{}-tag'.format(ssn_conf['service_base_name'])
        ssn_conf['tag2_name'] = '{}-secondary-tag'.format(ssn_conf['service_base_name'])
        ssn_conf['user_tag'] = "{0}:{0}-ssn-role".format(ssn_conf['service_base_name'])
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        ssn_conf['region'] = os.environ['aws_region']
        ssn_conf['ssn_image_name'] = os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])]
        ssn_conf['subnet_name'] = '{}-subnet'.format(ssn_conf['service_base_name'])
        ssn_conf['subnet_tag'] = {"Key": ssn_conf['tag_name'], "Value": ssn_conf['subnet_name']}
        ssn_conf['sg_name'] = '{}-ssn-sg'.format(ssn_conf['service_base_name'])
        ssn_conf['network_type'] = os.environ['conf_network_type']
        ssn_conf['datalab_ssh_user'] = os.environ['conf_os_user']
        ssn_conf['ssn_datalab_path'] = os.environ['ssn_datalab_path']
        ssn_conf['conf_tag_resource_id'] = os.environ['conf_tag_resource_id']
        ssn_conf['instance_hostname'] = (lambda x: datalab.meta_lib.get_instance_ip_address(
            ssn_conf['tag_name'], ssn_conf['instance_name']).get(
            'Private') if x == 'private' else datalab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name']))(ssn_conf['network_type'])
        ssn_conf['initial_user'] = (lambda x: 'ubuntu' if x == 'debian' else 'ec2-user')(os.environ['conf_os_family'])
        ssn_conf['sudo_group'] = (lambda x: 'sudo' if x == 'debian' else 'wheel')(os.environ['conf_os_family'])
        ssn_conf['step_cert_sans'] = (lambda x: (lambda x: ' --san {0} --san {1}'.format(
            datalab.meta_lib.get_instance_hostname(ssn_conf['tag_name'], ssn_conf['instance_name']),
            datalab.meta_lib.get_instance_ip_address(ssn_conf['tag_name'], ssn_conf['instance_name']).get(
                'Public')) if x == 'public' else ' --san {0}'.format(datalab.meta_lib.get_instance_ip_address(
            ssn_conf['tag_name'], ssn_conf['instance_name']).get('Private')))(
            ssn_conf['network_type']) if x == 'true' else '')(os.environ['conf_stepcerts_enabled'])

        if 'aws_vpc_id' in os.environ and os.environ['aws_vpc_id'] != '':
            ssn_conf['aws_vpc_id'] = os.environ['aws_vpc_id']
        else:
            ssn_conf['aws_vpc_id'] = datalab.meta_lib.get_vpc_by_tag(ssn_conf['tag_name'],
                                                                     ssn_conf['service_base_name'])
        if os.environ['conf_duo_vpc_enable'] == 'true' and 'aws_vpc2_id' in os.environ\
                and os.environ['aws_vpc2_id'] != '':
            ssn_conf['aws_vpc2_id'] = os.environ['aws_vpc2_id']
        else:
            ssn_conf['aws_vpc2_id'] = datalab.meta_lib.get_vpc_by_tag(ssn_conf['tag2_name'],
                                                                      ssn_conf['service_base_name'])
        if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_peering_id']:
            ssn_conf['aws_peering_id'] = datalab.meta_lib.get_peering_by_tag(ssn_conf['tag_name'],
                                                                           ssn_conf['service_base_name'])
        elif os.environ['conf_duo_vpc_enable'] == 'true' and aws_peering_id in os.environ \
                and os.environ['aws_peering_id'] != '':
            ssn_conf['aws_peering_id'] = os.environ['aws_peering_id']
        else:
            ssn_conf['aws_peering_id'] = None
        if 'aws_subnet_id' in os.environ and os.environ['aws_subnet_id'] != '':
            ssn_conf['aws_subnet_id'] = os.environ['aws_subnet_id']
        else:
            ssn_conf['aws_subnet_id'] = datalab.meta_lib.get_subnet_by_tag(ssn_conf['subnet_tag'], True)
        if 'aws_security_groups_ids' in os.environ and os.environ['aws_security_groups_ids'] != '':
            ssn_conf['aws_security_groups_ids'] = os.environ['aws_security_groups_ids']
        else:
            ssn_conf['aws_security_groups_ids'] = datalab.meta_lib.get_security_group_by_name(ssn_conf['sg_name'])
        if 'aws_billing_bucket' in os.environ and os.environ['aws_billing_bucket'] != '':
            ssn_conf['billing_enabled'] = True
            ssn_conf['aws_billing_bucket'] = os.environ['aws_billing_bucket']
        else:
            ssn_conf['billing_enabled'] = False
            ssn_conf['aws_billing_bucket'] = 'None'
        if 'aws_report_path' in os.environ and os.environ['aws_report_path'] != '':
            ssn_conf['aws_report_path'] = os.environ['aws_report_path']
        else:
            ssn_conf['aws_report_path'] = ''

        if 'keycloak_client_name' not in os.environ:
            os.environ['keycloak_client_name'] = '{}-ui'.format(ssn_conf['service_base_name'])
        if 'keycloak_client_secret' not in os.environ:
            os.environ['keycloak_client_secret'] = str(uuid.uuid4())

    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        traceback.print_exc()
        sys.exit(1)

    #creating datalab ssh user
    try:
        logging.info('[CREATING DATALAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            ssn_conf['instance_hostname'], os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem",
            ssn_conf['initial_user'], ssn_conf['datalab_ssh_user'], ssn_conf['sudo_group'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed creating ssh user", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        sys.exit(1)

    #installing prerequisites to ssn instance
    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        pip_packages = 'boto3=={} cryptography==36.0.2 bcrypt=={} backoff=={} argparse=={} fabric=={} awscli=={} pymongo=={} pyyaml=={}' \
                       ' jinja2=={}'.format(os.environ['pip_packages_boto3'], os.environ['pip_packages_bcrypt'],
                                            os.environ['pip_packages_backoff'], os.environ['pip_packages_argparse'],
                                            os.environ['pip_packages_fabric'], os.environ['pip_packages_awscli'],
                                            os.environ['pip_packages_pymongo'], os.environ['pip_packages_pyyaml'],
                                            os.environ['pip_packages_jinja2'])
        params = "--hostname {} --keyfile {} --pip_packages '{}' --user {} --region {}".format(
            ssn_conf['instance_hostname'], "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
            pip_packages, ssn_conf['datalab_ssh_user'], ssn_conf['region'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed installing software: pip, packages.", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        sys.exit(1)

    #configuring ssn instance
    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/",
                             "service_base_name": ssn_conf['service_base_name'],
                             "security_group_id": ssn_conf['aws_security_groups_ids'],
                             "vpc_id": ssn_conf['aws_vpc_id'], "subnet_id": ssn_conf['aws_subnet_id'],
                             "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --datalab_path {}" \
                 " --tag_resource_id {} --step_cert_sans '{}' ".format(
            ssn_conf['instance_hostname'], "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
            json.dumps(additional_config), ssn_conf['datalab_ssh_user'], ssn_conf['ssn_datalab_path'],
            ssn_conf['conf_tag_resource_id'], ssn_conf['step_cert_sans'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_ssn_node', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to configure SSN node.", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        sys.exit(1)

    #configuring docker at ssn instance
    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "project", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "tensor-rstudio", "tag": "latest"},
                             {"name": "tensor-jupyterlab", "tag": "latest"},
                             {"name": "jupyterlab", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine-service", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --datalab_path {} " \
                 "--cloud_provider {} --region {}".format(ssn_conf['instance_hostname'],
                                                          "{}{}.pem".format(os.environ['conf_key_dir'],
                                                                            os.environ['conf_key_name']),
                                                          json.dumps(additional_config), os.environ['conf_os_family'],
                                                          ssn_conf['datalab_ssh_user'], os.environ['ssn_datalab_path'],
                                                          os.environ['conf_cloud_provider'], os.environ['aws_region'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_docker', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to configure docker.", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        sys.exit(1)


    #configuring UI
    try:
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        cloud_params = [
            {
                'key': 'KEYCLOAK_REDIRECT_URI',
                'value': "https://{0}/".format(datalab.meta_lib.get_instance_hostname(ssn_conf['tag_name'],
                                                                                      ssn_conf['instance_name']))
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
                'value': os.environ['aws_edge_instance_size']
            },
            {
                'key': 'SUBNET_ID',
                'value': ssn_conf['aws_subnet_id']
            },
            {
                'key': 'REGION',
                'value': ssn_conf['region']
            },
            {
                'key': 'ZONE',
                'value': os.environ['aws_zone']
            },
            {
                'key': 'TAG_RESOURCE_ID',
                'value': ssn_conf['conf_tag_resource_id']
            },
            {
                'key': 'SG_IDS',
                'value': ssn_conf['aws_security_groups_ids']
            },
            {
                'key': 'SSN_INSTANCE_SIZE',
                'value': os.environ['aws_ssn_instance_size']
            },
            {
                'key': 'VPC_ID',
                'value': ssn_conf['aws_vpc_id']
            },
            {
                'key': 'CONF_KEY_DIR',
                'value': os.environ['conf_key_dir']
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
                'value': ''
            },
            {
                'key': 'AZURE_CLIENT_ID',
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
        if os.environ['conf_duo_vpc_enable'] == 'true':
            cloud_params.append(
                {
                    'key': 'SUBNET2_ID',
                    'value': ssn_conf['aws_subnet_id']
                })
            cloud_params.append(
                {
                    'key': 'VPC2_ID',
                    'value': ssn_conf['aws_vpc2_id']
                })
            cloud_params.append(
                {
                    'key': 'PEERING_ID',
                    'value': ssn_conf['aws_peering_id']
                })
        else:
            cloud_params.append(
                {
                    'key': 'SUBNET2_ID',
                    'value': ssn_conf['aws_subnet_id']
                })
            cloud_params.append(
                {
                    'key': 'VPC2_ID',
                    'value': ssn_conf['aws_vpc_id']
                })
            cloud_params.append(
                {
                    'key': 'PEERING_ID',
                    'value': ssn_conf['aws_peering_id']
                })
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
            cloud_params.append(
                {
                    'key': 'LETS_ENCRYPT_EMAIL',
                    'value': os.environ['conf_letsencrypt_email']
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
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')
        params = "--hostname {} " \
                 "--keyfile {} " \
                 "--datalab_path {} " \
                 "--os_user {} " \
                 "--os_family {} " \
                 "--request_id {} " \
                 "--resource {} " \
                 "--service_base_name {} " \
                 "--tag_resource_id {} " \
                 "--billing_tag {} " \
                 "--cloud_provider {} " \
                 "--account_id {} " \
                 "--billing_bucket {} " \
                 "--aws_job_enabled {} " \
                 "--report_path '{}' " \
                 "--billing_enabled {} " \
                 "--cloud_params '{}' " \
                 "--datalab_id '{}' " \
                 "--usage_date {} " \
                 "--product {} " \
                 "--usage_type {} " \
                 "--usage {} " \
                 "--cost {} " \
                 "--resource_id {} " \
                 "--default_endpoint_name {} " \
                 "--tags {} " \
                 "--keycloak_client_id {} " \
                 "--keycloak_client_secret {} " \
                 "--keycloak_auth_server_url {}". \
            format(ssn_conf['instance_hostname'],
                   "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
                   os.environ['ssn_datalab_path'],
                   ssn_conf['datalab_ssh_user'],
                   os.environ['conf_os_family'],
                   os.environ['request_id'],
                   os.environ['conf_resource'],
                   ssn_conf['service_base_name'],
                   os.environ['conf_tag_resource_id'],
                   os.environ['conf_billing_tag'],
                   os.environ['conf_cloud_provider'],
                   os.environ['aws_account_id'],
                   ssn_conf['aws_billing_bucket'],
                   os.environ['aws_job_enabled'],
                   ssn_conf['aws_report_path'],
                   ssn_conf['billing_enabled'],
                   json.dumps(cloud_params),
                   os.environ['datalab_id'],
                   os.environ['usage_date'],
                   os.environ['product'],
                   os.environ['usage_type'],
                   os.environ['usage'],
                   os.environ['cost'],
                   os.environ['resource_id'],
                   os.environ['default_endpoint_name'],
                   os.environ['tags'],
                   os.environ['keycloak_client_name'],
                   os.environ['keycloak_client_secret'],
                   os.environ['keycloak_auth_server_url'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_ui', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to configure Datalab UI.", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        sys.exit(1)

    logging.info('[CREATE KEYCLOAK CLIENT]')
    keycloak_params = "--service_base_name {} --keycloak_auth_server_url {} --keycloak_realm_name {} " \
                      "--keycloak_user {} --keycloak_user_password {} --instance_public_ip {} --keycloak_client_secret {} " \
        .format(ssn_conf['service_base_name'], os.environ['keycloak_auth_server_url'],
                os.environ['keycloak_realm_name'], os.environ['keycloak_user'],
                os.environ['keycloak_user_password'], datalab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name']), os.environ['keycloak_client_secret'])
    try:
        subprocess.run("~/scripts/{}.py {}".format('configure_keycloak', keycloak_params), shell=True, check=True)
    except Exception as err:
        datalab.fab.append_result("Failed to create ssn keycloak client: " + str(err))
        #clear_resources()
        #sys.exit(1)

    #ssn deployment summary
    try:
        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(ssn_conf['service_base_name']))
        logging.info("SSN Name: {}".format(ssn_conf['instance_name']))
        logging.info("SSN Hostname: {}".format(ssn_conf['instance_hostname']))
        logging.info("Role name: {}".format(ssn_conf['role_name']))
        logging.info("Role profile name: {}".format(ssn_conf['role_profile_name']))
        logging.info("Policy name: {}".format(ssn_conf['policy_name']))
        logging.info("Key name: {}".format(os.environ['conf_key_name']))
        logging.info("VPC ID: {}".format(ssn_conf['aws_vpc_id']))
        logging.info("Subnet ID: {}".format(ssn_conf['aws_subnet_id']))
        logging.info("Security IDs: {}".format(ssn_conf['aws_security_groups_ids']))
        logging.info("SSN instance shape: {}".format(os.environ['aws_ssn_instance_size']))
        logging.info("SSN AMI name: {}".format(ssn_conf['ssn_image_name']))
        logging.info("Region: {}".format(ssn_conf['region']))
        logging.info("DataLab UI HTTP URL: http://{}".format(datalab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name'])))
        logging.info("DataLab UI HTTPS URL: https://{}".format(datalab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name'])))

        with open("/root/result.json", 'w') as f:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "instance_name": ssn_conf['instance_name'],
                   "instance_hostname": datalab.meta_lib.get_instance_hostname(ssn_conf['tag_name'],
                                                                               ssn_conf['instance_name']),
                   "role_name": ssn_conf['role_name'],
                   "role_profile_name": ssn_conf['role_profile_name'],
                   "policy_name": ssn_conf['policy_name'],
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": ssn_conf['aws_vpc_id'],
                   "subnet_id": ssn_conf['aws_subnet_id'],
                   "security_id": ssn_conf['aws_security_groups_ids'],
                   "instance_shape": os.environ['aws_ssn_instance_size'],
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

        logging.info('[FINALIZE]')
        params = ""
        if os.environ['conf_lifecycle_stage'] == 'prod':
            params += "--key_id {}".format(os.environ['aws_access_key'])
            subprocess.run("~/scripts/{}.py {}".format('ssn_finalize', params), shell=True, check=True)
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Error with writing results.", str(err))
        cleanup_aws_resources(ssn_conf['tag_name'], ssn_conf['service_base_name'])
        sys.exit(1)