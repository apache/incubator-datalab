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

import json
import os
import sys
import time
from datalab.common_lib import manage_pkg
from datalab.fab import *
from datalab.notebook_lib import *
from fabric import *
from patchwork.files import exists
from patchwork import files


def enable_proxy(proxy_host, proxy_port):
    try:
        proxy_string = "http://%s:%s" % (proxy_host, proxy_port)
        proxy_https_string = "https://%s:%s" % (proxy_host, proxy_port)
        datalab.fab.conn.sudo('sed -i "/^export http_proxy/d" /etc/profile')
        datalab.fab.conn.sudo('sed -i "/^export https_proxy/d" /etc/profile')
        datalab.fab.conn.sudo('bash -c "echo export http_proxy=' + proxy_string + ' >> /etc/profile"')
        datalab.fab.conn.sudo('bash -c "echo export https_proxy=' + proxy_string + ' >> /etc/profile"')
        if exists(datalab.fab.conn, '/etc/yum.conf'):
            datalab.fab.conn.sudo('sed -i "/^proxy=/d" /etc/yum.conf')
        datalab.fab.conn.sudo('''bash -c "echo 'proxy={}' >> /etc/yum.conf" '''.format(proxy_string))
        manage_pkg('clean all', 'remote', '')
    except:
        sys.exit(1)


def downgrade_python_version():
    try:
       datalab.fab.conn.sudo('python3 -c "import os,sys,yum; yb = yum.YumBase(); pl = yb.doPackageLists(); \
        version = [pkg.vr for pkg in pl.installed if pkg.name == \'python\']; \
        os.system(\'yum -y downgrade python python-devel-2.7.5-58.el7.x86_64 python-libs-2.7.5-58.el7.x86_64\') \
        if version != [] and version[0] == \'2.7.5-68.el7\' else False"')
    except:
        sys.exit(1)


def ensure_r_local_kernel(spark_version, os_user, templates_dir, kernels_dir):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/r_kernel_ensured'.format(os_user)):
        try:
            datalab.fab.conn.sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            datalab.fab.conn.run('R -e "IRkernel::installspec()"')
            datalab.fab.conn.sudo('ln -s /opt/spark/ /usr/local/spark')
            try:
                datalab.fab.conn.sudo('''bash -c 'cd /usr/local/spark/R/lib/SparkR; R -e "install.packages(\'roxygen2\',repos=\'https://cloud.r-project.org\')" R -e "devtools::check(\'.\')"' ''')
            except:
                pass
            datalab.fab.conn.sudo('''bash -c 'cd /usr/local/spark/R/lib/SparkR; R -e "devtools::install(\'.\')"' ''')
            r_version = datalab.fab.conn.sudo("R --version | awk '/version / {print $3}'").stdout.replace('\n','')
            datalab.fab.conn.put(templates_dir + 'r_template.json', '/tmp/r_template.json')
            datalab.fab.conn.sudo('sed -i "s|R_VER|' + r_version + '|g" /tmp/r_template.json')
            datalab.fab.conn.sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/r_template.json')
            datalab.fab.conn.sudo('\cp -f /tmp/r_template.json {}/ir/kernel.json'.format(kernels_dir))
            datalab.fab.conn.sudo('ln -s /usr/lib64/R/ /usr/lib/R')
            datalab.fab.conn.sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/r_kernel_ensured'.format(os_user))
        except:
            sys.exit(1)


def ensure_r(os_user, r_libs):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/r_ensured'.format(os_user)):
        try:
            r_repository = 'https://cloud.r-project.org'
            manage_pkg('-y install', 'remote', 'cmake')
            manage_pkg('-y install', 'remote', 'libcur*')
            datalab.fab.conn.sudo('echo -e "[base]\nname=CentOS-7-Base\nbaseurl=http://buildlogs.centos.org/centos/7/os/x86_64-20140704-1/\ngpgcheck=1\ngpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7\npriority=1\nexclude=php mysql" >> /etc/yum.repos.d/CentOS-base.repo')
            manage_pkg('-y install', 'remote', 'R R-core R-core-devel R-devel --nogpgcheck')
            datalab.fab.conn.sudo('R CMD javareconf')
            datalab.fab.conn.sudo('''bash -c 'cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig' ''')
            for i in r_libs:
                datalab.fab.conn.sudo('R -e "install.packages(\'{}\',repos=\'{}\')"'.format(i, r_repository))
            datalab.fab.conn.sudo('R -e "library(\'devtools\');install.packages(repos=\'{}\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"'.format(r_repository))
            datalab.fab.conn.sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            datalab.fab.conn.sudo('R -e "library(\'devtools\');install_version(\'keras\', version = \'{}\', repos = \'{}\');"'.format(os.environ['notebook_keras_version'],r_repository))
            datalab.fab.conn.sudo('R -e "install.packages(\'RJDBC\',repos=\'{}\',dep=TRUE)"'.format(r_repository))
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/r_ensured'.format(os_user))
        except:
            sys.exit(1)


