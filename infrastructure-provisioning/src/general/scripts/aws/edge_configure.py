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

    print('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = os.environ['conf_service_base_name']
    edge_conf['key_name'] = os.environ['conf_key_name']
    edge_conf['user_key'] = os.environ['key']
    edge_conf['instance_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'], os.environ['project_name'])
    edge_conf['tag_name'] = edge_conf['service_base_name'] + '-Tag'
    edge_conf['bucket_name'] = '{}-{}-bucket'.format(edge_conf['service_base_name'],
                                                     os.environ['project_name']).lower().replace('_', '-')
    edge_conf['shared_bucket_name'] = (edge_conf['service_base_name'] + '-shared-bucket').lower().replace('_', '-')
    edge_conf['edge_security_group_name'] = '{}-SG'.format(edge_conf['instance_name'])
    edge_conf['notebook_instance_name'] = '{}-{}-nb'.format(edge_conf['service_base_name'],
                                                            os.environ['project_name'])
    edge_conf['notebook_role_profile_name'] = '{}-{}-nb-Profile' \
        .format(edge_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    edge_conf['notebook_security_group_name'] = '{}-{}-nb-SG'.format(edge_conf['service_base_name'],
                                                                     os.environ['project_name'])
    edge_conf['dataengine_instances_name'] = '{}-{}-dataengine' \
        .format(edge_conf['service_base_name'], os.environ['project_name'])
    tag = {"Key": edge_conf['tag_name'],
           "Value": "{}-{}-subnet".format(edge_conf['service_base_name'], os.environ['project_name'])}
    edge_conf['private_subnet_cidr'] = get_subnet_by_tag(tag)
    edge_conf['dlab_ssh_user'] = os.environ['conf_os_user']
    edge_conf['network_type'] = os.environ['conf_network_type']
    if edge_conf['network_type'] == 'public':
        edge_conf['edge_public_ip'] = get_instance_ip_address(edge_conf['tag_name'], edge_conf['instance_name']).get(
            'Public')
        edge_conf['edge_private_ip'] = get_instance_ip_address(edge_conf['tag_name'], edge_conf['instance_name']).get(
            'Private')
    elif edge_conf['network_type'] == 'private':
        edge_conf['edge_private_ip'] = get_instance_ip_address(edge_conf['tag_name'], edge_conf['instance_name']).get(
            'Private')
        edge_conf['edge_public_ip'] = edge_conf['edge_private_ip']
    edge_conf['vpc1_cidrs'] = get_vpc_cidr_by_id(os.environ['aws_vpc_id'])
    try:
        edge_conf['vpc2_cidrs'] = get_vpc_cidr_by_id(os.environ['aws_notebook_vpc_id'])
        edge_conf['vpc_cidrs'] = list(set(edge_conf['vpc1_cidrs'] + edge_conf['vpc2_cidrs']))
    except KeyError:
        edge_conf['vpc_cidrs'] = list(set(edge_conf['vpc1_cidrs']))

    edge_conf['allowed_ip_cidr'] = list()
    for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
        edge_conf['allowed_ip_cidr'].append(cidr.replace(' ', ''))


    instance_hostname = get_instance_hostname(edge_conf['tag_name'], edge_conf['instance_name'])
    keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], edge_conf['key_name'])

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
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['project_name'])
        sys.exit(1)

    try:
        print('[INSTALLING PREREQUISITES]')
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(instance_hostname, keyfile_name, edge_conf['dlab_ssh_user'], os.environ['aws_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing apps: apt & pip.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['project_name'])
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
                             "allowed_ip_cidr": edge_conf['allowed_ip_cidr']}
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
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['project_name'])
        sys.exit(1)


    try:
        print('[INSTALLING USERs KEY]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": os.environ['project_name'],
                             "user_keydir": os.environ['conf_key_dir'],
                             "user_key": edge_conf['user_key']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing users key." + str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['project_name'])
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
        append_result("Failed installing nginx reverse proxy." + str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['project_name'])
        sys.exit(1)

    try:
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(edge_conf['instance_name']))
        print("Hostname: {}".format(instance_hostname))
        print("Public IP: {}".format(edge_conf['edge_public_ip']))
        print("Private IP: {}".format(edge_conf['edge_private_ip']))
        print("Instance ID: {}".format(get_instance_by_name(edge_conf['tag_name'], edge_conf['instance_name'])))
        print("Key name: {}".format(edge_conf['key_name']))
        print("Bucket name: {}".format(edge_conf['bucket_name']))
        print("Shared bucket name: {}".format(edge_conf['shared_bucket_name']))
        print("Notebook SG: {}".format(edge_conf['notebook_security_group_name']))
        print("Notebook profiles: {}".format(edge_conf['notebook_role_profile_name']))
        print("Edge SG: {}".format(edge_conf['edge_security_group_name']))
        print("Notebook subnet: {}".format(edge_conf['private_subnet_cidr']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": instance_hostname,
                   "public_ip": edge_conf['edge_public_ip'],
                   "ip": edge_conf['edge_private_ip'],
                   "instance_id": get_instance_by_name(edge_conf['tag_name'], edge_conf['instance_name']),
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
                   "project_name": os.environ['project_name'],
                   "@class": "com.epam.dlab.dto.aws.edge.EdgeInfoAws",
                   "Action": "Create new EDGE server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)

    sys.exit(0)