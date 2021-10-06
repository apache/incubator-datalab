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

import argparse
import os
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--edge_ip', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
args = parser.parse_args()

spark_version = args.spark_version
hadoop_version = args.hadoop_version
jupyter_version = os.environ['notebook_jupyter_version']
scala_link = "https://www.scala-lang.org/files/archive/"
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"

docker_version = '18.09.4'
http_file = '/etc/systemd/system/docker.service.d/http-proxy.conf'
https_file = '/etc/systemd/system/docker.service.d/https-proxy.conf'
legion_dir = '/home/' + args.os_user + '/legion/legion/'
jupyterlab_image = os.environ['notebook_jupyterlab_image']
jupyterlab_dir = '/home/' + args.os_user + '/.jupyterlab/'
spark_script = jupyterlab_dir + 'spark.sh'
pyspark_local_path_dir = '/home/' + args.os_user + '/.jupyterlab/kernels/pyspark_local/'
py3spark_local_path_dir = '/home/' + args.os_user + '/.jupyterlab/kernels/py3spark_local/'
jupyter_conf_file = jupyterlab_dir + 'jupyter_notebook_config.py'
jupyterlab_conf_file = '\/etc\/jupyter\/jupyter_notebook_config.py'
scala_kernel_path = '/usr/local/share/jupyter/kernels/apache_toree_scala/'
r_kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
jars_dir = '/opt/jars/'
templates_dir = '/root/templates/'
files_dir = '/root/files/'
local_spark_path = '/opt/spark/'
toree_link = 'http://archive.apache.org/dist/incubator/toree/0.3.0-incubating/toree-pip/toree-0.3.0.tar.gz'
r_libs = ['R6', 'pbdZMQ={}'.format(os.environ['notebook_pbdzmq_version']), 'RCurl', 'reshape2', 'caTools={}'.format(os.environ['notebook_catools_version']), 'rJava', 'ggplot2']
gitlab_certfile = os.environ['conf_gitlab_certfile']


##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)

    # PREPARE DISK
    print("Prepare .ensure directory")
    try:
        if not exists(conn,'/home/' + args.os_user + '/.ensure_dir'):
            conn.sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)
    print("Mount additional volume")
    prepare_disk(args.os_user)

    # INSTALL DOCKER
    print ("Install Docker")
    configure_docker(args.os_user)

    # CONFIGURE JUPYTER FILES
    print("Configure jupyter files")
    ensure_jupyterlab_files(args.os_user, jupyterlab_dir, jupyterlab_image, jupyter_conf_file, jupyterlab_conf_file, args.exploratory_name, args.edge_ip)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install ungit")
    install_ungit(args.os_user, args.exploratory_name, args.edge_ip)
    if exists(conn, '/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)

    # INSTALL INACTIVITY CHECKER
    print("Install inactivity checker")
    install_inactivity_checker(args.os_user, args.ip_address)

    conn.close()