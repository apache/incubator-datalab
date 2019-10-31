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
import argparse
import json
import sys
import os

parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--instance_ip', type=str, default='')
args = parser.parse_args()


def create_user(os_user):
    initial_user = ''
    sudo_group = ''

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    elif os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    env.hosts = '{}'.format(args.instance_ip)
    env['connection_attempts'] = 100
    env.key_filename = args.keyfile
    env.user = initial_user
    env.host_string = env.user + "@" + env.hosts

    try:
        sudo('useradd -m -G {1} -s /bin/bash {0}'.format(os_user, sudo_group))
        sudo('echo "{} ALL = NOPASSWD:ALL" >> /etc/sudoers'.format(os_user))
        sudo('mkdir /home/{}/.ssh'.format(os_user))
        sudo('chown -R {0}:{0} /home/{1}/.ssh/'.format(initial_user, os_user))
        sudo('cat /home/{0}/.ssh/authorized_keys > /home/{1}/.ssh/authorized_keys'.format(initial_user, os_user))
        sudo('chown -R {0}:{0} /home/{0}/.ssh/'.format(os_user))
        sudo('chmod 700 /home/{0}/.ssh'.format(os_user))
        sudo('chmod 600 /home/{0}/.ssh/authorized_keys'.format(os_user))
        sudo('touch /home/{}/.ssh_user_ensured'.format(initial_user))
    except Exception as err:
        print('Failed to install gitlab.{}'.format(str(err)))
        sys.exit(1)


def prepare_config():
    try:
        with lcd('{}tmp/gitlab'.format(os.environ['conf_dlab_path'])):
            if os.path.exists('{}tmp/gitlab/gitlab.rb.bak'.format(os.environ['conf_dlab_path'])):
                local('cp gitlab.rb.bak gitlab.rb')
            else:
                local('cp gitlab.rb gitlab.rb.bak')
            if json.loads(os.environ['gitlab_ssl_enabled']):
                local('sed -i "s,EXTERNAL_URL,https://{}:443,g" gitlab.rb'.format(os.environ['instance_hostname']))
                local('sed -i "s/.*NGINX_ENABLED/nginx[\'enable\'] = true/g" gitlab.rb')
                local('sed -i "s,.*NGINX_SSL_CERTIFICATE_KEY,nginx[\'ssl_certificate_key\'] = \'{}\',g" gitlab.rb'.format(
                    os.environ['gitlab_ssl_certificate_key']))
                local('sed -i "s,.*NGINX_SSL_CERTIFICATE,nginx[\'ssl_certificate\'] = \'{}\',g" gitlab.rb'.format(
                    os.environ['gitlab_ssl_certificate']))
                local('sed -i "s,.*NGINX_SSL_DHPARAMS.*,nginx[\'ssl_dhparam\'] = \'{}\',g" gitlab.rb'.format(
                    os.environ['gitlab_ssl_dhparams']))
                if json.loads(os.environ['gitlab_https_redirect_enabled']):
                    local('sed -i "s,.*NGINX_REDIRECT_TO_HTTPS,nginx[\'redirect_http_to_https\'] = true,g" gitlab.rb')
                    local('sed -i "s,.*NGINX_REDIRECT_PORT,nginx[\'redirect_http_to_https_port\'] = 80,g" gitlab.rb')
            else:
                local('sed -i "s,EXTERNAL_URL,http://{},g" gitlab.rb'.format(os.environ['instance_hostname']))

            local('sed -i "s/LDAP_HOST/{}/g" gitlab.rb'.format(os.environ['ldap_hostname']))
            local('sed -i "s/LDAP_PORT/{}/g" gitlab.rb'.format(os.environ['ldap_port']))
            local('sed -i "s/LDAP_UID/{}/g" gitlab.rb'.format(os.environ['ldap_uid']))
            local('sed -i "s/LDAP_BIND_DN/{}/g" gitlab.rb'.format(os.environ['ldap_bind_dn']))
            local("sed -i 's/LDAP_PASSWORD/{}/g' gitlab.rb".format(os.environ['ldap_password']))
            local('sed -i "s/LDAP_BASE/{}/g" gitlab.rb'.format(os.environ['ldap_base']))
            local("sed -i 's/LDAP_ATTR_USERNAME/{}/g' gitlab.rb".format(os.environ['ldap_attr_username']))
            local("sed -i 's/LDAP_ATTR_EMAIL/{}/g' gitlab.rb".format(os.environ['ldap_attr_email']))

            local("sed -i 's/GITLAB_ROOT_PASSWORD/{}/g' gitlab.rb".format(os.environ['gitlab_root_password']))
        print('Initial config is ready.')
    except Exception as err:
        print('Failed to install gitlab.{}'.format(str(err)))
        sys.exit(1)


