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
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='')
parser.add_argument('--spark_configurations', type=str, default='')
args = parser.parse_args()

cluster_dir = '/opt/' + args.cluster_name + '/'
local_jars_dir = '/opt/jars/'
spark_version = args.spark_version
hadoop_version = args.hadoop_version
spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
             "-bin-hadoop" + hadoop_version + ".tgz"


def configure_rstudio():
    if not os.path.exists('/home/' + args.os_user + '/.ensure_dir/rstudio_dataengine_ensured'):
        try:
            local('echo "export R_LIBS_USER=' + cluster_dir + 'spark/R/lib:" >> /home/' + args.os_user + '/.bashrc')
            local("sed -i 's/^SPARK_HOME/#SPARK_HOME/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^YARN_CONF_DIR/#YARN_CONF_DIR/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^HADOOP_CONF_DIR/#HADOOP_CONF_DIR/' /home/" + args.os_user + "/.Renviron")
            local('echo \'SPARK_HOME="' + cluster_dir + 'spark/"\' >> /home/' + args.os_user + '/.Renviron')
            local("sed -i 's/^master/#master/' /home/" + args.os_user + "/.Rprofile")
            local('echo \'master="' + args.spark_master + '" # Cluster - "' + args.cluster_name + '" \' >> /home/' +
                  args.os_user + '/.Rprofile')
            local('''R -e "source('/home/{}/.Rprofile')"'''.format(args.os_user))
            local('touch /home/' + args.os_user + '/.ensure_dir/rstudio_dataengine_ensured')
        except Exception as err:
            print('Error: {0}'.format(err))
            sys.exit(1)
    else:
        try:
            local("sed -i '/R_LIBS_USER/ { s|=\(.*\)|=\\1" + cluster_dir + "spark/R/lib:| }' /home/" + args.os_user + "/.bashrc")
            local("sed -i 's/^SPARK_HOME/#SPARK_HOME/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^YARN_CONF_DIR/#YARN_CONF_DIR/' /home/" + args.os_user + "/.Renviron")
            local("sed -i 's/^HADOOP_CONF_DIR/#HADOOP_CONF_DIR/' /home/" + args.os_user + "/.Renviron")
            local('echo \'SPARK_HOME="' + cluster_dir + 'spark/"\' >> /home/' + args.os_user + '/.Renviron')
            local("sed -i 's/^master/#master/' /home/" + args.os_user + "/.Rprofile")
            local('echo \'master="' + args.spark_master + '" # Cluster - "' + args.cluster_name + '" \' >> /home/' +
                  args.os_user + '/.Rprofile')
            local('''R -e "source('/home/{}/.Rprofile')"'''.format(args.os_user))
        except Exception as err:
            print('Error: {0}'.format(err))
            sys.exit(1)


if __name__ == "__main__":
    dataengine_dir_prepare('/opt/{}/'.format(args.cluster_name))
    install_dataengine_spark(args.cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, args.os_user,
                             args.datalake_enabled)
    ensure_dataengine_tensorflow_jars(local_jars_dir)
    configure_dataengine_spark(args.cluster_name, local_jars_dir, cluster_dir, args.datalake_enabled,
                               args.spark_configurations)
    configure_rstudio()

