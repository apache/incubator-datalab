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

from fabric.api import *
from fabric.contrib.files import exists
import logging
import os
import random
import sys
import string
import json, uuid, time, datetime, csv
from dlab.meta_lib import *
from dlab.actions_lib import *
import dlab.actions_lib
import re
import traceback


def ensure_pip(requisites):
    try:
        if not exists('/home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user'])):
            sudo('echo PATH=$PATH:/usr/local/bin/:/opt/spark/bin/ >> /etc/profile')
            sudo('echo export PATH >> /etc/profile')
            sudo('pip install -UI pip=={} --no-cache-dir'.format(os.environ['conf_pip_version']))
            sudo('pip install -U {} --no-cache-dir'.format(requisites))
            sudo('touch /home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user']))
    except:
        sys.exit(1)


def dataengine_dir_prepare(cluster_dir):
    local('mkdir -p ' + cluster_dir)


def install_pip_pkg(requisites, pip_version, lib_group):
    status = list()
    error_parser = "Could not|No matching|ImportError:|failed|EnvironmentError:"
    try:
        if pip_version == 'pip3' and not exists('/bin/pip3'):
            sudo('ln -s /bin/pip3.5 /bin/pip3')
        sudo('{} install -U pip=={} setuptools'.format(pip_version, os.environ['conf_pip_version']))
        sudo('{} install -U pip=={} --no-cache-dir'.format(pip_version, os.environ['conf_pip_version']))
        sudo('{} install --upgrade pip=={}'.format(pip_version, os.environ['conf_pip_version']))
        for pip_pkg in requisites:
            sudo('{0} install {1} --no-cache-dir 2>&1 | if ! grep -w -i -E  "({2})" >  /tmp/{0}install_{1}.log; then  echo "" > /tmp/{0}install_{1}.log;fi'.format(pip_version, pip_pkg, error_parser))
            err = sudo('cat /tmp/{0}install_{1}.log'.format(pip_version, pip_pkg)).replace('"', "'")
            sudo('{0} freeze | if ! grep -w -i {1} > /tmp/{0}install_{1}.list; then  echo "" > /tmp/{0}install_{1}.list;fi'.format(pip_version, pip_pkg))
            res = sudo('cat /tmp/{0}install_{1}.list'.format(pip_version, pip_pkg))
            changed_pip_pkg = False
            if res == '':
                changed_pip_pkg = pip_pkg.replace("_", "-").split('-')
                changed_pip_pkg = changed_pip_pkg[0]
                sudo(
                    '{0} freeze | if ! grep -w -i {1} > /tmp/{0}install_{1}.list; then  echo "" > /tmp/{0}install_{1}.list;fi'.format(
                        pip_version, changed_pip_pkg))
                res = sudo(
                    'cat /tmp/{0}install_{1}.list'.format(pip_version, changed_pip_pkg))
            if res:
                res = res.lower()
                ansi_escape = re.compile(r'\x1b[^m]*m')
                ver = ansi_escape.sub('', res).split("\r\n")
                if changed_pip_pkg:
                    version = [i for i in ver if changed_pip_pkg.lower() in i][0].split('==')[1]
                else:
                    version = \
                    [i for i in ver if pip_pkg.lower() in i][0].split(
                        '==')[1]
                status.append({"group": "{}".format(lib_group), "name": pip_pkg, "version": version, "status": "installed"})
            else:
                status.append({"group": "{}".format(lib_group), "name": pip_pkg, "status": "failed", "error_message": err})
        return status
    except Exception as err:
        append_result("Failed to install {} packages".format(pip_version), str(err))
        print("Failed to install {} packages".format(pip_version))
        sys.exit(1)


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def ensure_dataengine_tensorflow_jars(jars_dir):
    local('wget https://dl.bintray.com/spark-packages/maven/tapanalyticstoolkit/spark-tensorflow-connector/1.0.0-s_2.11/spark-tensorflow-connector-1.0.0-s_2.11.jar \
         -O {}spark-tensorflow-connector-1.0.0-s_2.11.jar'.format(jars_dir))


def prepare(dataengine_service_dir, yarn_dir):
    local('mkdir -p ' + dataengine_service_dir)
    local('mkdir -p ' + yarn_dir)
    local('sudo mkdir -p /opt/python/')
    result = os.path.exists(dataengine_service_dir + 'usr/')
    return result


def configuring_notebook(dataengine_service_version):
    jars_path = '/opt/' + dataengine_service_version + '/jars/'
    local("""sudo bash -c "find """ + jars_path + """ -name '*netty*' | xargs rm -f" """)


def append_result(error, exception=''):
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
    with open('/root/result.json', 'a+') as f:
        text = f.read()
    if len(text) == 0:
        res = '{"error": ""}'
        with open('/root/result.json', 'w') as f:
            f.write(res)
    with open("/root/result.json") as f:
        data = json.load(f)
    if exception:
        data['error'] = data['error'] + " [Error-" + st + "]:" + error + " Exception: " + str(exception)
    else:
        data['error'] = data['error'] + " [Error-" + st + "]:" + error
    with open("/root/result.json", 'w') as f:
        json.dump(data, f)
    print(data)


def put_resource_status(resource, status, dlab_path, os_user, hostname):
    env['connection_attempts'] = 100
    keyfile = os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem"
    env.key_filename = [keyfile]
    env.host_string = os_user + '@' + hostname
    sudo('python ' + dlab_path + 'tmp/resource_status.py --resource {} --status {}'.format(resource, status))


