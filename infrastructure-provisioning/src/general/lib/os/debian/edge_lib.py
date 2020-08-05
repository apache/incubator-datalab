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

import os
import sys
from fabric.api import *
from fabric.contrib.files import exists
from dlab.common_lib import manage_pkg
from dlab.common_lib import install_certbot
from dlab.common_lib import run_certbot
from dlab.common_lib import configure_nginx_LE


def configure_http_proxy_server(config):
    try:
        if not exists('/tmp/http_proxy_ensured'):
            manage_pkg('-y install', 'remote', 'squid')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            put(template_file, '/tmp/squid.conf')
            sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            sudo('sed -i "s|PROXY_SUBNET|{}|g" /etc/squid/squid.conf'.format(proxy_subnet))
            sudo('sed -i "s|EDGE_USER_NAME|{}|g" /etc/squid/squid.conf'.format(config['project_name']))
            sudo('sed -i "s|LDAP_HOST|{}|g" /etc/squid/squid.conf'.format(config['ldap_host']))
            sudo('sed -i "s|LDAP_DN|{}|g" /etc/squid/squid.conf'.format(config['ldap_dn']))
            sudo('sed -i "s|LDAP_SERVICE_USERNAME|{}|g" /etc/squid/squid.conf'.format(config['ldap_user']))
            sudo('sed -i "s|LDAP_SERVICE_PASSWORD|{}|g" /etc/squid/squid.conf'.format(config['ldap_password']))
            sudo('sed -i "s|LDAP_AUTH_PATH|{}|g" /etc/squid/squid.conf'.format('/usr/lib/squid/basic_ldap_auth'))
            replace_string = ''
            for cidr in config['vpc_cidrs']:
                replace_string += 'acl AWS_VPC_CIDR dst {}\\n'.format(cidr)
            sudo('sed -i "s|VPC_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            replace_string = ''
            for cidr in config['allowed_ip_cidr']:
                replace_string += 'acl AllowedCIDRS src {}\\n'.format(cidr)
            sudo('sed -i "s|ALLOWED_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            sudo('service squid reload')
            sudo('sysv-rc-conf squid on')
            sudo('touch /tmp/http_proxy_ensured')
    except Exception as err:
        print("Failed to install and configure squid: " + str(err))
        sys.exit(1)


def install_nginx_lua(edge_ip, nginx_version, keycloak_auth_server_url, keycloak_realm_name, keycloak_client_id,
                      keycloak_client_secret, user, hostname, step_cert_sans):
    try:
        if not os.path.exists('/tmp/nginx_installed'):
            manage_pkg('-y install', 'remote', 'wget')
            manage_pkg('-y install', 'remote', 'gcc build-essential make automake zlib1g-dev libpcre++-dev libssl-dev git libldap2-dev libc6-dev libgd-dev libgeoip-dev libpcre3-dev apt-utils autoconf liblmdb-dev libtool libxml2-dev libyajl-dev pkgconf liblua5.1-0 liblua5.1-0-dev libreadline-dev libreadline6-dev libtinfo-dev libtool-bin lua5.1 zip readline-doc')
            if os.environ['conf_stepcerts_enabled'] == 'true':
                sudo('mkdir -p /home/{0}/keys'.format(user))
                sudo('''bash -c 'echo "{0}" | base64 --decode > /etc/ssl/certs/root_ca.crt' '''.format(
                     os.environ['conf_stepcerts_root_ca']))
                fingerprint = sudo('step certificate fingerprint /etc/ssl/certs/root_ca.crt')
                sudo('step ca bootstrap --fingerprint {0} --ca-url "{1}"'.format(fingerprint,
                                                                                 os.environ['conf_stepcerts_ca_url']))
                sudo('echo "{0}" > /home/{1}/keys/provisioner_password'.format(
                     os.environ['conf_stepcerts_kid_password'], user))
                sans = "--san localhost --san 127.0.0.1 {0}".format(step_cert_sans)
                cn = edge_ip
                sudo('step ca token {3} --kid {0} --ca-url "{1}" --root /etc/ssl/certs/root_ca.crt '
                     '--password-file /home/{2}/keys/provisioner_password {4} --output-file /tmp/step_token'.format(
                      os.environ['conf_stepcerts_kid'], os.environ['conf_stepcerts_ca_url'], user, cn, sans))
                token = sudo('cat /tmp/step_token')
                sudo('step ca certificate "{0}" /etc/ssl/certs/dlab.crt /etc/ssl/certs/dlab.key '
                     '--token "{1}" --kty=RSA --size 2048 --provisioner {2} '.format(cn, token,
                                                                                     os.environ['conf_stepcerts_kid']))
                sudo('touch /var/log/renew_certificates.log')
                put('/root/templates/manage_step_certs.sh', '/usr/local/bin/manage_step_certs.sh', use_sudo=True)
                sudo('chmod +x /usr/local/bin/manage_step_certs.sh')
                sudo('sed -i "s|STEP_ROOT_CERT_PATH|/etc/ssl/certs/root_ca.crt|g" '
                     '/usr/local/bin/manage_step_certs.sh')
                sudo('sed -i "s|STEP_CERT_PATH|/etc/ssl/certs/dlab.crt|g" /usr/local/bin/manage_step_certs.sh')
                sudo('sed -i "s|STEP_KEY_PATH|/etc/ssl/certs/dlab.key|g" /usr/local/bin/manage_step_certs.sh')
                sudo('sed -i "s|STEP_CA_URL|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_ca_url']))
                sudo('sed -i "s|RESOURCE_TYPE|edge|g" /usr/local/bin/manage_step_certs.sh')
                sudo('sed -i "s|SANS|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(sans))
                sudo('sed -i "s|CN|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(cn))
                sudo('sed -i "s|KID|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_kid']))
                sudo('sed -i "s|STEP_PROVISIONER_PASSWORD_PATH|/home/{0}/keys/provisioner_password|g" '
                     '/usr/local/bin/manage_step_certs.sh'.format(user))
                sudo('bash -c \'echo "0 * * * * root /usr/local/bin/manage_step_certs.sh >> '
                     '/var/log/renew_certificates.log 2>&1" >> /etc/crontab \'')
                put('/root/templates/step-cert-manager.service', '/etc/systemd/system/step-cert-manager.service',
                    use_sudo=True)
                sudo('systemctl daemon-reload')
                sudo('systemctl enable step-cert-manager.service')
            elif os.environ['conf_letsencrypt_enabled'] == 'true':
                print("Configuring letsencrypt certificates.")
                install_certbot(os.environ['conf_os_family'])
                if 'conf_letsencrypt_email' in os.environ:
                    run_certbot(os.environ['conf_letsencrypt_domain_name'], os.environ['project_name'], os.environ['conf_letsencrypt_email'])
                else:
                    run_certbot(os.environ['conf_letsencrypt_domain_name'], os.environ['project_name'])
            else:
                sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/dlab.key \
                     -out /etc/ssl/certs/dlab.crt -subj "/C=US/ST=US/L=US/O=dlab/CN={}"'.format(hostname))
            sudo('mkdir -p /tmp/lua')
            sudo('mkdir -p /tmp/src')
            with cd('/tmp/src/'):
                sudo('wget http://nginx.org/download/nginx-{}.tar.gz'.format(nginx_version))
                sudo('tar -xzf nginx-{}.tar.gz'.format(nginx_version))

                sudo('wget https://github.com/openresty/lua-nginx-module/archive/v0.10.15.tar.gz')
                sudo('tar -xzf v0.10.15.tar.gz')

                sudo('wget https://github.com/simplresty/ngx_devel_kit/archive/v0.3.1.tar.gz')
                sudo('tar -xzf v0.3.1.tar.gz')

                sudo('wget http://luajit.org/download/LuaJIT-2.0.5.tar.gz')
                sudo('tar -xzf LuaJIT-2.0.5.tar.gz')

                sudo('wget http://keplerproject.github.io/luarocks/releases/luarocks-2.2.2.tar.gz')
                sudo('tar -xzf luarocks-2.2.2.tar.gz')

                sudo('ln -sf nginx-{} nginx'.format(nginx_version))

            with cd('/tmp/src/LuaJIT-2.0.5/'):
                sudo('make')
                sudo('make install')

            with cd('/tmp/src/nginx/'), shell_env(LUAJIT_LIB='/usr/local/lib/', LUAJIT_INC='/usr/local/include/luajit-2.0'):
                sudo('./configure --user=nginx --group=nginx --prefix=/etc/nginx --sbin-path=/usr/sbin/nginx \
                              --conf-path=/etc/nginx/nginx.conf --pid-path=/run/nginx.pid --lock-path=/run/lock/subsys/nginx \
                              --error-log-path=/var/log/nginx/error.log --http-log-path=/var/log/nginx/access.log \
                              --with-http_gzip_static_module --with-http_stub_status_module --with-http_ssl_module --with-pcre \
                              --with-http_realip_module --with-file-aio --with-ipv6 --with-http_v2_module --with-ld-opt="-Wl,-rpath,$LUAJIT_LIB"  \
                              --without-http_scgi_module --without-http_uwsgi_module --without-http_fastcgi_module --with-http_sub_module \
                              --add-dynamic-module=/tmp/src/ngx_devel_kit-0.3.1 --add-dynamic-module=/tmp/src/lua-nginx-module-0.10.15')
                sudo('make')
                sudo('make install')

            with cd('/tmp/src/luarocks-2.2.2/'):
                sudo('./configure')
                sudo('make build')
                sudo('make install')
                sudo('wget https://luarocks.org/manifests/cdbattags/lua-resty-jwt-0.2.0-0.src.rock')
                sudo('luarocks build lua-resty-jwt-0.2.0-0.src.rock')
                sudo('wget https://luarocks.org/manifests/bungle/lua-resty-session-2.26-1.src.rock')
                sudo('luarocks build lua-resty-session-2.26-1.src.rock')
                sudo('wget https://luarocks.org/manifests/pintsized/lua-resty-http-0.15-0.src.rock')
                sudo('luarocks build lua-resty-http-0.15-0.src.rock')
                sudo('wget https://luarocks.org/manifests/hanszandbelt/lua-resty-openidc-1.7.2-1.src.rock')
                sudo('luarocks build lua-resty-openidc-1.7.2-1.src.rock')
                sudo('wget https://luarocks.org/manifests/starius/luacrypto-0.3.2-2.src.rock')
                sudo('luarocks build luacrypto-0.3.2-2.src.rock')
                sudo('wget https://luarocks.org/manifests/openresty/lua-cjson-2.1.0.6-1.src.rock')
                sudo('luarocks build lua-cjson-2.1.0.6-1.src.rock')
                sudo('wget https://luarocks.org/manifests/avlubimov/lua-resty-core-0.1.17-4.src.rock')
                sudo('luarocks build lua-resty-core-0.1.17-4.src.rock')
                sudo('wget https://luarocks.org/manifests/hjpotter92/random-1.1-0.rockspec')
                sudo('luarocks install random-1.1-0.rockspec')
                sudo('wget https://luarocks.org/manifests/rsander/lua-resty-string-0.09-0.rockspec')
                sudo('luarocks install lua-resty-string-0.09-0.rockspec')

            sudo('useradd -r nginx')
            sudo('rm -f /etc/nginx/nginx.conf')
            sudo('mkdir -p /opt/dlab/templates')
            put('/root/templates', '/opt/dlab', use_sudo=True)
            sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/dlab/templates/conf.d/proxy.conf'.format(edge_ip))
            sudo('sed -i \'s|KEYCLOAK_AUTH_URL|{}|g\' /opt/dlab/templates/conf.d/proxy.conf'.format(
                keycloak_auth_server_url))
            sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/dlab/templates/conf.d/proxy.conf'.format(
                keycloak_realm_name))
            sudo('sed -i \'s/KEYCLOAK_CLIENT_ID/{}/g\' /opt/dlab/templates/conf.d/proxy.conf'.format(
                keycloak_client_id))
            sudo('sed -i \'s/KEYCLOAK_CLIENT_SECRET/{}/g\' /opt/dlab/templates/conf.d/proxy.conf'.format(
                keycloak_client_secret))

            sudo('cp /opt/dlab/templates/nginx.conf /etc/nginx/')
            sudo('mkdir /etc/nginx/conf.d')
            sudo('cp /opt/dlab/templates/conf.d/proxy.conf /etc/nginx/conf.d/')
            sudo('mkdir /etc/nginx/locations')
            sudo('cp /opt/dlab/templates/nginx_debian /etc/init.d/nginx')
            sudo('chmod +x /etc/init.d/nginx')
            sudo('systemctl daemon-reload')
            sudo('systemctl enable nginx')
            sudo('/etc/init.d/nginx start')
            sudo('touch /tmp/nginx_installed')
    except Exception as err:
        print("Failed install nginx with ldap: " + str(err))
        sys.exit(1)