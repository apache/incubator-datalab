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

import argparse
import json
import sys
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
import os

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--r_mirror', type=str, default='')
args = parser.parse_args()

spark_version = args.spark_version
hadoop_version = args.hadoop_version
jupyter_version = os.environ['notebook_jupyter_version']
scala_link = "http://www.scala-lang.org/files/archive/"
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "http://d3kbcqa49mib13.cloudfront.net/spark-" + spark_version + "-bin-hadoop" + hadoop_version + ".tgz"

pyspark_local_path_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/pyspark_local/'
py3spark_local_path_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/py3spark_local/'
jupyter_conf_file = '/home/' + args.os_user + '/.local/share/jupyter/jupyter_notebook_config.py'
scala_kernel_path = '/usr/local/share/jupyter/kernels/apache_toree_scala/'
r_kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
jars_dir = '/opt/jars/'
templates_dir = '/root/templates/'
files_dir = '/root/files/'
local_spark_path = '/opt/spark/'
toree_link = 'https://dist.apache.org/repos/dist/dev/incubator/toree/0.2.0/snapshots/dev1/toree-pip/toree-0.2.0.dev1.tar.gz'
r_libs = ['R6', 'pbdZMQ', 'RCurl', 'devtools', 'reshape2', 'caTools', 'rJava', 'ggplot2']
gitlab_certfile = os.environ['conf_gitlab_certfile']


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    print "Configuring notebook server."
    try:
        if not exists('/home/' + args.os_user + '/.ensure_dir'):
            sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)

    print "Mount additional volume"
    prepare_disk(args.os_user)

    print "Install Java"
    ensure_jre_jdk(args.os_user)

    print "Install Scala"
    ensure_scala(scala_link, args.scala_version, args.os_user)

    print "Install python2 libraries"
    ensure_python2_libraries(args.os_user)

    print "Install python3 libraries"
    ensure_python3_libraries(args.os_user)

    print "Install Jupyter"
    configure_jupyter(args.os_user, jupyter_conf_file, templates_dir, jupyter_version)

    print "Install local Spark"
    ensure_local_spark(args.os_user, spark_link, spark_version, hadoop_version, local_spark_path)

    print "Install local jars"
    ensure_local_jars(args.os_user, jars_dir, files_dir, args.region, templates_dir)

    print "Install pyspark local kernel for Jupyter"
    ensure_pyspark_local_kernel(args.os_user, pyspark_local_path_dir, templates_dir, spark_version)

    print "Install py3spark local kernel for Jupyter"
    ensure_py3spark_local_kernel(args.os_user, py3spark_local_path_dir, templates_dir, spark_version)

    print "Install Toree-Scala kernel for Jupyter"
    ensure_toree_local_kernel(args.os_user, toree_link, scala_kernel_path, files_dir, args.scala_version, spark_version)

    print "Installing R"
    ensure_r(args.os_user, r_libs, args.region, args.r_mirror)

    print "Install R kernel for Jupyter"
    ensure_r_local_kernel(spark_version, args.os_user, templates_dir, r_kernels_dir)

    print "Install Ungit"
    install_nodejs(args.os_user)
    install_ungit(args.os_user)
    if exists('/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)