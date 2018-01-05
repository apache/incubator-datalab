#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
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


def terminate_nb(instance_name, bucket_name, region, zone, user_name):
    print('Terminating Dataproc cluster and cleaning Dataproc config from bucket')
    try:
        labels = [
            {instance_name: '*'}
        ]
        clusters_list = meta_lib.GCPMeta().get_dataproc_list(labels)
        if clusters_list:
            for cluster_name in clusters_list:
                actions_lib.GCPActions().bucket_cleanup(bucket_name, user_name, cluster_name)
                print('The bucket {} has been cleaned successfully'.format(bucket_name))
                actions_lib.GCPActions().delete_dataproc_cluster(cluster_name, region)
                print('The Dataproc cluster {} has been terminated successfully'.format(cluster_name))
        else:
            print("There are no Dataproc clusters to terminate.")
    except:
       sys.exit(1)

    print("Terminating data engine cluster")
    try:
        for vm in GCPMeta().get_list_instances(zone)['items']:
            try:
                if instance_name == vm['labels']['notebook_name']:
                    GCPActions().remove_instance(vm['name'], zone)
                    print("Instance {} has been terminated".format(vm['name']))
            except:
                pass
    except:
        sys.exit(1)

    print("Terminating notebook")
    try:
        GCPActions().remove_instance(instance_name, zone)
    except Exception as err:
        append_result("Failed to terminate notebook.", str(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    notebook_config['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['bucket_name'] = '{}-{}-bucket'.format(notebook_config['service_base_name'],
                                                           notebook_config['edge_user_name'])
    notebook_config['gcp_region'] = os.environ['gcp_region']
    notebook_config['gcp_zone'] = os.environ['gcp_zone']

    try:
        logging.info('[TERMINATE NOTEBOOK]')
        print('[TERMINATE NOTEBOOK]')
        try:
            terminate_nb(notebook_config['notebook_name'], notebook_config['bucket_name'],
                         notebook_config['gcp_region'], notebook_config['gcp_zone'],
                         notebook_config['edge_user_name'])
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to terminate notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Action": "Terminate notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)