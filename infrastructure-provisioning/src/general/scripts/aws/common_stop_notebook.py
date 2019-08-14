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
from dlab.meta_lib import *
from dlab.actions_lib import *
import boto3
import argparse
import sys


def stop_notebook(nb_tag_value, bucket_name, tag_name, ssh_user, key_path):
    print('Terminating EMR cluster and cleaning EMR config from S3 bucket')
    try:
        clusters_list = get_emr_list(nb_tag_value, 'Value')
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
                s3_cleanup(bucket_name, emr_name, os.environ['project_name'])
                print("The bucket {} has been cleaned successfully".format(bucket_name))
                terminate_emr(cluster_id)
                print("The EMR cluster {} has been terminated successfully".format(emr_name))
                remove_kernels(emr_name, tag_name, nb_tag_value, ssh_user, key_path, emr_version, computational_name)
                print("{} kernels have been removed from notebook successfully".format(emr_name))
        else:
            print("There are no EMR clusters to terminate.")
    except:
        sys.exit(1)

    print("Stopping data engine cluster")
    try:
        cluster_list = []
        master_ids = []
        cluster_instances_list = get_ec2_list('dataengine_notebook_name', nb_tag_value)
        for instance in cluster_instances_list:
            for tag in instance.tags:
                if tag['Key'] == 'Type' and tag['Value'] == 'master':
                    master_ids.append(instance.id)
        for id in master_ids:
            for tag in get_instance_attr(id, 'tags'):
                if tag['Key'] == 'Name':
                    cluster_list.append(tag['Value'].replace(' ', '')[:-2])
        stop_ec2('dataengine_notebook_name', nb_tag_value)
    except:
        sys.exit(1)

    print("Stopping notebook")
    try:
        stop_ec2(tag_name, nb_tag_value)
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
    create_aws_config_files()
    print('Generating infrastructure names and tags')
    notebook_config = dict()
    notebook_config['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['bucket_name'] = (notebook_config['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'
    notebook_config['key_path'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'

    logging.info('[STOP NOTEBOOK]')
    print('[STOP NOTEBOOK]')
    try:
        stop_notebook(notebook_config['notebook_name'], notebook_config['bucket_name'], notebook_config['tag_name'],
                      os.environ['conf_os_user'], notebook_config['key_path'])
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to stop notebook.", str(err))
        sys.exit(1)


    try:
        with open("/root/result.json", 'w') as result:
            res = {"notebook_name": notebook_config['notebook_name'],
                   "Tag_name": notebook_config['tag_name'],
                   "user_own_bucket_name": notebook_config['bucket_name'],
                   "Action": "Stop notebook server"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)

