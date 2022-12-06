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
parser.add_argument('--hdinsight_version', type=str, default='')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--python_version', type=str, default='')
parser.add_argument('--headnode_ip', type=str, default='')
args = parser.parse_args()

hdinsight_dir = '/opt/{}/jars/'.format(args.hdinsight_version)
kernels_dir = '/home/{}/.local/share/jupyter/kernels/'.format(args.os_user)
spark_dir = '/opt/{}/{}/spark/'.format(args.hdinsight_version, args.cluster_name)
yarn_dir = '/opt/{}/{}/conf/'.format(args.hdinsight_version, args.cluster_name)


def install_sparkamagic_kernels(args):
    try:
        subprocess.run('sudo jupyter nbextension enable --py --sys-prefix widgetsnbextension')
        sparkmagic_dir = subprocess.run("sudo pip3 show sparkmagic | grep 'Location: ' | awk '{print $2}'", capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user), shell=True, check=True)
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user), shell=True, check=True)
        #subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkrkernel --prefix=/home/{}/.local/'.format(sparkmagic_dir, args.os_user), shell=True, check=True)
        pyspark_kernel_name = 'PySpark (Python-{0} / Spark-{1} ) [{2}]'.format(args.python_version, args.spark_version,
                                                                         args.cluster_name)
        subprocess.run('sed -i \'s|PySpark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/pysparkkernel/kernel.json'.format(
            pyspark_kernel_name, args.os_user), shell=True, check=True)
        spark_kernel_name = 'Spark (Scala-{0} / Spark-{1} ) [{2}]'.format(args.scala_version, args.spark_version,
                                                                         args.cluster_name)
        subprocess.run('sed -i \'s|Spark|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkkernel/kernel.json'.format(
            spark_kernel_name, args.os_user), shell=True, check=True)
        #sparkr_kernel_name = 'SparkR (R-{0} / Spark-{1} ) [{2}]'.format(args.r_version, args.spark_version,
        #                                                                    args.cluster_name)
        #subprocess.run('sed -i \'s|SparkR|{0}|g\' /home/{1}/.local/share/jupyter/kernels/sparkrkernel/kernel.json'.format(
        #    sparkr_kernel_name, args.os_user), shell=True, check=True)
        subprocess.run('mkdir -p /home/' + args.os_user + '/.sparkmagic', shell=True, check=True)
        subprocess.run('cp -f /tmp/dataengine-service_sparkmagic_config.json /home/' + args.os_user + '/.sparkmagic/config.json', shell=True, check=True)
        subprocess.run('sed -i \'s|HEADNODEIP:PORT|{0}:{2}|g\' /home/{1}/.sparkmagic/config.json'.format(
                args.master_ip, args.os_user, args.livy_port, shell=True, check=True)
        subprocess.run('sudo chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user), shell=True, check=True)
        subprocess.run('touch /home/{}/.ensure_dir/sparkmagic_kernels_ensured'.format(args.os_user), shell=True, check=True)
    except:
        sys.exit(1)

if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        install_sparkamagic_kernels(args)