def configure_jupyter(os_user, jupyter_conf_file, templates_dir, jupyter_version, exploratory_name):
    if not exists('/home/' + os_user + '/.ensure_dir/jupyter_ensured'):
        try:
            sudo('pip2 install notebook==5.7.8 --no-cache-dir')
            sudo('pip2 install jupyter --no-cache-dir')
            sudo('pip3.5 install notebook=={} --no-cache-dir'.format(jupyter_version))
            sudo('pip3.5 install jupyter --no-cache-dir')
            sudo('rm -rf {}'.format(jupyter_conf_file))
            run('jupyter notebook --generate-config --config {}'.format(jupyter_conf_file))
            with cd('/home/{}'.format(os_user)):
                run('mkdir -p ~/.jupyter/custom/')
                run('echo "#notebook-container { width: auto; }" > ~/.jupyter/custom/custom.css')
            sudo('echo "c.NotebookApp.ip = \'0.0.0.0\'" >> {}'.format(jupyter_conf_file))
            sudo('echo "c.NotebookApp.base_url = \'/{0}/\'" >> {1}'.format(exploratory_name, jupyter_conf_file))
            sudo('echo c.NotebookApp.open_browser = False >> {}'.format(jupyter_conf_file))
            sudo('echo \'c.NotebookApp.cookie_secret = b"{0}"\' >> {1}'.format(id_generator(), jupyter_conf_file))
            sudo('''echo "c.NotebookApp.token = u''" >> {}'''.format(jupyter_conf_file))
            sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> {}'.format(jupyter_conf_file))
            put(templates_dir + 'jupyter-notebook.service', '/tmp/jupyter-notebook.service')
            sudo("chmod 644 /tmp/jupyter-notebook.service")
            if os.environ['application'] == 'tensor':
                sudo("sed -i '/ExecStart/s|-c \"|-c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64; |g' /tmp/jupyter-notebook.service")
            elif os.environ['application'] == 'deeplearning':
                sudo("sed -i '/ExecStart/s|-c \"|-c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:"
                     "/usr/local/cuda/lib64:/usr/lib64/openmpi/lib: ; export PYTHONPATH=/home/" + os_user +
                     "/caffe/python:/home/" + os_user + "/pytorch/build:$PYTHONPATH ; |g' /tmp/jupyter-notebook.service")
            sudo("sed -i 's|CONF_PATH|{}|' /tmp/jupyter-notebook.service".format(jupyter_conf_file))
            sudo("sed -i 's|OS_USR|{}|' /tmp/jupyter-notebook.service".format(os_user))
            sudo('\cp /tmp/jupyter-notebook.service /etc/systemd/system/jupyter-notebook.service')
            sudo('chown -R {0}:{0} /home/{0}/.local'.format(os_user))
            sudo('mkdir -p /mnt/var')
            sudo('chown {0}:{0} /mnt/var'.format(os_user))
            if os.environ['application'] == 'jupyter':
                sudo('jupyter-kernelspec remove -f python2 || echo "Such kernel doesnt exists"')
                sudo('jupyter-kernelspec remove -f python3 || echo "Such kernel doesnt exists"')
            sudo("systemctl daemon-reload")
            sudo("systemctl enable jupyter-notebook")
            sudo("systemctl start jupyter-notebook")
            sudo('touch /home/{}/.ensure_dir/jupyter_ensured'.format(os_user))
        except:
            sys.exit(1)
    else:
        try:
            sudo(
                'sed -i "s/c.NotebookApp.base_url =.*/c.NotebookApp.base_url = \'\/{0}\/\'/" {1}'.format(exploratory_name, jupyter_conf_file))
            sudo("systemctl restart jupyter-notebook")
        except Exception as err:
            print('Error:', str(err))
            sys.exit(1)

def configure_docker(os_user):
    try:
        if not exists('/home/' + os_user + '/.ensure_dir/docker_ensured'):
            docker_version = os.environ['ssn_docker_version']
            sudo('curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -')
            sudo('add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) \
                  stable"')
            sudo('apt-get update')
            sudo('apt-cache policy docker-ce')
            sudo('apt-get install -y docker-ce={}~ce~3-0~ubuntu'.format(docker_version))
            sudo('touch /home/{}/.ensure_dir/docker_ensured'.format(os_user))
    except Exception as err:
        print('Failed to configure Docker:', str(err))
        sys.exit(1)

def ensure_jupyterlab_files(os_user, jupyterlab_dir, jupyterlab_image, jupyter_conf_file, jupyterlab_conf_file, exploratory_name, edge_ip):
    if not exists(jupyterlab_dir):
        try:
            sudo('mkdir {}'.format(jupyterlab_dir))
#            put(templates_dir + 'pyspark_local_template.json', '/tmp/pyspark_local_template.json')
#            put(templates_dir + 'py3spark_local_template.json', '/tmp/py3spark_local_template.json')
            put('/root/Dockerfile_jupyterlab', '/tmp/Dockerfile_jupyterlab')
            put('/root/scripts/*', '/tmp/')
#            sudo('\cp /tmp/pyspark_local_template.json ' + jupyterlab_dir + 'pyspark_local_template.json')
#            sudo('\cp /tmp/py3spark_local_template.json ' + jupyterlab_dir + 'py3spark_local_template.json')
#            sudo('sed -i \'s/3.5/3.6/g\' {}py3spark_local_template.json'.format(jupyterlab_dir))
            sudo('mv /tmp/jupyterlab_run.sh {}jupyterlab_run.sh'.format(jupyterlab_dir))
            sudo('mv /tmp/Dockerfile_jupyterlab {}Dockerfile_jupyterlab'.format(jupyterlab_dir))
            sudo('mv /tmp/build.sh {}build.sh'.format(jupyterlab_dir))
            sudo('mv /tmp/start.sh {}start.sh'.format(jupyterlab_dir))
            sudo('sed -i \'s/nb_user/{}/g\' {}Dockerfile_jupyterlab'.format(os_user, jupyterlab_dir))
            sudo('sed -i \'s/jupyterlab_image/{}/g\' {}Dockerfile_jupyterlab'.format(jupyterlab_image, jupyterlab_dir))
            sudo('sed -i \'s/nb_user/{}/g\' {}start.sh'.format(os_user, jupyterlab_dir))
