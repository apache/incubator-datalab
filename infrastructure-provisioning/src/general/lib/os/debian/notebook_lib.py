#!/usr/bin/python

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

import backoff
import os
import re
import sys
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric.api import *
from fabric.contrib.files import exists


def enable_proxy(proxy_host, proxy_port):
    try:
        proxy_string = "http://%s:%s" % (proxy_host, proxy_port)
        proxy_https_string = "http://%s:%s" % (proxy_host, proxy_port)
        sudo('sed -i "/^export http_proxy/d" /etc/profile')
        sudo('sed -i "/^export https_proxy/d" /etc/profile')
        sudo('echo export http_proxy=' + proxy_string + ' >> /etc/profile')
        sudo('echo export https_proxy=' + proxy_string + ' >> /etc/profile')
        if exists('/etc/apt/apt.conf'):
            sudo("sed -i '/^Acquire::http::Proxy/d' /etc/apt/apt.conf")
        sudo("echo 'Acquire::http::Proxy \"" + proxy_string + "\";' >> /etc/apt/apt.conf")
        sudo("echo 'Acquire::http::Proxy \"" + proxy_https_string + "\";' >> /etc/apt/apt.conf")

        print("Renewing gpg key")
        renew_gpg_key()
    except:
        sys.exit(1)


def ensure_r_local_kernel(spark_version, os_user, templates_dir, kernels_dir):
    if not exists('/home/' + os_user + '/.ensure_dir/r_local_kernel_ensured'):
        try:
            sudo('R -e "IRkernel::installspec()"')
            r_version = sudo("R --version | awk '/version / {print $3}'")
            put(templates_dir + 'r_template.json', '/tmp/r_template.json')
            sudo('sed -i "s|R_VER|' + r_version + '|g" /tmp/r_template.json')
            sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/r_template.json')
            sudo('\cp -f /tmp/r_template.json {}/ir/kernel.json'.format(kernels_dir))
            sudo('ln -s /opt/spark/ /usr/local/spark')
            try:
                sudo('cd /usr/local/spark/R/lib/SparkR; R -e "install.packages(\'roxygen2\',repos=\'https://cloud.r-project.org\')" R -e "devtools::check(\'.\')"')
            except:
                pass
            sudo('cd /usr/local/spark/R/lib/SparkR; R -e "devtools::install(\'.\')"')
            sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            sudo('touch /home/' + os_user + '/.ensure_dir/r_local_kernel_ensured')
        except:
            sys.exit(1)

@backoff.on_exception(backoff.expo, SystemExit, max_tries=20)
def add_marruter_key():
    try:
        sudo('add-apt-repository -y ppa:marutter/rrutter')
    except:
        sys.exit(1)

