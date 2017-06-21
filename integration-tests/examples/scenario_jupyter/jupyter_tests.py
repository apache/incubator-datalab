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

import boto3
from fabric.api import *
import uuid
import argparse
from botocore.client import Config
import os

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


def get_files(s3client, s3resource, dist, bucket, local):
    s3list = s3client.get_paginator('list_objects')
    for result in s3list.paginate(Bucket=bucket, Delimiter='/', Prefix=dist):
        if result.get('CommonPrefixes') is not None:
            for subdir in result.get('CommonPrefixes'):
                get_files(s3client, s3resource, subdir.get('Prefix'), bucket, local)
        if result.get('Contents') is not None:
            for file in result.get('Contents'):
                if not os.path.exists(os.path.dirname(local + os.sep + file.get('Key'))):
                    os.makedirs(os.path.dirname(local + os.sep + file.get('Key')))
                s3resource.meta.client.download_file(bucket, file.get('Key'), local + os.sep + file.get('Key'))


def prepare_ipynb(kernel_name, template_path, ipynb_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('S3_BUCKET', args.bucket)
    text = text.replace('KERNEL_NAME', kernel_name)
    with open('/home/{}/{}.ipynb'.format(args.os_user, ipynb_name), 'w') as f:
        f.write(text)


def run_ipynb(ipynb_name):
    local('jupyter nbconvert --ExecutePreprocessor.timeout=-1 --execute /home/{}/{}.ipynb'.format(args.os_user, ipynb_name))


def prepare_templates():
    templates_dir = '/home/{}/'.format(args.os_user)
    local('mkdir -p {}'.format(templates_dir))
    s3client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'))
    get_files(s3client, s3resource, 'test_templates_jupyter', args.bucket, templates_dir)
    local('mv /home/{0}/test_templates_jupyter /home/{0}/test_templates'.format(args.os_user))


def run_pyspark():
    interpreters = ['pyspark_local', 'pyspark_' + args.cluster_name]
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_preparation_pyspark.ipynb'.format(args.os_user),
                      'preparation_pyspark')
        run_ipynb('preparation_pyspark')
        prepare_ipynb(i, '/home/{}/test_templates/template_visualization_pyspark.ipynb'.format(args.os_user),
                      'visualization_pyspark')
        run_ipynb('visualization_pyspark')


def run_spark():
    interpreters = ['apache_toree_scala', 'toree_' + args.cluster_name]
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_preparation_spark.ipynb'.format(args.os_user),
                      'preparation_spark')
        run_ipynb('preparation_spark')
        # prepare_ipynb(i, '/home/{}/test_templates/template_visualization_spark.ipynb'.format(args.os_user),
        #               'visualization_spark')
        # run_ipynb('visualization_spark')


def run_sparkr():
    interpreters = ['ir', 'r_' + args.cluster_name]
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_preparation_sparkr.ipynb'.format(args.os_user),
                      'preparation_sparkr')
        run_ipynb('preparation_sparkr')
        prepare_ipynb(i, '/home/{}/test_templates/template_visualization_sparkr.ipynb'.format(args.os_user),
                      'visualization_sparkr')
        run_ipynb('visualization_sparkr')


prepare_templates()
run_pyspark()
run_spark()
run_sparkr()
