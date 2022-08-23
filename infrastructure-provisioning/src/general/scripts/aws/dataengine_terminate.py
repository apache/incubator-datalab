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
import json
import os
import sys
import traceback
from datalab.logger import logging


def terminate_data_engine(tag_name, notebook_name,
                          os_user, key_path,
                          cluster_name, remote_kernel_name):
    logging.info("Terminating data engine cluster")
    try:
        datalab.actions_lib.remove_ec2(os.environ['conf_tag_resource_id'], cluster_name)
    except:
        sys.exit(1)

    logging.info("Removing Data Engine kernels from notebook")
    try:
        datalab.actions_lib.remove_dataengine_kernels(tag_name, notebook_name,
                                                      os_user, key_path, remote_kernel_name)
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
    logging.info('Generating infrastructure names and tags')
    datalab.actions_lib.create_aws_config_files()
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
    data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
    data_engine['project_name'] = os.environ['project_name']
    data_engine['endpoint_name'] = os.environ['endpoint_name']
    data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                          data_engine['project_name'],
                                                          data_engine['endpoint_name'],
                                                          data_engine['computational_name'])
    data_engine['notebook_name'] = os.environ['notebook_instance_name']
    data_engine['key_path'] = "{}/{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])

    try:
        logging.info('[TERMINATE DATA ENGINE]')
        try:
            terminate_data_engine(data_engine['tag_name'],
                                  data_engine['notebook_name'],
                                  os.environ['conf_os_user'],
                                  data_engine['key_path'], "{}:{}".format(
                    data_engine['service_base_name'],
                    data_engine['cluster_name']), data_engine['cluster_name'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to terminate Data Engine.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": data_engine['service_base_name'],
                   "Action": "Terminate Data Engine"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
