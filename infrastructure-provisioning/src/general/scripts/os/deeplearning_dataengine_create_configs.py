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

from fabric.api import *
import argparse
import os
import sys
import time
from fabric.api import lcd
from fabric.contrib.files import exists
from fabvenv import virtualenv
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
from dlab.common_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='')
parser.add_argument('--spark_configurations', type=str, default='')
args = parser.parse_args()

kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
cluster_dir = '/opt/' + args.cluster_name + '/'
local_jars_dir = '/opt/jars/'

spark_version = args.spark_version
hadoop_version = args.hadoop_version
scala_link = "http://www.scala-lang.org/files/archive/"
spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
             "-bin-hadoop" + hadoop_version + ".tgz"


def pyspark_kernel(args):
    spark_path = '/opt/' + args.cluster_name + '/spark/'
    local('mkdir -p ' + kernels_dir + 'pyspark_' + args.cluster_name + '/')
    kernel_path = kernels_dir + "pyspark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/{}/pyspark_dataengine_template.json".format(args.cluster_name)
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('PYTHON_SHORT_VERSION', '2.7')
    text = text.replace('PYTHON_FULL_VERSION', '2.7')
    text = text.replace('MASTER', args.spark_master)
    text = text.replace('PYTHON_PATH', '/usr/bin/python2.7')
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/{}/kernel_var.json'.format(args.cluster_name))
    local(
        "PYJ=`find /opt/{0}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {1} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{2}/caffe/python:/home/{2}/pytorch/build:\"|\' > /tmp/{0}/kernel_var.json".
        format(args.cluster_name, kernel_path, args.os_user))
    local('sudo mv /tmp/{}/kernel_var.json '.format(args.cluster_name) + kernel_path)

    local('mkdir -p ' + kernels_dir + 'py3spark_' + args.cluster_name + '/')
    kernel_path = kernels_dir + "py3spark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/{}/pyspark_dataengine_template.json".format(args.cluster_name)
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('MASTER', args.spark_master)
    text = text.replace('PYTHON_SHORT_VERSION', '3.6')
    text = text.replace('PYTHON_FULL_VERSION', '3.6')
    text = text.replace('PYTHON_PATH', '/usr/bin/python3.6')
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/{}/kernel_var.json'.format(args.cluster_name))
    local(
        "PYJ=`find /opt/{0}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {1} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{2}/caffe/python:/home/{2}/pytorch/build:\"|\' > /tmp/{0}/kernel_var.json".
        format(args.cluster_name, kernel_path, args.os_user))
    local('sudo mv /tmp/{}/kernel_var.json '.format(args.cluster_name) + kernel_path)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        dataengine_dir_prepare('/opt/{}/'.format(args.cluster_name))
        install_dataengine_spark(args.cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, args.os_user,
                                 args.datalake_enabled)
        ensure_dataengine_tensorflow_jars(local_jars_dir)
        configure_dataengine_spark(args.cluster_name, local_jars_dir, cluster_dir, args.datalake_enabled,
                                   args.spark_configurations)
        pyspark_kernel(args)
