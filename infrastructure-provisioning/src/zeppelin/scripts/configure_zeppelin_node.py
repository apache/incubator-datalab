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
from fabric import *
from patchwork.files import exists
from patchwork import files

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
parser.add_argument('--endpoint_url', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--edge_ip', type=str, default='')
args = parser.parse_args()

spark_version = args.spark_version
hadoop_version = args.hadoop_version
scala_link = "https://www.scala-lang.org/files/archive/"
zeppelin_version = args.zeppelin_version
zeppelin_link = "https://nexus.develop.dlabanalytics.com/repository/packages-public/zeppelin-"+ zeppelin_version +"-prebuilt.tar.gz"
python_venv_version = os.environ['notebook_python_venv_version']
python_venv_path = '/opt/python/python{0}/bin/python{1}'.format(python_venv_version, python_venv_version[:3])
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
zeppelin_interpreters = "md,python,shell"
python3_version = "3.8"
local_spark_path = '/opt/spark/'
templates_dir = '/root/templates/'
files_dir = '/root/files/'
jars_dir = '/opt/jars/'
r_libs = ['R6', 'pbdZMQ={}'.format(os.environ['notebook_pbdzmq_version']), 'RCurl', 'reshape2', 'caTools={}'.format(os.environ['notebook_catools_version']), 'rJava', 'ggplot2']
r_enabled = os.environ['notebook_r_enabled']
gitlab_certfile = os.environ['conf_gitlab_certfile']


def configure_zeppelin(os_user):
    if not exists(conn,'/home/' + os_user + '/.ensure_dir/zeppelin_ensured'):
        try:
            # test nexus
            conn.sudo('wget ' + zeppelin_link + ' -O /tmp/zeppelin-' + zeppelin_version + '-prebuilt.tar.gz')
            conn.sudo('tar -zxvf /tmp/zeppelin-' + zeppelin_version + '-prebuilt.tar.gz -C /opt/')
            conn.sudo('ln -s /opt/zeppelin-' + zeppelin_version + '-prebuilt.tar.gz /opt/zeppelin')
            conn.sudo('cp /opt/zeppelin/conf/zeppelin-env.sh.template /opt/zeppelin/conf/zeppelin-env.sh')
            java_home = conn.run("update-alternatives --query java | grep -o \'/.*/java-8.*/jre\'").stdout.splitlines()[0].replace('\n', '')
            conn.sudo('''bash -c "echo 'export JAVA_HOME=\'{}\'' >> /opt/zeppelin/conf/zeppelin-env.sh" '''.format(java_home))
            conn.sudo('cp /opt/zeppelin/conf/zeppelin-site.xml.template /opt/zeppelin/conf/zeppelin-site.xml')
            conn.sudo('sed -i \"/# export ZEPPELIN_PID_DIR/c\export ZEPPELIN_PID_DIR=/var/run/zeppelin\" /opt/zeppelin/conf/zeppelin-env.sh')
            conn.sudo('sed -i \"/# export ZEPPELIN_IDENT_STRING/c\export ZEPPELIN_IDENT_STRING=notebook\" /opt/zeppelin/conf/zeppelin-env.sh')
            conn.sudo('sed -i \"/# export ZEPPELIN_INTERPRETER_DEP_MVNREPO/c\export ZEPPELIN_INTERPRETER_DEP_MVNREPO=https://repo1.maven.org/maven2\" /opt/zeppelin/conf/zeppelin-env.sh')
            conn.sudo('sed -i \"/# export SPARK_HOME/c\export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
            conn.sudo('sed -i \'s/127.0.0.1/0.0.0.0/g\' /opt/zeppelin/conf/zeppelin-site.xml')
            conn.sudo('mkdir /var/log/zeppelin')
            conn.sudo('mkdir /var/run/zeppelin')
            conn.sudo('ln -s /var/log/zeppelin /opt/zeppelin/logs')
            conn.sudo('chown ' + os_user + ':' + os_user + ' -R /var/log/zeppelin')
            conn.sudo('ln -s /var/run/zeppelin /opt/zeppelin/run')
            conn.sudo('chown ' + os_user + ':' + os_user + ' -R /var/run/zeppelin')
            conn.sudo('''bash -l -c '/opt/zeppelin/bin/install-interpreter.sh --name {} --proxy-url $http_proxy' '''.format(zeppelin_interpreters))
            conn.sudo('''bash -l -c '/opt/zeppelin/bin/install-interpreter.sh --name sh --artifact /opt/zeppelin/interpreter/sh/zeppelin-shell-*.jar --proxy-url $http_proxy' ''')
            conn.sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin')
            conn.sudo('mkdir -p /opt/zeppelin/lib/interpreter/')
            conn.sudo('cp /opt/zeppelin/interpreter/md/zeppelin-markdown-*.jar /opt/zeppelin/lib/interpreter/')  # necessary when executing paragraph launches java process with "-cp :/opt/zeppelin/lib/interpreter/*:"
            conn.sudo('cp /opt/zeppelin/interpreter/sh/zeppelin-shell-*.jar /opt/zeppelin/lib/interpreter/')
            conn.sudo('rm -r /opt/zeppelin/interpreter/python/')
        except Exception as err:
            print('Error:', str(err))
            sys.exit(1)
        try:
            conn.put(templates_dir + 'zeppelin-notebook.service', '/tmp/zeppelin-notebook.service')
            conn.sudo("sed -i 's|OS_USR|" + os_user + "|' /tmp/zeppelin-notebook.service")
            http_proxy = conn.run('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n','')
            https_proxy = conn.run('''bash -l -c 'echo $https_proxy' ''').stdout.replace('\n','')
            conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTP_PROXY={}\"\'  /tmp/zeppelin-notebook.service'.format(
                http_proxy))
            conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTPS_PROXY={}\"\'  /tmp/zeppelin-notebook.service'.format(
                https_proxy))
            conn.sudo("chmod 644 /tmp/zeppelin-notebook.service")
            conn.sudo('cp /tmp/zeppelin-notebook.service /etc/systemd/system/zeppelin-notebook.service')
            conn.sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin/')
            conn.sudo('mkdir -p /mnt/var')
            conn.sudo('chown ' + os_user + ':' + os_user + ' /mnt/var')
            conn.sudo("systemctl daemon-reload")
            conn.sudo("systemctl enable zeppelin-notebook")
            conn.sudo('''bash -l -c 'echo \"d /var/run/zeppelin 0755 {}\" > /usr/lib/tmpfiles.d/zeppelin.conf' '''.format(os_user))
            conn.sudo('touch /home/' + os_user + '/.ensure_dir/zeppelin_ensured')
        except Exception as err:
            print('Error:', str(err))
            sys.exit(1)


