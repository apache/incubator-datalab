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


def ensure_r(os_user, r_libs, region, r_mirror):
    if not exists('/home/{}/.ensure_dir/r_ensured'.format(os_user)):
        try:
            if region == 'cn-north-1':
                r_repository = r_mirror
            else:
                r_repository = 'http://cran.us.r-project.org'
            sudo('yum install -y cmake')
            sudo('yum -y install libcur*')
            sudo('echo -e "[base]\nname=CentOS-7-Base\nbaseurl=http://buildlogs.centos.org/centos/7/os/x86_64-20140704-1/\ngpgcheck=1\ngpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7\npriority=1\nexclude=php mysql" >> /etc/yum.repos.d/CentOS-base.repo')
            sudo('yum install -y R R-core R-core-devel R-devel --nogpgcheck')
            sudo('R CMD javareconf')
            sudo('cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig')
            for i in r_libs:
                sudo('R -e "install.packages(\'{}\',repos=\'{}\')"'.format(i, r_repository))
            sudo('R -e "library(\'devtools\');install.packages(repos=\'{}\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"'.format(r_repository))
            sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            sudo('R -e "install.packages(\'RJDBC\',repos=\'{}\',dep=TRUE)"'.format(r_repository))
            sudo('touch /home/{}/.ensure_dir/r_ensured'.format(os_user))
        except:
            sys.exit(1)


def install_rstudio(os_user, local_spark_path, rstudio_pass, rstudio_version):
    if not exists('/home/' + os_user + '/.ensure_dir/rstudio_ensured'):
        try:
            sudo('yum install -y --nogpgcheck https://download2.rstudio.org/rstudio-server-rhel-{}-x86_64.rpm'.format(rstudio_version))
            sudo('mkdir -p /mnt/var')
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
            sudo('pip2 install matplotlib==2.0.2 --no-cache-dir')
            sudo('python3.5 -m pip install matplotlib==2.0.2 --no-cache-dir')
            if os.environ['application'] == 'tensor':
                sudo('rm -rf  /usr/lib64/python2.7/site-packages/numpy*')
                sudo('python2.7 -m pip install -U numpy --no-cache-dir')
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
            if os.environ['application'] == 'tensor':
                sudo('python2.7 -m pip install opencv-python h5py --no-cache-dir')
                sudo('python2.7 -m ipykernel install')
                sudo('python3.5 -m pip install opencv-python h5py --no-cache-dir')
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
            sudo('pip2 install pyparsing==2.0.3')
            sudo('yum install -y python-setuptools python-wheel')
            sudo('yum install -y python-virtualenv openssl-devel python-devel openssl-libs libxml2-devel libxslt-devel --nogpgcheck')
            try:
                sudo('python2 -m pip install backports.shutil_get_terminal_size ipython ipykernel --no-cache-dir')
            except:
                sudo('python2 -m pip install backports.shutil_get_terminal_size ipython==5.0.0 ipykernel --no-cache-dir')
            sudo('echo y | python2 -m pip uninstall backports.shutil_get_terminal_size')
            sudo('python2 -m pip install backports.shutil_get_terminal_size --no-cache-dir')
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
            try:
                sudo('python3.5 -m pip install ipython ipykernel --no-cache-dir')
            except:
                sudo('python3.5 -m pip install ipython==5.0.0 ipykernel --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python3_libraries_ensured')
        except:
            sys.exit(1)


