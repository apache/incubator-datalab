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
import multiprocessing
import os
import sys
import traceback
import subprocess
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from fabric import *


def install_libs_on_slaves(slave, data_engine):
    slave_name = data_engine['slave_node_name'] + '{}'.format(slave + 1)
    data_engine['slave_ip'] = get_instance_private_ip_address(
        data_engine['tag_name'], slave_name)
    params = '--os_user {} --instance_ip {} --keyfile "{}" --libs "{}"' \
        .format(data_engine['os_user'], data_engine['slave_ip'],
                data_engine['keyfile'], data_engine['libs'])
    try:
        # Run script to install additional libs
        subprocess.run("~/scripts/{}.py {}".format('install_additional_libs', params), shell=True, check=True)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        logging.info('[INSTALLING ADDITIONAL LIBRARIES ON DATAENGINE]')
        print('[INSTALLING ADDITIONAL LIBRARIES ON DATAENGINE]')
        data_engine = dict()
        try:
            data_engine['os_user'] = os.environ['conf_os_user']
            data_engine['service_base_name'] = os.environ['conf_service_base_name']
            data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
            data_engine['cluster_name'] = os.environ['computational_id']
            data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
            data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
            data_engine['master_ip'] = get_instance_private_ip_address(
                data_engine['tag_name'], data_engine['master_node_name'])
            data_engine['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
            data_engine['libs'] = os.environ['libs']
            data_engine['instance_count'] = int(node_count(data_engine['cluster_name']))
        except Exception as err:
            append_result("Failed to get parameter.", str(err))
            sys.exit(1)
        params = '--os_user {} --instance_ip {} --keyfile "{}" --libs "{}"' \
            .format(data_engine['os_user'], data_engine['master_ip'],
                    data_engine['keyfile'], data_engine['libs'])
        try:
            # Run script to install additional libs
            subprocess.run("~/scripts/{}.py {}".format('install_additional_libs', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
        try:
            jobs = []
            for slave in range(data_engine['instance_count'] - 1):
                p = multiprocessing.Process(target=install_libs_on_slaves, args=(slave, data_engine))
                jobs.append(p)
                p.start()
            for job in jobs:
                job.join()
            for job in jobs:
                if job.exitcode != 0:
                    raise Exception
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to install additional libraries.", str(err))
        sys.exit(1)
