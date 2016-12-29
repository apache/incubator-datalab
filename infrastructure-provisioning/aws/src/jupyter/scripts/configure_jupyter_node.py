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
import os

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--instance_name', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()

spark_version = os.environ['notebook_spark_version']
hadoop_version = os.environ['notebook_hadoop_version']
scala_link = "http://www.scala-lang.org/files/archive/scala-2.11.8.deb"
spark_link = "http://d3kbcqa49mib13.cloudfront.net/spark-" + spark_version + "-bin-hadoop" + hadoop_version + ".tgz"
pyspark_local_path_dir = '/home/ubuntu/.local/share/jupyter/kernels/pyspark_local/'
py3spark_local_path_dir = '/home/ubuntu/.local/share/jupyter/kernels/py3spark_local/'
jupyter_conf_file = '/home/ubuntu/.local/share/jupyter/jupyter_notebook_config.py'
s3_jars_dir = '/opt/jars/'
templates_dir = '/root/templates/'


def prepare_disk():
    if not exists('/home/ubuntu/.ensure_dir/disk_ensured'):
        try:
            sudo('''bash -c 'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/xvdb' ''')
            sudo('mkfs.ext4 /dev/xvdb1')
            sudo('mount /dev/xvdb1 /opt/')
            sudo(''' bash -c "echo '/dev/xvdb1 /opt/ ext4 errors=remount-ro 0 1' >> /etc/fstab" ''')
            sudo('touch /home/ubuntu/.ensure_dir/disk_ensured')
        except:
            sys.exit(1)


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def ensure_spark_scala():
    if not exists('/home/ubuntu/.ensure_dir/spark_scala_ensured'):
        try:
            sudo('apt-get install -y default-jre')
            sudo('apt-get install -y default-jdk')
            sudo('wget ' + scala_link + ' -O /tmp/scala.deb')
            sudo('dpkg -i /tmp/scala.deb')
            sudo('wget ' + spark_link + ' -O /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
            sudo('tar -zxvf /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
            sudo('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' /opt/spark')
            sudo('mkdir -p ' + pyspark_local_path_dir)
            sudo('mkdir -p ' + py3spark_local_path_dir)
            sudo('touch ' + pyspark_local_path_dir + 'kernel.json')
            sudo('touch ' + py3spark_local_path_dir + 'kernel.json')
            put(templates_dir + 'pyspark_local_template.json', '/tmp/pyspark_local_template.json')
            put(templates_dir + 'py3spark_local_template.json', '/tmp/py3spark_local_template.json')
            sudo(
                "PYJ=`find /opt/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; sed -i 's|PY4J|'$PYJ'|g' /tmp/pyspark_local_template.json")
            sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/pyspark_local_template.json')
            sudo('\cp /tmp/pyspark_local_template.json ' + pyspark_local_path_dir + 'kernel.json')
            sudo(
                "PYJ=`find /opt/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; sed -i 's|PY4J|'$PYJ'|g' /tmp/py3spark_local_template.json")
            sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/py3spark_local_template.json')
            sudo('\cp /tmp/py3spark_local_template.json ' + py3spark_local_path_dir + 'kernel.json')
            sudo('pip install --pre toree --no-cache-dir')
            sudo('ln -s /opt/spark/ /usr/local/spark')
            sudo('jupyter toree install')
            sudo('mv /usr/local/share/jupyter/kernels/apache_toree_scala/lib/* /tmp/')
            put(templates_dir + 'toree-assembly-0.2.0.jar', '/tmp/toree-assembly-0.2.0.jar')
            sudo('mv /tmp/toree-assembly-0.2.0.jar /usr/local/share/jupyter/kernels/apache_toree_scala/lib/')
            sudo('touch /home/ubuntu/.ensure_dir/spark_scala_ensured')
        except:
            sys.exit(1)


def ensure_python3_kernel():
    if not exists('/home/ubuntu/.ensure_dir/python3_kernel_ensured'):
        try:
            sudo('apt-get install python3-setuptools')
            sudo('apt install -y python3-pip')
            sudo('pip3 install ipython ipykernel --no-cache-dir')
            sudo('python3 -m ipykernel install')
            sudo('apt-get install -y libssl-dev python-virtualenv')
            sudo('touch /home/ubuntu/.ensure_dir/python3_kernel_ensured')
        except:
            sys.exit(1)


def ensure_s3_kernel():
    if not exists('/home/ubuntu/.ensure_dir/s3_kernel_ensured'):
        try:
            sudo('mkdir -p ' + s3_jars_dir)
            put(templates_dir + 'jars/local_jars.tar.gz', '/tmp/local_jars.tar.gz')
            sudo('tar -xzf /tmp/local_jars.tar.gz -C ' + s3_jars_dir)
            put(templates_dir + 'spark-defaults_local.conf', '/tmp/spark-defaults_local.conf')
            sudo("sed -i 's/URL/https:\/\/s3-{}.amazonaws.com/' /tmp/spark-defaults_local.conf".format(args.region))
            sudo('\cp /tmp/spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/ubuntu/.ensure_dir/s3_kernel_ensured')
        except:
            sys.exit(1)


def ensure_r_kernel():
    templates_dir = '/root/templates/'
    kernels_dir = '/home/ubuntu/.local/share/jupyter/kernels/'
    if not exists('/home/ubuntu/.ensure_dir/r_kernel_ensured'):
        try:
            sudo('apt-get install -y r-base r-base-dev r-cran-rcurl')
            sudo('apt-get install -y libcurl4-openssl-dev libssl-dev libreadline-dev')
            sudo('apt-get install -y cmake')
            #sudo('add-apt-repository -y ppa:webupd8team/java')
            #sudo('apt-get update')
            #sudo('echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections')
            #sudo('echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections')
            #sudo('apt-get -y install oracle-java8-installer')
            #sudo('update-java-alternatives -s java-8-oracle')
            #sudo('apt-get install oracle-java8-set-default')
            sudo('R CMD javareconf')
            sudo('cd /root; git clone https://github.com/zeromq/zeromq4-x.git; cd zeromq4-x/; mkdir build; cd build; cmake ..; make install; ldconfig')
            sudo('R -e "install.packages(\'R6\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'pbdZMQ\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'RCurl\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'devtools\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'reshape2\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'caTools\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'rJava\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "install.packages(\'ggplot2\',repos=\'http://cran.us.r-project.org\')"')
            sudo('R -e "library(\'devtools\');install.packages(repos=\'http://cran.us.r-project.org\',c(\'rzmq\',\'repr\',\'digest\',\'stringr\',\'RJSONIO\',\'functional\',\'plyr\'))"')
            sudo('R -e "library(\'devtools\');install_github(\'IRkernel/repr\');install_github(\'IRkernel/IRdisplay\');install_github(\'IRkernel/IRkernel\');"')
            sudo('R -e "install.packages(\'RJDBC\',repos=\'http://cran.us.r-project.org\',dep=TRUE)"')
            sudo('R -e "IRkernel::installspec()"')
            put(templates_dir + 'r_template.json', '/tmp/r_template.json')
            sudo('\cp -f /tmp/r_template.json {}/ir/kernel.json'.format(kernels_dir))
            # sudo('export PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/opt/aws/bin:/root/bin; R -e \'IRkernel::installspec(user = FALSE)\'')
            # Spark Install
            sudo('cd /usr/local/spark/R/lib/SparkR; R -e "devtools::install(\'.\')"')
            #sudo('export SPARK_HOME=/usr/local/spark/; cd $SPARK_HOME/R/lib/; sudo R --no-site-file --no-environ --no-save --no-restore CMD INSTALL "SparkR"')
            sudo('touch /home/ubuntu/.ensure_dir/r_kernel_ensured')
        except:
            sys.exit(1)


def configure_notebook_server(notebook_name):
    if not exists('/home/ubuntu/.ensure_dir/jupyter_ensured'):
        try:
            sudo('pip install jupyter --no-cache-dir')
            sudo('rm -rf ' + jupyter_conf_file)
            sudo('jupyter notebook --generate-config --config ' + jupyter_conf_file)
            sudo('echo "c.NotebookApp.ip = \'*\'" >> ' + jupyter_conf_file)
            sudo('echo c.NotebookApp.open_browser = False >> ' + jupyter_conf_file)
            sudo('echo "c.NotebookApp.base_url = \'/' + notebook_name + '/\'" >> ' + jupyter_conf_file)
            sudo('echo \'c.NotebookApp.cookie_secret = b"' + id_generator() + '"\' >> ' + jupyter_conf_file)
            sudo('''echo "c.NotebookApp.token = u''" >> ''' + jupyter_conf_file)
            sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> ' + jupyter_conf_file)
        except:
            sys.exit(1)

        ensure_spark_scala()

        try:
            put(templates_dir + 'jupyter-notebook.service', '/tmp/jupyter-notebook.service')
            sudo("chmod 644 /tmp/jupyter-notebook.service")
            sudo("sed -i 's|CONF_PATH|" + jupyter_conf_file + "|' /tmp/jupyter-notebook.service")
            sudo('\cp /tmp/jupyter-notebook.service /etc/systemd/system/jupyter-notebook.service')
            sudo('chown -R ubuntu:ubuntu /home/ubuntu/.local')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable jupyter-notebook")
            sudo("systemctl start jupyter-notebook")
            sudo('touch /home/ubuntu/.ensure_dir/jupyter_ensured')
        except:
            sys.exit(1)

        ensure_python3_kernel()

        ensure_s3_kernel()

        ensure_r_kernel()
    else:
        try:
            sudo("sed -i '/^c.NotebookApp.base_url/d' " + jupyter_conf_file)
            sudo('echo "c.NotebookApp.base_url = \'/' + notebook_name + '/\'" >> ' + jupyter_conf_file)
            sudo("systemctl stop jupyter-notebook; sleep 5")
            sudo("systemctl start jupyter-notebook")
        except:
            sys.exit(1)

##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = 'ubuntu@' + args.hostname
    deeper_config = json.loads(args.additional_config)

    print "Configuring notebook server."
    try:
        if not exists('/home/ubuntu/.ensure_dir'):
            sudo('mkdir /home/ubuntu/.ensure_dir')
    except:
        sys.exit(1)
    prepare_disk()
    configure_notebook_server("_".join(args.instance_name.split()))
