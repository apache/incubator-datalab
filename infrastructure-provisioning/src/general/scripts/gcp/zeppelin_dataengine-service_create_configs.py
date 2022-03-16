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
import subprocess
from datalab.actions_lib import *
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--numpy_version', type=str, default='')
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
    subprocess.run('sudo chown {0}:{0} -R /opt/zeppelin/'.format(args.os_user), shell=True, check=True)
    subprocess.run('sudo service zeppelin-notebook stop', shell=True, check=True)
    subprocess.run('sudo -i wget http://archive.cloudera.com/beta/livy/livy-server-{0}.zip -O /opt/{1}/{2}/livy-server-{0}.zip'
          .format(args.livy_version, args.dataproc_version, args.cluster_name), shell=True, check=True)
    subprocess.run('sudo unzip /opt/{0}/{1}/livy-server-{2}.zip -d /opt/{0}/{1}/'.format(args.dataproc_version, args.cluster_name, args.livy_version), shell=True, check=True)
    subprocess.run('sudo mv /opt/{0}/{1}/livy-server-{2}/ /opt/{0}/{1}/livy/'.format(args.dataproc_version, args.cluster_name, args.livy_version), shell=True, check=True)
    livy_path = '/opt/{0}/{1}/livy/'.format(args.dataproc_version, args.cluster_name)
    subprocess.run('sudo mkdir -p {0}/logs'.format(livy_path), shell=True, check=True)
    subprocess.run('sudo mkdir -p /var/run/livy', shell=True, check=True)
    subprocess.run('sudo chown {0}:{0} -R /var/run/livy'.format(args.os_user), shell=True, check=True)
    subprocess.run('sudo chown {0}:{0} -R {1}'.format(args.os_user, livy_path), shell=True, check=True)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(dataproc_dir, yarn_dir)
        if result == False :
            datalab.actions_lib.GCPActions().jars(args, dataproc_dir)
        datalab.actions_lib.GCPActions().yarn(args, yarn_dir)
        datalab.actions_lib.GCPActions().install_dataproc_spark(args)
        datalab.actions_lib.GCPActions().spark_defaults(args)
        configuring_notebook(args.dataproc_version)
        if args.multiple_clusters == 'true':
            install_remote_livy(args)
        installing_python(args.region, args.bucket, args.user_name, args.cluster_name, args.application,
                          args.pip_mirror, args.numpy_version)
        datalab.actions_lib.GCPActions().configure_zeppelin_dataproc_interpreter(args.dataproc_version,
                                                                                 args.cluster_name, spark_dir,
                                                                                 args.os_user, yarn_dir,
                                                                                 args.bucket, args.user_name,
                                                                                 args.multiple_clusters)
        update_zeppelin_interpreters(args.multiple_clusters, args.r_enabled)
