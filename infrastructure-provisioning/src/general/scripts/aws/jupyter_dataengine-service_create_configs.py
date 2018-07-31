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
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--excluded_lines', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--numpy_version', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--r_enabled', type=str, default='')
args = parser.parse_args()

emr_dir = '/opt/' + args.emr_version + '/jars/'
kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
spark_dir = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/'
yarn_dir = '/opt/' + args.emr_version + '/' + args.cluster_name + '/conf/'


def r_kernel(args):
    spark_path = '/opt/{}/{}/spark/'.format(args.emr_version, args.cluster_name)
    local('mkdir -p {}/r_{}/'.format(kernels_dir, args.cluster_name))
    kernel_path = "{}/r_{}/kernel.json".format(kernels_dir, args.cluster_name)
    template_file = "/tmp/r_dataengine-service_template.json"
    r_version = local("R --version | awk '/version / {print $3}'", capture = True)

    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('R_KERNEL_VERSION', 'R-{}'.format(str(r_version)))
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
    if 'emr-4.' in args.emr_version:
        text = text.replace('YARN_CLI_TYPE', 'yarn-client')
        text = text.replace('SPARK_ACTION', 'init()')
    else:
        text = text.replace('YARN_CLI_TYPE', 'yarn')
        text = text.replace('SPARK_ACTION', 'session(master = \\\"yarn\\\")')
    with open(kernel_path, 'w') as f:
        f.write(text)


def toree_kernel(args):
    spark_path = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/'
    scala_version = local('scala -e "println(scala.util.Properties.versionNumberString)"', capture=True)
    if args.emr_version == 'emr-4.3.0' or args.emr_version == 'emr-4.6.0' or args.emr_version == 'emr-4.8.0':
        local('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/')
        kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/toree_dataengine-service_template.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER_NAME', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH', spark_path)
        text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
        text = text.replace('SCALA_VERSION', scala_version)
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local(
            "PYJ=`find /opt/" + args.emr_version + "/" + args.cluster_name + "/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
        local('sudo mv /tmp/kernel_var.json ' + kernel_path)
    else:
        local('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/')
        local('tar zxvf /tmp/toree_kernel.tar.gz -C ' + kernels_dir + 'toree_' + args.cluster_name + '/')
        kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/toree_dataengine-service_templatev2.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER_NAME', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH', spark_path)
        text = text.replace('OS_USER', args.os_user)
        text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
        text = text.replace('SCALA_VERSION', scala_version)
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local(
            "PYJ=`find /opt/" + args.emr_version + "/" + args.cluster_name +
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


def add_breeze_library_emr(args):
    spark_defaults_path = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-defaults.conf'
    new_jars_directory_path = '/opt/' + args.emr_version + '/jars/usr/other/'
    breeze_tmp_dir = '/tmp/breeze_tmp_emr/'
    local('sudo mkdir -p ' + new_jars_directory_path)
    local('mkdir -p ' + breeze_tmp_dir)
    local('wget http://central.maven.org/maven2/org/scalanlp/breeze_2.11/0.12/breeze_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze_2.11-0.12.jar')
    local('wget http://central.maven.org/maven2/org/scalanlp/breeze-natives_2.11/0.12/breeze-natives_2.11-0.12.jar -O '
          + breeze_tmp_dir + 'breeze-natives_2.11-0.12.jar')
    local('wget http://central.maven.org/maven2/org/scalanlp/breeze-viz_2.11/0.12/breeze-viz_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze-viz_2.11-0.12.jar')
    local('wget http://central.maven.org/maven2/org/scalanlp/breeze-macros_2.11/0.12/breeze-macros_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze-macros_2.11-0.12.jar')
    local('wget http://central.maven.org/maven2/org/scalanlp/breeze-parent_2.11/0.12/breeze-parent_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze-parent_2.11-0.12.jar')
    local('wget http://central.maven.org/maven2/org/jfree/jfreechart/1.0.19/jfreechart-1.0.19.jar -O ' +
          breeze_tmp_dir + 'jfreechart-1.0.19.jar')
    local('wget http://central.maven.org/maven2/org/jfree/jcommon/1.0.24/jcommon-1.0.24.jar -O ' +
          breeze_tmp_dir + 'jcommon-1.0.24.jar')
    local('wget --no-check-certificate https://brunelvis.org/jar/spark-kernel-brunel-all-2.3.jar -O ' +
          breeze_tmp_dir + 'spark-kernel-brunel-all-2.3.jar')
    local('sudo mv ' + breeze_tmp_dir + '* ' + new_jars_directory_path)
    local(""" sudo bash -c "sed -i '/spark.driver.extraClassPath/s/$/:\/opt\/""" + args.emr_version +
          """\/jars\/usr\/other\/*/' """ + spark_defaults_path + """" """)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(emr_dir, yarn_dir)
        if result == False :
            jars(args, emr_dir)
        yarn(args, yarn_dir)
        install_emr_spark(args)
        pyspark_kernel(kernels_dir, args.emr_version, args.cluster_name, args.spark_version, args.bucket,
                       args.user_name, args.region, args.os_user, args.application, args.pip_mirror, args.numpy_version)
        toree_kernel(args)
        if args.r_enabled == 'true':
            r_kernel(args)
        spark_defaults(args)
        configuring_notebook(args.emr_version)
        add_breeze_library_emr(args)
