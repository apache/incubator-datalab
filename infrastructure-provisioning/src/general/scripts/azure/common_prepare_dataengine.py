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

import json
import time
from fabric.api import *
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import sys
import os
import uuid
import logging
from Crypto.PublicKey import RSA


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    edge_status = AzureMeta().get_list_instance_statuses(os.environ['conf_service_base_name'],
                                                         [os.environ['conf_service_base_name'] + '-' +
                                                          os.environ['edge_user_name'] + '-edge'])
    if len(edge_status) == 1:
        if edge_status[0]['status'] != 'running':
            logging.info('ERROR: Edge node is unavailable! Aborting...')
            print 'ERROR: Edge node is unavailable! Aborting...'
            ssn_hostname = AzureMeta().get_instance_public_ip_address(os.environ['conf_service_base_name'],
                                                                      os.environ['conf_service_base_name'] + '-ssn')
            put_resource_status('edge', 'Unavailable', os.environ['ssn_dlab_path'], os.environ['conf_os_user'],
                                ssn_hostname)
            append_result("Edge node is unavailable")
            sys.exit(1)
    else:
        append_result("Error with getting Edge instance status")
        sys.exit(1)
    print 'Generating infrastructure names and tags'
    data_engine = dict()
    try:
        data_engine['exploratory_name'] = os.environ['exploratory_name']
    except:
        data_engine['exploratory_name'] = ''
    try:
        data_engine['computational_name'] = os.environ['computational_name']
    except:
        data_engine['computational_name'] = ''
    data_engine['service_base_name'] = os.environ['conf_service_base_name']
    data_engine['resource_group_name'] = os.environ['azure_resource_group_name']
    data_engine['region'] = os.environ['azure_region']
    data_engine['key_name'] = os.environ['conf_key_name']
    data_engine['vpc_name'] = os.environ['azure_vpc_name']
    data_engine['subnet_name'] = os.environ['azure_subnet_name']
    data_engine['private_subnet_name'] = data_engine['service_base_name'] + '-' + os.environ['edge_user_name'] + \
                                         '-subnet'
    data_engine['private_subnet_cidr'] = AzureMeta().get_subnet(data_engine['resource_group_name'],
                                                                data_engine['vpc_name'],
                                                                data_engine['private_subnet_name']).address_prefix
    data_engine['master_security_group_name'] = data_engine['service_base_name'] + '-dataengine-master-sg'
    data_engine['slave_security_group_name'] = data_engine['service_base_name'] + '-dataengine-slave-sg'
    data_engine['master_node_name'] = data_engine['service_base_name'] + '-' + os.environ['edge_user_name'] + \
                                      '-dataengine-' + data_engine['exploratory_name'] + '-' + \
                                      data_engine['computational_name'] + '-master'
    data_engine['slave_node_name'] = data_engine['service_base_name'] + '-' + os.environ['edge_user_name'] + \
                                      '-dataengine-' + data_engine['exploratory_name'] + '-' + \
                                      data_engine['computational_name'] + '-slave'
    data_engine['master_network_interface_name'] = data_engine['master_node_name'] + '-nif'
    data_engine['master_size'] = os.environ['azure_dataengine_master_size']
    ssh_key_path = '/root/keys/' + os.environ['conf_key_name'] + '.pem'
    key = RSA.importKey(open(ssh_key_path, 'rb').read())
    data_engine['public_ssh_key'] = key.publickey().exportKey("OpenSSH")
    data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
    data_engine['slave_size'] = os.environ['azure_dataengine_slave_size']

    logging.info('[CREATING SECURITY GROUPS FOR MASTER NODE]')
    print "[CREATING SECURITY GROUPS FOR MASTER NODE]"
    try:
        list_rules = [
            {
                "name": "in-1",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": data_engine['private_subnet_cidr'],
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 100,
                "direction": "Inbound"
            },
            {
                "name": "in-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": AzureMeta().get_subnet(data_engine['resource_group_name'],
                                                                data_engine['vpc_name'],
                                                                data_engine['subnet_name']).address_prefix,
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 110,
                "direction": "Inbound"
            },
            {
                "name": "in-3",
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
                "destination_address_prefix": data_engine['private_subnet_cidr'],
                "access": "Allow",
                "priority": 100,
                "direction": "Outbound"
            },
            {
                "name": "out-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": AzureMeta().get_subnet(data_engine['resource_group_name'],
                                                                     data_engine['vpc_name'],
                                                                     data_engine['subnet_name']).address_prefix,
                "access": "Allow",
                "priority": 110,
                "direction": "Outbound"
            }
            # {
            #     "name": "out-3",
            #     "protocol": "*",
            #     "source_port_range": "*",
            #     "destination_port_range": "*",
            #     "source_address_prefix": "*",
            #     "destination_address_prefix": "*",
            #     "access": "Deny",
            #     "priority": 300,
            #     "direction": "Outbound"
            # }
        ]
        params = "--resource_group_name {} --security_group_name {} --region {} --list_rules '{}'".format(
            data_engine['resource_group_name'], data_engine['master_security_group_name'], data_engine['region'],
            json.dumps(list_rules))
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            AzureActions().remove_security_group(data_engine['resource_group_name'],
                                                 data_engine['master_security_group_name'])
        except:
            print "Master Security group hasn't been created."
        append_result("Failed to create Security groups. Exception:" + str(err))
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR SLAVE NODES]')
    print "[CREATING SECURITY GROUPS FOR SLAVE NODES]"
    try:
        list_rules = [
            {
                "name": "in-1",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": data_engine['private_subnet_cidr'],
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 100,
                "direction": "Inbound"
            },

            {
                "name": "in-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": AzureMeta().get_subnet(data_engine['resource_group_name'],
                                                                data_engine['vpc_name'],
                                                                data_engine['subnet_name']).address_prefix,
                "destination_address_prefix": "*",
                "access": "Allow",
                "priority": 110,
                "direction": "Inbound"
            },
            {
                "name": "in-3",
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
                "destination_address_prefix": data_engine['private_subnet_cidr'],
                "access": "Allow",
                "priority": 100,
                "direction": "Outbound"
            },
            {
                "name": "out-2",
                "protocol": "*",
                "source_port_range": "*",
                "destination_port_range": "*",
                "source_address_prefix": "*",
                "destination_address_prefix": AzureMeta().get_subnet(data_engine['resource_group_name'],
                                                                     data_engine['vpc_name'],
                                                                     data_engine['subnet_name']).address_prefix,
                "access": "Allow",
                "priority": 110,
                "direction": "Outbound"
            }
            # {
            #     "name": "out-3",
            #     "protocol": "*",
            #     "source_port_range": "*",
            #     "destination_port_range": "*",
            #     "source_address_prefix": "*",
            #     "destination_address_prefix": "*",
            #     "access": "Deny",
            #     "priority": 300,
            #     "direction": "Outbound"
            # }
        ]
        params = "--resource_group_name {} --security_group_name {} --region {} --list_rules '{}'".format(
            data_engine['resource_group_name'], data_engine['slave_security_group_name'], data_engine['region'],
            json.dumps(list_rules))
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        AzureActions().remove_security_group(data_engine['resource_group_name'],
                                             data_engine['master_security_group_name'])
        try:
            AzureActions().remove_security_group(data_engine['resource_group_name'],
                                                 data_engine['slave_security_group_name'])
        except:
            print "Slave Security group hasn't been created."
        append_result("Failed to create Security groups. Exception:" + str(err))
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    try:
        logging.info('[CREATE MASTER NODE]')
        print '[CREATE MASTER NODE]'
        params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} --security_group_name {} --subnet_name {} --service_base_name {} --resource_group_name {} --dlab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} --instance_type {} --user_name {}". \
            format(data_engine['master_node_name'], data_engine['master_size'], data_engine['region'],
                   data_engine['vpc_name'], data_engine['master_network_interface_name'],
                   data_engine['master_security_group_name'], data_engine['private_subnet_name'],
                   data_engine['service_base_name'], data_engine['resource_group_name'], initial_user, 'None',
                   data_engine['public_ssh_key'], '30', 'dataengine', os.environ['edge_user_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            AzureActions().remove_instance(data_engine['resource_group_name'], data_engine['master_node_name'])
        except:
            print "The instance hasn't been created."
        AzureActions().remove_security_group(data_engine['resource_group_name'],
                                             data_engine['master_security_group_name'])
        AzureActions().remove_security_group(data_engine['resource_group_name'],
                                             data_engine['slave_security_group_name'])
        append_result("Failed to create master instance.", str(err))
        sys.exit(1)

    try:
        for i in range(data_engine['instance_count'] - 1):
            logging.info('[CREATE SLAVE NODE {}]'.format(i+1))
            print '[CREATE SLAVE NODE {}]'.format(i+1)
            slave_name = data_engine['slave_node_name'] + '-{}'.format(i+1)
            slave_nif_name = slave_name + '-nif'
            params = "--instance_name {} --instance_size {} --region {} --vpc_name {} --network_interface_name {} --security_group_name {} --subnet_name {} --service_base_name {} --resource_group_name {} --dlab_ssh_user_name {} --public_ip_name {} --public_key '''{}''' --primary_disk_size {} --instance_type {} --user_name {}". \
                format(slave_name, data_engine['slave_size'], data_engine['region'], data_engine['vpc_name'],
                       slave_nif_name, data_engine['slave_security_group_name'], data_engine['private_subnet_name'],
                       data_engine['service_base_name'], data_engine['resource_group_name'], initial_user, 'None',
                       data_engine['public_ssh_key'], '30', 'dataengine', os.environ['edge_user_name'])
            try:
                local("~/scripts/{}.py {}".format('common_create_instance', params))
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        for i in range(data_engine['instance_count'] -1):
            slave_name = data_engine['slave_node_name'] + '-{}'.format(i+1)
            try:
                AzureActions().remove_instance(data_engine['resource_group_name'], slave_name)
            except:
                print "The slave instance {} hasn't been created.".format(slave_name)
        AzureActions().remove_instance(data_engine['resource_group_name'], data_engine['master_node_name'])
        AzureActions().remove_security_group(data_engine['resource_group_name'],
                                             data_engine['master_security_group_name'])
        AzureActions().remove_security_group(data_engine['resource_group_name'],
                                             data_engine['slave_security_group_name'])
        append_result("Failed to create slave instances.", str(err))
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        print '[SUMMARY]'
        print "Service base name: " + data_engine['service_base_name']
        print "Region: " + data_engine['region']
        print "Master node shape: " + data_engine['master_size']
        print "Slave node shape: " + data_engine['slave_size']
        print "Instance count: " + data_engine['instance_count']
        with open("/root/result.json", 'w') as result:
            res = {"hostname": data_engine['service_base_name'],
                   "instance_id": data_engine['master_node_name'],
                   "key_name": data_engine['key_name'],
                   "user_own_bucket_name": 'None',
                   "Action": "Create new Data Engine"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)

    sys.exit(0)
