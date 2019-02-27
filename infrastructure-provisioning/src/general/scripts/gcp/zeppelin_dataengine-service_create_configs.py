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
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--proxy_port', type=str, default='')
parser.add_argument('--livy_version', type=str, default='')
parser.add_argument('--multiple_clusters', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--r_enabled', type=str, default='')
args = parser.parse_args()

dataproc_dir = '/opt/' + args.dataproc_version + '/jars/'
spark_dir = '/opt/' + args.dataproc_version + '/' + args.cluster_name + '/spark/'
yarn_dir = '/opt/' + args.dataproc_version + '/' + args.cluster_name + '/conf/'


def install_remote_livy(args):
    local('sudo chown {0}:{0} -R /opt/zeppelin/'.format(args.os_user))
    local('sudo service zeppelin-notebook stop')
    local('sudo -i wget http://archive.cloudera.com/beta/livy/livy-server-{0}.zip -O /opt/{1}/{2}/livy-server-{0}.zip'
          .format(args.livy_version, args.dataproc_version, args.cluster_name))
    local('sudo unzip /opt/{0}/{1}/livy-server-{2}.zip -d /opt/{0}/{1}/'.format(args.dataproc_version, args.cluster_name, args.livy_version))
    local('sudo mv /opt/{0}/{1}/livy-server-{2}/ /opt/{0}/{1}/livy/'.format(args.dataproc_version, args.cluster_name, args.livy_version))
    livy_path = '/opt/{0}/{1}/livy/'.format(args.dataproc_version, args.cluster_name)
    local('sudo mkdir -p {0}/logs'.format(livy_path))
    local('sudo mkdir -p /var/run/livy')
    local('sudo chown {0}:{0} -R /var/run/livy'.format(args.os_user))
    local('sudo chown {0}:{0} -R {1}'.format(args.os_user, livy_path))


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(dataproc_dir, yarn_dir)
        if result == False :
            actions_lib.GCPActions().jars(args, dataproc_dir)
        actions_lib.GCPActions().yarn(args, yarn_dir)
        actions_lib.GCPActions().install_dataproc_spark(args)
        actions_lib.GCPActions().spark_defaults(args)
        configuring_notebook(args.dataproc_version)
        if args.multiple_clusters == 'true':
            install_remote_livy(args)
        installing_python(args.region, args.bucket, args.user_name, args.cluster_name, args.application, args.pip_mirror)
        actions_lib.GCPActions().configure_zeppelin_dataproc_interpreter(args.dataproc_version, args.cluster_name, spark_dir, args.os_user,
                                                                         yarn_dir, args.bucket, args.user_name, args.multiple_clusters)
        update_zeppelin_interpreters(args.multiple_clusters, args.r_enabled)
