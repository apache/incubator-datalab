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

import csv
import datetime
import json
import os
import random
import re
import string
import sys
import time
import traceback
import subprocess
from datalab.actions_lib import *
from datalab.common_lib import *
from datalab.meta_lib import *
from datalab.logger import logging
from fabric import *
from patchwork.files import exists
from patchwork import files


# general functions for all resources
def init_datalab_connection(hostname, username, keyfile, reserve_user='', run_echo=True):
    try:
        global conn
        if reserve_user:
            users = [username, reserve_user]
        else:
            users = [username]
        for user in users:
            attempt = 0
            while attempt < 15:
                logging.info('connection attempt {} with user {}'.format(attempt, user))
                conn = Connection(host=hostname, user=user, connect_kwargs={'banner_timeout': 200,
                                                                            'key_filename': keyfile})
                conn.config.run.echo = run_echo
                try:
                    conn.run('hostname')
                    conn.config.run.echo = run_echo
                    return conn
                except:
                    attempt += 1
                    time.sleep(10)
        if attempt == 15:
            logging.info('Unable to establish connection')
            raise Exception
    except Exception as err:
        logging.error('Function init_datalab_connection error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_pip(requisites):
    try:
        if not exists(conn, '/home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user'])):
            conn.sudo('bash -l -c "echo PATH=$PATH:/usr/local/bin/:/opt/spark/bin/ >> /etc/profile"')
            conn.sudo('bash -l -c "echo export PATH >> /etc/profile"')
            conn.sudo('pip3 install -UI pip=={} --no-cache-dir'.format(os.environ['conf_pip_version']))
            conn.sudo('pip3 install -U setuptools=={}'.format(os.environ['notebook_setuptools_version']))
            conn.sudo('pip3 install -UI {} --no-cache-dir'.format(requisites))
            conn.sudo('touch /home/{}/.ensure_dir/pip_path_added'.format(os.environ['conf_os_user']))
    except Exception as err:
        logging.error('Function ensure_pip error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


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
        logging.error('Function replace_multi_symbols error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def append_result(error, exception=''):
    try:
        ts = time.time()
        st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
        if exception:
            error_message = "[Error-{}]: {}. Exception: {}".format(st, error, str(exception))
            logging.error(error_message)
        else:
            error_message = "[Error-{}]: {}.".format(st, error)
            logging.error(error_message)
        with open('/root/result.json', 'a+') as f:
            text = f.read()
        if len(text) == 0:
            res = '{"error": ""}'
            with open('/root/result.json', 'w') as f:
                f.write(res)
        with open("/root/result.json") as f:
            data = json.load(f)
        data['error'] = data['error'] + error_message
        with open("/root/result.json", 'w') as f:
            json.dump(data, f)
        logging.error(data)
    except Exception as err:
        logging.error('Function append_result error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def put_resource_status(resource, status, datalab_path, os_user, hostname):
    try:
        keyfile = os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem"
        init_datalab_connection(hostname, os_user, keyfile)
        conn.sudo(
            'python3 ' + datalab_path + 'tmp/resource_status.py --resource {} --status {}'.format(resource, status))
        conn.close()
    except Exception as err:
        logging.error('Function put_resource_status error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_ciphers():
    try:
        conn.sudo(
            '''bash -c "echo -e '\nKexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256' >> /etc/ssh/sshd_config"''')
        conn.sudo(
            '''bash -c "echo -e 'Ciphers aes256-gcm@openssh.com,aes128-gcm@openssh.com,chacha20-poly1305@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr' >> /etc/ssh/sshd_config"''')
        conn.sudo(
            '''bash -c "echo -e '\tKexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256' >> /etc/ssh/ssh_config"''')
        conn.sudo(
            '''bash -c "echo -e '\tCiphers aes256-gcm@openssh.com,aes128-gcm@openssh.com,chacha20-poly1305@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr' >> /etc/ssh/ssh_config"''')
        try:
            conn.sudo('service ssh reload')
        except:
            conn.sudo('service sshd reload')
    except Exception as err:
        logging.error('Function pensure_ciphers error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def manage_npm_pkg(command):
    try:
        npm_count = 0
        installed = False
        npm_registry = ['https://registry.npmjs.org/', 'https://registry.npmjs.com/']
        while not installed:
            if npm_count > 60:
                logging.error("NPM registry is not available, please try later")
                sys.exit(1)
            else:
                try:
                    if npm_count % 2 == 0:
                        conn.sudo('npm config set registry {}'.format(npm_registry[0]))
                    else:
                        conn.sudo('npm config set registry {}'.format(npm_registry[1]))
                    conn.sudo('{}'.format(command))
                    installed = True
                except:
                    npm_count += 1
                    time.sleep(50)
    except Exception as err:
        logging.error('Function manage_npm_pkg error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def update_hosts_file(os_user):
    try:
        if not exists(conn, '/home/{}/.ensure_dir/hosts_file_updated'.format(os_user)):
            conn.sudo('sed -i "s/^127.0.0.1 localhost/127.0.0.1 localhost localhost.localdomain/g" /etc/hosts')
            conn.sudo('touch /home/{}/.ensure_dir/hosts_file_updated'.format(os_user))
    except Exception as err:
        logging.error('Function update_hosts_file error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def install_certbot(os_user):
    try:
        if not exists(datalab.fab.conn, '/home/{}/.ensure_dir/certbot_ensured'.format(os_user)):
            datalab.fab.conn.sudo('snap install core')
            datalab.fab.conn.sudo('snap refresh core')
            datalab.fab.conn.sudo('snap install --classic certbot')
            datalab.fab.conn.sudo('ln -s /snap/bin/certbot /usr/bin/certbot')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/certbot_ensured'.format(os_user))
    except Exception as err:
        logging.error('Installing Certbot error: ' + str(err))
        traceback.print_exc()
        sys.exit(1)


def run_certbot(domain_name, node, email=''):
    try:
        if node == 'ssn':
            datalab.fab.conn.sudo('service nginx stop')
        else:
            datalab.fab.conn.sudo('service openresty stop')
        if email != '':
            datalab.fab.conn.sudo('certbot certonly --standalone -n -d {}.{} -m {} --agree-tos'.format(node,
                                                                                                       domain_name,
                                                                                                       email))
        else:
            datalab.fab.conn.sudo('certbot certonly --standalone -n -d {}.{} --register-unsafely-without-email '
                                  '--agree-tos'.format(node, domain_name))
    except Exception as err:
        logging.error('Running Certbot error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_nginx_LE(domain_name, node):
    try:
        server_name_line ='    server_name {}.{};'.format(node, domain_name)
        cert_path_line = '    ssl_certificate  /etc/letsencrypt/live/{}.{}/fullchain.pem;'.format(node, domain_name)
        cert_key_line = '    ssl_certificate_key /etc/letsencrypt/live/{}.{}/privkey.pem;'.format(node, domain_name)
        #certbot_service = "ExecStart = /usr/bin/certbot -q renew --pre-hook 'service nginx stop' --post-hook 'service nginx start'"
        #certbot_service_path = '/lib/systemd/system/certbot.service'
        if node == 'ssn':
            nginx_config_path = '/etc/nginx/conf.d/nginx_proxy.conf'
        else:
            nginx_config_path = '/usr/local/openresty/nginx/conf/conf.d/proxy.conf'
        datalab.fab.conn.sudo('sed -i "s|.*    server_name .*|{}|" {}'.format(server_name_line, nginx_config_path))
        datalab.fab.conn.sudo('sed -i "s|.*    ssl_certificate .*|{}|" {}'.format(cert_path_line, nginx_config_path))
        datalab.fab.conn.sudo('sed -i "s|.*    ssl_certificate_key .*|{}|" {}'.format(cert_key_line, nginx_config_path))
        #datalab.fab.conn.sudo('sed -i "s|.*ExecStart.*|{}|" {}'.format(certbot_service, certbot_service_path))
        if node == 'ssn':
            datalab.fab.conn.sudo('systemctl restart nginx')
        else:
            datalab.fab.conn.sudo('systemctl restart openresty')
    except Exception as err:
        logging.error('Configuring Nginx Let’s Encrypt certs error:', str(err))
        traceback.print_exc()
        sys.exit(1)


#function for edge node only
def configure_http_proxy_server(config):
    try:
        if not exists(datalab.fab.conn,'/tmp/http_proxy_ensured'):
            manage_pkg('-y install', 'remote', 'squid')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            conn.put(template_file, '/tmp/squid.conf')
            conn.sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            conn.sudo('sed -i "s|PROXY_SUBNET|{}|g" /etc/squid/squid.conf'.format(proxy_subnet))
            replace_string = ''
            for cidr in config['vpc_cidrs']:
                replace_string += 'acl AWS_VPC_CIDR dst {}\\n'.format(cidr)
            conn.sudo('sed -i "s|VPC_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            replace_string = ''
            for cidr in config['allowed_ip_cidr']:
                replace_string += 'acl AllowedCIDRS src {}\\n'.format(cidr)
            conn.sudo('sed -i "s|ALLOWED_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            conn.sudo('systemctl restart squid')
            conn.sudo('touch /tmp/http_proxy_ensured')
    except Exception as err:
        logging.error('Fai to install and configure squid:', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_nftables(config):
    try:
        if not exists(datalab.fab.conn,'/tmp/nftables_ensured'):
            manage_pkg('-y install', 'remote', 'nftables')
            conn.sudo('systemctl enable nftables.service')
            conn.sudo('systemctl start nftables')
            conn.sudo('sysctl net.ipv4.ip_forward=1')
            if os.environ['conf_cloud_provider'] == 'aws':
                interface = 'eth0'
            elif os.environ['conf_cloud_provider'] == 'gcp':
                interface = 'ens4'
            conn.sudo('sed -i \'s/#net.ipv4.ip_forward=1/net.ipv4.ip_forward=1/g\' /etc/sysctl.conf')
            conn.sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/datalab/templates/nftables.conf'.format(config['edge_ip']))
            conn.sudo('sed -i "s|INTERFACE|{}|g" /opt/datalab/templates/nftables.conf'.format(interface))
            conn.sudo(
                'sed -i "s|SUBNET_CIDR|{}|g" /opt/datalab/templates/nftables.conf'.format(config['exploratory_subnet']))
            conn.sudo('cp /opt/datalab/templates/nftables.conf /etc/')
            conn.sudo('systemctl restart nftables')
            conn.sudo('touch /tmp/nftables_ensured')
    except Exception as err:
        logging.error('Failed to configure nftables:', (err))
        traceback.print_exc()
        sys.exit(1)

def ensure_python_venv_deeplearn(python_venv_version):
    try:
        if not exists(conn, '/opt/python/python{}'.format(python_venv_version)):
            if os.environ['conf_cloud_provider'] == 'azure':
                conn.sudo('rm /etc/apt/sources.list.d/cuda*')
                conn.sudo('apt update')
            conn.sudo('add-apt-repository ppa:deadsnakes/ppa -y')
            conn.sudo('apt install python{0} -y'.format(python_venv_version[:3]))
            conn.sudo('apt install virtualenv')
            conn.sudo('mkdir -p /opt/python/')
            conn.sudo('mkdir -p /opt/python/python{0}'.format(python_venv_version))
            conn.sudo('virtualenv --python=/usr/bin/python{0} /opt/python/python{1}'.format(python_venv_version[:3],python_venv_version))
            venv_command = 'source /opt/python/python{0}/bin/activate'.format(python_venv_version)
            pip_command = '/opt/python/python{0}/bin/pip{1}'.format(python_venv_version, python_venv_version[:3])
            conn.sudo('''bash -l -c '{0} && {1} install -U pip=={2}' '''.format(venv_command, pip_command, os.environ['conf_pip_version']))
            conn.sudo('''bash -l -c '{} && {} install -U ipython=={} ipykernel=={} NumPy=={} SciPy=={} Matplotlib=={} '''
                      '''pandas=={} Sympy=={} Pillow=={} scikit-learn=={} --no-cache-dir' '''
                      .format(venv_command, pip_command, os.environ['pip_packages_ipython'],
                              os.environ['pip_packages_ipykernel'],
                              os.environ['pip_packages_numpy'], os.environ['pip_packages_scipy'],
                              os.environ['pip_packages_matplotlib'],
                              os.environ['pip_packages_pandas'], os.environ['pip_packages_sympy'],
                              os.environ['pip_packages_pillow'],
                              os.environ['pip_packages_scikit_learn']))
    except Exception as err:
        logging.error('Function ensure_python_venv error:', str(err))
        traceback.print_exc()
        sys.exit(1)

# functions for all computation resources
def ensure_python_venv(python_venv_version):
    try:
        if not exists(conn, '/opt/python/python{}'.format(python_venv_version)):
            conn.sudo('wget https://www.python.org/ftp/python/{0}/Python-{0}.tgz -O /tmp/Python-{0}.tgz'.format(
                python_venv_version))
            conn.sudo('tar zxvf /tmp/Python-{}.tgz -C /tmp/'.format(python_venv_version))
            if os.environ['application'] in ('rstudio', 'tensor-rstudio'):
                conn.sudo('''bash -l -c 'cd /tmp/Python-{0} && ./configure --prefix=/opt/python/python{0} '''
                          '''--with-zlib-dir=/usr/local/lib/ --with-ensurepip=install --enable-shared' '''
                    .format(python_venv_version))
                conn.sudo('''bash -l -c 'echo "export LD_LIBRARY_PATH=/opt/python/python{}/lib" >> /etc/profile' '''
                    .format(python_venv_version))
            else:
                conn.sudo(
                    '''bash -l -c 'cd /tmp/Python-{0} && ./configure --prefix=/opt/python/python{0} '''
                    '''--with-zlib-dir=/usr/local/lib/ --with-ensurepip=install' '''.format(
                        python_venv_version))
            if os.environ['application'] == 'deeplearning':
                conn.sudo(
                    'apt-get install -y build-essential checkinstall libreadline-gplv2-dev libncursesw5-dev libsqlite3-dev tk-dev libgdbm-dev libc6-dev libbz2-dev')
                conn.sudo(
                    'apt install -y libncurses-dev libgdbm-dev libz-dev tk-dev libsqlite3-dev libreadline-dev liblzma-dev libffi-dev libssl-dev')
                conn.sudo('apt-get install -y uuid-dev')
                conn.sudo('apt install -y libgdbm-dev')
                conn.sudo('apt install virtualenv')
            conn.sudo('''bash -l -c 'cd /tmp/Python-{0} && make altinstall' '''.format(python_venv_version))
            conn.sudo('''bash -l -c 'cd /tmp && rm -rf Python-{}' '''.format(python_venv_version))
            conn.sudo('''bash -l -c 'virtualenv /opt/python/python{0}' '''.format(python_venv_version))
            conn.sudo('chown -R datalab-user:datalab-user /opt/python/')
            venv_command = 'source /opt/python/python{}/bin/activate'.format(python_venv_version)
            pip_command = '/opt/python/python{0}/bin/pip{1}'.format(python_venv_version, python_venv_version[:3])
            conn.sudo('''bash -l -c '{0} && {1} install -UI pip=={2}' '''.format(venv_command, pip_command,
                                                                                 os.environ['conf_pip_version']))
            conn.sudo('''bash -l -c '{} && {} install -UI ipython=={} ipykernel=={} NumPy=={} SciPy=={} Matplotlib=={} '''
                      '''pandas=={} Sympy=={} Pillow=={} scikit-learn=={} --no-cache-dir' '''
                      .format(venv_command, pip_command, os.environ['pip_packages_ipython'], os.environ['pip_packages_ipykernel'],
                              os.environ['pip_packages_numpy'], os.environ['pip_packages_scipy'], os.environ['pip_packages_matplotlib'],
                              os.environ['pip_packages_pandas'], os.environ['pip_packages_sympy'], os.environ['pip_packages_pillow'],
                              os.environ['pip_packages_scikit_learn']))

    except Exception as err:
        logging.error('Function ensure_python_venv error:', str(err))
        traceback.print_exc()
        sys.exit(1)

def ensure_anaconda():
    try:
        if not exists(conn, '/opt/anaconda3'):
            conn.sudo('wget https://repo.anaconda.com/archive/Anaconda3-2021.11-Linux-x86_64.sh -O /tmp/anaconda.sh')
            conn.sudo('bash /tmp/anaconda.sh -b -p /opt/anaconda3')
            conn.sudo('chown -R datalab-user /opt/anaconda3')
            #conn.sudo(''' bash -l -c "echo 'export PATH=/opt/anaconda3/bin/:\$PATH' >> /home/datalab-user/.bashrc" ''')
            #conn.run('source /home/datalab-user/.bashrc')
            conn.run('source /opt/anaconda3/etc/profile.d/conda.sh && conda create -y -p /opt/anaconda3/envs/jupyter-conda git pip ipykernel -c anaconda')
            conn.run('source /opt/anaconda3/etc/profile.d/conda.sh && conda activate jupyter-conda && /opt/anaconda3/envs/jupyter-conda/bin/pip install numpy scipy pandas scikit-learn transformers==4.4.2 gensim==4.0.1 tokenizers==0.10.1 python-levenshtein==0.12.2')
            conn.run('source /opt/anaconda3/etc/profile.d/conda.sh && conda activate jupyter-conda && /opt/anaconda3/envs/jupyter-conda/bin/pip install -U torch==1.10.0+cu111 torchvision==0.11.3+cu111 torchaudio==0.10.2+cu111 -f https://download.pytorch.org/whl/cu111/torch_stable.html --no-cache-dir')
            conn.sudo('chown -R datalab-user /home/datalab-user/.local/share/jupyter/kernels')
            conn.run('source /opt/anaconda3/etc/profile.d/conda.sh && conda activate jupyter-conda && python -m ipykernel install --user --name=jupyter-conda')
            conn.sudo('chown -R root /home/datalab-user/.local/share/jupyter/kernels')
            conn.sudo('systemctl restart jupyter-notebook')
    except Exception as err:
        logging.error('Function ensure_anaconda error:', str(err))
        traceback.print_exc()
        sys.exit(1)

def install_venv_pip_pkg(pkg_name, pkg_version=''):
    try:
        venv_install_command = 'source /opt/python/python{0}/bin/activate && /opt/python/python{0}/bin/pip{1}'.format(
            os.environ['notebook_python_venv_version'], os.environ['notebook_python_venv_version'][:3])
        if pkg_version:
            pip_pkg = '{}=={}'.format(pkg_name, pkg_version)
        else:
            pip_pkg = pkg_name
        conn.sudo('''bash -l -c '{0} install -U {1} --no-cache-dir' '''.format(venv_install_command, pip_pkg))
    except Exception as err:
        logging.error('Function install_venv_pip_pkg error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def install_pip_pkg(requisites, pip_version, lib_group, dataengine_service=False):
    status = list()
    error_parser = "Could not|No matching|ImportError:|failed|EnvironmentError:|requires|FileNotFoundError:|" \
                   "RuntimeError:|error:"
    try:
        if dataengine_service:
            install_command = pip_version
        elif os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning':
            install_command = 'conda activate && {}'.format(pip_version)
        else:
            install_command = 'source /opt/python/python{0}/bin/activate && /opt/python/python{0}/bin/pip{1}'.format(
                os.environ['notebook_python_venv_version'], os.environ['notebook_python_venv_version'][:3])
        for pip_pkg in requisites:
            name, vers = pip_pkg
            if pip_pkg[1] == '' or pip_pkg[1] == 'N/A':
                pip_pkg = pip_pkg[0]
                version = 'N/A'
            else:
                version = pip_pkg[1]
                pip_pkg = "{}=={}".format(pip_pkg[0], pip_pkg[1])
            conn.sudo(
                '''bash -l -c '{0} install -U {1} --use-deprecated=legacy-resolver --no-cache-dir 2>&1 | '''
                '''tee /tmp/{4}_install_{3}.tmp; if ! grep -w -i -E  "({2})" /tmp/{4}_install_{3}.tmp > /tmp/{4}_install_{3}.log; '''
                '''then  echo "" > /tmp/{4}_install_{3}.log;fi' '''.format(
                    install_command, pip_pkg, error_parser, name, pip_version))
            err = conn.sudo('cat /tmp/{0}_install_{1}.log'.format(pip_version, pip_pkg.split("==")[0])).stdout.replace(
                '"', "'").replace('\n', ' ')
            conn.sudo(
                '''bash -l -c '{0} freeze --all | if ! grep -w -i {1} > /tmp/{2}_install_{1}.list; '''
                '''then  echo "not_found" > /tmp/{2}_install_{1}.list;fi' '''.format(
                    install_command, name, pip_version))
            res = conn.sudo('''bash -l -c 'cat /tmp/{0}_install_{1}.list' '''.format(pip_version, name)).stdout.replace(
                '\n', '')
            conn.sudo(
                '''bash -l -c 'cat /tmp/{0}_install_{1}.tmp | if ! grep -w -i -E "(Successfully installed|up-to-date)" > '''
                '''/tmp/{0}_install_{1}.list; then  echo "not_installed" > /tmp/{0}_install_{1}.list;fi' '''.format(
                    pip_version, name))
            installed_out = conn.sudo(
                '''bash -l -c 'cat /tmp/{0}_install_{1}.list' '''.format(pip_version, name)).stdout.replace('\n', '')
            changed_pip_pkg = False
            if 'not_found' in res:
                changed_pip_pkg = pip_pkg.split("==")[0].replace("_", "-").split('-')
                changed_pip_pkg = changed_pip_pkg[0]
                conn.sudo(
                    '''bash -l -c '{0} freeze --all | if ! grep -w -i {1} > /tmp/{2}_install_{1}.list; then  echo "" > '''
                    '''/tmp/{2}_install_{1}.list;fi' '''.format(
                        install_command, changed_pip_pkg, pip_version))
                res = conn.sudo('cat /tmp/{0}_install_{1}.list'.format(pip_version, changed_pip_pkg)).stdout.replace(
                    '\n', '')
            if err and name not in installed_out:
                status_msg = 'installation_error'
                if 'ERROR: No matching distribution found for {}'.format(name) in err:
                    status_msg = 'invalid_name'
            elif res:
                res = res.lower()
                ansi_escape = re.compile(r'\x1b[^m]*m')
                ver = ansi_escape.sub('', res).split("\r\n")
                if changed_pip_pkg:
                    version = [i for i in ver if changed_pip_pkg.lower() in i][0].split('==')[1]
                else:
                    version = \
                        [i for i in ver if pip_pkg.split("==")[0].lower() in i][0].split('==')[1]
                status_msg = "installed"
            versions = []
            if 'Could not find a version that satisfies the requirement' in err \
                    and 'ERROR: No matching distribution found for {}=='.format(name) in err:
                versions = err[err.find("(from versions: ") + 16: err.find(") ")]
                if versions != '' and versions != 'none':
                    versions = versions.split(', ')
                    version = vers
                    status_msg = 'invalid_version'
                else:
                    versions = []

            conn.sudo('cat /tmp/{0}_install_{1}.tmp | if ! grep -w -i -E  "Installing collected packages:" > '
                      '/tmp/{0}_install_{1}.dep; then  echo "" > /tmp/{0}_install_{1}.dep;fi'.format(pip_version, name))
            dep = conn.sudo('cat /tmp/{0}_install_{1}.dep'.format(pip_version, name)).stdout.replace('\n', '').strip()[
                  31:]
            if dep == '':
                dep = []
            else:
                dep = dep.split(', ')
                for n, i in enumerate(dep):
                    if i == name:
                        dep[n] = ''
                    else:
                        conn.sudo('{0} show {1} 2>&1 | if ! grep Version: > '
                                  '/tmp/{0}_install_{1}.log; then echo "" > /tmp/{0}_install_{1}.log;fi'.format(
                            pip_version, i))
                        dep[n] = conn.sudo('cat /tmp/{0}_install_{1}.log'.format(
                            pip_version, i)).stdout.replace('\n', '').replace('Version: ', '{} v.'.format(i))
                dep = [i for i in dep if i]
            status.append({"group": lib_group, "name": name, "version": version, "status": status_msg,
                           "error_message": err, "available_versions": versions, "add_pkgs": dep})
            conn.sudo('rm -rf /tmp/*{}*'.format(name))
        return status
    except Exception as err:
        for pip_pkg in requisites:
            name, vers = pip_pkg
            status.append({"group": lib_group, "name": name, "version": vers, "status": 'installation_error',
                           "error_message": err})
        logging.error("Failed to install {} packages: {}".format(pip_version, err))
        traceback.print_exc()
        return status

def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def ensure_dataengine_tensorflow_jars(jars_dir):
    subprocess.run('wget https://repos.spark-packages.org/tapanalyticstoolkit/spark-tensorflow-connector/1.0.0-s_2.11/spark-tensorflow-connector-1.0.0-s_2.11.jar \
         -O {}spark-tensorflow-connector-1.0.0-s_2.11.jar'.format(jars_dir), shell=True, check=True)


def install_r_pkg(requisites):
    status = list()
    error_parser = "ERROR:|error:|Cannot|failed|Please run|requires|Error|Skipping|couldn't find"
    if os.environ['conf_resource'] == 'dataengine-service':
        ensure_dataengine_service_devtools()
    try:
        for r_pkg in requisites:
            name, vers = r_pkg
            version = vers
            if vers == 'N/A':
                vers = ''
            else:
                vers = '"{}"'.format(vers)
            if name == 'sparklyr':
                conn.run(
                    'sudo -i R -e \'devtools::install_version("{0}", version = {1}, repos = "http://cran.us.r-project.org", '
                    'dependencies = NA)\' 2>&1 | tee /tmp/install_{0}.tmp; if ! grep -w -E  "({2})" /tmp/install_{0}.tmp '
                    '> /tmp/install_{0}.log; then  echo "" > /tmp/install_{0}.log;fi'.format(name, vers, error_parser))
            else:
                conn.sudo(
                    'R -e \'devtools::install_version("{0}", version = {1}, repos = "https://cloud.r-project.org", '
                    'dependencies = NA)\' 2>&1 | tee /tmp/install_{0}.tmp; if ! grep -w -E "({2})" /tmp/install_{0}.tmp > '
                    '/tmp/install_{0}.log; then  echo "" > /tmp/install_{0}.log;fi'.format(name, vers, error_parser))
            dep = conn.sudo('grep "(NA.*->". /tmp/install_' + name + '.tmp | awk \'{print $1}\'').stdout.replace('\n',
                                                                                                                 ' ')
            dep_ver = conn.sudo('grep "(NA.*->". /tmp/install_' + name + '.tmp | awk \'{print $4}\'').stdout.replace(
                '\n', ' ').replace(')', '').split(' ')
            if dep == '':
                dep = []
            else:
                dep = dep.split(' ')
                for n, i in enumerate(dep):
                    if i == name:
                        dep[n] = ''
                    else:
                        dep[n] = '{} v.{}'.format(dep[n], dep_ver[n])
                dep = [i for i in dep if i]
            conn.sudo('hostname')
            err = conn.sudo('cat /tmp/install_{0}.log'.format(name)).stdout.replace('"', "'").replace('\n', '')
            conn.sudo(
                'R -e \'installed.packages()[,c(3:4)]\' | if ! grep -w {0} > /tmp/install_{0}.list; then  echo "" > /tmp/install_{0}.list;fi'.format(
                    name))
            res = conn.sudo('cat /tmp/install_{0}.list'.format(name)).stdout.replace('\n', '')
            if err:
                status_msg = 'installation_error'
                if 'couldn\'t find package \'{}\''.format(name) in err:
                    status_msg = 'invalid_name'
            elif res:
                ansi_escape = re.compile(r'\x1b[^m]*m')
                version = ansi_escape.sub('', res).split("\n")[0].split('"')[1]
                status_msg = 'installed'
            if 'Error in download_version_url(package, version, repos, type) :' in err or 'Error in parse_deps(paste(spec,' in err:
                conn.sudo('R -e \'install.packages("versions", repos="https://cloud.r-project.org", dep=TRUE)\'')
                versions = conn.sudo('R -e \'library(versions); available.versions("' + name + '")\' 2>&1 | grep -A 50 '
                                                                                               '\'date available\' | awk \'{print $2}\'').stdout.strip().replace(
                    '\n', ' ')[5:].split(' ')
                if versions != ['']:
                    status_msg = 'invalid_version'
                else:
                    versions = []
            else:
                versions = []
            status.append(
                {"group": "r_pkg", "name": name, "version": version, "status": status_msg, "error_message": err,
                 "available_versions": versions, "add_pkgs": dep})
        conn.sudo('rm /tmp/*{}*'.format(name))
        return status
    except Exception as err:
        for r_pkg in requisites:
            name, vers = r_pkg
            status.append(
                {"group": "r_pkg", "name": name, "version": vers, "status": 'installation_error', "error_message": err})
        logging.error("Failed to install R packages:", err)
        traceback.print_exc()
        return status


def update_spark_jars(jars_dir='/opt/jars'):
    try:
        configs = conn.sudo('find /opt/ /etc/ /usr/lib/ -name spark-defaults.conf -type f').stdout.split('\n')
        if exists(conn, jars_dir):
            for conf in filter(None, configs):
                des_path = ''
                all_jars = conn.sudo('find {0} -name "*.jar"'.format(jars_dir)).stdout.split('\n')
                if ('-des-' in conf):
                    des_path = '/'.join(conf.split('/')[:3])
                    all_jars = find_des_jars(all_jars, des_path)
                conn.sudo('''sed -i '/^# Generated\|^spark.jars/d' {0}'''.format(conf))
                conn.sudo(''' bash -l -c 'echo "# Generated spark.jars by DataLab from {0}\nspark.jars {1}" >> {2}' '''
                          .format(','.join(filter(None, [jars_dir, des_path])), ','.join(all_jars), conf))
                # conn.sudo("sed -i 's/^[[:space:]]*//' {0}".format(conf))
        else:
            logging.info("Can't find directory {0} with jar files".format(jars_dir))
    except Exception as err:
        logging.error('Function update_spark_jars error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def install_java_pkg(requisites):
    status = list()
    error_parser = "ERROR|error|No such|no such|Please run|requires|module not found|Exception"
    templates_dir = '/root/templates/'
    ivy_dir = '/opt/ivy'
    ivy_cache_dir = '{0}/cache/'.format(ivy_dir)
    ivy_settings = 'ivysettings.xml'
    dest_dir = '/opt/jars/java'
    try:
        ivy_jar = conn.sudo('find /opt /usr -name "*ivy-{0}.jar" | head -n 1'.format(
            os.environ['notebook_ivy_version'])).stdout.replace('\n', '')
        conn.sudo('mkdir -p {0} {1}'.format(ivy_dir, dest_dir))
        conn.put('{0}{1}'.format(templates_dir, ivy_settings), '/tmp/{}'.format(ivy_settings))
        conn.sudo('cp -f /tmp/{1} {0}/{1}'.format(ivy_dir, ivy_settings))
        proxy_string = conn.sudo('cat /etc/profile | grep http_proxy | cut -f2 -d"="').stdout.replace('\n', '')
        proxy_re = '(?P<proto>http.*)://(?P<host>[^:/ ]+):(?P<port>[0-9]*)'
        proxy_find = re.search(proxy_re, proxy_string)
        java_proxy = "export _JAVA_OPTIONS='-Dhttp.proxyHost={0} -Dhttp.proxyPort={1} \
            -Dhttps.proxyHost={0} -Dhttps.proxyPort={1}'".format(proxy_find.group('host'), proxy_find.group('port'))
        for java_pkg in requisites:
            conn.sudo('rm -rf {0}'.format(ivy_cache_dir))
            conn.sudo('mkdir -p {0}'.format(ivy_cache_dir))
            group, artifact, version, override = java_pkg
            logging.info("Installing package (override: {3}): {0}:{1}:{2}".format(group, artifact, version, override))
            conn.sudo(
                '''bash -c "{8}; java -jar {0} -settings {1}/{2} -cache {3} -dependency {4} {5} {6} 2>&1 | tee /tmp/install_{5}.tmp; if ! grep -w -E  \\"({7})\\" /tmp/install_{5}.tmp > /tmp/install_{5}.log; then echo \\"\\" > /tmp/install_{5}.log;fi" '''.format(
                    ivy_jar, ivy_dir, ivy_settings, ivy_cache_dir, group, artifact, version, error_parser, java_proxy))
            err = conn.sudo('cat /tmp/install_{0}.log'.format(artifact)).stdout.replace('"', "'").strip()
            conn.sudo('find {0} -name "{1}*.jar" | head -n 1 | rev | cut -f1 -d "/" | rev | \
                if ! grep -w -i {1} > /tmp/install_{1}.list; then echo "" > /tmp/install_{1}.list;fi'.format(
                ivy_cache_dir, artifact))
            res = conn.sudo('cat /tmp/install_{0}.list'.format(artifact)).stdout.replace('\n', '')
            if res:
                conn.sudo('cp -f $(find {0} -name "*.jar" | xargs) {1}'.format(ivy_cache_dir, dest_dir))
                status.append({"group": "java", "name": "{0}:{1}".format(group, artifact), "version": version,
                               "status": "installed"})
            else:
                status.append(
                    {"group": "java", "name": "{0}:{1}".format(group, artifact), "status": "installation_error",
                     "error_message": err})
        update_spark_jars()
        conn.sudo('rm -rf /tmp/*{}*'.format(artifact))
        return status
    except Exception as err:
        for java_pkg in requisites:
            group, artifact, version, override = java_pkg
            status.append({"group": "java", "name": "{0}:{1}".format(group, artifact), "status": "installation_error",
                           "error_message": err})
        logging.error("Failed to install {} packages".format(requisites))
        traceback.print_exc()
        return status


def get_available_r_pkgs():
    try:
        r_pkgs = dict()
        conn.sudo(
            'R -e \'write.table(available.packages(contriburl="https://cloud.r-project.org/src/contrib"), file="/tmp/r.csv", row.names=F, col.names=F, sep=",")\'')
        conn.get("/tmp/r.csv", "r.csv")
        with open('r.csv', 'r') as csvfile:
            reader = csv.reader(csvfile, delimiter=',')
            for row in reader:
                r_pkgs[row[0]] = row[1]
        return r_pkgs
    except Exception as err:
        logging.error('Function get_available_r_pkgs error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def install_ungit(os_user, notebook_name, edge_ip):
    if not exists(conn, '/home/{}/.ensure_dir/ungit_ensured'.format(os_user)):
        try:
            manage_npm_pkg('npm -g install ungit@{}'.format(os.environ['notebook_ungit_version']))
            if os.environ['conf_deeplearning_cloud_ami'] =='true' and os.environ['conf_cloud_provider'] =='azure' and os.environ['application'] =='deeplearning':
                conn.put('/root/templates/ungit.service.18_04', '/tmp/ungit.service')
            else:
                conn.put('/root/templates/ungit.service', '/tmp/ungit.service')
            conn.sudo("sed -i 's|OS_USR|{}|' /tmp/ungit.service".format(os_user))
            http_proxy = conn.run('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n', '')
            conn.sudo("sed -i 's|PROXY_HOST|{}|g' /tmp/ungit.service".format(http_proxy))
            conn.sudo("sed -i 's|NOTEBOOK_NAME|{}|' /tmp/ungit.service".format(
                notebook_name))
            conn.sudo("mv -f /tmp/ungit.service /etc/systemd/system/ungit.service")
            conn.run('git config --global user.name "Example User"')
            conn.run('git config --global user.email "example@example.com"')
            conn.run('mkdir -p ~/.git/templates/hooks')
            conn.put('/root/scripts/git_pre_commit.py', '/home/{}/.git/templates/hooks/pre-commit'.format(os_user))
            conn.sudo('chmod 755 ~/.git/templates/hooks/pre-commit')
            conn.run('git config --global init.templatedir ~/.git/templates')
            conn.run('touch ~/.gitignore')
            conn.run('git config --global core.excludesfile ~/.gitignore')
            conn.run('echo ".ipynb_checkpoints/" >> ~/.gitignore')
            conn.run('echo "spark-warehouse/" >> ~/.gitignore')
            conn.run('echo "metastore_db/" >> ~/.gitignore')
            conn.run('echo "derby.log" >> ~/.gitignore')
            conn.sudo(
                '''bash -l -c 'echo -e "Host git.epam.com\n   HostName git.epam.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p\n" > /home/{}/.ssh/config' '''.format(
                    edge_ip, os_user))
            conn.sudo(
                '''bash -l -c 'echo -e "Host github.com\n   HostName github.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p" >> /home/{}/.ssh/config' '''.format(
                    edge_ip, os_user))
            conn.sudo(
                '''bash -l -c 'echo -e "Host gitlab.com\n   HostName gitlab.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p" >> /home/{}/.ssh/config' '''.format(
                    edge_ip, os_user))
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable ungit.service')
            conn.sudo('systemctl start ungit.service')
            conn.sudo('touch /home/{}/.ensure_dir/ungit_ensured'.format(os_user))
        except Exception as err:
            logging.error('Function install_ungit error:', str(err))
            traceback.print_exc()
            sys.exit(1)
    else:
        try:
            conn.sudo("sed -i 's|--rootPath=/.*-ungit|--rootPath=/{}-ungit|' /etc/systemd/system/ungit.service".format(
                notebook_name))
            http_proxy = conn.run('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n', '')
            conn.sudo(
                "sed -i 's|HTTPS_PROXY=.*3128|HTTPS_PROXY={}|g' /etc/systemd/system/ungit.service".format(http_proxy))
            conn.sudo(
                "sed -i 's|HTTP_PROXY=.*3128|HTTP_PROXY={}|g' /etc/systemd/system/ungit.service".format(http_proxy))
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl restart ungit.service')
        except Exception as err:
            logging.error('Function install_ungit error:', str(err))
            traceback.print_exc()
            sys.exit(1)
    try:
        conn.run('''bash -l -c 'git config --global http.proxy $http_proxy' ''')
        conn.run('''bash -l -c 'git config --global https.proxy $https_proxy' ''')
    except Exception as err:
        logging.error('Function install_ungit error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def install_inactivity_checker(os_user, ip_address, rstudio=False):
    if not exists(conn, '/home/{}/.ensure_dir/inactivity_ensured'.format(os_user)):
        try:
            if not exists(conn, '/opt/inactivity'):
                conn.sudo('mkdir /opt/inactivity')
            conn.put('/root/templates/inactive.service', '/tmp/inactive.service')
            conn.sudo('cp /tmp/inactive.service /etc/systemd/system/inactive.service')
            conn.put('/root/templates/inactive.timer', '/tmp/inactive.timer')
            conn.sudo('cp /tmp/inactive.timer /etc/systemd/system/inactive.timer')
            if rstudio:
                conn.put('/root/templates/inactive_rs.sh', '/tmp/inactive.sh')
                conn.sudo('cp /tmp/inactive.sh /opt/inactivity/inactive.sh')
            else:
                conn.put('/root/templates/inactive.sh', '/tmp/inactive.sh')
                conn.sudo('cp /tmp/inactive.sh /opt/inactivity/inactive.sh')
            conn.sudo("sed -i 's|IP_ADRESS|{}|g' /opt/inactivity/inactive.sh".format(ip_address))
            conn.sudo("chmod 755 /opt/inactivity/inactive.sh")
            conn.sudo("chown root:root /etc/systemd/system/inactive.service")
            conn.sudo("chown root:root /etc/systemd/system/inactive.timer")
            conn.sudo('''bash -l -c "date +%s > /opt/inactivity/local_inactivity" ''')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable inactive.timer')
            conn.sudo('systemctl start inactive.timer')
            conn.sudo('touch /home/{}/.ensure_dir/inactive_ensured'.format(os_user))
        except Exception as err:
            logging.error('Function install_inactivity_checker error:', str(err))
            traceback.print_exc()
            sys.exit(1)


def get_spark_memory(creds=False, os_user='', hostname='', keyfile=''):
    try:
        if creds:
            con = init_datalab_connection(hostname, os_user, keyfile)
            mem = con.sudo('free -m | grep Mem | tr -s " " ":" | cut -f 2 -d ":"').stdout.replace('\n', '')
            instance_memory = int(mem)
        else:
            mem = conn.sudo('free -m | grep Mem | tr -s " " ":" | cut -f 2 -d ":"').stdout.replace('\n', '')
            instance_memory = int(mem)
        spark_memory = round(instance_memory * 85 / 100)
        return spark_memory
    except Exception as err:
        logging.error('Function install_inactivity_checker error:', str(err))
        traceback.print_exc()
        sys.exit(1)


# functions for dataengine/dataengine-service resources
def ensure_dataengine_service_devtools():
    try:
        if not exists(conn, '/home/{}/dataengine-service-devtools-ensured'.format(os.environ['conf_os_user'])):
            if os.environ['conf_cloud_provider'] in 'aws':
                manage_pkg('-y install', 'remote', 'libcurl libcurl-devel')
            elif (os.environ['conf_cloud_provider'] in 'gcp') and (
                    '-w-' in conn.sudo('hostname').stdout.replace('\n', '')):
                # manage_pkg('-y build-dep', 'remote', 'libcurl4-gnutls-dev libxml2-dev')
                manage_pkg('-y install', 'remote', 'libxml2-dev libcurl4-openssl-dev pkg-config')
            conn.sudo('R -e "install.packages(\'devtools\', repos = \'cloud.r-project.org\')"')
            if (os.environ['conf_cloud_provider'] in 'gcp') and (
                    "R_LIBS_SITE" not in conn.sudo('cat /opt/conda/miniconda3/lib/R/etc/Renviron').stdout):
                conn.sudo(
                    '''bash -l -c 'echo "R_LIBS_SITE=${R_LIBS_SITE-'/usr/local/lib/R/site-library:/usr/lib/R/site-library:/usr/lib/R/library'}" >> /opt/conda/miniconda3/lib/R/etc/Renviron' ''')
            conn.sudo('touch /home/{}/dataengine-service-devtools-ensured'.format(os.environ['conf_os_user']))
    except Exception as err:
        logging.error('Function ensure_dataengine_service_devtools error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_data_engine_service_livy(hostname, os_user, keyfile):
    try:
        init_datalab_connection(hostname, os_user, keyfile)
        if exists(conn, '/usr/local/lib/livy'):
            conn.sudo('rm -r /usr/local/lib/livy')
        conn.sudo('wget -P /tmp/ https://nexus.develop.dlabanalytics.com/repository/packages-public/livy.tar.gz '
                  '--no-check-certificate')
        conn.sudo('tar -xzvf /tmp/livy.tar.gz -C /usr/local/lib/')
        conn.sudo('ln -s /usr/local/lib/incubator-livy /usr/local/lib/livy')
        conn.put('/root/templates/dataengine-service_livy-env.sh', '/usr/local/lib/livy/conf/livy-env.sh')
        conn.put('/root/templates/dataengine-service_livy.service', '/tmp/livy.service')
        conn.sudo("sed -i 's|OS_USER|{}|' /tmp/livy.service".format(os_user))
        conn.sudo('mv /tmp/livy.service /etc/systemd/system/livy.service')
        conn.sudo('systemctl daemon-reload')
        conn.sudo('systemctl enable livy.service')
        conn.sudo('systemctl start livy.service')
        conn.close()
    except Exception as err:
        logging.error('Function configure_data_engine_service_livy error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def remove_rstudio_dataengines_kernel(cluster_name, os_user):
    try:
        cluster_re = ['-{}"'.format(cluster_name),
                      '-{}-'.format(cluster_name),
                      '-{}/'.format(cluster_name)]
        conn.get('/home/{}/.Rprofile'.format(os_user), 'Rprofile')
        data = open('Rprofile').read()
        conf = filter(None, data.split('\n'))
        # Filter config from any math of cluster_name in line,
        # separated by defined symbols to avoid partly matches
        conf = [i for i in conf if not any(x in i for x in cluster_re)]
        comment_all = lambda x: x if x.startswith('#master') else '#{}'.format(x)
        uncomment = lambda x: x[1:] if not x.startswith('#master') else x
        conf = [comment_all(i) for i in conf]
        conf = [uncomment(i) for i in conf]
        last_spark = max([conf.index(i) for i in conf if 'master=' in i] or [0])
        active_cluster = conf[last_spark].split('"')[-2] if last_spark != 0 else None
        conf = conf[:last_spark] + [conf[l][1:] for l in range(last_spark, len(conf)) if conf[l].startswith("#")] \
               + [conf[l] for l in range(last_spark, len(conf)) if not conf[l].startswith('#')]
        with open('.Rprofile', 'w') as f:
            for line in conf:
                f.write('{}\n'.format(line))
        conn.put('.Rprofile', '/home/{}/.Rprofile'.format(os_user))
        conn.get('/home/{}/.Renviron'.format(os_user), 'Renviron')
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
        conn.put('.Renviron', '/home/{}/.Renviron'.format(os_user))
        if len(conf) == 1:
            conn.sudo('rm -f /home/{}/.ensure_dir/rstudio_dataengine_ensured'.format(os_user))
            conn.sudo('rm -f /home/{}/.ensure_dir/rstudio_dataengine-service_ensured'.format(os_user))
        conn.sudo('''R -e "source('/home/{}/.Rprofile')"'''.format(os_user))
    except Exception as err:
        logging.error('Function remove_rstudio_dataengines_kernel error:', str(err))
        traceback.print_exc()
        sys.exit(1)

#following function should be checked if it needed
def configure_data_engine_service_pip(hostname, os_user, keyfile, emr=False):
    try:
        init_datalab_connection(hostname, os_user, keyfile)
        # datalab.common_lib.manage_pkg('-y install', 'remote', 'python3-pip')
        if not exists(conn, '/usr/bin/pip3') and conn.sudo("python3.9 -V 2>/dev/null | awk '{print $2}'").stdout:
            conn.sudo('ln -s /usr/bin/pip-3.9 /usr/bin/pip3')
        elif not exists(conn, '/usr/bin/pip3') and conn.sudo("python3.8 -V 2>/dev/null | awk '{print $2}'").stdout:
            conn.sudo('ln -s /usr/bin/pip-3.8 /usr/bin/pip3')
        elif not exists(conn, '/usr/bin/pip3') and conn.sudo("python3.7 -V 2>/dev/null | awk '{print $2}'").stdout:
            conn.sudo('ln -s /usr/bin/pip-3.7 /usr/bin/pip3')
        elif not exists(conn, '/usr/bin/pip3') and conn.sudo("python3.6 -V 2>/dev/null | awk '{print $2}'").stdout:
            conn.sudo('ln -s /usr/bin/pip-3.6 /usr/bin/pip3')
        elif not exists(conn, '/usr/bin/pip3') and conn.sudo("python3.5 -V 2>/dev/null | awk '{print $2}'").stdout:
            conn.sudo('ln -s /usr/bin/pip-3.5 /usr/bin/pip3')
        if emr:
            conn.sudo('pip3 install -U pip=={}'.format(os.environ['conf_pip_version']))
            conn.sudo('ln -s /usr/local/bin/pip3.7 /bin/pip3.7')
        conn.sudo('''bash -c -l 'echo "export PATH=$PATH:/usr/local/bin" >> /etc/profile' ''')
        conn.sudo('bash -c -l "source /etc/profile"')
        conn.run('bash -c -l "source /etc/profile"')
        conn.close()
    except Exception as err:
        logging.error('Function configure_data_engine_service_pip error:', str(err))
        traceback.print_exc()
        sys.exit(1)


## following function should be removed after configurion kernels connection via live for all computation resources:
def dataengine_dir_prepare(cluster_dir):
    try:
        subprocess.run('mkdir -p ' + cluster_dir, shell=True, check=True)
    except Exception as err:
        logging.error('Function dataengine_dir_prepare error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def prepare(dataengine_service_dir, yarn_dir):
    try:
        subprocess.run('mkdir -p ' + dataengine_service_dir, shell=True, check=True)
        subprocess.run('mkdir -p ' + yarn_dir, shell=True, check=True)
        subprocess.run('sudo mkdir -p /opt/python/', shell=True, check=True)
        result = os.path.exists(dataengine_service_dir + 'usr/')
        return result
    except Exception as err:
        logging.error('Function prepare error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def configuring_notebook(dataengine_service_version):
    try:
        jars_path = '/opt/' + dataengine_service_version + '/jars/'
        subprocess.run("""sudo bash -c "find """ + jars_path + """ -name '*netty*' | xargs rm -f" """, shell=True,
                       check=True)
    except Exception as err:
        logging.error('Function configuring_notebook error:', str(err))
        traceback.print_exc()
        sys.exit(1)


def find_cluster_kernels():
    try:
        de = [i for i in conn.sudo(
            '''bash -l -c 'find /opt/ -maxdepth 1 -name "*-de-*" -type d | rev | cut -f 1 -d "/" | rev | xargs -r' ''').stdout.replace(
            '\n', '').split(' ') if i != '']
        des = [i for i in conn.sudo(
            '''bash -l -c 'find /opt/ -maxdepth 2 -name "*-des-*" -type d | rev | cut -f 1,2 -d "/" | rev | xargs -r' ''').stdout.replace(
            '\n', '').split(' ') if i != '']
        return (de, des)
    except Exception as err:
        logging.error('Function find_cluster_kernels error:', str(err))
        traceback.print_exc()
        sys.exit(1)


# functions for jupyter deeplearning and tensor notebooks
def configure_jupyter(os_user, jupyter_conf_file, templates_dir, jupyter_version, exploratory_name):
    if not exists(conn, '/home/' + os_user + '/.ensure_dir/jupyter_ensured'):
        try:
            if os.environ['conf_deeplearning_cloud_ami'] == 'false' or os.environ['application'] != 'deeplearning':
                conn.sudo('pip3 install notebook=={} --no-cache-dir'.format(jupyter_version))
                conn.sudo('pip3 install jupyter MarkupSafe==2.0.1 --no-cache-dir') # requires investigation
                conn.sudo('rm -rf {}'.format(jupyter_conf_file))
            elif os.environ['application'] != 'tensor':
                conn.sudo('-i pip3 install environment_kernels')
            #if os.environ['conf_cloud_provider'] == 'aws' and os.environ['application'] == 'deeplearning': #should be checked if for other applications any files have owner root:root in datalab-user homefolder and where it is changed to root:root on deeplearning
            #    conn.sudo( pip3 install flask'chown -R {0}:{0} /home/{0}/.local'.format(os_user))
            conn.run('jupyter notebook --generate-config --config {}'.format(jupyter_conf_file))
            conn.run('mkdir -p ~/.jupyter/custom/')
            conn.run('echo "#notebook-container { width: auto; }" > ~/.jupyter/custom/custom.css')
            conn.sudo('echo "c.NotebookApp.ip = \'0.0.0.0\'" >> {}'.format(jupyter_conf_file))
            conn.sudo('echo "c.NotebookApp.base_url = \'/{0}/\'" >> {1}'.format(exploratory_name, jupyter_conf_file))
            conn.sudo('echo c.NotebookApp.open_browser = False >> {}'.format(jupyter_conf_file))
            conn.sudo('echo \'c.NotebookApp.cookie_secret = b"{0}"\' >> {1}'.format(id_generator(), jupyter_conf_file))
            conn.sudo('''echo "c.NotebookApp.token = u''" >> {}'''.format(jupyter_conf_file))
            conn.sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> {}'.format(jupyter_conf_file))
            if os.environ['conf_deeplearning_cloud_ami'] == 'true' and os.environ['application'] == 'deeplearning':
                conn.sudo(
                    '''echo "c.NotebookApp.kernel_spec_manager_class = 'environment_kernels.EnvironmentKernelSpecManager'" >> {}'''.format(
                        jupyter_conf_file))
                conn.sudo(
                    '''echo "c.EnvironmentKernelSpecManager.conda_env_dirs=['/home/ubuntu/anaconda3/envs']" >> {}'''.format(
                        jupyter_conf_file))
            conn.put(templates_dir + 'jupyter-notebook.service', '/tmp/jupyter-notebook.service')
            conn.sudo("chmod 644 /tmp/jupyter-notebook.service")
            if os.environ['application'] == 'tensor':
                conn.sudo(
                    "sed -i '/ExecStart/s|-c \"|-c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64; |g' /tmp/jupyter-notebook.service")
            elif os.environ['application'] == 'deeplearning' and os.environ['conf_deeplearning_cloud_ami'] == 'false':
                conn.sudo(
                    "sed -i '/ExecStart/s|-c \"|-c \"export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/cudnn/lib64:/usr/local/cuda/lib64:/usr/lib64/openmpi/lib: ; export PYTHONPATH=/home/" + os_user +
                    "/caffe/python:/home/" + os_user + "/pytorch/build:$PYTHONPATH ; |g' /tmp/jupyter-notebook.service")
            conn.sudo("sed -i 's|CONF_PATH|{}|' /tmp/jupyter-notebook.service".format(jupyter_conf_file))
            conn.sudo("sed -i 's|OS_USR|{}|' /tmp/jupyter-notebook.service".format(os_user))
            if os.environ['application'] == 'deeplearning' and os.environ['conf_cloud_provider'] == 'azure':
                java_home = conn.run("update-alternatives --query java | grep -o --color=never \'/.*/java-11.*/bin/java\'").stdout.splitlines()[0]
            else:
                java_home = conn.run("update-alternatives --query java | grep -o --color=never \'/.*/java-8.*/jre\'").stdout.splitlines()[0]
            conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"JAVA_HOME={}\"\'  /tmp/jupyter-notebook.service'.format(
                java_home))
            conn.sudo('\cp /tmp/jupyter-notebook.service /etc/systemd/system/jupyter-notebook.service')
            conn.sudo('chown -R {0}:{0} /home/{0}/.local'.format(os_user))
            conn.sudo('mkdir -p /mnt/var')
            conn.sudo('chown {0}:{0} /mnt/var'.format(os_user))
            if os.environ['application'] == 'jupyter' or os.environ['application'] == 'deeplearning':
                try:
                    conn.sudo('jupyter-kernelspec remove -f python3 || echo "Such kernel doesnt exists"')
                    conn.sudo('jupyter-kernelspec remove -f python2 || echo "Such kernel doesnt exists"')
                except Exception as err:
                    logging.error('Error:', str(err))
            conn.sudo("systemctl daemon-reload")
            conn.sudo("systemctl enable jupyter-notebook")
            conn.sudo("systemctl start jupyter-notebook")
            conn.sudo('touch /home/{}/.ensure_dir/jupyter_ensured'.format(os_user))
        except Exception as err:
            logging.error('Function configure_jupyter error:', str(err))
            traceback.print_exc()
            sys.exit(1)
    else:
        try:
            conn.sudo(
                'sed -i "s/c.NotebookApp.base_url =.*/c.NotebookApp.base_url = \'\/{0}\/\'/" {1}'.format(
                    exploratory_name, jupyter_conf_file))
            conn.sudo("systemctl restart jupyter-notebook")
        except Exception as err:
            logging.error('Function configure_jupyter error:', str(err))
            traceback.print_exc()
            sys.exit(1)


# jupyterlab
def configure_jupyterlab(os_user, jupyterlab_conf_file, templates_dir, jupyterlab_version, exploratory_name):
    if not exists(conn, '/home/' + os_user + '/.ensure_dir/jupyterlab_ensured'):
        try:
            conn.sudo('pip3 install jupyterlab --no-cache-dir')  # create external var with version
            conn.sudo('rm -rf {}'.format(jupyterlab_conf_file))
            conn.run('jupyter lab --generate-config')
            conn.sudo('echo "c.NotebookApp.ip = \'0.0.0.0\'" >> {}'.format(jupyterlab_conf_file))
            conn.sudo('echo "c.NotebookApp.base_url = \'/{0}/\'" >> {1}'.format(exploratory_name, jupyterlab_conf_file))
            conn.sudo('echo c.NotebookApp.open_browser = False >> {}'.format(jupyterlab_conf_file))
            conn.sudo('echo \'c.NotebookApp.cookie_secret = b"{0}"\' >> {1}'.format(id_generator(), jupyterlab_conf_file))
            conn.sudo('''echo "c.NotebookApp.token = u''" >> {}'''.format(jupyterlab_conf_file))
            conn.sudo('echo \'c.KernelSpecManager.ensure_native_kernel = False\' >> {}'.format(jupyterlab_conf_file))
            conn.put(templates_dir + 'jupyterlab-notebook.service', '/tmp/jupyterlab-notebook.service')
            if os.environ['application'] == 'deeplearning' and os.environ['conf_cloud_provider'] == 'azure':
                java_home = conn.run("update-alternatives --query java | grep -o --color=never \'/.*/java-11.*/bin/java\'").stdout.splitlines()[0]
            else:
                java_home = conn.run("update-alternatives --query java | grep -o --color=never \'/.*/java-8.*/jre\'").stdout.splitlines()[0]
            # conn.sudo('sed -i \'/\[Service\]/ a\Environment=\"JAVA_HOME={}\"\'  /tmp/jupyterlab-notebook.service'.format(
            #    java_home))
            conn.sudo('cp /tmp/jupyterlab-notebook.service /etc/systemd/system/jupyterlab-notebook.service')
            conn.sudo("systemctl daemon-reload")
            conn.sudo("systemctl enable jupyterlab-notebook")
            conn.sudo("systemctl start jupyterlab-notebook")
            conn.sudo('touch /home/{}/.ensure_dir/jupyterlab_ensured'.format(os_user))
        except Exception as err:
            logging.error('Function configure_jupyterlab error:', str(err))
            traceback.print_exc()
            sys.exit(1)
    else:
        try:
            conn.sudo(
                'sed -i "s/c.NotebookApp.base_url =.*/c.NotebookApp.base_url = \'\/{0}\/\'/" {1}'.format(
                    exploratory_name, jupyterlab_conf_file))
            conn.sudo("systemctl restart jupyterlab-notebook")
        except Exception as err:
            logging.error('Function configure_jupyterlab error:', str(err))
            traceback.print_exc()
            sys.exit(1)


def ensure_py3spark_local_kernel(os_user, py3spark_local_path_dir, templates_dir, spark_version, python_venv_path,
                                 python_venv_version):
    if not exists(conn, '/home/' + os_user + '/.ensure_dir/py3spark_local_kernel_ensured'):
        try:
            conn.sudo('mkdir -p ' + py3spark_local_path_dir)
            conn.sudo('touch ' + py3spark_local_path_dir + 'kernel.json')
            conn.put(templates_dir + 'py3spark_local_template.json', '/tmp/py3spark_local_template.json')
            conn.sudo('''bash -l -c "sed -i \"s:PY4J:$(find /opt/spark/ -name '*py4j*.zip'):g\" /tmp/py3spark_local_template.json" ''')
            conn.sudo('sed -i "s|PYTHON_VENV_PATH|' + python_venv_path + '|g" /tmp/py3spark_local_template.json')
            conn.sudo('sed -i "s|PYTHON_VENV_VERSION|' + python_venv_version + '|g" /tmp/py3spark_local_template.json')
            conn.sudo('sed -i "s|PYTHON_VENV_SHORT_VERSION|' + python_venv_version[:3] + '|g" /tmp/py3spark_local_template.json')
            conn.sudo('sed -i "s|SP_VER|' + spark_version + '|g" /tmp/py3spark_local_template.json')
            conn.sudo(
                'sed -i \'/PYTHONPATH\"\:/s|\(.*\)"|\\1/home/{0}/caffe/python:/home/{0}/pytorch/build:"|\' /tmp/py3spark_local_template.json'.format(
                    os_user))
            conn.sudo('\cp /tmp/py3spark_local_template.json ' + py3spark_local_path_dir + 'kernel.json')
            conn.sudo('touch /home/' + os_user + '/.ensure_dir/py3spark_local_kernel_ensured')
        except Exception as err:
            logging.error('Function ensure_py3spark_local_kernel error:', str(err))
            traceback.print_exc()
            sys.exit(1)


def ensure_toree_local_kernel(os_user, toree_link, scala_kernel_path, files_dir, scala_version, spark_version):
    if not exists(conn, '/home/' + os_user + '/.ensure_dir/toree_local_kernel_ensured'):
        try:
            conn.sudo('pip install ' + toree_link + ' --no-cache-dir')
            conn.sudo('ln -s /opt/spark/ /usr/local/spark')
            conn.sudo('jupyter toree install')
            conn.sudo('mv ' + scala_kernel_path + 'lib/* /tmp/')
            conn.put(files_dir + 'toree-assembly-0.5.0.jar', '/tmp/toree-assembly-0.5.0.jar')
            conn.sudo('mv /tmp/toree-assembly-0.5.0.jar ' + scala_kernel_path + 'lib/')
            conn.sudo(
                'sed -i "s|Apache Toree - Scala|Local Apache Toree - Scala (Scala-' + scala_version +
                ', Spark-' + spark_version + ')|g" ' + scala_kernel_path + 'kernel.json')
            conn.sudo('touch /home/' + os_user + '/.ensure_dir/toree_local_kernel_ensured')
        except Exception as err:
            logging.error('Function ensure_toree_local_kernel error:', str(err))
            traceback.print_exc()
            sys.exit(1)


def add_breeze_library_local(os_user):
    if not exists(conn, '/home/' + os_user + '/.ensure_dir/breeze_local_ensured'):
        try:
            breeze_tmp_dir = '/tmp/breeze_tmp_local/'
            jars_dir = '/opt/jars/'
            conn.sudo('mkdir -p {}'.format(breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze_{0}/{1}/breeze_{0}-{1}.jar -O \
                    {2}breeze_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-natives_{0}/{1}/breeze-natives_{0}-{1}.jar -O \
                    {2}breeze-natives_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-viz_{0}/{1}/breeze-viz_{0}-{1}.jar -O \
                    {2}breeze-viz_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-macros_{0}/{1}/breeze-macros_{0}-{1}.jar -O \
                    {2}breeze-macros_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/scalanlp/breeze-parent_{0}/{1}/breeze-parent_{0}-{1}.jar -O \
                    {2}breeze-parent_{0}-{1}.jar'.format('2.11', '0.12', breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/jfree/jfreechart/{0}/jfreechart-{0}.jar -O \
                    {1}jfreechart-{0}.jar'.format('1.0.19', breeze_tmp_dir))
            conn.sudo('wget https://repo1.maven.org/maven2/org/jfree/jcommon/{0}/jcommon-{0}.jar -O \
                    {1}jcommon-{0}.jar'.format('1.0.24', breeze_tmp_dir))
            conn.sudo('wget --no-check-certificate https://nexus.develop.dlabanalytics.com/repository/packages-public/spark-kernel-brunel-all-{0}.jar -O \
                    {1}spark-kernel-brunel-all-{0}.jar'.format('2.3', breeze_tmp_dir))
            conn.sudo('mv {0}* {1}'.format(breeze_tmp_dir, jars_dir))
            conn.sudo('touch /home/' + os_user + '/.ensure_dir/breeze_local_ensured')
        except Exception as err:
            logging.error('Function add_breeze_library_local error:', str(err))
            traceback.print_exc()
            sys.exit(1)


def remove_unexisting_kernel(os_user):
    if not exists(conn, '/home/{}/.ensure_dir/unexisting_kernel_removed'.format(os_user)):
        try:
            conn.sudo('jupyter-kernelspec remove -f python3')
            conn.sudo('jupyter kernelspec uninstall -y python3', warn=True)
            conn.sudo('touch /home/{}/.ensure_dir/unexisting_kernel_removed'.format(os_user))
        except Exception as err:
            logging.error('Function remove_unexisting_kernel error:', str(err))
            traceback.print_exc()
            sys.exit(1)


# functions for jupyterlab notebook
def ensure_jupyterlab_files(os_user, jupyterlab_dir, jupyterlab_image, jupyter_conf_file, jupyterlab_conf_file,
                            exploratory_name, edge_ip):
    if not exists(conn, jupyterlab_dir):
        try:
            conn.sudo('mkdir {}'.format(jupyterlab_dir))
            conn.put('/root/Dockerfile_jupyterlab', '/tmp/Dockerfile_jupyterlab')
            conn.put('/root/scripts/jupyterlab_run.sh', '/tmp/jupyterlab_run.sh')
            conn.put('/root/scripts/build.sh', '/tmp/build.sh')
            conn.put('/root/scripts/start.sh', '/tmp/start.sh')

            conn.sudo('mv /tmp/jupyterlab_run.sh {}jupyterlab_run.sh'.format(jupyterlab_dir))
            conn.sudo('mv /tmp/Dockerfile_jupyterlab {}Dockerfile_jupyterlab'.format(jupyterlab_dir))
            conn.sudo('mv /tmp/build.sh {}build.sh'.format(jupyterlab_dir))
            conn.sudo('mv /tmp/start.sh {}start.sh'.format(jupyterlab_dir))
            conn.sudo(
                'sed -i \'s/jupyterlab_image/{}/g\' {}Dockerfile_jupyterlab'.format(jupyterlab_image, jupyterlab_dir))
            conn.sudo('sed -i \'s/nb_user/{}/g\' {}start.sh'.format(os_user, jupyterlab_dir))
            conn.sudo('sed -i \'s/CONF_PATH/{}/g\' {}jupyterlab_run.sh'.format(jupyterlab_conf_file, jupyterlab_dir))
            conn.sudo('touch {}'.format(jupyter_conf_file))
            conn.sudo('''bash -l -c "echo 'c.NotebookApp.ip = \\"0.0.0.0\\" ' >> {}" '''.format(jupyter_conf_file))
            conn.sudo('''bash -l -c "echo 'c.NotebookApp.base_url = \\"/{0}/\\"' >> {1}" '''.format(exploratory_name,
                                                                                                    jupyter_conf_file))
            conn.sudo('''bash -l -c 'echo "c.NotebookApp.open_browser = False" >> {}' '''.format(jupyter_conf_file))
            conn.sudo('''bash -l -c "echo 'c.NotebookApp.cookie_secret = b\\"{0}\\"' >> {1}" '''.format(id_generator(),
                                                                                                        jupyter_conf_file))
            conn.sudo('''bash -l -c "echo \\"c.NotebookApp.token = u''\\" >> {}" '''.format(jupyter_conf_file))
            conn.sudo('''bash -l -c 'echo "c.KernelSpecManager.ensure_native_kernel = False" >> {}' '''.format(
                jupyter_conf_file))
            conn.sudo('chown datalab-user:datalab-user /opt')
            conn.sudo(
                '''bash -l -c 'echo -e "Host git.epam.com\n   HostName git.epam.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p\n" > /home/{}/.ssh/config' '''.format(
                    edge_ip, os_user))
            conn.sudo(
                '''bash -l -c 'echo -e "Host github.com\n   HostName github.com\n   ProxyCommand nc -X connect -x {}:3128 %h %p" >> /home/{}/.ssh/config' '''.format(
                    edge_ip, os_user))
        except Exception as err:
            logging.error('Function ensure_jupyterlab_files error:', str(err))
            traceback.print_exc()
            sys.exit(1)
    else:
        try:
            conn.sudo(
                'sed -i "s/c.NotebookApp.base_url =.*/c.NotebookApp.base_url = \'\/{0}\/\'/" {1}'.format(
                    exploratory_name, jupyter_conf_file))
        except Exception as err:
            logging.error('Function ensure_jupyterlab_files error:', str(err))
            traceback.print_exc()
            sys.exit(1)


#functions for zeppelin notebook/dataengine
def install_r_packages(os_user):
    try:
        if not exists(conn, '/home/' + os_user + '/.ensure_dir/r_packages_ensured'):
            conn.sudo('R -e "install.packages(\'devtools\', repos = \'https://cloud.r-project.org\')"')
            conn.sudo('R -e "install.packages(\'knitr\', repos = \'https://cloud.r-project.org\')"')
            conn.sudo('R -e "install.packages(\'ggplot2\', repos = \'https://cloud.r-project.org\')"')
            conn.sudo('R -e "install.packages(\'markdown\', repos = \'https://cloud.r-project.org\')"')
            conn.sudo('R -e "install.packages(c(\'devtools\',\'mplot\', \'googleVis\'), '
                      'repos = \'https://cloud.r-project.org\'); require(devtools); install_github(\'ramnathv/rCharts\')"')
            conn.sudo('R -e \'install.packages("versions", repos="https://cloud.r-project.org", dep=TRUE)\'')
            conn.sudo('touch /home/' + os_user + '/.ensure_dir/r_packages_ensured')
            conn.sudo("systemctl stop zeppelin-notebook")
            conn.sudo("systemctl daemon-reload")
            conn.sudo("systemctl enable zeppelin-notebook")
            conn.sudo("systemctl start zeppelin-notebook")
    except Exception as err:
        logging.error('Function install_r_packages error:', str(err))
        traceback.print_exc()
        sys.exit(1)

def update_zeppelin_interpreters(multiple_clusters, r_enabled, interpreter_mode='remote'):
    try:
        interpreters_config = '/opt/zeppelin/conf/interpreter.json'
        local_interpreters_config = '/tmp/interpreter.json'
        if interpreter_mode != 'remote':
            conn.get(local_interpreters_config, local_interpreters_config)
        if multiple_clusters == 'true':
            groups = [{"class": "org.apache.zeppelin.livy.LivySparkInterpreter", "name": "spark"},
                      {"class": "org.apache.zeppelin.livy.LivyPySparkInterpreter", "name": "pyspark"},
                      {"class": "org.apache.zeppelin.livy.LivyPySpark3Interpreter", "name": "pyspark3"},
                      {"class": "org.apache.zeppelin.livy.LivySparkSQLInterpreter", "name": "sql"}]
            if r_enabled:
                groups.append({"class": "org.apache.zeppelin.livy.LivySparkRInterpreter", "name": "sparkr"})
        else:
            groups = [{"class": "org.apache.zeppelin.spark.SparkInterpreter", "name": "spark"},
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
            conn.put(local_interpreters_config, local_interpreters_config)
            conn.sudo('cp -f {0} {1}'.format(local_interpreters_config, interpreters_config))
            conn.sudo('systemctl restart zeppelin-notebook')
        else:
            with open(interpreters_config, 'w') as f:
                f.write(json.dumps(data, indent=2))
            subprocess.run('sudo systemctl restart zeppelin-notebook', shell=True, check=True)
    except Exception as err:
        logging.error('Function update_zeppelin_interpreters error:', str(err))
        traceback.print_exc()
        sys.exit(1)


#functions for superset notebook (rewrite require)
def configure_superset(os_user, keycloak_auth_server_url, keycloak_realm_name, keycloak_client_id,
                       keycloak_client_secret, edge_instance_private_ip, edge_instance_public_ip, superset_name):
    logging.info('Superset configuring')
    try:
        if not exists(conn, '/home/{}/superset'.format(os_user)):
            conn.sudo(
                '''bash -c 'cd /home/{} && wget https://github.com/apache/incubator-superset/archive/{}.tar.gz' '''.format(
                    os_user, os.environ['notebook_superset_version']))
            conn.sudo('''bash -c 'cd /home/{} && tar -xzf {}.tar.gz' '''.format(os_user, os.environ[
                'notebook_superset_version']))
            conn.sudo('''bash -c 'cd /home/{} && ln -sf superset-{} superset' '''.format(os_user,
                                                                                         os.environ['notebook_superset_version']))
        if not exists(conn, '/tmp/superset-notebook_installed'):
            conn.sudo('mkdir -p /opt/datalab/templates')
            conn.local('cd  /root/templates; tar -zcvf /tmp/templates.tar.gz .')
            conn.put('/tmp/templates.tar.gz', '/tmp/templates.tar.gz')
            conn.sudo('tar -zxvf /tmp/templates.tar.gz -C /opt/datalab/templates')
            conn.sudo('sed -i \'s/OS_USER/{}/g\' /opt/datalab/templates/.env'.format(os_user))
            proxy_string = '{}:3128'.format(edge_instance_private_ip)
            conn.sudo('sed -i \'s|KEYCLOAK_AUTH_SERVER_URL|{}|g\' /opt/datalab/templates/id_provider.json'.format(
                keycloak_auth_server_url))
            conn.sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/datalab/templates/id_provider.json'.format(
                keycloak_realm_name))
            conn.sudo('sed -i \'s/CLIENT_ID/{}/g\' /opt/datalab/templates/id_provider.json'.format(keycloak_client_id))
            conn.sudo('sed -i \'s/CLIENT_SECRET/{}/g\' /opt/datalab/templates/id_provider.json'.format(
                keycloak_client_secret))
            conn.sudo('sed -i \'s/PROXY_STRING/{}/g\' /opt/datalab/templates/docker-compose.yml'.format(proxy_string))
            conn.sudo('sed -i \'s|KEYCLOAK_AUTH_SERVER_URL|{}|g\' /opt/datalab/templates/superset_config.py'.format(
                keycloak_auth_server_url))
            conn.sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/datalab/templates/superset_config.py'.format(
                keycloak_realm_name))
            conn.sudo('sed -i \'s/OS_USER/{}/g\' /opt/datalab/templates/superset_config.py'.format(os_user))
            conn.sudo(
                'sed -i \'s/EDGE_IP/{}/g\' /opt/datalab/templates/superset_config.py'.format(edge_instance_public_ip))
            conn.sudo('sed -i \'s/SUPERSET_NAME/{}/g\' /opt/datalab/templates/superset_config.py'.format(superset_name))
            conn.sudo('cp -f /opt/datalab/templates/.env /home/{}/superset/docker/'.format(os_user))
            conn.sudo(
                'cp -f /opt/datalab/templates/docker-compose.yml /home/{}/superset/'.format(
                    os_user))
            conn.sudo(
                'cp -f /opt/datalab/templates/id_provider.json /home/{}/superset/docker/'.format(
                    os_user))
            conn.sudo(
                'cp -f /opt/datalab/templates/requirements-extra.txt /home/{}/superset/requirements/'.format(
                    os_user))
            conn.sudo(
                'cp -f /opt/datalab/templates/superset_config.py /home/{}/superset/docker/pythonpath_dev/'.format(
                    os_user))
            conn.sudo(
                'cp -f /opt/datalab/templates/keycloak_security_manager.py /home/{}/superset/docker/pythonpath_dev/'.format(
                    os_user))
            conn.sudo('cp -f /opt/datalab/templates/docker-init.sh /home/{}/superset/docker/'.format(
                os_user))
            conn.sudo('touch /tmp/superset-notebook_installed')
    except Exception as err:
        logging.error('Function configure_superset error:', str(err))
        traceback.print_exc()
        sys.exit(1)


# function move to debian and check if needed
def configure_docker(os_user):
    try:
        if not exists(conn, '/home/' + os_user + '/.ensure_dir/docker_ensured'):
            docker_version = os.environ['ssn_docker_version']
            conn.sudo('curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -')
            conn.sudo('add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) \
                  stable"')
            # datalab.common_lib.manage_pkg('update', 'remote', '')
            conn.sudo('apt-get update')
            conn.sudo('apt-cache policy docker-ce')
            # datalab.common_lib.manage_pkg('-y install', 'remote', 'docker-ce=5:{}~3-0~ubuntu-focal'.format(docker_version))
            conn.sudo('apt-get install -y docker-ce=5:{}~3-0~ubuntu-focal'.format(docker_version))
            conn.sudo('touch /home/{}/.ensure_dir/docker_ensured'.format(os_user))
    except Exception as err:
        print('Failed to configure Docker:', str(err))
        sys.exit(1)

def ensure_docker_compose(os_user):
    try:
        configure_docker(os_user)
        if not exists(conn, '/home/{}/.ensure_dir/docker_compose_ensured'.format(os_user)):
            docker_compose_version = "1.24.1"
            conn.sudo(
                'curl -L https://github.com/docker/compose/releases/download/{}/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose'.format(
                    docker_compose_version))
            conn.sudo('chmod +x /usr/local/bin/docker-compose')
            conn.sudo('touch /home/{}/.ensure_dir/docker_compose_ensured'.format(os_user))
        conn.sudo('systemctl daemon-reload')
        conn.sudo('systemctl restart docker')
        return True
    except:
        return False



#function is just for azure, need check if it's needed
def set_git_proxy(os_user, hostname, keyfile, proxy_host):
    init_datalab_connection(hostname, os_user, keyfile)
    conn.run('git config --global http.proxy {}'.format(proxy_host))
    conn.run('git config --global https.proxy {}'.format(proxy_host))
    conn.close()


#function should be checked whether it needed
def update_pyopenssl_lib(os_user):
    if not exists(conn, '/home/{}/.ensure_dir/pyopenssl_updated'.format(os_user)):
        try:
            if exists(conn, '/usr/bin/pip3'):
                conn.sudo('pip3 install -U pyopenssl')
            conn.sudo('touch /home/{}/.ensure_dir/pyopenssl_updated'.format(os_user))
        except:
            sys.exit(1)

def get_hdinsight_headnode_private_ip(os_user, cluster_name, keyfile):
    init_datalab_connection('{}-ssh.azurehdinsight.net'.format(cluster_name), os_user, keyfile)
    headnode_private_ip = conn.sudo("cat /etc/hosts | grep headnode | awk '{print $1}'").stdout
    conn.close()
    return headnode_private_ip
