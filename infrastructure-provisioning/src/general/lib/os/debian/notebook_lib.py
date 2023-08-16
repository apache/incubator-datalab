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

import backoff
import os
import re
import sys
import subprocess
import time
from datalab.common_lib import *
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *
from patchwork.files import exists
from patchwork import files


def enable_proxy(proxy_host, proxy_port):
    try:
        proxy_string = "http://%s:%s" % (proxy_host, proxy_port)
        proxy_https_string = "http://%s:%s" % (proxy_host, proxy_port)
        datalab.fab.conn.sudo('sed -i "/^export http_proxy/d" /etc/profile')
        datalab.fab.conn.sudo('sed -i "/^export https_proxy/d" /etc/profile')
        datalab.fab.conn.sudo('''bash -c 'echo export http_proxy={} >> /etc/profile' '''.format(proxy_string))
        datalab.fab.conn.sudo('''bash -c 'echo export https_proxy={} >> /etc/profile' '''.format(proxy_string))
        if exists(datalab.fab.conn, '/etc/apt/apt.conf'):
            datalab.fab.conn.sudo("sed -i '/^Acquire::http::Proxy/d' /etc/apt/apt.conf")
        datalab.fab.conn.sudo('''bash -c "echo 'Acquire::http::Proxy \\"{}\\";' >> /etc/apt/apt.conf" '''.format(proxy_string))
        datalab.fab.conn.sudo('''bash -c "echo 'Acquire::http::Proxy \\"{}\\";' >> /etc/apt/apt.conf" '''.format(proxy_https_string))
    except:
        sys.exit(1)


def ensure_r_local_kernel(spark_version, os_user, templates_dir, kernels_dir):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/r_local_kernel_ensured'):
        try:
            datalab.fab.conn.sudo('R -e "IRkernel::installspec(prefix = \'/home/{}/.local/\')"'.format(os_user))
            r_version = datalab.fab.conn.sudo("R --version | awk '/version / {print $3}'").stdout.replace('\n','')
            datalab.fab.conn.put(templates_dir + 'r_template.json', '/tmp/r_template.json')
            datalab.fab.conn.sudo('sed -i "s|R_VER|' + r_version + '|g" /tmp/r_template.json')
            datalab.fab.conn.sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/r_template.json')
            datalab.fab.conn.sudo('\cp -f /tmp/r_template.json {}/ir/kernel.json'.format(kernels_dir))
            datalab.fab.conn.sudo('ln -s /opt/spark/ /usr/local/spark')
            try:
                datalab.fab.conn.sudo('R -e "devtools::install_version(\'roxygen2\', version = \'{}\', repos = \'https://cloud.r-project.org\')"'.format(os.environ['notebook_roxygen2_version']))
                datalab.fab.conn.sudo(''' bash -c 'cd /usr/local/spark/R/lib/SparkR; R -e "devtools::check()"' ''')
            except:
                pass
            datalab.fab.conn.sudo(''' bash -c 'cd /usr/local/spark/R/lib/SparkR; R -e "devtools::install()"' ''')
            datalab.fab.conn.sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/r_local_kernel_ensured')
        except:
            sys.exit(1)

@backoff.on_exception(backoff.expo, SystemExit, max_tries=20)
def add_marruter_key():
    try:
        datalab.fab.conn.sudo('add-apt-repository -y ppa:marutter/rrutter')
    except:
        sys.exit(1)

