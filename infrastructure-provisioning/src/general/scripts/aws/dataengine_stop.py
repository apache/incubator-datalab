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

import logging
import json
import os
from dlab.actions_lib import *
import boto3
import sys
import uuid


def stop_dataengine(tag_name, nb_tag_value):
    print("Stop Data Engine")
    try:
        stop_ec2(tag_name, nb_tag_value)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print('Generating infrastructure names and tags')
    data_engine_config = dict()
    data_engine_config['notebook_name'] = os.environ['notebook_instance_name']
    data_engine_config['tag_name'] = 'dataengine_notebook_name'
    data_engine_config['service_base_name'] = os.environ['conf_service_base_name']

    logging.info('[STOP DATA ENGINE CLUSTER]')
    print('[STOP DATA ENGINE CLUSTER]')
    try:
        stop_dataengine(data_engine_config['tag_name'], data_engine_config['notebook_name'])
    except Exception as err:
        append_result("Failed to stop notebook.", str(err))
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