def configure_local_livy_kernels(args):
    if not exists(conn,'/home/' + args.os_user + '/.ensure_dir/local_livy_kernel_ensured'):
        port_number_found = False
        default_port = 8998
        livy_port = ''
        conn.put(templates_dir + 'interpreter_livy.json', '/tmp/interpreter.json')
        conn.sudo('sed -i "s|ENDPOINTURL|' + args.endpoint_url + '|g" /tmp/interpreter.json')
        conn.sudo('sed -i "s|OS_USER|' + args.os_user + '|g" /tmp/interpreter.json')
        spark_memory = get_spark_memory()
        conn.sudo('sed -i "s|DRIVER_MEMORY|{}m|g" /tmp/interpreter.json'.format(spark_memory))
        while not port_number_found:
            port_free = conn.sudo('nmap -p ' + str(default_port) + ' localhost | grep "closed" > /dev/null; echo $?').stdout
            port_free = port_free[:1]
            if port_free == '0':
                livy_port = default_port
                port_number_found = True
            else:
                default_port += 1
        conn.sudo('sed -i "s|LIVY_PORT|' + str(livy_port) + '|g" /tmp/interpreter.json')
        update_zeppelin_interpreters(args.multiple_clusters, r_enabled, 'local')
        conn.sudo('cp -f /tmp/interpreter.json /opt/zeppelin/conf/interpreter.json')
        conn.sudo('echo "livy.server.port = ' + str(livy_port) + '" >> /opt/livy/conf/livy.conf')
        conn.sudo('''echo "SPARK_HOME='/opt/spark/'" >> /opt/livy/conf/livy-env.sh''')
        if exists(conn, '/opt/livy/conf/spark-blacklist.conf'):
            conn.sudo('sed -i "s/^/#/g" /opt/livy/conf/spark-blacklist.conf')
        conn.sudo("systemctl start livy-server")
        conn.sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/zeppelin/')
        conn.sudo('touch /home/' + args.os_user + '/.ensure_dir/local_livy_kernel_ensured')
    conn.sudo("systemctl daemon-reload")
    conn.sudo("systemctl start zeppelin-notebook")


