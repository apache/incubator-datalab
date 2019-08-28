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


import boto3
from botocore.client import Config
import argparse
import re
import time
import sys
from fabric.api import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import json

parser = argparse.ArgumentParser()
# parser.add_argument('--dry_run', action='store_true', help='Print all variables')
parser.add_argument('--region', type=str, help='Region for deploy cluster')
parser.add_argument('--bucket', type=str, help='Bucket for cluster jars')
parser.add_argument('--params', type=str, help='Params to be applied to Cluster ( MANDATORY !!! )')
args = parser.parse_args()


def upload_jars_parser(args):
    if not actions_lib.GCPActions().put_to_bucket(args.bucket, '/root/scripts/dataengine-service_jars_parser.py', 'jars_parser.py'):
        print('Failed to upload jars_parser script')
        raise Exception


def build_dataproc_cluster(args, cluster_name):
    print("Will be created cluster: {}".format(json.dumps(params, sort_keys=True, indent=4, separators=(',', ': '))))
    return actions_lib.GCPActions().create_dataproc_cluster(cluster_name, args.region, params)


def send_parser_job(args, cluster_name, cluster_version):
    job_body = json.loads(open('/root/templates/dataengine-service_job.json').read())
    job_body['job']['placement']['clusterName'] = cluster_name
    job_body['job']['pysparkJob']['mainPythonFileUri'] = 'gs://{}/jars_parser.py'.format(args.bucket)
    job_body['job']['pysparkJob']['args'][1] = args.bucket
    job_body['job']['pysparkJob']['args'][3] = (os.environ['project_name']).lower().replace('_', '-')
    job_body['job']['pysparkJob']['args'][5] = cluster_name
    job_body['job']['pysparkJob']['args'][7] = cluster_version
    job_body['job']['pysparkJob']['args'][9] = os.environ['conf_os_user']
    actions_lib.GCPActions().submit_dataproc_job(job_body)


##############
# Run script #
##############

if __name__ == "__main__":
    parser.print_help()

    params = json.loads(args.params)
    cluster_name = params['clusterName']
    cluster_version = params['config']['softwareConfig']['imageVersion']

    logfile = '{}_creation.log'.format(cluster_name)
    logpath = '/response/' + logfile
    out = open(logpath, 'w')
    out.close()

    upload_jars_parser(args)
    build_dataproc_cluster(args, cluster_name)
    send_parser_job(args, cluster_name, cluster_version)

    sys.exit(0)