#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import random
import string
import sys
from dlab.notebook_lib import *
from dlab.fab import *
import os, time


def enable_proxy(proxy_host, proxy_port):
    if not exists('/tmp/proxy_enabled'):
        try:
            proxy_string = "http://%s:%s" % (proxy_host, proxy_port)
            sudo('echo export http_proxy=' + proxy_string + ' >> /etc/profile')
            sudo('echo export https_proxy=' + proxy_string + ' >> /etc/profile')
            sudo("echo 'proxy={}' >> /etc/yum.conf".format(proxy_string))
            sudo('yum clean all')
            sudo('touch /tmp/proxy_enabled ')
        except:
            sys.exit(1)


def ensure_r_local_kernel(spark_version, os_user, templates_dir, kernels_dir):
    if not exists('/home/{}/.ensure_dir/r_kernel_ensured'.format(os_user)):
        try:
            sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            run('R -e "IRkernel::installspec()"')
            sudo('cd /usr/local/spark/R/lib/SparkR; R -e "devtools::install(\'.\')"')
            r_version = sudo("R --version | awk '/version / {print $3}'")
            put(templates_dir + 'r_template.json', '/tmp/r_template.json')
            sudo('sed -i "s|R_VER|' + r_version + '|g" /tmp/r_template.json')
            sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/r_template.json')
            sudo('\cp -f /tmp/r_template.json {}/ir/kernel.json'.format(kernels_dir))
            sudo('ln -s /usr/lib64/R/ /usr/lib/R')
            sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            sudo('touch /home/{}/.ensure_dir/r_kernel_ensured'.format(os_user))
        except:
            sys.exit(1)


def ensure_r(os_user, r_libs):
    if not exists('/home/{}/.ensure_dir/r_ensured'.format(os_user)):
        try:
            sudo('yum install -y cmake')
            sudo('yum -y install libcur*')
            sudo('echo -e "[base]\nname=CentOS-7-Base\nbaseurl=http://buildlogs.centos.org/centos/7/os/x86_64-20140704-1/\ngpgcheck=1\ngpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7\npriority=1\nexclude=php mysql" >> /etc/yum.repos.d/CentOS-base.repo')
            sudo('yum install -y R R-core R-core-devel R-devel --nogpgcheck')
            sudo('R CMD javareconf')
            sudo('cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig')
            for i in r_libs:
                sudo('R -e "install.packages(\'{}\',repos=\'http://cran.us.r-project.org\')"'.format(i))
            sudo('R -e "library(\'devtools\');install.packages(repos=\'http://cran.us.r-project.org\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"')
            sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            sudo('R -e "install.packages(\'RJDBC\',repos=\'http://cran.us.r-project.org\',dep=TRUE)"')
            sudo('touch /home/{}/.ensure_dir/r_ensured'.format(os_user))
        except:
            sys.exit(1)


def install_rstudio(os_user, local_spark_path, rstudio_pass, rstudio_version):
    if not exists('/home/' + os_user + '/.ensure_dir/rstudio_ensured'):
        try:
            sudo('yum install -y --nogpgcheck https://download2.rstudio.org/rstudio-server-rhel-{}-x86_64.rpm'.format(rstudio_version))
            sudo('mkdir /mnt/var')
            sudo('chown {0}:{0} /mnt/var'.format(os_user))
            sudo('touch /home/{}/.Renviron'.format(os_user))
            sudo('chown {0}:{0} /home/{0}/.Renviron'.format(os_user))
            sudo('''echo 'SPARK_HOME="{0}"' >> /home/{1}/.Renviron'''.format(local_spark_path, os_user))
            sudo('touch /home/{}/.Rprofile'.format(os_user))
            sudo('chown {0}:{0} /home/{0}/.Rprofile'.format(os_user))
            sudo('''echo 'library(SparkR, lib.loc = c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib")))' >> /home/{}/.Rprofile'''.format(os_user))
            sudo('rstudio-server start')
            sudo('echo "{0}:{1}" | chpasswd'.format(os_user, rstudio_pass))
            sudo("sed -i '/exit 0/d' /etc/rc.local")
            sudo('''bash -c "echo \'sed -i 's/^#SPARK_HOME/SPARK_HOME/' /home/{}/.Renviron\' >> /etc/rc.local"'''.format(os_user))
            sudo("bash -c 'echo exit 0 >> /etc/rc.local'")
            sudo('touch /home/{}/.ensure_dir/rstudio_ensured'.format(os_user))
        except:
            sys.exit(1)
    else:
        try:
            sudo('echo "{0}:{1}" | chpasswd'.format(os_user, rstudio_pass))
        except:
            sys.exit(1)


