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

import os, sys, json
from fabric import *
import argparse
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--storage', type=str, default='')
parser.add_argument('--cloud', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--azure_storage_account', type=str, default='')
parser.add_argument('--azure_datalake_account', type=str, default='')
args = parser.parse_args()


def prepare_templates():
    subprocess.run('mv /tmp/jupyter /home/{0}/test_templates'.format(args.os_user), shell=True, check=True)

def get_storage():
    storages = {"aws": args.storage,
                "azure": "{0}@{1}.blob.core.windows.net".format(args.storage, args.azure_storage_account),
                "gcp": args.storage}
    protocols = {"aws": "s3a", "azure": "wasbs", "gcp": "gs"}
    if args.azure_datalake_account:
        storages['azure'] = "{0}.azuredatalakestore.net/{1}".format(args.azure_datalake_account, args.storage)
        protocols['azure'] = 'adl'
    return (storages[args.cloud], protocols[args.cloud])

def prepare_ipynb(kernel_name, template_path, ipynb_name):
    with open(template_path, 'r') as f:
        text = f.read()
    text = text.replace('WORKING_STORAGE', get_storage()[0])
    text = text.replace('PROTOCOL_NAME', get_storage()[1])
    text = text.replace('KERNEL_NAME', kernel_name)
    with open('/home/{}/{}.ipynb'.format(args.os_user, ipynb_name), 'w') as f:
        f.write(text)

def run_ipynb(ipynb_name):
    subprocess.run('jupyter nbconvert --ExecutePreprocessor.timeout=-1 --ExecutePreprocessor.startup_timeout=300 --execute /home/{}/{}.ipynb'.format(args.os_user, ipynb_name), shell=True, check=True)

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

def run_sparkr():
    interpreters = ['ir', 'r_' + args.cluster_name]
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_preparation_sparkr.ipynb'.format(args.os_user),
                      'preparation_sparkr')
        run_ipynb('preparation_sparkr')
        prepare_ipynb(i, '/home/{}/test_templates/template_visualization_sparkr.ipynb'.format(args.os_user),
                      'visualization_sparkr')
        run_ipynb('visualization_sparkr')


if __name__ == "__main__":
    try:
        prepare_templates()
        run_pyspark()
        run_spark()
        run_sparkr()
    except Exception as err:
        print('Error!', str(err))
        sys.exit(1)

    sys.exit(0)