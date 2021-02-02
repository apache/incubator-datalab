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

import os
import sys
from datalab.common_lib import manage_pkg
from fabric import *
from fabric.contrib.files import exists


def configure_http_proxy_server(config):
    try:
        if not exists('/tmp/http_proxy_ensured'):
            manage_pkg('-y install', 'remote', 'squid')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            conn.put(template_file, '/tmp/squid.conf')
            conn.sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            conn.sudo('sed -i "s|PROXY_SUBNET|{}|g" /etc/squid/squid.conf'.format(proxy_subnet))
            conn.sudo('sed -i "s|EDGE_USER_NAME|{}|g" /etc/squid/squid.conf'.format(config['project_name']))
            conn.sudo('sed -i "s|LDAP_HOST|{}|g" /etc/squid/squid.conf'.format(config['ldap_host']))
            conn.sudo('sed -i "s|LDAP_DN|{}|g" /etc/squid/squid.conf'.format(config['ldap_dn']))
            conn.sudo('sed -i "s|LDAP_SERVICE_USERNAME|{}|g" /etc/squid/squid.conf'.format(config['ldap_user']))
            conn.sudo('sed -i "s|LDAP_SERVICE_PASSWORD|{}|g" /etc/squid/squid.conf'.format(config['ldap_password']))
            conn.sudo('sed -i "s|LDAP_AUTH_PATH|{}|g" /etc/squid/squid.conf'.format('/usr/lib64/squid/basic_ldap_auth'))
            replace_string = ''
            for cidr in config['vpc_cidrs']:
                replace_string += 'acl AWS_VPC_CIDR dst {}\\n'.format(cidr)
            conn.sudo('sed -i "s|VPC_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            replace_string = ''
            for cidr in config['allowed_ip_cidr']:
                replace_string += 'acl AllowedCIDRS src {}\\n'.format(cidr)
            conn.sudo('sed -i "s|ALLOWED_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            conn.sudo('systemctl restart squid')
            conn.sudo('chkconfig squid on')
            conn.sudo('touch /tmp/http_proxy_ensured')
    except Exception as err:
        print("Failed to install and configure squid: " + str(err))
        sys.exit(1)


def install_nginx_lua(edge_ip, nginx_version, keycloak_auth_server_url, keycloak_realm_name, keycloak_client_id,
                      keycloak_client_secret, user, hostname, step_cert_sans):
    try:
        if not os.path.exists('/tmp/nginx_installed'):
            manage_pkg('-y install', 'remote', 'wget')
            conn.sudo('wget https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm')
            try:
                conn.sudo('rpm -ivh epel-release-latest-7.noarch.rpm')
            except:
                print('Looks like EPEL is already installed.')
            manage_pkg('-y install', 'remote', 'gcc gcc-c++ make zlib-devel pcre-devel openssl-devel git openldap-devel')
            if os.environ['conf_stepcerts_enabled'] == 'true':
                conn.sudo('mkdir -p /home/{0}/keys'.format(user))
                conn.sudo('''bash -c 'echo "{0}" | base64 --decode > /etc/ssl/certs/root_ca.crt' '''.format(
                     os.environ['conf_stepcerts_root_ca']))
                fingerprint = conn.sudo('step certificate fingerprint /etc/ssl/certs/root_ca.crt')
                conn.sudo('step ca bootstrap --fingerprint {0} --ca-url "{1}"'.format(fingerprint,
                                                                                 os.environ['conf_stepcerts_ca_url']))
                conn.sudo('echo "{0}" > /home/{1}/keys/provisioner_password'.format(
                     os.environ['conf_stepcerts_kid_password'], user))
                sans = "--san localhost --san 127.0.0.1 {0}".format(step_cert_sans)
                cn = edge_ip
                conn.sudo('step ca token {3} --kid {0} --ca-url "{1}" --root /etc/ssl/certs/root_ca.crt '
                     '--password-file /home/{2}/keys/provisioner_password {4} --output-file /tmp/step_token'.format(
                    os.environ['conf_stepcerts_kid'], os.environ['conf_stepcerts_ca_url'], user, cn, sans))
                token = conn.sudo('cat /tmp/step_token')
                conn.sudo('step ca certificate "{0}" /etc/ssl/certs/datalab.crt /etc/ssl/certs/datalab.key '
                     '--token "{1}" --kty=RSA --size 2048 --provisioner {2} '.format(cn, token,
                                                                                     os.environ['conf_stepcerts_kid']))
                conn.sudo('touch /var/log/renew_certificates.log')
                conn.put('/root/templates/manage_step_certs.sh', '/usr/local/bin/manage_step_certs.sh', use_sudo=True)
                conn.sudo('chmod +x /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_ROOT_CERT_PATH|/etc/ssl/certs/root_ca.crt|g" '
                     '/usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_CERT_PATH|/etc/ssl/certs/datalab.crt|g" /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_KEY_PATH|/etc/ssl/certs/datalab.key|g" /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_CA_URL|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_ca_url']))
                conn.sudo('sed -i "s|RESOURCE_TYPE|edge|g" /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|SANS|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(sans))
                conn.sudo('sed -i "s|CN|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(cn))
                conn.sudo('sed -i "s|KID|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_kid']))
                conn.sudo('sed -i "s|STEP_PROVISIONER_PASSWORD_PATH|/home/{0}/keys/provisioner_password|g" '
                     '/usr/local/bin/manage_step_certs.sh'.format(user))
                conn.sudo('bash -c \'echo "0 * * * * root /usr/local/bin/manage_step_certs.sh >> '
                     '/var/log/renew_certificates.log 2>&1" >> /etc/crontab \'')
                conn.put('/root/templates/step-cert-manager.service', '/etc/systemd/system/step-cert-manager.service',
                    use_sudo=True)
                conn.sudo('systemctl daemon-reload')
                conn.sudo('systemctl enable step-cert-manager.service')
            else:
                if os.environ['conf_letsencrypt_enabled'] == 'true':
                    print(
                        'Lets Encrypt certificates are not supported for redhat in DataLab. Using self signed certificates')
                conn.sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/datalab.key \
                     -out /etc/ssl/certs/datalab.crt -subj "/C=US/ST=US/L=US/O=datalab/CN={}"'.format(hostname))
            conn.sudo('mkdir -p /tmp/lua')
            conn.sudo('mkdir -p /tmp/src')
            with cd('/tmp/src/'):
                conn.sudo('wget http://nginx.org/download/nginx-{}.tar.gz'.format(nginx_version))
                conn.sudo('tar -xzf nginx-{}.tar.gz'.format(nginx_version))

                conn.sudo('wget https://github.com/openresty/lua-nginx-module/archive/v0.10.15.tar.gz')
                conn.sudo('tar -xzf v0.10.15.tar.gz')

                conn.sudo('wget https://github.com/simplresty/ngx_devel_kit/archive/v0.3.1.tar.gz')
                conn.sudo('tar -xzf v0.3.1.tar.gz')

                conn.sudo('wget http://luajit.org/download/LuaJIT-2.0.5.tar.gz')
                conn.sudo('tar -xzf LuaJIT-2.0.5.tar.gz')

                conn.sudo('wget http://keplerproject.github.io/luarocks/releases/luarocks-2.2.2.tar.gz')
                conn.sudo('tar -xzf luarocks-2.2.2.tar.gz')

                conn.sudo('ln -sf nginx-{} nginx'.format(nginx_version))

            with cd('/tmp/src/LuaJIT-2.0.5/'):
                conn.sudo('make')
                conn.sudo('make install')

            with cd('/tmp/src/nginx/'), shell_env(LUAJIT_LIB='/usr/local/lib/', LUAJIT_INC='/usr/local/include/luajit-2.0'):
                conn.sudo('./configure --user=nginx --group=nginx --prefix=/etc/nginx --sbin-path=/usr/sbin/nginx \
                                              --conf-path=/etc/nginx/nginx.conf --pid-path=/run/nginx.pid --lock-path=/run/lock/subsys/nginx \
                                              --error-log-path=/var/log/nginx/error.log --http-log-path=/var/log/nginx/access.log \
                                              --with-http_gzip_static_module --with-http_stub_status_module --with-http_ssl_module --with-pcre \
                                              --with-http_realip_module --with-file-aio --with-ipv6 --with-http_v2_module --with-ld-opt="-Wl,-rpath,$LUAJIT_LIB"  \
                                              --without-http_scgi_module --without-http_uwsgi_module --without-http_fastcgi_module --with-http_sub_module \
                                              --add-dynamic-module=/tmp/src/ngx_devel_kit-0.3.1 --add-dynamic-module=/tmp/src/lua-nginx-module-0.10.15')
                conn.sudo('make')
                conn.sudo('make install')

            with cd('/tmp/src/luarocks-2.2.2/'):
                conn.sudo('./configure --with-lua-include=/usr/local/include/luajit-2.0')
                conn.sudo('make build')
                conn.sudo('make install')
                conn.sudo('luarocks install lua-resty-jwt')
                conn.sudo('luarocks install lua-resty-session')
                conn.sudo('luarocks install lua-resty-http')
                conn.sudo('luarocks install lua-resty-openidc')
                conn.sudo('luarocks install luacrypto')
                conn.sudo('luarocks install lua-cjson')
                conn.sudo('luarocks install lua-resty-core')
                conn.sudo('luarocks install random')
                conn.sudo('luarocks install lua-resty-string')

            conn.sudo('useradd -r nginx')
            conn.sudo('rm -f /etc/nginx/nginx.conf')
            conn.sudo('mkdir -p /opt/datalab/templates')
            conn.put('/root/templates', '/opt/datalab', use_sudo=True)
            conn.sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(edge_ip))
            conn.sudo('sed -i \'s|KEYCLOAK_AUTH_URL|{}|g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_auth_server_url))
            conn.sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_realm_name))
            conn.sudo('sed -i \'s/KEYCLOAK_CLIENT_ID/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_client_id))
            conn.sudo('sed -i \'s/KEYCLOAK_CLIENT_SECRET/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_client_secret))

            conn.sudo('cp /opt/datalab/templates/nginx.conf /etc/nginx/')
            conn.sudo('mkdir /etc/nginx/conf.d')
            conn.sudo('cp /opt/datalab/templates/conf.d/proxy.conf /etc/nginx/conf.d/')
            conn.sudo('mkdir /etc/nginx/locations')
            conn.sudo('cp /opt/datalab/templates/nginx_redhat /etc/init.d/nginx')
            conn.sudo('chmod +x /etc/init.d/nginx')
            conn.sudo('chkconfig --add nginx')
            conn.sudo('chkconfig --level 345 nginx on')
            conn.sudo('setsebool -P httpd_can_network_connect 1')
            conn.sudo('service nginx start')
            conn.sudo('touch /tmp/nginx_installed')
    except Exception as err:
        print("Failed install nginx with ldap: " + str(err))
        sys.exit(1)
