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

import boto3
import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
import os
import sys
import traceback
from datalab.logger import logging


def terminate_emr_cluster(emr_name, bucket_name, tag_name, nb_tag_value, ssh_user, key_path):
    logging.info('Terminating EMR cluster and cleaning EMR config from S3 bucket')
    try:
        clusters_list = datalab.meta_lib.get_emr_list(emr_name, 'Value')
        if clusters_list:
            for cluster_id in clusters_list:
                computational_name = ''
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                emr_version = cluster.get('ReleaseLabel')
                for tag in cluster.get('Tags'):
                    if tag.get('Key') == 'ComputationalName':
                        computational_name = tag.get('Value')
                datalab.actions_lib.s3_cleanup(bucket_name, emr_name, os.environ['project_name'])
                print("The bucket {} has been cleaned successfully".format(bucket_name))
                datalab.actions_lib.terminate_emr(cluster_id)
                print("The EMR cluster {} has been terminated successfully".format(emr_name))
                print("Removing EMR kernels from notebook")
                datalab.actions_lib.remove_kernels(emr_name, tag_name, nb_tag_value, ssh_user, key_path,
                                                   emr_version, computational_name)
        else:
            logging.info("There are no EMR clusters to terminate.")
    except:
        sys.exit(1)


if __name__ == "__main__":
    # generating variables dictionary
    datalab.actions_lib.create_aws_config_files()
    logging.info('Generating infrastructure names and tags')
    emr_conf = dict()
    emr_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    emr_conf['emr_name'] = os.environ['emr_cluster_name']
    emr_conf['notebook_name'] = os.environ['notebook_instance_name']
    emr_conf['project_name'] = os.environ['project_name']
    emr_conf['endpoint_name'] = os.environ['endpoint_name']
    emr_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(emr_conf['service_base_name'], emr_conf['project_name'],
                                                           emr_conf['endpoint_name']).lower().replace('_', '-')
    emr_conf['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'
    emr_conf['tag_name'] = emr_conf['service_base_name'] + '-tag'

    try:
        logging.info('[TERMINATE EMR CLUSTER]')
        try:
            terminate_emr_cluster(emr_conf['emr_name'], emr_conf['bucket_name'], emr_conf['tag_name'],
                                  emr_conf['notebook_name'], os.environ['conf_os_user'], emr_conf['key_path'])
        except Exception as err:
            traceback.print_exc()
            datalab.fab.append_result("Failed to terminate EMR cluster.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"dataengine-service_name": emr_conf['emr_name'],
                   "notebook_name": emr_conf['notebook_name'],
                   "user_own_bucket_name": emr_conf['bucket_name'],
                   "Action": "Terminate EMR cluster"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
