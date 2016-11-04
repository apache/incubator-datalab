#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

import argparse
from fabric.api import *
import boto3
from dlab.aws_meta import *
import os

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
args = parser.parse_args()


def configure_notebook():
    templates_dir = '/root/templates/'
    scripts_dir = '/root/scripts/'
    put(templates_dir + 'pyspark_emr_template.json', '/tmp/pyspark_emr_template.json')
    put(templates_dir + 'spark-defaults_template.conf', '/tmp/spark-defaults_template.conf')
    put(templates_dir + 'toree_emr_template.json','/tmp/toree_emr_template.json')
    put(scripts_dir + 'create_configs.py', '/tmp/create_configs.py')
    put(templates_dir + 'toree_kernel.tar.gz', '/tmp/toree_kernel.tar.gz')
    put(templates_dir + 'toree_emr_templatev2.json', '/tmp/toree_emr_templatev2.json')
    put(templates_dir + 'run_template.sh', '/tmp/run_template.sh')
    sudo('\cp /tmp/create_configs.py /usr/local/bin/create_configs.py')
    sudo('chmod 755 /usr/local/bin/create_configs.py')


def get_spark_version():
    spark_version = ''
    emr = boto3.client('emr')
    clusters = emr.list_clusters(ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING'])
    clusters = clusters.get('Clusters')
    for i in clusters:
        response = emr.describe_cluster(ClusterId=i.get('Id'))
        if response.get("Cluster").get("Name") == args.cluster_name:
            response =  response.get("Cluster").get("Applications")
            for j in response:
                if j.get("Name") == 'Spark':
                    spark_version = j.get("Version")
    return spark_version


def get_hadoop_version():
    hadoop_version = ''
    emr = boto3.client('emr')
    clusters = emr.list_clusters(ClusterStates=['RUNNING', 'WAITING', 'STARTING', 'BOOTSTRAPPING'])
    clusters = clusters.get('Clusters')
    for i in clusters:
        response = emr.describe_cluster(ClusterId=i.get('Id'))
        if response.get("Cluster").get("Name") == args.cluster_name:
            response =  response.get("Cluster").get("Applications")
            for j in response:
                if j.get("Name") == 'Hadoop':
                    hadoop_version = j.get("Version")
    return hadoop_version[0:3]


if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env.user = "ubuntu"
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts
    configure_notebook()
    spark_version = get_spark_version()
    hadoop_version = get_hadoop_version()
    sudo('/usr/bin/python /usr/local/bin/create_configs.py --bucket ' + args.bucket + ' --cluster_name ' + args.cluster_name + ' --emr_version ' + args.emr_version + ' --spark_version ' + spark_version + ' --hadoop_version ' + hadoop_version)