def install_rstudio(os_user, local_spark_path, rstudio_pass, rstudio_version):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/rstudio_ensured'):
        try:
            manage_pkg('-y install --nogpgcheck', 'remote', 'https://download2.rstudio.org/server/centos6/x86_64/rstudio-server-rhel-{}-x86_64.rpm'.format(rstudio_version))
            datalab.fab.conn.sudo('mkdir -p /mnt/var')
            datalab.fab.conn.sudo('chown {0}:{0} /mnt/var'.format(os_user))
            datalab.fab.conn.sudo("sed -i '/Type=forking/a \Environment=USER=datalab-user' /lib/systemd/system/rstudio-server.service")
            datalab.fab.conn.sudo(
                "sed -i '/ExecStart/s|=/usr/lib/rstudio-server/bin/rserver|=/bin/bash -c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64; /usr/lib/rstudio-server/bin/rserver --auth-none 1|g' /lib/systemd/system/rstudio-server.service")
            datalab.fab.conn.sudo("sed -i '/ExecStart/s|$|\"|g' /lib/systemd/system/rstudio-server.service")
            datalab.fab.conn.sudo("systemctl daemon-reload")
            datalab.fab.conn.sudo('touch /home/{}/.Renviron'.format(os_user))
            datalab.fab.conn.sudo('chown {0}:{0} /home/{0}/.Renviron'.format(os_user))
            datalab.fab.conn.sudo('''echo 'SPARK_HOME="{0}"' >> /home/{1}/.Renviron'''.format(local_spark_path, os_user))
            datalab.fab.conn.sudo('touch /home/{}/.Rprofile'.format(os_user))
            datalab.fab.conn.sudo('chown {0}:{0} /home/{0}/.Rprofile'.format(os_user))
            datalab.fab.conn.sudo('''echo 'library(SparkR, lib.loc = c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib")))' >> /home/{}/.Rprofile'''.format(os_user))
            http_proxy = datalab.fab.conn.run('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n','')
            https_proxy = datalab.fab.conn.run('''bash -l -c 'echo $https_proxy' ''').stdout.replace('\n','')
            datalab.fab.conn.sudo('''echo 'Sys.setenv(http_proxy = \"{}\")' >> /home/{}/.Rprofile'''.format(http_proxy, os_user))
            datalab.fab.conn.sudo('''echo 'Sys.setenv(https_proxy = \"{}\")' >> /home/{}/.Rprofile'''.format(https_proxy, os_user))
            datalab.fab.conn.sudo('rstudio-server start')
            datalab.fab.conn.sudo('''bash -c 'echo "{0}:{1}" | chpasswd' '''.format(os_user, rstudio_pass))
            datalab.fab.conn.sudo("sed -i '/exit 0/d' /etc/rc.local")
            datalab.fab.conn.sudo('''bash -c "echo \'sed -i 's/^#SPARK_HOME/SPARK_HOME/' /home/{}/.Renviron\' >> /etc/rc.local"'''.format(os_user))
            datalab.fab.conn.sudo("bash -c 'echo exit 0 >> /etc/rc.local'")
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/rstudio_ensured'.format(os_user))
        except:
            sys.exit(1)
    else:
        try:
            datalab.fab.conn.sudo('''bash -c 'echo "{0}:{1}" | chpasswd' '''.format(os_user, rstudio_pass))
        except:
            sys.exit(1)


def ensure_matplot(os_user):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/matplot_ensured'.format(os_user)):
        try:
            datalab.fab.conn.sudo('python3.5 -m pip install matplotlib=={} --no-cache-dir'.format(os.environ['notebook_matplotlib_version']))
            if os.environ['application'] in ('tensor', 'deeplearning'):
                datalab.fab.conn.sudo('python3.8 -m pip install -U numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/matplot_ensured'.format(os_user))
        except:
            sys.exit(1)


