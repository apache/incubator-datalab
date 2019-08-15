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

import json
from dlab.fab import *
from dlab.meta_lib import *
import sys, time, os
from dlab.actions_lib import *
import boto3


def terminate_edge_node(tag_name, project_name, tag_value, nb_sg, edge_sg, de_sg, emr_sg):
    print('Terminating EMR cluster')
    try:
        clusters_list = get_emr_list(tag_name)
        if clusters_list:
            for cluster_id in clusters_list:
                client = boto3.client('emr')
                cluster = client.describe_cluster(ClusterId=cluster_id)
                cluster = cluster.get("Cluster")
                emr_name = cluster.get('Name')
                if '{}'.format(tag_value[:-1]) in emr_name:
                    terminate_emr(cluster_id)
                    print("The EMR cluster {} has been terminated successfully".format(emr_name))
        else:
            print("There are no EMR clusters to terminate.")
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Terminating EDGE and notebook instances")
    try:
        remove_ec2(tag_name, tag_value)
    except:
        sys.exit(1)

    print("Removing s3 bucket")
    try:
        remove_s3('edge', project_name)
    except:
        sys.exit(1)

    print("Removing IAM roles and profiles")
    try:
        remove_all_iam_resources('notebook', project_name)
        remove_all_iam_resources('edge', project_name)
    except:
        sys.exit(1)

    print("Removing security groups")
    try:
        remove_sgroups(emr_sg)
        remove_sgroups(de_sg)
        remove_sgroups(nb_sg)
        remove_sgroups(edge_sg)
    except:
        sys.exit(1)

    print("Removing private subnet")
    try:
        remove_subnets(tag_value)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'], os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print('Generating infrastructure names and tags')
    project_conf = dict()
    project_conf['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    project_conf['endpoint_name'] = '{}-{}-endpoint'.format(project_conf['service_base_name'], os.environ['endpoint_name'])
    project_conf['project_name'] = os.environ['project_name']
    project_conf['tag_name'] = project_conf['service_base_name'] + '-Tag'
    project_conf['tag_value'] = project_conf['service_base_name'] + "-" + os.environ['project_name'] + '-*'
    project_conf['edge_sg'] = project_conf['service_base_name'] + "-" + os.environ['project_name'] + '-edge'
    project_conf['nb_sg'] = project_conf['service_base_name'] + "-" + os.environ['project_name'] + '-nb'
    project_conf['edge_instance_name'] = project_conf['service_base_name'] + "-" + os.environ['project_name'] + '-edge'
    project_conf['de_sg'] = project_conf['service_base_name'] + "-" + project_conf['project_name'] + \
                                             '-dataengine*'
    project_conf['emr_sg'] = project_conf['service_base_name'] + "-" + project_conf['project_name'] + '-des-*'

    try:
        logging.info('[TERMINATE PROJECT]')
        print('[TERMINATE PROJECT]')
        try:
            terminate_edge_node(project_conf['tag_name'], project_conf['project_name'], project_conf['tag_value'],
                                project_conf['nb_sg'], project_conf['edge_sg'], project_conf['de_sg'], project_conf['emr_sg'])
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to terminate project.", str(err))
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    try:
        endpoint_id = get_instance_by_name(project_conf['tag_name'], project_conf['endpoint_name'])
        print("Endpoint id: " + endpoint_id)
        ec2 = boto3.client('ec2')
        ec2.delete_tags(Resources=[endpoint_id], Tags=[{'Key': 'project_tag'}, {'Key': 'endpoint_tag'}])
    except Exception as err:
        print("Failed to remove Project tag from Enpoint", str(err))
#        traceback.print_exc()
#        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": project_conf['service_base_name'],
                   "project_name": project_conf['project_name'],
                   "Action": "Terminate edge node"}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except:
        print("Failed writing results.")
        sys.exit(0)
