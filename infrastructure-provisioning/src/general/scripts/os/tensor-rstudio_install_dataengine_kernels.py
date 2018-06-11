#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

import argparse
from fabric.api import *
from fabric.contrib.files import exists
from dlab.meta_lib import *
import os
from dlab.fab import *

parser = argparse.ArgumentParser()
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='false')
args = parser.parse_args()


def configure_notebook(keyfile, hoststring):
    scripts_dir = '/root/scripts/'
    templates_dir = '/root/templates/'
    put(scripts_dir + 'tensor-rstudio_dataengine_create_configs.py', '/tmp/tensor-rstudio_dataengine_create_configs.py')
    put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
    spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
    spark_memory = get_spark_memory(True, args.os_user, spark_master_ip, keyfile)
    run('echo "spark.executor.memory {0}m" >> /tmp/notebook_spark-defaults_local.conf'.format(spark_memory))
    sudo('\cp /tmp/tensor-rstudio_dataengine_create_configs.py /usr/local/bin/tensor-rstudio_dataengine_create_configs.py')
    sudo('chmod 755 /usr/local/bin/tensor-rstudio_dataengine_create_configs.py')
    sudo('mkdir -p /usr/lib/python2.7/dlab/')
    run('mkdir -p /tmp/dlab_libs/')
    local('scp -i {} /usr/lib/python2.7/dlab/* {}:/tmp/dlab_libs/'.format(keyfile, hoststring))
    run('chmod a+x /tmp/dlab_libs/*')
    sudo('mv /tmp/dlab_libs/* /usr/lib/python2.7/dlab/')
    if exists('/usr/lib64'):
        sudo('ln -fs /usr/lib/python2.7/dlab /usr/lib64/python2.7/dlab')


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
    sudo("/usr/bin/python /usr/local/bin/tensor-rstudio_dataengine_create_configs.py "
         "--cluster_name {} --spark_version {} --hadoop_version {} --os_user {} --spark_master {} --region {} --datalake_enabled {}".
         format(args.cluster_name, args.spark_version, args.hadoop_version, args.os_user, args.spark_master, region,
                args.datalake_enabled))
