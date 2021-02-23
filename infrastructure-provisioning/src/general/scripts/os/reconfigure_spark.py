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
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--instance_ip', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--resource_type', type=str, default='')
parser.add_argument('--spark_type', type=str, default='local')
parser.add_argument('--cluster_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.instance_ip, args.os_user, args.keyfile)

    jars_dir = '/opt/jars/'
    templates_dir = '/root/templates/'
    if args.resource_type == 'dataengine':
        memory_type = ''
    else:
        memory_type = 'driver'
    if args.spark_type == 'local':
        configure_local_spark(jars_dir, templates_dir, memory_type)
    elif args.spark_type == 'dataengine':
        if not exists(conn,'/usr/local/bin/notebook_reconfigure_dataengine_spark.py'):
            conn.put('/root/scripts/notebook_reconfigure_dataengine_spark.py',
                '/tmp/notebook_reconfigure_dataengine_spark.py')
            conn.sudo('mv /tmp/notebook_reconfigure_dataengine_spark.py '
                 '/usr/local/bin/notebook_reconfigure_dataengine_spark.py')
        conn.sudo('mkdir -p /tmp/{}'.format(args.cluster_name))
        conn.put('{}notebook_spark-defaults_local.conf'.format(templates_dir),
            '/tmp/{}/notebook_spark-defaults_local.conf'.format(args.cluster_name))
        cluster_dir = '/opt/' + args.cluster_name + '/'
        if 'azure_datalake_enable' in os.environ:
            datalake_enabled = os.environ['azure_datalake_enable']
        else:
            datalake_enabled = 'false'
        if 'spark_configurations' not in os.environ:
            os.environ['spark_configurations'] = '[]'
        conn.sudo('/usr/bin/python3 /usr/local/bin/notebook_reconfigure_dataengine_spark.py --cluster_name {0} '
             '--jars_dir {1} --cluster_dir {2} --datalake_enabled {3} --spark_configurations "{4}"'.format(
              args.cluster_name, jars_dir, cluster_dir, datalake_enabled, os.environ['spark_configurations']))
    conn.close()