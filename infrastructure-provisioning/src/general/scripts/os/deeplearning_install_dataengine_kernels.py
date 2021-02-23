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
from fabric import *
from patchwork.files import exists

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
    conn.run('mkdir -p /tmp/{}/'.format(args.cluster_name))
    conn.put(templates_dir + 'sparkmagic_config_template.json', '/tmp/sparkmagic_config_template.json')
    if not exists(conn,'/tmp/deeplearning_dataengine_create_configs.py'):
        conn.put(scripts_dir + 'deeplearning_dataengine_create_configs.py',
            '/tmp/deeplearning_dataengine_create_configs.py')
    # conn.put(templates_dir + 'pyspark_dataengine_template.json', '/tmp/{}/pyspark_dataengine_template.json'.format(args.cluster_name))
    # conn.put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/{}/notebook_spark-defaults_local.conf'.format(args.cluster_name))
    spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
    # spark_memory = get_spark_memory(True, args.os_user, spark_master_ip, keyfile)
    # conn.run('echo "spark.executor.memory {0}m" >> /tmp/{1}/notebook_spark-defaults_local.conf'.format(spark_memory, args.cluster_name))
    if not exists(conn,'/usr/local/bin/deeplearning_dataengine_create_configs.py'):
        conn.put(scripts_dir + 'deeplearning_dataengine_create_configs.py', '/tmp/deeplearning_dataengine_create_configs.py')
        conn.sudo('cp /tmp/deeplearning_dataengine_create_configs.py /usr/local/bin/deeplearning_dataengine_create_configs.py')
        conn.sudo('chmod 755 /usr/local/bin/deeplearning_dataengine_create_configs.py')
    if not exists(conn,'/usr/lib/python3.8/datalab/'):
        conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
        conn.put('/usr/lib/python3.8/datalab/*', '/usr/lib/python3.8/datalab/', use_sudo=True)
        conn.sudo('chmod a+x /usr/lib/python3.8/datalab/*')
        if exists('/usr/lib64'):
            conn.sudo('mkdir -p /usr/lib64/python3.8')
            conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')

def create_inactivity_log(master_ip, hoststring):
    reworked_ip = master_ip.replace('.', '-')
    conn.sudo("date +%s > /opt/inactivity/{}_inactivity".format(reworked_ip))

if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts
    try:
        region = os.environ['aws_region']
    except:
        region = ''
    configure_notebook(args.keyfile, env.host_string)
    create_inactivity_log(args.spark_master_ip, env.host_string)
    if 'spark_configurations' not in os.environ:
        os.environ['spark_configurations'] = '[]'
    conn.sudo('/usr/bin/python3 /usr/local/bin/deeplearning_dataengine_create_configs.py '
         '--cluster_name {} --spark_version {} --hadoop_version {} --os_user {} --spark_master {} --region {} '
         '--datalake_enabled {} --spark_configurations "{}"'.format(args.cluster_name, args.spark_version,
                                                                  args.hadoop_version, args.os_user,  args.spark_master,
                                                                  region, args.datalake_enabled,
                                                                  os.environ['spark_configurations']))
