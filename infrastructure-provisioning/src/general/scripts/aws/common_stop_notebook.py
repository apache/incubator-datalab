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
from fabric import *
from datalab.logger import logging


def stop_notebook(nb_tag_value, bucket_name, tag_name, ssh_user, key_path):
    logging.info('Terminating EMR cluster and cleaning EMR config from S3 bucket')
    try:
        clusters_list = datalab.meta_lib.get_emr_list(nb_tag_value, 'Value')
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
                logging.info("The bucket {} has been cleaned successfully".format(bucket_name))
                datalab.actions_lib.terminate_emr(cluster_id)
                logging.info("The EMR cluster {} has been terminated successfully".format(emr_name))
                datalab.actions_lib.remove_kernels(emr_name, tag_name, nb_tag_value, ssh_user, key_path, emr_version,
                                                   computational_name)
                logging.info("{} kernels have been removed from notebook successfully".format(emr_name))
        else:
            logging.info("There are no EMR clusters to terminate.")
    except:
        sys.exit(1)

    logging.info("Stopping data engine cluster")
    try:
        cluster_list = []
        master_ids = []
        cluster_instances_list = datalab.meta_lib.get_ec2_list('dataengine_notebook_name', nb_tag_value)
        for instance in cluster_instances_list:
            for tag in instance.tags:
                if tag['Key'] == 'Type' and tag['Value'] == 'master':
                    master_ids.append(instance.id)
        for id in master_ids:
            for tag in datalab.meta_lib.get_instance_attr(id, 'tags'):
                if tag['Key'] == 'Name':
                    cluster_list.append(tag['Value'].replace(' ', '')[:-2])
        datalab.actions_lib.stop_ec2('dataengine_notebook_name', nb_tag_value)
    except:
        sys.exit(1)

    logging.info("Stopping notebook")
    try:
        datalab.actions_lib.stop_ec2(tag_name, nb_tag_value)
    except:
        sys.exit(1)


if __name__ == "__main__":
    # generating variables dictionary
    datalab.actions_lib.create_aws_config_files()
    logging.info('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = (os.environ['conf_service_base_name'])
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['project_name'] = os.environ['project_name']
    notebook_config['endpoint_name'] = os.environ['endpoint_name']
    notebook_config['bucket_name'] = '{0}-{1}-{2}-bucket'.format(notebook_config['service_base_name'],
                                                                  notebook_config['project_name'],
                                                                  notebook_config['endpoint_name']
                                                                 ).lower().replace('_', '-')
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-tag'
    notebook_config['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'

    logging.info('[STOP NOTEBOOK]')
    try:
        stop_notebook(notebook_config['notebook_name'], notebook_config['bucket_name'], notebook_config['tag_name'],
                      os.environ['conf_os_user'], notebook_config['key_path'])
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        datalab.fab.append_result("Failed to stop notebook.", str(err))
        sys.exit(1)


    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "user_own_bucket_name": notebook_config['bucket_name'],
                   "Action": "Stop notebook server"}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        sys.exit(1)
