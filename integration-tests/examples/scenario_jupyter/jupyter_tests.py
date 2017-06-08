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

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


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
    local('aws s3 cp --recursive s3://' + args.bucket + '/test_templates_jupyter/ /home/{}/test_templates/'.
          format(args.os_user))


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