def ensure_matplot(os_user):
    if not exists('/home/{}/.ensure_dir/matplot_ensured'.format(os_user)):
        try:
            sudo('pip2 install matplotlib --no-cache-dir')
            sudo('python3.5 -m pip install matplotlib --no-cache-dir')
            if os.environ['application'] == 'tensor':
                sudo('rm -rf  /usr/lib64/python2.7/site-packages/numpy*')
                sudo('python2.7 -m pip install -U numpy')
            sudo('touch /home/{}/.ensure_dir/matplot_ensured'.format(os_user))
        except:
            sys.exit(1)


def ensure_sbt(os_user):
    if not exists('/home/{}/.ensure_dir/sbt_ensured'.format(os_user)):
        try:
            sudo('curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo')
            sudo('yum install -y sbt')
            sudo('touch /home/{}/.ensure_dir/sbt_ensured'.format(os_user))
        except:
            sys.exit(1)


def ensure_jre_jdk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/jre_jdk_ensured'):
        try:
            sudo('yum install -y java-1.8.0-openjdk')
            sudo('yum install -y java-1.8.0-openjdk-devel')
            sudo('touch /home/' + os_user + '/.ensure_dir/jre_jdk_ensured')
        except:
            sys.exit(1)


def ensure_scala(scala_link, scala_version, os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/scala_ensured'):
        try:
            sudo('wget {}scala-{}.rpm -O /tmp/scala.rpm'.format(scala_link, scala_version))
            sudo('rpm -i /tmp/scala.rpm')
            sudo('touch /home/' + os_user + '/.ensure_dir/scala_ensured')
        except:
            sys.exit(1)


def ensure_additional_python_libs(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/additional_python_libs_ensured'):
        try:
            sudo('yum clean all')
            sudo('yum install -y zlib-devel libjpeg-turbo-devel --nogpgcheck')
            if os.environ['application'] == 'jupyter' or os.environ['application'] == 'zeppelin':
                sudo('pip2 install NumPy SciPy pandas Sympy Pillow sklearn --no-cache-dir')
                sudo('python3.5 -m pip install NumPy SciPy pandas Sympy Pillow sklearn --no-cache-dir')
            if os.environ['application'] == 'zeppelin':
                sudo('wget http://mirror.centos.org/centos/7/os/x86_64/Packages/tkinter-2.7.5-48.el7.x86_64.rpm')
                sudo('yum install -y tkinter-2.7.5-48.el7.x86_64.rpm --nogpgcheck')
                sudo('yum install -y python35u-tkinter')
            if os.environ['application'] == 'tensor':
                sudo('python2.7 -m pip install keras opencv-python h5py --no-cache-dir')
                sudo('python2.7 -m ipykernel install')
                sudo('python3.5 -m pip install keras opencv-python h5py --no-cache-dir')
                sudo('python3.5 -m ipykernel install')
            sudo('touch /home/' + os_user + '/.ensure_dir/additional_python_libs_ensured')
        except:
            sys.exit(1)


def ensure_python3_specific_version(python3_version, os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python3_specific_version_ensured'):
        try:
            sudo('yum install -y yum-utils python34 openssl-devel')
            sudo('yum -y groupinstall development --nogpgcheck')
            if len(python3_version) < 4:
                python3_version = python3_version + ".0"
            sudo('wget https://www.python.org/ftp/python/{0}/Python-{0}.tgz'.format(python3_version))
            sudo('tar xzf Python-{0}.tgz; cd Python-{0}; ./configure --prefix=/usr/local; make altinstall'.format(python3_version))
            sudo('touch /home/' + os_user + '/.ensure_dir/python3_specific_version_ensured')
        except:
            sys.exit(1)


def ensure_python2_libraries(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python2_libraries_ensured'):
        try:
            sudo('yum install -y https://forensics.cert.org/centos/cert/7/x86_64/pyparsing-2.0.3-1.el7.noarch.rpm')
            sudo('yum install -y python-setuptools python-wheel')
            sudo('yum install -y python-virtualenv openssl-devel python-devel openssl-libs libxml2-devel libxslt-devel --nogpgcheck')
            sudo('python2 -m pip install backports.shutil_get_terminal_size ipython ipykernel')
            sudo('echo y | python2 -m pip uninstall backports.shutil_get_terminal_size')
            sudo('python2 -m pip install backports.shutil_get_terminal_size')
            sudo('pip2 install -U pip setuptools --no-cache-dir')
            sudo('pip2 install boto3 --no-cache-dir')
            sudo('pip2 install fabvenv fabric-virtualenv --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python2_libraries_ensured')
        except:
            sys.exit(1)


def ensure_python3_libraries(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python3_libraries_ensured'):
        try:
            sudo('yum -y install https://centos7.iuscommunity.org/ius-release.rpm')
            sudo('yum install -y python35u python35u-pip python35u-devel')
            sudo('python3.5 -m pip install -U pip setuptools --no-cache-dir')
            sudo('python3.5 -m pip install boto3 --no-cache-dir')
            sudo('python3.5 -m pip install fabvenv fabric-virtualenv --no-cache-dir')
            sudo('python3.5 -m pip install ipython ipykernel --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python3_libraries_ensured')
        except:
            sys.exit(1)


def install_tensor(os_user, tensorflow_version, files_dir, templates_dir):
    if not exists('/home/' + os_user + '/.ensure_dir/tensor_ensured'):
        try:
            # install nvidia drivers
            sudo('echo "blacklist nouveau" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('echo "options nouveau modeset=0" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('dracut --force')
            sudo('shutdown -r 1')
            time.sleep(90)
            sudo('yum -y install gcc kernel-devel-$(uname -r) kernel-headers-$(uname -r)')
            sudo('wget http://us.download.nvidia.com/XFree86/Linux-x86_64/367.57/NVIDIA-Linux-x86_64-367.57.run -O /home/' + os_user + '/NVIDIA-Linux-x86_64-367.57.run')
            sudo('/bin/bash /home/' + os_user + '/NVIDIA-Linux-x86_64-367.57.run -s --no-install-libglvnd')
            sudo('rm -f /home/' + os_user + '/NVIDIA-Linux-x86_64-367.57.run')
            # install cuda
            sudo('wget https://developer.nvidia.com/compute/cuda/8.0/prod/local_installers/cuda-repo-rhel7-8-0-local-8.0.44-1.x86_64-rpm')
            sudo('mv cuda-repo-rhel7-8-0-local-8.0.44-1.x86_64-rpm cuda-repo-rhel7-8-0-local-8.0.44-1.x86_64.rpm; rpm -i cuda-repo-rhel7-8-0-local-8.0.44-1.x86_64.rpm')
            sudo('yum clean all')
            sudo('yum -y install cuda')
            sudo('python3.5 -m pip install --upgrade pip wheel numpy')
            sudo('mv /usr/local/cuda-8.0 /opt/')
            sudo('ln -s /opt/cuda-8.0 /usr/local/cuda-8.0')
            sudo('rm -rf /home/' + os_user + '/cuda-repo-rhel7-8-0-local-8.0.44-1.x86_64.rpm')
            # install cuDNN
            put(files_dir + 'cudnn-8.0-linux-x64-v5.1.tgz', '/tmp/cudnn-8.0-linux-x64-v5.1.tgz')
            run('tar xvzf /tmp/cudnn-8.0-linux-x64-v5.1.tgz -C /tmp')
            sudo('mkdir -p /opt/cudnn/include')
            sudo('mkdir -p /opt/cudnn/lib64')
            sudo('mv /tmp/cuda/include/cudnn.h /opt/cudnn/include')
            sudo('mv /tmp/cuda/lib64/libcudnn* /opt/cudnn/lib64')
            sudo('chmod a+r /opt/cudnn/include/cudnn.h /opt/cudnn/lib64/libcudnn*')
            run('echo "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/opt/cudnn/lib64\"" >> ~/.bashrc')
            # install TensorFlow and run TensorBoard
            sudo('wget https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-' + tensorflow_version + '-cp27-none-linux_x86_64.whl')
            sudo('wget https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-' + tensorflow_version + '-cp35-cp35m-linux_x86_64.whl')
            sudo('python2.7 -m pip install --upgrade tensorflow_gpu-' + tensorflow_version + '-cp27-none-linux_x86_64.whl')
            sudo('python3.5 -m pip install --upgrade tensorflow_gpu-' + tensorflow_version + '-cp35-cp35m-linux_x86_64.whl')
            sudo('rm -rf /home/' + os_user + '/tensorflow_gpu-*')
            sudo('mkdir /var/log/tensorboard; chown ' + os_user + ':' + os_user + ' -R /var/log/tensorboard')
            put(templates_dir + 'tensorboard.service', '/tmp/tensorboard.service')
            sudo("sed -i 's|OS_USR|" + os_user + "|' /tmp/tensorboard.service")
            sudo("chmod 644 /tmp/tensorboard.service")
            sudo('\cp /tmp/tensorboard.service /etc/systemd/system/')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable tensorboard")
            sudo("systemctl start tensorboard")
            # install Theano
            sudo('python2.7 -m pip install Theano')
            sudo('python3.5 -m pip install Theano')
            sudo('touch /home/' + os_user + '/.ensure_dir/tensor_ensured')
        except:
            sys.exit(1)


def install_maven(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        sudo('wget http://apache.volia.net/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz -O /tmp/maven.tar.gz')
        sudo('tar -zxvf /tmp/maven.tar.gz -C /opt/')
        sudo('ln -fs /opt/apache-maven-3.3.9/bin/mvn /usr/bin/mvn')
        sudo('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        sudo('pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        sudo('pip3.5 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        sudo('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_maven_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        local('wget http://apache.volia.net/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz -O /tmp/maven.tar.gz')
        local('sudo tar -zxvf /tmp/maven.tar.gz -C /opt/')
        local('sudo ln -fs /opt/apache-maven-3.3.9/bin/mvn /usr/bin/mvn')
        local('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        local('sudo -i pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        local('sudo -i pip3.5 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        local('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_gitweb(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/gitweb_ensured'):
        sudo('yum install -y policycoreutils-python httpd perl-CGI --nogpgcheck')
        with cd('/tmp'):
            sudo('git clone https://github.com/git/git.git')
            with cd('git'):
                sudo('make GITWEB_PROJECTROOT="/home/' + os_user + '" prefix=/usr gitweb')
                sudo('cp -Rf gitweb /var/www/')
        sudo('sed -i -e "s/Listen 80/Listen 8085/g" /etc/httpd/conf/httpd.conf')
        sudo('sed -i -e "s/User apache/User ' + os_user + '/g" /etc/httpd/conf/httpd.conf')
        sudo('sed -i -e "s/Group apache/Group ' + os_user + '/g" /etc/httpd/conf/httpd.conf')
        sudo('sed -i -e "/IncludeOptional/ s/^#*/#/" /etc/httpd/conf/httpd.conf')
        put('/root/templates/gitweb-virtualhost.conf', '/tmp/gitweb-virtualhost.conf')
        sudo('echo "#Virualhost for GitWeb" | sudo tee -a /etc/httpd/conf/httpd.conf')
        sudo('cat /tmp/gitweb-virtualhost.conf | sudo tee -a /etc/httpd/conf/httpd.conf')
        sudo('chcon -R -t httpd_unconfined_script_exec_t /var/www/gitweb')
        sudo('semanage port -a -t http_port_t -p tcp 8085')
        sudo('systemctl enable httpd.service')
        sudo('systemctl start httpd.service')
        sudo('touch /home/' + os_user + '/.ensure_dir/gitweb_ensured')


def install_opencv(os_user):
    if not exists('/home/{}/.ensure_dir/opencv_ensured'.format(os_user)):
        sudo('yum install -y cmake python3 python3-devel python3-numpy gcc gcc-c++')
        sudo('pip2 install numpy')
        sudo('pip3.5 install numpy')
        run('git clone https://github.com/opencv/opencv.git')
        with cd('/home/{}/opencv/'.format(os_user)):
            run('git checkout 3.2.0')
            run('mkdir release')
        with cd('/home/{}/opencv/release/'.format(os_user)):
            run('cmake -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=$(python2 -c "import sys; print(sys.prefix)") -D PYTHON_EXECUTABLE=$(which python2) ..')
            run('make -j4')
            sudo('make install')
        sudo('touch /home/' + os_user + '/.ensure_dir/opencv_ensured')


def install_caffe(os_user):
    if not exists('/home/{}/.ensure_dir/caffe_ensured'.format(os_user)):
        env.shell = "/bin/bash -l -c -i"
        install_opencv(os_user)
        sudo('yum install -y protobuf-devel leveldb-devel snappy-devel opencv-devel boost-devel hdf5-devel gcc gcc-c++')
        sudo('yum install -y gflags-devel glog-devel lmdb-devel')
        sudo('yum install -y openblas-devel gflags-devel glog-devel lmdb-devel')
        sudo('git clone https://github.com/BVLC/caffe.git')
        with cd('/home/{}/caffe/'.format(os_user)):
            sudo('pip2 install -r python/requirements.txt')
            sudo('pip3.5 install -r python/requirements.txt')
            sudo('cp Makefile.config.example Makefile.config')
            sudo('sed -i \'/^PYTHON_INCLUDE \:=/s/\\\\/\/usr\/lib64\/python2.7\/site-packages\/numpy\/core\/include /g\' Makefile.config')
            sudo('sed -i \'/\/usr\/lib\/python2.7\/dist-packages\/numpy\/core\/include/d\' Makefile.config')
            sudo('sed -i \'/BLAS \:=/d\' Makefile.config')
            sudo('echo "BLAS := open" >> Makefile.config')
            sudo('echo "BLAS_INCLUDE := /usr/include/openblas" >> Makefile.config')
            sudo('echo "OPENCV_VERSION := 3" >> Makefile.config')
            sudo('echo "LIBRARIES += glog gflags protobuf boost_system boost_filesystem m hdf5_serial_hl hdf5_serial" '
                 '>> Makefile.config')
            sudo('sed -i \'/INCLUDE_DIRS \:=/s/$/ \/usr \/usr\/lib64/g\' Makefile.config')
            sudo('sed -i \'/LIBRARY_DIRS \:=/s/$/ \/usr \/usr\/lib64/g\' Makefile.config')
            sudo('make all')
            sudo('make test')
            sudo('make runtest')
            sudo('make pycaffe')
        sudo('touch /home/' + os_user + '/.ensure_dir/caffe_ensured')


def install_caffe2(os_user):
    if not exists('/home/{}/.ensure_dir/caffe2_ensured'.format(os_user)):
        env.shell = "/bin/bash -l -c -i"
        sudo('yum update')
        sudo('yum install -y automake cmake3 gcc gcc-c++ kernel-devel leveldb-devel lmdb-devel libtool protobuf-devel '
             'python-devel snappy-devel')
        sudo('yum install -y http://mirror.centos.org/centos/7/os/x86_64/Packages/snappy-devel-1.1.0-3.el7.x86_64.rpm')
        sudo('pip2 install flask graphviz hypothesis jupyter matplotlib numpy protobuf pydot python-nvd3 pyyaml '
             'requests scikit-image scipy setuptools tornado future')
        sudo('pip3.5 install flask graphviz hypothesis jupyter matplotlib numpy protobuf pydot python-nvd3 pyyaml '
             'requests scikit-image scipy setuptools tornado future')
        sudo('cp /opt/cudnn/include/* /opt/cuda-8.0/include/')
        sudo('cp /opt/cudnn/lib64/* /opt/cuda-8.0/lib64/')
        sudo('git clone --recursive https://github.com/caffe2/caffe2')
        with cd('/home/{}/caffe2/'.format(os_user)):
            sudo('mkdir build')
        with cd('/home/{}/caffe2/build/'.format(os_user)):
            sudo('cmake3 ..')
            sudo('make -j8 install')
        sudo('touch /home/' + os_user + '/.ensure_dir/caffe2_ensured')


def install_cntk(os_user):
    if not exists('/home/{}/.ensure_dir/cntk_ensured'.format(os_user)):
        sudo('pip2 install https://cntk.ai/PythonWheel/GPU/cntk-2.0rc3-cp27-cp27mu-linux_x86_64.whl')
        sudo('pip3.5 install https://cntk.ai/PythonWheel/GPU/cntk-2.0rc3-cp35-cp35m-linux_x86_64.whl')
        sudo('touch /home/{}/.ensure_dir/cntk_ensured'.format(os_user))


def install_keras(os_user):
    if not exists('/home/{}/.ensure_dir/keras_ensured'.format(os_user)):
        sudo('pip2 install keras')
        sudo('pip3.5 install keras')
        sudo('touch /home/{}/.ensure_dir/keras_ensured'.format(os_user))


def install_mxnet(os_user):
    if not exists('/home/{}/.ensure_dir/mxnet_ensured'.format(os_user)):
        sudo('pip2 install mxnet-cu80 opencv-python')
        sudo('pip3.5 install mxnet-cu80 opencv-python')
        sudo('touch /home/{}/.ensure_dir/mxnet_ensured'.format(os_user))
