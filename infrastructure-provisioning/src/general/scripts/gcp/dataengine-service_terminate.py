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

from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import boto3
import argparse
import sys
import os


def terminate_dataproc_cluster(notebook_name, dataproc_name, bucket_name, ssh_user, key_path):
    print('Terminating Dataproc cluster and cleaning Dataproc config from bucket')
    try:
        cluster = meta_lib.GCPMeta().get_list_cluster_statuses([dataproc_name])
        if cluster[0]['status'] == 'running':
            computational_name = meta_lib.GCPMeta().get_cluster(dataproc_name).get('labels').get('computational_name')
            actions_lib.GCPActions().bucket_cleanup(bucket_name, os.environ['project_name'], dataproc_name)
            print('The bucket {} has been cleaned successfully'.format(bucket_name))
            actions_lib.GCPActions().delete_dataproc_cluster(dataproc_name, os.environ['gcp_region'])
            print('The Dataproc cluster {} has been terminated successfully'.format(dataproc_name))
            actions_lib.GCPActions().remove_kernels(notebook_name, dataproc_name, cluster[0]['version'], ssh_user,
                                                    key_path, computational_name)
        else:
            print("There are no Dataproc clusters to terminate.")
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    print('Generating infrastructure names and tags')
    dataproc_conf = dict()
    dataproc_conf['service_base_name'] = os.environ['conf_service_base_name']
    dataproc_conf['edge_user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    dataproc_conf['project_name'] = (os.environ['project_name']).lower().replace('_', '-')
    dataproc_conf['dataproc_name'] = os.environ['dataproc_cluster_name']
    dataproc_conf['gcp_project_id'] = os.environ['gcp_project_id']
    dataproc_conf['gcp_region'] = os.environ['gcp_region']
    dataproc_conf['gcp_zone'] = os.environ['gcp_zone']
    dataproc_conf['notebook_name'] = os.environ['notebook_instance_name']
    dataproc_conf['bucket_name'] = '{}-{}-bucket'.format(dataproc_conf['service_base_name'], dataproc_conf['project_name'])
    dataproc_conf['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])

    try:
        logging.info('[TERMINATE DATAPROC CLUSTER]')
        print('[TERMINATE DATAPROC CLUSTER]')
        try:
            terminate_dataproc_cluster(dataproc_conf['notebook_name'], dataproc_conf['dataproc_name'],
                                       dataproc_conf['bucket_name'], os.environ['conf_os_user'], dataproc_conf['key_path'])
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to terminate Dataproc cluster.", str(err))
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"dataengine-service_name": dataproc_conf['dataproc_name'],
                   "notebook_name": dataproc_conf['notebook_name'],
                   "user_own_bucket_name": dataproc_conf['bucket_name'],
                   "Action": "Terminate Dataproc cluster"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)