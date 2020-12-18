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
from datalab.actions_lib import *
from datalab.meta_lib import *
from fabric.api import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--edge_user_name', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--proxy_port', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
args = parser.parse_args()


def configure_notebook(args):
    scripts_dir = '/root/scripts/'
    put(scripts_dir + '{}_dataengine-service_create_configs.py'.format(args.application), '/tmp/create_configs.py')
    sudo('\cp /tmp/create_configs.py /usr/local/bin/create_configs.py')
    sudo('chmod 755 /usr/local/bin/create_configs.py')
    sudo('mkdir -p /usr/lib/python2.7/datalab/')
    run('mkdir -p /tmp/datalab_libs/')
    local('scp -i {} /usr/lib/python2.7/datalab/* {}:/tmp/datalab_libs/'.format(args.keyfile, env.host_string))
    run('chmod a+x /tmp/datalab_libs/*')
    sudo('mv /tmp/datalab_libs/* /usr/lib/python2.7/datalab/')
    if exists('/usr/lib64'):
        sudo('ln -fs /usr/lib/python2.7/datalab /usr/lib64/python2.7/datalab')


if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts
    configure_notebook(args)
    spark_version = datalab.actions_lib.GCPActions().get_cluster_app_version(args.bucket, args.project_name, args.cluster_name, 'spark')
    hadoop_version = datalab.actions_lib.GCPActions().get_cluster_app_version(args.bucket, args.project_name, args.cluster_name, 'hadoop')
    sudo('echo "[global]" > /etc/pip.conf; echo "proxy = $(cat /etc/profile | grep proxy | head -n1 | cut -f2 -d=)" >> /etc/pip.conf')
    sudo('echo "use_proxy=yes" > ~/.wgetrc; proxy=$(cat /etc/profile | grep proxy | head -n1 | cut -f2 -d=); echo "http_proxy=$proxy" >> ~/.wgetrc; echo "https_proxy=$proxy" >> ~/.wgetrc')
    sudo('unset http_proxy https_proxy; export gcp_project_id="{0}"; export conf_resource="{1}"; /usr/bin/python /usr/local/bin/create_configs.py --bucket {2} --cluster_name {3} --dataproc_version {4} --spark_version {5} --hadoop_version {6} --region {7} --user_name {8} --os_user {9} --pip_mirror {10} --application {11}'
         .format(os.environ['gcp_project_id'], os.environ['conf_resource'], args.bucket, args.cluster_name, args.dataproc_version, spark_version, hadoop_version,
                 args.region, args.project_name, args.os_user, args.pip_mirror, args.application))
