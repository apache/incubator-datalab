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


def clear_resources():
    emr_id = datalab.meta_lib.get_emr_id_by_name(notebook_config['cluster_name'])
    datalab.actions_lib.terminate_emr(emr_id)
    datalab.actions_lib.remove_kernels(notebook_config['cluster_name'], notebook_config['tag_name'],
                                       os.environ['notebook_instance_name'], os.environ['conf_os_user'],
                                       notebook_config['key_path'], os.environ['emr_version'])


if __name__ == "__main__":
    try:
        # generating variables dictionary
        datalab.actions_lib.create_aws_config_files()
        logging.info('Generating infrastructure names and tags')
        notebook_config = dict()
        notebook_config['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        notebook_config['notebook_name'] = os.environ['notebook_instance_name']
        notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
        notebook_config['project_name'] = os.environ['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name']
        notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                     notebook_config['project_name'],
                                                                     notebook_config['endpoint_name']
                                                                     ).lower().replace('_', '-')
        notebook_config['cluster_name'] = datalab.meta_lib.get_not_configured_emr(notebook_config['tag_name'],
                                                                                  notebook_config['notebook_name'],
                                                                                  True)
        notebook_config['notebook_ip'] = datalab.meta_lib.get_instance_ip_address(
            notebook_config['tag_name'], notebook_config['notebook_name']).get('Private')
        notebook_config['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'
        notebook_config['cluster_id'] = datalab.meta_lib.get_emr_id_by_name(notebook_config['cluster_name'])
        edge_instance_name = '{}-{}-{}-edge'.format(notebook_config['service_base_name'],
                                                    os.environ['project_name'], os.environ['endpoint_name'])
        edge_instance_hostname = datalab.meta_lib.get_instance_hostname(notebook_config['tag_name'], edge_instance_name)
        if os.environ['application'] == 'deeplearning':
            application = 'jupyter'
        else:
            application = os.environ['application']
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        params = "--bucket {} --cluster_name {} --emr_version {} --keyfile {} --notebook_ip {} --region {} " \
                 "--emr_excluded_spark_properties {} --project_name {} --os_user {}  --edge_hostname {} " \
                 "--proxy_port {} --scala_version {} --application {}" \
            .format(notebook_config['bucket_name'], notebook_config['cluster_name'], os.environ['emr_version'],
                    notebook_config['key_path'], notebook_config['notebook_ip'], os.environ['aws_region'],
                    os.environ['emr_excluded_spark_properties'], os.environ['project_name'],
                    os.environ['conf_os_user'], edge_instance_hostname, '3128', os.environ['notebook_scala_version'],
                    os.environ['application'])
        try:
            subprocess.run("~/scripts/{}_{}.py {}".format(application, 'install_dataengine-service_kernels', params), shell=True, check=True)
            datalab.actions_lib.remove_emr_tag(notebook_config['cluster_id'], ['State'])
            datalab.actions_lib.tag_emr_volume(notebook_config['cluster_id'], notebook_config['cluster_name'],
                                               os.environ['conf_tag_resource_id'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing EMR kernels.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[UPDATING SPARK CONFIGURATION FILES ON NOTEBOOK]')
        params = "--hostname {0} " \
                 "--keyfile {1} " \
                 "--os_user {2} " \
            .format(notebook_config['notebook_ip'],
                    notebook_config['key_path'],
                    os.environ['conf_os_user'])
        try:
            subprocess.run("~/scripts/{0}.py {1}".format('common_configure_spark', params), shell=True, check=True)
            datalab.actions_lib.remove_emr_tag(notebook_config['cluster_id'], ['State'])
            datalab.actions_lib.tag_emr_volume(notebook_config['cluster_id'], notebook_config['cluster_name'],
                                               os.environ['conf_tag_resource_id'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure Spark.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "Action": "Configure notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        clear_resources()
        sys.exit(1)