def install_tensor(os_user, tensorflow_version, files_dir, templates_dir, nvidia_version):
    if not exists('/home/' + os_user + '/.ensure_dir/tensor_ensured'):
        try:
            # install nvidia drivers
            sudo('echo "blacklist nouveau" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('echo "options nouveau modeset=0" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('dracut --force')
            with settings(warn_only=True):
                reboot(wait=90)
            sudo('yum -y install gcc kernel-devel-$(uname -r) kernel-headers-$(uname -r)')
            sudo('wget http://us.download.nvidia.com/XFree86/Linux-x86_64/{0}/NVIDIA-Linux-x86_64-{0}.run -O /home/{1}/NVIDIA-Linux-x86_64-{0}.run'.format(nvidia_version, os_user))
            sudo('/bin/bash /home/{0}/NVIDIA-Linux-x86_64-{1}.run -s'.format(os_user, nvidia_version))
            sudo('rm -f /home/{0}/NVIDIA-Linux-x86_64-{1}.run'.format(os_user, nvidia_version))
            # install cuda
            sudo('python3.5 -m pip install --upgrade pip wheel numpy --no-cache-dir')
            sudo('wget -P /opt https://developer.nvidia.com/compute/cuda/8.0/prod/local_installers/cuda_8.0.44_linux-run')
            sudo('sh /opt/cuda_8.0.44_linux-run --silent --toolkit')
            sudo('mv /usr/local/cuda-8.0 /opt/')
            sudo('ln -s /opt/cuda-8.0 /usr/local/cuda-8.0')
            sudo('rm -f /opt/cuda_8.0.44_linux-run')
            # install cuDNN
            cudnn = 'cudnn-8.0-linux-x64-v6.0.tgz'
            run('wget http://developer.download.nvidia.com/compute/redist/cudnn/v6.0/{0} -O /tmp/{0}'.format(cudnn))
            run('tar xvzf /tmp/{} -C /tmp'.format(cudnn))
            sudo('mkdir -p /opt/cudnn/include')
            sudo('mkdir -p /opt/cudnn/lib64')
            sudo('mv /tmp/cuda/include/cudnn.h /opt/cudnn/include')
            sudo('mv /tmp/cuda/lib64/libcudnn* /opt/cudnn/lib64')
            sudo('chmod a+r /opt/cudnn/include/cudnn.h /opt/cudnn/lib64/libcudnn*')
            run('echo "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64\"" >> ~/.bashrc')
            # install TensorFlow and run TensorBoard
            sudo('wget https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-' + tensorflow_version + '-cp27-none-linux_x86_64.whl')
            sudo('wget https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-' + tensorflow_version + '-cp35-cp35m-linux_x86_64.whl')
            sudo('python2.7 -m pip install --upgrade tensorflow_gpu-' + tensorflow_version + '-cp27-none-linux_x86_64.whl --no-cache-dir')
            sudo('python3.5 -m pip install --upgrade tensorflow_gpu-' + tensorflow_version + '-cp35-cp35m-linux_x86_64.whl --no-cache-dir')
            sudo('rm -rf /home/' + os_user + '/tensorflow_gpu-*')
            sudo('mkdir /var/log/tensorboard; chown ' + os_user + ':' + os_user + ' -R /var/log/tensorboard')
            put(templates_dir + 'tensorboard.service', '/tmp/tensorboard.service')
            sudo("sed -i 's|OS_USR|" + os_user + "|' /tmp/tensorboard.service")
            sudo("chmod 644 /tmp/tensorboard.service")
            sudo('\cp /tmp/tensorboard.service /etc/systemd/system/')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable tensorboard")
            sudo("systemctl start tensorboard")
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
        sudo('pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        sudo('pip3.5 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        sudo('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_maven_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        local('wget http://apache.volia.net/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz -O /tmp/maven.tar.gz')
        local('sudo tar -zxvf /tmp/maven.tar.gz -C /opt/')
        local('sudo ln -fs /opt/apache-maven-3.3.9/bin/mvn /usr/bin/mvn')
        local('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        local('sudo -i pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        local('sudo -i pip3.5 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        local('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_nodejs(os_user):
    if not exists('/home/{}/.ensure_dir/nodejs_ensured'.format(os_user)):
        sudo('yum install -y npm nodejs')
        sudo('touch /home/{}/.ensure_dir/nodejs_ensured'.format(os_user))


def install_os_pkg(requisites):
    status = list()
    error_parser = "Could not|No matching|Error:|failed|Requires:|Errno"
    try:
        print("Updating repositories and installing requested tools: {}".format(requisites))
        sudo('yum update-minimal --security -y --skip-broken')
        sudo('export LC_ALL=C')
        for os_pkg in requisites:
            sudo('yum -y install {0} --nogpgcheck 2>&1 | if ! grep -w -E  "({1})" >  /tmp/os_install_{0}.log; then  echo "" > /tmp/os_install_{0}.log;fi'.format(os_pkg, error_parser))
            err = sudo('cat /tmp/os_install_{}.log'.format(os_pkg)).replace('"', "'")
            try:
                res = sudo('python -c "import os,sys,yum; yb = yum.YumBase(); pl = yb.doPackageLists(); print [pkg.vr for pkg in pl.installed if pkg.name == \'{0}\'][0]"'.format(os_pkg))
                version = res.split('\r\n')[1].replace("'", "\"")
                status.append({"group": "os_pkg", "name": os_pkg, "version": version, "status": "installed"})
            except:
                status.append({"group": "os_pkg", "name": os_pkg, "status": "failed", "error_message": err})
        return status
    except:
        return "Fail to install OS packages"


def get_available_os_pkgs():
    try:
        sudo('yum update-minimal --security -y --skip-broken')
        yum_raw = sudo('python -c "import os,sys,yum; yb = yum.YumBase(); pl = yb.doPackageLists(); print {pkg.name:pkg.vr for pkg in pl.available}"')
        yum_list = yum_raw.split('\r\n')[1].replace("'","\"")
        os_pkgs = json.loads(yum_list)
        return os_pkgs
    except:
        sys.exit(1)


def install_opencv(os_user):
    if not exists('/home/{}/.ensure_dir/opencv_ensured'.format(os_user)):
        sudo('yum install -y cmake python34 python34-devel python34-pip gcc gcc-c++')
        sudo('pip2 install numpy --no-cache-dir')
        sudo('pip3.4 install numpy --no-cache-dir')
        sudo('pip3.5 install numpy --no-cache-dir')
        run('git clone https://github.com/opencv/opencv.git')
        with cd('/home/{}/opencv/'.format(os_user)):
            run('git checkout 3.2.0')
            run('mkdir release')
        with cd('/home/{}/opencv/release/'.format(os_user)):
            run('cmake -DINSTALL_TESTS=OFF -D CUDA_GENERATION=Auto -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=$(python2 -c "import sys; print(sys.prefix)") -D PYTHON_EXECUTABLE=$(which python2) ..')
            run('make -j$(nproc)')
            sudo('make install')
        sudo('touch /home/' + os_user + '/.ensure_dir/opencv_ensured')


def install_caffe(os_user, region, caffe_version):
    if not exists('/home/{}/.ensure_dir/caffe_ensured'.format(os_user)):
        env.shell = "/bin/bash -l -c -i"
        install_opencv(os_user)
        with cd('/etc/yum.repos.d/'):
            if region == 'cn-north-1':
                mirror = 'mirror.lzu.edu.cn'
            else:
                mirror = 'mirror.centos.org'
            sudo('echo "[centosrepo]" > centos.repo')
            sudo('echo "name=Centos 7 Repository" >> centos.repo')
            sudo('echo "baseurl=http://{}/centos/7/os/x86_64/" >> centos.repo'.format(mirror))
            sudo('echo "enabled=1" >> centos.repo')
            sudo('echo "gpgcheck=1" >> centos.repo')
            sudo('echo "gpgkey=http://{}/centos/7/os/x86_64/RPM-GPG-KEY-CentOS-7" >> centos.repo'.format(mirror))
        sudo('yum update-minimal --security -y')
        sudo('yum install -y protobuf-devel leveldb-devel snappy-devel boost-devel hdf5-devel gcc gcc-c++')
        sudo('yum install -y gflags-devel glog-devel lmdb-devel yum-utils && package-cleanup --cleandupes')
        sudo('yum install -y openblas-devel gflags-devel glog-devel lmdb-devel')
        sudo('git clone https://github.com/BVLC/caffe.git')
        with cd('/home/{}/caffe/'.format(os_user)):
            sudo('git checkout {}'.format(caffe_version))
            sudo('pip2 install -r python/requirements.txt --no-cache-dir')
            sudo('pip3.5 install -r python/requirements.txt --no-cache-dir')
            sudo('echo "CUDA_DIR := /usr/local/cuda" > Makefile.config')
            cuda_arch = sudo("/opt/cuda-8.0/extras/demo_suite/deviceQuery | grep 'CUDA Capability' | tr -d ' ' | cut -f2 -d ':'")
            sudo('echo "CUDA_ARCH := -gencode arch=compute_{0},code=sm_{0}" >> Makefile.config'.format(cuda_arch.replace('.', '')))
            sudo('echo "PYTHON_INCLUDE := /usr/include/python2.7 /usr/lib64/python2.7/site-packages/numpy/core/include" >> Makefile.config')
            sudo('echo "BLAS := open" >> Makefile.config')
            sudo('echo "BLAS_INCLUDE := /usr/include/openblas" >> Makefile.config')
            sudo('echo "OPENCV_VERSION := 3" >> Makefile.config')
            sudo('echo "LIBRARIES += glog gflags protobuf boost_system boost_filesystem m hdf5_serial_hl hdf5_serial" >> Makefile.config')
            sudo('echo "PYTHON_LIB := /usr/lib" >> Makefile.config')
            sudo('echo "INCLUDE_DIRS := \\\$(PYTHON_INCLUDE) /usr/local/include /usr /usr/lib64 /usr/include/python2.7 /usr/lib64/python2.7/site-packages/numpy/core/include" >> Makefile.config')
            sudo('echo "LIBRARY_DIRS := \\\$(PYTHON_LIB) /usr/local/lib /usr/lib /usr /usr/lib64" >> Makefile.config')
            sudo('echo "BUILD_DIR := build" >> Makefile.config')
            sudo('echo "DISTRIBUTE_DIR := distribute" >> Makefile.config')
            sudo('echo "TEST_GPUID := 0" >> Makefile.config')
            sudo('echo "Q ?= @" >> Makefile.config')
            sudo('make all -j$(nproc)')
            sudo('make test -j$(nproc)')
            run('make runtest')
            sudo('make pycaffe')
        sudo('touch /home/' + os_user + '/.ensure_dir/caffe_ensured')


def install_caffe2(os_user, caffe2_version):
    if not exists('/home/{}/.ensure_dir/caffe2_ensured'.format(os_user)):
        env.shell = "/bin/bash -l -c -i"
        sudo('yum update-minimal --security -y')
        sudo('yum install -y automake cmake3 gcc gcc-c++ kernel-devel leveldb-devel lmdb-devel libtool protobuf-devel graphviz')
        sudo('pip2 install flask graphviz hypothesis jupyter matplotlib==2.0.2 numpy protobuf pydot python-nvd3 pyyaml '
             'requests scikit-image scipy setuptools tornado future --no-cache-dir')
        sudo('pip3.5 install flask graphviz hypothesis jupyter matplotlib==2.0.2 numpy protobuf pydot python-nvd3 pyyaml '
             'requests scikit-image scipy setuptools tornado future --no-cache-dir')
        sudo('cp /opt/cudnn/include/* /opt/cuda-8.0/include/')
        sudo('cp /opt/cudnn/lib64/* /opt/cuda-8.0/lib64/')
        sudo('git clone --recursive https://github.com/caffe2/caffe2')
        cuda_arch = sudo("/opt/cuda-8.0/extras/demo_suite/deviceQuery | grep 'CUDA Capability' | tr -d ' ' | cut -f2 -d ':'")
        with cd('/home/{}/caffe2/'.format(os_user)):
            with settings(warn_only=True):
                sudo('git checkout v{}'.format(caffe2_version))
                sudo('git submodule update --recursive')
            sudo('mkdir build && cd build && cmake .. -DCUDA_ARCH_BIN="{0}" -DCUDA_ARCH_PTX="{0}" && make "-j$(nproc)" install'.format(cuda_arch.replace('.', '')))
        sudo('touch /home/' + os_user + '/.ensure_dir/caffe2_ensured')


def install_cntk(os_user, cntk_version):
    if not exists('/home/{}/.ensure_dir/cntk_ensured'.format(os_user)):
        sudo('echo "exclude=*.i386 *.i686" >> /etc/yum.conf')
        sudo('yum clean all && yum update-minimal --security -y')
        sudo('yum install -y openmpi openmpi-devel --nogpgcheck')
        sudo('sed -i "s/LD_LIBRARY_PATH:/LD_LIBRARY_PATH:\/usr\/lib64\/openmpi\/lib:/g" /etc/systemd/system/jupyter-notebook.service')
        sudo('systemctl daemon-reload')
        sudo('systemctl restart jupyter-notebook')
        sudo('pip2 install https://cntk.ai/PythonWheel/GPU/cntk-{}-cp27-cp27mu-linux_x86_64.whl --no-cache-dir'.format(cntk_version))
        sudo('pip3.5 install https://cntk.ai/PythonWheel/GPU/cntk-{}-cp35-cp35m-linux_x86_64.whl --no-cache-dir'.format(cntk_version))
        sudo('touch /home/{}/.ensure_dir/cntk_ensured'.format(os_user))


def install_keras(os_user, keras_version):
    if not exists('/home/{}/.ensure_dir/keras_ensured'.format(os_user)):
        sudo('pip2 install keras=={} --no-cache-dir'.format(keras_version))
        sudo('pip3.5 install keras=={} --no-cache-dir'.format(keras_version))
        sudo('pip2 uninstall -y ipython')
        sudo('pip2 install ipython --no-cache-dir')
        sudo('pip2 install matplotlib==2.0.2 --no-cache-dir')
        sudo('pip2 uninstall -y numpy')
        sudo('pip2 install numpy --no-cache-dir')
        sudo('touch /home/{}/.ensure_dir/keras_ensured'.format(os_user))


def install_theano(os_user, theano_version):
    if not exists('/home/{}/.ensure_dir/theano_ensured'.format(os_user)):
        sudo('python2.7 -m pip install Theano=={} --no-cache-dir'.format(theano_version))
        sudo('python3.5 -m pip install Theano=={} --no-cache-dir'.format(theano_version))
        sudo('touch /home/{}/.ensure_dir/theano_ensured'.format(os_user))


def install_mxnet(os_user, mxnet_version):
    if not exists('/home/{}/.ensure_dir/mxnet_ensured'.format(os_user)):
        sudo('pip2 install mxnet-cu80=={} opencv-python --no-cache-dir'.format(mxnet_version))
        sudo('pip3.5 install mxnet-cu80=={} opencv-python --no-cache-dir'.format(mxnet_version))
        sudo('touch /home/{}/.ensure_dir/mxnet_ensured'.format(os_user))


def install_torch(os_user):
    if not exists('/home/{}/.ensure_dir/torch_ensured'.format(os_user)):
        run('git clone https://github.com/torch/distro.git ~/torch --recursive')
        with cd('/home/{}/torch/'.format(os_user)):
            sudo('yum install -y --nogpgcheck cmake curl readline-devel ncurses-devel gcc-c++ gcc-gfortran git '
                 'gnuplot unzip libjpeg-turbo-devel libpng-devel ImageMagick GraphicsMagick-devel fftw-devel '
                 'sox-devel sox zeromq3-devel qt-devel qtwebkit-devel sox-plugins-freeworld ipython qt-devel')
            run('./install.sh -b')
        run('source /home/{}/.bashrc'.format(os_user))
        sudo('touch /home/{}/.ensure_dir/torch_ensured'.format(os_user))


def install_gitlab_cert(os_user, certfile):
    try:
        sudo('mv -f /home/{0}/{1} /etc/pki/ca-trust/source/anchors/{1}'.format(os_user, certfile))
        sudo('update-ca-trust')
    except Exception as err:
        print('Failed to install gitlab certificate.{}'.format(str(err)))