#            sudo('sed -i \'s/jup_version/{}/g\' {}Dockerfile_jupyterlab'.format(jupyter_version, jupyterlab_dir))
#            sudo('sed -i \'s/hadoop_version/{}/g\' {}Dockerfile_jupyterlab'.format(os.environ['notebook_hadoop_version'], jupyterlab_dir))
#            sudo('sed -i \'s/tornado_version/{}/g\' {}Dockerfile_jupyterlab'.format(os.environ['notebook_tornado_version'], jupyterlab_dir))
#            sudo('sed -i \'s/matplotlib_version/{}/g\' {}Dockerfile_jupyterlab'.format(os.environ['notebook_matplotlib_version'], jupyterlab_dir))
#            sudo('sed -i \'s/numpy_version/{}/g\' {}Dockerfile_jupyterlab'.format(os.environ['notebook_numpy_version'], jupyterlab_dir))
#            sudo('sed -i \'s/spark_version/{}/g\' {}Dockerfile_jupyterlab'.format(os.environ['notebook_spark_version'], jupyterlab_dir))
#            sudo('sed -i \'s/scala_version/{}/g\' {}Dockerfile_jupyterlab'.format(os.environ['notebook_scala_version'], jupyterlab_dir))
            sudo('sed -i \'s/CONF_PATH/{}/g\' {}jupyterlab_run.sh'.format(jupyterlab_conf_file, jupyterlab_dir))
            sudo('touch {}'.format(jupyter_conf_file))
            sudo('echo "c.NotebookApp.ip = \'0.0.0.0\'" >> {}'.format(jupyter_conf_file))
            sudo('echo "c.NotebookApp.base_url = \'/{0}/\'" >> {1}'.format(exploratory_name, jupyter_conf_file))
            sudo('echo c.NotebookApp.open_browser = False >> {}'.format(jupyter_conf_file))
            sudo('echo \'c.NotebookApp.cookie_secret = b"{0}"\' >> {1}'.format(id_generator(), jupyter_conf_file))
            sudo('''echo "c.NotebookApp.token = u''" >> {}'''.format(jupyter_conf_file))
            sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> {}'.format(jupyter_conf_file))
            sudo('chown dlab-user:dlab-user /opt')
            sudo('echo -e "Host git.epam.com\n   HostName git.epam.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p\n" > /home/{}/.ssh/config'.format(edge_ip, os_user))
            sudo('echo -e "Host github.com\n   HostName github.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p" >> /home/{}/.ssh/config'.format(edge_ip, os_user))
#            sudo('touch {}'.format(spark_script))
#            sudo('echo "#!/bin/bash" >> {}'.format(spark_script))
#            sudo(
#                'echo "PYJ=\`find /opt/spark/ -name \'*py4j*.zip\' | tr \'\\n\' \':\' | sed \'s|:$||g\'\`; sed -i \'s|PY4J|\'$PYJ\'|g\' /tmp/pyspark_local_template.json" >> {}'.format(
#                spark_script))
#            sudo(
#                'echo "sed -i \'14s/:",/:\\/home\\/dlab-user\\/caffe\\/python:\\/home\\/dlab-user\\/pytorch\\/build:",/\' /tmp/pyspark_local_template.json" >> {}'.format(
#                    spark_script))
#            sudo('echo \'sed -i "s|SP_VER|{}|g" /tmp/pyspark_local_template.json\' >> {}'.format(os.environ['notebook_spark_version'], spark_script))
#            sudo(
#                'echo "PYJ=\`find /opt/spark/ -name \'*py4j*.zip\' | tr \'\\n\' \':\' | sed \'s|:$||g\'\`; sed -i \'s|PY4J|\'$PYJ\'|g\' /tmp/py3spark_local_template.json" >> {}'.format(
#                spark_script))
#            sudo(
#                'echo "sed -i \'14s/:",/:\\/home\\/dlab-user\\/caffe\\/python:\\/home\\/dlab-user\\/pytorch\\/build:",/\' /tmp/py3spark_local_template.json" >> {}'.format(
#                    spark_script))
#            sudo('echo \'sed -i "s|SP_VER|{}|g" /tmp/py3spark_local_template.json\' >> {}'.format(os.environ['notebook_spark_version'], spark_script))
#            sudo('echo "cp /tmp/pyspark_local_template.json /home/{}/.local/share/jupyter/kernels/pyspark_local/kernel.json" >> {}'.format(os_user, spark_script))
#            sudo(
#                'echo "cp /tmp/py3spark_local_template.json /home/{}/.local/share/jupyter/kernels/py3spark_local/kernel.json" >> {}'.format(
#                    os_user, spark_script))
#            sudo('git clone https://github.com/legion-platform/legion.git')
#            sudo('cp {}sdk/Pipfile {}sdk_Pipfile'.format(legion_dir, jupyterlab_dir))
#            sudo('cp {}sdk/Pipfile.lock {}sdk_Pipfile.lock'.format(legion_dir, jupyterlab_dir))
#            sudo('cp {}toolchains/python/Pipfile {}toolchains_Pipfile'.format(legion_dir, jupyterlab_dir))
#            sudo('cp {}toolchains/python/Pipfile.lock {}toolchains_Pipfile.lock'.format(legion_dir, jupyterlab_dir))
#            sudo('cp {}cli/Pipfile {}cli_Pipfile'.format(legion_dir, jupyterlab_dir))
#            sudo('cp {}cli/Pipfile.lock {}cli_Pipfile.lock'.format(legion_dir, jupyterlab_dir))
#            sudo('cp -r {}sdk {}sdk'.format(legion_dir, jupyterlab_dir))
#            sudo('cp -r {}toolchains/python {}toolchains_python'.format(legion_dir, jupyterlab_dir))
#            sudo('cp -r {}cli {}cli'.format(legion_dir, jupyterlab_dir))
        except:
           sys.exit(1)
    else:
        try:
            sudo(
                'sed -i "s/c.NotebookApp.base_url =.*/c.NotebookApp.base_url = \'\/{0}\/\'/" {1}'.format(
                    exploratory_name, jupyter_conf_file))
        except Exception as err:
            print('Error:', str(err))
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
            sudo('sed -i \'/PYTHONPATH\"\:/s|\(.*\)"|\\1/home/{0}/caffe/python:/home/{0}/pytorch/build:"|\' /tmp/pyspark_local_template.json'.format(os_user))
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
            sudo('sed -i \'/PYTHONPATH\"\:/s|\(.*\)"|\\1/home/{0}/caffe/python:/home/{0}/pytorch/build:"|\' /tmp/py3spark_local_template.json'.format(os_user))
            sudo('\cp /tmp/py3spark_local_template.json ' + py3spark_local_path_dir + 'kernel.json')
            sudo('touch /home/' + os_user + '/.ensure_dir/py3spark_local_kernel_ensured')
        except:
            sys.exit(1)


