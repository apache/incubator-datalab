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
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.notebook_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--master_ip', type=str, default='')
parser.add_argument('--node_type', type=str, default='')
args = parser.parse_args()

spark_version = args.spark_version
hadoop_version = args.hadoop_version
tensorflow_version = os.environ['notebook_tensorflow_version']
nvidia_version = os.environ['notebook_nvidia_version']
theano_version = os.environ['notebook_theano_version']
keras_version = os.environ['notebook_keras_version']
caffe_version = os.environ['notebook_caffe_version']
caffe2_version = os.environ['notebook_caffe2_version']
cmake_version = os.environ['notebook_cmake_version']
cntk_version = os.environ['notebook_cntk_version']
mxnet_version = os.environ['notebook_mxnet_version']
python3_version = "3.4"
python_venv_version = os.environ['notebook_python_venv_version']
python_venv_path = '/opt/python/python{0}/bin/python{1}'.format(python_venv_version, python_venv_version[:3])
scala_link = "https://www.scala-lang.org/files/archive/"
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"

cuda_version = os.environ['notebook_cuda_version']
cuda_file_name = os.environ['notebook_cuda_file_name']
cudnn_version = os.environ['notebook_cudnn_version']
cudnn_file_name = os.environ['notebook_cudnn_file_name']

templates_dir = '/root/templates/'
files_dir = '/root/files/'
local_spark_path = '/opt/spark/'
jars_dir = '/opt/jars/'
r_libs = ['R6', 'pbdZMQ={}'.format(os.environ['notebook_pbdzmq_version']), 'RCurl', 'reshape2', 'caTools={}'.format(os.environ['notebook_catools_version']), 'rJava', 'ggplot2']
if os.environ['application'] == 'deeplearning':
    tensorflow_version = '1.4.0'
    cuda_version = '8.0'
    cuda_file_name = 'cuda_8.0.44_linux-run'
    cudnn_version = '6.0'
    cudnn_file_name = 'cudnn-8.0-linux-x64-v6.0.tgz'


def start_spark(os_user, master_ip, node):
    if not exists(conn,'/home/{0}/.ensure_dir/start_spark-{1}_ensured'.format(os_user, node)):
        if not exists(conn,'/opt/spark/conf/spark-env.sh'):
            conn.sudo('mv /opt/spark/conf/spark-env.sh.template /opt/spark/conf/spark-env.sh')
        conn.sudo('''echo "SPARK_MASTER_HOST='{}'" >> /opt/spark/conf/spark-env.sh'''.format(master_ip))
        if os.environ['application'] in ('tensor', 'tensor-rstudio'):
            conn.sudo('''echo "LD_LIBRARY_PATH=/opt/cudnn/lib64:/usr/local/cuda/lib64" >> /opt/spark/conf/spark-env.sh''')
        if os.environ['application'] == 'deeplearning':
            conn.sudo('''echo "LD_LIBRARY_PATH=/opt/cudnn/lib64:/usr/local/cuda/lib64:/usr/lib64/openmpi/lib" >> /opt/spark/conf/spark-env.sh''')
        if node == 'master':
            conn.sudo("sed -i '/start-slaves.sh/d' /opt/spark/sbin/start-all.sh")
            conn.sudo('''echo '"${}/sbin"/start-slave.sh spark://{}:7077' >> /opt/spark/sbin/start-all.sh'''.format('{SPARK_HOME}', master_ip))
            conn.put('/root/templates/spark-master.service', '/tmp/spark-master.service')
            conn.sudo('mv /tmp/spark-master.service /etc/systemd/system/spark-master.service')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable spark-master.service')
            conn.sudo('systemctl start spark-master.service')
        if node == 'slave':
            with open('/root/templates/spark-slave.service', 'r') as f:
                text = f.read()
            text = text.replace('MASTER', 'spark://{}:7077'.format(master_ip))
            with open('/root/templates/spark-slave.service', 'w') as f:
                f.write(text)
            conn.put('/root/templates/spark-slave.service', '/tmp/spark-slave.service')
            conn.sudo('mv /tmp/spark-slave.service /etc/systemd/system/spark-slave.service')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable spark-slave.service')
            conn.sudo('systemctl start spark-slave.service')
        conn.sudo('touch /home/{0}/.ensure_dir/start_spark-{1}_ensured'.format(os_user, node))

##############
# Run script #
##############


