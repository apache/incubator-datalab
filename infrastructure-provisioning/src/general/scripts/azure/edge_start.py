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
from datalab.logger import logging
import os
import sys

if __name__ == "__main__":
    logging.info('Generating infrastructure names and tags')
    AzureMeta = datalab.meta_lib.AzureMeta()
    AzureActions = datalab.actions_lib.AzureActions()
    edge_conf = dict()
    edge_conf['service_base_name'] = os.environ['conf_service_base_name']
    edge_conf['resource_group_name'] = os.environ['azure_resource_group_name']
    edge_conf['project_name'] = os.environ['project_name']
    edge_conf['endpoint_name'] = os.environ['endpoint_name']
    edge_conf['instance_name'] = '{0}-{1}-{2}-edge'.format(edge_conf['service_base_name'],
                                                           edge_conf['project_name'], edge_conf['endpoint_name'])
    edge_conf['instance_dns_name'] = 'host-{}.{}.cloudapp.azure.com'.format(edge_conf['instance_name'],
                                                                            os.environ['azure_region'])

    logging.info('[START EDGE]')
    try:
        AzureActions.start_instance(edge_conf['resource_group_name'], edge_conf['instance_name'])
    except Exception as err:
        datalab.fab.append_result("Failed to start edge.", str(err))
        sys.exit(1)

    try:
        public_ip_address = AzureMeta.get_instance_public_ip_address(edge_conf['resource_group_name'],
                                                                     edge_conf['instance_name'])
        private_ip_address = AzureMeta.get_private_ip_address(edge_conf['resource_group_name'],
                                                              edge_conf['instance_name'])
        logging.info('[SUMMARY]')
        logging.info("Instance name: {}".format(edge_conf['instance_name']))
        logging.info("Hostname: {}".format(edge_conf['instance_dns_name']))
        logging.info("Public IP: {}".format(public_ip_address))
        logging.info("Private IP: {}".format(private_ip_address))
        with open("/root/result.json", 'w') as result:
            res = {"instance_name": edge_conf['instance_name'],
                   "hostname": edge_conf['instance_dns_name'],
                   "public_ip": public_ip_address,
                   "ip": private_ip_address,
                   "Action": "Start up notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