def ensure_r(os_user, r_libs):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/r_ensured'):
        try:
            r_repository = 'https://cloud.r-project.org'
            #add_marruter_key()
            datalab.fab.conn.sudo('apt update')
            manage_pkg('-yV install', 'remote', 'libfreetype6-dev libpng-dev libtiff5-dev libjpeg-dev '
                                                'libfontconfig1-dev libharfbuzz-dev libfribidi-dev libssl-dev '
                                                'libcurl4-gnutls-dev libgit2-dev libxml2-dev libreadline-dev')
            manage_pkg('-y install', 'remote', 'cmake')
            datalab.fab.conn.sudo('''bash -c -l 'apt-key adv --keyserver-options http-proxy="$http_proxy" --keyserver hkp://keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9' ''')
            datalab.fab.conn.sudo("add-apt-repository 'deb https://cloud.r-project.org/bin/linux/ubuntu focal-cran40/'")
            manage_pkg('update', 'remote', '')
            manage_pkg('-y install', 'remote', 'r-base r-base-dev')
            datalab.fab.conn.sudo('R CMD javareconf')
            datalab.fab.conn.sudo('''bash -c 'cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig' ''')
            datalab.fab.conn.sudo('R -e "install.packages(\'devtools\',repos=\'{}\')"'.format(r_repository))
            for i in r_libs:
                if '=' in i:
                    name = i.split('=')[0]
                    vers = '"{}"'.format(i.split('=')[1])
                else:
                    name = i
                    vers = ''
                datalab.fab.conn.sudo('R -e \'devtools::install_version("{}", version = {}, repos ="{}", dependencies = NA)\''.format(name, vers, r_repository))
                #sudo('R -e "install.packages(\'{}\',repos=\'{}\')"'.format(i, r_repository))
            datalab.fab.conn.sudo('R -e "library(\'devtools\');install.packages(repos=\'{}\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"'.format(r_repository))
            try:
                datalab.fab.conn.sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            except:
                datalab.fab.conn.sudo('R -e "options(download.file.method = "wget");library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            if os.environ['application'] == 'tensor-rstudio':
                datalab.fab.conn.sudo('R -e "library(\'devtools\');install_version(\'keras\', version = \'{}\', repos = \'{}\');"'.format(os.environ['notebook_keras_version'],r_repository))
            datalab.fab.conn.sudo('R -e "install.packages(\'RJDBC\',repos=\'{}\',dep=TRUE)"'.format(r_repository))
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/r_ensured')
        except:
            sys.exit(1)


def install_rstudio(os_user, local_spark_path, rstudio_pass, rstudio_version, python_venv_version=''):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/rstudio_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'r-base')
            manage_pkg('-y install', 'remote', 'gdebi-core')
            datalab.fab.conn.sudo('wget https://download2.rstudio.org/server/bionic/amd64/rstudio-server-{}-amd64.deb'.format(rstudio_version))
            datalab.fab.conn.sudo('gdebi -n rstudio-server-{}-amd64.deb'.format(rstudio_version))
            datalab.fab.conn.sudo('mkdir -p /mnt/var')
            datalab.fab.conn.sudo('chown {0}:{0} /mnt/var'.format(os_user))
            http_proxy = datalab.fab.conn.run('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n','')
            https_proxy = datalab.fab.conn.run('''bash -l -c 'echo $https_proxy' ''').stdout.replace('\n','')
            datalab.fab.conn.sudo("sed -i '/Type=forking/a \Environment=USER=datalab-user' /lib/systemd/system/rstudio-server.service")
            datalab.fab.conn.sudo("sed -i '/ExecStart/s|=/usr/lib/rstudio-server/bin/rserver|=/bin/bash -c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64; /usr/lib/rstudio-server/bin/rserver --auth-none 1|g' /lib/systemd/system/rstudio-server.service")
            datalab.fab.conn.sudo("sed -i '/ExecStart/s|$|\"|g' /lib/systemd/system/rstudio-server.service")
            datalab.fab.conn.sudo(
                'sed -i \'/\[Service\]/a Environment=\"HTTP_PROXY={}\"\'  /lib/systemd/system/rstudio-server.service'.format(
                    http_proxy))
            datalab.fab.conn.sudo(
                'sed -i \'/\[Service\]/a Environment=\"HTTPS_PROXY={}\"\'  /lib/systemd/system/rstudio-server.service'.format(
                    https_proxy))
            java_home = datalab.fab.conn.run("update-alternatives --query java | grep -o \'/.*/java-8.*/jre\'").stdout.splitlines()[0].replace('\n','')
            datalab.fab.conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"JAVA_HOME={}\"\'  /lib/systemd/system/rstudio-server.service'.format(
                java_home))
            datalab.fab.conn.sudo("systemctl daemon-reload")
            datalab.fab.conn.sudo('touch /home/{}/.Renviron'.format(os_user))
            datalab.fab.conn.sudo('chown {0}:{0} /home/{0}/.Renviron'.format(os_user))
            datalab.fab.conn.sudo('''echo 'SPARK_HOME="{0}"' >> /home/{1}/.Renviron'''.format(local_spark_path, os_user))
            datalab.fab.conn.sudo('''echo 'JAVA_HOME="{0}"' >> /home/{1}/.Renviron'''.format(java_home, os_user))
            datalab.fab.conn.sudo(
                '''echo 'RETICULATE_PYTHON="/opt/python/python{0}/bin/python{1}"' >> /home/{2}/.Renviron'''.format(
                    python_venv_version, python_venv_version[:3], os_user))
            datalab.fab.conn.sudo(
                '''echo 'LD_LIBRARY_PATH="/opt/python/python{0}/lib/"' >> /home/{1}/.Renviron'''.format(python_venv_version, os_user))
            datalab.fab.conn.sudo('touch /home/{}/.Rprofile'.format(os_user))
            datalab.fab.conn.sudo('chown {0}:{0} /home/{0}/.Rprofile'.format(os_user))
            datalab.fab.conn.sudo('''echo 'library(SparkR, lib.loc = c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib")))' >> /home/{}/.Rprofile'''.format(os_user))
            datalab.fab.conn.sudo('''echo 'Sys.setenv(http_proxy = \"{}\")' >> /home/{}/.Rprofile'''.format(http_proxy, os_user))
            datalab.fab.conn.sudo('''echo 'Sys.setenv(https_proxy = \"{}\")' >> /home/{}/.Rprofile'''.format(https_proxy, os_user))
            datalab.fab.conn.sudo('rstudio-server start')
            datalab.fab.conn.sudo('''bash -c 'echo "{0}:{1}" | chpasswd' '''.format(os_user, rstudio_pass))
            #sudo("sed -i '/exit 0/d' /etc/rc.local")
            #sudo('''bash -c "echo \'sed -i 's/^#SPARK_HOME/SPARK_HOME/' /home/{}/.Renviron\' >> /etc/rc.local"'''.format(os_user))
            #sudo("bash -c 'echo exit 0 >> /etc/rc.local'")
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/rstudio_ensured'.format(os_user))
        except:
            sys.exit(1)
    else:
        try:
            datalab.fab.conn.sudo('''bash -c 'echo "{0}:{1}" | chpasswd' '''.format(os_user, rstudio_pass))
        except:
            sys.exit(1)


