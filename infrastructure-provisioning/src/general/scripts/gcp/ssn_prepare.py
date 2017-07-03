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


if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'
    pre_defined_vpc = False
    pre_defined_subnet = False
    pre_defined_sg = False
    logging.info('[DERIVING NAMES]')
    print '[DERIVING NAMES]'
    service_base_name = os.environ['conf_service_base_name']
    role_name = service_base_name.lower().replace('-', '_') + '-ssn-Role'
    role_profile_name = service_base_name.lower().replace('-', '_') + '-ssn-Profile'
    policy_name = service_base_name.lower().replace('-', '_') + '-ssn-Policy'
    user_bucket_name = (service_base_name + '-ssn-bucket').lower().replace('_', '-')
    tag_name = service_base_name + '-Tag'
    instance_name = service_base_name + '-ssn'
    region = os.environ['region']
    ssn_ami_name = os.environ['aws_' + os.environ['conf_os_family'] + '_ami_name']
    policy_path = '/root/files/ssn_policy.json'
    vpc_cidr = '10.0.1.0/24'
    sg_name = instance_name + '-SG'

    logging.info('[CREATE VPC]')
    print '[CREATE VPC]'
    GCPActions().create_vpc(service_base_name)

    time.sleep(10)
    network_selfLink = GCPMeta().network_get(service_base_name)['selfLink']

    subnet_name = service_base_name + '-ssn-subnet'
    public_net_cidr = '10.0.1.0/24'
    time.sleep(10)

    logging.info('[CREATE SUBNET]')
    print '[CREATE SUBNET]'
    GCPActions().create_subnet(subnet_name, vpc_cidr, network_selfLink, region)

    params = {}
    params['name'] = service_base_name + '-ssn-firewall'
    params['sourceRanges'] = ['0.0.0.0/0']
    rule = {}
    rule['IPProtocol'] = 'tcp'
    rule['ports'] = '22'
    params['allowed'] = []
    params['allowed'].append(rule)
    params['network'] = network_selfLink

    logging.info('[CREATE FIREWALL]')
    print '[CREATE FIREWALL]'
    GCPActions().firewall_add(params)

    logging.info('[CREATE SSN INSTANCE]')
    print '[CREATE SSN INSTANCE]'

    ssh_key = open('/root/keys/' + os.environ['conf_key_name'] + '.pem', 'r')

    instance_params = {
          "name": instance_name,
          "machineType": "zones/{}/machineTypes/{}".format(os.environ['zone'], os.environ['ssn_instance_size']),
          "networkInterfaces": [
            {
              "network": "global/networks/{}".format(service_base_name),
              "subnetwork": "regions/{}/subnetworks/{}".format(os.environ['region'], subnet_name),
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
              "deviceName": instance_name,
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
    GCPActions().create_instance(instance_params)