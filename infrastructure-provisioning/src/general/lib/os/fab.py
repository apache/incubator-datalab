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
import json, uuid, time, datetime, csv
from dlab.meta_lib import *
from dlab.actions_lib import *
import dlab.actions_lib
import re


def ensure_pip(requisites):
    try:
        if not exists('/home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user'])):
            sudo('echo PATH=$PATH:/usr/local/bin/:/opt/spark/bin/ >> /etc/profile')
            sudo('echo export PATH >> /etc/profile')
            sudo('pip install -UI pip=={} --no-cache-dir'.format(os.environ['conf_pip_version']))
            sudo('pip install -U {} --no-cache-dir'.format(requisites))
            sudo('touch /home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user']))
        return True
    except:
        return False


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
            sudo('pip2 install notebook=={} --no-cache-dir'.format(jupyter_version))
            sudo('pip2 install jupyter --no-cache-dir')
            sudo('pip3.5 install notebook=={} --no-cache-dir'.format(jupyter_version))
            sudo('pip3.5 install jupyter --no-cache-dir')
            sudo('rm -rf ' + jupyter_conf_file)
            run('jupyter notebook --generate-config --config ' + jupyter_conf_file)
            with cd('/home/{}'.format(os_user)):
                run('mkdir -p ~/.jupyter/custom/')
                run('echo "#notebook-container { width: auto; }" > ~/.jupyter/custom/custom.css')
            sudo('echo "c.NotebookApp.ip = \'*\'" >> ' + jupyter_conf_file)
            sudo('echo "c.NotebookApp.base_url = \'/{0}/\'" >> {1}'.format(exploratory_name, jupyter_conf_file))
            sudo('echo c.NotebookApp.open_browser = False >> ' + jupyter_conf_file)
            sudo('echo \'c.NotebookApp.cookie_secret = b"' + id_generator() + '"\' >> ' + jupyter_conf_file)
            sudo('''echo "c.NotebookApp.token = u''" >> ''' + jupyter_conf_file)
            sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> ' + jupyter_conf_file)
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
    sudo('echo -e "\nKexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256" >> /etc/ssh/sshd_config')
    sudo('echo -e "Ciphers aes256-gcm@openssh.com,aes128-gcm@openssh.com,chacha20-poly1305@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr" >> /etc/ssh/sshd_config')
    sudo('echo -e "\tKexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256" >> /etc/ssh/ssh_config')
    sudo('echo -e "\tCiphers aes256-gcm@openssh.com,aes128-gcm@openssh.com,chacha20-poly1305@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr" >> /etc/ssh/ssh_config')
    try:
        sudo('service ssh restart')
    except:
        sudo('service sshd restart')


def install_r_pkg(requisites):
    status = list()
    error_parser = "ERROR:|error:|Cannot|failed|Please run|requires"
    try:
        for r_pkg in requisites:
            if r_pkg == 'sparklyr':
                run('sudo -i R -e \'install.packages("{0}", repos="http://cran.us.r-project.org", dep=TRUE)\' 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E  "({1})" /tmp/tee.tmp > /tmp/install_{0}.log; then  echo "" > /tmp/install_{0}.log;fi'.format(r_pkg, error_parser))
            sudo('R -e \'install.packages("{0}", repos="http://cran.us.r-project.org", dep=TRUE)\' 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E  "({1})" /tmp/tee.tmp >  /tmp/install_{0}.log; then  echo "" > /tmp/install_{0}.log;fi'.format(r_pkg, error_parser))
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

