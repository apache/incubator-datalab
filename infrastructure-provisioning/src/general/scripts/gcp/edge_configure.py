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
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    edge_conf['key_name'] = os.environ['conf_key_name']
    edge_conf['user_keyname'] = os.environ['project_name']
    edge_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            edge_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        edge_conf['vpc_name'] = edge_conf['service_base_name'] + '-ssn-vpc'
    edge_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
    edge_conf['subnet_name'] = '{0}-{1}-subnet'.format(edge_conf['service_base_name'], edge_conf['project_name'])
    edge_conf['region'] = os.environ['gcp_region']
    edge_conf['zone'] = os.environ['gcp_zone']
    edge_conf['vpc_selflink'] = GCPMeta().get_vpc(edge_conf['vpc_name'])['selfLink']
    edge_conf['private_subnet_prefix'] = os.environ['gcp_private_subnet_prefix']
    edge_conf['edge_service_account_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'],
                                                                 edge_conf['project_name'])
    edge_conf['edge_role_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'],
                                                      edge_conf['project_name'])
    edge_conf['ps_service_account_name'] = '{}-{}-ps'.format(edge_conf['service_base_name'],
                                                             edge_conf['project_name'])
    edge_conf['ps_role_name'] = '{}-{}-ps'.format(edge_conf['service_base_name'],
                                                  edge_conf['project_name'])
    edge_conf['instance_name'] = '{0}-{1}-edge'.format(edge_conf['service_base_name'], edge_conf['project_name'])
    edge_conf['firewall_name'] = edge_conf['instance_name'] + '{}-firewall'.format(edge_conf['instance_name'])
    edge_conf['notebook_firewall_name'] = '{0}-{1}-nb-firewall'.format(edge_conf['service_base_name'],
                                                                       edge_conf['project_name'])
    edge_conf['bucket_name'] = '{0}-{1}-bucket'.format(edge_conf['service_base_name'], edge_conf['project_name'])
    edge_conf['shared_bucket_name'] = '{}-shared-bucket'.format(edge_conf['service_base_name'])
    edge_conf['instance_size'] = os.environ['gcp_edge_instance_size']
    edge_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    edge_conf['static_address_name'] = '{0}-{1}-ip'.format(edge_conf['service_base_name'], edge_conf['project_name'])
    instance_hostname = GCPMeta().get_instance_public_ip_by_name(edge_conf['instance_name'])
    edge_conf['dlab_ssh_user'] = os.environ['conf_os_user']
    edge_conf['private_subnet_cidr'] = GCPMeta().get_subnet(edge_conf['subnet_name'],
                                                            edge_conf['region'])['ipCidrRange']
    edge_conf['static_ip'] = \
        GCPMeta().get_static_address(edge_conf['region'], edge_conf['static_address_name'])['address']
    edge_conf['private_ip'] = GCPMeta().get_private_ip_address(edge_conf['instance_name'])
    edge_conf['vpc_cidrs'] = [edge_conf['vpc_cidr']]
    edge_conf['fw_common_name'] = '{}-{}-ps'.format(edge_conf['service_base_name'], edge_conf['project_name'])
    edge_conf['fw_ps_ingress'] = '{}-ingress'.format(edge_conf['fw_common_name'])
    edge_conf['fw_ps_egress_private'] = '{}-egress-private'.format(edge_conf['fw_common_name'])
    edge_conf['fw_ps_egress_public'] = '{}-egress-public'.format(edge_conf['fw_common_name'])
    edge_conf['fw_edge_ingress_public'] = '{}-ingress-public'.format(edge_conf['instance_name'])
    edge_conf['fw_edge_ingress_internal'] = '{}-ingress-internal'.format(edge_conf['instance_name'])
    edge_conf['fw_edge_egress_public'] = '{}-egress-public'.format(edge_conf['instance_name'])
    edge_conf['fw_edge_egress_internal'] = '{}-egress-internal'.format(edge_conf['instance_name'])
    edge_conf['allowed_ip_cidr'] = list()
    for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
        edge_conf['allowed_ip_cidr'].append(cidr.replace(' ', ''))

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
            (instance_hostname, "/root/keys/" + os.environ['conf_key_name'] + ".pem", initial_user,
             edge_conf['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed creating ssh user 'dlab'.", str(err))
        GCPActions().remove_instance(edge_conf['instance_name'], edge_conf['zone'])
        GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_ps_ingress'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_private'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_public'])
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        print('[INSTALLING PREREQUISITES]')
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(instance_hostname, edge_conf['ssh_key_path'], edge_conf['dlab_ssh_user'], os.environ['gcp_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing apps: apt & pip.", str(err))
        GCPActions().remove_instance(edge_conf['instance_name'], edge_conf['zone'])
        GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_ps_ingress'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_private'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_public'])
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['subnet_name'], edge_conf['region'])
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
                 .format(instance_hostname, edge_conf['ssh_key_path'], json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_http_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing http proxy.", str(err))
        GCPActions().remove_instance(edge_conf['instance_name'], edge_conf['zone'])
        GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_ps_ingress'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_private'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_public'])
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['subnet_name'], edge_conf['region'])
        sys.exit(1)


    try:
        print('[INSTALLING USERs KEY]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": edge_conf['user_keyname'],
                             "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, edge_conf['ssh_key_path'], json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing users key. Excpeption: " + str(err))
        GCPActions().remove_instance(edge_conf['instance_name'], edge_conf['zone'])
        GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_ps_ingress'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_private'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_public'])
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        print('[INSTALLING NGINX REVERSE PROXY]')
        logging.info('[INSTALLING NGINX REVERSE PROXY]')
        params = "--hostname {} --keyfile {} --user {}" \
            .format(instance_hostname, edge_conf['ssh_key_path'], edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_nginx_reverse_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed installing nginx reverse proxy. Excpeption: " + str(err))
        GCPActions().remove_instance(edge_conf['instance_name'], edge_conf['zone'])
        GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_ps_ingress'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_private'])
        GCPActions().remove_firewall(edge_conf['fw_ps_egress_public'])
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(edge_conf['instance_name']))
        print("Hostname: {}".format(instance_hostname))
        print("Public IP: {}".format(edge_conf['static_ip']))
        print("Private IP: {}".format(edge_conf['private_ip']))
        print("Key name: {}".format(edge_conf['key_name']))
        print("Bucket name: {}".format(edge_conf['bucket_name']))
        print("Shared bucket name: {}".format(edge_conf['shared_bucket_name']))
        print("Notebook subnet: {}".format(edge_conf['private_subnet_cidr']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": instance_hostname,
                   "public_ip": edge_conf['static_ip'],
                   "ip": edge_conf['private_ip'],
                   "instance_id": edge_conf['instance_name'],
                   "key_name": edge_conf['key_name'],
                   "user_own_bucket_name": edge_conf['bucket_name'],
                   "shared_bucket_name": edge_conf['shared_bucket_name'],
                   "tunnel_port": "22",
                   "socks_port": "1080",
                   "notebook_subnet": edge_conf['private_subnet_cidr'],
                   "full_edge_conf": edge_conf,
                   "project_name": os.environ['project_name'],
                   "Action": "Create new EDGE server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)
