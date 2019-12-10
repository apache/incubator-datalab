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

import logging
import json
import os
from dlab.actions_lib import *
import sys


def stop_data_engine(cluster_name):
    print("Stop Data Engine")
    try:
        stop_ec2(os.environ['conf_tag_resource_id'], cluster_name)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'],
                                               os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + \
                         os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print('Generating infrastructure names and tags')
    data_engine_config = dict()
    try:
        data_engine_config['exploratory_name'] = os.environ['exploratory_name']
    except:
        data_engine_config['exploratory_name'] = ''
    try:
        data_engine_config['computational_name'] = os.environ['computational_name']
    except:
        data_engine_config['computational_name'] = ''
    data_engine_config['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    data_engine_config['project_name'] = os.environ['project_name']
    data_engine_config['cluster_name'] = \
        data_engine_config['service_base_name'] + '-' \
        + data_engine_config['project_name'] + '-de-' + \
        data_engine_config['exploratory_name'] + '-' \
        + data_engine_config['computational_name']

    logging.info('[STOP DATA ENGINE CLUSTER]')
    print('[STOP DATA ENGINE CLUSTER]')
    try:
        stop_data_engine("{}:{}".format(data_engine_config['service_base_name'],
                                        data_engine_config['cluster_name']))
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to stop Data Engine.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": data_engine_config['service_base_name'],
                   "Action": "Stop Data Engine"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)
