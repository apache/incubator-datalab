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


def terminate_nb(instance_name, bucket_name, region, zone, user_name):
    print('Terminating Dataproc cluster and cleaning Dataproc config from bucket')
    try:
        labels = [
            {instance_name: '*'}
        ]
        clusters_list = GCPMeta.get_dataproc_list(labels)
        if clusters_list:
            for cluster_name in clusters_list:
                GCPActions.bucket_cleanup(bucket_name, user_name, cluster_name)
                print('The bucket {} has been cleaned successfully'.format(bucket_name))
                GCPActions.delete_dataproc_cluster(cluster_name, region)
                print('The Dataproc cluster {} has been terminated successfully'.format(cluster_name))
        else:
            print("There are no Dataproc clusters to terminate.")
    except Exception as err:
        dlab.fab.append_result("Failed to terminate dataproc", str(err))
        sys.exit(1)

    print("Terminating data engine cluster")
    try:
        clusters_list = GCPMeta.get_list_instances_by_label(zone, instance_name)
        if clusters_list.get('items'):
            for vm in clusters_list['items']:
                try:
                    GCPActions().remove_instance(vm['name'], zone)
                    print("Instance {} has been terminated".format(vm['name']))
                except:
                    pass
        else:
            print("There are no data engine clusters to terminate.")

    except Exception as err:
        dlab.fab.append_result("Failed to terminate dataengine", str(err))
        sys.exit(1)

    print("Terminating notebook")
    try:
        GCPActions.remove_instance(instance_name, zone)
    except Exception as err:
        dlab.fab.append_result("Failed to terminate instance", str(err))
        sys.exit(1)


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
    notebook_config['edge_user_name'] = (os.environ['edge_user_name'])
    notebook_config['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
    notebook_config['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                 notebook_config['project_name'],
                                                                 notebook_config['endpoint_name'])
    notebook_config['gcp_region'] = os.environ['gcp_region']
    notebook_config['gcp_zone'] = os.environ['gcp_zone']

    try:
        logging.info('[TERMINATE NOTEBOOK]')
        print('[TERMINATE NOTEBOOK]')
        try:
            terminate_nb(notebook_config['notebook_name'], notebook_config['bucket_name'],
                         notebook_config['gcp_region'], notebook_config['gcp_zone'],
                         notebook_config['project_name'])
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to terminate notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Terminate notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
