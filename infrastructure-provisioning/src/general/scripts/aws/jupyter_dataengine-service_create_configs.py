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
from datalab.logger import logging
#from datalab.actions_lib import *
#from datalab.common_lib import *
#from datalab.fab import *
#from datalab.notebook_lib import *
#from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dry_run', type=str, default='false')
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--r_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--excluded_lines', type=str, default='')
parser.add_argument('--project_name', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--pip_mirror', type=str, default='')
parser.add_argument('--numpy_version', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--master_ip', type=str, default='')
parser.add_argument('--python_version', type=str, default='')
args = parser.parse_args()

emr_dir = '/opt/' + args.emr_version + '/jars/'
kernels_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/'
spark_dir = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/'
yarn_dir = '/opt/' + args.emr_version + '/' + args.cluster_name + '/conf/'


def r_kernel(args):
    spark_path = '/opt/{}/{}/spark/'.format(args.emr_version, args.cluster_name)
    subprocess.run('mkdir -p {}/r_{}/'.format(kernels_dir, args.cluster_name), shell=True, check=True)
    kernel_path = "{}/r_{}/kernel.json".format(kernels_dir, args.cluster_name)
    template_file = "/tmp/r_dataengine-service_template.json"

    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', args.cluster_name)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
    text = text.replace('R_KERNEL_VERSION', 'R-{}'.format(args.r_version))
    text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
    if 'emr-4.' in args.emr_version:
        text = text.replace('YARN_CLI_TYPE', 'yarn-client')
        text = text.replace('SPARK_ACTION', 'init()')
    else:
        text = text.replace('YARN_CLI_TYPE', 'yarn')
        text = text.replace('SPARK_ACTION', 'session(master = \\\"yarn\\\")')
    with open(kernel_path, 'w') as f:
        f.write(text)


def toree_kernel(args):
    spark_path = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/'
    scala_version = subprocess.run("Spark-submit --version 2>&1 | awk '/Scala version / {gsub(/,/, \"\"); print $4}'", shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
    if args.emr_version == 'emr-4.3.0' or args.emr_version == 'emr-4.6.0' or args.emr_version == 'emr-4.8.0':
        subprocess.run('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/', shell=True, check=True)
        kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/toree_dataengine-service_template.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER_NAME', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH', spark_path)
        text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
        text = text.replace('SCALA_VERSION', args.scala_version)
        with open(kernel_path, 'w') as f:
            f.write(text)
        subprocess.run('touch /tmp/kernel_var.json', shell=True, check=True)
        subprocess.run(
            '''bash -l -c "PYJ=`find /opt/" + args.emr_version + "/" + args.cluster_name + "/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat " + kernel_path + " | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json" ''', shell=True, check=True)
        subprocess.run('sudo mv /tmp/kernel_var.json ' + kernel_path, shell=True, check=True)
    else:
        subprocess.run('mkdir -p ' + kernels_dir + 'toree_' + args.cluster_name + '/', shell=True, check=True)
        subprocess.run('tar zxvf /tmp/toree_kernel.tar.gz -C ' + kernels_dir + 'toree_' + args.cluster_name + '/', shell=True, check=True)
        subprocess.run('sudo mv {0}toree_{1}/toree-0.3.0-incubating/* {0}toree_{1}/'.format(kernels_dir, args.cluster_name), shell=True, check=True)
        subprocess.run('sudo rm -r {0}toree_{1}/toree-0.3.0-incubating'.format(kernels_dir, args.cluster_name), shell=True, check=True)
        kernel_path = kernels_dir + "toree_" + args.cluster_name + "/kernel.json"
        template_file = "/tmp/toree_dataengine-service_templatev2.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER_NAME', args.cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + args.spark_version)
        text = text.replace('SPARK_PATH', spark_path)
        text = text.replace('OS_USER', args.os_user)
        text = text.replace('DATAENGINE-SERVICE_VERSION', args.emr_version)
        text = text.replace('SCALA_VERSION', args.scala_version)
        with open(kernel_path, 'w') as f:
            f.write(text)
        subprocess.run('touch /tmp/kernel_var.json', shell=True, check=True)
        subprocess.run('''bash -l -c "PYJ=`find /opt/{}/{}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {} | sed 's|PY4J|'$PYJ'|g' > /tmp/kernel_var.json" '''.format(args.emr_versio, args.cluster_name, kernel_path), shell=True, check=True)
        subprocess.run('sudo mv /tmp/kernel_var.json ' + kernel_path, shell=True, check=True)
        run_sh_path = kernels_dir + "toree_" + args.cluster_name + "/bin/run.sh"
        template_sh_file = '/tmp/run_template.sh'
        with open(template_sh_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER_NAME', args.cluster_name)
        text = text.replace('OS_USER', args.os_user)
        with open(run_sh_path, 'w') as f:
            f.write(text)


def add_breeze_library_emr(args):
    spark_defaults_path = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-defaults.conf'
    new_jars_directory_path = '/opt/' + args.emr_version + '/jars/usr/other/'
    breeze_tmp_dir = '/tmp/breeze_tmp_emr/'
    subprocess.run('sudo mkdir -p ' + new_jars_directory_path, shell=True, check=True)
    subprocess.run('mkdir -p ' + breeze_tmp_dir, shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/scalanlp/breeze_2.11/0.12/breeze_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze_2.11-0.12.jar', shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-natives_2.11/0.12/breeze-natives_2.11-0.12.jar -O '
          + breeze_tmp_dir + 'breeze-natives_2.11-0.12.jar', shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-viz_2.11/0.12/breeze-viz_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze-viz_2.11-0.12.jar', shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-macros_2.11/0.12/breeze-macros_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze-macros_2.11-0.12.jar', shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-parent_2.11/0.12/breeze-parent_2.11-0.12.jar -O ' +
          breeze_tmp_dir + 'breeze-parent_2.11-0.12.jar', shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/jfree/jfreechart/1.0.19/jfreechart-1.0.19.jar -O ' +
          breeze_tmp_dir + 'jfreechart-1.0.19.jar', shell=True, check=True)
    subprocess.run('wget https://repo1.maven.org/maven2/org/jfree/jcommon/1.0.24/jcommon-1.0.24.jar -O ' +
          breeze_tmp_dir + 'jcommon-1.0.24.jar', shell=True, check=True)
    subprocess.run('wget --no-check-certificate https://nexus.develop.dlabanalytics.com/repository/packages-public/spark-kernel-brunel-all-2.3.jar -O ' +
          breeze_tmp_dir + 'spark-kernel-brunel-all-2.3.jar', shell=True, check=True)
    subprocess.run('sudo mv ' + breeze_tmp_dir + '* ' + new_jars_directory_path, shell=True, check=True)
    subprocess.run(""" sudo bash -c "sed -i '/spark.driver.extraClassPath/s/$/:\/opt\/""" + args.emr_version +
          """\/jars\/usr\/other\/*/' """ + spark_defaults_path + """" """, shell=True, check=True)

def install_sparkamagic_kernels(args):
    try:
        subprocess.run('sudo jupyter nbextension enable --py --sys-prefix widgetsnbextension', shell=True, check=True)
        sparkmagic_dir = subprocess.run("sudo pip3 show sparkmagic | grep 'Location: ' | awk '{print $2}'", capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkkernel --user'.format(sparkmagic_dir), shell=True, check=True)
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/pysparkkernel --user'.format(sparkmagic_dir), shell=True, check=True)
        subprocess.run('sudo jupyter-kernelspec install {}/sparkmagic/kernels/sparkrkernel --user'.format(sparkmagic_dir), shell=True, check=True)
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
        subprocess.run('sudo mv -f /home/{0}/.local/share/jupyter/kernels/pysparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/pysparkkernel_{1}'.format(args.os_user, args.cluster_name), shell=True, check=True)
        subprocess.run('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkkernel '
              '/home/{0}/.local/share/jupyter/kernels/sparkkernel_{1}'.format(args.os_user, args.cluster_name), shell=True, check=True)
        subprocess.run('sudo mv -f /home/{0}/.local/share/jupyter/kernels/sparkrkernel '
              '/home/{0}/.local/share/jupyter/kernels/sparkrkernel_{1}'.format(args.os_user, args.cluster_name), shell=True, check=True)
        subprocess.run('mkdir -p /home/' + args.os_user + '/.sparkmagic', shell=True, check=True)
        subprocess.run('cp -f /tmp/sparkmagic_config_template.json /home/' + args.os_user + '/.sparkmagic/config.json', shell=True, check=True)
        subprocess.run('sed -i \'s|LIVY_HOST|{0}|g\' /home/{1}/.sparkmagic/config.json'.format(
                args.master_ip, args.os_user), shell=True, check=True)
        subprocess.run('sudo chown -R {0}:{0} /home/{0}/.sparkmagic/'.format(args.os_user), shell=True, check=True)
    except:
        traceback.print_exc()
        sys.exit(1)



if __name__ == "__main__":
    if args.dry_run == 'true':
        parser.print_help()
    else:
        install_sparkamagic_kernels(args)
        #result = prepare(emr_dir, yarn_dir)
        #if result == False :
        #    jars(args, emr_dir)
        #yarn(args, yarn_dir)
        #install_emr_spark(args)
        #pyspark_kernel(kernels_dir, args.emr_version, args.cluster_name, args.spark_version, args.bucket,
        #               args.project_name, args.region, args.os_user, args.application, args.pip_mirror, args.numpy_version)
        #toree_kernel(args)
        #if args.r_version != 'false':
        #    print('R version: {}'.format(args.r_version))
        #    r_kernel(args)
        #spark_defaults(args)
        #configuring_notebook(args.emr_version)
        #add_breeze_library_emr(args)
