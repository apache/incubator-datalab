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
import boto3
import os
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
    templates_dir = '/root/templates/'
    files_dir = '/root/files/'
    scripts_dir = '/root/scripts/'
    conn.put(templates_dir + 'sparkmagic_config_template.json', '/tmp/sparkmagic_config_template.json')
    # conn.put(templates_dir + 'pyspark_dataengine-service_template.json', '/tmp/pyspark_dataengine-service_template.json')
    # conn.put(templates_dir + 'r_dataengine-service_template.json', '/tmp/r_dataengine-service_template.json')
    # conn.put(templates_dir + 'toree_dataengine-service_template.json','/tmp/toree_dataengine-service_template.json')
    conn.put(scripts_dir + '{}_dataengine-service_create_configs.py'.format(args.application),
        '/tmp/jupyter_dataengine-service_create_configs.py')
    # conn.put(files_dir + 'toree_kernel.tar.gz', '/tmp/toree_kernel.tar.gz')
    # conn.put(templates_dir + 'toree_dataengine-service_templatev2.json', '/tmp/toree_dataengine-service_templatev2.json')
    # conn.put(templates_dir + 'run_template.sh', '/tmp/run_template.sh')
    conn.sudo(
        '\cp /tmp/jupyter_dataengine-service_create_configs.py /usr/local/bin/jupyter_dataengine-service_create_configs.py')
    conn.sudo('chmod 755 /usr/local/bin/jupyter_dataengine-service_create_configs.py')
    conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
    conn.run('mkdir -p /tmp/datalab_libs/')
    local('scp -i {} /usr/lib/python3.8/datalab/*.py {}:/tmp/datalab_libs/'.format(args.keyfile, env.host_string))
    conn.run('chmod a+x /tmp/datalab_libs/*')
    conn.sudo('mv /tmp/datalab_libs/* /usr/lib/python3.8/datalab/')
    if exists('/usr/lib64'):
        conn.sudo('mkdir -p /usr/lib64/python3.8')
        conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')


if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts
    configure_notebook(args)
    spark_version = get_spark_version(args.cluster_name)
    hadoop_version = get_hadoop_version(args.cluster_name)
    r_enabled = os.environ['notebook_r_enabled']
    numpy_version = os.environ['notebook_numpy_version']
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=args.region)
    s3_client.download_file(args.bucket, args.project_name + '/' + args.cluster_name + '/scala_version',
                            '/tmp/scala_version')
    s3_client.download_file(args.bucket, args.project_name + '/' + args.cluster_name + '/python_version',
                            '/tmp/python_version')
    with open('/tmp/scala_version') as f:
        scala_version = str(f.read()).rstrip()
        print(scala_version)
    with open('/tmp/python_version') as f:
        python_version = str(f.read()).rstrip()
        print(python_version)
    if r_enabled == 'true':
        s3_client.download_file(args.bucket, args.project_name + '/' + args.cluster_name + '/r_version', '/tmp/r_version')
        with open('/tmp/r_version') as g:
            r_version = str(g.read()).rstrip()
            print(r_version)
    else:
        r_version = 'false'
    cluster_id = get_emr_id_by_name(args.cluster_name)
    master_instances = get_emr_instances_list(cluster_id, 'MASTER')
    master_ip = master_instances[0].get('PrivateIpAddress')
    conn.sudo("/usr/bin/python3 /usr/local/bin/jupyter_dataengine-service_create_configs.py --bucket " + args.bucket
         + " --cluster_name " + args.cluster_name + " --emr_version " + args.emr_version + " --spark_version "
         + spark_version + " --scala_version " + scala_version + " --r_version " + r_version + " --hadoop_version "
         + hadoop_version + " --region " + args.region + " --excluded_lines '" + args.emr_excluded_spark_properties
         + "' --project_name " + args.project_name + " --os_user " + args.os_user + " --pip_mirror "
         + args.pip_mirror + " --numpy_version " + numpy_version + " --application "
         + args.application + " --master_ip " + master_ip + " --python_version " + python_version)
