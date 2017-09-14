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
import json
import sys
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
import os


parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--r_mirror', type=str, default='')
parser.add_argument('--master_ip', type=str, default='')
parser.add_argument('--node_type', type=str, default='')
args = parser.parse_args()

spark_version = args.spark_version
hadoop_version = args.hadoop_version
scala_link = "http://www.scala-lang.org/files/archive/"
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "http://d3kbcqa49mib13.cloudfront.net/spark-" + spark_version + "-bin-hadoop" + hadoop_version + ".tgz"

templates_dir = '/root/templates/'
files_dir = '/root/files/'
local_spark_path = '/opt/spark/'
jars_dir = '/opt/jars/'
r_libs = ['R6', 'pbdZMQ', 'RCurl', 'devtools', 'reshape2', 'caTools', 'rJava', 'ggplot2']


def configure_spark_master(os_user, master_ip):
    if not exists('/home/{}/.ensure_dir/spark_master_ensured'.format(os_user)):
        run('mv /opt/spark/conf/spark-env.sh.template /opt/spark/conf/spark-env.sh')
        run('''echo "SPARK_MASTER_IP='{}'"'''.format(master_ip))
        run('/opt/spark/sbin/start-master.sh')
        sudo('touch /home/{}/.ensure_dir/spark_master_ensured'.format(os_user))


def start_spark_slave(os_user, master_ip):
    if not exists('/home/{}/.ensure_dir/spark_slave_ensured'.format(os_user)):
        run('/opt/spark/sbin/start-slave.sh spark://{}:7077'.format(master_ip))
        sudo('touch /home/{}/.ensure_dir/spark_slave_ensured'.format(os_user))


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    print "Configuring notebook server."
    try:
        if not exists('/home/' + args.os_user + '/.ensure_dir'):
            sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)

    print "Install Java"
    ensure_jre_jdk(args.os_user)

    print "Install local Spark"
    ensure_local_spark(args.os_user, spark_link, spark_version, hadoop_version, local_spark_path)

    print "Install local jars"
    ensure_local_jars(args.os_user, jars_dir, files_dir, args.region, templates_dir)

    print "Install Scala"
    ensure_scala(scala_link, args.scala_version, args.os_user)

    print "Install python2 libraries"
    ensure_python2_libraries(args.os_user)

    print "Install python3 libraries"
    ensure_python3_libraries(args.os_user)

    print "Installing R"
    ensure_r(args.os_user, r_libs, args.region, args.r_mirror)

    if args.node_type == 'master':
        print "Configuring Spark"
        configure_spark_master(args.os_user, args.hostname)
    elif args.node_type == 'slave':
        print "Starting Spark"
        start_spark_slave(args.os_user, args.master_ip)
