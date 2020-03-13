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

import sys, os
from fabric.api import *
import json
import argparse
import logging
import traceback
import dlab.ssn_lib
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib

parser = argparse.ArgumentParser()
parser.add_argument('--ssn_unique_index', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        GCPMeta = dlab.meta_lib.GCPMeta()
        GCPActions = dlab.actions_lib.GCPActions()
        ssn_conf = dict()
        ssn_conf['instance'] = 'ssn'
        ssn_conf['pre_defined_vpc'] = False
        ssn_conf['pre_defined_subnet'] = False
        ssn_conf['pre_defined_firewall'] = False
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')
        ssn_conf['ssn_unique_index'] = args.ssn_unique_index
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = dlab.fab.replace_multi_symbols(
                os.environ['conf_service_base_name'].replace('_', '-').lower()[:20], '-', True)
        ssn_conf['region'] = os.environ['gcp_region']
        ssn_conf['zone'] = os.environ['gcp_zone']
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        ssn_conf['instance_size'] = os.environ['gcp_ssn_instance_size']
        ssn_conf['vpc_name'] = '{}-vpc'.format(ssn_conf['service_base_name'])
        ssn_conf['subnet_name'] = '{}-subnet'.format(ssn_conf['service_base_name'])
        ssn_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
        ssn_conf['subnet_prefix'] = '20'
        ssn_conf['firewall_name'] = '{}-ssn-sg'.format(ssn_conf['service_base_name'])
        ssn_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        ssn_conf['service_account_name'] = '{}-ssn-sa'.format(ssn_conf['service_base_name']).replace('_', '-')
        ssn_conf['image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
        ssn_conf['role_name'] = '{}-{}-ssn-role'.format(ssn_conf['service_base_name'], ssn_conf['ssn_unique_index'])
        ssn_conf['static_address_name'] = '{}-ssn-static-ip'.format(ssn_conf['service_base_name'])
        ssn_conf['ssn_policy_path'] = '/root/files/ssn_policy.json'
        ssn_conf['ssn_roles_path'] = '/root/files/ssn_roles.json'
        ssn_conf['network_tag'] = ssn_conf['instance_name']
        ssn_conf['instance_labels'] = {"name": ssn_conf['instance_name'],
                                       "sbn": ssn_conf['service_base_name'],
                                       os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        ssn_conf['allowed_ip_cidr'] = os.environ['conf_allowed_ip_cidr']
    except Exception as err:
        dlab.fab.dlab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    if GCPMeta.get_instance(ssn_conf['instance_name']):
        dlab.fab.dlab.fab.append_result("Service base name should be unique and less or equal 20 symbols. "
                                        "Please try again.")
        sys.exit(1)

    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            ssn_conf['pre_defined_vpc'] = True
            ssn_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        try:
            logging.info('[CREATE VPC]')
            print('[CREATE VPC]')
            params = "--vpc_name {}".format(ssn_conf['vpc_name'])
            try:
                local("~/scripts/{}.py {}".format('ssn_create_vpc', params))
                os.environ['gcp_vpc_name'] = ssn_conf['vpc_name']
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            dlab.fab.append_result("Failed to create VPC.", str(err))
            if not ssn_conf['pre_defined_vpc']:
                try:
                    GCPActions.remove_vpc(ssn_conf['vpc_name'])
                except:
                    print("VPC hasn't been created.")
            sys.exit(1)

    try:
        ssn_conf['vpc_selflink'] = GCPMeta.get_vpc(ssn_conf['vpc_name'])['selfLink']
        if os.environ['gcp_subnet_name'] == '':
            raise KeyError
        else:
            ssn_conf['pre_defined_subnet'] = True
            ssn_conf['subnet_name'] = os.environ['gcp_subnet_name']
    except KeyError:
        try:
            logging.info('[CREATE SUBNET]')
            print('[CREATE SUBNET]')
            params = "--subnet_name {} --region {} --vpc_selflink {} --prefix {} --vpc_cidr {} --ssn {}".\
                format(ssn_conf['subnet_name'], ssn_conf['region'], ssn_conf['vpc_selflink'], ssn_conf['subnet_prefix'],
                       ssn_conf['vpc_cidr'], True)
            try:
                local("~/scripts/{}.py {}".format('common_create_subnet', params))
                os.environ['gcp_subnet_name'] = ssn_conf['subnet_name']
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            dlab.fab.append_result("Failed to create Subnet.", str(err))
            if not ssn_conf['pre_defined_subnet']:
                try:
                    GCPActions.remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
                except:
                    print("Subnet hasn't been created.")
            if not ssn_conf['pre_defined_vpc']:
                GCPActions.remove_vpc(ssn_conf['vpc_name'])
            sys.exit(1)


    try:
        if os.environ['gcp_firewall_name'] == '':
            raise KeyError
        else:
            ssn_conf['pre_defined_firewall'] = True
            ssn_conf['firewall_name'] = os.environ['gcp_firewall_name']
    except KeyError:
        try:
            logging.info('[CREATE FIREWALL]')
            print('[CREATE FIREWALL]')
            firewall_rules = dict()
            firewall_rules['ingress'] = []
            firewall_rules['egress'] = []

            ingress_rule = dict()
            ingress_rule['name'] = '{}-ingress'.format(ssn_conf['firewall_name'])
            ingress_rule['targetTags'] = [ssn_conf['network_tag']]
            ingress_rule['sourceRanges'] = [ssn_conf['allowed_ip_cidr']]
            rules = [
                {
                    'IPProtocol': 'tcp',
                    'ports': ['22', '80', '443']
                }
            ]
            ingress_rule['allowed'] = rules
            ingress_rule['network'] = ssn_conf['vpc_selflink']
            ingress_rule['direction'] = 'INGRESS'
            firewall_rules['ingress'].append(ingress_rule)

            egress_rule = dict()
            egress_rule['name'] = '{}-egress'.format(ssn_conf['firewall_name'])
            egress_rule['targetTags'] = [ssn_conf['network_tag']]
            egress_rule['destinationRanges'] = [ssn_conf['allowed_ip_cidr']]
            rules = [
                {
                    'IPProtocol': 'all',
                }
            ]
            egress_rule['allowed'] = rules
            egress_rule['network'] = ssn_conf['vpc_selflink']
            egress_rule['direction'] = 'EGRESS'
            firewall_rules['egress'].append(egress_rule)

            params = "--firewall '{}'".format(json.dumps(firewall_rules))
            try:
                local("~/scripts/{}.py {}".format('common_create_firewall', params))
                os.environ['gcp_firewall_name'] = ssn_conf['firewall_name']
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            dlab.fab.append_result("Failed to create Firewall.", str(err))
            if not ssn_conf['pre_defined_subnet']:
                GCPActions.remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
            if not ssn_conf['pre_defined_vpc']:
                GCPActions.remove_vpc(ssn_conf['vpc_name'])
            sys.exit(1)

    try:
        logging.info('[CREATE SERVICE ACCOUNT AND ROLE]')
        print('[CREATE SERVICE ACCOUNT AND ROLE]')
        params = "--service_account_name {} --role_name {} --policy_path {} --roles_path {} --unique_index {} " \
                 "--service_base_name {}".format( ssn_conf['service_account_name'], ssn_conf['role_name'],
                                                  ssn_conf['ssn_policy_path'], ssn_conf['ssn_roles_path'],
                                                  ssn_conf['ssn_unique_index'], ssn_conf['service_base_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_service_account', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Unable to create Service account and role.", str(err))
        try:
            GCPActions.remove_service_account(ssn_conf['service_account_name'], ssn_conf['service_base_name'])
            GCPActions.remove_role(ssn_conf['role_name'])
        except:
            print("Service account hasn't been created")
        if not ssn_conf['pre_defined_firewall']:
            GCPActions.remove_firewall('{}-ingress'.format(ssn_conf['firewall_name']))
            GCPActions.remove_firewall('{}-egress'.format(ssn_conf['firewall_name']))
        if not ssn_conf['pre_defined_subnet']:
            GCPActions.remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if not ssn_conf['pre_defined_vpc']:
            GCPActions.remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CREATING STATIC IP ADDRESS]')
        print('[CREATING STATIC IP ADDRESS]')
        params = "--address_name {} --region {}".format(ssn_conf['static_address_name'], ssn_conf['region'])
        try:
            local("~/scripts/{}.py {}".format('ssn_create_static_ip', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to create static ip.", str(err))
        try:
            GCPActions.remove_static_address(ssn_conf['static_address_name'], ssn_conf['region'])
        except:
            print("Static IP address hasn't been created.")
        GCPActions.remove_service_account(ssn_conf['service_account_name'], ssn_conf['service_base_name'])
        GCPActions.remove_role(ssn_conf['role_name'])
        GCPActions.remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions.remove_bucket(ssn_conf['shared_bucket_name'])
        if not ssn_conf['pre_defined_firewall']:
            GCPActions.remove_firewall('{}-ingress'.format(ssn_conf['firewall_name']))
            GCPActions.remove_firewall('{}-egress'.format(ssn_conf['firewall_name']))
        if not ssn_conf['pre_defined_subnet']:
            GCPActions.remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if not ssn_conf['pre_defined_vpc']:
            GCPActions.remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        ssn_conf['initial_user'] = 'ubuntu'
        ssn_conf['sudo_group'] = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        ssn_conf['initial_user'] = 'ec2-user'
        ssn_conf['sudo_group'] = 'wheel'

    try:
        ssn_conf['static_ip'] = GCPMeta.get_static_address(ssn_conf['region'],
                                                           ssn_conf['static_address_name'])['address']
        logging.info('[CREATE SSN INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--instance_name {0} --region {1} --zone {2} --vpc_name {3} --subnet_name {4} --instance_size {5}"\
                 " --ssh_key_path {6} --initial_user {7} --service_account_name {8} --image_name {9}"\
                 " --instance_class {10} --static_ip {11} --network_tag {12} --labels '{13}' " \
                 "--primary_disk_size {14} --service_base_name {15}".\
            format(ssn_conf['instance_name'], ssn_conf['region'], ssn_conf['zone'], ssn_conf['vpc_name'],
                   ssn_conf['subnet_name'], ssn_conf['instance_size'], ssn_conf['ssh_key_path'],
                   ssn_conf['initial_user'], ssn_conf['service_account_name'], ssn_conf['image_name'], 'ssn',
                   ssn_conf['static_ip'], ssn_conf['network_tag'], json.dumps(ssn_conf['instance_labels']), '20',
                   ssn_conf['service_base_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Unable to create ssn instance.", str(err))
        GCPActions.remove_service_account(ssn_conf['service_account_name'], ssn_conf['service_base_name'])
        GCPActions.remove_role(ssn_conf['role_name'])
        GCPActions.remove_static_address(ssn_conf['static_address_name'], ssn_conf['region'])
        GCPActions.remove_bucket(ssn_conf['ssn_bucket_name'])
        GCPActions.remove_bucket(ssn_conf['shared_bucket_name'])
        if not ssn_conf['pre_defined_firewall']:
            GCPActions.remove_firewall('{}-ingress'.format(ssn_conf['firewall_name']))
            GCPActions.remove_firewall('{}-egress'.format(ssn_conf['firewall_name']))
        if not ssn_conf['pre_defined_subnet']:
            GCPActions.remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if not ssn_conf['pre_defined_vpc']:
            GCPActions.remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)
