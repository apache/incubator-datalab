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

from fabric import *
from patchwork.files import exists
from patchwork import files
import sys
import os
import time
import subprocess
import datalab.fab

def manage_pkg(command, environment, requisites):
    try:
        allow = False
        counter = 0
        while not allow:
            if counter > 60:
                print("Notebook is broken please recreate it.")
                sys.exit(1)
            else:
                print('Package manager is:')
                if environment == 'remote':
                    if datalab.fab.conn.sudo('pgrep "^apt" -a && echo "busy" || echo "ready"') == 'busy' or datalab.fab.conn.sudo('pgrep "^dpkg" -a && echo "busy" || echo "ready"') == 'busy':
                        counter += 1
                        time.sleep(10)
                    else:
                        try:
                            error_parser = "frontend is locked|locked|not get lock|unavailable"

                            datalab.fab.conn.sudo('dpkg --configure -a 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({0})" /tmp/tee.tmp > '
                                                  '/tmp/dpkg.log; then echo "no_error" > /tmp/dpkg.log;fi'.format(error_parser))
                            err = datalab.fab.conn.sudo('cat /tmp/dpkg.log').stdout.replace('\n','')
                            count = 0
                            while 'no_error' not in err and count < 10:
                                pid = datalab.fab.conn.sudo('lsof /var/lib/dpkg/lock-frontend | grep dpkg | awk \'{print $2}\'').stdout.replace('\n','')
                                if pid != '':
                                    datalab.fab.conn.sudo('kill -9 {}'.format(pid))
                                    datalab.fab.conn.sudo('rm -f /var/lib/dpkg/lock-frontend')
                                    pid = datalab.fab.conn.sudo('lsof /var/lib/dpkg/lock | grep dpkg | awk \'{print $2}\'').stdout.replace('\n','')
                                if pid != '':
                                    datalab.fab.conn.sudo('kill -9 {}'.format(pid))
                                    datalab.fab.conn.sudo('rm -f /var/lib/dpkg/lock')
                                datalab.fab.conn.sudo('dpkg --configure -a 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({0})" /tmp/tee.tmp > '
                                     '/tmp/dpkg.log; then echo "no_error" > /tmp/dpkg.log;fi'.format(error_parser))
                                err = datalab.fab.conn.sudo('cat /tmp/dpkg.log').stdout
                                count = count + 1

                            datalab.fab.conn.sudo('apt update 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({0})" /tmp/tee.tmp > '
                                 '/tmp/apt.log; then echo "no_error" > /tmp/apt.log;fi'.format(error_parser))
                            err = datalab.fab.conn.sudo('cat /tmp/apt.log').stdout
                            count = 0
                            while 'no_error' not in err and count < 10:
                                pid = datalab.fab.conn.sudo('lsof /var/lib/apt/lists/lock | grep apt | awk \'{print $2}\'').stdout.replace('\n','')
                                if pid != '':
                                    datalab.fab.conn.sudo('kill -9 {}'.format(pid))
                                    datalab.fab.conn.sudo('rm -f /var/lib/apt/lists/lock')
                                datalab.fab.conn.sudo('apt update 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({0})" /tmp/tee.tmp > '
                                 '/tmp/apt.log; then echo "" > /tmp/apt.log;fi'.format(error_parser))
                                err = datalab.fab.conn.sudo('cat /tmp/apt.log').stdout
                                count = count + 1

                            datalab.fab.conn.sudo('apt-get {0} {1} 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({2})" /tmp/tee.tmp > '
                                 '/tmp/apt-get.log; then echo "no_error" > /tmp/apt-get.log;fi'.format(command, requisites, error_parser))
                            err = datalab.fab.conn.sudo('cat /tmp/apt-get.log').stdout
                            count = 0
                            while 'no_error' not in err and count < 10:
                                datalab.fab.conn.sudo('lsof /var/lib/dpkg/lock')
                                datalab.fab.conn.sudo('lsof /var/lib/apt/lists/lock')
                                datalab.fab.conn.sudo('lsof /var/cache/apt/archives/lock')
                                datalab.fab.conn.sudo('rm -f /var/lib/apt/lists/lock')
                                datalab.fab.conn.sudo('rm -f /var/cache/apt/archives/lock')
                                datalab.fab.conn.sudo('rm -f /var/lib/dpkg/lock')
                                datalab.fab.conn.sudo('apt-get {0} {1} 2>&1 | tee /tmp/tee.tmp; if ! grep -w -E "({2})" /tmp/tee.tmp > '
                                     '/tmp/apt.log; then echo "no_error" > /tmp/apt.log;fi'.format(command, requisites, error_parser))
                                err = datalab.fab.conn.sudo('cat /tmp/apt.log').stdout
                                count = count + 1
                            allow = True
                        except Exception as err:
                            traceback.print_exc()
                            append_result("Failed to manage_pkgs", str(err))
                elif environment == 'local':
                    if subprocess.run('sudo pgrep "^apt" -a && echo "busy" || echo "ready"', capture_output=True, shell=True, check=True) == 'busy':
                        counter += 1
                        time.sleep(10)
                    else:
                        allow = True
                        subprocess.run('sudo apt-get {0} {1}'.format(command, requisites), capture_output=True, shell=True, check=True)
                else:
                    print('Wrong environment')
    except:
        sys.exit(1)