def install_java_pkg(requisites):
    status = list()
    error_parser = "ERROR|error|No such|no such|Please run|requires"
    try:
        if not exists('/home/' + os_user + '/java_libs'):
            run('mkdir /home/' + os_user + '/java_libs')
        run('cd /home/' + os_user + '/java_libs')
        for java_pkg in requisites:
            splitted_pkg = java_pkg.split(":")
            name_pkg = splitted_pkg[1] + '-' + splitted_pkg[2] + '.jar'
            sudo('wget -O {3} https://search.maven.org/classic/remotecontent?filepath={0}/{1}/{2}/{3} 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E  "({4})" /tmp/tee.tmp >  /tmp/install_{3}.log; then  echo "" > /tmp/install_{3}.log;fi'.format(splitted_pkg[0].replace(".","/"), splitted_pkg[1], splitted_pkg[2],name_pkg, error_parser))
            sudo('ls -la | if ! grep -w {0} > /tmp/install_{0}.list; then  echo "" > /tmp/install_{0}.list;fi'.format(java_pkg))
            sudo('jar tf {0} 2>&1 |if ! grep -w -E "({1})" > /tmp/install_{0}.list; then  echo "" > /tmp/install_{0}.list;fi'.format(name_pkg, error_parser))
            err = sudo('cat /tmp/install_{0}.log'.format(name_pkg)).replace('"', "'")
            res = sudo('cat /tmp/install_{0}.list'.format(name_pkg))
            if res:
                err+=' jar tf results:' + sudo('cat /tmp/install_{0}.list'.format(name_pkg))
                status.append({"group": "java_pkg", "name": java_pkg, "status": "failed", "error_message": err})
            else:
                status.append({"group": "java_pkg", "name": splitted_pkg[0]+':'+splitted_pkg[1], "version": splitted_pkg[2], "status": "installed"})
        return status
    except:
        return "Fail to install Java packages"

def get_available_r_pkgs():
    try:
        r_pkgs = dict()
        sudo('R -e \'write.table(available.packages(contriburl="http://cran.us.r-project.org/src/contrib"), file="/tmp/r.csv", row.names=F, col.names=F, sep=",")\'')
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


def install_ungit(os_user, notebook_name):
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
        sudo('R -e "install.packages(\'devtools\', repos = \'http://cran.us.r-project.org\')"')
        sudo('R -e "install.packages(\'knitr\', repos = \'http://cran.us.r-project.org\')"')
        sudo('R -e "install.packages(\'ggplot2\', repos = \'http://cran.us.r-project.org\')"')
        sudo('R -e "install.packages(c(\'devtools\',\'mplot\', \'googleVis\'), '
             'repos = \'http://cran.us.r-project.org\'); require(devtools); install_github(\'ramnathv/rCharts\')"')
        sudo('touch /home/' + os_user + '/.ensure_dir/r_packages_ensured')


def add_breeze_library_local(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/breeze_local_ensured'):
        try:
            breeze_tmp_dir = '/tmp/breeze_tmp_local/'
            jars_dir = '/opt/jars/'
            sudo('mkdir -p {}'.format(breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze_{0}/{1}/breeze_{0}-{1}.jar -O \
                    {2}breeze_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-natives_{0}/{1}/breeze-natives_{0}-{1}.jar -O \
                    {2}breeze-natives_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-viz_{0}/{1}/breeze-viz_{0}-{1}.jar -O \
                    {2}breeze-viz_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-macros_{0}/{1}/breeze-macros_{0}-{1}.jar -O \
                    {2}breeze-macros_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/scalanlp/breeze-parent_{0}/{1}/breeze-parent_{0}-{1}.jar -O \
                    {2}breeze-parent_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/jfree/jfreechart/{0}/jfreechart-{0}.jar -O \
                    {1}jfreechart-{0}.jar'.format('1.0.19', breeze_tmp_dir))
            sudo('wget http://central.maven.org/maven2/org/jfree/jcommon/{0}/jcommon-{0}.jar -O \
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
    sudo('echo "export PATH=$PATH:/usr/local/bin" >> /etc/profile')
    sudo('source /etc/profile')
    run('source /etc/profile')


def remove_rstudio_dataengines_kernel(cluster_name, os_user):
    try:
        get('/home/{}/.Rprofile'.format(os_user), 'Rprofile')
        data = open('Rprofile').read()
        conf = [i for i in data.split('\n') if i != '']
        conf = [i for i in conf if cluster_name not in i]
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
        conf = [i for i in data.split('\n') if i != '']
        comment_all = lambda x: x if x.startswith('#') else '#{}'.format(x)
        conf = [comment_all(i) for i in conf]
        conf = [i for i in conf if cluster_name not in i]
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
