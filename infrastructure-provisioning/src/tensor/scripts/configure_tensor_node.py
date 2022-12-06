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
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *
from patchwork.files import exists
from patchwork import files

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--ip_address', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--edge_ip', type=str, default='')
args = parser.parse_args()

spark_version = os.environ['notebook_spark_version']
hadoop_version = os.environ['notebook_hadoop_version']
tensorflow_version = os.environ['notebook_tensorflow_version']
jupyter_version = os.environ['notebook_jupyter_version']
nvidia_version = os.environ['notebook_nvidia_version']
theano_version = os.environ['notebook_theano_version']
keras_version = os.environ['notebook_keras_version']
python_venv_version = os.environ['notebook_python_venv_version']
python_venv_path = '/opt/python/python{0}/bin/python{1}'.format(python_venv_version, python_venv_version[:3])
if args.region == 'cn-north-1':
    spark_link = "http://mirrors.hust.edu.cn/apache/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
else:
    spark_link = "https://archive.apache.org/dist/spark/spark-" + spark_version + "/spark-" + spark_version + \
                 "-bin-hadoop" + hadoop_version + ".tgz"
pyspark_local_path_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/pyspark_local/'
py3spark_local_path_dir = '/home/' + args.os_user + '/.local/share/jupyter/kernels/py3spark_local/'
local_spark_path = '/opt/spark/'
jars_dir = '/opt/jars/'
templates_dir = '/root/templates/'
files_dir = '/root/files/'
jupyter_conf_file = '/home/' + args.os_user + '/.local/share/jupyter/jupyter_notebook_config.py'
gitlab_certfile = os.environ['conf_gitlab_certfile']
cuda_version = os.environ['notebook_cuda_version']
cuda_file_name = os.environ['notebook_cuda_file_name']
cudnn_version = os.environ['notebook_cudnn_version']
cudnn_file_name = os.environ['notebook_cudnn_file_name']
venv_libs = "numpy scipy matplotlib pandas scikit-learn opencv-python tensorflow=={0}".format(tensorflow_version)

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
    print("Mount additional volume")
    prepare_disk(args.os_user)

    # INSTALL LANGUAGES
    print("Install Java")
    ensure_jre_jdk(args.os_user)
    print("Install Python 3 modules")
    ensure_python3_libraries(args.os_user)

    # INSTALL PYTHON IN VIRTUALENV
    print("Configure Python Virtualenv")
    ensure_python_venv(python_venv_version)

    # INSTALL TENSORFLOW AND OTHER DEEP LEARNING LIBRARIES
    print("Install TensorFlow")
    install_tensor(args.os_user, cuda_version, cuda_file_name,
                   cudnn_version, cudnn_file_name, tensorflow_version,
                   templates_dir, nvidia_version)
    print("Install Theano")
    install_theano(args.os_user, theano_version)
    print("Installing Keras")
    install_keras(args.os_user, keras_version)

    # INSTALL JUPYTER NOTEBOOK
    print("Install Jupyter")
    configure_jupyter(args.os_user, jupyter_conf_file, templates_dir, jupyter_version, args.exploratory_name)

    # INSTALL SPARK AND CLOUD STORAGE JARS FOR SPARK
    print("Install local Spark")
    ensure_local_spark(args.os_user, spark_link, spark_version, hadoop_version, local_spark_path )
    print("Install storage jars")
    ensure_local_jars(args.os_user, jars_dir)
    print("Configure local Spark")
    configure_local_spark(jars_dir, templates_dir)

    # INSTALL JUPYTER KERNELS
    #print("Install pyspark local kernel for Jupyter")
    #ensure_pyspark_local_kernel(args.os_user, pyspark_local_path_dir, templates_dir, spark_version)
    print("Install py3spark local kernel for Jupyter")
    ensure_py3spark_local_kernel(args.os_user, py3spark_local_path_dir, templates_dir, spark_version, python_venv_path, python_venv_version)

    # INSTALL UNGIT
    print("Install nodejs")
    install_nodejs(args.os_user)
    print("Install Ungit")
    install_ungit(args.os_user, args.exploratory_name, args.edge_ip)
    if exists(conn, '/home/{0}/{1}'.format(args.os_user, gitlab_certfile)):
        install_gitlab_cert(args.os_user, gitlab_certfile)

    # INSTALL INACTIVITY CHECKER
    print("Install inactivity checker")
    install_inactivity_checker(args.os_user, args.ip_address)

    # INSTALL OPTIONAL PACKAGES
    print("Installing additional Python packages")
    ensure_additional_python_libs(args.os_user)
    print('Installing Pytorch')
    ensure_pytorch(args.os_user)
    print("Install Matplotlib")
    ensure_matplot(args.os_user)

    print("Install python venv required libs")
    ensure_venv_libs(args.os_user, venv_libs)
    datalab.fab.install_venv_pip_pkg('--extra-index-url https://google-coral.github.io/py-repo/ tflite_runtime==2.5.0')

    #POST INSTALLATION PROCESS
    print("Updating pyOpenSSL library")
    update_pyopenssl_lib(args.os_user)
    print("Removing unexisting kernels")
    remove_unexisting_kernel(args.os_user)

    conn.close()
