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
import json


if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'
    pre_defined_vpc = False
    pre_defined_subnet = False
    pre_defined_firewall = False
    logging.info('[DERIVING NAMES]')
    print('[DERIVING NAMES]')
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
    ssn_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
    ssn_conf['subnet_prefix'] = '20'
    ssn_conf['firewall_name'] = '{}-ssn-firewall'.format(ssn_conf['service_base_name'])
    ssn_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    ssn_conf['service_account_name'] = '{}-ssn-sa'.format(ssn_conf['service_base_name']).replace('_', '-')
    ssn_conf['image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
    ssn_conf['role_name'] = ssn_conf['service_base_name'] + '-ssn-role'
    ssn_conf['static_address_name'] = '{}-ssn-ip'.format(ssn_conf['service_base_name'])
    ssn_conf['ssn_policy_path'] = '/root/files/ssn_policy.json'
    ssn_conf['ssn_roles_path'] = '/root/files/ssn_roles.json'
    ssn_conf['network_tag'] = ssn_conf['instance_name']
    ssn_conf['instance_labels'] = {"name": ssn_conf['instance_name'],
                                   "sbn": ssn_conf['service_base_name'],
                                   os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
    ssn_conf['allowed_ip_cidr'] = os.environ['conf_allowed_ip_cidr']

    if GCPMeta().get_instance(ssn_conf['instance_name']):
        print("Service base name should be unique and less or equal 12 symbols. Please try again.")
        sys.exit(1)

    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            ssn_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        try:
            pre_defined_vpc = True
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
            print('Error: {0}'.format(err))
            append_result("Failed to create VPC. Exception:" + str(err))
            if pre_defined_vpc:
                try:
                    GCPActions().remove_vpc(ssn_conf['vpc_name'])
                except:
                    print("VPC hasn't been created.")
            sys.exit(1)

    try:
        ssn_conf['vpc_selflink'] = GCPMeta().get_vpc(ssn_conf['vpc_name'])['selfLink']
        if os.environ['gcp_subnet_name'] == '':
            raise KeyError
        else:
            ssn_conf['subnet_name'] = os.environ['gcp_subnet_name']
    except KeyError:
        try:
            pre_defined_subnet = True
            logging.info('[CREATE SUBNET]')
            print('[CREATE SUBNET]')
            params = "--subnet_name {} --region {} --vpc_selflink {} --prefix {} --vpc_cidr {}".\
                format(ssn_conf['subnet_name'], ssn_conf['region'], ssn_conf['vpc_selflink'], ssn_conf['subnet_prefix'],
                       ssn_conf['vpc_cidr'])
            try:
                local("~/scripts/{}.py {}".format('common_create_subnet', params))
                os.environ['gcp_subnet_name'] = ssn_conf['subnet_name']
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to create Subnet.", str(err))
            if pre_defined_vpc:
                try:
                    GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
                except:
                    print("Subnet hasn't been created.")
                GCPActions().remove_vpc(ssn_conf['vpc_name'])
            sys.exit(1)


    try:
        if os.environ['gcp_firewall_name'] == '':
            raise KeyError
        else:
            ssn_conf['firewall_name'] = os.environ['gcp_firewall_name']
    except KeyError:
        try:
            pre_defined_firewall = True
            logging.info('[CREATE FIREWALL]')
            print('[CREATE FIREWALL]')
            firewall_rules = dict()
            firewall_rules['ingress'] = []
            firewall_rules['egress'] = []

            ingress_rule = dict()
            ingress_rule['name'] = ssn_conf['firewall_name'] + '-ingress'
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
            egress_rule['name'] = ssn_conf['firewall_name'] + '-egress'
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
            print('Error: {0}'.format(err))
            append_result("Failed to create Firewall.", str(err))
            if pre_defined_vpc:
                GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
                GCPActions().remove_vpc(ssn_conf['vpc_name'])
            sys.exit(1)

    try:
        logging.info('[CREATE SERVICE ACCOUNT AND ROLE]')
        print('[CREATE SERVICE ACCOUNT AND ROLE]')
        params = "--service_account_name {} --role_name {} --policy_path {} --roles_path {}".format(
            ssn_conf['service_account_name'], ssn_conf['role_name'],
            ssn_conf['ssn_policy_path'], ssn_conf['ssn_roles_path'])
        try:
            local("~/scripts/{}.py {}".format('common_create_service_account', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create Service account and role.", str(err))
        try:
            GCPActions().remove_service_account(ssn_conf['service_account_name'])
            GCPActions().remove_role(ssn_conf['role_name'])
        except:
            print("Service account hasn't been created")
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {}".format(ssn_conf['ssn_bucket_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception

        params = "--bucket_name {}".format(ssn_conf['shared_bucket_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create bucket.", str(err))
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-ingress')
            GCPActions().remove_firewall(ssn_conf['firewall_name'] + '-egress')
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[SET PERMISSIONS FOR SSN BUCKET]')
        print('[SET PERMISSIONS FOR SSN BUCKET]')
        GCPActions().set_bucket_owner(ssn_conf['ssn_bucket_name'], ssn_conf['service_account_name'])
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to set bucket permissions.", str(err))
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
        logging.info('[CREATING STATIC IP ADDRESS]')
        print('[CREATING STATIC IP ADDRESS]')
        params = "--address_name {} --region {}".format(ssn_conf['static_address_name'], ssn_conf['region'])
        try:
            local("~/scripts/{}.py {}".format('ssn_create_static_ip', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create static ip.", str(err))
        try:
            GCPActions().remove_static_address(ssn_conf['static_address_name'], ssn_conf['region'])
        except:
            print("Static IP address hasn't been created.")
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

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    try:
        ssn_conf['static_ip'] = \
            GCPMeta().get_static_address(ssn_conf['region'], ssn_conf['static_address_name'])['address']
        logging.info('[CREATE SSN INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--instance_name {0} --region {1} --zone {2} --vpc_name {3} --subnet_name {4} --instance_size {5}"\
                 " --ssh_key_path {6} --initial_user {7} --service_account_name {8} --image_name {9}"\
                 " --instance_class {10} --static_ip {11} --network_tag {12} --labels '{13}' --primary_disk_size {14}".\
            format(ssn_conf['instance_name'], ssn_conf['region'], ssn_conf['zone'], ssn_conf['vpc_name'],
                   ssn_conf['subnet_name'], ssn_conf['instance_size'], ssn_conf['ssh_key_path'], initial_user,
                   ssn_conf['service_account_name'], ssn_conf['image_name'], 'ssn', ssn_conf['static_ip'],
                   ssn_conf['network_tag'], json.dumps(ssn_conf['instance_labels']), '20')
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create ssn instance.", str(err))
        GCPActions().remove_service_account(ssn_conf['service_account_name'])
        GCPActions().remove_role(ssn_conf['role_name'])
        GCPActions().remove_static_address(ssn_conf['static_address_name'], ssn_conf['region'])
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