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
import os
import sys
import traceback
import subprocess
from fabric import *
from datalab.logger import logging


def start_data_engine(cluster_name):
    logging.info("Start Data Engine")
    try:
        datalab.actions_lib.start_ec2(os.environ['conf_tag_resource_id'], cluster_name)
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
    datalab.actions_lib.create_aws_config_files()
    logging.info('Generating infrastructure names and tags')
    data_engine = dict()
    
    try:
        data_engine['exploratory_name'] = os.environ['exploratory_name'].lower()
    except:
        data_engine['exploratory_name'] = ''
    try:
        data_engine['computational_name'] = os.environ['computational_name'].lower()
    except:
        data_engine['computational_name'] = ''
    data_engine['service_base_name'] = (os.environ['conf_service_base_name'])
    data_engine['project_name'] = os.environ['project_name']
    data_engine['endpoint_name'] = os.environ['endpoint_name']

    data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                          data_engine['project_name'],
                                                          data_engine['endpoint_name'],
                                                          data_engine['computational_name'])

    logging.info('[START DATA ENGINE CLUSTER]')
    try:
        start_data_engine("{}:{}".format(data_engine['service_base_name'],
                                         data_engine['cluster_name']))
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to start Data Engine.", str(err))
        sys.exit(1)

    try:
        logging.info('[UPDATE LAST ACTIVITY TIME]')
        logging.info('[UPDATE LAST ACTIVITY TIME]')
        data_engine['computational_id'] = data_engine['cluster_name'] + '-m'
        data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
        data_engine['notebook_ip'] = datalab.meta_lib.get_instance_ip_address(
            data_engine['tag_name'], os.environ['notebook_instance_name']).get('Private')
        data_engine['computational_ip'] = datalab.meta_lib.get_instance_ip_address(
            data_engine['tag_name'], data_engine['computational_id']).get('Private')
        data_engine['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        params = '--os_user {0} --notebook_ip {1} --keyfile "{2}" --cluster_ip {3}' \
            .format(os.environ['conf_os_user'], data_engine['notebook_ip'], data_engine['keyfile'],
                    data_engine['computational_ip'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('update_inactivity_on_start', params), shell=True, check=True)
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to update last activity time.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": data_engine['service_base_name'],
                   "Action": "Start Data Engine"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
