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
import logging
import os
import random
import sys
import string
import json, uuid, time, datetime
from dlab.meta_lib import *
from dlab.actions_lib import *


def ensure_pip(requisites):
    try:
        if not exists('/home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user'])):
            sudo('echo PATH=$PATH:/usr/local/bin/:/opt/spark/bin/ >> /etc/profile')
            sudo('echo export PATH >> /etc/profile')
            sudo('pip install -U pip --no-cache-dir')
            sudo('pip install -U ' + requisites + ' --no-cache-dir')
            sudo('touch /home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user']))
        return True
    except:
        return False


def create_aws_config_files(generate_full_config=False):
    try:
        aws_user_dir = os.environ['AWS_DIR']
        logging.info(local("rm -rf " + aws_user_dir+" 2>&1", capture=True))
        logging.info(local("mkdir -p " + aws_user_dir+" 2>&1", capture=True))

        with open(aws_user_dir + '/config', 'w') as aws_file:
            aws_file.write("[default]\n")
            aws_file.write("region = {}\n".format(os.environ['aws_region']))

        if generate_full_config:
            with open(aws_user_dir + '/credentials', 'w') as aws_file:
                aws_file.write("[default]\n")
                aws_file.write("aws_access_key_id = {}\n".format(os.environ['aws_access_key']))
                aws_file.write("aws_secret_access_key = {}\n".format(os.environ['aws_secret_access_key']))

        logging.info(local("chmod 600 " + aws_user_dir + "/*"+" 2>&1", capture=True))
        logging.info(local("chmod 550 " + aws_user_dir+" 2>&1", capture=True))

        return True
    except:
        return False


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def prepare_disk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/disk_ensured'):
        try:
            sudo('''bash -c 'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/xvdb' ''')
            sudo('mkfs.ext4 /dev/xvdb1')
            sudo('mount /dev/xvdb1 /opt/')
            sudo(''' bash -c "echo '/dev/xvdb1 /opt/ ext4 errors=remount-ro 0 1' >> /etc/fstab" ''')
            sudo('touch /home/' + os_user + '/.ensure_dir/disk_ensured')
        except:
            sys.exit(1)


def ensure_local_jars(os_user, s3_jars_dir, files_dir, region, templates_dir):
    if not exists('/home/' + os_user + '/.ensure_dir/s3_kernel_ensured'):
        try:
            sudo('mkdir -p ' + s3_jars_dir)
            put(files_dir + 'notebook_local_jars.tar.gz', '/tmp/notebook_local_jars.tar.gz')
            sudo('tar -xzf /tmp/notebook_local_jars.tar.gz -C ' + s3_jars_dir)
            put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
            sudo("sed -i 's/URL/https:\/\/s3-{}.amazonaws.com/' /tmp/notebook_spark-defaults_local.conf".format(region))
            if os.environ['application'] == 'zeppelin':
                sudo('echo \"spark.jars $(ls -1 ' + s3_jars_dir + '* | tr \'\\n\' \',\')\" >> /tmp/notebook_spark-defaults_local.conf')
            sudo('\cp /tmp/notebook_spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/' + os_user + '/.ensure_dir/s3_kernel_ensured')
        except:
            sys.exit(1)