if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)

    # PREPARE DISK
    print("Prepare .ensure directory")
    try:
        if not exists(conn,'/home/' + args.os_user + '/.ensure_dir'):
            conn.sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)

    # INSTALL LANGUAGES
    print("Install Java")
    ensure_jre_jdk(args.os_user)
    if os.environ['application'] in ('jupyter', 'zeppelin'):
        print("Install Scala")
        ensure_scala(scala_link, args.scala_version, args.os_user)
    if (os.environ['application'] in ('jupyter', 'zeppelin')
        and os.environ['notebook_r_enabled'] == 'true') \
            or os.environ['application'] in ('rstudio', 'tensor-rstudio'):
        print("Installing R")
        ensure_r(args.os_user, r_libs)
    print("Install Python 3 modules")
    ensure_python3_libraries(args.os_user)
    if os.environ['application'] == 'zeppelin':
        print("Install python3 specific version")
        ensure_python3_specific_version(python3_version, args.os_user)

    # INSTALL PYTHON IN VIRTUALENV
    if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['conf_cloud_provider'] == 'azure' and \
            os.environ['application'] == 'deeplearning':
        print('Python Virtualenv already configured')
    else:
        print("Configure Python Virtualenv")
        ensure_python_venv(python_venv_version)

    # INSTALL SPARK AND CLOUD STORAGE JARS FOR SPARK
    print("Install Spark")
    ensure_local_spark(args.os_user, spark_link, spark_version, hadoop_version, local_spark_path)
    print("Install storage jars")
    ensure_local_jars(args.os_user, jars_dir)
    print("Configure local Spark")
    configure_local_spark(jars_dir, templates_dir, '')

    #INSTALL TENSORFLOW AND OTHER DEEP LEARNING LIBRARIES
    if os.environ['application'] in ('tensor', 'tensor-rstudio'):
        print("Installing TensorFlow")
        install_tensor(args.os_user, cuda_version, cuda_file_name,
                       cudnn_version, cudnn_file_name, tensorflow_version,
                       templates_dir, nvidia_version)
        print("Install Theano")
        install_theano(args.os_user, theano_version)
        print("Installing Keras")
        install_keras(args.os_user, keras_version)

    # INSTALL DEEP LEARNING FRAMEWORKS
    if os.environ['application'] == 'deeplearning' and os.environ['conf_deeplearning_cloud_ami'] != 'true':
        print("Installing Caffe2")
        install_caffe2(args.os_user, caffe2_version, cmake_version)
        #print("Installing Torch")
        #install_torch(args.os_user)
        print("Install CNTK Python library")
        install_cntk(args.os_user, cntk_version)
        print("Installing MXNET")
        install_mxnet(args.os_user, mxnet_version)

    # START SPARK CLUSTER
    if args.node_type == 'master':
        print("Starting Spark master")
        start_spark(args.os_user, args.hostname, node='master')
    elif args.node_type == 'slave':
        print("Starting Spark slave")
        start_spark(args.os_user, args.master_ip, node='slave')

    # INSTALL OPTIONAL PACKAGES
    if os.environ['application'] in ('jupyter', 'zeppelin', 'tensor', 'deeplearning'):
        print("Install additional Python packages")
        ensure_additional_python_libs(args.os_user)
        print("Install matplotlib")
        ensure_matplot(args.os_user)
    if os.environ['application'] == 'jupyter':
        print("Install SBT")
        ensure_sbt(args.os_user)
        print("Install Breeze")
        add_breeze_library_local(args.os_user)
    if os.environ['application'] == 'zeppelin' and os.environ['notebook_r_enabled'] == 'true':
        print("Install additional R packages")
        install_r_packages(args.os_user)

    # INSTALL LIVY
    if not exists(conn, '/home/{0}/.ensure_dir/livy_ensured'.format(args.os_user)):
        conn.sudo('wget -P /tmp/ https://nexus.develop.dlabanalytics.com/repository/packages-public/livy.tar.gz --no-check-certificate')
        conn.sudo('tar -xzvf /tmp/livy.tar.gz -C /tmp/')
        conn.sudo('mv /tmp/incubator-livy /opt/livy')
        conn.sudo('mkdir /var/log/livy')
        conn.put('/root/templates/livy-env.sh', '/tmp/livy-env.sh')
        conn.sudo("sed -i 's|=python3|={}|' /tmp/livy-env.sh".format(python_venv_path))
        conn.sudo('mv /tmp/livy-env.sh /opt/livy/conf/livy-env.sh')
        conn.sudo('chown -R -L {0}:{0} /opt/livy/'.format(args.os_user))
        conn.sudo('chown -R {0}:{0} /var/log/livy'.format(args.os_user))
        conn.put('/root/templates/livy.service', '/tmp/livy.service')
        conn.sudo("sed -i 's|OS_USER|{}|' /tmp/livy.service".format(args.os_user))
        conn.sudo('mv /tmp/livy.service /etc/systemd/system/livy.service')
        conn.sudo('systemctl daemon-reload')
        conn.sudo('systemctl enable livy.service')
        conn.sudo('systemctl start livy.service')
        conn.sudo('touch /home/{0}/.ensure_dir/livy_ensured'.format(args.os_user))

    conn.close()