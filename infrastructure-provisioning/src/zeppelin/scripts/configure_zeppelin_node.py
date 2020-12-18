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
import json
import os
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric.api import *
from fabric.contrib.files import exists

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--instance_name', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--zeppelin_version', type=str, default='')
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--proxy_port', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--livy_version', type=str, default='')
parser.add_argument('--multiple_clusters', type=str, default='')
parser.add_argument('--r_mirror', type=str, default='')
parser.add_argument('--endpoint_url', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--edge_ip', type=str, default='')
args = parser.parse_args()

spark_version = args.spark_version
hadoop_version = args.hadoop_version
scala_link = "http://www.scala-lang.org/files/archive/"
zeppelin_version = args.zeppelin_version
zeppelin_link = "http://archive.apache.org/dist/zeppelin/zeppelin-" + zeppelin_version + "/zeppelin-" + \
                zeppelin_version + "-bin-netinst.tgz"
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
zeppelin_interpreters = "md,python,shell"
python3_version = "3.4"
local_spark_path = '/opt/spark/'
templates_dir = '/root/templates/'
files_dir = '/root/files/'
jars_dir = '/opt/jars/'
r_libs = ['R6', 'pbdZMQ', 'RCurl', 'reshape2', 'caTools={}'.format(os.environ['notebook_catools_version']), 'rJava', 'ggplot2']
r_enabled = os.environ['notebook_r_enabled']
gitlab_certfile = os.environ['conf_gitlab_certfile']


def configure_zeppelin(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/zeppelin_ensured'):
        try:
            sudo('wget ' + zeppelin_link + ' -O /tmp/zeppelin-' + zeppelin_version + '-bin-netinst.tgz')
            sudo('tar -zxvf /tmp/zeppelin-' + zeppelin_version + '-bin-netinst.tgz -C /opt/')
            sudo('ln -s /opt/zeppelin-' + zeppelin_version + '-bin-netinst /opt/zeppelin')
            sudo('cp /opt/zeppelin/conf/zeppelin-env.sh.template /opt/zeppelin/conf/zeppelin-env.sh')
            java_home = run("update-alternatives --query java | grep -o \'/.*/java-8.*/jre\'").splitlines()[0]
            sudo("echo 'export JAVA_HOME=\'{}\'' >> /opt/zeppelin/conf/zeppelin-env.sh".format(java_home))
            sudo('cp /opt/zeppelin/conf/zeppelin-site.xml.template /opt/zeppelin/conf/zeppelin-site.xml')
            sudo('sed -i \"/# export ZEPPELIN_PID_DIR/c\export ZEPPELIN_PID_DIR=/var/run/zeppelin\" /opt/zeppelin/conf/zeppelin-env.sh')
            sudo('sed -i \"/# export ZEPPELIN_IDENT_STRING/c\export ZEPPELIN_IDENT_STRING=notebook\" /opt/zeppelin/conf/zeppelin-env.sh')
            sudo('sed -i \"/# export ZEPPELIN_INTERPRETER_DEP_MVNREPO/c\export ZEPPELIN_INTERPRETER_DEP_MVNREPO=https://repo1.maven.org/maven2\" /opt/zeppelin/conf/zeppelin-env.sh')
            sudo('sed -i \"/# export SPARK_HOME/c\export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
            sudo('sed -i \'s/127.0.0.1/0.0.0.0/g\' /opt/zeppelin/conf/zeppelin-site.xml')
            sudo('mkdir /var/log/zeppelin')
            sudo('mkdir /var/run/zeppelin')
            sudo('ln -s /var/log/zeppelin /opt/zeppelin-' + zeppelin_version + '-bin-netinst/logs')
            sudo('chown ' + os_user + ':' + os_user + ' -R /var/log/zeppelin')
            sudo('ln -s /var/run/zeppelin /opt/zeppelin-' + zeppelin_version + '-bin-netinst/run')
            sudo('chown ' + os_user + ':' + os_user + ' -R /var/run/zeppelin')
            sudo('/opt/zeppelin/bin/install-interpreter.sh --name ' + zeppelin_interpreters + ' --proxy-url $http_proxy')
            sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin-' + zeppelin_version + '-bin-netinst')
            sudo('cp /opt/zeppelin-' + zeppelin_version + '-bin-netinst/interpreter/md/zeppelin-markdown-*.jar /opt/zeppelin/lib/interpreter/') # necessary when executing paragraph launches java process with "-cp :/opt/zeppelin/lib/interpreter/*:"
            sudo('cp /opt/zeppelin-' + zeppelin_version + '-bin-netinst/interpreter/shell/zeppelin-shell-*.jar /opt/zeppelin/lib/interpreter/')
        except:
            sys.exit(1)
        try:
            put(templates_dir + 'zeppelin-notebook.service', '/tmp/zeppelin-notebook.service')
            sudo("sed -i 's|OS_USR|" + os_user + "|' /tmp/zeppelin-notebook.service")
            http_proxy = run('echo $http_proxy')
            https_proxy = run('echo $https_proxy')
            sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTP_PROXY={}\"\'  /tmp/zeppelin-notebook.service'.format(
                http_proxy))
            sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTPS_PROXY={}\"\'  /tmp/zeppelin-notebook.service'.format(
                https_proxy))
            sudo("chmod 644 /tmp/zeppelin-notebook.service")
            sudo('cp /tmp/zeppelin-notebook.service /etc/systemd/system/zeppelin-notebook.service')
            sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin/')
            sudo('mkdir -p /mnt/var')
            sudo('chown ' + os_user + ':' + os_user + ' /mnt/var')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable zeppelin-notebook")
            sudo('echo \"d /var/run/zeppelin 0755 ' + os_user + '\" > /usr/lib/tmpfiles.d/zeppelin.conf')
            sudo('touch /home/' + os_user + '/.ensure_dir/zeppelin_ensured')
        except:
            sys.exit(1)


