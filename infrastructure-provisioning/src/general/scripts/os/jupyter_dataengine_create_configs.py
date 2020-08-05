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
    template_file = "/tmp/{}/r_dataengine_template.json".format(args.cluster_name)
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
    scala_version = local('spark-submit --version 2>&1 | grep -o -P "Scala version \K.{0,7}"', capture=True)
    local('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/')
    local('tar zxvf /tmp/{}/toree_kernel.tar.gz -C '.format(args.cluster_name) + kernels_dir + 'toree_' + args.cluster_name + '/')
    local('sudo mv {0}toree_{1}/toree-0.3.0-incubating/* {0}toree_{1}/'.format(kernels_dir, args.cluster_name))
    local('sudo rm -r {0}toree_{1}/toree-0.3.0-incubating'.format(kernels_dir, args.cluster_name))
    kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/{}/toree_dataengine_template.json".format(args.cluster_name)
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
    local('touch /tmp/{}/kernel_var.json'.format(args.cluster_name))
    local(
        "PYJ=`find /opt/" + args.cluster_name +
        "/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat " + kernel_path +
        " | sed 's|PY4J|'$PYJ'|g' > /tmp/{}/kernel_var.json".format(args.cluster_name))
    local('sudo mv /tmp/{}/kernel_var.json '.format(args.cluster_name) + kernel_path)
    run_sh_path = kernels_dir + "toree_" + args.cluster_name + "/bin/run.sh"
    template_sh_file = '/tmp/{}/run_template.sh'.format(args.cluster_name)
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

def install_sparkamagic_kernels(args):
    try:
        local('sudo jupyter nbextension enable --py --sys-prefix widgetsnbextension')
        sparkmagic_dir = local("sudo pip3 show sparkmagic | grep 'Location: ' | awk '{print $2}'", capture=True)
        local('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --user'.format(sparkmagic_dir))
        local('sudo jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --user'.format(sparkmagic_dir))
        local('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkrkernel --user'.format(sparkmagic_dir))
        pyspark_kernel_name = 'PySpark (Python-3.6 / Spark-{0} ) [{1}]'.format(args.spark_version,
                                                                         args.cluster_name)
        local('sed -i \'s|PySpark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/pysparkkernel/kernel.json'.format(
            pyspark_kernel_name, args.os_user))
        scala_version = local('spark-submit --version 2>&1 | grep -o -P "Scala version \K.{0,7}"', capture=True)
        spark_kernel_name = 'Spark (Scala-{0} / Spark-{1} ) [{2}]'.format(scala_version, args.spark_version,
                                                                         args.cluster_name)
        local('sed -i \'s|Spark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkkernel/kernel.json'.format(
            spark_kernel_name, args.os_user))
        r_version = local("R --version | awk '/version / {print $3}'", capture=True)
        sparkr_kernel_name = 'SparkR (R-{0} / Spark-{1} ) [{2}]'.format(str(r_version), args.spark_version,
                                                                            args.cluster_name)
        local('sed -i \'s|SparkR|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkrkernel/kernel.json'.format(
            sparkr_kernel_name, args.os_user))
        local('sudo mv -f /home/{0}/.local/share/jupyter/kernels/pysparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/pysparkkernel_{1}'.format(args.os_user, args.cluster_name))
        local('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/sparkkernel_{1}'.format(args.os_user, args.cluster_name))
        local('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkrkernel '
              '/home/{0}/.local/share/jupyter/kernels/sparkrkernel_{1}'.format(args.os_user, args.cluster_name))
        local('mkdir -p /home/' + args.os_user + '/.sparkmagic')
        local('cp -f /tmp/sparkmagic_config_template.json /home/' + args.os_user + '/.sparkmagic/config.json')
        spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
        local('sed -i \'s|LIVY_HOST|{0}|g\' /home/{1}/.sparkmagic/config.json'.format(
                spark_master_ip, args.os_user))
        local('sudo chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user))
    except:
        sys.exit(1)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        install_sparkamagic_kernels(args)
        #dataengine_dir_prepare('/opt/{}/'.format(args.cluster_name))
        #install_dataengine_spark(args.cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, args.os_user,
        #                         args.datalake_enabled)
        #configure_dataengine_spark(args.cluster_name, local_jars_dir, cluster_dir, args.datalake_enabled,
        #                           args.spark_configurations)
        #pyspark_kernel(args)
        #toree_kernel(args)
        #if args.r_enabled == 'true':
        #    r_kernel(args)
