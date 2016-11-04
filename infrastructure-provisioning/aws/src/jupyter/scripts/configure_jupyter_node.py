#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import random
import string
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--instance_name', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()

scala_link = "http://www.scala-lang.org/files/archive/scala-2.11.8.deb"
spark_link = "http://d3kbcqa49mib13.cloudfront.net/spark-1.6.2-bin-hadoop2.6.tgz"
spark_version = "1.6.2"
hadoop_version = "2.6"
pyspark_local_path_dir = '/home/ubuntu/.local/share/jupyter/kernels/pyspark_local/'
py3spark_local_path_dir = '/home/ubuntu/.local/share/jupyter/kernels/py3spark_local/'
s3_jars_dir = '/opt/jars/'
templates_dir = '/root/templates/'


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
            sudo('\cp /tmp/pyspark_local_template.json ' + pyspark_local_path_dir + 'kernel.json')
            sudo('\cp /tmp/py3spark_local_template.json ' + py3spark_local_path_dir + 'kernel.json')
            sudo('pip install --pre toree')
            sudo('ln -s /opt/spark/ /usr/local/spark')
            sudo('jupyter toree install')
            sudo('touch /home/ubuntu/.ensure_dir/spark_scala_ensured')
        except:
            sys.exit(1)


def ensure_python3_kernel():
    if not exists('/home/ubuntu/.ensure_dir/python3_kernel_ensured'):
        try:
            sudo('apt-get install python3-setuptools')
            sudo('apt install -y python3-pip')
            sudo('pip3 install ipython ipykernel')
            sudo('python3 -m ipykernel install')
            sudo('add-apt-repository -y ppa:fkrull/deadsnakes')
            sudo('apt update')
            sudo('apt install -y python3.4')
            sudo('python3.4 -m pip install ipython ipykernel  --upgrade')
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
            sudo('\cp /tmp/spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/ubuntu/.ensure_dir/s3_kernel_ensured')
        except:
            sys.exit(1)


def configure_notebook_server(notebook_name):
    try:
        sudo('pip install jupyter')
        sudo('rm -rf /root/.jupyter/jupyter_notebook_config.py')
        sudo("for i in $(ps aux | grep jupyter | grep -v grep | awk '{print $2}'); do kill -9 $i; done")
        sudo('jupyter notebook --generate-config --config /root/.jupyter/jupyter_notebook_config.py')
        sudo('echo "c.NotebookApp.ip = \'*\'" >> /root/.jupyter/jupyter_notebook_config.py')
        sudo('echo c.NotebookApp.open_browser = False >> /root/.jupyter/jupyter_notebook_config.py')
        sudo('echo "c.NotebookApp.base_url = \'/' + notebook_name +
             '/\'" >> /root/.jupyter/jupyter_notebook_config.py')
        sudo('echo \'c.NotebookApp.cookie_secret = "' + id_generator() +
             '"\' >> /root/.jupyter/jupyter_notebook_config.py')
    except:
        sys.exit(1)

    ensure_spark_scala()

    try:
        sudo("sleep 5; for i in $(ps aux | grep jupyter | grep -v grep | awk '{print $2}'); do kill -9 $i; done")
        sudo("sleep 5; screen -d -m jupyter notebook --config /root/.jupyter/jupyter_notebook_config.py; "
             "sleep 5;")
        # for further start up when system boots
        sudo("sed -i '/exit 0/d' /etc/rc.local")
        sudo("sed -i '/screen/d' /etc/rc.local")
        sudo("chmod 757 /etc/rc.local")
        sudo("""echo "cd /home/ubuntu; runuser -l ubuntu -c 'sudo screen -d -m jupyter notebook --config /root/.jupyter/jupyter_notebook_config.py'" >> /etc/rc.local""")
        sudo("chmod 755 /etc/rc.local")
        sudo("bash -c 'echo exit 0 >> /etc/rc.local'")

    except:
        sys.exit(1)

    ensure_python3_kernel()

    ensure_s3_kernel()


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
    configure_notebook_server("_".join(args.instance_name.split()))
