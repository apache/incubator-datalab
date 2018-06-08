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
parser.add_argument('--r_enabled', type=str, default='')
args = parser.parse_args()

kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
cluster_dir = '/opt/' + args.cluster_name + '/'
local_jars_dir = '/opt/jars/'

spark_version = args.spark_version
hadoop_version = args.hadoop_version
scala_link = "http://www.scala-lang.org/files/archive/"
spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
             "-bin-hadoop" + hadoop_version + ".tgz"


def r_kernel(args):
    spark_path = '/opt/{}/spark/'.format(args.cluster_name)
    local('mkdir -p {}/r_{}/'.format(kernels_dir, args.cluster_name))
    kernel_path = "{}/r_{}/kernel.json".format(kernels_dir, args.cluster_name)
    template_file = "/tmp/r_dataengine_template.json"
    r_version = local("R --version | awk '/version / {print $3}'", capture = True)

    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('R_KERNEL_VERSION', 'R-{}'.format(str(r_version)))
    text = text.replace('SPARK_ACTION', 'init()')
    text = text.replace('MASTER', args.spark_master)
    with open(kernel_path, 'w') as f:
        f.write(text)


def toree_kernel(args):
    spark_path = '/opt/' + args.cluster_name + '/spark/'
    scala_version = local('scala -e "println(scala.util.Properties.versionNumberString)"', capture=True)
    local('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/')
    local('tar zxvf /tmp/toree_kernel.tar.gz -C ' + kernels_dir + 'toree_' + args.cluster_name + '/')
    kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/toree_dataengine_template.json"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('OS_USER', args.os_user)
    text = text.replace('MASTER', args.spark_master)
    text = text.replace('SCALA_VERSION', scala_version)
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/kernel_var.json')
    local(
        "PYJ=`find /opt/" + args.cluster_name +
        "/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat " + kernel_path +
        " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
    local('sudo mv /tmp/kernel_var.json ' + kernel_path)
    run_sh_path = kernels_dir + "toree_" + args.cluster_name + "/bin/run.sh"
    template_sh_file = '/tmp/run_template.sh'
    with open(template_sh_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('OS_USER', args.os_user)
    with open(run_sh_path, 'w') as f:
        f.write(text)


def pyspark_kernel(args):
    spark_path = '/opt/' + args.cluster_name + '/spark/'
    local('mkdir -p ' + kernels_dir + 'pyspark_' + args.cluster_name + '/')
    kernel_path = kernels_dir + "pyspark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/pyspark_dataengine_template.json"
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
    local('touch /tmp/kernel_var.json')
    local(
        "PYJ=`find /opt/{0}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {1} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{2}/caffe/python:/home/{2}/pytorch/build:\"|\' > /tmp/kernel_var.json".
        format(args.cluster_name, kernel_path, args.os_user))
    local('sudo mv /tmp/kernel_var.json ' + kernel_path)

    local('mkdir -p ' + kernels_dir + 'py3spark_' + args.cluster_name + '/')
    kernel_path = kernels_dir + "py3spark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/pyspark_dataengine_template.json"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('MASTER', args.spark_master)
    text = text.replace('PYTHON_SHORT_VERSION', '3.5')
    text = text.replace('PYTHON_FULL_VERSION', '3.5')
    text = text.replace('PYTHON_PATH', '/usr/bin/python3.5')
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/kernel_var.json')
    local(
        "PYJ=`find /opt/{0}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {1} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{2}/caffe/python:/home/{2}/pytorch/build:\"|\' > /tmp/kernel_var.json".
        format(args.cluster_name, kernel_path, args.os_user))
    local('sudo mv /tmp/kernel_var.json ' + kernel_path)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        dataengine_dir_prepare('/opt/{}/'.format(args.cluster_name))
        install_dataengine_spark(spark_link, spark_version, hadoop_version, cluster_dir, args.os_user,
                                 args.datalake_enabled)
        configure_dataengine_spark(local_jars_dir, cluster_dir, args.region, args.datalake_enabled)
        pyspark_kernel(args)
        toree_kernel(args)
        if args.r_enabled == 'true':
            r_kernel(args)
