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

import os
import sys
from fabric.api import *
from fabric.contrib.files import exists


def configure_http_proxy_server(config):
    try:
        if not exists('/tmp/http_proxy_ensured'):
            sudo('apt-get -y install squid')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            with open("/tmp/tmpsquid.conf", 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line.replace('PROXY_SUBNET', proxy_subnet))
            put('/tmp/tmpsquid.conf', '/tmp/squid.conf')
            sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            sudo('service squid reload')
            sudo('sysv-rc-conf squid on')
            sudo('touch /tmp/http_proxy_ensured')
        return True
    except:
        return False

def install_nginx_ldap(edge_ip, nginx_version, ldap_ip, ldap_dn, ldap_user_pass, ldap_user):
    try:
        if not os.path.exists('/tmp/nginx_installed'):
            sudo('apt-get install -y wget')
            sudo('apt-get -y install gcc build-essential make zlib1g-dev libpcre++-dev libssl-dev git libldap2-dev')
            sudo('mkdir -p /tmp/nginx_auth_ldap')
            with cd('/tmp/nginx_auth_ldap'):
                sudo('git clone https://github.com/kvspb/nginx-auth-ldap.git')
            sudo('mkdir -p /tmp/src')
            with cd('/tmp/src/'):
                sudo('wget http://nginx.org/download/nginx-{}.tar.gz'.format(nginx_version))
                sudo('tar -xzf nginx-{}.tar.gz'.format(nginx_version))
                sudo('ln -sf nginx-{} nginx'.format(nginx_version))
            with cd('/tmp/src/nginx/'):
                sudo('./configure --user=nginx --group=nginx --prefix=/etc/nginx --sbin-path=/usr/sbin/nginx \
                              --conf-path=/etc/nginx/nginx.conf --pid-path=/run/nginx.pid --lock-path=/run/lock/subsys/nginx \
                              --error-log-path=/var/log/nginx/error.log --http-log-path=/var/log/nginx/access.log \
                              --with-http_gzip_static_module --with-http_stub_status_module --with-http_ssl_module --with-pcre \
                              --with-http_realip_module --with-file-aio --with-ipv6 --with-http_v2_module --with-debug \
                              --without-http_scgi_module --without-http_uwsgi_module --without-http_fastcgi_module --with-http_sub_module \
                              --add-module=/tmp/nginx_auth_ldap/nginx-auth-ldap/')
                sudo('make')
                sudo('make install')
            sudo('useradd -r nginx')
            sudo('rm -f /etc/nginx/nginx.conf')
            sudo('mkdir -p /opt/dlab/templates')
            put('/root/templates', '/opt/dlab', use_sudo=True)
            sudo('sed -i \'s/LDAP_IP/{}/g\' /opt/dlab/templates/nginx.conf'.format(ldap_ip))
            sudo('sed -i \'s/LDAP_DN/{}/g\' /opt/dlab/templates/nginx.conf'.format(ldap_dn))
            sudo('sed -i \'s/LDAP_USER_PASSWORD/{}/g\' /opt/dlab/templates/nginx.conf'.format(ldap_user_pass))
            sudo('sed -i \'s/LDAP_USER/{}/g\' /opt/dlab/templates/nginx.conf'.format(ldap_user))
            sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/dlab/templates/conf.d/proxy.conf'.format(edge_ip))
            sudo('cp /opt/dlab/templates/nginx.conf /etc/nginx/')
            sudo('mkdir /etc/nginx/conf.d')
            sudo('cp /opt/dlab/templates/conf.d/proxy.conf /etc/nginx/conf.d/')
            sudo('mkdir /etc/nginx/locations')
            sudo('cp /opt/dlab/templates/nginx_debian /etc/init.d/nginx')
            sudo('chmod +x /etc/init.d/nginx')
            sudo('systemctl daemon-reload')
            sudo('/etc/init.d/nginx start')
            #sudo('update-rc.d nginx enable 3 4 5')
            sudo('touch /tmp/nginx_installed')
    except Exception as err:
        print("Failed install nginx with ldap: " + str(err))
        sys.exit(1)
