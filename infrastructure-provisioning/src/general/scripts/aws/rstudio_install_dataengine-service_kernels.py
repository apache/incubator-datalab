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
from datalab.meta_lib import *
from fabric import *

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
    scripts_dir = '/root/scripts/'
    conn.put(scripts_dir + '{}_dataengine-service_create_configs.py'.format(args.application),
        '/tmp/rstudio_dataengine-service_create_configs.py')
    conn.sudo(
        '\cp /tmp/rstudio_dataengine-service_create_configs.py /usr/local/bin/rstudio_dataengine-service_create_configs.py')
    conn.sudo('chmod 755 /usr/local/bin/rstudio_dataengine-service_create_configs.py')
    conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
    conn.run('mkdir -p /tmp/datalab_libs/')
    conn.local('rsync -e "ssh -i {}" /usr/lib/python3.8/datalab/*.py {}@{}:/tmp/datalab_libs/'.format(args.keyfile, args.os_user, args.notebook_ip))
    conn.run('chmod a+x /tmp/datalab_libs/*')
    conn.sudo('mv /tmp/datalab_libs/* /usr/lib/python3.8/datalab/')
    if exists(conn, '/usr/lib64'):
        conn.sudo('mkdir -p /usr/lib64/python3.8')
        conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')


if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)
    configure_notebook(args)
    spark_version = get_spark_version(args.cluster_name)
    hadoop_version = get_hadoop_version(args.cluster_name)
    conn.sudo("/usr/bin/python3 /usr/local/bin/rstudio_dataengine-service_create_configs.py --bucket " + args.bucket +
         " --cluster_name " + args.cluster_name + " --emr_version " + args.emr_version + " --spark_version " +
         spark_version + " --hadoop_version " + hadoop_version + " --region " + args.region + " --excluded_lines '"
         + args.emr_excluded_spark_properties + "' --project_name " + args.project_name + " --os_user " + args.os_user)
