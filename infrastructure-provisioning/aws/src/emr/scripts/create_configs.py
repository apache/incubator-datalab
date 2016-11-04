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

import boto3
from fabric.api import *
import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
args = parser.parse_args()

emr_dir = '/opt/' + args.emr_version + '/jars/'
kernels_dir = '/home/ubuntu/.local/share/jupyter/kernels/'
yarn_dir = '/srv/hadoopconf/'
if args.emr_version == 'emr-4.3.0' or args.emr_version == 'emr-4.6.0' or args.emr_version == 'emr-4.8.0':
    hadoop_version = '2.6'
else:
    hadoop_version = args.hadoop_version
spark_link = "http://d3kbcqa49mib13.cloudfront.net/spark-" + args.spark_version + "-bin-hadoop" + hadoop_version + ".tgz"


def install_emr_spark(args):
    local('wget ' + spark_link + ' -O /tmp/spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
    local('mkdir -p /opt/' + args.emr_version)
    local('tar -zxvf /tmp/spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/' + args.emr_version + '/')


def prepare():
    local('mkdir -p ' + yarn_dir)
    local('mkdir -p ' + emr_dir)
    result = os.path.exists(emr_dir + args.emr_version + "/aws")
    return result


def jars(args):
    print "Downloading jars..."
    s3_client = boto3.client('s3')
    s3_client.download_file(args.bucket, 'jars/' + args.emr_version + '/jars.tar.gz', '/tmp/jars.tar.gz')
    local('tar -zhxvf /tmp/jars.tar.gz -C ' + emr_dir)


def yarn(args):
    print "Downloading yarn configuration..."
    s3client = boto3.client('s3')
    s3resource = boto3.resource('s3')
    get_files(s3client, s3resource, 'config/{}/'.format(args.cluster_name), args.bucket, yarn_dir)


def pyspark_kernel(args):
    local('mkdir -p ' + kernels_dir + 'pyspark_' + args.cluster_name + '/')
    kernel_path = kernels_dir + "pyspark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/pyspark_emr_template.json"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('SPARK_PATH',
                        '/opt/' + args.emr_version + '/' + 'spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '/')
    text = text.replace('PY_VER', '2.7')
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/kernel_var.json')
    local(
        "PYJ=`find /opt/" + args.emr_version + "/ -name '*py4j*.zip'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
    local('sudo mv /tmp/kernel_var.json ' + kernel_path)
    s3_client = boto3.client('s3')
    s3_client.download_file(args.bucket, 'python_version', '/tmp/python_version')
    with file('/tmp/python_version') as f:
        python_version = f.read()
    python_version = python_version[0:3]
    if python_version == '3.4':
        local('mkdir -p ' + kernels_dir + 'py3spark_' + args.cluster_name + '/')
        kernel_path = kernels_dir + "py3spark_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/pyspark_emr_template.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH',
                            '/opt/' + args.emr_version + '/' + 'spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '/')
        text = text.replace('PY_VER', '3.4')
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local(
            "PYJ=`find /opt/" + args.emr_version + "/ -name '*py4j*.zip'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
        local('sudo mv /tmp/kernel_var.json ' + kernel_path)
    elif python_version == '3.5':
        local('mkdir -p ' + kernels_dir + 'py3spark_' + args.cluster_name + '/')
        kernel_path = kernels_dir + "py3spark_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/pyspark_emr_template.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH',
                            '/opt/' + args.emr_version + '/' + 'spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '/')
        text = text.replace('PY_VER', '3.5')
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local(
            "PYJ=`find /opt/" + args.emr_version + "/ -name '*py4j*.zip'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
        local('sudo mv /tmp/kernel_var.json ' + kernel_path)


def toree_kernel(args):
    if args.emr_version == 'emr-4.3.0' or args.emr_version == 'emr-4.6.0' or args.emr_version == 'emr-4.8.0':
        local('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/')
        kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/toree_emr_template.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH',
                            '/opt/' + args.emr_version + '/' + 'spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '/')
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local(
            "PYJ=`find /opt/" + args.emr_version + "/ -name '*py4j*.zip'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
        local('sudo mv /tmp/kernel_var.json ' + kernel_path)
    else:
        local('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/')
        local('tar zxvf /tmp/toree_kernel.tar.gz -C ' + kernels_dir + 'toree_' + args.cluster_name + '/')
        kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/toree_emr_templatev2.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH',
                            '/opt/' + args.emr_version + '/' + 'spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '/')
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local(
            "PYJ=`find /opt/" + args.emr_version + "/ -name '*py4j*.zip'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json")
        local('sudo mv /tmp/kernel_var.json ' + kernel_path)
        run_sh_path = kernels_dir + "toree_" + args.cluster_name + "/bin/run.sh"
        template_sh_file = '/tmp/run_template.sh'
        with open(template_sh_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER', args.cluster_name)
        with open(run_sh_path, 'w') as f:
            f.write(text)


def get_files(s3client, s3resource, dist, bucket, local):
    s3list = s3client.get_paginator('list_objects')
    for result in s3list.paginate(Bucket=bucket, Delimiter='/', Prefix=dist):
        if result.get('CommonPrefixes') is not None:
            for subdir in result.get('CommonPrefixes'):
                get_files(s3client, s3resource, subdir.get('Prefix'), bucket, local)
        if result.get('Contents') is not None:
            for file in result.get('Contents'):
                if not os.path.exists(os.path.dirname(local + os.sep + file.get('Key'))):
                     os.makedirs(os.path.dirname(local + os.sep + file.get('Key')))
                s3resource.meta.client.download_file(bucket, file.get('Key'), local + os.sep + file.get('Key'))


def spark_defaults(args):
    missed_jar_path1 = '/opt/' + args.emr_version + '/jars/usr/lib/hadoop/client/*'
    missed_jar_path2 = '/opt/' + args.emr_version + '/jars/usr/lib/hadoop/*'
    spark_def_path = '/opt/' + args.emr_version + '/' + 'spark-' + args.spark_version + '-bin-hadoop' + hadoop_version + '/conf/spark-defaults.conf'
    s3_client = boto3.client('s3')
    s3_client.download_file(args.bucket, 'spark-defaults.conf', '/tmp/spark-defaults-emr.conf')
    local('touch /tmp/spark-defaults-temporary.conf')
    local('cat  /tmp/spark-defaults-emr.conf | grep spark.driver.extraClassPath |  tr "[ :]" "\\n" | sed "/^$/d" | sed "s|^|/opt/EMRVERSION/jars|g" | tr "\\n" ":" | sed "s|/opt/EMRVERSION/jars||1" | sed "s/\(.*\)\:/\\1 /" | sed "s|:|    |1" | sed "r|$|" | sed "s|$|:MISSEDJAR1|" | sed "s|$|:MISSEDJAR2|" | sed "s|\(.*\)\ |\\1|" > /tmp/spark-defaults-temporary.conf')
    local('printf "\\n"')
    local('cat /tmp/spark-defaults-emr.conf | grep spark.driver.extraLibraryPath |  tr "[ :]" "\\n" | sed "/^$/d" | sed "s|^|/opt/EMRVERSION/jars|g" | tr "\\n" ":" | sed "s|/opt/EMRVERSION/jars||1" | sed "s/\(.*\)\:/\\1 /" | sed "s|:|    |1" | sed "r|$|" | sed "s|\(.*\)\ |\\1|" >> /tmp/spark-defaults-temporary.conf')
    template_file = "/tmp/spark-defaults-temporary.conf"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('EMRVERSION', args.emr_version)
    text = text.replace('MISSEDJAR1', missed_jar_path1)
    text = text.replace('MISSEDJAR2', missed_jar_path2)
    with open(spark_def_path, 'w') as f:
        f.write(text)

if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare()
        if result == False :
            jars(args)
        yarn(args)
        install_emr_spark(args)
        pyspark_kernel(args)
        toree_kernel(args)
        spark_defaults(args)