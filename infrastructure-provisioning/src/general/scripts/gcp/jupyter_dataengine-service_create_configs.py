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
from botocore.client import Config
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
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--r_version', type=str, default='')
parser.add_argument('--r_enabled', type=str, default='')
args = parser.parse_args()

dataproc_dir = '/opt/{}/jars/'.format(args.dataproc_version)
kernels_dir = '/home/{}/.local/share/jupyter/kernels/'.format(args.os_user)
spark_dir = '/opt/{}/{}/spark/'.format(args.dataproc_version, args.cluster_name)
yarn_dir = '/opt/{}/{}/conf/'.format(args.dataproc_version, args.cluster_name)


def r_kernel(args):
    spark_path = '/opt/{}/{}/spark/'.format(args.dataproc_version, args.cluster_name)
    local('mkdir -p {}/r_{}/'.format(kernels_dir, args.cluster_name))
    kernel_path = "{}/r_{}/kernel.json".format(kernels_dir, args.cluster_name)
    template_file = "/tmp/r_dataengine-service_template.json"

    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('R_KERNEL_VERSION', 'R-{}'.format(args.r_version))
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.dataproc_version)
    text = text.replace('YARN_CLI_TYPE', 'yarn')
    text = text.replace('SPARK_ACTION', 'session(master = \\\"yarn\\\")')
    with open(kernel_path, 'w') as f:
        f.write(text)


def toree_kernel(args):
    spark_path = '/opt/{0}/{1}/spark/'.format(args.dataproc_version, args.cluster_name)
    scala_version = local('scala -e "println(scala.util.Properties.versionNumberString)"', capture=True)
    local('mkdir -p {0}toree_{1}/'.format(kernels_dir, args.cluster_name))
    local('tar zxvf /tmp/toree_kernel.tar.gz -C {0}toree_{1}/'.format(kernels_dir, args.cluster_name))
    kernel_path = '{0}toree_{1}/kernel.json'.format(kernels_dir, args.cluster_name)
    template_file = "/tmp/toree_dataengine-service_templatev2.json"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('OS_USER', args.os_user)
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.dataproc_version)
    text = text.replace('SCALA_VERSION', scala_version)
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/kernel_var.json')
    local(
        "PYJ=`find /opt/" + args.dataproc_version + "/" + args.cluster_name +
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


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(dataproc_dir, yarn_dir)
        if result == False :
            actions_lib.GCPActions().jars(args, dataproc_dir)
        actions_lib.GCPActions().yarn(args, yarn_dir)
        actions_lib.GCPActions().install_dataproc_spark(args)
        pyspark_kernel(kernels_dir, args.dataproc_version, args.cluster_name, args.spark_version, args.bucket,
                       args.user_name, args.region, args.os_user, args.application, args.pip_mirror)
        toree_kernel(args)
        if args.r_enabled == 'true':
            r_kernel(args)
        actions_lib.GCPActions().spark_defaults(args)
        configuring_notebook(args.dataproc_version)
