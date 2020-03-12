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
import sys
import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import traceback
import os
import uuid
from fabric.api import *


def clear_resources():
    emr_id = dlab.meta_lib.get_emr_id_by_name(notebook_config['cluster_name'])
    dlab.actions_lib.terminate_emr(emr_id)
    dlab.actions_lib. remove_kernels(notebook_config['cluster_name'], notebook_config['tag_name'],
                                     os.environ['notebook_instance_name'], os.environ['conf_os_user'],
                                     notebook_config['key_path'], os.environ['emr_version'])


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        # generating variables dictionary
        dlab.actions_lib.create_aws_config_files()
        print('Generating infrastructure names and tags')
        notebook_config = dict()
        notebook_config['service_base_name'] = os.environ['conf_service_base_name'] = dlab.fab.replace_multi_symbols(
                os.environ['conf_service_base_name'][:20], '-', True)
        notebook_config['notebook_name'] = os.environ['notebook_instance_name']
        notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
        notebook_config['project_name'] = os.environ['project_name']
        notebook_config['endpoint_name'] = os.environ['endpoint_name']
        notebook_config['bucket_name'] = ('{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                      notebook_config['project_name'],
                                                                      notebook_config['endpoint_name']))
        notebook_config['cluster_name'] = dlab.meta_lib.get_not_configured_emr(notebook_config['tag_name'],
                                                                               notebook_config['notebook_name'], True)
        notebook_config['notebook_ip'] = dlab.meta_lib.get_instance_ip_address(
            notebook_config['tag_name'], notebook_config['notebook_name']).get('Private')
        notebook_config['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'
        notebook_config['cluster_id'] = dlab.meta_lib.get_emr_id_by_name(notebook_config['cluster_name'])
        edge_instance_name = '{}-{}-{}-edge'.format(notebook_config['service_base_name'],
                                                    os.environ['project_name'], os.environ['endpoint_name'])
        edge_instance_hostname = dlab.meta_lib.get_instance_hostname(notebook_config['tag_name'], edge_instance_name)
        if os.environ['application'] == 'deeplearning':
            application = 'jupyter'
        else:
            application = os.environ['application']
    except Exception as err:
        clear_resources()
        dlab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        print('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        params = "--bucket {} --cluster_name {} --emr_version {} --keyfile {} --notebook_ip {} --region {} " \
                 "--emr_excluded_spark_properties {} --project_name {} --os_user {}  --edge_hostname {} " \
                 "--proxy_port {} --scala_version {} --application {} --pip_mirror {}" \
            .format(notebook_config['bucket_name'], notebook_config['cluster_name'], os.environ['emr_version'],
                    notebook_config['key_path'], notebook_config['notebook_ip'], os.environ['aws_region'],
                    os.environ['emr_excluded_spark_properties'], os.environ['project_name'],
                    os.environ['conf_os_user'], edge_instance_hostname, '3128', os.environ['notebook_scala_version'],
                    os.environ['application'], os.environ['conf_pypi_mirror'])
        try:
            local("~/scripts/{}_{}.py {}".format(application, 'install_dataengine-service_kernels', params))
            dlab.actions_lib.remove_emr_tag(notebook_config['cluster_id'], ['State'])
            dlab.actions_lib.tag_emr_volume(notebook_config['cluster_id'], notebook_config['cluster_name'],
                                            os.environ['conf_tag_resource_id'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed installing EMR kernels.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[UPDATING SPARK CONFIGURATION FILES ON NOTEBOOK]')
        print('[UPDATING SPARK CONFIGURATION FILES ON NOTEBOOK]')
        params = "--hostname {0} " \
                 "--keyfile {1} " \
                 "--os_user {2} " \
            .format(notebook_config['notebook_ip'],
                    notebook_config['key_path'],
                    os.environ['conf_os_user'])
        try:
            local("~/scripts/{0}.py {1}".format('common_configure_spark', params))
            dlab.actions_lib.remove_emr_tag(notebook_config['cluster_id'], ['State'])
            dlab.actions_lib.tag_emr_volume(notebook_config['cluster_id'], notebook_config['cluster_name'],
                                            os.environ['conf_tag_resource_id'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        dlab.fab.append_result("Failed to configure Spark.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "Action": "Configure notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        clear_resources()
        sys.exit(1)