def configure_local_spark_kernels(args, python_venv_path):
    if not exists(conn,'/home/' + args.os_user + '/.ensure_dir/local_spark_kernel_ensured'):
        conn.put(templates_dir + 'interpreter_spark.json', '/tmp/interpreter.json')
        conn.sudo('sed -i "s|ENDPOINTURL|' + args.endpoint_url + '|g" /tmp/interpreter.json')
        conn.sudo('sed -i "s|OS_USER|' + args.os_user + '|g" /tmp/interpreter.json')
        spark_memory = get_spark_memory()
        conn.sudo('sed -i "s|DRIVER_MEMORY|{}m|g" /tmp/interpreter.json'.format(spark_memory))
        conn.sudo('sed -i "s|PYTHON_VENV_PATH|{}|g" /tmp/interpreter.json'.format(python_venv_path))
        update_zeppelin_interpreters(args.multiple_clusters, r_enabled, 'local')
        conn.sudo('cp -f /tmp/interpreter.json /opt/zeppelin/conf/interpreter.json')
        conn.sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/zeppelin/')
        conn.sudo('touch /home/' + args.os_user + '/.ensure_dir/local_spark_kernel_ensured')
    conn.sudo("systemctl stop zeppelin-notebook")
    conn.sudo("systemctl daemon-reload")
    conn.sudo("systemctl enable zeppelin-notebook")
    conn.sudo("systemctl start zeppelin-notebook")


def install_local_livy(args):
    if not exists(conn,'/home/' + args.os_user + '/.ensure_dir/local_livy_ensured'):
        conn.sudo('wget http://archive.cloudera.com/beta/livy/livy-server-' + args.livy_version + '.zip -O /opt/livy-server-'
             + args.livy_version + '.zip')
        conn.sudo('unzip /opt/livy-server-' + args.livy_version + '.zip -d /opt/')
        conn.sudo('mv /opt/livy-server-' + args.livy_version + '/ /opt/livy/')
        conn.sudo('mkdir -p /var/run/livy')
        conn.sudo('mkdir -p /opt/livy/logs')
        conn.sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /var/run/livy')
        conn.sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/livy/')
        conn.put(templates_dir + 'livy-server-cluster.service', '/tmp/livy-server-cluster.service')
        conn.sudo('mv /tmp/livy-server-cluster.service /opt/')
        conn.put(templates_dir + 'livy-server.service', '/tmp/livy-server.service')
        conn.sudo("sed -i 's|OS_USER|" + args.os_user + "|' /tmp/livy-server.service")
        conn.sudo("chmod 644 /tmp/livy-server.service")
        conn.sudo('cp /tmp/livy-server.service /etc/systemd/system/livy-server.service')
        conn.sudo("systemctl daemon-reload")
        conn.sudo("systemctl enable livy-server")
        conn.sudo('touch /home/' + args.os_user + '/.ensure_dir/local_livy_ensured')


##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)
    deeper_config = json.loads(args.additional_config)

    # PREPARE DISK
    print("Prepare .ensure directory")
    try:
        if not exists(conn,'/home/' + args.os_user + '/.ensure_dir'):
            conn.sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
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
        ensure_r(args.os_user, r_libs)
    print("Install Python 3 modules")
    ensure_python3_libraries(args.os_user)

    # INSTALL PYTHON IN VIRTUALENV
    print("Configure Python Virtualenv")
    ensure_python_venv(python_venv_version)
    #print("Install Python 3 specific version")
    #ensure_python3_specific_version(python3_version, args.os_user)

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
        configure_local_spark_kernels(args, python_venv_path)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install Ungit")
    install_ungit(args.os_user, args.exploratory_name, args.edge_ip)
    if exists(conn, '/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)
    # COPY PRE-COMMIT SCRIPT TO ZEPPELIN
    conn.sudo('cp /home/{}/.git/templates/hooks/pre-commit /opt/zeppelin/notebook/.git/hooks/'.format(args.os_user))

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

    conn.close()
