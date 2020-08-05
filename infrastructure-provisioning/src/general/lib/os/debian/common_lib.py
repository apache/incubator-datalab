#!/usr/bin/python

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
import sys
import os
import time


def manage_pkg(command, environment, requisites):
    try:
        attempt = 0
        installed = False
        while not installed:
            print('Pkg installation attempt: {}'.format(attempt))
            if attempt > 60:
                print("Notebook is broken please recreate it.")
                sys.exit(1)
            else:
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
                                if sudo('pgrep "^apt" -a && echo "busy" || echo "ready"') == 'busy':
                                    counter += 1
                                    time.sleep(10)
                                else:
                                    allow = True
                                    sudo('apt-get {0} {1}'.format(command, requisites))
                            elif environment == 'local':
                                if local('sudo pgrep "^apt" -a && echo "busy" || echo "ready"', capture=True) == 'busy':
                                    counter += 1
                                    time.sleep(10)
                                else:
                                    allow = True
                                    local('sudo apt-get {0} {1}'.format(command, requisites), capture=True)
                            else:
                                print('Wrong environment')
                    installed = True
                except:
                    print("Will try to install with nex attempt.")
                    sudo('dpkg --configure -a')
                    attempt += 1
    except:
        sys.exit(1)

def ensure_pkg(user, requisites='linux-headers-generic python-pip python-dev '
                                'groff gcc vim less git wget sysv-rc-conf '
                                'libssl-dev unattended-upgrades nmap '
                                'libffi-dev unzip libxml2-dev haveged'):
    try:
        if not exists('/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
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
                        sudo('unattended-upgrades -v')
                        sudo(
                            'sed -i \'s|APT::Periodic::Unattended-Upgrade "1"|APT::Periodic::Unattended-Upgrade "0"|\' /etc/apt/apt.conf.d/20auto-upgrades')
                        sudo('export LC_ALL=C')
                        sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
                        sudo('systemctl enable haveged')
                        sudo('systemctl start haveged')
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
        sudo('mv /etc/apt/trusted.gpg /etc/apt/trusted.bkp')
        sudo('apt-key update')
    except:
        sys.exit(1)


def change_pkg_repos():
    if not exists('/tmp/pkg_china_ensured'):
        put('/root/files/sources.list', '/tmp/sources.list')
        sudo('mv /tmp/sources.list /etc/apt/sources.list')
        manage_pkg('update', 'remote', '')
        sudo('touch /tmp/pkg_china_ensured')


def find_java_path_remote():
    java_path = sudo("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"")
    return java_path


def find_java_path_local():
    java_path = local("sh -c \"update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'\"", capture=True)
    return java_path


def ensure_ntpd(user, edge_private_ip=''):
    try:
        if not exists('/home/{}/.ensure_dir/ntpd_ensured'.format(user)):
            sudo('timedatectl set-ntp no')
            manage_pkg('-y install', 'remote', 'ntp ntpdate')
            sudo('echo "tinker panic 0" >> /etc/ntp.conf')
            if os.environ['conf_resource'] != 'ssn' and os.environ['conf_resource'] != 'edge':
                sudo('echo "server {} prefer iburst" >> /etc/ntp.conf'.format(edge_private_ip))
            sudo('systemctl restart ntp')
            sudo('systemctl enable ntp')
            sudo('touch /home/{}/.ensure_dir/ntpd_ensured'.format(user))
    except:
        sys.exit(1)


def ensure_java(user):
    try:
        if not exists('/home/{}/.ensure_dir/java_ensured'.format(user)):
            manage_pkg('-y install', 'remote', 'openjdk-8-jdk')
            sudo('touch /home/{}/.ensure_dir/java_ensured'.format(user))
    except:
        sys.exit(1)


def ensure_step(user):
    try:
        if not exists('/home/{}/.ensure_dir/step_ensured'.format(user)):
            manage_pkg('-y install', 'remote', 'wget')
            sudo('wget https://github.com/smallstep/cli/releases/download/v0.13.3/step-cli_0.13.3_amd64.deb '
                 '-O /tmp/step-cli_0.13.3_amd64.deb')
            sudo('dpkg -i /tmp/step-cli_0.13.3_amd64.deb')
            sudo('touch /home/{}/.ensure_dir/step_ensured'.format(user))
    except:
        sys.exit(1)

def install_certbot(os_family):
    try:
        print('Installing Certbot')
        if os_family == 'debian':
            sudo('apt-get -y update')
            sudo('apt-get -y install software-properties-common')
            sudo('add-apt-repository -y universe')
            sudo('add-apt-repository -y ppa:certbot/certbot')
            sudo('apt-get -y update')
            sudo('apt-get -y install certbot')
        elif os_family == 'redhat':
            print('This OS family is not supported yet')
    except Exception as err:
        traceback.print_exc()
        print('Failed Certbot install: ' + str(err))
        sys.exit(1)

def run_certbot(domain_name, node, email=''):
    try:
        print('Running  Certbot')
        sudo('service nginx stop')
        if email != '':
            sudo('certbot certonly --standalone -n -d {}.{} -m {}'.format(node, domain_name, email))
        else:
            sudo('certbot certonly --standalone -n -d {}.{} --register-unsafely-without-email --agree-tos'.format(node, domain_name))
    except Exception as err:
        traceback.print_exc()
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)

def configure_nginx_LE(domain_name, node):
    try:
        server_name_line ='    server_name {}.{};'.format(node, domain_name)
        cert_path_line = '    ssl_certificate  /etc/letsencrypt/live/{}.{}/fullchain.pem;'.format(node, domain_name)
        cert_key_line = '    ssl_certificate_key /etc/letsencrypt/live/{}.{}/privkey.pem;'.format(node, domain_name)
        certbot_service = 'ExecStart = /usr/bin/certbot -q renew --pre-hook \"service nginx stop\" --post-hook \"service nginx start\"'
        certbot_service_path = '/lib/systemd/system/certbot.service'
        if node == 'ssn':
            nginx_config_path = '/etc/nginx/conf.d/nginx_proxy.conf'
        else:
            nginx_config_path = '/etc/nginx/conf.d/proxy.conf'
        sudo('sed -i "s|.*    server_name .*|{}|" {}'.format(server_name_line, nginx_config_path))
        sudo('sed -i "s|.*    ssl_certificate .*|{}|" {}'.format(cert_path_line, nginx_config_path))
        sudo('sed -i "s|.*    ssl_certificate_key .*|{}|" {}'.format(cert_key_line, nginx_config_path))
        sudo('sed -i "s|.*ExecStart.*|{}|" {}'.format(certbot_service, certbot_service_path))
        sudo('systemctl restart nginx')
    except Exception as err:
        traceback.print_exc()
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)
