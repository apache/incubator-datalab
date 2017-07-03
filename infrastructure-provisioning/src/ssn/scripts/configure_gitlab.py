#!/usr/bin/python
# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
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
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


def prepare_config():
    with cd('{}tmp/gitlab'.format(os.environ['conf_dlab_path'])):
        local('cp gitlab.rb gitlab.rb.bak')
        if json.loads(os.environ['gitlab_ssl_enabled']):
            local('sed -i "s,EXTERNAL_URL,https://{}:443,g" gitlab.rb'.format(os.environ['instance_hostname']))
            local('sed -i "s/.*NGINX_ENABLED.*/nginx[\'enable\'] = true/g" gitlab.rb')
            local('sed -i "s,.*NGINX_SSL_CERTIFICATE.*,nginx[\'ssl_certificate\'] = \"{}\",g" gitlab.rb'.format(
                os.environ['gitlab_ssl_certificate']))
            local('sed -i "s,.*NGINX_SSL_CERTIFICATE_KEY.*,nginx[\'ssl_certificate_key\'] = \"{}\",g" gitlab.rb'.format(
                os.environ['gitlab_ssl_certificate_key']))
            local('sed -i "s,.*NGINX_SSL_DHPARAMS.*,nginx[\'ssl_dhparam\'] = \"{}\",g" gitlab.rb'.format(
                os.environ['gitlab_ssl_dhparams']))
        else:
            local('sed -i "s,EXTERNAL_URL,http://{},g" gitlab.rb'.format(os.environ['instance_hostname']))

        local('sed -i "s/LDAP_HOST/{}/g" gitlab.rb'.format(os.environ['ldap_host']))
        local('sed -i "s/LDAP_PORT/{}/g" gitlab.rb'.format(os.environ['ldap_port']))
        local('sed -i "s/LDAP_UID/{}/g" gitlab.rb'.format(os.environ['ldap_uid']))
        local('sed -i "s/LDAP_BIND_DN/{}/g" gitlab.rb'.format(os.environ['ldap_bind_dn']))
        local("sed -i 's/LDAP_PASSWORD/{}/g' gitlab.rb".format(os.environ['ldap_password']))
        local('sed -i "s/LDAP_BASE/{}/g" gitlab.rb'.format(os.environ['ldap_base']))
        local('sed -i "s/LDAP_ATTR_USERNAME/{}/g" gitlab.rb'.format(os.environ['ldap_attr_username']))
        local('sed -i "s/LDAP_ATTR_EMAIL/{}/g" gitlab.rb'.format(os.environ['ldap_attr_email']))

        local("sed -i 's/GITLAB_ROOT_PASSWORD/{}/g' gitlab.rb".format(os.environ['gitlab_root_password']))
    print 'Initial config is ready.'


def install_gitlab():
    try:
        print 'Installing gitlab...'
        if os.environ['conf_os_family'] == 'debian':
            sudo('curl -sS https://packages.gitlab.com/install/repositories/gitlab/gitlab-ce/script.deb.sh | sudo bash')
            sudo('apt install gitlab-ce')
        elif os.environ['conf_os_family'] == 'redhat':
            sudo('curl -sS https://packages.gitlab.com/install/repositories/gitlab/gitlab-ce/script.rpm.sh | sudo bash')
            sudo('yum install gitlab-ce')
        else:
            print 'Failed to install gitlab.'
            raise Exception

        with cd('{}tmp/gitlab'.format(os.environ['conf_dlab_path'])):
            put('gitlab.rb', '/tmp/gitlab.rb')
            # local('rm gitlab.rb')
        sudo('rm /etc/gitlab/gitlab.rb')
        sudo('mv /tmp/gitlab.rb /etc/gitlab/gitlab.rb')

        if json.loads(os.environ['gitlab_ssl_enabled']):
            sudo('mkdir -p /etc/gitlab/ssl')
            sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout {0} \
                    -out {1} -subj "/C=US/ST=US/L=US/O=dlab/CN={2}"'.format(os.environ['gitlab_ssl_certificate_key'],
                                                                            os.environ['gitlab_ssl_certificate'],
                                                                            os.environ['instance_hostname']))
            sudo('openssl dhparam -out {} 2048'.format(os.environ['gitlab_ssl_dhparams']))


        sudo('gitlab-ctl reconfigure')
    except Exception as err:
        print 'Failed to install gitlab.', str(err)
        sys.exit(1)


def configure_gitlab():
    try:
        # Get root private token
        raw = run('curl --request POST "http://localhost/api/v4/session?login=root&password={}"'
                  .format(os.environ['gitlab_root_password']))
        data = json.loads(raw)
        if not json.loads(os.environ['gitlab_signup_enabled']):
            print 'Disabling signup...'
            run('curl --request PUT "http://localhost/api/v4/application/settings?private_token={}&sudo=root&signup_enabled=false"'
                .format(data['private_token']))
        if not json.loads(os.environ['gitlab_public_repos']):
            print 'Disabling public repos...'
            run('curl --request PUT "http://localhost/api/v4/application/settings?private_token={}&sudo=root&restricted_visibility_levels=public"'
                .format(data['private_token']))
    except Exception as err:
        print "Failed to connect to GitLab via API..", str(err)
        sys.exit(1)


def summary():
    print '[SUMMARY]'
    print 'Gitlab hostname: {}'.format(os.environ['instance_hostname'])
    print 'ROOT password: {}'.format(os.environ['gitlab_root_password'])
    print 'SSL enabled: {}'.format(json.loads(os.environ['gitlab_ssl_enabled']))
    print 'Signup enabled: {}'.format(json.loads(os.environ['gitlab_signup_enabled']))
    print 'Public repos: {}'.format(json.loads(os.environ['gitlab_public_repos']))



if __name__ == "__main__":
    env.hosts = '{}'.format(args.instance_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = args.keyfile
    env.host_string = env.user + "@" + env.hosts

    prepare_config()
    install_gitlab()
    configure_gitlab()

    summary()