def pyspark_kernel(kernels_dir, dataengine_service_version, cluster_name, spark_version, bucket, user_name, region, os_user='',
                   application='', pip_mirror='', numpy_version='1.14.3'):
    spark_path = '/opt/{0}/{1}/spark/'.format(dataengine_service_version, cluster_name)
    local('mkdir -p {0}pyspark_{1}/'.format(kernels_dir, cluster_name))
    kernel_path = '{0}pyspark_{1}/kernel.json'.format(kernels_dir, cluster_name)
    template_file = "/tmp/pyspark_dataengine-service_template.json"
    with open(template_file, 'r') as f:
        text = f.read()
    text = text.replace('CLUSTER_NAME', cluster_name)
    text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
    text = text.replace('SPARK_PATH', spark_path)
    text = text.replace('PYTHON_SHORT_VERSION', '2.7')
    text = text.replace('PYTHON_FULL_VERSION', '2.7')
    text = text.replace('PYTHON_PATH', '/usr/bin/python2.7')
    text = text.replace('DATAENGINE-SERVICE_VERSION', dataengine_service_version)
    with open(kernel_path, 'w') as f:
        f.write(text)
    local('touch /tmp/kernel_var.json')
    local("PYJ=`find /opt/{0}/{1}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {2} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{3}/caffe/python:/home/{3}/pytorch/build:\"|\' > /tmp/kernel_var.json".
          format(dataengine_service_version, cluster_name, kernel_path, os_user))
    local('sudo mv /tmp/kernel_var.json ' + kernel_path)
    get_cluster_python_version(region, bucket, user_name, cluster_name)
    with file('/tmp/python_version') as f:
        python_version = f.read()
    if python_version != '\n':
        installing_python(region, bucket, user_name, cluster_name, application, pip_mirror, numpy_version)
        local('mkdir -p {0}py3spark_{1}/'.format(kernels_dir, cluster_name))
        kernel_path = '{0}py3spark_{1}/kernel.json'.format(kernels_dir, cluster_name)
        template_file = "/tmp/pyspark_dataengine-service_template.json"
        with open(template_file, 'r') as f:
            text = f.read()
        text = text.replace('CLUSTER_NAME', cluster_name)
        text = text.replace('SPARK_VERSION', 'Spark-' + spark_version)
        text = text.replace('SPARK_PATH', spark_path)
        text = text.replace('PYTHON_SHORT_VERSION', python_version[0:3])
        text = text.replace('PYTHON_FULL_VERSION', python_version[0:3])
        text = text.replace('PYTHON_PATH', '/opt/python/python' + python_version[:5] + '/bin/python' +
                            python_version[:3])
        text = text.replace('DATAENGINE-SERVICE_VERSION', dataengine_service_version)
        with open(kernel_path, 'w') as f:
            f.write(text)
        local('touch /tmp/kernel_var.json')
        local("PYJ=`find /opt/{0}/{1}/spark/ -name '*py4j*.zip' | tr '\\n' ':' | sed 's|:$||g'`; cat {2} | sed 's|PY4J|'$PYJ'|g' | sed \'/PYTHONPATH\"\:/s|\(.*\)\"|\\1/home/{3}/caffe/python:/home/{3}/pytorch/build:\"|\' > /tmp/kernel_var.json"
              .format(dataengine_service_version, cluster_name, kernel_path, os_user))
        local('sudo mv /tmp/kernel_var.json {}'.format(kernel_path))


def ensure_ciphers():
    try:
        sudo('echo -e "\nKexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256" >> /etc/ssh/sshd_config')
        sudo('echo -e "Ciphers aes256-gcm@openssh.com,aes128-gcm@openssh.com,chacha20-poly1305@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr" >> /etc/ssh/sshd_config')
        sudo('echo -e "\tKexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256" >> /etc/ssh/ssh_config')
        sudo('echo -e "\tCiphers aes256-gcm@openssh.com,aes128-gcm@openssh.com,chacha20-poly1305@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr" >> /etc/ssh/ssh_config')
        try:
            sudo('service ssh reload')
        except:
            sudo('service sshd reload')
    except Exception as err:
        traceback.print_exc()
        print('Failed to ensure ciphers: ', str(err))
        sys.exit(1)


def install_r_pkg(requisites):
    status = list()
    error_parser = "ERROR:|error:|Cannot|failed|Please run|requires"
    try:
        for r_pkg in requisites:
            if r_pkg == 'sparklyr':
                run('sudo -i R -e \'install.packages("{0}", repos="https://cloud.r-project.org", dep=TRUE)\' 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E  "({1})" /tmp/tee.tmp > /tmp/install_{0}.log; then  echo "" > /tmp/install_{0}.log;fi'.format(r_pkg, error_parser))
            sudo('R -e \'install.packages("{0}", repos="https://cloud.r-project.org", dep=TRUE)\' 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E  "({1})" /tmp/tee.tmp >  /tmp/install_{0}.log; then  echo "" > /tmp/install_{0}.log;fi'.format(r_pkg, error_parser))
            err = sudo('cat /tmp/install_{0}.log'.format(r_pkg)).replace('"', "'")
            sudo('R -e \'installed.packages()[,c(3:4)]\' | if ! grep -w {0} > /tmp/install_{0}.list; then  echo "" > /tmp/install_{0}.list;fi'.format(r_pkg))
            res = sudo('cat /tmp/install_{0}.list'.format(r_pkg))
            if res:
                ansi_escape = re.compile(r'\x1b[^m]*m')
                version = ansi_escape.sub('', res).split("\r\n")[0].split('"')[1]
                status.append({"group": "r_pkg", "name": r_pkg, "version": version, "status": "installed"})
            else:
                status.append({"group": "r_pkg", "name": r_pkg, "status": "failed", "error_message": err})
        return status
    except:
        return "Fail to install R packages"


def update_spark_jars(jars_dir='/opt/jars'):
    try:
        configs = sudo('find /opt/ /etc/ /usr/lib/ -name spark-defaults.conf -type f').split('\r\n')
        if exists(jars_dir):
            for conf in filter(None, configs):
                des_path = ''
                all_jars = sudo('find {0} -name "*.jar"'.format(jars_dir)).split('\r\n')
                if ('-des-' in conf):
                    des_path = '/'.join(conf.split('/')[:3])
                    all_jars = find_des_jars(all_jars, des_path)
                sudo('''sed -i '/^# Generated\|^spark.jars/d' {0}'''.format(conf))
                sudo('echo "# Generated spark.jars by DLab from {0}\nspark.jars {1}" >> {2}'
                     .format(','.join(filter(None, [jars_dir, des_path])), ','.join(all_jars), conf))
                # sudo("sed -i 's/^[[:space:]]*//' {0}".format(conf))
        else:
            print("Can't find directory {0} with jar files".format(jars_dir))
    except Exception as err:
        append_result("Failed to update spark.jars parameter", str(err))
        print("Failed to update spark.jars parameter")
        sys.exit(1)


