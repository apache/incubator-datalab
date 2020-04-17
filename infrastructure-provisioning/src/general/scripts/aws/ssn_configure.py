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
import sys
import os
from fabric.api import *
import dlab.ssn_lib
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import traceback
import json

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    ssn_conf = dict()
    ssn_conf['instance'] = 'ssn'

    def clear_resources():
        if ssn_conf['domain_created']:
            dlab.actions_lib.remove_route_53_record(os.environ['ssn_hosted_zone_id'],
                                                    os.environ['ssn_hosted_zone_name'],
                                                    os.environ['ssn_subdomain'])
        dlab.actions_lib.remove_ec2(ssn_conf['tag_name'], ssn_conf['instance_name'])
        dlab.actions_lib.remove_all_iam_resources(ssn_conf['instance'])
        dlab.actions_lib.remove_s3(ssn_conf['instance'])
        if ssn_conf['pre_defined_sg']:
            dlab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
        if ssn_conf['pre_defined_subnet']:
            dlab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                      ssn_conf['service_base_name'])
            dlab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
        if ssn_conf['pre_defined_vpc']:
            dlab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc_id'])
            dlab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
            dlab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
        if ssn_conf['pre_defined_vpc2']:
            dlab.actions_lib.remove_peering('*')
            try:
                dlab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            dlab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
            dlab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])

    try:
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = dlab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and \
                'ssn_subdomain' in os.environ:
            ssn_conf['domain_created'] = True
        else:
            ssn_conf['domain_created'] = False
        ssn_conf['pre_defined_vpc'] = False
        ssn_conf['pre_defined_subnet'] = False
        ssn_conf['pre_defined_sg'] = False
        ssn_conf['billing_enabled'] = True
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
        ssn_conf['sg_name'] = '{}-ssn-sg'.format(ssn_conf['service_base_name'])
        ssn_conf['network_type'] = os.environ['conf_network_type']
        ssn_conf['dlab_ssh_user'] = os.environ['conf_os_user']

        try:
            if os.environ['aws_vpc_id'] == '':
                raise KeyError
        except KeyError:
            ssn_conf['tag'] = {"Key": ssn_conf['tag_name'], "Value": "{}-subnet".format(ssn_conf['service_base_name'])}
            os.environ['aws_vpc_id'] = dlab.meta_lib.get_vpc_by_tag(ssn_conf['tag_name'], ssn_conf['service_base_name'])
            ssn_conf['pre_defined_vpc'] = True
        try:
            if os.environ['aws_subnet_id'] == '':
                raise KeyError
        except KeyError:
            ssn_conf['tag'] = {"Key": ssn_conf['tag_name'], "Value": "{}-subnet".format(ssn_conf['service_base_name'])}
            os.environ['aws_subnet_id'] = dlab.meta_lib.get_subnet_by_tag(ssn_conf['tag'], True)
            ssn_conf['pre_defined_subnet'] = True
        try:
            if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_vpc2_id']:
                raise KeyError
        except KeyError:
            ssn_conf['tag'] = {"Key": ssn_conf['tag2_name'], "Value": "{}-subnet".format(ssn_conf['service_base_name'])}
            os.environ['aws_vpc2_id'] = dlab.meta_lib.get_vpc_by_tag(ssn_conf['tag2_name'],
                                                                     ssn_conf['service_base_name'])
            ssn_conf['pre_defined_vpc2'] = True
        try:
            if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_peering_id']:
                raise KeyError
        except KeyError:
            os.environ['aws_peering_id'] = dlab.meta_lib.get_peering_by_tag(ssn_conf['tag_name'],
                                                                            ssn_conf['service_base_name'])
            ssn_conf['pre_defined_peering'] = True
        try:
            if os.environ['aws_security_groups_ids'] == '':
                raise KeyError
        except KeyError:
            os.environ['aws_security_groups_ids'] = dlab.meta_lib.get_security_group_by_name(ssn_conf['sg_name'])
            ssn_conf['pre_defined_sg'] = True
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
        try:
            if not os.environ['aws_report_path']:
                raise KeyError
        except KeyError:
            os.environ['aws_report_path'] = ''
    except Exception as err:
        dlab.fab.append_result("Failed to generate variables dictionary.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            ssn_conf['initial_user'] = 'ubuntu'
            ssn_conf['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            ssn_conf['initial_user'] = 'ec2-user'
            ssn_conf['sudo_group'] = 'wheel'

        if ssn_conf['network_type'] == 'private':
            ssn_conf['instance_hostname'] = dlab.meta_lib.get_instance_ip_address(
                ssn_conf['tag_name'], ssn_conf['instance_name']).get('Private')
        else:
            ssn_conf['instance_hostname'] = dlab.meta_lib.get_instance_hostname(
                ssn_conf['tag_name'], ssn_conf['instance_name'])

        if os.environ['conf_stepcerts_enabled'] == 'true':
            ssn_conf['step_cert_sans'] = ' --san {0} '.format(dlab.meta_lib.get_instance_ip_address(
                ssn_conf['tag_name'], ssn_conf['instance_name']).get('Private'))
            if ssn_conf['network_type'] == 'public':
                ssn_conf['step_cert_sans'] += ' --san {0} --san {1}'.format(
                    dlab.meta_lib.get_instance_hostname(ssn_conf['tag_name'], ssn_conf['instance_name']),
                    dlab.meta_lib.get_instance_ip_address(ssn_conf['tag_name'],
                                                          ssn_conf['instance_name']).get('Public'))
        else:
            ssn_conf['step_cert_sans'] = ''

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            ssn_conf['instance_hostname'], os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem",
            ssn_conf['initial_user'], ssn_conf['dlab_ssh_user'], ssn_conf['sudo_group'])

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
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        print('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages 'boto3 backoff argparse fabric==1.14.0 awscli pymongo " \
                 "pyyaml jinja2' --user {} --region {}". \
            format(ssn_conf['instance_hostname'], os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem",
                   ssn_conf['dlab_ssh_user'], os.environ['aws_region'])

        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing software: pip, packages.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        print('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/", "service_base_name":
                             ssn_conf['service_base_name'],
                             "security_group_id": os.environ['aws_security_groups_ids'],
                             "vpc_id": os.environ['aws_vpc_id'], "subnet_id": os.environ['aws_subnet_id'],
                             "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --dlab_path {} " \
                 "--tag_resource_id {} --step_cert_sans '{}' ".format(
                  ssn_conf['instance_hostname'],
                  "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
                  json.dumps(additional_config), ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'],
                  os.environ['conf_tag_resource_id'], ssn_conf['step_cert_sans'])

        try:
            local("~/scripts/{}.py {}".format('configure_ssn_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed configuring ssn.", str(err))
        clear_resources()
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
                             {"name": "tensor-rstudio", "tag": "latest"},
                             {"name": "jupyterlab", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine-service", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --dlab_path {} " \
                 "--cloud_provider {} --region {}".format(ssn_conf['instance_hostname'],
                                                          "{}{}.pem".format(os.environ['conf_key_dir'],
                                                                            os.environ['conf_key_name']),
                                                          json.dumps(additional_config), os.environ['conf_os_family'],
                                                          ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'],
                                                          os.environ['conf_cloud_provider'], os.environ['aws_region'])

        try:
            local("~/scripts/{}.py {}".format('configure_docker', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Unable to configure docker.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        cloud_params = [
            {
                'key': 'KEYCLOAK_REDIRECT_URI',
                'value': "https://{0}/".format(dlab.meta_lib.get_instance_hostname(ssn_conf['tag_name'],
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
                'value': os.environ['aws_subnet_id']
            },
            {
                'key': 'REGION',
                'value': os.environ['aws_region']
            },
            {
                'key': 'ZONE',
                'value': os.environ['aws_zone']
            },
            {
                'key': 'TAG_RESOURCE_ID',
                'value': os.environ['conf_tag_resource_id']
            },
            {
                'key': 'SG_IDS',
                'value': os.environ['aws_security_groups_ids']
            },
            {
                'key': 'SSN_INSTANCE_SIZE',
                'value': os.environ['aws_ssn_instance_size']
            },
            {
                'key': 'VPC_ID',
                'value': os.environ['aws_vpc_id']
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
                    'value': os.environ['aws_subnet_id']
                })
            cloud_params.append(
                {
                    'key': 'VPC2_ID',
                    'value': os.environ['aws_vpc2_id']
                })
            cloud_params.append(
                {
                    'key': 'PEERING_ID',
                    'value': os.environ['aws_peering_id']
                })
        else:
            cloud_params.append(
                {
                    'key': 'SUBNET2_ID',
                    'value': os.environ['aws_subnet_id']
                })
            cloud_params.append(
                {
                    'key': 'VPC2_ID',
                    'value': os.environ['aws_vpc_id']
                })
            cloud_params.append(
                {
                    'key': 'PEERING_ID',
                    'value': ''
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
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')
        params = "--hostname {} " \
                 "--keyfile {} " \
                 "--dlab_path {} " \
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
                 "--dlab_id '{}' " \
                 "--usage_date {} " \
                 "--product {} " \
                 "--usage_type {} " \
                 "--usage {} " \
                 "--cost {} " \
                 "--resource_id {} " \
                 "--default_endpoint_name {} " \
                 "--tags {}" \
                 "--keycloak_client_id {}" \
                 "--keycloak_client_secret {}" \
                 "--keycloak_auth_server_url {}". \
            format(ssn_conf['instance_hostname'],
                   "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
                   os.environ['ssn_dlab_path'],
                   ssn_conf['dlab_ssh_user'],
                   os.environ['conf_os_family'],
                   os.environ['request_id'],
                   os.environ['conf_resource'],
                   ssn_conf['service_base_name'],
                   os.environ['conf_tag_resource_id'],
                   os.environ['conf_billing_tag'],
                   os.environ['conf_cloud_provider'],
                   os.environ['aws_account_id'],
                   os.environ['aws_billing_bucket'],
                   os.environ['aws_job_enabled'],
                   os.environ['aws_report_path'],
                   ssn_conf['billing_enabled'],
                   json.dumps(cloud_params),
                   os.environ['dlab_id'],
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
            local("~/scripts/{}.py {}".format('configure_ui', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Unable to configure UI.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        print('[SUMMARY]')
        print("Service base name: {}".format(ssn_conf['service_base_name']))
        print("SSN Name: {}".format(ssn_conf['instance_name']))
        print("SSN Hostname: {}".format(ssn_conf['instance_hostname']))
        print("Role name: {}".format(ssn_conf['role_name']))
        print("Role profile name: {}".format(ssn_conf['role_profile_name']))
        print("Policy name: {}".format(ssn_conf['policy_name']))
        print("Key name: {}".format(os.environ['conf_key_name']))
        print("VPC ID: {}".format(os.environ['aws_vpc_id']))
        print("Subnet ID: {}".format(os.environ['aws_subnet_id']))
        print("Security IDs: {}".format(os.environ['aws_security_groups_ids']))
        print("SSN instance shape: {}".format(os.environ['aws_ssn_instance_size']))
        print("SSN AMI name: {}".format(ssn_conf['ssn_image_name']))
        print("Region: {}".format(ssn_conf['region']))
        ssn_conf['jenkins_url'] = "http://{}/jenkins".format(dlab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name']))
        ssn_conf['jenkins_url_https'] = "https://{}/jenkins".format(dlab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name']))
        print("Jenkins URL: {}".format(ssn_conf['jenkins_url']))
        print("Jenkins URL HTTPS: {}".format(ssn_conf['jenkins_url_https']))
        print("DLab UI HTTP URL: http://{}".format(dlab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name'])))
        print("DLab UI HTTPS URL: https://{}".format(dlab.meta_lib.get_instance_hostname(
            ssn_conf['tag_name'], ssn_conf['instance_name'])))
        try:
            with open('jenkins_creds.txt') as f:
                print(f.read())
        except:
            print("Jenkins is either configured already or have issues in configuration routine.")

        with open("/root/result.json", 'w') as f:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "instance_name": ssn_conf['instance_name'],
                   "instance_hostname": dlab.meta_lib.get_instance_hostname(ssn_conf['tag_name'],
                                                                            ssn_conf['instance_name']),
                   "role_name": ssn_conf['role_name'],
                   "role_profile_name": ssn_conf['role_profile_name'],
                   "policy_name": ssn_conf['policy_name'],
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": os.environ['aws_vpc_id'],
                   "subnet_id": os.environ['aws_subnet_id'],
                   "security_id": os.environ['aws_security_groups_ids'],
                   "instance_shape": os.environ['aws_ssn_instance_size'],
                   "region": ssn_conf['region'],
                   "action": "Create SSN instance"}
            f.write(json.dumps(res))

        print('Upload response file')
        params = "--instance_name {} --local_log_filepath {} --os_user {} --instance_hostname {}".\
            format(ssn_conf['instance_name'], local_log_filepath, ssn_conf['dlab_ssh_user'],
                   ssn_conf['instance_hostname'])
        local("~/scripts/{}.py {}".format('upload_response_file', params))

        logging.info('[FINALIZE]')
        print('[FINALIZE]')
        params = ""
        if os.environ['conf_lifecycle_stage'] == 'prod':
            params += "--key_id {}".format(os.environ['aws_access_key'])
            local("~/scripts/{}.py {}".format('ssn_finalize', params))
    except Exception as err:
        dlab.fab.append_result("Error with writing results.", str(err))
        clear_resources()
        sys.exit(1)
