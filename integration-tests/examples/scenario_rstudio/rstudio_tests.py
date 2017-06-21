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

from fabric.api import *
import argparse
import boto3
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


def prepare_templates():
    templates_dir = '/home/{}/test_rstudio/'.format(args.os_user)
    local('mkdir -p {}'.format(templates_dir))
    s3client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'))
    get_files(s3client, s3resource, 'test_templates_rstudio', args.bucket, templates_dir)


def prepare_rscript(template_path, rscript_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('S3_BUCKET', args.bucket)
    text = text.replace('MASTER', 'yarn')
    with open('/home/{}/{}.r'.format(args.os_user, rscript_name), 'w') as f:
        f.write(text)


def enable_local_kernel():
    local('sed -i "s/^#//g" /home/{0}/.Renviron | sed -i "/emr/s/^/#/g" /home/{0}/.Renviron'.format(args.os_user))
    local('rm -f metastore_db/db*')


def enable_local_kernel_in_template(template_path, rscript_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('S3_BUCKET', args.bucket)
    text = text.replace('MASTER', 'local[*]')
    with open('/home/{}/{}.r'.format(args.os_user, rscript_name), 'w') as f:
        f.write(text)


def enable_remote_kernel():
    local('sed -i "s/^#//g" /home/{0}/.Renviron | sed -i "/\/opt\/spark\//s/^/#/g" /home/{0}/.Renviron'.
          format(args.os_user))


def run_rscript(rscript_name):
    local('R < ' + rscript_name + '.r --no-save')


prepare_templates()
# Running on remote kernel
prepare_rscript('/home/{}/test_rstudio/template_preparation.r'.format(args.os_user), 'preparation')
run_rscript('preparation')
prepare_rscript('/home/{}/test_rstudio/template_visualization.r'.format(args.os_user), 'visualization')
run_rscript('visualization')
# Running on local kernel
enable_local_kernel()
enable_local_kernel_in_template('/home/{}/test_rstudio/template_preparation.r'.format(args.os_user), 'preparation')
enable_local_kernel_in_template('/home/{}/test_rstudio/template_visualization.r'.format(args.os_user), 'visualization')
run_rscript('preparation')
run_rscript('visualization')

