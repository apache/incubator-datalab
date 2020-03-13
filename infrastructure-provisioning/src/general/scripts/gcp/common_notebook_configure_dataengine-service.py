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
import os
import traceback
import uuid
from fabric.api import *


def clear_resources():
    GCPActions.delete_dataproc_cluster(notebook_config['cluster_name'], os.environ['gcp_region'])
    GCPActions.remove_kernels(notebook_config['notebook_name'], notebook_config['cluster_name'],
                              os.environ['dataproc_version'], os.environ['conf_os_user'],
                              notebook_config['key_path'])


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    GCPMeta = dlab.meta_lib.GCPMeta()
    GCPActions = dlab.actions_lib.GCPActions()
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['edge_user_name'] = (os.environ['edge_user_name'])
    notebook_config['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    notebook_config['project_tag'] = notebook_config['project_name']
    notebook_config['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
    notebook_config['endpoint_tag'] = notebook_config['endpoint_name']
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
    notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                 notebook_config['project_name'],
                                                                 notebook_config['endpoint_name'])
    notebook_config['cluster_name'] = GCPMeta.get_not_configured_dataproc(notebook_config['notebook_name'])
    notebook_config['notebook_ip'] = GCPMeta.get_private_ip_address(notebook_config['notebook_name'])
    notebook_config['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    edge_instance_name = '{0}-{1}-{2}-edge'.format(notebook_config['service_base_name'],
                                                   notebook_config['project_name'], notebook_config['endpoint_tag'])
    edge_instance_hostname = GCPMeta.get_private_ip_address(edge_instance_name)
    if os.environ['application'] == 'deeplearning':
        application = 'jupyter'
    else:
        application = os.environ['application']

    additional_tags = os.environ['tags'].replace("': u'", ":").replace("', u'", ",").replace("{u'", "" ).replace(
        "'}", "").lower()

    notebook_config['cluster_labels'] = {
        os.environ['notebook_instance_name']: "configured",
        "name": notebook_config['cluster_name'],
        "sbn": notebook_config['service_base_name'],
        "notebook_name": os.environ['notebook_instance_name'],
        "product": "dlab",
        "computational_name": (os.environ['computational_name'].replace('_', '-').lower())
    }

    for tag in additional_tags.split(','):
        label_key = tag.split(':')[0]
        label_value = tag.split(':')[1].replace('_', '-')
        if '@' in label_value:
            label_value = label_value[:label_value.find('@')]
        if label_value != '':
            notebook_config['cluster_labels'].update({label_key: label_value})

    try:
        logging.info('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        print('[INSTALLING KERNELS INTO SPECIFIED NOTEBOOK]')
        params = "--bucket {} --cluster_name {} --dataproc_version {} --keyfile {} --notebook_ip {} --region {} " \
                 "--edge_user_name {} --project_name {} --os_user {}  --edge_hostname {} --proxy_port {} " \
                 "--scala_version {} --application {} --pip_mirror {}" \
            .format(notebook_config['bucket_name'], notebook_config['cluster_name'], os.environ['dataproc_version'],
                    notebook_config['key_path'], notebook_config['notebook_ip'], os.environ['gcp_region'],
                    notebook_config['edge_user_name'], notebook_config['project_name'], os.environ['conf_os_user'],
                    edge_instance_hostname, '3128', os.environ['notebook_scala_version'], os.environ['application'],
                    os.environ['conf_pypi_mirror'])
        try:
            local("~/scripts/{}_{}.py {}".format(application, 'install_dataengine-service_kernels', params))
            GCPActions.update_dataproc_cluster(notebook_config['cluster_name'], notebook_config['cluster_labels'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        dlab.fab.append_result("Failed installing Dataproc kernels.", str(err))
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
