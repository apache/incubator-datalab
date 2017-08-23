#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

from dlab.fab import *
from dlab.actions_lib import *
import sys, os, json
from fabric.api import *
from dlab.ssn_lib import *
from Crypto.PublicKey import RSA


if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'
    pre_defined_resource_group = False
    pre_defined_vpc = False
    pre_defined_subnet = False
    pre_defined_sg = False
    ssn_conf = dict()
    ssn_conf['service_base_name'] = os.environ['conf_service_base_name']
    ssn_conf['vpc_name'] = os.environ['conf_service_base_name'] + '-ssn-vpc'
    ssn_conf['subnet_name'] = os.environ['conf_service_base_name'] + '-ssn-subnet'
    ssn_conf['region'] = os.environ['azure_region']
    ssn_conf['vpc_cidr'] = '10.10.0.0/16'
    ssn_conf['subnet_prefix'] = '20'
    ssn_conf['storage_account_name'] = (ssn_conf['service_base_name'] + 'ssn').lower().replace('_', '').replace('-', '')
    ssn_conf['ssn_container_name'] = (ssn_conf['service_base_name'] + '-ssn').lower().replace('_', '-')
    ssn_conf['shared_container_name'] = (ssn_conf['service_base_name'] + '-shared').lower().replace('_', '')
    ssn_conf['instance_name'] = ssn_conf['service_base_name'] + '-ssn'
    ssn_conf['network_interface_name'] = ssn_conf['service_base_name'] + '-ssn-nif'
    ssn_conf['static_public_ip_name'] = ssn_conf['service_base_name'] + '-ssn-ip'
    ssn_conf['security_group_name'] = ssn_conf['instance_name'] + '-sg'
    ssh_key_path = '/root/keys/' + os.environ['conf_key_name'] + '.pem'
    key = RSA.importKey(open(ssh_key_path, 'rb').read())
    ssn_conf['public_ssh_key'] = key.publickey().exportKey("OpenSSH")

    try:
        if os.environ['azure_resource_group_name'] == '':
            raise KeyError
    except KeyError:
        pre_defined_resource_group = True
        logging.info('[CREATING RESOURCE GROUP]')
        print "[CREATING RESOURCE GROUP]"
        try:
            params = "--resource_group_name {} --region {}".format(ssn_conf['service_base_name'], ssn_conf['region'])
            try:
                local("~/scripts/{}.py {}".format('ssn_create_resource_group', params))
            except:
                traceback.print_exc()
                raise Exception
            os.environ['azure_resource_group_name'] = ssn_conf['service_base_name']
        except Exception as err:
            try:
                AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
            except:
                print "Resource group hasn't been created."
            append_result("Failed to create Resource Group. Exception:" + str(err))
            sys.exit(1)

    try:
        if os.environ['azure_vpc_name'] == '':
            raise KeyError
    except KeyError:
        pre_defined_vpc = True
        logging.info('[CREATING VIRTUAL NETWORK]')
        print "[CREATING VIRTUAL NETWORK]"
        try:
            params = "--resource_group_name {} --vpc_name {} --region {} --vpc_cidr {}".format(
                ssn_conf['service_base_name'], ssn_conf['vpc_name'], ssn_conf['region'], ssn_conf['vpc_cidr'])
            try:
                local("~/scripts/{}.py {}".format('ssn_create_vpc', params))
            except:
                traceback.print_exc()
                raise Exception
            os.environ['azure_vpc_name'] = ssn_conf['vpc_name']
        except Exception as err:
            if pre_defined_resource_group:
                AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
            try:
                AzureActions().remove_vpc(ssn_conf['service_base_name'], ssn_conf['vpc_name'])
            except:
                print "Virtual Network hasn't been created."
            append_result("Failed to create Virtual Network. Exception:" + str(err))
            sys.exit(1)

    try:
        if os.environ['azure_subnet_name'] == '':
            raise KeyError
    except KeyError:
        pre_defined_vpc = True
        logging.info('[CREATING SUBNET]')
        print "[CREATING SUBNET]"
        try:
            params = "--resource_group_name {} --vpc_name {} --region {} --vpc_cidr {} --subnet_name {} --prefix {}".\
                format(ssn_conf['service_base_name'], ssn_conf['vpc_name'], ssn_conf['region'], ssn_conf['vpc_cidr'],
                       ssn_conf['subnet_name'], ssn_conf['subnet_prefix'])
            try:
                local("~/scripts/{}.py {}".format('common_create_subnet', params))
            except:
                traceback.print_exc()
                raise Exception
            os.environ['azure_subnet_name'] = ssn_conf['subnet_name']
        except Exception as err:
            if pre_defined_resource_group:
                AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
            if pre_defined_vpc:
                AzureActions().remove_vpc(ssn_conf['service_base_name'], ssn_conf['vpc_name'])
            try:
                AzureActions().remove_subnet(ssn_conf['service_base_name'], ssn_conf['vpc_name'],
                                             ssn_conf['subnet_name'])
            except:
                print "Subnet hasn't been created."
            append_result("Failed to create Subnet. Exception:" + str(err))
            sys.exit(1)

    try:
        if os.environ['azure_security_group_name'] == '':
            raise KeyError
    except KeyError:
        pre_defined_sg = True
        logging.info('[CREATING SECURITY GROUPS]')
        print "[CREATING SECURITY GROUPS]"
        try:
            list_rules = [
                {
                    "name": "in-1",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "80",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 100,
                    "direction": "Inbound"
                },
                {
                    "name": "in-2",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "443",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 110,
                    "direction": "Inbound"
                },
                {
                    "name": "in-3",
                    "protocol": "Tcp",
                    "source_port_range": "*",
                    "destination_port_range": "22",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 120,
                    "direction": "Inbound"
                },
                {
                    "name": "in-4",
                    "protocol": "*",
                    "source_port_range": "*",
                    "destination_port_range": "*",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Deny",
                    "priority": 200,
                    "direction": "Inbound"
                },
                {
                    "name": "out-1",
                    "protocol": "*",
                    "source_port_range": "*",
                    "destination_port_range": "*",
                    "source_address_prefix": "*",
                    "destination_address_prefix": "*",
                    "access": "Allow",
                    "priority": 100,
                    "direction": "Outbound"
                }
            ]
            params = "--resource_group_name {} --security_group_name {} --region {} --list_rules '{}'".\
                format(ssn_conf['service_base_name'], ssn_conf['security_group_name'], ssn_conf['region'],
                       json.dumps(list_rules))
            try:
                local("~/scripts/{}.py {}".format('common_create_security_group', params))
            except:
                traceback.print_exc()
                raise Exception
            os.environ['azure_security_group_name'] = ssn_conf['security_group_name']
        except Exception as err:
            if pre_defined_resource_group:
                AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
            if pre_defined_vpc:
                AzureActions().remove_vpc(ssn_conf['service_base_name'], ssn_conf['vpc_name'])
                AzureActions().remove_subnet(ssn_conf['service_base_name'], ssn_conf['vpc_name'], ssn_conf['subnet_name'])
            append_result("Failed to create Security groups. Exception:" + str(err))
            sys.exit(1)

    try:
        logging.info('[CREATE STORAGE ACCOUNT AND CONTAINERS]')
        print('[CREATE STORAGE ACCOUNT AND CONTAINERS]')
        params = "--container_name {} --shared_container_name {} --account_name {} --resource_group_name {} --region {}". \
                 format(ssn_conf['ssn_container_name'], ssn_conf['shared_container_name'],
                        ssn_conf['storage_account_name'], ssn_conf['service_base_name'], ssn_conf['region'])
        try:
            local("~/scripts/{}.py {}".format('common_create_container', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(ssn_conf['service_base_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(ssn_conf['service_base_name'], ssn_conf['vpc_name'], ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(ssn_conf['service_base_name'], ssn_conf['security_group_name'])
        try:
            AzureActions().remove_storage_account(ssn_conf['service_base_name'], ssn_conf['storage_account_name'])
        except:
            print "Storage account hasn't been created."
        append_result("Failed to create storage account and containers. Exception:" + str(err))
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    try:
        logging.info('[CREATE SSN INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} --security_group_name {} --subnet_name {} --service_base_name {} --dlab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} --instance_type {}".\
            format(ssn_conf['instance_name'], os.environ['azure_ssn_instance_size'], ssn_conf['region'], os.environ['azure_vpc_name'],
                   ssn_conf['network_interface_name'], os.environ['azure_security_group_name'], os.environ['azure_subnet_name'],
                   ssn_conf['service_base_name'], initial_user, ssn_conf['static_public_ip_name'],
                   ssn_conf['public_ssh_key'], '30', 'ssn')
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(ssn_conf['service_base_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(ssn_conf['service_base_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(ssn_conf['service_base_name'], ssn_conf['vpc_name'], ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(ssn_conf['service_base_name'], ssn_conf['security_group_name'])
        AzureActions().remove_storage_account(ssn_conf['service_base_name'], ssn_conf['storage_account_name'])
        try:
            AzureActions().remove_instance(ssn_conf['service_base_name'], ssn_conf['instance_name'])
        except:
            print "The instance {} hasn't been created".format(ssn_conf['instance_name'])
        append_result("Failed to create instance. Exception:" + str(err))
        sys.exit(1)