def ensure_sbt(os_user):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/sbt_ensured'.format(os_user)):
        try:
            datalab.fab.conn.sudo('curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo')
            manage_pkg('-y install', 'remote', 'sbt')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/sbt_ensured'.format(os_user))
        except:
            sys.exit(1)


def ensure_jre_jdk(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/jre_jdk_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'java-1.8.0-openjdk')
            manage_pkg('-y install', 'remote', 'java-1.8.0-openjdk-devel')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/jre_jdk_ensured')
        except:
            sys.exit(1)


def ensure_scala(scala_link, scala_version, os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/scala_ensured'):
        try:
            datalab.fab.conn.sudo('wget {}scala-{}.rpm -O /tmp/scala.rpm'.format(scala_link, scala_version))
            datalab.fab.conn.sudo('rpm -i /tmp/scala.rpm')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/scala_ensured')
        except:
            sys.exit(1)


def ensure_additional_python_libs(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/additional_python_libs_ensured'):
        try:
            manage_pkg('clean', 'remote', 'all')
            manage_pkg('-y install', 'remote', 'zlib-devel libjpeg-turbo-devel --nogpgcheck')
            if os.environ['application'] in ('jupyter', 'zeppelin'):
                datalab.fab.conn.sudo('python3.5 -m pip install NumPy=={} SciPy pandas Sympy Pillow sklearn --no-cache-dir'.format(os.environ['notebook_numpy_version']))
            if os.environ['application'] in ('tensor', 'deeplearning'):
                datalab.fab.conn.sudo('python3.8 -m pip install opencv-python h5py --no-cache-dir')
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/additional_python_libs_ensured')
        except:
            sys.exit(1)


def ensure_python3_specific_version(python3_version, os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/python3_specific_version_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'yum-utils python34 openssl-devel')
            manage_pkg('-y groupinstall', 'remote', 'development --nogpgcheck')
            if len(python3_version) < 4:
                python3_version = python3_version + ".0"
            datalab.fab.conn.sudo('wget https://www.python.org/ftp/python/{0}/Python-{0}.tgz'.format(python3_version))
            datalab.fab.conn.sudo('tar xzf Python-{0}.tgz; cd Python-{0}; ./configure --prefix=/usr/local; make altinstall'.format(python3_version))
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/python3_specific_version_ensured')
        except:
            sys.exit(1)

def ensure_python3_libraries(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/python3_libraries_ensured'):
        try:
            manage_pkg('-y install', 'remote', 'https://centos7.iuscommunity.org/ius-release.rpm')
            manage_pkg('-y install', 'remote', 'python35u python35u-pip python35u-devel')
            datalab.fab.conn.sudo('python3.5 -m pip install -U pip=={} setuptools --no-cache-dir'.format(os.environ['conf_pip_version']))
            datalab.fab.conn.sudo('python3.5 -m pip install boto3 --no-cache-dir')
            datalab.fab.conn.sudo('python3.5 -m pip install fabvenv fabric-virtualenv future patchwork --no-cache-dir')
            try:
                datalab.fab.conn.sudo('python3.5 -m pip install tornado=={0} ipython==7.9.0 ipykernel=={1} --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            except:
                datalab.fab.conn.sudo('python3.5 -m pip install tornado=={0} ipython==5.0.0 ipykernel=={1} --no-cache-dir' \
                     .format(os.environ['notebook_tornado_version'], os.environ['notebook_ipykernel_version']))
            datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/python3_libraries_ensured')
        except:
            sys.exit(1)


def install_tensor(os_user, cuda_version, cuda_file_name,
                   cudnn_version, cudnn_file_name, tensorflow_version,
                   templates_dir, nvidia_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/tensor_ensured'.format(os_user)):
        try:
            # install nvidia drivers
            datalab.fab.conn.sudo('''bash -c 'echo "blacklist nouveau" >> /etc/modprobe.d/blacklist-nouveau.conf' ''')
            datalab.fab.conn.sudo('''bash -c 'echo "options nouveau modeset=0" >> /etc/modprobe.d/blacklist-nouveau.conf' ''')
            datalab.fab.conn.sudo('dracut --force')
            datalab.fab.conn.sudo('reboot', warn=True)
            time.sleep(150)
            manage_pkg('-y install', 'remote', 'libglvnd-opengl libglvnd-devel dkms gcc kernel-devel-$(uname -r) kernel-headers-$(uname -r)')
            datalab.fab.conn.sudo('wget http://us.download.nvidia.com/XFree86/Linux-x86_64/{0}/NVIDIA-Linux-x86_64-{0}.run -O /home/{1}/NVIDIA-Linux-x86_64-{0}.run'.format(nvidia_version, os_user))
            datalab.fab.conn.sudo('/bin/bash /home/{0}/NVIDIA-Linux-x86_64-{1}.run -s --dkms'.format(os_user, nvidia_version))
            datalab.fab.conn.sudo('rm -f /home/{0}/NVIDIA-Linux-x86_64-{1}.run'.format(os_user, nvidia_version))
            # install cuda
            datalab.fab.conn.sudo('python3.5 -m pip install --upgrade pip=={0} wheel numpy=={1} --no-cache-dir'. format(os.environ['conf_pip_version'], os.environ['notebook_numpy_version']))
            datalab.fab.conn.sudo('wget -P /opt https://developer.nvidia.com/compute/cuda/{0}/prod/local_installers/{1}'.format(cuda_version, cuda_file_name))
            datalab.fab.conn.sudo('sh /opt/{} --silent --toolkit'.format(cuda_file_name))
            datalab.fab.conn.sudo('mv /usr/local/cuda-{} /opt/'.format(cuda_version[:-2]))
            datalab.fab.conn.sudo('ln -s /opt/cuda-{0} /usr/local/cuda-{0}'.format(cuda_version[:-2]))
            datalab.fab.conn.sudo('rm -f /opt/{}'.format(cuda_file_name))
            # install cuDNN
            datalab.fab.conn.run('wget https://developer.download.nvidia.com/compute/redist/cudnn/v{0}/{1} -O /tmp/{1}'.format(cudnn_version, cudnn_file_name))
            datalab.fab.conn.run('tar xvzf /tmp/{} -C /tmp'.format(cudnn_file_name))
            datalab.fab.conn.sudo('mkdir -p /opt/cudnn/include')
            datalab.fab.conn.sudo('mkdir -p /opt/cudnn/lib64')
            datalab.fab.conn.sudo('mv /tmp/cuda/include/cudnn.h /opt/cudnn/include')
            datalab.fab.conn.sudo('mv /tmp/cuda/lib64/libcudnn* /opt/cudnn/lib64')
            datalab.fab.conn.sudo('chmod a+r /opt/cudnn/include/cudnn.h /opt/cudnn/lib64/libcudnn*')
            datalab.fab.conn.run('''bash -l -c 'echo "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64\"" >> ~/.bashrc' ''')
            # install TensorFlow and run TensorBoard
            datalab.fab.conn.sudo('wget https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp27-none-linux_x86_64.whl'.format(tensorflow_version))
            datalab.fab.conn.sudo('wget https://storage.googleapis.com/tensorflow/linux/gpu/tensorflow_gpu-{}-cp35-cp35m-linux_x86_64.whl'.format(tensorflow_version))
            datalab.fab.conn.sudo('python3.8 -m pip install --upgrade tensorflow_gpu-{}-cp35-cp35m-linux_x86_64.whl --no-cache-dir'.format(tensorflow_version))
            datalab.fab.conn.sudo('rm -rf /home/{}/tensorflow_gpu-*'.format(os_user))
            datalab.fab.conn.sudo('mkdir /var/log/tensorboard; chown {0}:{0} -R /var/log/tensorboard'.format(os_user))
            datalab.fab.conn.put('{}tensorboard.service'.format(templates_dir), '/tmp/tensorboard.service')
            datalab.fab.conn.sudo("sed -i 's|OS_USR|{}|' /tmp/tensorboard.service".format(os_user))
            datalab.fab.conn.sudo("chmod 644 /tmp/tensorboard.service")
            datalab.fab.conn.sudo('\cp /tmp/tensorboard.service /etc/systemd/system/')
            datalab.fab.conn.sudo("systemctl daemon-reload")
            datalab.fab.conn.sudo("systemctl enable tensorboard")
            datalab.fab.conn.sudo("systemctl start tensorboard")
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/tensor_ensured'.format(os_user))
        except:
            sys.exit(1)


def install_maven(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/maven_ensured'):
        datalab.fab.conn.sudo('wget http://apache.volia.net/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz -O /tmp/maven.tar.gz')
        datalab.fab.conn.sudo('tar -zxvf /tmp/maven.tar.gz -C /opt/')
        datalab.fab.conn.sudo('ln -fs /opt/apache-maven-3.3.9/bin/mvn /usr/bin/mvn')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/maven_ensured')


def install_livy_dependencies(os_user):
    if not exists(datalab.fab.conn,'/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        datalab.fab.conn.sudo('pip3.5 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured')


def install_maven_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/maven_ensured'):
        subprocess.run('wget http://apache.volia.net/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz -O /tmp/maven.tar.gz', shell=True, check=True)
        subprocess.run('sudo tar -zxvf /tmp/maven.tar.gz -C /opt/', shell=True, check=True)
        subprocess.run('sudo ln -fs /opt/apache-maven-3.3.9/bin/mvn /usr/bin/mvn', shell=True, check=True)
        subprocess.run('touch /home/' + os_user + '/.ensure_dir/maven_ensured', shell=True, check=True)


def install_livy_dependencies_emr(os_user):
    if not os.path.exists('/home/' + os_user + '/.ensure_dir/livy_dependencies_ensured'):
        subprocess.run('sudo -i pip3.5 install cloudpickle requests requests-kerberos flake8 flaky pytest --no-cache-dir', shell=True, check=True)
        subprocess.run('touch /home/' + os_user + '/.ensure_dir/livy_dependencies_ensured', shell=True, check=True)


def install_nodejs(os_user):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/nodejs_ensured'.format(os_user)):
        datalab.fab.conn.sudo('curl -sL https://rpm.nodesource.com/setup_6.x | sudo -E bash -')
        manage_pkg('-y install', 'remote', 'nodejs')
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/nodejs_ensured'.format(os_user))


def install_os_pkg(requisites):
    status = list()
    error_parser = "Could not|No matching|Error:|failed|Requires:|Errno"
    new_pkgs_parser = "Dependency Installed:"
    try:
        print("Updating repositories and installing requested tools: {}".format(requisites))
        manage_pkg('update-minimal --security -y --skip-broken', 'remote', '')
        #datalab.fab.conn.sudo('export LC_ALL=C')
        for os_pkg in requisites:
            name, vers = os_pkg
            if vers != '' and vers !='N/A':
                version = vers
                os_pkg = "{}-{}".format(name, vers)
            else:
                version = 'N/A'
                os_pkg = name
            manage_pkg('-y install', 'remote', '{0} --nogpgcheck 2>&1 | tee /tmp/os_install_{2}.tmp; if ! grep -w -E  "({1})" '
                                               '/tmp/os_install_{2}.tmp >  /tmp/os_install_{2}.log; then  echo "no_error" > /tmp/os_install_{2}.log;fi'.format(os_pkg, error_parser, name))
            install_output = datalab.fab.conn.sudo('cat /tmp/os_install_{}.tmp'.format(name)).stdout
            err = datalab.fab.conn.sudo('cat /tmp/os_install_{}.log'.format(name)).stdout.replace('"', "'")
            datalab.fab.conn.sudo('cat /tmp/os_install_{0}.tmp | if ! grep -w -E -A 30 "({1})" /tmp/os_install_{0}.tmp > '
                 '/tmp/os_install_{0}.log; then echo "no_dependencies" > /tmp/os_install_{0}.log;fi'.format(name, new_pkgs_parser))
            dep = datalab.fab.conn.sudo('cat /tmp/os_install_{}.log'.format(name)).stdout
            if 'no_dependencies' in dep:
                dep = []
            else:
                dep = dep[len(new_pkgs_parser): dep.find("Complete!") - 1].replace('  ', '').strip().split('\r\n')
                for n, i in enumerate(dep):
                    i = i.split('.')[0]
                    datalab.fab.conn.sudo('yum info {0} 2>&1 | if ! grep Version > /tmp/os_install_{0}.log; then echo "" > /tmp/os_install_{0}.log;fi'.format(i))
                    dep[n] =sudo('cat /tmp/os_install_{}.log'.format(i)).replace('Version     : ', '{} v.'.format(i))
                dep = [i for i in dep if i]
            versions = []
            datalab.fab.conn.sudo(
                'yum list installed | if ! grep "{0}\." > /tmp/os_install_{0}.list; then echo "not_installed" > /tmp/os_install_{0}.list;fi'.format(
                    name))
            res = datalab.fab.conn.sudo('cat /tmp/os_install_{}.list '.format(name) + '| awk \'{print $1":"$2}\'').stdout.replace('\n', '')
            #res = datalab.fab.conn.sudo('python3 -c "import os,sys,yum; yb = yum.YumBase(); pl = yb.doPackageLists(); print [pkg.vr for pkg in pl.installed if pkg.name == \'{0}\']"'.format(name)).stdout.split('\r\n')[1]
            if "no_error" not in err:
                status_msg = 'installation_error'
            elif "not_installed" not in res:
                version = res.split(":")[1]
                status_msg = "installed"
            if 'No package {} available'.format(os_pkg) in install_output:
                versions = datalab.fab.conn.sudo('yum --showduplicates list ' + name + ' | expand | grep ' + name + ' | awk \'{print $2}\'').stdout.replace('\r\n', '')
                if versions and versions != 'Error: No matching Packages to list':
                    versions = versions.split(' ')
                    status_msg = 'invalid_version'
                    for n, i in enumerate(versions):
                        if ':' in i:
                            versions[n] = i.split(':')[1].split('-')[0]
                        else:
                            versions[n] = i.split('-')[0]
                else:
                    versions = []
                    status_msg = 'invalid_name'
            status.append({"group": "os_pkg", "name": name, "version": version, "status": status_msg,
                           "error_message": err, "add_pkgs": dep, "available_versions": versions})
        datalab.fab.conn.sudo('rm /tmp/*{}*'.format(name))
        return status
    except Exception as err:
        for os_pkg in requisites:
            name, vers = os_pkg
            status.append(
                {"group": "os_pkg", "name": name, "version": vers, "status": 'installation_error', "error_message": err})
        print("Failed to install OS packages: {}".format(requisites))
        return status

def remove_os_pkg(pkgs):
    try:
        manage_pkg('remove -y', 'remote', '{}'.format(' '.join(pkgs)))
    except:
        sys.exit(1)


def get_available_os_pkgs():
    try:
        os_pkgs = dict()
        manage_pkg('update-minimal --security -y --skip-broken', 'remote', '')
        #downgrade_python_version()
        yum_names = datalab.fab.conn.sudo("yum list available | grep -v \"Loaded plugins:\|Available Packages\" | xargs -n3 | column -t | awk '{print $1}'").stdout.split('\n')
        for pkg in yum_names:
            if "." in pkg:
                os_pkgs[pkg.split('.')[0]] = 'N/A'
            elif pkg != '':
                os_pkgs[pkg] = 'N/A'
        return os_pkgs
    except Exception as err:
        append_result("Failed to get available os packages.", str(err))
        sys.exit(1)


def install_opencv(os_user):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/opencv_ensured'.format(os_user)):
        manage_pkg('-y install', 'remote', 'cmake python34 python34-devel python34-pip gcc gcc-c++')
        datalab.fab.conn.sudo('pip3.4 install numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy_version']))
        datalab.fab.conn.sudo('pip3.5 install numpy=={} --no-cache-dir'.format(os.environ['notebook_numpy_version']))
        datalab.fab.conn.run('git clone https://github.com/opencv/opencv.git')
        datalab.fab.conn.run('cd /home/{}/opencv/ && git checkout 3.2.0'.format(os_user))
        datalab.fab.conn.run('cd /home/{}/opencv/ && mkdir release'.format(os_user))
        datalab.fab.conn.run('cd /home/{}/opencv/release/ && cmake -DINSTALL_TESTS=OFF -D CUDA_GENERATION=Auto -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=$(python2 -c "import sys; print(sys.prefix)") -D PYTHON_EXECUTABLE=$(which python2) ..')
        datalab.fab.conn.run('cd /home/{}/opencv/release/ && make -j$(nproc)')
        datalab.fab.conn.sudo('''bash -c 'cd /home/{}/opencv/release/ &&  make install' ''')
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/opencv_ensured')


def install_caffe2(os_user, caffe2_version, cmake_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/caffe2_ensured'.format(os_user)):
        env.shell = "/bin/bash -l -c -i"
        manage_pkg('update-minimal --security -y', 'remote', '')
        manage_pkg('-y install --nogpgcheck', 'remote', 'automake cmake3 gcc gcc-c++ kernel-devel leveldb-devel lmdb-devel libtool protobuf-devel graphviz')
        datalab.fab.conn.sudo('pip3.5 install flask graphviz hypothesis jupyter matplotlib=={} numpy=={} protobuf pydot python-nvd3 pyyaml '
             'requests scikit-image scipy setuptools tornado future --no-cache-dir'.format(os.environ['notebook_matplotlib_version'], os.environ['notebook_numpy_version']))
        datalab.fab.conn.sudo('cp /opt/cudnn/include/* /opt/cuda-8.0/include/')
        datalab.fab.conn.sudo('cp /opt/cudnn/lib64/* /opt/cuda-8.0/lib64/')
        datalab.fab.conn.sudo('wget https://cmake.org/files/v{2}/cmake-{1}.tar.gz -O /home/{0}/cmake-{1}.tar.gz'.format(
            os_user, cmake_version, cmake_version.split('.')[0] + "." + cmake_version.split('.')[1]))
        datalab.fab.conn.sudo('tar -zxvf cmake-{}.tar.gz'.format(cmake_version))
        datalab.fab.conn.sudo('''bash -c 'cd /home/{}/cmake-{}/ && ./bootstrap --prefix=/usr/local && make && make install' '''.format(os_user, cmake_version))
        datalab.fab.conn.sudo('ln -s /usr/local/bin/cmake /bin/cmake{}'.format(cmake_version))
        datalab.fab.conn.sudo('git clone https://github.com/pytorch/pytorch.git')
        datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && git submodule update --init' '''.format(os_user))
        datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && git checkout v{}' '''.format(os_user, caffe2_version), warn=True)
        datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && git submodule update --recursive' '''.format(os_user), warn=True)
        datalab.fab.conn.sudo('''bash -c 'cd /home/{}/pytorch/ && mkdir build && cd build && cmake{} .. && make "-j$(nproc)" install' '''.format(os_user, cmake_version))
        datalab.fab.conn.sudo('touch /home/' + os_user + '/.ensure_dir/caffe2_ensured')


def install_cntk(os_user, cntk_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/cntk_ensured'.format(os_user)):
        datalab.fab.conn.sudo('echo "exclude=*.i386 *.i686" >> /etc/yum.conf')
        manage_pkg('clean', 'remote', 'all')
        manage_pkg('update-minimal --security -y', 'remote', '')
        manage_pkg('-y install --nogpgcheck', 'remote', 'openmpi openmpi-devel')
        datalab.fab.conn.sudo('pip3.5 install https://cntk.ai/PythonWheel/GPU/cntk-{}-cp35-cp35m-linux_x86_64.whl --no-cache-dir'.format(cntk_version))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/cntk_ensured'.format(os_user))


def install_keras(os_user, keras_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/keras_ensured'.format(os_user)):
        datalab.fab.conn.sudo('pip3.5 install keras=={} --no-cache-dir'.format(keras_version))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/keras_ensured'.format(os_user))


def install_theano(os_user, theano_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/theano_ensured'.format(os_user)):
        datalab.fab.conn.sudo('python3.8 -m pip install Theano=={} --no-cache-dir'.format(theano_version))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/theano_ensured'.format(os_user))


def install_mxnet(os_user, mxnet_version):
    if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/mxnet_ensured'.format(os_user)):
        datalab.fab.conn.sudo('pip3.5 install mxnet-cu80=={} opencv-python --no-cache-dir'.format(mxnet_version))
        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/mxnet_ensured'.format(os_user))


#def install_torch(os_user):
#    if not exists(conn,'/home/{}/.ensure_dir/torch_ensured'.format(os_user)):
#        run('git clone https://github.com/torch/distro.git ~/torch --recursive')
#        with cd('/home/{}/torch/'.format(os_user)):
#            manage_pkg('-y install --nogpgcheck', 'remote', 'cmake curl readline-devel ncurses-devel gcc-c++ gcc-gfortran git gnuplot unzip libjpeg-turbo-devel libpng-devel ImageMagick GraphicsMagick-devel fftw-devel sox-devel sox zeromq3-devel qt-devel qtwebkit-devel sox-plugins-freeworld qt-devel')
#            run('./install.sh -b')
#        run('source /home/{}/.bashrc'.format(os_user))
#        conn.sudo('touch /home/{}/.ensure_dir/torch_ensured'.format(os_user))


def install_gitlab_cert(os_user, certfile):
    try:
        datalab.fab.conn.sudo('mv -f /home/{0}/{1} /etc/pki/ca-trust/source/anchors/{1}'.format(os_user, certfile))
        datalab.fab.conn.sudo('update-ca-trust')
    except Exception as err:
        print('Failed to install gitlab certificate.{}'.format(str(err)))
