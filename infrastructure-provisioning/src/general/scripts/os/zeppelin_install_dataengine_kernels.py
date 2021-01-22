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
from datalab.fab import *
from datalab.meta_lib import *
from fabric.api import *
from fabric.contrib.files import exists

parser = argparse.ArgumentParser()
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='false')
parser.add_argument('--spark_master_ip', type=str, default='')
args = parser.parse_args()


def configure_notebook(keyfile, hoststring):
    templates_dir = '/root/templates/'
    scripts_dir = '/root/scripts/'
    run('mkdir -p /tmp/{}/'.format(args.cluster_name))
    if os.environ['notebook_multiple_clusters'] == 'true':
        put(templates_dir + 'dataengine_interpreter_livy.json', '/tmp/{}/dataengine_interpreter.json'.format(args.cluster_name))
    else:
        put(templates_dir + 'dataengine_interpreter_spark.json',
            '/tmp/{}/dataengine_interpreter.json'.format(args.cluster_name))
    put(templates_dir + 'notebook_spark-defaults_local.conf',
        '/tmp/{}/notebook_spark-defaults_local.conf'.format(args.cluster_name))
    spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
    spark_memory = get_spark_memory(True, args.os_user, spark_master_ip, keyfile)
    run('sed -i "s|EXECUTOR_MEMORY|{}m|g " /tmp/{}/dataengine_interpreter.json'.format(spark_memory, args.cluster_name))
    run('echo "spark.executor.memory {0}m" >> /tmp/{1}/notebook_spark-defaults_local.conf'.format(spark_memory,
                                                                                                  args.cluster_name))
    if not exists('/usr/local/bin/zeppelin_dataengine_create_configs.py'):
        put(scripts_dir + 'zeppelin_dataengine_create_configs.py',
            '/usr/local/bin/zeppelin_dataengine_create_configs.py', use_sudo=True)
        sudo('chmod 755 /usr/local/bin/zeppelin_dataengine_create_configs.py')
    if not exists('/usr/lib/python2.7/datalab/'):
        sudo('mkdir -p /usr/lib/python2.7/datalab/')
        put('/usr/lib/python2.7/datalab/*', '/usr/lib/python2.7/datalab/', use_sudo=True)
        sudo('chmod a+x /usr/lib/python2.7/datalab/*')
        if exists('/usr/lib64'):
            sudo('ln -fs /usr/lib/python2.7/datalab /usr/lib64/python2.7/datalab')

def create_inactivity_log(master_ip, hoststring):
    reworked_ip = master_ip.replace('.', '-')
    sudo("date +%s > /opt/inactivity/{}_inactivity".format(reworked_ip))

if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts
    try:
        region = os.environ['aws_region']
    except:
        region = ''
    if 'spark_configurations' not in os.environ:
        os.environ['spark_configurations'] = '[]'
    configure_notebook(args.keyfile, env.host_string)
    create_inactivity_log(args.spark_master_ip, env.host_string)
    livy_version = os.environ['notebook_livy_version']
    r_enabled = os.environ['notebook_r_enabled']
    sudo('/usr/bin/python3 /usr/local/bin/zeppelin_dataengine_create_configs.py '
         '--cluster_name {} --spark_version {} --hadoop_version {} --os_user {} --spark_master {} --keyfile {} \
         --notebook_ip {} --livy_version {} --multiple_clusters {} --region {} --datalake_enabled {} '
         '--r_enabled {} --spark_configurations "{}"'.
         format(args.cluster_name, args.spark_version, args.hadoop_version, args.os_user, args.spark_master,
                args.keyfile, args.notebook_ip, livy_version, os.environ['notebook_multiple_clusters'], region,
                args.datalake_enabled, r_enabled, os.environ['spark_configurations']))
