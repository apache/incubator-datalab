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
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *
from patchwork.files import exists
from patchwork import files

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--rstudio_pass', type=str, default='')
parser.add_argument('--rstudio_version', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--edge_ip', type=str, default='')
args = parser.parse_args()

spark_version = os.environ['notebook_spark_version']
hadoop_version = os.environ['notebook_hadoop_version']
python_venv_version = os.environ['notebook_python_venv_version']
python_venv_path = '/opt/python/python{0}/bin/python{1}'.format(python_venv_version, python_venv_version[:3])
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
local_spark_path = '/opt/spark/'
jars_dir = '/opt/jars/'
templates_dir = '/root/templates/'
files_dir = '/root/files/'
r_libs = ['R6', 'pbdZMQ={}'.format(os.environ['notebook_pbdzmq_version']), 'RCurl', 'reshape2', 'caTools={}'.format(os.environ['notebook_catools_version']), 'rJava', 'ggplot2', 'evaluate', 'formatR', 'yaml',
          'Rcpp', 'rmarkdown', 'base64enc', 'tibble', 'sparklyr']
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

    # INSTALL LANGUAGES
    print("Install Java")
    ensure_jre_jdk(args.os_user)
    print("Install R")
    ensure_r(args.os_user, r_libs)
    print("Install Python 3 modules")
    ensure_python3_libraries(args.os_user)

    # INSTALL PYTHON IN VIRTUALENV
    print("Configure Python Virtualenv")
    ensure_python_venv(python_venv_version)

    # INSTALL RSTUDIO
    print("Install RStudio")
    install_rstudio(args.os_user, local_spark_path, args.rstudio_pass, args.rstudio_version, python_venv_version)

    # INSTALL SPARK AND CLOUD STORAGE JARS FOR SPARK
    print("Install local Spark")
    ensure_local_spark(args.os_user, spark_link, spark_version, hadoop_version, local_spark_path)
    print("Install storage jars")
    ensure_local_jars(args.os_user, jars_dir)
    print("Configure local Spark")
    configure_local_spark(jars_dir, templates_dir)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install Ungit")
    install_ungit(args.os_user, args.exploratory_name, args.edge_ip)
    if exists(conn, '/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)

    # INSTALL INACTIVITY CHECKER
    print("Install inactivity checker")
    install_inactivity_checker(args.os_user, args.ip_address, True)

    #POST INSTALLATION PROCESS
    print("Updating pyOpenSSL library")
    update_pyopenssl_lib(args.os_user)

    conn.close()
