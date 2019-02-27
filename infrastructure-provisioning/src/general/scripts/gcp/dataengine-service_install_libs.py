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

import os
import sys
import logging
import traceback
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
from fabric.api import *
import multiprocessing


def install_libs(instance, data_engine):
    data_engine['instance_ip'] = meta_lib.GCPMeta().get_private_ip_address(instance)
    params = '--os_user {} --instance_ip {} --keyfile "{}" --libs "{}"'\
        .format(data_engine['os_user'], data_engine['instance_ip'],
                data_engine['keyfile'], data_engine['libs'])
    try:
        # Run script to install additional libs
        local("~/scripts/{}.py {}".format('install_additional_libs', params))
    except:
        traceback.print_exc()
        raise Exception


if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        logging.info('[INSTALLING ADDITIONAL LIBRARIES ON DATAENGINE-SERVICE]')
        print('[INSTALLING ADDITIONAL LIBRARIES ON DATAENGINE-SERVICE]')
        data_engine = dict()
        try:
            data_engine['os_user'] = os.environ['conf_os_user']
            data_engine['cluster_name'] = os.environ['computational_id']
            data_engine['gcp_project_id'] = os.environ['gcp_project_id']
            data_engine['gcp_region'] = os.environ['gcp_region']
            data_engine['gcp_zone'] = os.environ['gcp_zone']
            res = meta_lib.GCPMeta().get_list_instances(data_engine['gcp_zone'], data_engine['cluster_name'])
            data_engine['cluster_instances'] = [i.get('name') for i in res['items']]
            data_engine['keyfile'] = '{}{}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
            data_engine['libs'] = os.environ['libs']
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to get parameter.", str(err))
            sys.exit(1)
        try:
            jobs = []
            for instance in data_engine['cluster_instances']:
                p = multiprocessing.Process(target=install_libs, args=(instance, data_engine))
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
