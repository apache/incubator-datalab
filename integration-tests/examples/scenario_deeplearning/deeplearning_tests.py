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
    try:
        subprocess.run('/bin/bash -c "source /etc/profile && wget http://files.fast.ai/data/dogscats.zip -O /tmp/dogscats.zip"', shell=True, check=True)
        subprocess.run('unzip -q /tmp/dogscats.zip -d /tmp', shell=True, check=True)
        subprocess.run('/bin/bash -c "mkdir -p /home/{0}/{1}"'.format(args.os_user, "{test,train}"), shell=True, check=True)
        subprocess.run('mv /tmp/dogscats/test1/* /home/{0}/test'.format(args.os_user), shell=True, check=True)
        subprocess.run('/bin/bash -c "mv /tmp/dogscats/valid/{0}/* /home/{1}/train"'.format("{cats,dogs}", args.os_user), shell=True, check=True)
        subprocess.run('/bin/bash -c "mv /tmp/dogscats/train/{0}/* /home/{1}/train"'.format("{cats,dogs}", args.os_user), shell=True, check=True)
    except Exception as err:
        print('Failed to download/unpack image dataset!', str(err))
        sys.exit(1)
    subprocess.run('mkdir -p /home/{0}/logs'.format(args.os_user), shell=True, check=True)
    subprocess.run('mv /tmp/deeplearning /home/{0}/test_templates'.format(args.os_user), shell=True, check=True)

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
    text = text.replace('KERNEL_NAME', kernel_name)
    with open('/home/{}/{}.ipynb'.format(args.os_user, ipynb_name), 'w') as f:
        f.write(text)

def run_ipynb(ipynb_name):
    subprocess.run('''bash -l -c 'export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64: ''' \
    '''/usr/lib64/openmpi/lib; jupyter nbconvert --ExecutePreprocessor.timeout=-1 --ExecutePreprocessor.startup_timeout=300 --execute /home/{}/{}.ipynb' '''.format(args.os_user, ipynb_name), shell=True, check=True)

def run_tensor():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_preparation_tensor.ipynb'.format(args.os_user), 'preparation_tensor')
        run_ipynb('preparation_tensor')
        prepare_ipynb(i, '/home/{}/test_templates/template_visualization_tensor.ipynb'.format(args.os_user), 'visualization_tensor')
        run_ipynb('visualization_tensor')

def run_caffe():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_caffe.ipynb'.format(args.os_user), 'test_caffe')
        run_ipynb('test_caffe')

def run_caffe2():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_caffe2.ipynb'.format(args.os_user), 'test_caffe2')
        run_ipynb('test_caffe2')

def run_cntk():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_cntk.ipynb'.format(args.os_user), 'test_cntk')
        run_ipynb('test_cntk')

def run_keras():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_keras.ipynb'.format(args.os_user), 'test_keras')
        run_ipynb('test_keras')

def run_mxnet():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_mxnet.ipynb'.format(args.os_user), 'test_mxnet')
        run_ipynb('test_mxnet')

def run_theano():
    interpreters = ['pyspark_local']
    for i in interpreters:
        prepare_ipynb(i, '/home/{}/test_templates/template_theano.ipynb'.format(args.os_user), 'test_theano')
        run_ipynb('test_theano')

#def run_torch():
#    interpreters = ['itorch']
#    for i in interpreters:
#        prepare_ipynb(i, '/home/{}/test_templates/template_torch.ipynb'.format(args.os_user), 'test_torch')
#        run_ipynb('test_torch')


if __name__ == "__main__":
    try:
        prepare_templates()
        run_tensor()
        run_caffe()
        run_caffe2()
        run_cntk()
        run_keras()
        run_mxnet()
        run_theano()
        #run_torch()
    except Exception as err:
        print('Error!', str(err))
        sys.exit(1)

    sys.exit(0)