def ensure_r(os_user, r_libs, region, r_mirror):
    if not exists('/home/' + os_user + '/.ensure_dir/r_ensured'):
        try:
            if region == 'cn-north-1':
                r_repository = r_mirror
            else:
                r_repository = 'https://cloud.r-project.org'
            add_marruter_key()
            sudo('apt update')
            manage_pkg('-yV install', 'remote', 'libssl-dev libcurl4-gnutls-dev libgit2-dev libxml2-dev libreadline-dev')
            manage_pkg('-y install', 'remote', 'cmake')
            #sudo('apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9')
            #sudo("add-apt-repository 'deb https://cloud.r-project.org/bin/linux/ubuntu bionic-cran40/'")
            #manage_pkg('update', 'remote', '')
            manage_pkg('-y install', 'remote', 'r-base r-base-dev')
            sudo('R CMD javareconf')
            sudo('cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig')
            sudo('R -e "install.packages(\'devtools\',repos=\'{}\')"'.format(r_repository))
            for i in r_libs:
                if '=' in i:
                    name = i.split('=')[0]
                    vers = '"{}"'.format(i.split('=')[1])
                else:
                    name = i
                    vers = ''
                sudo('R -e \'devtools::install_version("{}", version = {}, repos ="{}", dependencies = NA)\''.format(name, vers, r_repository))
                #sudo('R -e "install.packages(\'{}\',repos=\'{}\')"'.format(i, r_repository))
            sudo('R -e "library(\'devtools\');install.packages(repos=\'{}\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"'.format(r_repository))
            try:
                sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            except:
                sudo('R -e "options(download.file.method = "wget");library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            if os.environ['application'] == 'tensor-rstudio':
                sudo('R -e "library(\'devtools\');install_version(\'keras\', version = \'{}\', repos = \'{}\');"'.format(os.environ['notebook_keras_version'],r_repository))
            sudo('R -e "install.packages(\'RJDBC\',repos=\'{}\',dep=TRUE)"'.format(r_repository))
            sudo('touch /home/' + os_user + '/.ensure_dir/r_ensured')
        except:
            sys.exit(1)


def install_rstudio(os_user, local_spark_path, rstudio_pass, rstudio_version):
    if not exists('/home/' + os_user + '/.ensure_dir/rstudio_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'r-base')
            manage_pkg('-y install', 'remote', 'gdebi-core')
            sudo('wget https://download2.rstudio.org/server/trusty/amd64/rstudio-server-{}-amd64.deb'.format(rstudio_version))
            sudo('gdebi -n rstudio-server-{}-amd64.deb'.format(rstudio_version))
            sudo('mkdir -p /mnt/var')
            sudo('chown {0}:{0} /mnt/var'.format(os_user))
            http_proxy = run('echo $http_proxy')
            https_proxy = run('echo $https_proxy')
            sudo("sed -i '/Type=forking/a \Environment=USER=datalab-user' /etc/systemd/system/rstudio-server.service")
            sudo(
                "sed -i '/ExecStart/s|=/usr/lib/rstudio-server/bin/rserver|=/bin/bash -c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64; /usr/lib/rstudio-server/bin/rserver --auth-none 1|g' /etc/systemd/system/rstudio-server.service")
            sudo("sed -i '/ExecStart/s|$|\"|g' /etc/systemd/system/rstudio-server.service")
            sudo(
                'sed -i \'/\[Service\]/a Environment=\"HTTP_PROXY={}\"\'  /etc/systemd/system/rstudio-server.service'.format(
                    http_proxy))
            sudo(
                'sed -i \'/\[Service\]/a Environment=\"HTTPS_PROXY={}\"\'  /etc/systemd/system/rstudio-server.service'.format(
                    https_proxy))
            java_home = run("update-alternatives --query java | grep -o \'/.*/java-8.*/jre\'").splitlines()[0]
            sudo('sed -i \'/\[Service\]/ a\Environment=\"JAVA_HOME={}\"\'  /etc/systemd/system/rstudio-server.service'.format(
                java_home))
            sudo("systemctl daemon-reload")
            sudo('touch /home/{}/.Renviron'.format(os_user))
            sudo('chown {0}:{0} /home/{0}/.Renviron'.format(os_user))
            sudo('''echo 'SPARK_HOME="{0}"' >> /home/{1}/.Renviron'''.format(local_spark_path, os_user))
            sudo('''echo 'JAVA_HOME="{0}"' >> /home/{1}/.Renviron'''.format(java_home, os_user))
            sudo('touch /home/{}/.Rprofile'.format(os_user))
            sudo('chown {0}:{0} /home/{0}/.Rprofile'.format(os_user))
            sudo('''echo 'library(SparkR, lib.loc = c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib")))' >> /home/{}/.Rprofile'''.format(os_user))
            sudo('''echo 'Sys.setenv(http_proxy = \"{}\")' >> /home/{}/.Rprofile'''.format(http_proxy, os_user))
            sudo('''echo 'Sys.setenv(https_proxy = \"{}\")' >> /home/{}/.Rprofile'''.format(https_proxy, os_user))
            sudo('rstudio-server start')
            sudo('echo "{0}:{1}" | chpasswd'.format(os_user, rstudio_pass))
            #sudo("sed -i '/exit 0/d' /etc/rc.local")
            #sudo('''bash -c "echo \'sed -i 's/^#SPARK_HOME/SPARK_HOME/' /home/{}/.Renviron\' >> /etc/rc.local"'''.format(os_user))
            #sudo("bash -c 'echo exit 0 >> /etc/rc.local'")
            sudo('touch /home/{}/.ensure_dir/rstudio_ensured'.format(os_user))
        except:
            sys.exit(1)
    else:
        try:
            sudo('echo "{0}:{1}" | chpasswd'.format(os_user, rstudio_pass))
        except:
            sys.exit(1)


def ensure_matplot(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/matplot_ensured'):
        try:
            sudo("sudo sed -i~orig -e 's/# deb-src/deb-src/' /etc/apt/sources.list")
            manage_pkg('update', 'remote', '')
            manage_pkg('-y build-dep', 'remote', 'python-matplotlib')
            sudo('pip2 install matplotlib==2.0.2 --no-cache-dir')
            sudo('pip3 install matplotlib==2.0.2 --no-cache-dir')
            if os.environ['application'] in ('tensor', 'deeplearning'):
                sudo('python2.7 -m pip install -U numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy2_version']))
                sudo('python3.6 -m pip install -U numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            sudo('touch /home/' + os_user + '/.ensure_dir/matplot_ensured')
        except:
            sys.exit(1)

@backoff.on_exception(backoff.expo, SystemExit, max_tries=10)
def add_sbt_key():
    sudo(
        'curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add')

def ensure_sbt(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/sbt_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'apt-transport-https')
            sudo('echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list')

            add_sbt_key()
            manage_pkg('update', 'remote', '')
            manage_pkg('-y install', 'remote', 'sbt')
            sudo('touch /home/' + os_user + '/.ensure_dir/sbt_ensured')
        except:
            sys.exit(1)


def ensure_scala(scala_link, scala_version, os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/scala_ensured'):
        try:
            sudo('wget {}scala-{}.deb -O /tmp/scala.deb'.format(scala_link, scala_version))
            sudo('dpkg -i /tmp/scala.deb')
            sudo('touch /home/' + os_user + '/.ensure_dir/scala_ensured')
        except:
            sys.exit(1)


def ensure_jre_jdk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/jre_jdk_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'default-jre')
            manage_pkg('-y install', 'remote', 'default-jdk')
            manage_pkg('-y install', 'remote', 'openjdk-8-jdk')
            manage_pkg('-y install', 'remote', 'openjdk-8-jre')
            sudo('touch /home/' + os_user + '/.ensure_dir/jre_jdk_ensured')
        except:
            sys.exit(1)


def ensure_additional_python_libs(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/additional_python_libs_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'libjpeg8-dev zlib1g-dev')
            if os.environ['application'] in ('jupyter', 'zeppelin'):
                sudo('pip2 install NumPy=={} SciPy pandas Sympy Pillow sklearn --no-cache-dir'.format(os.environ['notebook_numpy2_version']))
                sudo('pip3 install NumPy=={} SciPy pandas Sympy Pillow sklearn --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            if os.environ['application'] in ('tensor', 'deeplearning'):
                sudo('pip2 install opencv-python==4.2.0.32 h5py --no-cache-dir')
                sudo('pip3 install opencv-python h5py --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/additional_python_libs_ensured')
        except:
            sys.exit(1)


def ensure_python3_specific_version(python3_version, os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python3_specific_version_ensured'):
        try:
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
            try:
                manage_pkg('-y install', 'remote', 'libssl1.0-dev python-virtualenv')
            except:
                sudo('pip2 install virtualenv --no-cache-dir')
                manage_pkg('-y install', 'remote', 'libssl1.0-dev')
            try:
                sudo('pip2 install tornado=={0} ipython ipykernel=={1} --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            except:
                sudo('pip2 install tornado=={0} ipython==5.0.0 ipykernel=={1} --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            sudo('pip2 install -U pip=={} --no-cache-dir'.format(os.environ['conf_pip_version']))
            sudo('pip2 install boto3 backoff --no-cache-dir')
            sudo('pip2 install fabvenv fabric-virtualenv future --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python2_libraries_ensured')
        except:
            sys.exit(1)


def ensure_python3_libraries(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python3_libraries_ensured'):
        try:
            #manage_pkg('-y install', 'remote', 'python3-setuptools')
            manage_pkg('-y install', 'remote', 'python3-pip')
            manage_pkg('-y install', 'remote', 'libkrb5-dev')
            sudo('pip3 install setuptools=={}'.format(os.environ['notebook_setuptools_version']))
            try:
                sudo('pip3 install tornado=={0} ipython==7.9.0 ipykernel=={1} sparkmagic --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            except:
                sudo('pip3 install tornado=={0} ipython==5.0.0 ipykernel=={1} sparkmagic --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            sudo('pip3 install -U pip=={} --no-cache-dir'.format(os.environ['conf_pip_version']))
            sudo('pip3 install boto3 --no-cache-dir')
            sudo('pip3 install fabvenv fabric-virtualenv future --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python3_libraries_ensured')
        except:
            sys.exit(1)


def install_tensor(os_user, cuda_version, cuda_file_name,
                   cudnn_version, cudnn_file_name, tensorflow_version,
                   templates_dir, nvidia_version):
    if not exists('/home/{}/.ensure_dir/tensor_ensured'.format(os_user)):
        try:
            # install nvidia drivers
            sudo('echo "blacklist nouveau" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('echo "options nouveau modeset=0" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('update-initramfs -u')
            with settings(warn_only=True):
                reboot(wait=180)
            manage_pkg('-y install', 'remote', 'dkms libglvnd-dev')
            kernel_version = run('uname -r | tr -d "[..0-9-]"')
            if kernel_version == 'azure':
                manage_pkg('-y install', 'remote', 'linux-modules-`uname -r`')
            else:
                # legacy support for old kernels
                sudo('if [[ $(apt-cache search linux-image-`uname -r`) ]]; then apt-get -y '
                     'install linux-image-`uname -r`; else apt-get -y install linux-modules-`uname -r`; fi;')
            sudo('wget http://us.download.nvidia.com/tesla/{0}/NVIDIA-Linux-x86_64-{0}.run -O '
                 '/home/{1}/NVIDIA-Linux-x86_64-{0}.run'.format(nvidia_version, os_user))
            sudo('/bin/bash /home/{0}/NVIDIA-Linux-x86_64-{1}.run -s --dkms'.format(os_user, nvidia_version))
            sudo('rm -f /home/{0}/NVIDIA-Linux-x86_64-{1}.run'.format(os_user, nvidia_version))
            # install cuda
            sudo('python3 -m pip install --upgrade pip=={0} wheel numpy=={1} --no-cache-dir'.format(
                os.environ['conf_pip_version'], os.environ['notebook_numpy_version']))
            sudo('wget -P /opt http://developer.download.nvidia.com/compute/cuda/{0}/Prod/local_installers/{1}'.format(
                cuda_version, cuda_file_name))
            sudo('sh /opt/{} --silent --toolkit'.format(cuda_file_name))
            sudo('mv /usr/local/cuda-{} /opt/'.format(cuda_version))
            sudo('ln -s /opt/cuda-{0} /usr/local/cuda-{0}'.format(cuda_version))
            sudo('rm -f /opt/{}'.format(cuda_file_name))
            # install cuDNN
            run('wget http://developer.download.nvidia.com/compute/redist/cudnn/v{0}/{1} -O /tmp/{1}'.format(
                cudnn_version, cudnn_file_name))
            run('tar xvzf /tmp/{} -C /tmp'.format(cudnn_file_name))
            sudo('mkdir -p /opt/cudnn/include')
            sudo('mkdir -p /opt/cudnn/lib64')
            sudo('mv /tmp/cuda/include/cudnn.h /opt/cudnn/include')
            sudo('mv /tmp/cuda/lib64/libcudnn* /opt/cudnn/lib64')
            sudo('chmod a+r /opt/cudnn/include/cudnn.h /opt/cudnn/lib64/libcudnn*')
            run(
                'echo "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64\"" >> ~/.bashrc')
            # install TensorFlow and run TensorBoard
            # sudo('python2.7 -m pip install --upgrade https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp27-none-linux_x86_64.whl --no-cache-dir'.format(tensorflow_version))
            sudo('python3 -m pip install --upgrade '
                 'https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp36-cp36m-manylinux2010_x86_64.whl'
                 ' --no-cache-dir'.format(tensorflow_version))
            sudo('mkdir /var/log/tensorboard; chown {0}:{0} -R /var/log/tensorboard'.format(os_user))
            put('{}tensorboard.service'.format(templates_dir), '/tmp/tensorboard.service')
            sudo("sed -i 's|OS_USR|{}|' /tmp/tensorboard.service".format(os_user))
            http_proxy = run('echo $http_proxy')
            https_proxy = run('echo $https_proxy')
            sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTP_PROXY={}\"\'  /tmp/tensorboard.service'.format(
                http_proxy))
            sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTPS_PROXY={}\"\'  /tmp/tensorboard.service'.format(
                https_proxy))
            sudo("chmod 644 /tmp/tensorboard.service")
            sudo('\cp /tmp/tensorboard.service /etc/systemd/system/')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable tensorboard")
            sudo("systemctl start tensorboard")
            sudo('touch /home/{}/.ensure_dir/tensor_ensured'.format(os_user))

        except:
            sys.exit(1)


def install_maven(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        manage_pkg('-y install', 'remote', 'maven')
        sudo('touch /home/' + os_user + '/.ensure_dir/maven_ensured')

def install_gcloud(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/gcloud_ensured'):
        sudo('echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list')
        sudo('curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -')
        manage_pkg('-y install', 'remote', 'google-cloud-sdk')
        sudo('touch /home/' + os_user + '/.ensure_dir/gcloud_ensured')

def install_livy_dependencies(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        manage_pkg('-y install', 'remote', 'libkrb5-dev')
        sudo('pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        sudo('pip3 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        sudo('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_maven_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        manage_pkg('-y install', 'local', 'maven')
        local('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        manage_pkg('-y install', 'local', 'libkrb5-dev')
        local('sudo pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        local('sudo pip3 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        local('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_nodejs(os_user):
    if not exists('/home/{}/.ensure_dir/nodejs_ensured'.format(os_user)):
        sudo('curl -sL https://deb.nodesource.com/setup_13.x | sudo -E bash -')
        manage_pkg('-y install', 'remote', 'nodejs')
        sudo('touch /home/{}/.ensure_dir/nodejs_ensured'.format(os_user))


def install_os_pkg(requisites):
    status = list()
    error_parser = "Could not|No matching|Error:|E:|failed|Requires:"
    new_pkgs_parser = "The following NEW packages will be installed:"
    try:
        print("Updating repositories and installing requested tools: {}".format(requisites))
        manage_pkg('update', 'remote', '')
        for os_pkg in requisites:
            name, vers = os_pkg
            if os_pkg[1] != '' and os_pkg[1] !='N/A':
                version = os_pkg[1]
                os_pkg = "{}={}".format(os_pkg[0], os_pkg[1])
            else:
                version = 'N/A'
                os_pkg = os_pkg[0]
            sudo('DEBIAN_FRONTEND=noninteractive apt-get -y install --allow-downgrades {0} 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({1})" /tmp/tee.tmp > '
                 '/tmp/os_install_{2}.log; then echo "" > /tmp/os_install_{2}.log;fi'.format(os_pkg, error_parser, name))
            err = sudo('cat /tmp/os_install_{}.log'.format(name)).replace('"', "'")
            sudo('cat /tmp/tee.tmp | if ! grep -w -E -A 30 "({1})" /tmp/tee.tmp > '
                 '/tmp/os_install_{0}.log; then echo "" > /tmp/os_install_{0}.log;fi'.format(name, new_pkgs_parser))
            dep = sudo('cat /tmp/os_install_{}.log'.format(name))
            if dep == '':
                dep = []
            else:
                dep = dep[len(new_pkgs_parser): dep.find(" upgraded, ") - 1].replace('\r', '') \
                        .replace('\n', '').replace('  ', ' ').strip().split(' ')
                for n, i in enumerate(dep):
                    if i == name:
                        dep[n] = ''
                    else:
                        sudo('apt show {0} 2>&1 | if ! grep Version: > '
                 '/tmp/os_install_{0}.log; then echo "" > /tmp/os_install_{0}.log;fi'.format(i))
                        dep[n] =sudo('cat /tmp/os_install_{}.log'.format(i)).replace('Version: ', '{} v.'.format(i))
                dep = [i for i in dep if i]
            versions = []
            sudo('apt list --installed | if ! grep {0}/ > /tmp/os_install_{0}.list; then  echo "" > /tmp/os_install_{0}.list;fi'.format(name))
            res = sudo('cat /tmp/os_install_{}.list'.format(name))
            if err:
                status_msg = 'installation_error'
                if 'E: Unable to locate package {}'.format(name) in err:
                    status_msg = 'invalid_name'
            elif res:
                ansi_escape = re.compile(r'\x1b[^m]*m')
                ver = ansi_escape.sub('', res).split("\r\n")
                version = [i for i in ver if os_pkg.split("=")[0] in i][0].split(' ')[1]
                status_msg = "installed"
            if 'E: Version' in err and 'was not found' in err:
                versions = sudo ('apt-cache policy {} | grep 500 | grep -v Packages'.format(name))\
                    .replace('\r\n', '').replace(' 500', '').replace('     ', ' ').replace('***', '').strip().split(' ')
                if versions != '':
                    status_msg = 'invalid_version'
            status.append({"group": "os_pkg", "name": name, "version": version, "status": status_msg,
                           "error_message": err, "add_pkgs": dep, "available_versions": versions})
        sudo('unattended-upgrades -v')
        sudo('export LC_ALL=C')
        return status
    except Exception as err:
        for os_pkg in requisites:
            name, vers = os_pkg
            status.append(
                {"group": "os_pkg", "name": name, "version": vers, "status": 'installation_error', "error_message": err})
        print("Failed to install OS packages: {}".format(requisites))
        return status


@backoff.on_exception(backoff.expo, SystemExit, max_tries=10)
def remove_os_pkg(pkgs):
    try:
        sudo('apt remove --purge -y {}'.format(' '.join(pkgs)))
    except:
        sys.exit(1)


def get_available_os_pkgs():
    try:
        os_pkgs = dict()
        ansi_escape = re.compile(r'\x1b[^m]*m')
        manage_pkg('update', 'remote', '')
        apt_raw = sudo("apt list")
        apt_list = ansi_escape.sub('', apt_raw).split("\r\n")
        for pkg in apt_list:
            if "/" in pkg:
                os_pkgs[pkg.split('/')[0]] = pkg.split(' ')[1]
        return os_pkgs
    except:
        sys.exit(1)


def install_caffe2(os_user, caffe2_version, cmake_version):
    if not exists('/home/{}/.ensure_dir/caffe2_ensured'.format(os_user)):
        env.shell = "/bin/bash -l -c -i"
        manage_pkg('update', 'remote', '')
        manage_pkg('-y install --no-install-recommends', 'remote', 'build-essential cmake git libgoogle-glog-dev '
                   'libprotobuf-dev protobuf-compiler python-dev python-pip')
        sudo('pip2 install numpy=={} protobuf --no-cache-dir'.format(os.environ['notebook_numpy2_version']))
        sudo('pip3 install numpy=={} protobuf --no-cache-dir'.format(os.environ['notebook_numpy_version']))
        manage_pkg('-y install --no-install-recommends', 'remote', 'libgflags-dev')
        manage_pkg('-y install --no-install-recommends', 'remote', 'libgtest-dev libiomp-dev libleveldb-dev liblmdb-dev '
                   'libopencv-dev libopenmpi-dev libsnappy-dev openmpi-bin openmpi-doc python-pydot')
        sudo('pip2 install flask graphviz hypothesis jupyter matplotlib==2.0.2 pydot python-nvd3 pyyaml requests scikit-image '
             'scipy setuptools tornado --no-cache-dir')
        sudo('pip3 install flask graphviz hypothesis jupyter matplotlib==2.0.2 pydot python-nvd3 pyyaml requests scikit-image '
             'scipy setuptools tornado --no-cache-dir')
        sudo('cp -f /opt/cudnn/include/* /opt/cuda-{}/include/'.format(os.environ['notebook_cuda_version']))
        sudo('cp -f /opt/cudnn/lib64/* /opt/cuda-{}/lib64/'.format(os.environ['notebook_cuda_version']))
        sudo('wget https://cmake.org/files/v{2}/cmake-{1}.tar.gz -O /home/{0}/cmake-{1}.tar.gz'.format(
            os_user, cmake_version, cmake_version.split('.')[0] + "." + cmake_version.split('.')[1]))
        sudo('tar -zxvf cmake-{}.tar.gz'.format(cmake_version))
        with cd('/home/{}/cmake-{}/'.format(os_user, cmake_version)):
            sudo('./bootstrap --prefix=/usr/local && make && make install')
        sudo('ln -s /usr/local/bin/cmake /bin/cmake{}'.format(cmake_version))
        sudo('git clone https://github.com/pytorch/pytorch.git')
        with cd('/home/{}/pytorch/'.format(os_user)):
            sudo('git submodule update --init')
            with settings(warn_only=True):
                sudo('git checkout {}'.format(os.environ['notebook_pytorch_branch']))
                sudo('git submodule update --init --recursive')
            sudo('python3 setup.py install')
        sudo('touch /home/' + os_user + '/.ensure_dir/caffe2_ensured')


def install_cntk(os_user, cntk2_version, cntk_version):
    if not exists('/home/{}/.ensure_dir/cntk_ensured'.format(os_user)):
        sudo('pip2 install https://cntk.ai/PythonWheel/GPU/cntk-{}-cp27-cp27mu-linux_x86_64.whl --no-cache-dir'.format(cntk2_version))
        sudo('pip3 install https://cntk.ai/PythonWheel/GPU/cntk_gpu-{}.post1-cp36-cp36m-manylinux1_x86_64.whl --no-cache-dir'.format(cntk_version))
        sudo('touch /home/{}/.ensure_dir/cntk_ensured'.format(os_user))


def install_keras(os_user, keras_version):
    if not exists('/home/{}/.ensure_dir/keras_ensured'.format(os_user)):
        sudo('pip2 install keras=={} --no-cache-dir'.format(keras_version))
        sudo('pip3 install keras=={} --no-cache-dir'.format(keras_version))
        sudo('touch /home/{}/.ensure_dir/keras_ensured'.format(os_user))


def install_theano(os_user, theano_version):
    if not exists('/home/{}/.ensure_dir/theano_ensured'.format(os_user)):
        sudo('python2.7 -m pip install Theano=={} --no-cache-dir'.format(theano_version))
        sudo('python3 -m pip install Theano=={} --no-cache-dir'.format(theano_version))
        sudo('touch /home/{}/.ensure_dir/theano_ensured'.format(os_user))


def install_mxnet(os_user, mxnet_version):
    if not exists('/home/{}/.ensure_dir/mxnet_ensured'.format(os_user)):
        sudo('pip2 install mxnet-cu101=={} opencv-python==4.2.0.32 --no-cache-dir'.format(mxnet_version))
        sudo('pip3 install mxnet-cu101=={} opencv-python --no-cache-dir'.format(mxnet_version))
        sudo('touch /home/{}/.ensure_dir/mxnet_ensured'.format(os_user))


#def install_torch(os_user):
#    if not exists('/home/{}/.ensure_dir/torch_ensured'.format(os_user)):
#        run('git clone https://github.com/nagadomi/distro.git ~/torch --recursive')
#        with cd('/home/{}/torch/'.format(os_user)):
#           run('bash install-deps;')
#           run('./install.sh -b')
#        run('source /home/{}/.bashrc'.format(os_user))
#        sudo('touch /home/{}/.ensure_dir/torch_ensured'.format(os_user))


def install_gitlab_cert(os_user, certfile):
    try:
        sudo('mv -f /home/{0}/{1} /etc/ssl/certs/{1}'.format(os_user, certfile))
    except Exception as err:
        print('Failed to install gitlab certificate. {}'.format(str(err)))
