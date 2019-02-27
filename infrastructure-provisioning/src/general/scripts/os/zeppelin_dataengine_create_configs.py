#!/usr/bin/python

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
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--livy_version', type=str, default='')
parser.add_argument('--multiple_clusters', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='')
parser.add_argument('--r_enabled', type=str, default='')
parser.add_argument('--spark_configurations', type=str, default='')
args = parser.parse_args()

cluster_dir = '/opt/' + args.cluster_name + '/'
local_jars_dir = '/opt/jars/'
spark_version = args.spark_version
hadoop_version = args.hadoop_version
spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
             "-bin-hadoop" + hadoop_version + ".tgz"


def configure_zeppelin_dataengine_interpreter(cluster_name, cluster_dir, os_user, multiple_clusters, spark_master):
    try:
        port_number_found = False
        zeppelin_restarted = False
        default_port = 8998
        livy_port = ''
        livy_path = '/opt/' + cluster_name + '/livy/'
        local('echo \"Configuring Data Engine path for Zeppelin\"')
        local('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/' + cluster_name +
              '\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
        local('sudo chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin/')
        local('sudo systemctl daemon-reload')
        local('sudo service zeppelin-notebook stop')
        local('sudo service zeppelin-notebook start')
        while not zeppelin_restarted:
            local('sleep 5')
            result = local('sudo bash -c "nmap -p 8080 localhost | grep closed > /dev/null" ; echo $?', capture=True)
            result = result[:1]
            if result == '1':
                zeppelin_restarted = True
        local('sleep 5')
        local('echo \"Configuring Data Engine spark interpreter for Zeppelin\"')
        if multiple_clusters == 'true':
            while not port_number_found:
                port_free = local('sudo bash -c "nmap -p ' + str(default_port) +
                                  ' localhost | grep closed > /dev/null" ; echo $?', capture=True)
                port_free = port_free[:1]
                if port_free == '0':
                    livy_port = default_port
                    port_number_found = True
                else:
                    default_port += 1
            local('sudo echo "livy.server.port = ' + str(livy_port) + '" >> ' + livy_path + 'conf/livy.conf')
            local('sudo echo "livy.spark.master = ' + spark_master + '" >> ' + livy_path + 'conf/livy.conf')
            if os.path.exists(livy_path + 'conf/spark-blacklist.conf'):
                local('sudo sed -i "s/^/#/g" ' + livy_path + 'conf/spark-blacklist.conf')
            local(''' sudo echo "export SPARK_HOME=''' + cluster_dir + '''spark/" >> ''' + livy_path + '''conf/livy-env.sh''')
            local(''' sudo echo "export PYSPARK3_PYTHON=python3.5" >> ''' +
                  livy_path + '''conf/livy-env.sh''')
            template_file = "/tmp/{}/dataengine_interpreter.json".format(args.cluster_name)
            fr = open(template_file, 'r+')
            text = fr.read()
            text = text.replace('CLUSTER_NAME', cluster_name)
            text = text.replace('SPARK_HOME', cluster_dir + 'spark/')
            text = text.replace('LIVY_PORT', str(livy_port))
            text = text.replace('MASTER', str(spark_master))
            fw = open(template_file, 'w')
            fw.write(text)
            fw.close()
            for _ in range(5):
                try:
                    local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                          "@/tmp/{}/dataengine_interpreter.json http://localhost:8080/api/interpreter/setting".format(args.cluster_name))
                    break
                except:
                    local('sleep 5')
            local('sudo cp /opt/livy-server-cluster.service /etc/systemd/system/livy-server-' + str(livy_port) +
                  '.service')
            local("sudo sed -i 's|OS_USER|" + os_user + "|' /etc/systemd/system/livy-server-" + str(livy_port) +
                  '.service')
            local("sudo sed -i 's|LIVY_PATH|" + livy_path + "|' /etc/systemd/system/livy-server-" + str(livy_port)
                  + '.service')
            local('sudo chmod 644 /etc/systemd/system/livy-server-' + str(livy_port) + '.service')
            local("sudo systemctl daemon-reload")
            local("sudo systemctl enable livy-server-" + str(livy_port))
            local('sudo systemctl start livy-server-' + str(livy_port))
        else:
            template_file = "/tmp/{}/dataengine_interpreter.json".format(args.cluster_name)
            p_versions = ["2", "3.5"]
            for p_version in p_versions:
                fr = open(template_file, 'r+')
                text = fr.read()
                text = text.replace('CLUSTERNAME', cluster_name)
                text = text.replace('PYTHONVERSION', p_version)
                text = text.replace('SPARK_HOME', cluster_dir + 'spark/')
                text = text.replace('PYTHONVER_SHORT', p_version[:1])
                text = text.replace('MASTER', str(spark_master))
                tmp_file = "/tmp/dataengine_spark_py" + p_version + "_interpreter.json"
                fw = open(tmp_file, 'w')
                fw.write(text)
                fw.close()
                for _ in range(5):
                    try:
                        local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                              "@/tmp/dataengine_spark_py" + p_version +
                              "_interpreter.json http://localhost:8080/api/interpreter/setting")
                        break
                    except:
                        local('sleep 5')
        local('touch /home/' + os_user + '/.ensure_dir/dataengine_' + cluster_name + '_interpreter_ensured')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)


def install_remote_livy(args):
    local('sudo chown ' + args.os_user + ':' + args.os_user + ' -R /opt/zeppelin/')
    local('sudo service zeppelin-notebook stop')
    local('sudo -i wget http://archive.cloudera.com/beta/livy/livy-server-' + args.livy_version + '.zip -O /opt/' +
          args.cluster_name + '/livy-server-' + args.livy_version + '.zip')
    local('sudo unzip /opt/' + args.cluster_name + '/livy-server-' + args.livy_version + '.zip -d /opt/' +
          args.cluster_name + '/')
    local('sudo mv /opt/' + args.cluster_name + '/livy-server-' + args.livy_version + '/ /opt/' + args.cluster_name +
          '/livy/')
    livy_path = '/opt/' + args.cluster_name + '/livy/'
    local('sudo mkdir -p ' + livy_path + '/logs')
    local('sudo mkdir -p /var/run/livy')
    local('sudo chown ' + args.os_user + ':' + args.os_user + ' -R /var/run/livy')
    local('sudo chown ' + args.os_user + ':' + args.os_user + ' -R ' + livy_path)


if __name__ == "__main__":
    dataengine_dir_prepare('/opt/{}/'.format(args.cluster_name))
    install_dataengine_spark(args.cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, args.os_user,
                             args.datalake_enabled)
    configure_dataengine_spark(args.cluster_name, local_jars_dir, cluster_dir, args.datalake_enabled,
                               args.spark_configurations)
    if args.multiple_clusters == 'true':
        install_remote_livy(args)
    configure_zeppelin_dataengine_interpreter(args.cluster_name, cluster_dir, args.os_user,
                                              args.multiple_clusters, args.spark_master)
    update_zeppelin_interpreters(args.multiple_clusters, args.r_enabled)