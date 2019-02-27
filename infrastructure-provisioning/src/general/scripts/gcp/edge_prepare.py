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
import traceback


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    edge_conf['key_name'] = os.environ['conf_key_name']
    edge_conf['user_keyname'] = os.environ['edge_user_name']
    edge_conf['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            edge_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        edge_conf['vpc_name'] = edge_conf['service_base_name'] + '-ssn-vpc'
    edge_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
    edge_conf['private_subnet_name'] = '{0}-{1}-subnet'.format(edge_conf['service_base_name'],
                                                               edge_conf['edge_user_name'])
    edge_conf['subnet_name'] = os.environ['gcp_subnet_name']
    edge_conf['region'] = os.environ['gcp_region']
    edge_conf['zone'] = os.environ['gcp_zone']
    edge_conf['vpc_selflink'] = GCPMeta().get_vpc(edge_conf['vpc_name'])['selfLink']
    edge_conf['private_subnet_prefix'] = os.environ['gcp_private_subnet_prefix']
    edge_conf['edge_service_account_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'],
                                                                 edge_conf['edge_user_name'])
    edge_conf['edge_role_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'],
                                                      edge_conf['edge_user_name'])
    edge_conf['ps_service_account_name'] = '{}-{}-ps'.format(edge_conf['service_base_name'],
                                                             edge_conf['edge_user_name'])
    edge_conf['ps_role_name'] = '{}-{}-ps'.format(edge_conf['service_base_name'],
                                                  edge_conf['edge_user_name'])
    edge_conf['ps_policy_path'] = '/root/files/ps_policy.json'
    edge_conf['ps_roles_path'] = '/root/files/ps_roles.json'
    edge_conf['instance_name'] = '{0}-{1}-edge'.format(edge_conf['service_base_name'], edge_conf['edge_user_name'])
    edge_conf['ssn_instance_name'] = '{}-ssn'.format(edge_conf['service_base_name'])
    edge_conf['bucket_name'] = '{0}-{1}-bucket'.format(edge_conf['service_base_name'], edge_conf['edge_user_name'])
    edge_conf['shared_bucket_name'] = '{}-shared-bucket'.format(edge_conf['service_base_name'])
    edge_conf['instance_size'] = os.environ['gcp_edge_instance_size']
    edge_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    edge_conf['image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
    edge_conf['static_address_name'] = '{0}-{1}-ip'.format(edge_conf['service_base_name'], edge_conf['edge_user_name'])
    edge_conf['fw_edge_ingress_public'] = '{}-ingress-public'.format(edge_conf['instance_name'])
    edge_conf['fw_edge_ingress_internal'] = '{}-ingress-internal'.format(edge_conf['instance_name'])
    edge_conf['fw_edge_egress_public'] = '{}-egress-public'.format(edge_conf['instance_name'])
    edge_conf['fw_edge_egress_internal'] = '{}-egress-internal'.format(edge_conf['instance_name'])
    edge_conf['ps_firewall_target'] = '{0}-{1}-ps'.format(edge_conf['service_base_name'],
                                                          edge_conf['edge_user_name'])
    edge_conf['fw_common_name'] = '{}-{}-ps'.format(edge_conf['service_base_name'], edge_conf['edge_user_name'])
    edge_conf['fw_ps_ingress'] = '{}-ingress'.format(edge_conf['fw_common_name'])
    edge_conf['fw_ps_egress_private'] = '{}-egress-private'.format(edge_conf['fw_common_name'])
    edge_conf['fw_ps_egress_public'] = '{}-egress-public'.format(edge_conf['fw_common_name'])
    edge_conf['network_tag'] = edge_conf['instance_name']
    edge_conf['instance_labels'] = {"name": edge_conf['instance_name'],
                                    "sbn": edge_conf['service_base_name'],
                                    "user": edge_conf['edge_user_name'],
                                    "product": "dlab"}
    edge_conf['allowed_ip_cidr'] = os.environ['conf_allowed_ip_cidr']

    # FUSE in case of absence of user's key
    fname = "/root/keys/{}.pub".format(edge_conf['user_keyname'])
    if not os.path.isfile(fname):
        print("USERs PUBLIC KEY DOES NOT EXIST in {}".format(fname))
        sys.exit(1)

    print("Will create exploratory environment with edge node as access point as following: ".format(json.dumps(
        edge_conf, sort_keys=True, indent=4, separators=(',', ': '))))
    logging.info(json.dumps(edge_conf))

    try:
        logging.info('[CREATE SUBNET]')
        print('[CREATE SUBNET]')
        params = "--subnet_name {} --region {} --vpc_selflink {} --prefix {} --vpc_cidr {}" \
                 .format(edge_conf['private_subnet_name'], edge_conf['region'], edge_conf['vpc_selflink'],
                         edge_conf['private_subnet_prefix'], edge_conf['vpc_cidr'])
        try:
            local("~/scripts/{}.py {}".format('common_create_subnet', params))
            edge_conf['private_subnet_cidr'] = GCPMeta().get_subnet(edge_conf['private_subnet_name'],
                                                                    edge_conf['region'])['ipCidrRange']
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        try:
            GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        except:
            print("Subnet hasn't been created.")
        append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    print('NEW SUBNET CIDR CREATED: {}'.format(edge_conf['private_subnet_cidr']))

    try:
        logging.info('[CREATE SERVICE ACCOUNT AND ROLE FOR EDGE NODE]')
        print('[CREATE SERVICE ACCOUNT AND ROLE FOR EDGE NODE]')
        params = "--service_account_name {} --role_name {}".format(edge_conf['edge_service_account_name'],
                                                                   edge_conf['edge_role_name'])

        try:
            local("~/scripts/{}.py {}".format('common_create_service_account', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        try:
            GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
            GCPActions().remove_role(edge_conf['edge_role_name'])
        except:
            print("Service account or role hasn't been created")
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        append_result("Failed to creating service account and role.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE SERVICE ACCOUNT AND ROLE FOR PRIVATE SUBNET]')
        print('[CREATE SERVICE ACCOUNT AND ROLE FOR NOTEBOOK NODE]')
        params = "--service_account_name {} --role_name {} --policy_path {} --roles_path {}".format(
            edge_conf['ps_service_account_name'], edge_conf['ps_role_name'],
            edge_conf['ps_policy_path'], edge_conf['ps_roles_path'])

        try:
            local("~/scripts/{}.py {}".format('common_create_service_account', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        try:
            GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
            GCPActions().remove_role(edge_conf['ps_role_name'])
        except:
            print("Service account or role hasn't been created")
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        append_result("Failed to creating service account and role.", str(err))
        sys.exit(1)

    try:
        pre_defined_firewall = True
        logging.info('[CREATE FIREWALL FOR EDGE NODE]')
        print('[CREATE FIREWALL FOR EDGE NODE]')
        firewall_rules = dict()
        firewall_rules['ingress'] = []
        firewall_rules['egress'] = []

        ingress_rule = dict()
        ingress_rule['name'] = edge_conf['fw_edge_ingress_public']
        ingress_rule['targetTags'] = [edge_conf['network_tag']]
        ingress_rule['sourceRanges'] = [edge_conf['allowed_ip_cidr']]
        rules = [
            {
                'IPProtocol': 'tcp',
                'ports': ['22']
            }
        ]
        ingress_rule['allowed'] = rules
        ingress_rule['network'] = edge_conf['vpc_selflink']
        ingress_rule['direction'] = 'INGRESS'
        firewall_rules['ingress'].append(ingress_rule)

        ingress_rule = dict()
        ingress_rule['name'] = edge_conf['fw_edge_ingress_internal']
        ingress_rule['targetTags'] = [edge_conf['network_tag']]
        ingress_rule['sourceRanges'] = [edge_conf['private_subnet_cidr']]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        ingress_rule['allowed'] = rules
        ingress_rule['network'] = edge_conf['vpc_selflink']
        ingress_rule['direction'] = 'INGRESS'
        firewall_rules['ingress'].append(ingress_rule)

        egress_rule = dict()
        egress_rule['name'] = edge_conf['fw_edge_egress_public']
        egress_rule['targetTags'] = [edge_conf['network_tag']]
        egress_rule['destinationRanges'] = [edge_conf['allowed_ip_cidr']]
        rules = [
            {
                'IPProtocol': 'udp',
                'ports': ['53']
            },
            {
                'IPProtocol': 'tcp',
                'ports': ['80', '443']
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = edge_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        egress_rule = dict()
        egress_rule['name'] = edge_conf['fw_edge_egress_internal']
        egress_rule['targetTags'] = [edge_conf['network_tag']]
        egress_rule['destinationRanges'] = [edge_conf['private_subnet_cidr']]
        rules = [
            {
                'IPProtocol': 'tcp',
                'ports': ['22', '8888', '8080', '8787', '6006', '20888', '8088', '18080', '50070', '8085', '8081',
                          '4040-4045']
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = edge_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        params = "--firewall '{}'".format(json.dumps(firewall_rules))
        try:
            local("~/scripts/{}.py {}".format('common_create_firewall', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        append_result("Failed to create firewall for Edge node.", str(err))
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATE FIREWALL FOR PRIVATE SUBNET]')
        print('[CREATE FIREWALL FOR PRIVATE SUBNET]')
        firewall_rules = dict()
        firewall_rules['ingress'] = []
        firewall_rules['egress'] = []

        ingress_rule = dict()
        ingress_rule['name'] = edge_conf['fw_ps_ingress']
        ingress_rule['targetTags'] = [
            edge_conf['ps_firewall_target']
        ]
        ingress_rule['sourceRanges'] = [edge_conf['private_subnet_cidr'],
                                        GCPMeta().get_subnet(edge_conf['subnet_name'],
                                                             edge_conf['region'])['ipCidrRange']
                                        ]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        ingress_rule['allowed'] = rules
        ingress_rule['network'] = edge_conf['vpc_selflink']
        ingress_rule['direction'] = 'INGRESS'
        firewall_rules['ingress'].append(ingress_rule)

        egress_rule = dict()
        egress_rule['name'] = edge_conf['fw_ps_egress_private']
        egress_rule['targetTags'] = [
            edge_conf['ps_firewall_target']
        ]
        egress_rule['destinationRanges'] = [edge_conf['private_subnet_cidr'],
                                            GCPMeta().get_subnet(edge_conf['subnet_name'],
                                                                 edge_conf['region'])['ipCidrRange']
                                            ]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = edge_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        egress_rule = dict()
        egress_rule['name'] = edge_conf['fw_ps_egress_public']
        egress_rule['targetTags'] = [
            edge_conf['ps_firewall_target']
        ]
        egress_rule['destinationRanges'] = [edge_conf['allowed_ip_cidr']]
        rules = [
            {
                'IPProtocol': 'tcp',
                'ports': ['443']
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = edge_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        params = "--firewall '{}'".format(json.dumps(firewall_rules))
        try:
            local("~/scripts/{}.py {}".format('common_create_firewall', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create firewall for private subnet.", str(err))
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_ingress_internal'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_public'])
        GCPActions().remove_firewall(edge_conf['fw_edge_egress_internal'])
        GCPActions().remove_service_account(edge_conf['ps_service_account_name'])
        GCPActions().remove_role(edge_conf['ps_role_name'])
        GCPActions().remove_service_account(edge_conf['edge_service_account_name'])
        GCPActions().remove_role(edge_conf['edge_role_name'])
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {}".format(edge_conf['bucket_name'])

        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create bucket.", str(err))
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
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        logging.info('[SET PERMISSIONS FOR USER AND SHARED BUCKETS]')
        print('[SET PERMISSIONS FOR USER AND SHARED BUCKETS]')
        GCPActions().set_bucket_owner(edge_conf['bucket_name'], edge_conf['ps_service_account_name'])
        GCPActions().set_bucket_owner(edge_conf['shared_bucket_name'], edge_conf['ps_service_account_name'])
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to set bucket permissions.", str(err))
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
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATING STATIC IP ADDRESS]')
        print('[CREATING STATIC IP ADDRESS]')
        params = "--address_name {} --region {}".format(edge_conf['static_address_name'], edge_conf['region'])
        try:
            local("~/scripts/{}.py {}".format('edge_create_static_ip', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create static ip.", str(err))
        try:
            GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        except:
            print("Static IP address hasn't been created.")
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
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    try:
        edge_conf['static_ip'] = \
            GCPMeta().get_static_address(edge_conf['region'], edge_conf['static_address_name'])['address']
        logging.info('[CREATE EDGE INSTANCE]')
        print('[CREATE EDGE INSTANCE]')
        params = "--instance_name {} --region {} --zone {} --vpc_name {} --subnet_name {} --instance_size {} --ssh_key_path {} --initial_user {} --service_account_name {} --image_name {} --instance_class {} --static_ip {} --network_tag {} --labels '{}'".\
            format(edge_conf['instance_name'], edge_conf['region'], edge_conf['zone'], edge_conf['vpc_name'],
                   edge_conf['subnet_name'], edge_conf['instance_size'], edge_conf['ssh_key_path'], initial_user,
                   edge_conf['edge_service_account_name'], edge_conf['image_name'], 'edge', edge_conf['static_ip'],
                   edge_conf['network_tag'], json.dumps(edge_conf['instance_labels']))
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create instance.", str(err))
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
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)