def ensure_local_spark(os_user, spark_link, spark_version, hadoop_version, local_spark_path):
    if not exists('/home/' + os_user + '/.ensure_dir/local_spark_ensured'):
        try:
            sudo('wget ' + spark_link + ' -O /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
            sudo('tar -zxvf /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
            sudo('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + local_spark_path)
            sudo('chown -R ' + os_user + ':' + os_user + ' ' + local_spark_path)
            sudo('touch /home/' + os_user + '/.ensure_dir/local_spark_ensured')
        except:
            sys.exit(1)


def prepare(emr_dir, yarn_dir):
    local('mkdir -p ' + emr_dir)
    local('mkdir -p ' + yarn_dir)
    local('sudo mkdir -p /opt/python/')
    result = os.path.exists(emr_dir + 'usr/')
    return result


def spark_defaults(args):
    spark_def_path = '/opt/' + args.emr_version + '/' + args.cluster_name + '/spark/conf/spark-defaults.conf'
    for i in eval(args.excluded_lines):
        local(""" sudo bash -c " sed -i '/""" + i + """/d' """ + spark_def_path + """ " """)
    local(""" sudo bash -c " sed -i '/#/d' """ + spark_def_path + """ " """)
    local(""" sudo bash -c " sed -i '/^\s*$/d' """ + spark_def_path + """ " """)
    local(""" sudo bash -c "sed -i '/spark.driver.extraClassPath/,/spark.driver.extraLibraryPath/s|/usr|/opt/EMRVERSION/jars/usr|g' """ + spark_def_path + """ " """)
    local(""" sudo bash -c "sed -i '/spark.yarn.dist.files/s/\/etc\/spark\/conf/\/opt\/EMRVERSION\/CLUSTER\/conf/g' """ + spark_def_path + """ " """)
    template_file = spark_def_path
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('EMRVERSION', args.emr_version)
    text = text.replace('CLUSTER', args.cluster_name)
    with open(spark_def_path, 'w') as f:
        f.write(text)
    endpoint_url = 'https://s3-' + args.region + '.amazonaws.com'
    local("""bash -c 'echo "spark.hadoop.fs.s3a.endpoint    """ + endpoint_url + """" >> """ + spark_def_path + """'""")


def configuring_notebook(args):
    jars_path = '/opt/' + args.emr_version + '/jars/'
    local("""sudo bash -c "find """ + jars_path + """ -name '*netty*' | xargs rm -f" """)


def append_result(error):
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    with open('/root/result.json', 'a+') as f:
        text = f.read()
    if len(text) == 0:
        res = '{}'
        with open('/root/result.json', 'w') as f:
            f.write(res)
    with open("/root/result.json") as f:
        res = {"[" + st + "]-" + str(uuid.uuid4())[:4] + " Error": error}
        data = json.load(f)
    data.update(res)
    with open("/root/result.json", 'w') as f:
        json.dump(data, f)
    print data


def put_resource_status(resource, status, dlab_path, os_user):
    env['connection_attempts'] = 100
    keyfile = "/root/keys/" + os.environ['conf_key_name'] + ".pem"
    hostname = get_instance_hostname(os.environ['conf_service_base_name'] + '-ssn')
    env.key_filename = [keyfile]
    env.host_string = os_user + '@' + hostname
    sudo('python ' + dlab_path + 'tmp/resource_status.py --resource {} --status {}'.format(resource, status))


def configure_jupyter(os_user, jupyter_conf_file, templates_dir):
    if not exists('/home/' + os_user + '/.ensure_dir/jupyter_ensured'):
        try:
            sudo('pip install jupyter --no-cache-dir')
            sudo('rm -rf ' + jupyter_conf_file)
            sudo('jupyter notebook --generate-config --config ' + jupyter_conf_file)
            sudo('echo "c.NotebookApp.ip = \'*\'" >> ' + jupyter_conf_file)
            sudo('echo c.NotebookApp.open_browser = False >> ' + jupyter_conf_file)
            sudo('echo \'c.NotebookApp.cookie_secret = b"' + id_generator() + '"\' >> ' + jupyter_conf_file)
            sudo('''echo "c.NotebookApp.token = u''" >> ''' + jupyter_conf_file)
            sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> ' + jupyter_conf_file)
            put(templates_dir + 'jupyter-notebook.service', '/tmp/jupyter-notebook.service')
            sudo("chmod 644 /tmp/jupyter-notebook.service")
            sudo("sed -i 's|CONF_PATH|" + jupyter_conf_file + "|' /tmp/jupyter-notebook.service")
            sudo("sed -i 's|OS_USR|" + os_user + "|' /tmp/jupyter-notebook.service")
            sudo('\cp /tmp/jupyter-notebook.service /etc/systemd/system/jupyter-notebook.service')
            sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/.local')
            sudo('mkdir /mnt/var')
            sudo('chown ' + os_user + ':' + os_user + ' /mnt/var')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable jupyter-notebook")
            sudo("systemctl start jupyter-notebook")
            sudo('touch /home/' + os_user + '/.ensure_dir/jupyter_ensured')
        except:
            sys.exit(1)


def ensure_pyspark_local_kernel(os_user, pyspark_local_path_dir, templates_dir, spark_version):
    if not exists('/home/' + os_user + '/.ensure_dir/pyspark_local_kernel_ensured'):
        try:
            sudo('mkdir -p ' + pyspark_local_path_dir)
            sudo('touch ' + pyspark_local_path_dir + 'kernel.json')
            put(templates_dir + 'pyspark_local_template.json', '/tmp/pyspark_local_template.json')
            sudo(
                "PYJ=`find /opt/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; sed -i 's|PY4J|'$PYJ'|g' /tmp/pyspark_local_template.json")
            sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/pyspark_local_template.json')
            sudo('\cp /tmp/pyspark_local_template.json ' + pyspark_local_path_dir + 'kernel.json')
            sudo('touch /home/' + os_user + '/.ensure_dir/pyspark_local_kernel_ensured')
        except:
            sys.exit(1)


def ensure_py3spark_local_kernel(os_user, py3spark_local_path_dir, templates_dir, spark_version):
    if not exists('/home/' + os_user + '/.ensure_dir/py3spark_local_kernel_ensured'):
        try:
            sudo('mkdir -p ' + py3spark_local_path_dir)
            sudo('touch ' + py3spark_local_path_dir + 'kernel.json')
            put(templates_dir + 'py3spark_local_template.json', '/tmp/py3spark_local_template.json')
            sudo(
                "PYJ=`find /opt/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; sed -i 's|PY4J|'$PYJ'|g' /tmp/py3spark_local_template.json")
            sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/py3spark_local_template.json')
            sudo('\cp /tmp/py3spark_local_template.json ' + py3spark_local_path_dir + 'kernel.json')
            sudo('touch /home/' + os_user + '/.ensure_dir/py3spark_local_kernel_ensured')
        except:
            sys.exit(1)