def configure_local_livy_kernels(args):
    if not exists('/home/' + args.os_user + '/.ensure_dir/local_livy_kernel_ensured'):
        port_number_found = False
        default_port = 8998
        livy_port = ''
        put(templates_dir + 'interpreter_livy.json', '/tmp/interpreter.json')
        sudo('sed -i "s|ENDPOINTURL|' + args.endpoint_url + '|g" /tmp/interpreter.json')
        sudo('sed -i "s|OS_USER|' + args.os_user + '|g" /tmp/interpreter.json')
        spark_memory = get_spark_memory()
        sudo('sed -i "s|DRIVER_MEMORY|{}m|g" /tmp/interpreter.json'.format(spark_memory))
        while not port_number_found:
            port_free = sudo('nmap -p ' + str(default_port) + ' localhost | grep "closed" > /dev/null; echo $?')
            port_free = port_free[:1]
            if port_free == '0':
                livy_port = default_port
                port_number_found = True
            else:
                default_port += 1
        sudo('sed -i "s|LIVY_PORT|' + str(livy_port) + '|g" /tmp/interpreter.json')
        update_zeppelin_interpreters(args.multiple_clusters, r_enabled, 'local')
        sudo('cp -f /tmp/interpreter.json /opt/zeppelin/conf/interpreter.json')
        sudo('echo "livy.server.port = ' + str(livy_port) + '" >> /opt/livy/conf/livy.conf')
        sudo('''echo "SPARK_HOME='/opt/spark/'" >> /opt/livy/conf/livy-env.sh''')
        if exists('/opt/livy/conf/spark-blacklist.conf'):
            sudo('sed -i "s/^/#/g" /opt/livy/conf/spark-blacklist.conf')
        sudo("systemctl start livy-server")
        sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/zeppelin/')
        sudo('touch /home/' + args.os_user + '/.ensure_dir/local_livy_kernel_ensured')
    sudo("systemctl daemon-reload")
    sudo("systemctl start zeppelin-notebook")


def configure_local_spark_kernels(args):
    if not exists('/home/' + args.os_user + '/.ensure_dir/local_spark_kernel_ensured'):
        put(templates_dir + 'interpreter_spark.json', '/tmp/interpreter.json')
        sudo('sed -i "s|ENDPOINTURL|' + args.endpoint_url + '|g" /tmp/interpreter.json')
        sudo('sed -i "s|OS_USER|' + args.os_user + '|g" /tmp/interpreter.json')
        spark_memory = get_spark_memory()
        sudo('sed -i "s|DRIVER_MEMORY|{}m|g" /tmp/interpreter.json'.format(spark_memory))
        update_zeppelin_interpreters(args.multiple_clusters, r_enabled, 'local')
        sudo('cp -f /tmp/interpreter.json /opt/zeppelin/conf/interpreter.json')
        sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/zeppelin/')
        sudo('touch /home/' + args.os_user + '/.ensure_dir/local_spark_kernel_ensured')
    sudo("systemctl daemon-reload")
    sudo("systemctl start zeppelin-notebook")


