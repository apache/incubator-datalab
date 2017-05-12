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
from dlab.common_lib import *
import os
import re


def enable_proxy(proxy_host, proxy_port):
    if not exists('/tmp/proxy_enabled'):
        try:
            proxy_string = "http://%s:%s" % (proxy_host, proxy_port)
            sudo('echo export http_proxy=' + proxy_string + ' >> /etc/profile')
            sudo('echo export https_proxy=' + proxy_string + ' >> /etc/profile')
            sudo("echo 'Acquire::http::Proxy \"" + proxy_string + "\";' >> /etc/apt/apt.conf")
            sudo('touch /tmp/proxy_enabled ')

            print "Renewing gpg key"
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
            sudo('cd /usr/local/spark/R/lib/SparkR; R -e "devtools::install(\'.\')"')
            sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            sudo('touch /home/' + os_user + '/.ensure_dir/r_local_kernel_ensured')
        except:
            sys.exit(1)


def ensure_r(os_user, r_libs):
    if not exists('/home/' + os_user + '/.ensure_dir/r_ensured'):
        try:
            sudo('apt-get install -y libcurl4-openssl-dev libssl-dev libreadline-dev')
            sudo('apt-get install -y cmake')
            if os.environ['application'] == 'deeplearning':
                try:
                    sudo("""sh -c 'echo "deb http://cran.rstudio.com/bin/linux/ubuntu trusty/" >> /etc/apt/sources.list'""")
                    sudo('gpg --keyserver keyserver.ubuntu.com --recv-key E084DAB9')
                    sudo('gpg -a --export E084DAB9 |  apt-key add -')
                    sudo('apt-get update')
                except:
                    sudo('apt-get update')
                sudo('apt-get -y --force-yes install r-base')
            else:
                sudo('apt-get install -y r-base r-base-dev')
            sudo('R CMD javareconf')
            sudo('cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig')
            for i in r_libs:
                sudo('R -e "install.packages(\'{}\',repos=\'http://cran.us.r-project.org\')"'.format(i))
            sudo('R -e "library(\'devtools\');install.packages(repos=\'http://cran.us.r-project.org\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"')
            try:
                sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            except:
                sudo('R -e "options(download.file.method = "wget");library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            sudo('R -e "install.packages(\'RJDBC\',repos=\'http://cran.us.r-project.org\',dep=TRUE)"')
            sudo('touch /home/' + os_user + '/.ensure_dir/r_ensured')
        except:
            sys.exit(1)


def install_rstudio(os_user, local_spark_path, rstudio_pass, rstudio_version):
    if not exists('/home/' + os_user + '/.ensure_dir/rstudio_ensured'):
        try:
            sudo('apt-get install -y r-base')
            sudo('apt-get install -y gdebi-core')
            sudo('wget https://download2.rstudio.org/rstudio-server-{}-amd64.deb'.format(rstudio_version))
            sudo('gdebi -n rstudio-server-{}-amd64.deb'.format(rstudio_version))
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
    if not exists('/home/' + os_user + '/.ensure_dir/matplot_ensured'):
        try:
            sudo('apt-get build-dep -y python-matplotlib')
            sudo('pip2 install matplotlib --no-cache-dir')
            sudo('pip3 install matplotlib --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/matplot_ensured')
        except:
            sys.exit(1)


def ensure_sbt(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/sbt_ensured'):
        try:
            sudo('apt-get install -y apt-transport-https')
            sudo('echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list')
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823')
            sudo('apt-get update')
            sudo('apt-get install -y sbt')
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
            sudo('apt-get install -y default-jre')
            sudo('apt-get install -y default-jdk')
            sudo('touch /home/' + os_user + '/.ensure_dir/jre_jdk_ensured')
        except:
            sys.exit(1)


def ensure_additional_python_libs(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/additional_python_libs_ensured'):
        try:
            sudo('apt-get install -y libjpeg8-dev zlib1g-dev')
            if os.environ['application'] == 'jupyter' or os.environ['application'] == 'zeppelin':
                sudo('pip2 install NumPy SciPy pandas Sympy Pillow sklearn --no-cache-dir')
                sudo('pip3 install NumPy SciPy pandas Sympy Pillow sklearn --no-cache-dir')
            if os.environ['application'] == 'tensor':
                sudo('pip2 install keras opencv-python h5py --no-cache-dir')
                sudo('python2 -m ipykernel install')
                sudo('pip3 install keras opencv-python h5py --no-cache-dir')
                sudo('python3 -m ipykernel install')
            sudo('touch /home/' + os_user + '/.ensure_dir/additional_python_libs_ensured')
        except:
            sys.exit(1)


def ensure_python3_specific_version(python3_version, os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python3_specific_version_ensured'):
        try:
            sudo('add-apt-repository -y ppa:fkrull/deadsnakes')
            sudo('apt update')
            sudo('apt install -y python' + python3_version + ' python' + python3_version +'-dev')
            sudo('touch /home/' + os_user + '/.ensure_dir/python3_specific_version_ensured')
        except:
            sys.exit(1)


def ensure_python2_libraries(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python2_libraries_ensured'):
        try:
            try:
                sudo('apt-get install -y libssl-dev python-virtualenv')
            except:
                sudo('pip2 install virtualenv')
                sudo('apt-get install -y libssl-dev')
            sudo('pip2 install ipython ipykernel --no-cache-dir')
            sudo('pip2 install -U pip --no-cache-dir')
            sudo('pip2 install boto3 --no-cache-dir')
            sudo('pip2 install fabvenv fabric-virtualenv --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python2_libraries_ensured')
        except:
            sys.exit(1)


def ensure_python3_libraries(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/python3_libraries_ensured'):
        try:
            sudo('apt-get install python3-setuptools')
            sudo('apt install -y python3-pip')
            sudo('pip3 install ipython ipykernel --no-cache-dir')
            sudo('pip3 install -U pip --no-cache-dir')
            sudo('pip3 install boto3 --no-cache-dir')
            sudo('pip3 install fabvenv fabric-virtualenv --no-cache-dir')
            sudo('touch /home/' + os_user + '/.ensure_dir/python3_libraries_ensured')
        except:
            sys.exit(1)


def install_tensor(os_user, tensorflow_version, files_dir, templates_dir):
    if not exists('/home/' + os_user + '/.ensure_dir/tensor_ensured'):
        try:
            # install nvidia drivers
            sudo('echo "blacklist nouveau" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('echo "options nouveau modeset=0" >> /etc/modprobe.d/blacklist-nouveau.conf')
            sudo('update-initramfs -u')
            sudo('shutdown -r 1')
            time.sleep(90)
            sudo('apt-get -y install linux-image-extra-`uname -r`')
            sudo('wget http://us.download.nvidia.com/XFree86/Linux-x86_64/367.57/NVIDIA-Linux-x86_64-367.57.run -O /home/' + os_user + '/NVIDIA-Linux-x86_64-367.57.run')
            sudo('/bin/bash /home/' + os_user + '/NVIDIA-Linux-x86_64-367.57.run -s --no-install-libglvnd')
            sudo('rm -f /home/' + os_user + '/NVIDIA-Linux-x86_64-367.57.run')
            # install cuda
            sudo('wget -P /opt https://developer.nvidia.com/compute/cuda/8.0/prod/local_installers/cuda_8.0.44_linux-run')
            sudo('sh /opt/cuda_8.0.44_linux-run --silent --toolkit')
            sudo('mv /usr/local/cuda-8.0 /opt/')
            sudo('ln -s /opt/cuda-8.0 /usr/local/cuda-8.0')
            sudo('rm -f /opt/cuda_8.0.44_linux-run')
            # install cuDNN
            put(files_dir + 'cudnn-8.0-linux-x64-v5.1.tgz', '/tmp/cudnn-8.0-linux-x64-v5.1.tgz')
            run('tar xvzf /tmp/cudnn-8.0-linux-x64-v5.1.tgz -C /tmp')
            sudo('mkdir -p /opt/cudnn/include')
            sudo('mkdir -p /opt/cudnn/lib64')
            sudo('mv /tmp/cuda/include/cudnn.h /opt/cudnn/include')
            sudo('mv /tmp/cuda/lib64/libcudnn* /opt/cudnn/lib64')
            sudo('chmod a+r /opt/cudnn/include/cudnn.h /opt/cudnn/lib64/libcudnn*')
            run('echo "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64\"" >> ~/.bash_profile')
            # install TensorFlow and run TensorBoard
            sudo('python2.7 -m pip install --upgrade https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-' + tensorflow_version + '-cp27-none-linux_x86_64.whl')
            sudo('python3 -m pip install --upgrade https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-' + tensorflow_version + '-cp35-cp35m-linux_x86_64.whl')
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
            sudo('python3 -m pip install Theano')
            sudo('touch /home/' + os_user + '/.ensure_dir/tensor_ensured')
        except:
            sys.exit(1)


def install_maven(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        sudo('apt-get -y install maven')
        sudo('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        sudo('apt-get -y install libkrb5-dev')
        sudo('pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        sudo('pip3 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        sudo('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_maven_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        local('sudo apt-get -y install maven')
        local('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        local('sudo apt-get -y install libkrb5-dev')
        local('sudo pip2 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        local('sudo pip3 install cloudpickle requests requests-kerberos flake8 flaky pytest')
        local('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_gitweb(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/gitweb_ensured'):
        sudo('apt-get install -y fcgiwrap gitweb libapache2-mod-perl2 libcgi-pm-perl apache2')
        sudo('sed -i -e "s/80/8085/g" /etc/apache2/ports.conf')
        sudo('sed -i -e "s/\/var\/lib\/git/\/home\/' + os_user + '/g" /etc/gitweb.conf')
        put('/root/templates/gitweb-virtualhost.conf', '/tmp/gitweb-virtualhost.conf')
        sudo('mv -f /tmp/gitweb-virtualhost.conf /etc/apache2/sites-available/000-default.conf')
        sudo('a2enmod cgi')
        sudo('service apache2 restart')
        sudo('touch /home/' + os_user + '/.ensure_dir/gitweb_ensured')


def install_os_pkg(requisites):
    try:
        print "Updating repositories and installing requested tools: " + requisites
        sudo('apt-get update')
        sudo('apt-get -y install python-pip python3-pip')
        for pkg in requisites:
            sudo('apt-get -y install ' + pkg)
        sudo('unattended-upgrades -v')
        sudo('export LC_ALL=C')
        return True
    except:
        return False


def get_available_os_pkgs():
    try:
        os_pkgs = dict()
        ansi_escape = re.compile(r'\x1b[^m]*m')
        sudo('apt-get update')
        apt_raw = sudo("apt list")
        apt_list = ansi_escape.sub('', apt_raw).split("\r\n")
        for pkg in apt_list:
            if "/" in pkg:
                os_pkgs[pkg.split('/')[0]] = pkg.split(' ')[1]
        return os_pkgs
    except:
        sys.exit(1)