def install_java_pkg(requisites):
    status = list()
    error_parser = "ERROR|error|No such|no such|Please run|requires|module not found"
    templates_dir = '/root/templates/'
    ivy_dir = '/opt/ivy'
    ivy_cache_dir = '{0}/cache/'.format(ivy_dir)
    ivy_settings = 'ivysettings.xml'
    dest_dir = '/opt/jars/java'
    try:
        ivy_jar = sudo('find /opt /usr -name "*ivy-{0}.jar" | head -n 1'.format(os.environ['notebook_ivy_version']))
        sudo('mkdir -p {0} {1}'.format(ivy_dir, dest_dir))
        put('{0}{1}'.format(templates_dir, ivy_settings), '{0}/{1}'.format(ivy_dir, ivy_settings), use_sudo=True)
        proxy_string = sudo('cat /etc/profile | grep http_proxy | cut -f2 -d"="')
        proxy_re = '(?P<proto>http.*)://(?P<host>[^:/ ]+):(?P<port>[0-9]*)'
        proxy_find = re.search(proxy_re, proxy_string)
        java_proxy = "export _JAVA_OPTIONS='-Dhttp.proxyHost={0} -Dhttp.proxyPort={1} \
            -Dhttps.proxyHost={0} -Dhttps.proxyPort={1}'".format(proxy_find.group('host'), proxy_find.group('port'))
        for java_pkg in requisites:
            sudo('rm -rf {0}'.format(ivy_cache_dir))
            sudo('mkdir -p {0}'.format(ivy_cache_dir))
            group, artifact, version, override = java_pkg
            print("Installing package (override: {3}): {0}:{1}:{2}".format(group, artifact, version, override))
            sudo('{8}; java -jar {0} -settings {1}/{2} -cache {3} -dependency {4} {5} {6} 2>&1 | tee /tmp/tee.tmp; \
                if ! grep -w -E  "({7})" /tmp/tee.tmp > /tmp/install_{5}.log; then echo "" > /tmp/install_{5}.log;fi'
                 .format(ivy_jar, ivy_dir, ivy_settings, ivy_cache_dir, group, artifact, version, error_parser, java_proxy))
            err = sudo('cat /tmp/install_{0}.log'.format(artifact)).replace('"', "'").strip()
            sudo('find {0} -name "{1}*.jar" | head -n 1 | rev | cut -f1 -d "/" | rev | \
                if ! grep -w -i {1} > /tmp/install_{1}.list; then echo "" > /tmp/install_{1}.list;fi'.format(ivy_cache_dir, artifact))
            res = sudo('cat /tmp/install_{0}.list'.format(artifact))
            if res:
                sudo('cp -f $(find {0} -name "*.jar" | xargs) {1}'.format(ivy_cache_dir, dest_dir))
                status.append({"group": "java", "name": "{0}:{1}".format(group, artifact), "version": version, "status": "installed"})
            else:
                status.append({"group": "java", "name": "{0}:{1}".format(group, artifact), "status": "failed", "error_message": err})
        update_spark_jars()
        return status
    except Exception as err:
        append_result("Failed to install {} packages".format(requisites), str(err))
        print("Failed to install {} packages".format(requisites))
        sys.exit(1)


def get_available_r_pkgs():
    try:
        r_pkgs = dict()
        sudo('R -e \'write.table(available.packages(contriburl="https://cloud.r-project.org/src/contrib"), file="/tmp/r.csv", row.names=F, col.names=F, sep=",")\'')
        get("/tmp/r.csv", "r.csv")
        with open('r.csv', 'rb') as csvfile:
            reader = csv.reader(csvfile, delimiter=',')
            for row in reader:
                r_pkgs[row[0]] = row[1]
        return r_pkgs
    except:
        sys.exit(1)


def ensure_toree_local_kernel(os_user, toree_link, scala_kernel_path, files_dir, scala_version, spark_version):
    if not exists('/home/' + os_user + '/.ensure_dir/toree_local_kernel_ensured'):
        try:
            sudo('pip install ' + toree_link + ' --no-cache-dir')
            sudo('ln -s /opt/spark/ /usr/local/spark')
            sudo('jupyter toree install')
            sudo('mv ' + scala_kernel_path + 'lib/* /tmp/')
            put(files_dir + 'toree-assembly-0.2.0.jar', '/tmp/toree-assembly-0.2.0.jar')
            sudo('mv /tmp/toree-assembly-0.2.0.jar ' + scala_kernel_path + 'lib/')
            sudo(
                'sed -i "s|Apache Toree - Scala|Local Apache Toree - Scala (Scala-' + scala_version +
                ', Spark-' + spark_version + ')|g" ' + scala_kernel_path + 'kernel.json')
            sudo('touch /home/' + os_user + '/.ensure_dir/toree_local_kernel_ensured')
        except:
            sys.exit(1)


def install_ungit(os_user, notebook_name, edge_ip):
    if not exists('/home/{}/.ensure_dir/ungit_ensured'.format(os_user)):
        try:
            sudo('npm -g install ungit@{}'.format(os.environ['notebook_ungit_version']))
            put('/root/templates/ungit.service', '/tmp/ungit.service')
            sudo("sed -i 's|OS_USR|{}|' /tmp/ungit.service".format(os_user))
            http_proxy = run('echo $http_proxy')
            sudo("sed -i 's|PROXY_HOST|{}|g' /tmp/ungit.service".format(http_proxy))
            sudo("sed -i 's|NOTEBOOK_NAME|{}|' /tmp/ungit.service".format(
                notebook_name))
            sudo("mv -f /tmp/ungit.service /etc/systemd/system/ungit.service")
            run('git config --global user.name "Example User"')
            run('git config --global user.email "example@example.com"')
            run('mkdir -p ~/.git/templates/hooks')
            put('/root/scripts/git_pre_commit.py', '~/.git/templates/hooks/pre-commit', mode=0755)
            run('git config --global init.templatedir ~/.git/templates')
            run('touch ~/.gitignore')
            run('git config --global core.excludesfile ~/.gitignore')
            run('echo ".ipynb_checkpoints/" >> ~/.gitignore')
            run('echo "spark-warehouse/" >> ~/.gitignore')
            run('echo "metastore_db/" >> ~/.gitignore')
            run('echo "derby.log" >> ~/.gitignore')
            sudo(
                'echo -e "Host git.epam.com\n   HostName git.epam.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p\n" > /home/{}/.ssh/config'.format(
                    edge_ip, os_user))
            sudo(
                'echo -e "Host github.com\n   HostName github.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p" >> /home/{}/.ssh/config'.format(
                    edge_ip, os_user))
            sudo(
                'echo -e "Host gitlab.com\n   HostName gitlab.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p" >> /home/{}/.ssh/config'.format(
                    edge_ip, os_user))
            sudo('systemctl daemon-reload')
            sudo('systemctl enable ungit.service')
            sudo('systemctl start ungit.service')
            sudo('touch /home/{}/.ensure_dir/ungit_ensured'.format(os_user))
        except:
            sys.exit(1)
    else:
        try:
            sudo("sed -i 's|--rootPath=/.*-ungit|--rootPath=/{}-ungit|' /etc/systemd/system/ungit.service".format(
                notebook_name))
            http_proxy = run('echo $http_proxy')
            sudo("sed -i 's|HTTPS_PROXY=.*3128|HTTPS_PROXY={}|g' /etc/systemd/system/ungit.service".format(http_proxy))
            sudo("sed -i 's|HTTP_PROXY=.*3128|HTTP_PROXY={}|g' /etc/systemd/system/ungit.service".format(http_proxy))
            sudo('systemctl daemon-reload')
            sudo('systemctl restart ungit.service')
        except:
            sys.exit(1)
    run('git config --global http.proxy $http_proxy')
    run('git config --global https.proxy $https_proxy')


