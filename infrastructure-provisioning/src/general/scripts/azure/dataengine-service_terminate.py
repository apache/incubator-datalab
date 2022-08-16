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


def terminate_hdin_cluster(hdin_name, bucket_name, tag_name, nb_tag_value, ssh_user, key_path):
    logging.info('Terminating hdin cluster and cleaning hdin config from S3 bucket')
    # try:
    #     clusters_list = datalab.meta_lib.get_hdin_list(hdin_name, 'Value')
    #     if clusters_list:
    #         for cluster_id in clusters_list:
    #             computational_name = ''
    #             client = boto3.client('hdin')
    #             cluster = client.describe_cluster(ClusterId=cluster_id)
    #             cluster = cluster.get("Cluster")
    #             hdin_name = cluster.get('Name')
    #             hdin_version = cluster.get('ReleaseLabel')
    #             for tag in cluster.get('Tags'):
    #                 if tag.get('Key') == 'ComputationalName':
    #                     computational_name = tag.get('Value')
    #             datalab.actions_lib.s3_cleanup(bucket_name, hdin_name, os.environ['project_name'])
    #             print("The bucket {} has been cleaned successfully".format(bucket_name))
    #             datalab.actions_lib.terminate_hdin(cluster_id)
    #             print("The hdin cluster {} has been terminated successfully".format(hdin_name))
    #             print("Removing hdin kernels from notebook")
    #             datalab.actions_lib.remove_kernels(hdin_name, tag_name, nb_tag_value, ssh_user, key_path,
    #                                                hdin_version, computational_name)
    #     else:
    #         logging.info("There are no hdin clusters to terminate.")
    except:
        sys.exit(1)


if __name__ == "__main__":
    # generating variables dictionary
    datalab.actions_lib.create_aws_config_files()
    logging.info('Generating infrastructure names and tags')
    hdin_conf = dict()
    hdin_conf['service_base_name'] = (os.environ['conf_service_base_name'])
    hdin_conf['hdin_name'] = os.environ['computational_name']
    hdin_conf['notebook_name'] = os.environ['notebook_instance_name']
    hdin_conf['project_name'] = os.environ['project_name']
    hdin_conf['endpoint_name'] = os.environ['endpoint_name']
    hdin_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(hdin_conf['service_base_name'], hdin_conf['project_name'],
                                                           hdin_conf['endpoint_name']).lower().replace('_', '-')
    hdin_conf['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'
    hdin_conf['tag_name'] = hdin_conf['service_base_name'] + '-tag'

    # try:
    #     logging.info('[TERMINATE hdin CLUSTER]')
    #     try:
    #         terminate_hdin_cluster(hdin_conf['hdin_name'], hdin_conf['bucket_name'], hdin_conf['tag_name'],
    #                               hdin_conf['notebook_name'], os.environ['conf_os_user'], hdin_conf['key_path'])
    #     except Exception as err:
    #         traceback.print_exc()
    #         datalab.fab.append_result("Failed to terminate hdin cluster.", str(err))
    #         raise Exception
    # except:
    #     sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"dataengine-service_name": hdin_conf['hdin_name'],
                   "notebook_name": hdin_conf['notebook_name'],
                   "Action": "Terminate HDInsight cluster"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
