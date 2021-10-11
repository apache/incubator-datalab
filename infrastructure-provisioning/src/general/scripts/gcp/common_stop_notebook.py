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
from datalab.logger import logging
import os
import sys


def stop_notebook(instance_name, bucket_name, region, zone, ssh_user, key_path, project_name):
    logging.info('Terminating Dataproc cluster and cleaning Dataproc config from bucket')
    try:
        labels = [
            {instance_name: '*'}
        ]
        clusters_list = GCPMeta.get_dataproc_list(labels)
        if clusters_list:
            for cluster_name in clusters_list:
                computational_name = GCPMeta.get_cluster(cluster_name).get('labels').get(
                    'computational_name')
                cluster = GCPMeta.get_list_cluster_statuses([cluster_name])
                GCPActions.bucket_cleanup(bucket_name, project_name, cluster_name)
                logging.info('The bucket {} has been cleaned successfully'.format(bucket_name))
                GCPActions.delete_dataproc_cluster(cluster_name, region)
                logging.info('The Dataproc cluster {} has been terminated successfully'.format(cluster_name))
                GCPActions.remove_kernels(instance_name, cluster_name, cluster[0]['version'], ssh_user,
                                          key_path, computational_name)
        else:
            logging.info("There are no Dataproc clusters to terminate.")
    except Exception as err:
        datalab.fab.append_result("Failed to terminate dataproc", str(err))
        sys.exit(1)

    logging.info("Stopping data engine cluster")
    try:
        clusters_list = GCPMeta.get_list_instances_by_label(zone, instance_name)
        if clusters_list.get('items'):
            for vm in clusters_list['items']:
                try:
                    GCPActions.stop_instance(vm['name'], zone)
                    logging.info("Instance {} has been stopped".format(vm['name']))
                except:
                    pass
        else:
            logging.info("There are no data engine clusters to terminate.")

    except Exception as err:
        datalab.fab.append_result("Failed to stop dataengine cluster", str(err))
        sys.exit(1)

    logging.info("Stopping notebook")
    try:
        GCPActions.stop_instance(instance_name, zone)
    except Exception as err:
        datalab.fab.append_result("Failed to stop instance", str(err))
        sys.exit(1)


if __name__ == "__main__":
    # generating variables dictionary
    GCPMeta = datalab.meta_lib.GCPMeta()
    GCPActions = datalab.actions_lib.GCPActions()
    logging.info('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['edge_user_name'] = (os.environ['edge_user_name'])
    notebook_config['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    notebook_config['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                 notebook_config['project_name'],
                                                                 notebook_config['endpoint_name'])
    notebook_config['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
    notebook_config['gcp_region'] = os.environ['gcp_region']
    notebook_config['gcp_zone'] = os.environ['gcp_zone']

    logging.info('[STOP NOTEBOOK]')
    try:
        stop_notebook(notebook_config['notebook_name'], notebook_config['bucket_name'],
                      notebook_config['gcp_region'], notebook_config['gcp_zone'],
                      os.environ['conf_os_user'], notebook_config['key_path'],
                      notebook_config['project_name'])
    except Exception as err:
        logging.info('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to stop notebook.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Stop notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