def install_gitlab():
    try:
        print('Installing gitlab...')
        if os.environ['conf_os_family'] == 'debian':
            sudo('curl -sS https://packages.gitlab.com/install/repositories/gitlab/gitlab-ce/script.deb.sh | sudo bash')
            sudo('apt install gitlab-ce -y')
        elif os.environ['conf_os_family'] == 'redhat':
            sudo('curl -sS https://packages.gitlab.com/install/repositories/gitlab/gitlab-ce/script.rpm.sh | sudo bash')
            sudo('yum install gitlab-ce -y')
        else:
            print('Failed to install gitlab.')
            raise Exception

        with lcd('{}tmp/gitlab'.format(os.environ['conf_dlab_path'])):
            put('gitlab.rb', '/tmp/gitlab.rb')
            local('rm gitlab.rb')
        sudo('rm /etc/gitlab/gitlab.rb')
        sudo('mv /tmp/gitlab.rb /etc/gitlab/gitlab.rb')

        if json.loads(os.environ['gitlab_ssl_enabled']):
            sudo('mkdir -p /etc/gitlab/ssl')
            sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout {0} \
                    -out {1} -subj "/C=US/ST=US/L=US/O=dlab/CN={2}"'.format(os.environ['gitlab_ssl_certificate_key'],
                                                                            os.environ['gitlab_ssl_certificate'],
                                                                            os.environ['instance_hostname']))
            sudo('openssl dhparam -out {} 2048'.format(os.environ['gitlab_ssl_dhparams']))
            get('{}'.format(os.environ['gitlab_ssl_certificate']), '{}tmp/gitlab'.format(os.environ['conf_dlab_path']))

        sudo('gitlab-ctl reconfigure')
    except Exception as err:
        print('Failed to install gitlab.{}'.format(str(err)))
        sys.exit(1)


def configure_gitlab():
    try:
        # Get root private token
        if json.loads(os.environ['gitlab_ssl_enabled']):
            proto = 'https'
        else:
            proto = 'http'

        with settings(hide('everything')):
            raw = run('curl -k --request POST "{0}://localhost/api/v4/session?login=root&password={1}"'
                    .format(proto, os.environ['gitlab_root_password']))
            data = json.loads(raw)
            if not json.loads(os.environ['gitlab_signup_enabled']):
                print('Disabling signup...')
                run('curl -k --request PUT "{0}://localhost/api/v4/application/settings?private_token={1}&sudo=root&signup_enabled=false"'
                    .format(proto, data['private_token']))
            if not json.loads(os.environ['gitlab_public_repos']):
                print('Disabling public repos...')
                run('curl -k --request PUT "{0}://localhost/api/v4/application/settings?private_token={1}&sudo=root&restricted_visibility_levels=public"'
                    .format(proto, data['private_token']))
    except Exception as err:
        print("Failed to connect to GitLab via API..{}".format(str(err)))
        sys.exit(1)


def summary():
    data = dict()
    data['instance_id'] = os.environ['instance_id']
    data['gitlab_hostname'] = os.environ['instance_hostname']
    data['root_password'] = os.environ['gitlab_root_password']
    data['ssl_enabled'] = json.loads(os.environ['gitlab_ssl_enabled'])
    data['https_redirect_enabled'] = json.loads(os.environ['gitlab_https_redirect_enabled'])
    data['signup_enabled'] = json.loads(os.environ['gitlab_signup_enabled'])
    data['public_repos'] = json.loads(os.environ['gitlab_public_repos'])
    data['os_family'] = os.environ['conf_os_family']
    data['os_user'] = os.environ['conf_os_user']
    data['key_name'] = os.environ['conf_key_name']
    print('[SUMMARY]')
    for key in data:
        print('{0}: {1}'.format(key, data[key]))

    with open('{}tmp/result/gitlab.json'.format(os.environ['conf_dlab_path']), 'w') as result:
        result.write(json.dumps(data))


if __name__ == "__main__":
    create_user(os.environ['conf_os_user'])

    env.hosts = '{}'.format(args.instance_ip)
    env['connection_attempts'] = 100
    env.key_filename = args.keyfile
    env.user = os.environ['conf_os_user']
    env.host_string = env.user + "@" + env.hosts

    prepare_config()
    install_gitlab()
    configure_gitlab()

    summary()