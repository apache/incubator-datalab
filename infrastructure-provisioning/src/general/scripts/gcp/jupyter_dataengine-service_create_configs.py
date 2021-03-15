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
import subprocess
from datalab.actions_lib import *
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--r_version', type=str, default='')
parser.add_argument('--r_enabled', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--python_version', type=str, default='')
parser.add_argument('--master_ip', type=str, default='')
args = parser.parse_args()

dataproc_dir = '/opt/{}/jars/'.format(args.dataproc_version)
kernels_dir = '/home/{}/.local/share/jupyter/kernels/'.format(args.os_user)
spark_dir = '/opt/{}/{}/spark/'.format(args.dataproc_version, args.cluster_name)
yarn_dir = '/opt/{}/{}/conf/'.format(args.dataproc_version, args.cluster_name)


def r_kernel(args):
    spark_path = '/opt/{}/{}/spark/'.format(args.dataproc_version, args.cluster_name)
    subprocess.run('mkdir -p {}/r_{}/'.format(kernels_dir, args.cluster_name), shell=True, check=True)
    kernel_path = "{}/r_{}/kernel.json".format(kernels_dir, args.cluster_name)
    template_file = "/tmp/r_dataengine-service_template.json"

    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('R_KERNEL_VERSION', 'R-{}'.format(args.r_version))
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.dataproc_version)
    text = text.replace('YARN_CLI_TYPE', 'yarn')
    text = text.replace('SPARK_ACTION', 'session(master = \\\"yarn\\\")')
    with open(kernel_path, 'w') as f:
        f.write(text)


def toree_kernel(args):
    spark_path = '/opt/{0}/{1}/spark/'.format(args.dataproc_version, args.cluster_name)
    subprocess.run('mkdir -p {0}toree_{1}/'.format(kernels_dir, args.cluster_name), shell=True, check=True)
    subprocess.run('tar zxvf /tmp/toree_kernel.tar.gz -C {0}toree_{1}/'.format(kernels_dir, args.cluster_name), shell=True, check=True)
    subprocess.run('sudo mv {0}toree_{1}/toree-0.3.0-incubating/* {0}toree_{1}/'.format(kernels_dir, args.cluster_name), shell=True, check=True)
    subprocess.run('sudo rm -r {0}toree_{1}/toree-0.3.0-incubating'.format(kernels_dir, args.cluster_name), shell=True, check=True)
    kernel_path = '{0}toree_{1}/kernel.json'.format(kernels_dir, args.cluster_name)
    template_file = "/tmp/toree_dataengine-service_templatev2.json"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('OS_USER', args.os_user)
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.dataproc_version)
    text = text.replace('SCALA_VERSION', args.scala_version)
    with open(kernel_path, 'w') as f:
        f.write(text)
    subprocess.run('touch /tmp/kernel_var.json', shell=True, check=True)
    subprocess.run(
        '''bash -l -c "PYJ=`find /opt/{}/{}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {} | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json" '''.format(args.dataproc_version, args.cluster_name, kernel_path), shell=True, check=True)
    subprocess.run('sudo mv /tmp/kernel_var.json ' + kernel_path, shell=True, check=True)
    run_sh_path = kernels_dir + "toree_" + args.cluster_name + "/bin/run.sh"
    template_sh_file = '/tmp/run_template.sh'
    with open(template_sh_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('OS_USER', args.os_user)
    with open(run_sh_path, 'w') as f:
        f.write(text)

def install_sparkamagic_kernels(args):
    try:
        subprocess.run('sudo jupyter nbextension enable --py --sys-prefix widgetsnbextension')
        sparkmagic_dir = subprocess.run("sudo pip3 show sparkmagic | grep 'Location: ' | awk '{print $2}'", capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user), shell=True, check=True)
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user), shell=True, check=True)
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkrkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user), shell=True, check=True)
        pyspark_kernel_name = 'PySpark (Python-{0} / Spark-{1} ) [{2}]'.format(args.python_version, args.spark_version,
                                                                         args.cluster_name)
        subprocess.run('sed -i \'s|PySpark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/pysparkkernel/kernel.json'.format(
            pyspark_kernel_name, args.os_user), shell=True, check=True)
        spark_kernel_name = 'Spark (Scala-{0} / Spark-{1} ) [{2}]'.format(args.scala_version, args.spark_version,
                                                                         args.cluster_name)
        subprocess.run('sed -i \'s|Spark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkkernel/kernel.json'.format(
            spark_kernel_name, args.os_user), shell=True, check=True)
        sparkr_kernel_name = 'SparkR (R-{0} / Spark-{1} ) [{2}]'.format(args.r_version, args.spark_version,
                                                                            args.cluster_name)
        subprocess.run('sed -i \'s|SparkR|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkrkernel/kernel.json'.format(
            sparkr_kernel_name, args.os_user), shell=True, check=True)
        subprocess.run('mkdir -p /home/' + args.os_user + '/.sparkmagic', shell=True, check=True)
        subprocess.run('cp -f /tmp/sparkmagic_config_template.json /home/' + args.os_user + '/.sparkmagic/config.json', shell=True, check=True)
        subprocess.run('sed -i \'s|LIVY_HOST|{0}|g\' /home/{1}/.sparkmagic/config.json'.format(
                args.master_ip, args.os_user), shell=True, check=True)
        subprocess.run('sudo chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user), shell=True, check=True)
    except:
        sys.exit(1)

if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        install_sparkamagic_kernels(args)
        #result = prepare(dataproc_dir, yarn_dir)
        #if result == False :
        #    actions_lib.GCPActions().jars(args, dataproc_dir)
        #actions_lib.GCPActions().yarn(args, yarn_dir)
        #actions_lib.GCPActions().install_dataproc_spark(args)
        #pyspark_kernel(kernels_dir, args.dataproc_version, args.cluster_name, args.spark_version, args.bucket,
        #               args.user_name, args.region, args.os_user, args.application, args.pip_mirror)
        #toree_kernel(args)
        #if args.r_enabled == 'true':
        #    r_kernel(args)
        #actions_lib.GCPActions().spark_defaults(args)
        #configuring_notebook(args.dataproc_version)
