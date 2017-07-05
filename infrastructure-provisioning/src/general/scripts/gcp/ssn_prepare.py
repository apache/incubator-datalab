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
    print '[DERIVING NAMES]'
    ssn_conf = dict()
    ssn_conf['service_base_name'] = os.environ['conf_service_base_name']
    ssn_conf['region'] = os.environ['region']
    ssn_conf['zone'] = os.environ['zone']
    ssn_conf['ssn_bucket_name'] = (ssn_conf['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    ssn_conf['instance_name'] = ssn_conf['service_base_name'] + '-ssn'
    ssn_conf['vpc_name'] = ssn_conf['service_base_name'] + '-ssn-vpc'
    ssn_conf['subnet_name'] = ssn_conf['service_base_name'] + '-ssn-subnet'
    ssn_conf['subnet_cidr'] = '10.10.1.0/24'
    ssn_conf['firewall_name'] = ssn_conf['service_base_name'] + '-ssn-firewall'

    # service_base_name = os.environ['conf_service_base_name']
    # role_name = service_base_name.lower().replace('-', '_') + '-ssn-Role'
    # role_profile_name = service_base_name.lower().replace('-', '_') + '-ssn-Profile'
    # policy_name = service_base_name.lower().replace('-', '_') + '-ssn-Policy'
    # user_bucket_name = (service_base_name + '-ssn-bucket').lower().replace('_', '-')
    # tag_name = service_base_name + '-Tag'
    # instance_name = service_base_name + '-ssn'
    # region = os.environ['region']
    # ssn_ami_name = os.environ['aws_' + os.environ['conf_os_family'] + '_ami_name']
    # policy_path = '/root/files/ssn_policy.json'
    # vpc_cidr = '10.0.1.0/24'
    # sg_name = instance_name + '-SG'

    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
    except KeyError:
        try:
            pre_defined_vpc = True
            logging.info('[CREATE VPC]')
            print '[CREATE VPC]'
            params = "--vpc_name {}".format(ssn_conf['vpc_name'])
            try:
                local("~/scripts/{}.py {}".format('ssn_create_vpc', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            append_result("Failed to create VPC. Exception:" + str(err))
            if pre_defined_vpc:
                try:
                    GCPActions().remove_vpc(ssn_conf['vpc_name'])
                except:
                    print "VPC hasn't been created."
            sys.exit(1)
    ssn_conf['vpc_selflink'] = GCPMeta().get_vpc(ssn_conf['vpc_name'])['selfLink']

    try:
        if os.environ['gcp_subnet_name'] == '':
            raise KeyError
    except KeyError:
        try:
            pre_defined_subnet = True
            logging.info('[CREATE SUBNET]')
            print '[CREATE SUBNET]'
            params = "--subnet_name {} --region {} --vpc_selflink {} --subnet_cidr {}".\
                format(ssn_conf['subnet_name'], ssn_conf['region'], ssn_conf['vpc_selflink'], ssn_conf['subnet_cidr'])
            try:
                local("~/scripts/{}.py {}".format('common_create_subnet', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            append_result("Failed to create Subnet.", str(err))
            if pre_defined_vpc:
                try:
                    GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
                except:
                    print "Subnet hasn't been created."
                GCPActions().remove_vpc(ssn_conf['vpc_name'])
            sys.exit(1)


    try:
        if os.environ['gcp_firewall_rules'] == '':
            raise KeyError
    except KeyError:
        try:
            pre_defined_firewall = True
            logging.info('[CREATE FIREWALL]')
            print '[CREATE FIREWALL]'
            firewall = {}
            firewall['name'] = ssn_conf['firewall_name']
            firewall['sourceRanges'] = ['0.0.0.0/0']
            rule = {}
            rule['IPProtocol'] = ['tcp', 'icmp']
            rule['ports'] = ['22', '80', '8080', '443']
            firewall['allowed'] = []
            firewall['allowed'].append(rule)
            firewall['network'] = ssn_conf['vpc_selflink']

            params = "--firewall '{}'".format(json.dumps(firewall))
            try:
                local("~/scripts/{}.py '{}'".format('common_create_firewall', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            append_result("Failed to create Firewall.", str(err))
            if pre_defined_vpc:
                GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
                GCPActions().remove_vpc(ssn_conf['vpc_name'])
            sys.exit(1)


    logging.info('[CREATE SSN BUCKET]')
    print '[CREATE SSN BUCKET]'
    GCPActions().create_bucket(ssn_conf['ssn_bucket_name'])

    logging.info('[CREATE SSN INSTANCE]')
    print '[CREATE SSN INSTANCE]'

    ssh_key = open('/root/keys/' + os.environ['conf_key_name'] + '.pem', 'r')

    instance_params = {
          "name": ssn_conf['instance_name'],
          "machineType": "zones/{}/machineTypes/{}".format(ssn_conf['zone'], os.environ['ssn_instance_size']),
          "networkInterfaces": [
            {
              "network": "global/networks/{}".format(ssn_conf['service_base_name']),
              "subnetwork": "regions/{}/subnetworks/{}".format(ssn_conf['region'], ssn_conf['subnet_name']),
              "accessConfigs": [
                {
                  "type": "ONE_TO_ONE_NAT"
                }
              ]
            },
          ],
          "metadata":
            { "items": [
              {
                "key": "ssh-keys",
                "value": "{}:{}".format(os.environ['conf_os_user'], ssh_key.read())
              }
              ]
            },
          "disks": [
            {
              "deviceName": ssn_conf['instance_name'],
              "autoDelete": 'true',
              "initializeParams": {
                "diskSizeGb": "10",
                "sourceImage": "/projects/ubuntu-os-cloud/global/images/ubuntu-1604-xenial-v20170502"
              },
              "boot": 'true',
              "mode": "READ_WRITE"
            }
          ]
        }
    time.sleep(30)
    GCPActions().create_instance(instance_params)