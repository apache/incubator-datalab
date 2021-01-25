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
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric.api import *
from fabric.contrib.files import exists

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--jupyter_version', type=str, default='')
parser.add_argument('--scala_version', type=str, default='')
parser.add_argument('--spark_version', type=str, default='')
parser.add_argument('--hadoop_version', type=str, default='')
parser.add_argument('--r_mirror', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--edge_ip', type=str, default='')
args = parser.parse_args()


jupyter_conf_file = '/home/' + args.os_user + '/.local/share/jupyter/jupyter_notebook_config.py'
templates_dir = '/root/templates/'
spark_version = args.spark_version
hadoop_version = args.hadoop_version
nvidia_version = os.environ['notebook_nvidia_version']
caffe_version = os.environ['notebook_caffe_version']
caffe2_version = os.environ['notebook_caffe2_version']
cmake_version = os.environ['notebook_cmake_version']
cntk_version = os.environ['notebook_cntk_version']
cntk2_version = os.environ['notebook_cntk2_version']
mxnet_version = os.environ['notebook_mxnet_version']
keras_version = os.environ['notebook_keras_version']
theano_version = os.environ['notebook_theano_version']
tensorflow_version = os.environ['notebook_tensorflow_version']
cuda_version = os.environ['notebook_cuda_version']
cuda_file_name = os.environ['notebook_cuda_file_name']
cudnn_version = os.environ['notebook_cudnn_version']
cudnn_file_name = os.environ['notebook_cudnn_file_name']

if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
local_spark_path = '/opt/spark/'
jars_dir = '/opt/jars/'
files_dir = '/root/files/'
pyspark_local_path_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/pyspark_local/'
py3spark_local_path_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/py3spark_local/'
gitlab_certfile = os.environ['conf_gitlab_certfile']


def install_itorch(os_user):
    if not exists('/home/{}/.ensure_dir/itorch_ensured'.format(os_user)):
        run('git clone https://github.com/facebook/iTorch.git')
        with cd('/home/{}/iTorch/'.format(os_user)):
            run('luarocks install luacrypto')
            run('luarocks install uuid')
            run('luarocks install lzmq')
            run('luarocks make')
        sudo('cp -rf /home/{0}/.ipython/kernels/itorch/ /home/{0}/.local/share/jupyter/kernels/'.format(os_user))
        sudo('chown -R {0}:{0} /home/{0}/.local/share/jupyter/'.format(os_user))
        sudo('touch /home/{}/.ensure_dir/itorch_ensured'.format(os_user))


if __name__ == "__main__":
    print("Configure connections")
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    # PREPARE DISK
    print("Prepare .ensure directory")
    try:
        if not exists('/home/' + args.os_user + '/.ensure_dir'):
            sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
            sudo('touch /home/' + args.os_user + '/.ensure_dir/deep_learning')
    except:
        sys.exit(1)
    print("Mount additional volume")
    prepare_disk(args.os_user)

    # INSTALL LANGUAGES
    print("Install Java")
    ensure_jre_jdk(args.os_user)
    print("Install Python 3 modules")
    ensure_python3_libraries(args.os_user)

    # INSTALL TENSORFLOW AND OTHER DEEP LEARNING LIBRARIES AND FRAMEWORKS
    print("Install TensorFlow")
    install_tensor(args.os_user, cuda_version, cuda_file_name,
                   cudnn_version, cudnn_file_name, tensorflow_version,
                   templates_dir, nvidia_version)
    print("Install Theano")
    install_theano(args.os_user, theano_version)
    print("Installing Keras")
    install_keras(args.os_user, keras_version)
    print("Installing Caffe2")
    install_caffe2(args.os_user, caffe2_version, cmake_version)
    #print("Installing Torch")
    #install_torch(args.os_user)
    print("Install CNTK Python library")
    install_cntk(args.os_user,cntk2_version, cntk_version)
    print("Installing MXNET")
    install_mxnet(args.os_user, mxnet_version)

    # INSTALL JUPYTER NOTEBOOK
    print("Install Jupyter")
    configure_jupyter(args.os_user, jupyter_conf_file, templates_dir, args.jupyter_version, args.exploratory_name)

    # INSTALL SPARK AND CLOUD STORAGE JARS FOR SPARK
    print("Install local Spark")
    ensure_local_spark(args.os_user, spark_link, spark_version, hadoop_version, local_spark_path)
    print("Install storage jars")
    ensure_local_jars(args.os_user, jars_dir)
    print("Configure local Spark")
    configure_local_spark(jars_dir, templates_dir)

    # INSTALL JUPYTER KERNELS
    print("Install pyspark local kernel for Jupyter")
    ensure_pyspark_local_kernel(args.os_user, pyspark_local_path_dir, templates_dir, spark_version)
    print("Install py3spark local kernel for Jupyter")
    ensure_py3spark_local_kernel(args.os_user, py3spark_local_path_dir, templates_dir, spark_version)
    #print("Installing ITorch kernel for Jupyter")
    #install_itorch(args.os_user)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install Ungit")
    install_ungit(args.os_user, args.exploratory_name, args.edge_ip)
    if exists('/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)

    # INSTALL INACTIVITY CHECKER
    print("Install inactivity checker")
    install_inactivity_checker(args.os_user, args.ip_address)

    # INSTALL OPTIONAL PACKAGES
    print("Installing additional Python packages")
    ensure_additional_python_libs(args.os_user)
    print("Install Matplotlib")
    ensure_matplot(args.os_user)

    #POST INSTALLATION PROCESS
    print("Updating pyOpenSSL library")
    update_pyopenssl_lib(args.os_user)