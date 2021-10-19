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
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        logging.info('[GETTING AVAILABLE PACKAGES]')
        print('[GETTING AVAILABLE PACKAGES]')
        data_engine = dict()
        try:
            data_engine['os_user'] = os.environ['conf_os_user']
            data_engine['service_base_name'] = os.environ['conf_service_base_name']
            data_engine['group_name'] = os.environ['libCacheKey']
            data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
            data_engine['cluster_name'] = os.environ['computational_id']
            data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
            data_engine['master_ip'] = get_instance_private_ip_address(
                data_engine['tag_name'], data_engine['master_node_name'])
            data_engine['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to get parameter.", str(err))
            sys.exit(1)
        params = "--os_user {} --instance_ip {} --keyfile '{}' --group {}" \
            .format(data_engine['os_user'], data_engine['master_ip'], data_engine['keyfile'], data_engine['group_name'])
        try:
            # Run script to get available libs
            subprocess.run("~/scripts/{}.py {}".format('get_list_available_pkgs', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to get available libraries.", str(err))
        sys.exit(1)
