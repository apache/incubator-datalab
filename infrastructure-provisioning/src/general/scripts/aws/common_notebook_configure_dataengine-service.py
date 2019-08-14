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
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os
import uuid


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'
    notebook_config['bucket_name'] = (notebook_config['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    notebook_config['cluster_name'] = get_not_configured_emr(notebook_config['tag_name'],
                                                             notebook_config['notebook_name'], True)
    notebook_config['notebook_ip'] = get_instance_ip_address(notebook_config['tag_name'],
                                                             notebook_config['notebook_name']).get('Private')
    notebook_config['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'
    notebook_config['cluster_id'] = get_emr_id_by_name(notebook_config['cluster_name'])
    edge_instance_name = notebook_config['service_base_name'] + "-" + os.environ['project_name'] + '-edge'
    edge_instance_hostname = get_instance_hostname(notebook_config['tag_name'], edge_instance_name)
    if os.environ['application'] == 'deeplearning':
        application = 'jupyter'
    else:
        application = os.environ['application']

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        print('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        params = "--bucket {} --cluster_name {} --emr_version {} --keyfile {} --notebook_ip {} --region {} --emr_excluded_spark_properties {} --project_name {} --os_user {}  --edge_hostname {} --proxy_port {} --scala_version {} --application {} --pip_mirror {}" \
            .format(notebook_config['bucket_name'], notebook_config['cluster_name'], os.environ['emr_version'],
                    notebook_config['key_path'], notebook_config['notebook_ip'], os.environ['aws_region'],
                    os.environ['emr_excluded_spark_properties'], os.environ['project_name'],
                    os.environ['conf_os_user'], edge_instance_hostname, '3128', os.environ['notebook_scala_version'],
                    os.environ['application'], os.environ['conf_pypi_mirror'])
        try:
            local("~/scripts/{}_{}.py {}".format(application, 'install_dataengine-service_kernels', params))
            remove_emr_tag(notebook_config['cluster_id'], ['State'])
            tag_emr_volume(notebook_config['cluster_id'], notebook_config['cluster_name'], os.environ['conf_tag_resource_id'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing EMR kernels.", str(err))
        emr_id = get_emr_id_by_name(notebook_config['cluster_name'])
        terminate_emr(emr_id)
        remove_kernels(notebook_config['cluster_name'], notebook_config['tag_name'], os.environ['notebook_instance_name'],
                       os.environ['conf_os_user'], notebook_config['key_path'], os.environ['emr_version'])
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
            remove_emr_tag(notebook_config['cluster_id'], ['State'])
            tag_emr_volume(notebook_config['cluster_id'], notebook_config['cluster_name'], os.environ['conf_tag_resource_id'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to configure Spark.", str(err))
        emr_id = get_emr_id_by_name(notebook_config['cluster_name'])
        terminate_emr(emr_id)
        remove_kernels(notebook_config['cluster_name'], notebook_config['tag_name'], os.environ['notebook_instance_name'],
                       os.environ['conf_os_user'], notebook_config['key_path'], os.environ['emr_version'])
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "Action": "Configure notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)
