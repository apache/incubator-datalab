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


def ensure_pkg(user, requisites='git vim gcc python-devel openssl-devel nmap libffi libffi-devel unzip libxml2-devel'):
    try:
        if not exists('/home/{}/.ensure_dir/pkg_upgraded'.format(user)):
            print("Updating repositories and installing requested tools: {}".format(requisites))
            if sudo("systemctl list-units  --all | grep firewalld | awk '{print $1}'") != '':
                sudo('systemctl disable firewalld.service')
                sudo('systemctl stop firewalld.service')
            sudo('setenforce 0')
            sudo("sed -i '/^SELINUX=/s/SELINUX=.*/SELINUX=disabled/g' /etc/selinux/config")
            mirror = 'mirror.centos.org'
            with cd('/etc/yum.repos.d/'):
                sudo('echo "[Centos-repo]" > centOS-base.repo')
                sudo('echo "name=Centos 7 Repository" >> centOS-base.repo')
                sudo('echo "baseurl=http://{}/centos/7/os/x86_64/" >> centOS-base.repo'.format(mirror))
                sudo('echo "enabled=1" >> centOS-base.repo')
                sudo('echo "gpgcheck=1" >> centOS-base.repo')
                sudo('echo "gpgkey=http://{}/centos/7/os/x86_64/RPM-GPG-KEY-CentOS-7" >> centOS-base.repo'.format(mirror))
            sudo('yum-config-manager --enable rhui-REGION-rhel-server-optional')
            sudo('yum update-minimal --security -y')
            sudo('yum -y install wget')
            sudo('wget --no-check-certificate https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm')
            sudo('rpm -ivh epel-release-latest-7.noarch.rpm')
            sudo('yum repolist')
            sudo('yum -y install python-pip gcc')
            sudo('rm -f epel-release-latest-7.noarch.rpm')
            sudo('export LC_ALL=C')
            sudo('yum -y install ' + requisites)
            sudo('touch /home/{}/.ensure_dir/pkg_upgraded'.format(user))
    except:
        sys.exit(1)


def change_pkg_repos():
    if not exists('/tmp/pkg_china_ensured'):
        put('/root/files/sources.list', '/tmp/sources.list')
        sudo('mv /tmp/sources.list  /etc/yum.repos.d/CentOS-Base-aliyun.repo')
        sudo('touch /tmp/pkg_china_ensured')


def find_java_path_remote():
    java_path = sudo("alternatives --display java | grep 'slave jre: ' | awk '{print $3}'")
    return java_path


def find_java_path_local():
    java_path = local("alternatives --display java | grep 'slave jre: ' | awk '{print $3}'", capture=True)
    return java_path


def ensure_ntpd(user, edge_private_ip=''):
    try:
        if not exists('/home/{}/.ensure_dir/ntpd_ensured'.format(user)):
            sudo('systemctl disable chronyd')
            sudo('yum -y install ntp')
            sudo('echo "tinker panic 0" >> /etc/ntp.conf')
            sudo('systemctl start ntpd')
            if os.environ['conf_resource'] != 'ssn' and os.environ['conf_resource'] != 'edge':
                sudo('echo "server {} prefer iburst" >> /etc/ntp.conf'.format(edge_private_ip))
                sudo('systemctl restart ntpd')
            sudo('systemctl enable ntpd')
            sudo('touch /home/{}/.ensure_dir/ntpd_ensured'.format(user))
    except:
        sys.exit(1)