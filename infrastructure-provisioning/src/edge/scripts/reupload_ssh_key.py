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

import logging
import os
import sys
import traceback
import subprocess
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from fabric import *

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        if os.environ['conf_cloud_provider'] == 'aws':
            create_aws_config_files()
        logging.info('[REUPLOADING USER SSH KEY]')
        print('[REUPLOADING USER SSH KEY]')
        reupload_config = dict()
        reupload_config['os_user'] = os.environ['conf_os_user']
        reupload_config['edge_user_name'] = os.environ['edge_user_name']
        reupload_config['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        reupload_config['resource_id'] = os.environ['resource_id']
        reupload_config['additional_config'] = {"user_keyname": reupload_config['edge_user_name'],
                                                "user_keydir": os.environ['conf_key_dir']}
        print(reupload_config)
        try:
            params = "--conf_resource {} --instance_id {} --os_user '{}'" \
                     " --keyfile '{}' --additional_config '{}'".format(
                os.environ['conf_resource'], reupload_config['resource_id'],
                reupload_config['os_user'],  reupload_config['keyfile'],
                json.dumps(reupload_config['additional_config']))
            subprocess.run("~/scripts/{}.py {}".format('common_reupload_key', params), shell=True, check=True)
        except Exception as err:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to reupload user ssh key.", str(err))
        sys.exit(1)

    with open("/root/result.json", 'w') as result:
        res = {"Action": "Reupload user ssh key"}
        result.write(json.dumps(res))