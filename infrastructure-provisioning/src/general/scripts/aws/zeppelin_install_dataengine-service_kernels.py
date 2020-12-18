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
from datalab.meta_lib import *
from fabric.api import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--emr_excluded_spark_properties', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--proxy_port', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
args = parser.parse_args()


def configure_notebook(args):
    templates_dir = '/root/templates/'
    scripts_dir = '/root/scripts/'
    if os.environ['notebook_multiple_clusters'] == 'true':
        put(templates_dir + 'dataengine-service_interpreter_livy.json', '/tmp/dataengine-service_interpreter.json')
    else:
        put(templates_dir + 'dataengine-service_interpreter_spark.json', '/tmp/dataengine-service_interpreter.json')
    put(scripts_dir + '{}_dataengine-service_create_configs.py'.format(args.application),
        '/tmp/zeppelin_dataengine-service_create_configs.py')
    sudo(
        '\cp /tmp/zeppelin_dataengine-service_create_configs.py /usr/local/bin/zeppelin_dataengine-service_create_configs.py')
    sudo('chmod 755 /usr/local/bin/zeppelin_dataengine-service_create_configs.py')
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
    spark_version = get_spark_version(args.cluster_name)
    hadoop_version = get_hadoop_version(args.cluster_name)
    livy_version = os.environ['notebook_livy_version']
    r_enabled = os.environ['notebook_r_enabled']
    numpy_version = os.environ['notebook_numpy_version']
    command = "/usr/bin/python /usr/local/bin/zeppelin_dataengine-service_create_configs.py " \
             "--bucket {0} " \
             "--cluster_name {1} " \
             "--emr_version {2} " \
             "--spark_version {3} " \
             "--hadoop_version {4} " \
             "--region {5} " \
             "--excluded_lines '{6}' " \
             "--project_name {7} " \
             "--os_user {8} " \
             "--edge_hostname {9} " \
             "--proxy_port {10} " \
             "--scala_version {11} " \
             "--livy_version {12} " \
             "--multiple_clusters {13} " \
             "--pip_mirror {14} " \
             "--numpy_version {15} " \
             "--application {16} " \
             "--r_enabled {17}" \
        .format(args.bucket,
                args.cluster_name,
                args.emr_version,
                spark_version,
                hadoop_version,
                args.region,
                args.emr_excluded_spark_properties,
                args.project_name,
                args.os_user,
                args.edge_hostname,
                args.proxy_port,
                args.scala_version,
                livy_version,
                os.environ['notebook_multiple_clusters'],
                args.pip_mirror,
                numpy_version,
                args.application,
                r_enabled)
    sudo(command)