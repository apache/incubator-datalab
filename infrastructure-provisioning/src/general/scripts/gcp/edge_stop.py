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
    GCPMeta = datalab.meta_lib.GCPMeta()
    GCPActions = datalab.actions_lib.GCPActions()
    edge_conf = dict()
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    edge_conf['zone'] = os.environ['gcp_zone']
    edge_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    edge_conf['endpoint_name'] = os.environ['endpoint_name'].replace('_', '-').lower()
    edge_conf['instance_name'] = '{0}-{1}-{2}-edge'.format(edge_conf['service_base_name'],
                                                           edge_conf['project_name'], edge_conf['endpoint_name'])

    logging.info('[STOP EDGE]')
    try:
        GCPActions.stop_instance(edge_conf['instance_name'], edge_conf['zone'])
    except Exception as err:
        datalab.fab.append_result("Failed to stop edge.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"instance_name": edge_conf['instance_name'],
                   "Action": "Stop edge server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
