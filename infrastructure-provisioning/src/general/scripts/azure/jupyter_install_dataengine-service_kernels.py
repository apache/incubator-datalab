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
import os
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.fab import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--hdinsight_version', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--edge_user_name', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--proxy_port', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--headnode_ip', type=str, default='')
args = parser.parse_args()

def configure_notebook(args):
    templates_dir = '/root/templates/'
    files_dir = '/root/files/'
    scripts_dir = '/root/scripts/'
    datalab.fab.conn.put(templates_dir + 'dataengine-service_sparkmagic_config.json', '/tmp/dataengine-service_sparkmagic_config.json')
    datalab.fab.conn.put(scripts_dir + '{}_dataengine-service_create_configs.py'.format(args.application), '/tmp/create_configs.py')
    datalab.fab.conn.sudo('\cp /tmp/create_configs.py /usr/local/bin/create_configs.py')
    datalab.fab.conn.sudo('chmod 755 /usr/local/bin/create_configs.py')
    datalab.fab.conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
    datalab.fab.conn.run('mkdir -p /tmp/datalab_libs/')
    host_string = args.os_user + "@" + args.notebook_ip
    datalab.fab.conn.local('rsync -e "ssh -i {}" /usr/lib/python3.8/datalab/*.py {}:/tmp/datalab_libs/'.format(args.keyfile, host_string))
    datalab.fab.conn.run('chmod a+x /tmp/datalab_libs/*')
    datalab.fab.conn.sudo('mv /tmp/datalab_libs/* /usr/lib/python3.8/datalab/')
    if exists(datalab.fab.conn, '/usr/lib64'):
        datalab.fab.conn.sudo('mkdir -p /usr/lib64/python3.8')
        datalab.fab.conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')

def install_sparkamagic_kernels(args):
    try:
        datalab.fab.conn.sudo('jupyter nbextension enable --py --sys-prefix widgetsnbextension')
        sparkmagic_dir = datalab.fab.conn.sudo(''' bash -l -c 'pip3 show sparkmagic | grep "Location: "' ''').stdout.rstrip("\n\r").split(' ')[1]
        datalab.fab.conn.sudo('jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user))
        datalab.fab.conn.sudo('jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user))
        #datalab.fab.conn.sudo('jupyter-kernelspec install {}/sparkmagic/kernels/sparkrkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user))
        pyspark_kernel_name = 'PySpark (Python-{0} / Spark-{1} ) [{2}]'.format(args.python_version, args.spark_version,
                                                                         args.cluster_name)
        datalab.fab.conn.sudo('sed -i \'s|PySpark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/pysparkkernel/kernel.json'.format(
            pyspark_kernel_name, args.os_user))
        spark_kernel_name = 'Spark (Scala-{0} / Spark-{1} ) [{2}]'.format(args.scala_version, args.spark_version,
                                                                         args.cluster_name)
        datalab.fab.conn.sudo('sed -i \'s|Spark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkkernel/kernel.json'.format(
            spark_kernel_name, args.os_user))
        #sparkr_kernel_name = 'SparkR (R-{0} / Spark-{1} ) [{2}]'.format(args.r_version, args.spark_version,
        #                                                                   args.cluster_name)
        #datalab.fab.conn.sudo('sed -i \'s|SparkR|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkrkernel/kernel.json'.format(
        #    sparkr_kernel_name, args.os_user))
        datalab.fab.conn.sudo('mkdir -p /home/' + args.os_user + '/.sparkmagic')
        datalab.fab.conn.sudo('cp -f /tmp/dataengine-service_sparkmagic_config.json /home/' + args.os_user + '/.sparkmagic/config.json')
        datalab.fab.conn.sudo('sed -i \'s|HEADNODEIP:PORT|{0}:{2}|g\' /home/{1}/.sparkmagic/config.json'.format(
                args.master_ip, args.os_user, args.livy_port))
        datalab.fab.conn.sudo('chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/sparkmagic_kernels_ensured'.format(args.os_user))
    except:
        sys.exit(1)


if __name__ == "__main__":
    global conn
    conn = init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)
    configure_notebook(args)
    args.spark_version = '3.1.2'
    args.python_version = '3.8.10'
    args.livy_port = '8998'
    args.master_ip = args.headnode_ip
    install_sparkamagic_kernels(args)