def ensure_pkg(user, requisites='linux-headers-generic python3-pip python3-dev python3-virtualenv '
                                'groff gcc vim less git wget '
                                'libssl-dev unattended-upgrades nmap '
                                'libffi-dev unzip libxml2-dev haveged'):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
            count = 0
            check = False
            while not check:
                if count > 60:
                    print("Repositories are not available. Please, try again later.")
                    sys.exit(1)
                else:
                    try:
                        print("Updating repositories "
                                "and installing requested tools: {}".format(requisites))
                        print("Attempt number " + str(count) + " to install requested tools. Max 60 tries.")
                        manage_pkg('update', 'remote', '')
                        manage_pkg('-y install', 'remote', requisites)
                        datalab.fab.conn.sudo('unattended-upgrades -v')
                        datalab.fab.conn.sudo(
                            'sed -i \'s|APT::Periodic::Unattended-Upgrade "1"|APT::Periodic::Unattended-Upgrade "0"|\' /etc/apt/apt.conf.d/20auto-upgrades')
                        datalab.fab.conn.run('export LC_ALL=C')
                        datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
                        datalab.fab.conn.sudo('systemctl enable haveged')
                        datalab.fab.conn.sudo('systemctl start haveged')
                        if os.environ['conf_cloud_provider'] == 'aws':
                            manage_pkg('-y install --install-recommends', 'remote', 'linux-aws-hwe')
                        check = True
                    except:
                        count += 1
                        time.sleep(50)
    except:
        sys.exit(1)


def renew_gpg_key():
    try:
#        if exists(conn, '/etc/apt/trusted.gpg'):
#            datalab.fab.conn.sudo('mv /etc/apt/trusted.gpg /etc/apt/trusted.bkp')
        datalab.fab.conn.sudo('apt-key update')
    except:
        sys.exit(1)


def change_pkg_repos():
    if not exists(datalab.fab.conn,'/tmp/pkg_china_ensured'):
        datalab.fab.conn.put('/root/files/sources.list', '/tmp/sources.list')
        datalab.fab.conn.sudo('mv /tmp/sources.list /etc/apt/sources.list')
        manage_pkg('update', 'remote', '')
        datalab.fab.conn.sudo('touch /tmp/pkg_china_ensured')