def install_inactivity_checker(os_user, ip_adress, rstudio=False):
    if not exists('/home/{}/.ensure_dir/inactivity_ensured'.format(os_user)):
        try:
            if not exists('/opt/inactivity'):
                sudo('mkdir /opt/inactivity')
            put('/root/templates/inactive.service', '/etc/systemd/system/inactive.service', use_sudo=True)
            put('/root/templates/inactive.timer', '/etc/systemd/system/inactive.timer', use_sudo=True)
            if rstudio:
                put('/root/templates/inactive_rs.sh', '/opt/inactivity/inactive.sh', use_sudo=True)
            else:
                put('/root/templates/inactive.sh', '/opt/inactivity/inactive.sh', use_sudo=True)
            sudo("sed -i 's|IP_ADRESS|{}|g' /opt/inactivity/inactive.sh".format(ip_adress))
            sudo("chmod 755 /opt/inactivity/inactive.sh")
            sudo("chown root:root /etc/systemd/system/inactive.service")
            sudo("chown root:root /etc/systemd/system/inactive.timer")
            sudo("date +%s > /opt/inactivity/local_inactivity")
            sudo('systemctl daemon-reload')
            sudo('systemctl enable inactive.timer')
            sudo('systemctl start inactive.timer')
            sudo('touch /home/{}/.ensure_dir/inactive_ensured'.format(os_user))
        except Exception as err:
            print('Failed to setup inactivity check service!', str(err))
            sys.exit(1)


def set_git_proxy(os_user, hostname, keyfile, proxy_host):
    env['connection_attempts'] = 100
    env.key_filename = [keyfile]
    env.host_string = os_user + '@' + hostname
    run('git config --global http.proxy {}'.format(proxy_host))
    run('git config --global https.proxy {}'.format(proxy_host))


def set_mongo_parameters(client, mongo_parameters):
    for i in mongo_parameters:
        client.dlabdb.settings.insert_one({"_id": i, "value": mongo_parameters[i]})


