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
        dlab.actions_lib.remove_all_iam_resources('notebook', edge_conf['project_name'])
        dlab.actions_lib.remove_all_iam_resources('edge', edge_conf['project_name'])
        dlab.actions_lib.remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        dlab.actions_lib.remove_sgroups(edge_conf['dataengine_instances_name'])
        dlab.actions_lib.remove_sgroups(edge_conf['notebook_instance_name'])
        dlab.actions_lib.remove_sgroups(edge_conf['instance_name'])
        dlab.actions_lib.remove_s3('edge', edge_conf['project_name'])

    try:
        print('Generating infrastructure names and tags')
        edge_conf = dict()
        edge_conf['service_base_name'] = os.environ['conf_service_base_name'] = dlab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        edge_conf['key_name'] = os.environ['conf_key_name']
        edge_conf['user_key'] = os.environ['key']
        edge_conf['project_name'] = os.environ['project_name']
        edge_conf['endpoint_name'] = os.environ['endpoint_name']
        edge_conf['instance_name'] = '{}-{}-{}-edge'.format(edge_conf['service_base_name'], edge_conf['project_name'],
                                                            edge_conf['endpoint_name'])
        edge_conf['tag_name'] = edge_conf['service_base_name'] + '-tag'
        edge_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(edge_conf['service_base_name'],
                                                               edge_conf['project_name'],
                                                               edge_conf['endpoint_name']).replace('_', '-')
        edge_conf['shared_bucket_name'] = '{0}-{1}-shared-bucket'.format(edge_conf['service_base_name'],
                                                                         edge_conf['endpoint_name']).replace('_', '-')
        edge_conf['edge_security_group_name'] = '{}-{}-{}-edge-sg'.format(edge_conf['service_base_name'],
                                                                          edge_conf['project_name'],
                                                                          edge_conf['endpoint_name'])
        edge_conf['notebook_instance_name'] = '{}-{}-{}-nb'.format(edge_conf['service_base_name'],
                                                                   edge_conf['project_name'],
                                                                   edge_conf['endpoint_name'])
        edge_conf['notebook_role_profile_name'] = '{}-{}-{}-nb-profile'.format(edge_conf['service_base_name'],
                                                                               edge_conf['project_name'],
                                                                               edge_conf['endpoint_name'])
        edge_conf['notebook_security_group_name'] = '{}-{}-{}-nb-sg'.format(edge_conf['service_base_name'],
                                                                            edge_conf['project_name'],
                                                                            edge_conf['endpoint_name'])
        edge_conf['dataengine_instances_name'] = '{}-{}-{}-de'.format(edge_conf['service_base_name'],
                                                                      edge_conf['project_name'],
                                                                      edge_conf['endpoint_name'])
        tag = {"Key": edge_conf['tag_name'],
               "Value": "{}-{}-{}-subnet".format(edge_conf['service_base_name'], edge_conf['project_name'],
                                                 edge_conf['endpoint_name'])}
        edge_conf['private_subnet_cidr'] = dlab.meta_lib.get_subnet_by_tag(tag)
        edge_conf['dlab_ssh_user'] = os.environ['conf_os_user']
        edge_conf['network_type'] = os.environ['conf_network_type']
        if edge_conf['network_type'] == 'public':
            edge_conf['edge_public_ip'] = dlab.meta_lib.get_instance_ip_address(edge_conf['tag_name'],
                                                                  edge_conf['instance_name']).get('Public')
            edge_conf['edge_private_ip'] = dlab.meta_lib.get_instance_ip_address(
                edge_conf['tag_name'], edge_conf['instance_name']).get('Private')
        elif edge_conf['network_type'] == 'private':
            edge_conf['edge_private_ip'] = dlab.meta_lib.get_instance_ip_address(
                edge_conf['tag_name'], edge_conf['instance_name']).get('Private')
            edge_conf['edge_public_ip'] = edge_conf['edge_private_ip']
        edge_conf['vpc1_cidrs'] = dlab.meta_lib.get_vpc_cidr_by_id(os.environ['aws_vpc_id'])
        try:
            edge_conf['vpc2_cidrs'] = dlab.meta_lib.get_vpc_cidr_by_id(os.environ['aws_notebook_vpc_id'])
            edge_conf['vpc_cidrs'] = list(set(edge_conf['vpc1_cidrs'] + edge_conf['vpc2_cidrs']))
        except KeyError:
            edge_conf['vpc_cidrs'] = list(set(edge_conf['vpc1_cidrs']))

        edge_conf['allowed_ip_cidr'] = list()
        for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
            edge_conf['allowed_ip_cidr'].append(cidr.replace(' ', ''))

        edge_conf['instance_hostname'] = dlab.meta_lib.get_instance_hostname(edge_conf['tag_name'],
                                                                             edge_conf['instance_name'])
        edge_conf['keyfile_name'] = "{}{}.pem".format(os.environ['conf_key_dir'], edge_conf['key_name'])

        if os.environ['conf_stepcerts_enabled'] == 'true':
            edge_conf['step_cert_sans'] = ' --san {0} '.format(edge_conf['edge_private_ip'])
            if edge_conf['network_type'] == 'public':
                edge_conf['step_cert_sans'] += ' --san {0} --san {1}'.format(
                    dlab.meta_lib.get_instance_hostname(edge_conf['tag_name'], edge_conf['instance_name']),
                    edge_conf['edge_public_ip'])
        else:
            edge_conf['step_cert_sans'] = ''
        if os.environ['conf_os_family'] == 'debian':
            edge_conf['initial_user'] = 'ubuntu'
            edge_conf['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            edge_conf['initial_user'] = 'ec2-user'
            edge_conf['sudo_group'] = 'wheel'
    except Exception as err:
        dlab.fab.append_result("Failed to generate variables dictionary.", str(err))
        clear_resources()
        sys.exit(1)

    try:
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
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(edge_conf['instance_hostname'], edge_conf['keyfile_name'], edge_conf['dlab_ssh_user'],
                   os.environ['aws_region'])
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
                             "allowed_ip_cidr": edge_conf['allowed_ip_cidr']}
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
        additional_config = {"user_keyname": edge_conf['project_name'],
                             "user_keydir": os.environ['conf_key_dir'],
                             "user_key": edge_conf['user_key']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            edge_conf['instance_hostname'], edge_conf['keyfile_name'], json.dumps(additional_config),
            edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing users key." + str(err))
        clear_resources()
        sys.exit(1)

    try:
        print('[INSTALLING NGINX REVERSE PROXY]')
        logging.info('[INSTALLING NGINX REVERSE PROXY]')
        edge_conf['keycloak_client_secret'] = str(uuid.uuid4())
        params = "--hostname {} --keyfile {} --user {} --keycloak_client_id {} --keycloak_client_secret {} " \
                 "--step_cert_sans '{}' ".format(
                  edge_conf['instance_hostname'], edge_conf['keyfile_name'], edge_conf['dlab_ssh_user'],
                  '{}-{}-{}'.format(edge_conf['service_base_name'], edge_conf['project_name'],
                                    edge_conf['endpoint_name']),
                  edge_conf['keycloak_client_secret'], edge_conf['step_cert_sans'])
        try:
            local("~/scripts/{}.py {}".format('configure_nginx_reverse_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
        keycloak_params = "--service_base_name {} --keycloak_auth_server_url {} --keycloak_realm_name {} " \
                          "--keycloak_user {} --keycloak_user_password {} --keycloak_client_secret {} " \
                          "--edge_public_ip {} --hostname {} --project_name {} --endpoint_name {} ".format(
                           edge_conf['service_base_name'], os.environ['keycloak_auth_server_url'],
                           os.environ['keycloak_realm_name'], os.environ['keycloak_user'],
                           os.environ['keycloak_user_password'], edge_conf['keycloak_client_secret'],
                           edge_conf['instance_hostname'], edge_conf['instance_hostname'], edge_conf['project_name'],
                           edge_conf['endpoint_name'])
        try:
            local("~/scripts/{}.py {}".format('configure_keycloak', keycloak_params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing nginx reverse proxy." + str(err))
        clear_resources()
        sys.exit(1)

    try:
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(edge_conf['instance_name']))
        print("Hostname: {}".format(edge_conf['instance_hostname']))
        print("Public IP: {}".format(edge_conf['edge_public_ip']))
        print("Private IP: {}".format(edge_conf['edge_private_ip']))
        print("Instance ID: {}".format(dlab.meta_lib.get_instance_by_name(edge_conf['tag_name'],
                                                                          edge_conf['instance_name'])))
        print("Key name: {}".format(edge_conf['key_name']))
        print("Bucket name: {}".format(edge_conf['bucket_name']))
        print("Shared bucket name: {}".format(edge_conf['shared_bucket_name']))
        print("Notebook SG: {}".format(edge_conf['notebook_security_group_name']))
        print("Notebook profiles: {}".format(edge_conf['notebook_role_profile_name']))
        print("Edge SG: {}".format(edge_conf['edge_security_group_name']))
        print("Notebook subnet: {}".format(edge_conf['private_subnet_cidr']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": edge_conf['instance_hostname'],
                   "public_ip": edge_conf['edge_public_ip'],
                   "ip": edge_conf['edge_private_ip'],
                   "instance_id": dlab.meta_lib.get_instance_by_name(edge_conf['tag_name'], edge_conf['instance_name']),
                   "key_name": edge_conf['key_name'],
                   "user_own_bicket_name": edge_conf['bucket_name'],
                   "shared_bucket_name": edge_conf['shared_bucket_name'],
                   "tunnel_port": "22",
                   "socks_port": "1080",
                   "notebook_sg": edge_conf['notebook_security_group_name'],
                   "notebook_profile": edge_conf['notebook_role_profile_name'],
                   "edge_sg": edge_conf['edge_security_group_name'],
                   "notebook_subnet": edge_conf['private_subnet_cidr'],
                   "full_edge_conf": edge_conf,
                   "project_name": edge_conf['project_name'],
                   "@class": "com.epam.dlab.dto.aws.edge.EdgeInfoAws",
                   "Action": "Create new EDGE server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results.", str(err))
        clear_resources()
        sys.exit(1)

