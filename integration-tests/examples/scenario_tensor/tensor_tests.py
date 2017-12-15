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
    try:
        s3list = s3client.get_paginator('list_objects')
        for result in s3list.paginate(Bucket=bucket, Delimiter='/', Prefix=dist):
            if result.get('CommonPrefixes') is not None:
                for subdir in result.get('CommonPrefixes'):
                    get_files(s3client, s3resource, subdir.get('Prefix'), bucket, local)
            if result.get('Contents') is not None:
                for file in result.get('Contents'):
                    if not os.path.exists(os.path.dirname(local + os.sep + file.get('Key'))):
                        os.makedirs(os.path.dirname(local + os.sep + file.get('Key')))
                        continue
                    s3resource.meta.client.download_file(bucket, file.get('Key'), local + os.sep + file.get('Key'))
    except Exception as err:
        print(str(err))


def prepare_ipynb(kernel_name, template_path, ipynb_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('KERNEL_NAME', kernel_name)
    with open('/home/{}/{}.ipynb'.format(args.os_user, ipynb_name), 'w') as f:
        f.write(text)


def run_ipynb(ipynb_name):
    local('export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64; ' \
            'jupyter nbconvert --ExecutePreprocessor.timeout=-1 --ExecutePreprocessor.startup_timeout=300 --execute /home/{}/{}.ipynb'.format(args.os_user, ipynb_name))


def prepare_templates():
    templates_dir = '/home/{}/'.format(args.os_user)
    s3client = boto3.client('s3', config=Config(signature_version='s3v4'), endpoint_url='https://s3-{}.amazonaws.com'.format(args.region), region_name=args.region)
    s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'), endpoint_url='https://s3-{}.amazonaws.com'.format(args.region), region_name=args.region)
    get_files(s3client, s3resource, 'test_templates_tensor', args.bucket, templates_dir)
    local('mv /home/{0}/test_templates_tensor /home/{0}/test_templates'.format(args.os_user))


def run_tensor():
    interpreters = ['pyspark_local']
    with lcd('/home/{0}/test_templates'.format(args.os_user)):
        local('tar -zxf train.tar.gz -C /home/{0} && tar -zxf test.tar.gz -C /home/{0}'.format(args.os_user))
    local('mkdir -p /home/{}/logs'.format(args.os_user))
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_preparation_tensor.ipynb'.format(args.os_user), 'preparation_tensor')
        run_ipynb('preparation_tensor')
        prepare_ipynb(i, '/home/{}/test_templates/template_visualization_tensor.ipynb'.format(args.os_user), 'visualization_tensor')
        run_ipynb('visualization_tensor')


prepare_templates()
run_tensor()