def install_r_packages(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/r_packages_ensured'):
        sudo('R -e "install.packages(\'devtools\', repos = \'https://cloud.r-project.org\')"')
        sudo('R -e "install.packages(\'knitr\', repos = \'https://cloud.r-project.org\')"')
        sudo('R -e "install.packages(\'ggplot2\', repos = \'https://cloud.r-project.org\')"')
        sudo('R -e "install.packages(c(\'devtools\',\'mplot\', \'googleVis\'), '
             'repos = \'https://cloud.r-project.org\'); require(devtools); install_github(\'ramnathv/rCharts\')"')
        sudo('touch /home/' + os_user + '/.ensure_dir/r_packages_ensured')


def add_breeze_library_local(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/breeze_local_ensured'):
        try:
            breeze_tmp_dir = '/tmp/breeze_tmp_local/'
            jars_dir = '/opt/jars/'
            sudo('mkdir -p {}'.format(breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze_{0}/{1}/breeze_{0}-{1}.jar -O \
                    {2}breeze_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-natives_{0}/{1}/breeze-natives_{0}-{1}.jar -O \
                    {2}breeze-natives_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-viz_{0}/{1}/breeze-viz_{0}-{1}.jar -O \
                    {2}breeze-viz_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-macros_{0}/{1}/breeze-macros_{0}-{1}.jar -O \
                    {2}breeze-macros_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-parent_{0}/{1}/breeze-parent_{0}-{1}.jar -O \
                    {2}breeze-parent_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/jfree/jfreechart/{0}/jfreechart-{0}.jar -O \
                    {1}jfreechart-{0}.jar'.format('1.0.19', breeze_tmp_dir))
            sudo('wget https://repo1.maven.org/maven2/org/jfree/jcommon/{0}/jcommon-{0}.jar -O \
                    {1}jcommon-{0}.jar'.format('1.0.24', breeze_tmp_dir))
            sudo('wget --no-check-certificate https://brunelvis.org/jar/spark-kernel-brunel-all-{0}.jar -O \
                    {1}spark-kernel-brunel-all-{0}.jar'.format('2.3', breeze_tmp_dir))
            sudo('mv {0}* {1}'.format(breeze_tmp_dir, jars_dir))
            sudo('touch /home/' + os_user + '/.ensure_dir/breeze_local_ensured')
        except:
            sys.exit(1)


def configure_data_engine_service_pip(hostname, os_user, keyfile):
    env['connection_attempts'] = 100
    env.key_filename = [keyfile]
    env.host_string = os_user + '@' + hostname
    if not exists('/usr/bin/pip2'):
        sudo('ln -s /usr/bin/pip-2.7 /usr/bin/pip2')
    if not exists('/usr/bin/pip3') and sudo("python3.4 -V 2>/dev/null | awk '{print $2}'"):
        sudo('ln -s /usr/bin/pip-3.4 /usr/bin/pip3')
    elif not exists('/usr/bin/pip3') and sudo("python3.5 -V 2>/dev/null | awk '{print $2}'"):
        sudo('ln -s /usr/bin/pip-3.5 /usr/bin/pip3')
    elif not exists('/usr/bin/pip3') and sudo("python3.6 -V 2>/dev/null | awk '{print $2}'"):
        sudo('ln -s /usr/bin/pip-3.6 /usr/bin/pip3')
    sudo('echo "export PATH=$PATH:/usr/local/bin" >> /etc/profile')
    sudo('source /etc/profile')
    run('source /etc/profile')


def remove_rstudio_dataengines_kernel(cluster_name, os_user):
    try:
        cluster_re = ['-{}"'.format(cluster_name),
                      '-{}-'.format(cluster_name),
                      '-{}/'.format(cluster_name)]
        get('/home/{}/.Rprofile'.format(os_user), 'Rprofile')
        data = open('Rprofile').read()
        conf = filter(None, data.split('\n'))
        # Filter config from any math of cluster_name in line,
        # separated by defined symbols to avoid partly matches
        conf = [i for i in conf if not any(x in i for x in cluster_re)]
        comment_all = lambda x: x if x.startswith('#master') else '#{}'.format(x)
        uncomment = lambda x: x[1:] if not x.startswith('#master') else x
        conf =[comment_all(i) for i in conf]
        conf =[uncomment(i) for i in conf]
        last_spark = max([conf.index(i) for i in conf if 'master=' in i] or [0])
        active_cluster = conf[last_spark].split('"')[-2] if last_spark != 0 else None
        conf = conf[:last_spark] + [conf[l][1:] for l in range(last_spark, len(conf)) if conf[l].startswith("#")] \
                                 + [conf[l] for l in range(last_spark, len(conf)) if not conf[l].startswith('#')]
        with open('.Rprofile', 'w') as f:
            for line in conf:
                f.write('{}\n'.format(line))
        put('.Rprofile', '/home/{}/.Rprofile'.format(os_user))
        get('/home/{}/.Renviron'.format(os_user), 'Renviron')
        data = open('Renviron').read()
        conf = filter(None, data.split('\n'))
        comment_all = lambda x: x if x.startswith('#') else '#{}'.format(x)
        conf = [comment_all(i) for i in conf]
        # Filter config from any math of cluster_name in line,
        # separated by defined symbols to avoid partly matches
        conf = [i for i in conf if not any(x in i for x in cluster_re)]
        if active_cluster:
            activate_cluster = lambda x: x[1:] if active_cluster in x else x
            conf = [activate_cluster(i) for i in conf]
        else:
            last_spark = max([conf.index(i) for i in conf if 'SPARK_HOME' in i])
            conf = conf[:last_spark] + [conf[l][1:] for l in range(last_spark, len(conf)) if conf[l].startswith("#")]
        with open('.Renviron', 'w') as f:
            for line in conf:
                f.write('{}\n'.format(line))
        put('.Renviron', '/home/{}/.Renviron'.format(os_user))
        if len(conf) == 1:
           sudo('rm -f /home/{}/.ensure_dir/rstudio_dataengine_ensured'.format(os_user))
           sudo('rm -f /home/{}/.ensure_dir/rstudio_dataengine-service_ensured'.format(os_user))
        sudo('''R -e "source('/home/{}/.Rprofile')"'''.format(os_user))
    except:
        sys.exit(1)


def restart_zeppelin(creds=False, os_user='', hostname='', keyfile=''):
    if creds:
        env['connection_attempts'] = 100
        env.key_filename = [keyfile]
        env.host_string = os_user + '@' + hostname
    sudo("systemctl daemon-reload")
    sudo("systemctl restart zeppelin-notebook")


def get_spark_memory(creds=False, os_user='', hostname='', keyfile=''):
    if creds:
        with settings(host_string='{}@{}'.format(os_user, hostname)):
            mem = sudo('free -m | grep Mem | tr -s " " ":" | cut -f 2 -d ":"')
            instance_memory = int(mem)
    else:
        mem = sudo('free -m | grep Mem | tr -s " " ":" | cut -f 2 -d ":"')
        instance_memory = int(mem)
    try:
        if instance_memory > int(os.environ['dataengine_expl_instance_memory']):
            spark_memory = instance_memory - int(os.environ['dataengine_os_expl_memory'])
        else:
            spark_memory = instance_memory * int(os.environ['dataengine_os_memory']) / 100
        return spark_memory
    except Exception as err:
        print('Error:', str(err))
        return err


def replace_multi_symbols(string, symbol, symbol_cut=False):
    try:
        symbol_amount = 0
        for i in range(len(string)):
            if string[i] == symbol:
                symbol_amount = symbol_amount + 1
        while symbol_amount > 1:
            string = string.replace(symbol + symbol, symbol)
            symbol_amount = symbol_amount - 1
        if symbol_cut and string[-1] == symbol:
            string = string[:-1]
        return string
    except Exception as err:
        logging.info("Error with replacing multi symbols: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with replacing multi symbols",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


def update_pyopenssl_lib(os_user):
    if not exists('/home/{}/.ensure_dir/pyopenssl_updated'.format(os_user)):
        try:
            if exists('/usr/bin/pip3'):
                sudo('pip3 install -U pyopenssl')
            sudo('pip2 install -U pyopenssl')
            sudo('touch /home/{}/.ensure_dir/pyopenssl_updated'.format(os_user))
        except:
            sys.exit(1)


def find_cluster_kernels():
    try:
        with settings(sudo_user='root'):
            de = [i for i in sudo('find /opt/ -maxdepth 1 -name "*-de-*" -type d | rev | '
                                  'cut -f 1 -d "/" | rev | xargs -r').split(' ') if i != '']
            des =  [i for i in sudo('find /opt/ -maxdepth 2 -name "*-des-*" -type d | rev | '
                                    'cut -f 1,2 -d "/" | rev | xargs -r').split(' ') if i != '']
        return (de, des)
    except:
        sys.exit(1)


def update_zeppelin_interpreters(multiple_clusters, r_enabled, interpreter_mode='remote'):
    try:
        interpreters_config = '/opt/zeppelin/conf/interpreter.json'
        local_interpreters_config = '/tmp/interpreter.json'
        if interpreter_mode != 'remote':
            get(local_interpreters_config, local_interpreters_config)
        if multiple_clusters == 'true':
            groups = [{"class": "org.apache.zeppelin.livy.LivySparkInterpreter", "name": "spark"},
                      {"class": "org.apache.zeppelin.livy.LivyPySparkInterpreter", "name": "pyspark"},
                      {"class": "org.apache.zeppelin.livy.LivyPySpark3Interpreter", "name": "pyspark3"},
                      {"class": "org.apache.zeppelin.livy.LivySparkSQLInterpreter", "name": "sql"}]
            if r_enabled:
                groups.append({"class": "org.apache.zeppelin.livy.LivySparkRInterpreter", "name": "sparkr"})
        else:
            groups = [{"class": "org.apache.zeppelin.spark.SparkInterpreter","name": "spark"},
                      {"class": "org.apache.zeppelin.spark.PySparkInterpreter", "name": "pyspark"},
                      {"class": "org.apache.zeppelin.spark.SparkSqlInterpreter", "name": "sql"}]
            if r_enabled:
                groups.append({"class": "org.apache.zeppelin.spark.SparkRInterpreter", "name": "r"})
        r_conf = {"zeppelin.R.knitr": "true", "zeppelin.R.image.width": "100%", "zeppelin.R.cmd": "R",
                  "zeppelin.R.render.options": "out.format = 'html', comment = NA, echo = FALSE, results = 'asis', message = F, warning = F"}
        if interpreter_mode != 'remote':
            data = json.loads(open(local_interpreters_config).read())
        else:
            data = json.loads(open(interpreters_config).read())
        for i in data['interpreterSettings'].keys():
            if data['interpreterSettings'][i]['group'] == 'md':
                continue
            elif data['interpreterSettings'][i]['group'] == 'sh':
                continue
            if r_enabled == 'true':
                data['interpreterSettings'][i]['properties'].update(r_conf)
            data['interpreterSettings'][i]['interpreterGroup'] = groups

        if interpreter_mode != 'remote':
            with open(local_interpreters_config, 'w') as f:
                f.write(json.dumps(data, indent=2))
            put(local_interpreters_config, local_interpreters_config)
            sudo('cp -f {0} {1}'.format(local_interpreters_config, interpreters_config))
            sudo('systemctl restart zeppelin-notebook')
        else:
            with open(interpreters_config, 'w') as f:
                f.write(json.dumps(data, indent=2))
            local('sudo systemctl restart zeppelin-notebook')
    except Exception as err:
        print('Failed to update Zeppelin interpreters', str(err))
        sys.exit(1)


def update_hosts_file(os_user):
    try:
        if not exists('/home/{}/.ensure_dir/hosts_file_updated'.format(os_user)):
            sudo('sed -i "s/^127.0.0.1 localhost/127.0.0.1 localhost localhost.localdomain/g" /etc/hosts')
            sudo('touch /home/{}/.ensure_dir/hosts_file_updated'.format(os_user))
    except Exception as err:
        print('Failed to update hosts file', str(err))
        sys.exit(1)

def ensure_docker_compose(os_user):
    try:
        configure_docker(os_user)
        if not exists('/home/{}/.ensure_dir/docker_compose_ensured'.format(os_user)):
            docker_compose_version = "1.24.1"
            sudo('curl -L https://github.com/docker/compose/releases/download/{}/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose'.format(docker_compose_version))
            sudo('chmod +x /usr/local/bin/docker-compose')
            sudo('touch /home/{}/.ensure_dir/docker_compose_ensured'.format(os_user))
        sudo('systemctl daemon-reload')
        sudo('systemctl restart docker')
        return True
    except:
        return False

def configure_superset(os_user, keycloak_auth_server_url, keycloak_realm_name, keycloak_client_id, keycloak_client_secret, edge_instance_private_ip, edge_instance_public_ip, superset_name):
    print('Superset configuring')
    try:
        if not exists('/home/{}/incubator-superset'.format(os_user)):
            with cd('/home/{}'.format(os_user)):
                sudo('wget https://github.com/apache/incubator-superset/archive/{}.tar.gz'.format(os.environ['notebook_superset_version']))
                sudo('tar -xzf {}.tar.gz'.format(os.environ['notebook_superset_version']))
                sudo('ln -sf incubator-superset-{} incubator-superset'.format(os.environ['notebook_superset_version']))
        if not exists('/tmp/superset-notebook_installed'):
            sudo('mkdir -p /opt/dlab/templates')
            put('/root/templates', '/opt/dlab', use_sudo=True)
            sudo('sed -i \'s/OS_USER/{}/g\' /opt/dlab/templates/.env'.format(os_user))
            proxy_string = '{}:3128'.format(edge_instance_private_ip)
            sudo('sed -i \'s|KEYCLOAK_AUTH_SERVER_URL|{}|g\' /opt/dlab/templates/id_provider.json'.format(keycloak_auth_server_url))
            sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/dlab/templates/id_provider.json'.format(keycloak_realm_name))
            sudo('sed -i \'s/CLIENT_ID/{}/g\' /opt/dlab/templates/id_provider.json'.format(keycloak_client_id))
            sudo('sed -i \'s/CLIENT_SECRET/{}/g\' /opt/dlab/templates/id_provider.json'.format(keycloak_client_secret))
            sudo('sed -i \'s/PROXY_STRING/{}/g\' /opt/dlab/templates/docker-compose.yml'.format(proxy_string))
            sudo('sed -i \'s|KEYCLOAK_AUTH_SERVER_URL|{}|g\' /opt/dlab/templates/superset_config.py'.format(keycloak_auth_server_url))
            sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/dlab/templates/superset_config.py'.format(keycloak_realm_name))
            sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/dlab/templates/superset_config.py'.format(edge_instance_public_ip))
            sudo('sed -i \'s/SUPERSET_NAME/{}/g\' /opt/dlab/templates/superset_config.py'.format(superset_name))
            sudo('cp -f /opt/dlab/templates/.env /home/{}/incubator-superset/contrib/docker/'.format(os_user))
            sudo('cp -f /opt/dlab/templates/docker-compose.yml /home/{}/incubator-superset/contrib/docker/'.format(os_user))
            sudo('cp -f /opt/dlab/templates/id_provider.json /home/{}/incubator-superset/contrib/docker/'.format(os_user))
            sudo('cp -f /opt/dlab/templates/requirements-extra.txt /home/{}/incubator-superset/contrib/docker/'.format(os_user))
            sudo('cp -f /opt/dlab/templates/superset_config.py /home/{}/incubator-superset/contrib/docker/'.format(os_user))
            sudo('cp -f /opt/dlab/templates/docker-init.sh /home/{}/incubator-superset/contrib/docker/'.format(os_user))
            sudo('touch /tmp/superset-notebook_installed')
    except Exception as err:
        print("Failed configure superset: " + str(err))
        sys.exit(1)
