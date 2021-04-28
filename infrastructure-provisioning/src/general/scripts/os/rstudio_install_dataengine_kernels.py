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
from patchwork import files

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
    scripts_dir = '/root/scripts/'
    templates_dir = '/root/templates/'
    conn.run('mkdir -p /tmp/{}/'.format(args.cluster_name))
    conn.put(templates_dir + 'notebook_spark-defaults_local.conf',
        '/tmp/{}/notebook_spark-defaults_local.conf'.format(args.cluster_name))
    spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
    spark_memory = get_spark_memory(True, args.os_user, spark_master_ip, keyfile)
    conn.run('echo "spark.executor.memory {0}m" >> /tmp/{1}/notebook_spark-defaults_local.conf'.format(spark_memory,
                                                                                                  args.cluster_name))
    if not exists(conn,'/usr/local/bin/rstudio_dataengine_create_configs.py'):
        conn.put(scripts_dir + 'rstudio_dataengine_create_configs.py', '/tmp/rstudio_dataengine_create_configs.py')
        conn.sudo('cp -f /tmp/rstudio_dataengine_create_configs.py /usr/local/bin/rstudio_dataengine_create_configs.py')
        conn.sudo('chmod 755 /usr/local/bin/rstudio_dataengine_create_configs.py')
    if not exists(conn,'/usr/lib/python3.8/datalab/'):
        conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
        conn.local('cd  /usr/lib/python3.8/datalab/; tar -zcvf /tmp/datalab.tar.gz *')
        conn.put('/tmp/datalab.tar.gz', '/tmp/datalab.tar.gz')
        conn.sudo('tar -zxvf /tmp/datalab.tar.gz -C /usr/lib/python3.8/datalab/')
        conn.sudo('chmod a+x /usr/lib/python3.8/datalab/*')
        if exists(conn, '/usr/lib64'):
            conn.sudo('mkdir -p /usr/lib64/python3.8')
            conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')

def create_inactivity_log(master_ip, hoststring):
    reworked_ip = master_ip.replace('.', '-')
    conn.sudo('''bash -l -c "date +%s > /opt/inactivity/{}_inactivity" '''.format(reworked_ip))

if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)
    try:
        region = os.environ['aws_region']
    except:
        region = ''
    if 'spark_configurations' not in os.environ:
        os.environ['spark_configurations'] = '[]'
    configure_notebook(args.keyfile, args.notebook_ip)
    create_inactivity_log(args.spark_master_ip, args.notebook_ip)
    conn.sudo('/usr/bin/python3 /usr/local/bin/rstudio_dataengine_create_configs.py '
         '--cluster_name {} --spark_version {} --hadoop_version {} --os_user {} --spark_master {} --region {} '
         '--datalake_enabled {} --spark_configurations "{}"'.
         format(args.cluster_name, args.spark_version, args.hadoop_version, args.os_user, args.spark_master, region,
                args.datalake_enabled, os.environ['spark_configurations']))
