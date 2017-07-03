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
    ssn_ami_id = get_ami_id(ssn_ami_name)
    policy_path = '/root/files/ssn_policy.json'
    vpc_cidr = '172.31.0.0/16'
    sg_name = instance_name + '-SG'

    logging.info('[CREATE VPC]')
    print '[CREATE VPC]'
    GcpActions().create_vpc(service_base_name)

    time.sleep(10)
    network_selfLink = GcpMeta().network_get(service_base_name)['selfLink']

    params = {}
    params['name'] = 'ssh22'
    params['sourceRanges'] = ['0.0.0.0/0']
    rule = {}
    rule['IPProtocol'] = 'tcp'
    rule['ports'] = '22'
    params['allowed'] = []
    params['allowed'].append(rule)
    params['network'] = network_selfLink

    logging.info('[CREATE SUBNET]')
    print '[CREATE SUBNET]'
    GcpActions().create_subnet(service_base_name, vpc_cidr, service_base_name, region)

    logging.info('[CREATE FIREWALL]')
    print '[CREATE FIREWALL]'
    GcpActions.firewall_add(params)

    logging.info('[CREATE SSN INSTANCE]')
    print '[CREATE SSN INSTANCE]'

    ssh_key = open('/root/keys/' + os.environ['conf_key_name'] + '.pem', 'r')

    instance_params = {
          "name": instance_name,
          "machineType": "zones/us-central1-c/machineTypes/n1-standard-1",
          "networkInterfaces": [
            {
              "network": "global/networks/dlab",
              "subnetwork": "regions/us-central1/subnetworks/dlabpublic",
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