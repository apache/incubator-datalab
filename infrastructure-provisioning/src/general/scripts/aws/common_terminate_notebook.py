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
import boto3
import uuid


def terminate_nb(nb_tag_value, bucket_name, tag_name):
    print('Terminating EMR cluster and cleaning EMR config from S3 bucket')
    try:
        clusters_list = dlab.meta_lib.get_emr_list(nb_tag_value, 'Value')
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                print('Cleaning bucket from configs for cluster {}'.format(emr_name))
                dlab.actions_lib.s3_cleanup(bucket_name, emr_name, os.environ['project_name'])
                print("The bucket {} has been cleaned successfully".format(bucket_name))
                print('Terminating cluster {}'.format(emr_name))
                dlab.actions_lib.terminate_emr(cluster_id)
                print("The EMR cluster {} has been terminated successfully".format(emr_name))
        else:
            print("There are no EMR clusters to terminate.")
    except:
        sys.exit(1)

    print("Terminating data engine cluster")
    try:
        dlab.actions_lib.remove_ec2('dataengine_notebook_name', nb_tag_value)
    except:
        sys.exit(1)

    print("Terminating notebook")
    try:
        dlab.actions_lib.remove_ec2(tag_name, nb_tag_value)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    dlab.actions_lib.create_aws_config_files()
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['project_name'] = os.environ['project_name']
    notebook_config['endpoint_name'] = os.environ['endpoint_name']
    notebook_config['bucket_name'] = ('{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                  notebook_config['project_name'],
                                                                  notebook_config['endpoint_name']))
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'

    try:
        logging.info('[TERMINATE NOTEBOOK]')
        print('[TERMINATE NOTEBOOK]')
        try:
            terminate_nb(notebook_config['notebook_name'], notebook_config['bucket_name'], notebook_config['tag_name'])
        except Exception as err:
            traceback.print_exc()
            dlab.fab.append_result("Failed to terminate notebook.", str(err))
            raise Exception
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "user_own_bucket_name": notebook_config['bucket_name'],
                   "Action": "Terminate notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
