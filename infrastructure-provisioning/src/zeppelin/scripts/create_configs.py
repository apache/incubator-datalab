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

import boto3
from botocore.client import Config
from fabric.api import *
import argparse
import os
import sys
import time
from fabric.api import lcd
from fabric.contrib.files import exists
from fabvenv import virtualenv
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
from dlab.common_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--excluded_lines', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

emr_dir = '/opt/' + args.emr_version + '/jars/'
kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
spark_dir = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/'
yarn_dir = '/opt/' + args.emr_version + '/' + args.cluster_name + '/conf/'


def configure_zeppelin_emr_interpreter(args):
    try:
        zeppelin_restarted = False
        spark_libs = "/opt/" + args.emr_version + "/jars/usr/share/aws/aws-java-sdk/aws-java-sdk-core*.jar /opt/" + args.emr_version + "/jars/usr/lib/hadoop/hadoop-aws*.jar /opt/" + args.emr_version + "/jars/usr/share/aws/aws-java-sdk/aws-java-sdk-s3-*.jar /opt/" + args.emr_version + "/jars/usr/lib/hadoop-lzo/lib/hadoop-lzo-*.jar"
        local('echo \"Configuring emr path for Zeppelin\"')
        local('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/' + args.emr_version + '\/' + args.cluster_name + '\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
        local('sed -i \"s/^export HADOOP_CONF_DIR.*/export HADOOP_CONF_DIR=\/opt\/' + args.emr_version + '\/' + args.cluster_name + '\/conf/\" /opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-env.sh')
        local('echo \"spark.jars $(ls ' + spark_libs + ' | tr \'\\n\' \',\')\" >> /opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-defaults.conf')
        local('echo \"spark.executorEnv.PYTHONPATH pyspark.zip:py4j-src.zip\" >> /opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-defaults.conf')
        local('sed -i \'/spark.yarn.dist.files/s/$/,file:\/opt\/' + args.emr_version + '\/' + args.cluster_name + '\/spark\/python\/lib\/py4j-src.zip,file:\/opt\/' + args.emr_version + '\/' + args.cluster_name + '\/spark\/python\/lib\/pyspark.zip/\' /opt/' + args.emr_version + '/' +  args.cluster_name + '/spark/conf/spark-defaults.conf')
        local('sudo chown ' + args.os_user + ':' + args.os_user + ' -R /opt/zeppelin/')
        local('sudo service zeppelin-notebook restart')
        while not zeppelin_restarted:
            result = local('nc -z localhost 8080; echo $?', capture=True)
            if result == '0':
                zeppelin_restarted = True
        local('echo \"Configuring emr spark interpreter for Zeppelin\"')
        template_file = "/tmp/emr_spark_interpreter.json"
        p_versions = ["2"]
        for p_version in p_versions:
            fr = open(template_file, 'r+')
            text = fr.read()
            text = text.replace('CLUSTERNAME', args.cluster_name)
            text = text.replace('PYTHONVERSION', p_version)
            text = text.replace('EMRVERSION', args.cluster_name)
            tmp_file = "/tmp/emr_spark_py" + p_version + "_interpreter.json"
            fw = open(tmp_file, 'w')
            fw.write(text)
            fw.close()
            for _ in range(5):
                try:
                    local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                          "@/tmp/emr_spark_py" + p_version +
                          "_interpreter.json http://localhost:8080/api/interpreter/setting")
                    break
                except:
                    local('sleep 5')
                    pass
        local('touch /home/' + args.os_user + '/.ensure_dir/emr_' + args.cluster_name + '_interpreter_ensured')
    except:
            sys.exit(1)


if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        result = prepare(emr_dir, yarn_dir)
        if result == False :
            jars(args, emr_dir)
        yarn(args, yarn_dir)
        install_emr_spark(args)
        spark_defaults(args)
        configuring_notebook(args)
        configure_zeppelin_emr_interpreter(args)