def install_local_livy(args):
    if not exists('/home/' + args.os_user + '/.ensure_dir/local_livy_ensured'):
        sudo('wget http://archive.cloudera.com/beta/livy/livy-server-' + args.livy_version + '.zip -O /opt/livy-server-'
             + args.livy_version + '.zip')
        sudo('unzip /opt/livy-server-' + args.livy_version + '.zip -d /opt/')
        sudo('mv /opt/livy-server-' + args.livy_version + '/ /opt/livy/')
        sudo('mkdir -p /var/run/livy')
        sudo('mkdir -p /opt/livy/logs')
        sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /var/run/livy')
        sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/livy/')
        put(templates_dir + 'livy-server-cluster.service', '/tmp/livy-server-cluster.service')
        sudo('mv /tmp/livy-server-cluster.service /opt/')
        put(templates_dir + 'livy-server.service', '/tmp/livy-server.service')
        sudo("sed -i 's|OS_USER|" + args.os_user + "|' /tmp/livy-server.service")
        sudo("chmod 644 /tmp/livy-server.service")
        sudo('cp /tmp/livy-server.service /etc/systemd/system/livy-server.service')
        sudo("systemctl daemon-reload")
        sudo("systemctl enable livy-server")
        sudo('touch /home/' + args.os_user + '/.ensure_dir/local_livy_ensured')


##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname
    deeper_config = json.loads(args.additional_config)

    # PREPARE DISK
    print("Prepare .ensure directory")
    try:
        if not exists('/home/' + args.os_user + '/.ensure_dir'):
            sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)
    print("Mount additional volume")
    prepare_disk(args.os_user)

    # INSTALL LANGUAGES
    print("Install Java")
    ensure_jre_jdk(args.os_user)
    print("Installing Scala")
    ensure_scala(scala_link, args.scala_version, args.os_user)
    if os.environ['notebook_r_enabled'] == 'true':
        print("Installing R")
        ensure_r(args.os_user, r_libs, args.region, args.r_mirror)
    print("Install Python 2 modules")
    ensure_python2_libraries(args.os_user)
    print("Install Python 3 modules")
    ensure_python3_libraries(args.os_user)
    print("Install Python 3 specific version")
    ensure_python3_specific_version(python3_version, args.os_user)

    # INSTALL SPARK AND CLOUD STORAGE JARS FOR SPARK
    print("Install local Spark")
    ensure_local_spark(args.os_user, spark_link, args.spark_version, args.hadoop_version, local_spark_path)
    print("Install storage jars")
    ensure_local_jars(args.os_user, jars_dir)
    print("Configure local Spark")
    configure_local_spark(jars_dir, templates_dir)

    # INSTALL ZEPPELIN
    print("Install Zeppelin")
    configure_zeppelin(args.os_user)

    # INSTALL ZEPPELIN KERNELS
    if args.multiple_clusters == 'true':
        print("Installing Livy for local kernels")
        install_local_livy(args)
        print("Configuring local kernels")
        configure_local_livy_kernels(args)
    else:
        print("Configuring local kernels")
        configure_local_spark_kernels(args)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install Ungit")
    install_ungit(args.os_user, args.exploratory_name, args.edge_ip)
    if exists('/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)
    # COPY PRE-COMMIT SCRIPT TO ZEPPELIN
    sudo('cp /home/{}/.git/templates/hooks/pre-commit /opt/zeppelin/notebook/.git/hooks/'.format(args.os_user))

    # INSTALL INACTIVITY CHECKER
    print("Install inactivity checker")
    install_inactivity_checker(args.os_user, args.ip_address)

    # INSTALL OPTIONAL PACKAGES
    if os.environ['notebook_r_enabled'] == 'true':
        print("Install additional R packages")
        install_r_packages(args.os_user)
    print("Install additional Python packages")
    ensure_additional_python_libs(args.os_user)
    print("Install Matplotlib.")
    ensure_matplot(args.os_user)
    
    #POST INSTALLATION PROCESS
    print("Updating pyOpenSSL library")
    update_pyopenssl_lib(args.os_user)