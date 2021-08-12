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
from datalab.fab import *
from datalab.meta_lib import *
from fabric import *
from patchwork.files import exists
from patchwork import files

parser = argparse.ArgumentParser()
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='false')
parser.add_argument('--spark_master_ip', type=str, default='')
args = parser.parse_args()


def configure_notebook(keyfile, hoststring):
    templates_dir = '/root/templates/'
    scripts_dir = '/root/scripts/'
    conn.run('mkdir -p /tmp/{}/'.format(args.cluster_name))
    conn.put(templates_dir + 'sparkmagic_config_template.json', '/tmp/sparkmagic_config_template.json')
    # conn.put(templates_dir + 'pyspark_dataengine_template.json', '/tmp/{}/pyspark_dataengine_template.json'.format(args.cluster_name))
    # conn.put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/{}/notebook_spark-defaults_local.conf'.format(args.cluster_name))
    spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
    # spark_memory = get_spark_memory(True, args.os_user, spark_master_ip, keyfile)
    # conn.run('echo "spark.executor.memory {0}m" >> /tmp/{1}/notebook_spark-defaults_local.conf'.format(spark_memory, args.cluster_name))
    if not exists(conn,'/usr/local/bin/tensor_dataengine_create_configs.py'):
        conn.put(scripts_dir + 'tensor_dataengine_create_configs.py', '/tmp/tensor_dataengine_create_configs.py')
        conn.sudo('cp -f /tmp/tensor_dataengine_create_configs.py /usr/local/bin/tensor_dataengine_create_configs.py')
        conn.sudo('chmod 755 /usr/local/bin/tensor_dataengine_create_configs.py')
    if not exists(conn,'/usr/lib/python3.8/datalab/'):
        conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
        conn.local('cd  /usr/lib/python3.8/datalab/; tar -zcvf /tmp/datalab.tar.gz *')
        conn.put('/tmp/datalab.tar.gz', '/tmp/datalab.tar.gz')
        conn.sudo('tar -zxvf /tmp/datalab.tar.gz -C /usr/lib/python3.8/datalab/')
        conn.sudo('chmod a+x /usr/lib/python3.8/datalab/*')
        if exists(conn, '/usr/lib64'):
            conn.sudo('mkdir -p /usr/lib64/python3.8')
            conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')

def install_sparkamagic_kernels(args):
    try:
        datalab.fab.conn.sudo('sudo jupyter nbextension enable --py --sys-prefix widgetsnbextension')
        sparkmagic_dir = datalab.fab.conn.sudo(''' bash -l -c 'pip3 show sparkmagic | grep "Location: "' ''').stdout.rstrip("\n\r").split(' ')[1]
        datalab.fab.conn.sudo('jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user))
        datalab.fab.conn.sudo('jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user))
        #datalab.fab.conn.sudo('jupyter-kernelspec install {}/sparkmagic/kernels/sparkrkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user))
        pyspark_kernel_name = 'PySpark (Python-{2} / Spark-{0} ) [{1}]'.format(args.spark_version,
                                                                         args.cluster_name, os.environ['notebook_python_venv_version'][:3])
        datalab.fab.conn.sudo('sed -i \'s|PySpark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/pysparkkernel/kernel.json'.format(
            pyspark_kernel_name, args.os_user))
        scala_version = datalab.fab.conn.sudo('''bash -l -c 'spark-submit --version 2>&1 | grep -o -P "Scala version \K.{0,7}"' ''').stdout.rstrip("\n\r")
        spark_kernel_name = 'Spark (Scala-{0} / Spark-{1} ) [{2}]'.format(scala_version, args.spark_version,
                                                                         args.cluster_name)
        datalab.fab.conn.sudo('sed -i \'s|Spark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkkernel/kernel.json'.format(
            spark_kernel_name, args.os_user))
        #r_version = datalab.fab.conn.sudo(''' bash -l -c 'R --version | grep -o -P "R version \K.{0,5}"' ''').stdout.rstrip("\n\r")
        #sparkr_kernel_name = 'SparkR (R-{0} / Spark-{1} ) [{2}]'.format(r_version, args.spark_version,
        #                                                                    args.cluster_name)
        #datalab.fab.conn.sudo('sed -i \'s|SparkR|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkrkernel/kernel.json'.format(
        #    sparkr_kernel_name, args.os_user))
        datalab.fab.conn.sudo('sudo mv -f /home/{0}/.local/share/jupyter/kernels/pysparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/pysparkkernel_{1}'.format(args.os_user, args.cluster_name))
        datalab.fab.conn.sudo('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/sparkkernel_{1}'.format(args.os_user, args.cluster_name))
        #datalab.fab.conn.run('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkrkernel '
        #      '/home/{0}/.local/share/jupyter/kernels/sparkrkernel_{1}'.format(args.os_user, args.cluster_name))
        datalab.fab.conn.sudo('mkdir -p /home/' + args.os_user + '/.sparkmagic')
        datalab.fab.conn.sudo('cp -f /tmp/sparkmagic_config_template.json /home/' + args.os_user + '/.sparkmagic/config.json')
        spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
        datalab.fab.conn.sudo('sed -i \'s|LIVY_HOST|{0}|g\' /home/{1}/.sparkmagic/config.json'.format(
                spark_master_ip, args.os_user))
        datalab.fab.conn.sudo('sudo chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user))
    except Exception as err:
        print(err)
        sys.exit(1)

def create_inactivity_log(master_ip, hoststring):
    reworked_ip = master_ip.replace('.', '-')
    conn.sudo('''bash -l -c "date +%s > /opt/inactivity/{}_inactivity" '''.format(reworked_ip))

if __name__ == "__main__":
    global conn
    conn = datalab.fab.init_datalab_connection(args.notebook_ip, args.os_user, args.keyfile)
    try:
        region = os.environ['aws_region']
    except:
        region = ''
    if 'spark_configurations' not in os.environ:
        os.environ['spark_configurations'] = '[]'
    configure_notebook(args.keyfile, args.notebook_ip)
    install_sparkamagic_kernels(args)
    create_inactivity_log(args.spark_master_ip, args.notebook_ip)
    #conn.sudo('/usr/bin/python3 /usr/local/bin/tensor_dataengine_create_configs.py '
    #     '--cluster_name {} --spark_version {} --hadoop_version {} --os_user {} --spark_master {} --region {} '
    #     '--datalake_enabled {} --spark_configurations "{}"'.
    #     format(args.cluster_name, args.spark_version, args.hadoop_version, args.os_user, args.spark_master, region,
    #            args.datalake_enabled, os.environ['spark_configurations']))
