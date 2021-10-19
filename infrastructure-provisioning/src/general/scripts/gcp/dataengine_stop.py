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

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys
import traceback


def stop_data_engine(zone, cluster_name):
    logging.info("Stopping data engine cluster")
    try:
        instances = GCPMeta.get_list_instances(zone, cluster_name)
        if 'items' in instances:
            for i in instances['items']:
                GCPActions.stop_instance(i['name'], zone)
    except Exception as err:
        datalab.fab.append_result("Failed to stop dataengine", str(err))
        sys.exit(1)


if __name__ == "__main__":
    # generating variables dictionary
    GCPMeta = datalab.meta_lib.GCPMeta()
    GCPActions = datalab.actions_lib.GCPActions()
    logging.info('Generating infrastructure names and tags')
    data_engine = dict()
    if 'exploratory_name' in os.environ:
        data_engine['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-').lower()
    else:
        data_engine['exploratory_name'] = ''
    if 'computational_name' in os.environ:
        data_engine['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
    else:
        data_engine['computational_name'] = ''
    data_engine['service_base_name'] = os.environ['conf_service_base_name']
    data_engine['zone'] = os.environ['gcp_zone']
    data_engine['user_name'] = os.environ['edge_user_name']
    data_engine['project_name'] = os.environ['project_name'].replace('_', '-').lower()
    data_engine['endpoint_name'] = os.environ['endpoint_name'].replace('_', '-').lower()
    data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                          data_engine['project_name'],
                                                          data_engine['endpoint_name'],
                                                          data_engine['computational_name'])
    try:
        logging.info('[STOPPING DATA ENGINE]')
        try:
            stop_data_engine(data_engine['zone'], data_engine['cluster_name'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to stop Data Engine.", str(err))
            raise Exception
    except:
        sys.exit(1)
    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": data_engine['service_base_name'],
                   "Action": "Stop Data Engine"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
