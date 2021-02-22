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
import sys
import os
import subprocess

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
                    if conn.sudo('pgrep yum -a && echo "busy" || echo "ready"') == 'busy':
                        counter += 1
                        time.sleep(10)
                    else:
                        allow = True
                        conn.sudo('yum {0} {1}'.format(command, requisites))
                elif environment == 'local':
                    if subprocess.run('sudo pgrep yum -a && echo "busy" || echo "ready"', capture_output=True, shell=True, check=True) == 'busy':
                        counter += 1
                        time.sleep(10)
                    else:
                        allow = True
                        subprocess.run('sudo yum {0} {1}'.format(command, requisites), capture_output=True, shell=True, check=True)
                else:
                    print('Wrong environment')
    except:
        sys.exit(1)

def ensure_pkg(user, requisites='git vim gcc python-devel openssl-devel nmap libffi libffi-devel unzip libxml2-devel'):
    try:
        if not exists(conn,'/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
            print("Updating repositories and installing requested tools: {}".format(requisites))
            if conn.sudo("systemctl list-units  --all | grep firewalld | awk '{print $1}'") != '':
                conn.sudo('systemctl disable firewalld.service')
                conn.sudo('systemctl stop firewalld.service')
            conn.sudo('setenforce 0')
            conn.sudo("sed -i '/^SELINUX=/s/SELINUX=.*/SELINUX=disabled/g' /etc/selinux/config")
            mirror = 'mirror.centos.org'
            with conn.cd('/etc/yum.repos.d/'):
                conn.sudo('echo "[Centos-repo]" > centOS-base.repo')
                conn.sudo('echo "name=Centos 7 Repository" >> centOS-base.repo')
                conn.sudo('echo "baseurl=http://{}/centos/7/os/x86_64/" >> centOS-base.repo'.format(mirror))
                conn.sudo('echo "enabled=1" >> centOS-base.repo')
                conn.sudo('echo "gpgcheck=1" >> centOS-base.repo')
                conn.sudo('echo "gpgkey=http://{}/centos/7/os/x86_64/RPM-GPG-KEY-CentOS-7" >> centOS-base.repo'.format(mirror))
            conn.sudo('yum-config-manager --enable rhui-REGION-rhel-server-optional')
            manage_pkg('update-minimal --security -y', 'remote', '')
            manage_pkg('-y install', 'remote', 'wget')
            conn.sudo('wget --no-check-certificate https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm')
            conn.sudo('rpm -ivh epel-release-latest-7.noarch.rpm')
            manage_pkg('repolist', 'remote', '')
            manage_pkg('-y install', 'remote', 'python3-pip gcc')
            conn.sudo('rm -f epel-release-latest-7.noarch.rpm')
            conn.sudo('export LC_ALL=C')
            manage_pkg('-y install', 'remote', requisites)
            conn.sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
    except:
        sys.exit(1)


def change_pkg_repos():
    if not exists(conn,'/tmp/pkg_china_ensured'):
        conn.put('/root/files/sources.list', '/tmp/sources.list')
        conn.sudo('mv /tmp/sources.list  /etc/yum.repos.d/CentOS-Base-aliyun.repo')
        conn.sudo('touch /tmp/pkg_china_ensured')


def find_java_path_remote():
    java_path = conn.sudo("alternatives --display java | grep 'slave jre: ' | awk '{print $3}'").stdout
    return java_path


def find_java_path_local():
    java_path = subprocess.run("alternatives --display java | grep 'slave jre: ' | awk '{print $3}'", capture_output=True, shell=True, check=True)
    return java_path


def ensure_ntpd(user, edge_private_ip=''):
    try:
        if not exists(conn,'/home/{}/.ensure_dir/ntpd_ensured'.format(user)):
            conn.sudo('systemctl disable chronyd')
            manage_pkg('-y install', 'remote', 'ntp')
            conn.sudo('echo "tinker panic 0" >> /etc/ntp.conf')
            conn.sudo('systemctl start ntpd')
            if os.environ['conf_resource'] != 'ssn' and os.environ['conf_resource'] != 'edge':
                conn.sudo('echo "server {} prefer iburst" >> /etc/ntp.conf'.format(edge_private_ip))
                conn.sudo('systemctl restart ntpd')
            conn.sudo('systemctl enable ntpd')
            conn.sudo('touch /home/{}/.ensure_dir/ntpd_ensured'.format(user))
    except:
        sys.exit(1)


def ensure_java(user):
    try:
        if not exists(conn,'/home/{}/.ensure_dir/java_ensured'.format(user)):
            manage_pkg('-y install', 'remote', 'java-1.8.0-openjdk-devel')
            conn.sudo('touch /home/{}/.ensure_dir/java_ensured'.format(user))
    except:
        sys.exit(1)


def ensure_step(user):
    try:
        if not exists(conn,'/home/{}/.ensure_dir/step_ensured'.format(user)):
            manage_pkg('-y install', 'remote', 'wget')
            conn.sudo('wget https://github.com/smallstep/cli/releases/download/v0.13.3/step_0.13.3_linux_amd64.tar.gz '
                 '-O /tmp/step_0.13.3_linux_amd64.tar.gz')
            conn.sudo('tar zxvf /tmp/step_0.13.3_linux_amd64.tar.gz -C /tmp/')
            conn.sudo('mv /tmp/step_0.13.3/bin/step /usr/bin/')
            conn.sudo('touch /home/{}/.ensure_dir/step_ensured'.format(user))
    except:
        sys.exit(1)

def install_certbot(os_family):
    try:
        print('Installing Certbot')
        print('Redhat is not supported yet. Skipping....')
    except Exception as err:
        traceback.print_exc()
        print('Failed Certbot install: ' + str(err))
        sys.exit(1)

def run_certbot(domain_name, email=''):
    try:
        print('Running  Certbot')
        print('Redhat is not supported yet. Skipping....')
    except Exception as err:
        traceback.print_exc()
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)

def configure_nginx_LE(domain_name):
    try:
        print('Redhat is not supported yet. Skipping....')
    except Exception as err:
        traceback.print_exc()
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)