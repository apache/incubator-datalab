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
from fabric import *

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
parser.add_argument('--application', type=str, default='')
args = parser.parse_args()


def configure_notebook(args):
    templates_dir = '/root/templates/'
    scripts_dir = '/root/scripts/'
    if os.environ['notebook_multiple_clusters'] == 'true':
        conn.put(templates_dir + 'dataengine-service_interpreter_livy.json', '/tmp/dataengine-service_interpreter.json')
    else:
        conn.put(templates_dir + 'dataengine-service_interpreter_spark.json', '/tmp/dataengine-service_interpreter.json')
    conn.put(scripts_dir + '{}_dataengine-service_create_configs.py'.format(args.application), '/tmp/create_configs.py')
    conn.sudo('\cp /tmp/create_configs.py /usr/local/bin/create_configs.py')
    conn.sudo('chmod 755 /usr/local/bin/create_configs.py')
    conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
    conn.run('mkdir -p /home/{}/datalab_libs/'.format(args.os_user))
    conn.local('rsync -e "ssh -i {0}" /usr/lib/python3.8/datalab/*.py {1}@{2}:/home/{1}/datalab_libs/'
               .format(args.keyfile, args.os_user, args.notebook_ip))
    conn.run('chmod a+x /home/{}/datalab_libs/*'.format(args.os_user))
    conn.sudo('mv /home/{}/datalab_libs/* /usr/lib/python3.8/datalab/'.format(args.os_user))
    conn.sudo('rm -rf /home/{}/datalab_libs/'.format(args.os_user))
    if exists(conn, '/usr/lib64'):
        conn.sudo('mkdir -p /usr/lib64/python3.8')
        conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')


if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)
    configure_notebook(args)
    r_enabled = os.environ['notebook_r_enabled']
    spark_version = datalab.actions_lib.GCPActions().get_cluster_app_version(args.bucket, args.project_name, args.cluster_name, 'spark')
    hadoop_version = datalab.actions_lib.GCPActions().get_cluster_app_version(args.bucket, args.project_name, args.cluster_name, 'hadoop')
    conn.sudo('''bash -l -c 'echo "[global]" > /etc/pip.conf; echo "proxy = $(cat /etc/profile | grep proxy | head -n1 | cut -f2 -d=)" >> /etc/pip.conf' ''')
    conn.sudo('''bash -l -c 'echo "use_proxy=yes" > ~/.wgetrc; proxy=$(cat /etc/profile | grep proxy | head -n1 | cut -f2 -d=); echo "http_proxy=$proxy" >> ~/.wgetrc; echo "https_proxy=$proxy" >> ~/.wgetrc' ''')
    conn.sudo('''bash -l -c 'unset http_proxy https_proxy; export gcp_project_id="{0}"; export conf_resource="{1}"; /usr/bin/python3 /usr/local/bin/create_configs.py --bucket {2} --cluster_name {3} --dataproc_version {4} --spark_version {5} --hadoop_version {6} --region {7} --user_name {8} --os_user {9} --application {10} --livy_version {11} --multiple_clusters {12} --r_enabled {13} --numpy_version {14}' '''
        .format(os.environ['gcp_project_id'], os.environ['conf_resource'], args.bucket, args.cluster_name,
                args.dataproc_version, spark_version, hadoop_version, args.region, args.project_name, args.os_user,
                args.application, os.environ['notebook_livy_version'], os.environ['notebook_multiple_clusters'],
                r_enabled, os.environ['notebook_numpy_version']))