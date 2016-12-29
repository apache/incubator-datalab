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

from dlab.aws_actions import *
from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import sys
import os

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--rstudio_pass', type=str, default='')
args = parser.parse_args()

spark_version = os.environ['notebook_spark_version']
hadoop_version = os.environ['notebook_hadoop_version']
spark_link = "http://d3kbcqa49mib13.cloudfront.net/spark-" + spark_version + "-bin-hadoop" + hadoop_version + ".tgz"
local_spark_path = '/opt/spark/'
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


def ensure_libraries_py():
    if not exists('/home/ubuntu/.ensure_dir/ensure_libraries_py_installed'):
        try:
            sudo('export LC_ALL=C')
            sudo('apt-get install python3-setuptools')
            sudo('apt install -y python3-pip')
            sudo('apt-get install -y python-virtualenv')
            sudo('pip2 install -U pip --no-cache-dir')
            sudo('pip2 install boto boto3 --no-cache-dir')
            sudo('pip2 install fabvenv fabric-virtualenv --no-cache-dir')
            sudo('pip3 install -U pip --no-cache-dir')
            sudo('pip3 install boto boto3 --no-cache-dir')
            sudo('pip3 install fabvenv fabric-virtualenv --no-cache-dir')
            sudo('touch /home/ubuntu/.ensure_dir/ensure_libraries_py_installed')
        except:
            sys.exit(1)


def install_rstudio():
    if not exists('/home/ubuntu/.ensure_dir/rstudio_ensured'):
        try:
            sudo('apt-get install -y default-jre')
            sudo('apt-get install -y default-jdk')
            sudo('apt-get install -y r-base')
            sudo('apt-get install -y gdebi-core')
            sudo('apt-get install -y r-cran-rjava r-cran-evaluate r-cran-formatr r-cran-yaml r-cran-rcpp r-cran-catools r-cran-jsonlite r-cran-ggplot2')
            sudo('R CMD javareconf')
            sudo('R -e \'install.packages("rmarkdown", repos = "https://cran.revolutionanalytics.com")\'')
            sudo('R -e \'install.packages("base64enc", repos = "https://cran.revolutionanalytics.com")\'')
            sudo('R -e \'install.packages("tibble", repos = "https://cran.revolutionanalytics.com")\'')
            sudo('wget https://download2.rstudio.org/rstudio-server-1.0.44-amd64.deb')
            sudo('gdebi -n rstudio-server-1.0.44-amd64.deb')
            sudo('mkdir /mnt/var')
            sudo('chown ubuntu:ubuntu /mnt/var')
            sudo('touch /home/ubuntu/.Renviron')
            sudo('chown ubuntu:ubuntu /home/ubuntu/.Renviron')
            sudo('''echo 'SPARK_HOME="''' + local_spark_path + '''"' >> /home/ubuntu/.Renviron''')
            sudo('touch /home/ubuntu/.Rprofile')
            sudo('chown ubuntu:ubuntu /home/ubuntu/.Rprofile')
            sudo('''echo 'library(SparkR, lib.loc = c(file.path(Sys.getenv("SPARK_HOME"), "R", "lib")))' >> /home/ubuntu/.Rprofile''')
            sudo('rstudio-server start')
            sudo('echo "ubuntu:' + args.rstudio_pass + '" | chpasswd')
            sudo('touch /home/ubuntu/.ensure_dir/rstudio_ensured')
        except:
            sys.exit(1)
    else:
        try:
            sudo('echo "ubuntu:' + args.rstudio_pass + '" | chpasswd')
        except:
            sys.exit(1)


def ensure_local_spark():
    if not exists('/home/ubuntu/.ensure_dir/local_spark_ensured'):
        try:
            sudo('wget ' + spark_link + ' -O /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
            sudo('tar -zxvf /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
            sudo('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + local_spark_path)
            sudo('chown -R ubuntu:ubuntu ' + local_spark_path)
            sudo('touch /home/ubuntu/.ensure_dir/local_spark_ensured')
        except:
            sys.exit(1)


def ensure_s3_kernel():
    if not exists('/home/ubuntu/.ensure_dir/s3_kernel_ensured'):
        try:
            sudo('mkdir -p ' + s3_jars_dir)
            put(templates_dir + 'jars/local_jars.tar.gz', '/tmp/local_jars.tar.gz')
            sudo('tar -xzf /tmp/local_jars.tar.gz -C ' + s3_jars_dir)
            put(templates_dir + 'spark-defaults_local.conf', '/tmp/spark-defaults_local.conf')
            sudo("sed -i 's/URL/https:\/\/s3-{}.amazonaws.com/' /tmp/spark-defaults_local.conf".format(
                args.region))
            sudo('\cp /tmp/spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/ubuntu/.ensure_dir/s3_kernel_ensured')
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

    print "Configuring notebook server."
    try:
        if not exists('/home/ubuntu/.ensure_dir'):
            sudo('mkdir /home/ubuntu/.ensure_dir')
    except:
        sys.exit(1)

    print "Mount additional volume"
    prepare_disk()

    print "Install python libraries"
    ensure_libraries_py()

    print "Install RStudio"
    install_rstudio()

    print "Install local Spark"
    ensure_local_spark()

    print "Install local S3 kernels"
    ensure_s3_kernel()

    # for image purpose
    print "Clean up lock files"
    remove_apt_lock()