def find_java_path_remote():
    java_path = datalab.fab.conn.sudo("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"").stdout.replace('\n','')
    return java_path


def find_java_path_local():
    java_path = subprocess.run("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"", capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
    return java_path


def ensure_ntpd(user, edge_private_ip=''):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/ntpd_ensured'.format(user)):
            datalab.fab.conn.sudo('timedatectl set-ntp no')
            manage_pkg('-y install', 'remote', 'ntp ntpdate')
            datalab.fab.conn.sudo('bash -c \"echo "tinker panic 0" >> /etc/ntp.conf\"')
            if os.environ['conf_resource'] != 'ssn' and os.environ['conf_resource'] != 'edge':
                datalab.fab.conn.sudo('bash -c \"echo "server {} prefer iburst" >> /etc/ntp.conf\"'.format(edge_private_ip))
            datalab.fab.conn.sudo('systemctl restart ntp')
            datalab.fab.conn.sudo('systemctl enable ntp')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/ntpd_ensured'.format(user))
    except:
        sys.exit(1)


def ensure_java(user):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/java_ensured'.format(user)):
            manage_pkg('-y install', 'remote', 'openjdk-8-jdk')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/java_ensured'.format(user))
    except:
        sys.exit(1)


def ensure_step(user):
    try:
        if not exists(datalab.fab.conn,'/home/{}/.ensure_dir/step_ensured'.format(user)):
            manage_pkg('-y install', 'remote', 'wget')
            datalab.fab.conn.sudo('wget https://github.com/smallstep/cli/releases/download/v0.13.3/step-cli_0.13.3_amd64.deb '
                 '-O /tmp/step-cli_0.13.3_amd64.deb')
            datalab.fab.conn.sudo('dpkg -i /tmp/step-cli_0.13.3_amd64.deb')
            datalab.fab.conn.sudo('touch /home/{}/.ensure_dir/step_ensured'.format(user))
    except:
        sys.exit(1)

def install_certbot(os_family):
    try:
        print('Installing Certbot')
        if os_family == 'debian':
            datalab.fab.conn.sudo('apt-get -y update')
            datalab.fab.conn.sudo('apt-get -y install software-properties-common')
            datalab.fab.conn.sudo('add-apt-repository -y universe')
            datalab.fab.conn.sudo('add-apt-repository -y ppa:certbot/certbot')
            datalab.fab.conn.sudo('apt-get -y update')
            datalab.fab.conn.sudo('apt-get -y install certbot')
        elif os_family == 'redhat':
            print('This OS family is not supported yet')
    except Exception as err:
        traceback.print_exc()
        print('Failed Certbot install: ' + str(err))
        sys.exit(1)

def run_certbot(domain_name, node, email=''):
    try:
        print('Running  Certbot')
        if node == 'ssn':
            datalab.fab.conn.sudo('service nginx stop')
        else:
            datalab.fab.conn.sudo('service openresty stop')
        if email != '':
            datalab.fab.conn.sudo('certbot certonly --standalone -n -d {}.{} -m {} --agree-tos'.format(node, domain_name, email))
        else:
            datalab.fab.conn.sudo('certbot certonly --standalone -n -d {}.{} --register-unsafely-without-email --agree-tos'.format(node, domain_name))
    except Exception as err:
        traceback.print_exc()
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)

def configure_nginx_LE(domain_name, node):
    try:
        server_name_line ='    server_name {}.{};'.format(node, domain_name)
        cert_path_line = '    ssl_certificate  /etc/letsencrypt/live/{}.{}/fullchain.pem;'.format(node, domain_name)
        cert_key_line = '    ssl_certificate_key /etc/letsencrypt/live/{}.{}/privkey.pem;'.format(node, domain_name)
        certbot_service = "ExecStart = /usr/bin/certbot -q renew --pre-hook 'service nginx stop' --post-hook 'service nginx start'"
        certbot_service_path = '/lib/systemd/system/certbot.service'
        if node == 'ssn':
            nginx_config_path = '/etc/nginx/conf.d/nginx_proxy.conf'
        else:
            nginx_config_path = '/usr/local/openresty/nginx/conf/conf.d/proxy.conf'
        datalab.fab.conn.sudo('sed -i "s|.*    server_name .*|{}|" {}'.format(server_name_line, nginx_config_path))
        datalab.fab.conn.sudo('sed -i "s|.*    ssl_certificate .*|{}|" {}'.format(cert_path_line, nginx_config_path))
        datalab.fab.conn.sudo('sed -i "s|.*    ssl_certificate_key .*|{}|" {}'.format(cert_key_line, nginx_config_path))
        datalab.fab.conn.sudo('sed -i "s|.*ExecStart.*|{}|" {}'.format(certbot_service, certbot_service_path))
        if node == 'ssn':
            datalab.fab.conn.sudo('systemctl restart nginx')
        else:
            datalab.fab.conn.sudo('systemctl restart openresty')
    except Exception as err:
        traceback.print_exc()
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)