def ensure_matplot(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/matplot_ensured'):
        try:
            datalab.fab.conn.sudo("sudo sed -i~orig -e 's/# deb-src/deb-src/' /etc/apt/sources.list")
            manage_pkg('update', 'remote', '')
            manage_pkg('-y build-dep', 'remote', 'python3-matplotlib')
            datalab.fab.conn.sudo('pip3 install matplotlib=={} --no-cache-dir'.format(os.environ['notebook_matplotlib_version']))
            if os.environ['application'] == 'tensor':
                datalab.fab.conn.sudo(
                    'python3.8 -m pip install -U numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            if os.environ['application'] == 'deeplearning':
                datalab.fab.conn.sudo(
                    'pip3 install -U numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/matplot_ensured')
        except:
            sys.exit(1)

@backoff.on_exception(backoff.expo, SystemExit, max_tries=10)
def add_sbt_key():
    datalab.fab.conn.sudo(
        'curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo -H gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import')

def ensure_sbt(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/sbt_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'apt-transport-https')
            datalab.fab.conn.sudo('echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list')
            datalab.fab.conn.sudo(
                'echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list')
            add_sbt_key()
            datalab.fab.conn.sudo(
                'sudo chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg')

            manage_pkg('update', 'remote', '')
            manage_pkg('-y install', 'remote', 'sbt')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/sbt_ensured')
        except:
            sys.exit(1)


def ensure_scala(scala_link, scala_version, os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/scala_ensured'):
        try:
            datalab.fab.conn.sudo('hostname; pwd')
            datalab.fab.conn.sudo('wget {}scala-{}.deb --tries=3 -O /tmp/scala.deb'.format(scala_link, scala_version))
            datalab.fab.conn.sudo('dpkg -i /tmp/scala.deb')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/scala_ensured')
        except:
            sys.exit(1)


def ensure_jre_jdk(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/jre_jdk_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'default-jre')
            manage_pkg('-y install', 'remote', 'default-jdk')
            if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['conf_cloud_provider'] == 'gcp' and os.environ['application'] == 'deeplearning':
                datalab.fab.conn.sudo(
                    'wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | sudo apt-key add -')
                datalab.fab.conn.sudo('add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/')
                datalab.fab.conn.sudo('apt-get update')
                datalab.fab.conn.sudo('apt-get install adoptopenjdk-8-hotspot -y')
            else:
                manage_pkg('-y install', 'remote', 'openjdk-8-jdk')
                manage_pkg('-y install', 'remote', 'openjdk-8-jre')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/jre_jdk_ensured')
        except:
            sys.exit(1)



def ensure_additional_python_libs(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/additional_python_libs_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'libjpeg8-dev zlib1g-dev')
            if os.environ['application'] in ('jupyter', 'zeppelin'):
                datalab.fab.conn.sudo('pip3 install NumPy=={} SciPy pandas Sympy Pillow sklearn --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            if os.environ['application'] in ('tensor', 'deeplearning'):
                datalab.fab.conn.sudo('pip3 install opencv-python h5py --no-cache-dir')
                #datalab.fab.conn.sudo('pip3 install python3-opencv scikit-learn --no-cache-dir')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/additional_python_libs_ensured')
        except:
            sys.exit(1)


def ensure_python3_specific_version(python3_version, os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/python3_specific_version_ensured'):
        try:
            if len(python3_version) < 4:
                python3_version = python3_version + ".0"
            datalab.fab.conn.sudo('wget https://www.python.org/ftp/python/{0}/Python-{0}.tgz'.format(python3_version))
            datalab.fab.conn.sudo(''' bash -c 'tar xzf Python-{0}.tgz; cd Python-{0}; ./configure --prefix=/usr/local; make altinstall' '''.format(python3_version))
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/python3_specific_version_ensured')
        except:
            sys.exit(1)

def ensure_python3_libraries(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/python3_libraries_ensured'):
        try:
            #manage_pkg('-y install', 'remote', 'python3-setuptools')
            manage_pkg('-y install', 'remote', 'python3-pip')
            manage_pkg('-y install', 'remote', 'libkrb5-dev')
            manage_pkg('-y install', 'remote', 'libbz2-dev libsqlite3-dev tk-dev libncursesw5-dev libreadline-dev '
                                               'liblzma-dev uuid-dev lzma-dev libgdbm-dev')  #necessary for python build
            datalab.fab.conn.sudo('pip3 install -U keyrings.alt backoff')
            if os.environ['conf_cloud_provider'] == 'aws' and os.environ['conf_deeplearning_cloud_ami'] == 'true': 
                datalab.fab.conn.sudo('pip3 install --upgrade --user pyqt5==5.12')
                datalab.fab.conn.sudo('pip3 install --upgrade --user pyqtwebengine==5.12')
                datalab.fab.conn.sudo('pip3 install setuptools')
            else:
                datalab.fab.conn.sudo('pip3 install setuptools=={}'.format(os.environ['notebook_setuptools_version']))
            try:
                datalab.fab.conn.sudo('pip3 install nbformat==5.1.3 nbconvert==5.6.1 tornado=={0} ipython==7.21.0 ipykernel=={1} sparkmagic --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            except:
                datalab.fab.conn.sudo('pip3 install nbformat==5.1.3 nbconvert==5.6.1 tornado=={0} ipython==7.9.0 ipykernel=={1} sparkmagic --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            datalab.fab.conn.sudo('pip3 install -U pip=={} --no-cache-dir'.format(os.environ['conf_pip_version']))
            datalab.fab.conn.sudo('pip3 install boto3 --no-cache-dir')
            datalab.fab.conn.sudo('pip3 install fabvenv fabric-virtualenv future patchwork --no-cache-dir')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/python3_libraries_ensured')
        except:
            sys.exit(1)

def install_nvidia_drivers(os_user):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/nvidia_ensured'.format(os_user)):
        try:
            if os.environ['conf_cloud_provider'] == 'aws':
                cuda_version = '11.3.0'
                cuda_file_name = "cuda-repo-ubuntu2004-11-3-local_11.3.0-465.19.01-1_amd64.deb"
                cuda_key = '/var/cuda-repo-ubuntu2004-11-3-local/7fa2af80.pub'
            else:
                cuda_version = '11.4.0'
                cuda_file_name = 'cuda-repo-ubuntu2004-11-4-local_11.4.0-470.42.01-1_amd64.deb'
                cuda_key = '/var/cuda-repo-ubuntu2004-11-4-local/7fa2af80.pub'
            # install nvidia drivers
            datalab.fab.conn.sudo(
                'wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2004/x86_64/cuda-ubuntu2004.pin')
            datalab.fab.conn.sudo('mv cuda-ubuntu2004.pin /etc/apt/preferences.d/cuda-repository-pin-600')
            datalab.fab.conn.sudo(
                'wget https://developer.download.nvidia.com/compute/cuda/{}/local_installers/{}'.format(cuda_version, cuda_file_name))
            datalab.fab.conn.sudo('dpkg -i {}'.format(cuda_file_name))
            datalab.fab.conn.sudo('apt-key add {}'.format(cuda_key))
            manage_pkg('update', 'remote', '')
            manage_pkg('-y install', 'remote', 'cuda')
            #clean space on disk
            manage_pkg('clean', 'remote', 'all')
            datalab.fab.conn.sudo('rm {}'.format(cuda_file_name))
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/nvidia_ensured'.format(os_user))
        except Exception as err:
            print('Failed to install_nvidia_drivers: ', str(err))
            sys.exit(1)

def install_tensor(os_user, cuda_version, cuda_file_name,
                   cudnn_version, cudnn_file_name, tensorflow_version,
                   templates_dir, nvidia_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/tensor_ensured'.format(os_user)):
        try:
            # install nvidia drivers
            #datalab.fab.conn.sudo('''bash -c 'echo "blacklist nouveau" >> /etc/modprobe.d/blacklist-nouveau.conf' ''')
            #datalab.fab.conn.sudo('''bash -c 'echo "options nouveau modeset=0" >> /etc/modprobe.d/blacklist-nouveau.conf' ''')
            #datalab.fab.conn.sudo('update-initramfs -u')
            #datalab.fab.conn.sudo('reboot', warn=True)
            #time.sleep(60)
            ##manage_pkg('-y install', 'remote', 'dkms libglvnd-dev')
            #kernel_version = datalab.fab.conn.run('uname -r | tr -d "[..0-9-]"').stdout.replace('\n','')
            #if kernel_version == 'azure':
            #    manage_pkg('-y install', 'remote', 'linux-modules-`uname -r`')
            #else:
                # legacy support for old kernels
            #    datalab.fab.conn.sudo(''' bash -c 'if [[ $(apt-cache search linux-image-`uname -r`) ]]; then apt-get -y '''
            #    '''install linux-image-`uname -r`; else apt-get -y install linux-modules-`uname -r`; fi;' ''')
            #datalab.fab.conn.sudo('wget https://us.download.nvidia.com/tesla/{0}/NVIDIA-Linux-x86_64-{0}.run -O '
            #     '/home/{1}/NVIDIA-Linux-x86_64-{0}.run'.format(nvidia_version, os_user))
            #datalab.fab.conn.sudo('/bin/bash /home/{0}/NVIDIA-Linux-x86_64-{1}.run -s --dkms'.format(os_user, nvidia_version))
            #datalab.fab.conn.sudo('rm -f /home/{0}/NVIDIA-Linux-x86_64-{1}.run'.format(os_user, nvidia_version))
            # install cuda
            #datalab.fab.conn.sudo('python3 -m pip install --upgrade pip=={0} wheel numpy=={1} --no-cache-dir'.format(
            #    os.environ['conf_pip_version'], os.environ['notebook_numpy_version']))
            #datalab.fab.conn.sudo('wget -P /opt https://developer.download.nvidia.com/compute/cuda/{0}/Prod/local_installers/{1}'.format(
            #    cuda_version, cuda_file_name))
            #datalab.fab.conn.sudo('apt -y install gcc-8 g++-8')
            ##datalab.fab.conn.sudo('update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-8 8')
            #datalab.fab.conn.sudo('update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-8 8')
            #datalab.fab.conn.sudo('sh /opt/{} --silent --toolkit'.format(cuda_file_name))
            #datalab.fab.conn.sudo('update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-9 9')
            #datalab.fab.conn.sudo('update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-9 9')
            #datalab.fab.conn.sudo('mv /usr/local/cuda-{} /opt/'.format(cuda_version))
            #datalab.fab.conn.sudo('ln -s /opt/cuda-{0} /usr/local/cuda-{0}'.format(cuda_version))
            #datalab.fab.conn.sudo('rm -f /opt/{}'.format(cuda_file_name))
            # install cuDNN
            #datalab.fab.conn.sudo('nvidia-smi')
            #datalab.fab.conn.sudo('nvcc --version')
            datalab.fab.conn.run('wget https://developer.download.nvidia.com/compute/redist/cudnn/v{0}/{1} -O /tmp/{1}'.format(
                cudnn_version, cudnn_file_name))
            datalab.fab.conn.run('tar xvzf /tmp/{} -C /tmp'.format(cudnn_file_name))
            datalab.fab.conn.sudo('mkdir -p /opt/cudnn/include')
            datalab.fab.conn.sudo('mkdir -p /opt/cudnn/lib64')
            datalab.fab.conn.sudo('mv /tmp/cuda/include/cudnn.h /opt/cudnn/include')
            datalab.fab.conn.sudo('mv /tmp/cuda/lib64/libcudnn* /opt/cudnn/lib64')
            datalab.fab.conn.sudo('chmod a+r /opt/cudnn/include/cudnn.h /opt/cudnn/lib64/libcudnn*')
            datalab.fab.conn.run('''bash -l -c 'echo "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64\"" >> ~/.bashrc' ''')
            # install TensorFlow and run TensorBoard
            # datalab.fab.conn.sudo('python2.7 -m pip install --upgrade https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp27-none-linux_x86_64.whl --no-cache-dir'.format(tensorflow_version))
            datalab.fab.install_venv_pip_pkg('tensorflow-gpu',tensorflow_version)
            datalab.fab.conn.sudo('mkdir /var/log/tensorboard')
            datalab.fab.conn.sudo('chown {0}:{0} -R /var/log/tensorboard'.format(os_user))
            datalab.fab.conn.put('{}tensorboard.service'.format(templates_dir), '/tmp/tensorboard.service')
            datalab.fab.conn.sudo("sed -i 's|OS_USR|{}|' /tmp/tensorboard.service".format(os_user))
            venv_activation = 'source /opt/python/python{0}/bin/activate'.format(os.environ['notebook_python_venv_version'])
            datalab.fab.conn.sudo("sed -i 's|VENV_ACTIVATION|{}|' /tmp/tensorboard.service".format(venv_activation))
            http_proxy = datalab.fab.conn.run('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n','')
            https_proxy = datalab.fab.conn.run('''bash -l -c 'echo $https_proxy' ''').stdout.replace('\n','')
            datalab.fab.conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTP_PROXY={}\"\'  /tmp/tensorboard.service'.format(
                http_proxy))
            datalab.fab.conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"HTTPS_PROXY={}\"\'  /tmp/tensorboard.service'.format(
                https_proxy))
            datalab.fab.conn.sudo("chmod 644 /tmp/tensorboard.service")
            datalab.fab.conn.sudo('\cp /tmp/tensorboard.service /etc/systemd/system/')
            datalab.fab.conn.sudo("systemctl daemon-reload")
            datalab.fab.conn.sudo("systemctl enable tensorboard")
            datalab.fab.conn.sudo("systemctl start tensorboard")
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/tensor_ensured'.format(os_user))

        except Exception as err:
            print('Failed to install_tensor: ', str(err))
            sys.exit(1)


def ensure_venv_libs(os_user, libs):
    if not exists(datalab.fab.conn, '/home/' + os_user + '/.ensure_dir/venv_libs_ensured'):
        datalab.fab.install_venv_pip_pkg(libs)
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/venv_libs_ensured')


def ensure_pytorch(os_user, gpu=True):
    if not exists(datalab.fab.conn, '/home/' + os_user + '/.ensure_dir/pytorch_ensured'):
        if gpu:
            datalab.fab.install_venv_pip_pkg('torch==1.10.2+cu113 torchvision==0.11.3+cu113 '
                                                  'torchaudio==0.10.2+cu113 -f '
                                                  'https://download.pytorch.org/whl/cu113/torch_stable.html')
        else:
            datalab.fab.install_venv_pip_pkg('torch==1.10.2+cpu torchvision==0.11.3+cpu torchaudio==0.10.2+cpu -f '
                                                  'https://download.pytorch.org/whl/cpu/torch_stable.html')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/pytorch_ensured')


def install_maven(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/maven_ensured'):
        manage_pkg('-y install', 'remote', 'maven')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/maven_ensured')

def install_gcloud(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/gcloud_ensured'):
        datalab.fab.conn.sudo('echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list')
        datalab.fab.conn.sudo('curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -')
        manage_pkg('-y install', 'remote', 'google-cloud-sdk')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/gcloud_ensured')

def install_livy_dependencies(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        manage_pkg('-y install', 'remote', 'libkrb5-dev')
        datalab.fab.conn.sudo('pip3 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_maven_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        manage_pkg('-y install', 'local', 'maven')
        datalab.fab.conn.local('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        manage_pkg('-y install', 'local', 'libkrb5-dev')
        datalab.fab.conn.local('sudo pip3 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        datalab.fab.conn.local('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_nodejs(os_user):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/nodejs_ensured'.format(os_user)):
        if os.environ['conf_cloud_provider'] == 'gcp' and os.environ['application'] == 'deeplearning':
            datalab.fab.conn.sudo('add-apt-repository --remove ppa:deadsnakes/ppa -y')
        datalab.fab.conn.sudo('curl -sL https://deb.nodesource.com/setup_15.x | sudo -E bash -')
        manage_pkg('-y install', 'remote', 'nodejs')
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/nodejs_ensured'.format(os_user))

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
            datalab.fab.conn.sudo('DEBIAN_FRONTEND=noninteractive apt-get -y install --allow-downgrades {0} 2>&1 | '
                                  'tee /tmp/os_install_{2}.tmp; if ! grep -w -E "({1})" /tmp/os_install_{2}.tmp > '
                                  '/tmp/os_install_{2}.log; then echo "no_error" > /tmp/os_install_{2}.log;fi'
                                  .format(os_pkg, error_parser, name))
            err = datalab.fab.conn.sudo('cat /tmp/os_install_{}.log'.format(name)).stdout.replace('"', "'")
            datalab.fab.conn.sudo('cat /tmp/os_install_{0}.tmp | if ! grep -w -E -A 30 "({1})" /tmp/os_install_{0}.tmp > '
                                  '/tmp/os_install_{0}.log; then echo "no_new_pkgs" > /tmp/os_install_{0}.log;fi'
                                  .format(name, new_pkgs_parser))
            dep = datalab.fab.conn.sudo('cat /tmp/os_install_{}.log'.format(name)).stdout
            if 'no_new_pkgs' in dep:
                dep = []
            else:
                dep = dep[len(new_pkgs_parser): dep.find(" upgraded, ") - 1].replace('\r', '') \
                        .replace('\n', '').replace('  ', ' ').strip().split(' ')
                for n, i in enumerate(dep):
                    if i == name:
                        dep[n] = ''
                    else:
                        datalab.fab.conn.sudo('apt show {0} 2>&1 | if ! grep Version: > '
                 '/tmp/os_install_{0}.log; then echo "no_version" > /tmp/os_install_{0}.log;fi'.format(i))
                        dep[n] = datalab.fab.conn.sudo('cat /tmp/os_install_{}.log'.format(i)).stdout.replace('Version: ', '{} v.'.format(i)).replace('\n', '')
                dep = [i for i in dep if i]
            versions = []
            datalab.fab.conn.sudo('apt list --installed | if ! grep {0}/ > /tmp/os_install_{0}.list; then  echo "not_installed" > /tmp/os_install_{0}.list;fi'.format(name))
            res = datalab.fab.conn.sudo('cat /tmp/os_install_{}.list'.format(name)).stdout.replace('\n', '')
            if "no_error" not in err:
                status_msg = 'installation_error'
                if 'E: Unable to locate package {}'.format(name) in err:
                    status_msg = 'invalid_name'
            elif "not_installed" not in res:
                ansi_escape = re.compile(r'\x1b[^m]*m')
                ver = ansi_escape.sub('', res).split("\r\n")
                version = [i for i in ver if os_pkg.split("=")[0] in i][0].split(' ')[1]
                status_msg = "installed"
            if 'E: Version' in err and 'was not found' in err:
                versions = datalab.fab.conn.sudo('apt-cache policy {} | grep 500 | grep -v Packages'.format(name)).stdout\
                    .replace('\r\n', '').replace(' 500', '').replace('     ', ' ').replace('***', '').strip().split(' ')
                if versions != '':
                    status_msg = 'invalid_version'
            status.append({"group": "os_pkg", "name": name, "version": version, "status": status_msg,
                           "error_message": err, "add_pkgs": dep, "available_versions": versions})
        datalab.fab.conn.sudo('unattended-upgrades -v')
        #datalab.fab.conn.sudo('export LC_ALL=C')
        datalab.fab.conn.sudo('rm /tmp/*{}*'.format(name))
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
        datalab.fab.conn.sudo('apt remove --purge -y {}'.format(' '.join(pkgs)))
    except:
        sys.exit(1)


def get_available_os_pkgs():
    try:
        os_pkgs = dict()
        ansi_escape = re.compile(r'\x1b[^m]*m')
        manage_pkg('update', 'remote', '')
        apt_raw = datalab.fab.conn.sudo("apt list").stdout
        apt_list = ansi_escape.sub('', apt_raw).split("\n")
        for pkg in apt_list:
            if "/" in pkg:
                os_pkgs[pkg.split('/')[0]] = pkg.split(' ')[1]
        return os_pkgs
    except:
        sys.exit(1)


def install_caffe2(os_user, caffe2_version, cmake_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/caffe2_ensured'.format(os_user)):
        manage_pkg('update', 'remote', '')
        manage_pkg('-y install --no-install-recommends', 'remote', 'build-essential cmake git libgoogle-glog-dev '
                    'libprotobuf-dev protobuf-compiler python3-dev python3-pip')
        datalab.fab.conn.sudo('pip3 install numpy=={} protobuf --no-cache-dir'.format(os.environ['notebook_numpy_version']))
        manage_pkg('-y install --no-install-recommends', 'remote', 'libgflags-dev')
        manage_pkg('-y install --no-install-recommends', 'remote',
                   'libgtest-dev libiomp-dev libleveldb-dev liblmdb-dev '
                   'libopencv-dev libopenmpi-dev libsnappy-dev openmpi-bin openmpi-doc python-pydot')
        datalab.fab.conn.sudo(
            'pip3 install flask graphviz hypothesis jupyter matplotlib=={} pydot python-nvd3 pyyaml requests scikit-image '
            'scipy tornado --no-cache-dir'.format(os.environ['notebook_matplotlib_version']))
        if os.environ['application'] == 'deeplearning':
            datalab.fab.conn.sudo('apt install -y cmake')
            datalab.fab.conn.sudo('pip3 install torch==1.5.1+cu101 torchvision==0.6.1+cu101 -f https://download.pytorch.org/whl/torch_stable.html')
        else:
            # datalab.fab.conn.sudo('mkdir /opt/cuda-{}'.format(os.environ['notebook_cuda_version']))
            # datalab.fab.conn.sudo('mkdir /opt/cuda-{}/include/'.format(os.environ['notebook_cuda_version']))
            # datalab.fab.conn.sudo('mkdir /opt/cuda-{}/lib64/'.format(os.environ['notebook_cuda_version']))
            datalab.fab.conn.sudo('cp -f /opt/cudnn/include/* /opt/cuda-{}/include/'.format(os.environ['notebook_cuda_version']))
            datalab.fab.conn.sudo('cp -f /opt/cudnn/lib64/* /opt/cuda-{}/lib64/'.format(os.environ['notebook_cuda_version']))
            datalab.fab.conn.sudo('wget https://cmake.org/files/v{2}/cmake-{1}.tar.gz -O /home/{0}/cmake-{1}.tar.gz'.format(
            os_user, cmake_version, cmake_version.split('.')[0] + "." + cmake_version.split('.')[1]))
            datalab.fab.conn.sudo('tar -zxvf /home/datalab-user/cmake-{}.tar.gz'.format(cmake_version))
            datalab.fab.conn.sudo('''bash -c 'cd /home/{}/cmake-{}/ && ./bootstrap --prefix=/usr/local && make && make install' '''.format(os_user, cmake_version))
            datalab.fab.conn.sudo('ln -s /usr/local/bin/cmake /bin/cmake{}'.format(cmake_version))
            datalab.fab.conn.sudo('git clone https://github.com/pytorch/pytorch.git')
            datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && git submodule update --init' '''.format(os_user))
            datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && git checkout {}' '''.format(os_user, os.environ['notebook_pytorch_branch']), warn=True)
            datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && git submodule update --init --recursive' '''.format(os_user), warn=True)
            datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && python3 setup.py install' '''.format(os_user))
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/caffe2_ensured')


def install_cntk(os_user, cntk_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/cntk_ensured'.format(os_user)):
        datalab.fab.conn.sudo('pip3 install cntk-gpu=={} --no-cache-dir'.format(cntk_version))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/cntk_ensured'.format(os_user))


def install_keras(os_user, keras_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/keras_ensured'.format(os_user)):
        datalab.fab.install_venv_pip_pkg('keras',keras_version)
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/keras_ensured'.format(os_user))


def install_theano(os_user, theano_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/theano_ensured'.format(os_user)):
        datalab.fab.install_venv_pip_pkg('Theano',theano_version)
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/theano_ensured'.format(os_user))


def install_mxnet(os_user, mxnet_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/mxnet_ensured'.format(os_user)):
        datalab.fab.conn.sudo('pip3 install mxnet-cu101=={} opencv-python --no-cache-dir'.format(mxnet_version))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/mxnet_ensured'.format(os_user))


#def install_torch(os_user):
#    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/torch_ensured'.format(os_user)):
#        run('git clone https://github.com/nagadomi/distro.git ~/torch --recursive')
#        with cd('/home/{}/torch/'.format(os_user)):
#           run('bash install-deps;')
#           run('./install.sh -b')
#        run('source /home/{}/.bashrc'.format(os_user))
#        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/torch_ensured'.format(os_user))


def install_gitlab_cert(os_user, certfile):
    try:
        datalab.fab.conn.sudo('mv -f /home/{0}/{1} /etc/ssl/certs/{1}'.format(os_user, certfile))
    except Exception as err:
        print('Failed to install gitlab certificate. {}'.format(str(err)))
