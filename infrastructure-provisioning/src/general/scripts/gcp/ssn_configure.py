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
import sys, os
from fabric.api import *
from dlab.ssn_lib import *
import traceback
import json

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'

    try:
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')
        pre_defined_vpc = False
        pre_defined_subnet = False
        pre_defined_firewall = False
        billing_enabled = False

        ssn_conf = dict()
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower().replace('_', '-')[:12], '-', True)
        ssn_conf['region'] = os.environ['gcp_region']
        ssn_conf['zone'] = os.environ['gcp_zone']
        ssn_conf['ssn_bucket_name'] = '{}-ssn-bucket'.format(ssn_conf['service_base_name'])
        ssn_conf['shared_bucket_name'] = '{}-shared-bucket'.format(ssn_conf['service_base_name'])
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        ssn_conf['instance_size'] = os.environ['gcp_ssn_instance_size']
        ssn_conf['vpc_name'] = '{}-ssn-vpc'.format(ssn_conf['service_base_name'])
        ssn_conf['subnet_name'] = '{}-ssn-subnet'.format(ssn_conf['service_base_name'])
        ssn_conf['subnet_cidr'] = '10.10.1.0/24'
        ssn_conf['firewall_name'] = '{}-ssn-firewall'.format(ssn_conf['service_base_name'])
        ssn_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        ssn_conf['dlab_ssh_user'] = os.environ['conf_os_user']
        ssn_conf['service_account_name'] = '{}-ssn-sa'.format(ssn_conf['service_base_name']).replace('_', '-')
        ssn_conf['image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
        ssn_conf['role_name'] = ssn_conf['service_base_name'] + '-ssn-role'

        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_vpc = True
            os.environ['gcp_vpc_name'] = ssn_conf['vpc_name']
        try:
            if os.environ['gcp_subnet_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_subnet = True
            os.environ['gcp_subnet_name'] = ssn_conf['subnet_name']
        try:
            if os.environ['gcp_firewall_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_firewall = True
            os.environ['gcp_firewall_name'] = ssn_conf['firewall_name']

        try:
            if os.environ['aws_account_id'] == '':
                raise KeyError
            if os.environ['aws_billing_bucket'] == '':
                raise KeyError
        except KeyError:
            billing_enabled = False
        if not billing_enabled:
            os.environ['aws_account_id'] = 'None'
            os.environ['aws_billing_bucket'] = 'None'
            os.environ['aws_report_path'] = 'None'
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed deriving names.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        instance_hostname = GCPMeta().get_instance_public_ip_by_name(ssn_conf['instance_name'])
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (instance_hostname, ssn_conf['ssh_key_path'], initial_user, ssn_conf['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed creating ssh user 'dlab-user'.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        print('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages " \
                 "'boto3 backoff argparse fabric==1.14.0 awscli pymongo pyyaml " \
                 "google-api-python-client google-cloud-storage pycrypto' --user {} --region {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'],
                   ssn_conf['dlab_ssh_user'], ssn_conf['region'])

        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing software: pip, packages.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        print('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/",
                             "service_base_name": ssn_conf['service_base_name'],
                             "security_group_id": ssn_conf['firewall_name'], "vpc_id": ssn_conf['vpc_name'],
                             "subnet_id": ssn_conf['subnet_name'], "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --dlab_path {} --tag_resource_id {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'], ssn_conf['service_base_name'])

        try:
            local("~/scripts/{}.py {}".format('configure_ssn_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed configuring ssn.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        print('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "project", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "tensor-rstudio", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"},
                             {"name": "dataengine-service", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --dlab_path {} --cloud_provider {} --region {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   os.environ['conf_os_family'], ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'],
                   os.environ['conf_cloud_provider'], ssn_conf['region'])

        try:
            local("~/scripts/{}.py {}".format('configure_docker', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to configure docker.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')

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
                'value': os.environ['conf_service_base_name']
            },
            {
                'key': 'EDGE_INSTANCE_SIZE',
                'value': ''
            },
            {
                'key': 'SUBNET_ID',
                'value': ''
            },
            {
                'key': 'REGION',
                'value': ''
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
                'value': ''
            },
            {
                'key': 'SSN_INSTANCE_SIZE',
                'value': ''
            },
            {
                'key': 'VPC_ID',
                'value': ''
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
            }
        ]
        params = "--hostname {} --keyfile {} --dlab_path {} --os_user {} --os_family {} --request_id {} \
                 --resource {} --service_base_name {} --cloud_provider {} --cloud_params '{}'". \
            format(instance_hostname, ssn_conf['ssh_key_path'], os.environ['ssn_dlab_path'], ssn_conf['dlab_ssh_user'],
                   os.environ['conf_os_family'], os.environ['request_id'], os.environ['conf_resource'],
                   ssn_conf['service_base_name'], os.environ['conf_cloud_provider'],  json.dumps(cloud_params))
        try:
            local("~/scripts/{}.py {}".format('configure_ui', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to configure UI.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        print('[SUMMARY]')
        print("Service base name: {}".format(ssn_conf['service_base_name']))
        print("SSN Name: {}".format(ssn_conf['instance_name']))
        print("SSN Hostname: {}".format(instance_hostname))
        print("Role name: {}".format(ssn_conf['role_name']))
        print("Key name: {}".format(os.environ['conf_key_name']))
        print("VPC Name: {}".format(ssn_conf['vpc_name']))
        print("Subnet Name: {}".format(ssn_conf['subnet_name']))
        print("Firewall Names: {}".format(ssn_conf['firewall_name']))
        print("SSN instance size: {}".format(ssn_conf['instance_size']))
        print("SSN AMI name: {}".format(ssn_conf['image_name']))
        print("SSN bucket name: {}".format(ssn_conf['ssn_bucket_name']))
        print("Region: {}".format(ssn_conf['region']))
        jenkins_url = "http://{}/jenkins".format(instance_hostname)
        jenkins_url_https = "https://{}/jenkins".format(instance_hostname)
        print("Jenkins URL: {}".format(jenkins_url))
        print("Jenkins URL HTTPS: {}".format(jenkins_url_https))
        print("DLab UI HTTP URL: http://{}".format(instance_hostname))
        print("DLab UI HTTPS URL: https://{}".format(instance_hostname))
        try:
            with open('jenkins_creds.txt') as f:
                print(f.read())
        except:
            print("Jenkins is either configured already or have issues in configuration routine.")

        with open("/root/result.json", 'w') as f:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "instance_name": ssn_conf['instance_name'],
                   "instance_hostname": instance_hostname,
                   "role_name": ssn_conf['role_name'],
                   #"role_profile_name": role_profile_name,
                   #"policy_name": policy_name,
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": ssn_conf['vpc_name'],
                   "subnet_id": ssn_conf['subnet_name'],
                   "security_id": ssn_conf['firewall_name'],
                   "instance_shape": ssn_conf['instance_size'],
                   "bucket_name": ssn_conf['ssn_bucket_name'],
                   "shared_bucket_name": ssn_conf['shared_bucket_name'],
                   "region": ssn_conf['region'],
                   "action": "Create SSN instance"}
            f.write(json.dumps(res))

        print('Upload response file')
        params = "--instance_name {} --local_log_filepath {} --os_user {} --instance_hostname {}".\
            format(ssn_conf['instance_name'], local_log_filepath, ssn_conf['dlab_ssh_user'], instance_hostname)
        local("~/scripts/{}.py {}".format('upload_response_file', params))
    except Exception as err:
        print('Error: {0}'.format(err))
        GCPActions().remove_instance(ssn_conf['instance_name'], ssn_conf['zone'])
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions().remove_bucket(ssn_conf['shared_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)
