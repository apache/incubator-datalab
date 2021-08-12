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
import sys
from datalab.actions_lib import *
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--spark_master', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='')
parser.add_argument('--spark_configurations', type=str, default='')
args = parser.parse_args()

kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
cluster_dir = '/opt/' + args.cluster_name + '/'
local_jars_dir = '/opt/jars/'

spark_version = args.spark_version
hadoop_version = args.hadoop_version
scala_link = "https://www.scala-lang.org/files/archive/"
spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
             "-bin-hadoop" + hadoop_version + ".tgz"


def pyspark_kernel(args):
    spark_path = '/opt/' + args.cluster_name + '/spark/'
    subprocess.run('mkdir -p ' + kernels_dir + 'pyspark_' + args.cluster_name + '/', shell=True, check=True)
    kernel_path = kernels_dir + "pyspark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/{}/pyspark_dataengine_template.json".format(args.cluster_name)
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('PYTHON_SHORT_VERSION', '3.8')
    text = text.replace('PYTHON_FULL_VERSION', '3.8')
    text = text.replace('MASTER', args.spark_master)
    text = text.replace('PYTHON_PATH', '/usr/bin/python3.8')
    with open(kernel_path, 'w') as f:
        f.write(text)
    subprocess.run('touch /tmp/{}/kernel_var.json'.format(args.cluster_name), shell=True, check=True)
    subprocess.run(
        '''bash -l -c "PYJ=`find /opt/{0}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {1} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{2}/caffe/python:/home/{2}/pytorch/build:\"|\' > /tmp/{0}/kernel_var.json" '''.
        format(args.cluster_name, kernel_path, args.os_user), shell=True, check=True)
    subprocess.run('sudo mv /tmp/{}/kernel_var.json '.format(args.cluster_name) + kernel_path, shell=True, check=True)

    subprocess.run('mkdir -p ' + kernels_dir + 'py3spark_' + args.cluster_name + '/', shell=True, check=True)
    kernel_path = kernels_dir + "py3spark_" + args.cluster_name + "/kernel.json"
    template_file = "/tmp/{}/pyspark_dataengine_template.json".format(args.cluster_name)
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('MASTER', args.spark_master)
    text = text.replace('PYTHON_SHORT_VERSION', '3.8')
    text = text.replace('PYTHON_FULL_VERSION', '3.8')
    text = text.replace('PYTHON_PATH', '/usr/bin/python3.8')
    with open(kernel_path, 'w') as f:
        f.write(text)
    subprocess.run('touch /tmp/{}/kernel_var.json'.format(args.cluster_name), shell=True, check=True)
    subprocess.run(
        '''bash -l -c "PYJ=`find /opt/{0}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {1} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{2}/caffe/python:/home/{2}/pytorch/build:\"|\' > /tmp/{0}/kernel_var.json" '''.
        format(args.cluster_name, kernel_path, args.os_user), shell=True, check=True)
    subprocess.run('sudo mv /tmp/{}/kernel_var.json '.format(args.cluster_name) + kernel_path, shell=True, check=True)

def install_sparkamagic_kernels(args):
    try:
        subprocess.run('sudo jupyter nbextension enable --py --sys-prefix widgetsnbextension', shell=True, check=True)
        sparkmagic_dir = subprocess.run("sudo pip3 show sparkmagic | grep 'Location: ' | awk '{print $2}'", capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --user'.format(sparkmagic_dir), shell=True, check=True)
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --user'.format(sparkmagic_dir), shell=True, check=True)

        pyspark_kernel_name = 'PySpark (Python-{2} / Spark-{0} ) [{1}]'.format(args.spark_version,
                                                                         args.cluster_name, os.environ['notebook_python_venv_version'][:3])
        subprocess.run('sed -i \'s|PySpark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/pysparkkernel/kernel.json'.format(
            pyspark_kernel_name, args.os_user), shell=True, check=True)
        scala_version = subprocess.run('spark-submit --version 2>&1 | grep -o -P "Scala version \K.{0,7}"', capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        spark_kernel_name = 'Spark (Scala-{0} / Spark-{1} ) [{2}]'.format(scala_version, args.spark_version,
                                                                         args.cluster_name)
        subprocess.run('sed -i \'s|Spark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkkernel/kernel.json'.format(
            spark_kernel_name, args.os_user), shell=True, check=True)

        subprocess.run('sudo mv -f /home/{0}/.local/share/jupyter/kernels/pysparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/pysparkkernel_{1}'.format(args.os_user, args.cluster_name), shell=True, check=True)
        subprocess.run('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/sparkkernel_{1}'.format(args.os_user, args.cluster_name), shell=True, check=True)

        subprocess.run('mkdir -p /home/' + args.os_user + '/.sparkmagic', shell=True, check=True)
        subprocess.run('cp -f /tmp/sparkmagic_config_template.json /home/' + args.os_user + '/.sparkmagic/config.json', shell=True, check=True)
        spark_master_ip = args.spark_master.split('//')[1].split(':')[0]
        subprocess.run('sed -i \'s|LIVY_HOST|{0}|g\' /home/{1}/.sparkmagic/config.json'.format(
                spark_master_ip, args.os_user), shell=True, check=True)
        subprocess.run('sudo chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user), shell=True, check=True)
    except:
        sys.exit(1)

if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        install_sparkamagic_kernels(args)
        #dataengine_dir_prepare('/opt/{}/'.format(args.cluster_name))
        #install_dataengine_spark(args.cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, args.os_user,
        #                         args.datalake_enabled)
        #ensure_dataengine_tensorflow_jars(local_jars_dir)
        #configure_dataengine_spark(args.cluster_name, local_jars_dir, cluster_dir, args.datalake_enabled,
        #                           args.spark_configurations)
        #pyspark_kernel(args)
