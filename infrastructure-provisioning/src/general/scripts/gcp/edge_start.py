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
import sys


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    print('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    edge_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    edge_conf['instance_name'] = '{0}-{1}-edge'.format(edge_conf['service_base_name'], edge_conf['project_name'])
    edge_conf['region'] = os.environ['gcp_region']
    edge_conf['zone'] = os.environ['gcp_zone']
    edge_conf['static_address_name'] = '{0}-{1}-ip'.format(edge_conf['service_base_name'], edge_conf['project_name'])

    logging.info('[START EDGE]')
    print('[START EDGE]')
    try:
        GCPActions().start_instance(edge_conf['instance_name'], edge_conf['zone'])
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to start edge.", str(err))
        sys.exit(1)

    try:
        instance_hostname = GCPMeta().get_instance_public_ip_by_name(edge_conf['instance_name'])
        public_ip_address = \
            GCPMeta().get_static_address(edge_conf['region'], edge_conf['static_address_name'])['address']
        ip_address = GCPMeta().get_private_ip_address(edge_conf['instance_name'])
        print('[SUMMARY]')
        logging.info('[SUMMARY]')
        print("Instance name: {}".format(edge_conf['instance_name']))
        print("Hostname: {}".format(instance_hostname))
        print("Public IP: {}".format(public_ip_address))
        print("Private IP: {}".format(ip_address))
        with open("/root/result.json", 'w') as result:
            res = {"instance_name": edge_conf['instance_name'],
                   "hostname": instance_hostname,
                   "public_ip": public_ip_address,
                   "ip": ip_address,
                   "Action": "Start up notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(1)

