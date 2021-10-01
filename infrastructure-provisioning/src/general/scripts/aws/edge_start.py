#!/usr/bin/python3

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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
import os
import sys
from datalab.logger import logging

if __name__ == "__main__":
    # generating variables dictionary
    datalab.actions_lib.create_aws_config_files()
    logging.info('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    edge_conf['project_name'] = os.environ['project_name']
    edge_conf['endpoint_name'] = os.environ['endpoint_name']
    edge_conf['instance_name'] = '{0}-{1}-{2}-edge'.format(edge_conf['service_base_name'],
                                                           edge_conf['project_name'], edge_conf['endpoint_name'])
    edge_conf['tag_name'] = edge_conf['service_base_name'] + '-tag'

    logging.info('[START EDGE]')
    try:
        datalab.actions_lib.start_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to start edge.", str(err))
        sys.exit(1)

    try:
        instance_hostname = datalab.meta_lib.get_instance_hostname(edge_conf['tag_name'], edge_conf['instance_name'])
        addresses = datalab.meta_lib.get_instance_ip_address(edge_conf['tag_name'], edge_conf['instance_name'])
        ip_address = addresses.get('Private')
        public_ip_address = addresses.get('Public')
        logging.info('[SUMMARY]')
        logging.info('[SUMMARY]')
        logging.info("Instance name: {}".format(edge_conf['instance_name']))
        logging.info("Hostname: {}".format(instance_hostname))
        logging.info("Public IP: {}".format(public_ip_address))
        logging.info("Private IP: {}".format(ip_address))
        with open("/root/result.json", 'w') as result:
            res = {"instance_name": edge_conf['instance_name'],
                   "hostname": instance_hostname,
                   "public_ip": public_ip_address,
                   "ip": ip_address,
                   "Action": "Start up notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
