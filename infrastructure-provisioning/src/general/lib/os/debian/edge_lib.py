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
from datalab.common_lib import configure_nginx_LE
from datalab.common_lib import install_certbot
from datalab.common_lib import manage_pkg
from datalab.common_lib import run_certbot
from fabric import *
from patchwork.files import exists


def configure_http_proxy_server(config):
    try:
        if not exists(datalab.fab.conn,'/tmp/http_proxy_ensured'):
            manage_pkg('-y install', 'remote', 'squid')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            datalab.fab.conn.put(template_file, '/tmp/squid.conf')
            datalab.fab.conn.sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            datalab.fab.conn.sudo('sed -i "s|PROXY_SUBNET|{}|g" /etc/squid/squid.conf'.format(proxy_subnet))
            datalab.fab.conn.sudo('sed -i "s|EDGE_USER_NAME|{}|g" /etc/squid/squid.conf'.format(config['project_name']))
            datalab.fab.conn.sudo('sed -i "s|LDAP_HOST|{}|g" /etc/squid/squid.conf'.format(config['ldap_host']))
            datalab.fab.conn.sudo('sed -i "s|LDAP_DN|{}|g" /etc/squid/squid.conf'.format(config['ldap_dn']))
            datalab.fab.conn.sudo('sed -i "s|LDAP_SERVICE_USERNAME|{}|g" /etc/squid/squid.conf'.format(config['ldap_user']))
            datalab.fab.conn.sudo('sed -i "s|LDAP_SERVICE_PASSWORD|{}|g" /etc/squid/squid.conf'.format(config['ldap_password']))
            datalab.fab.conn.sudo('sed -i "s|LDAP_AUTH_PATH|{}|g" /etc/squid/squid.conf'.format('/usr/lib/squid/basic_ldap_auth'))
            replace_string = ''
            for cidr in config['vpc_cidrs']:
                replace_string += 'acl AWS_VPC_CIDR dst {}\\n'.format(cidr)
            datalab.fab.conn.sudo('sed -i "s|VPC_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            replace_string = ''
            for cidr in config['allowed_ip_cidr']:
                replace_string += 'acl AllowedCIDRS src {}\\n'.format(cidr)
            datalab.fab.conn.sudo('sed -i "s|ALLOWED_CIDRS|{}|g" /etc/squid/squid.conf'.format(replace_string))
            datalab.fab.conn.sudo('systemctl restart squid')
            datalab.fab.conn.sudo('touch /tmp/http_proxy_ensured')
    except Exception as err:
        print("Failed to install and configure squid: " + str(err))
        sys.exit(1)


def install_nginx_lua(edge_ip, nginx_version, keycloak_auth_server_url, keycloak_realm_name, keycloak_client_id,
                      keycloak_client_secret, user, hostname, step_cert_sans):
    try:
        if not os.path.exists('/tmp/nginx_installed'):
            manage_pkg('-y install', 'remote',
                       'gcc build-essential make automake zlib1g-dev libpcre++-dev libssl-dev git libldap2-dev '
                       'libc6-dev libgd-dev libgeoip-dev libpcre3-dev apt-utils autoconf liblmdb-dev libtool '
                       'libxml2-dev libyajl-dev pkgconf libreadline-dev libreadline6-dev libtinfo-dev '
                       'libtool-bin zip readline-doc perl curl liblua5.1-0 liblua5.1-0-dev lua5.1')
            manage_pkg('-y install --no-install-recommends', 'remote', 'wget gnupg ca-certificates')
            if os.environ['conf_stepcerts_enabled'] == 'true':
                datalab.fab.conn.sudo('mkdir -p /home/{0}/keys'.format(user))
                datalab.fab.conn.sudo('''bash -c 'echo "{0}" | base64 --decode > /etc/ssl/certs/root_ca.crt' '''.format(
                     os.environ['conf_stepcerts_root_ca']))
                fingerprint = datalab.fab.conn.sudo('step certificate fingerprint /etc/ssl/certs/root_ca.crt')
                datalab.fab.conn.sudo('step ca bootstrap --fingerprint {0} --ca-url "{1}"'.format(fingerprint,
                                                                                 os.environ['conf_stepcerts_ca_url']))
                datalab.fab.conn.sudo('echo "{0}" > /home/{1}/keys/provisioner_password'.format(
                     os.environ['conf_stepcerts_kid_password'], user))
                sans = "--san localhost --san 127.0.0.1 {0}".format(step_cert_sans)
                cn = edge_ip
                datalab.fab.conn.sudo('step ca token {3} --kid {0} --ca-url "{1}" --root /etc/ssl/certs/root_ca.crt '
                     '--password-file /home/{2}/keys/provisioner_password {4} --output-file /tmp/step_token'.format(
                    os.environ['conf_stepcerts_kid'], os.environ['conf_stepcerts_ca_url'], user, cn, sans))
                token = datalab.fab.conn.sudo('cat /tmp/step_token')
                datalab.fab.conn.sudo('step ca certificate "{0}" /etc/ssl/certs/datalab.crt /etc/ssl/certs/datalab.key '
                     '--token "{1}" --kty=RSA --size 2048 --provisioner {2} '.format(cn, token,
                                                                                     os.environ['conf_stepcerts_kid']))
                datalab.fab.conn.sudo('touch /var/log/renew_certificates.log')
                datalab.fab.conn.put('/root/templates/manage_step_certs.sh', '/usr/local/bin/manage_step_certs.sh', use_sudo=True)
                datalab.fab.conn.sudo('chmod +x /usr/local/bin/manage_step_certs.sh')
                datalab.fab.conn.sudo('sed -i "s|STEP_ROOT_CERT_PATH|/etc/ssl/certs/root_ca.crt|g" '
                     '/usr/local/bin/manage_step_certs.sh')
                datalab.fab.conn.sudo('sed -i "s|STEP_CERT_PATH|/etc/ssl/certs/datalab.crt|g" /usr/local/bin/manage_step_certs.sh')
                datalab.fab.conn.sudo('sed -i "s|STEP_KEY_PATH|/etc/ssl/certs/datalab.key|g" /usr/local/bin/manage_step_certs.sh')
                datalab.fab.conn.sudo('sed -i "s|STEP_CA_URL|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_ca_url']))
                datalab.fab.conn.sudo('sed -i "s|RESOURCE_TYPE|edge|g" /usr/local/bin/manage_step_certs.sh')
                datalab.fab.conn.sudo('sed -i "s|SANS|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(sans))
                datalab.fab.conn.sudo('sed -i "s|CN|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(cn))
                datalab.fab.conn.sudo('sed -i "s|KID|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_kid']))
                datalab.fab.conn.sudo('sed -i "s|STEP_PROVISIONER_PASSWORD_PATH|/home/{0}/keys/provisioner_password|g" '
                     '/usr/local/bin/manage_step_certs.sh'.format(user))
                datalab.fab.conn.sudo('bash -c \'echo "0 * * * * root /usr/local/bin/manage_step_certs.sh >> '
                     '/var/log/renew_certificates.log 2>&1" >> /etc/crontab \'')
                datalab.fab.conn.put('/root/templates/step-cert-manager.service', '/etc/systemd/system/step-cert-manager.service',
                    use_sudo=True)
                datalab.fab.conn.sudo('systemctl daemon-reload')
                datalab.fab.conn.sudo('systemctl enable step-cert-manager.service')
            else:
                datalab.fab.conn.sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/datalab.key \
                     -out /etc/ssl/certs/datalab.crt -subj "/C=US/ST=US/L=US/O=datalab/CN={}"'.format(hostname))

            datalab.fab.conn.sudo('mkdir -p /tmp/src')
            with datalab.fab.conn.cd('/tmp/src/'):
                datalab.fab.conn.sudo('wget https://luarocks.org/releases/luarocks-3.3.1.tar.gz')
                datalab.fab.conn.sudo('tar -xzf luarocks-3.3.1.tar.gz')

            datalab.fab.conn.sudo('wget -O - https://openresty.org/package/pubkey.gpg | sudo apt-key add -')
            datalab.fab.conn.sudo('add-apt-repository -y "deb http://openresty.org/package/ubuntu $(lsb_release -sc) main"')
            datalab.fab.conn.sudo('apt-get update')
            datalab.fab.conn.sudo('apt-get -y install openresty=1.19.3.1-1~focal1')

            with datalab.fab.conn.cd('/tmp/src/luarocks-3.3.1/'):
                datalab.fab.conn.sudo('./configure')
                datalab.fab.conn.sudo('make install')
                datalab.fab.conn.sudo('luarocks install lua-resty-jwt 0.2.2 --tree /usr/local/openresty/lualib/resty/')
                datalab.fab.conn.sudo('luarocks install lua-resty-openidc --tree /usr/local/openresty/lualib/resty/')

            datalab.fab.conn.sudo('luarocks install lua-resty-jwt 0.2.2')
            datalab.fab.conn.sudo('luarocks install lua-resty-openidc')

            datalab.fab.conn.sudo('useradd -r nginx')

            datalab.fab.conn.sudo('mkdir -p /opt/datalab/templates')
            datalab.fab.conn.put('/root/templates', '/opt/datalab', use_sudo=True)
            datalab.fab.conn.sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(edge_ip))
            datalab.fab.conn.sudo('sed -i \'s|KEYCLOAK_AUTH_URL|{}|g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_auth_server_url))
            datalab.fab.conn.sudo('sed -i \'s/KEYCLOAK_REALM_NAME/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_realm_name))
            datalab.fab.conn.sudo('sed -i \'s/KEYCLOAK_CLIENT_ID/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_client_id))
            datalab.fab.conn.sudo('sed -i \'s/KEYCLOAK_CLIENT_SECRET/{}/g\' /opt/datalab/templates/conf.d/proxy.conf'.format(
                keycloak_client_secret))

            datalab.fab.conn.sudo('cp /opt/datalab/templates/nginx.conf /usr/local/openresty/nginx/conf')
            datalab.fab.conn.sudo('mkdir /usr/local/openresty/nginx/conf/conf.d')
            datalab.fab.conn.sudo('cp /opt/datalab/templates/conf.d/proxy.conf /usr/local/openresty/nginx/conf/conf.d/')
            datalab.fab.conn.sudo('mkdir /usr/local/openresty/nginx/conf/locations')
            datalab.fab.conn.sudo('systemctl start openresty')
            datalab.fab.conn.sudo('touch /tmp/nginx_installed')
            if os.environ['conf_letsencrypt_enabled'] == 'true':
                print("Configuring letsencrypt certificates.")
                install_certbot(os.environ['conf_os_family'])
                if 'conf_letsencrypt_email' in os.environ:
                    run_certbot(os.environ['conf_letsencrypt_domain_name'], os.environ['project_name'].lower(), os.environ['conf_letsencrypt_email'])
                else:
                    run_certbot(os.environ['conf_letsencrypt_domain_name'], os.environ['project_name'].lower())
                configure_nginx_LE(os.environ['conf_letsencrypt_domain_name'], os.environ['project_name'].lower())
    except Exception as err:
        print("Failed install nginx with ldap: " + str(err))
        sys.exit(1)

def configure_nftables(config):
    try:
        if not exists(datalab.fab.conn,'/tmp/nftables_ensured'):
            manage_pkg('-y install', 'remote', 'nftables')
            datalab.fab.conn.sudo('systemctl enable nftables.service')
            datalab.fab.conn.sudo('systemctl start nftables')
            datalab.fab.conn.sudo('sysctl net.ipv4.ip_forward=1')
            if os.environ['conf_cloud_provider'] == 'aws':
                interface = 'eth0'
            elif os.environ['conf_cloud_provider'] == 'gcp':
                interface = 'ens4'
            datalab.fab.conn.sudo('sed -i \'s/#net.ipv4.ip_forward=1/net.ipv4.ip_forward=1/g\' /etc/sysctl.conf')
            datalab.fab.conn.sudo('sed -i \'s/EDGE_IP/{}/g\' /opt/datalab/templates/nftables.conf'.format(config['edge_ip']))
            datalab.fab.conn.sudo('sed -i "s|INTERFACE|{}|g" /opt/datalab/templates/nftables.conf'.format(interface))
            datalab.fab.conn.sudo(
                'sed -i "s|SUBNET_CIDR|{}|g" /opt/datalab/templates/nftables.conf'.format(config['exploratory_subnet']))
            datalab.fab.conn.sudo('cp /opt/datalab/templates/nftables.conf /etc/')
            datalab.fab.conn.sudo('systemctl restart nftables')
            datalab.fab.conn.sudo('touch /tmp/nftables_ensured')
    except Exception as err:
        print("Failed to configure nftables: " + str(err))
        sys.exit(1)