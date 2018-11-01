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

from fabric.api import *
import argparse
import json
import sys
import os
from dlab.ssn_lib import *
from dlab.fab import *
import traceback

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--dlab_path', type=str, default='')
parser.add_argument('--tag_resource_id', type=str, default='')
args = parser.parse_args()


def set_hostname(subdomain, hosted_zone_name):
    try:
        sudo('hostnamectl set-hostname {0}.{1}'.format(subdomain, hosted_zone_name))
    except Exception as err:
        traceback.print_exc()
        print('Failed to set hostname: ', str(err))
        sys.exit(1)


def cp_key(keyfile, host_string, os_user):
    try:
        key_name=keyfile.split("/")
        sudo('mkdir -p /home/' + os_user + '/keys')
        sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/keys')
        local('scp -r -q -i {0} {0} {1}:/home/{3}/keys/{2}'.format(keyfile, host_string, key_name[-1], os_user))
        sudo('chmod 600 /home/' + os_user + '/keys/*.pem')
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy key: ', str(err))
        sys.exit(1)


def cp_backup_scripts(dlab_path):
    try:
        with cd(dlab_path + "tmp/"):
            put('/root/scripts/backup.py', "backup.py")
            put('/root/scripts/restore.py', "restore.py")
            run('chmod +x backup.py restore.py')
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy backup scripts: ', str(err))
        sys.exit(1)


def cp_gitlab_scripts(dlab_path):
    try:
        if not exists('{}tmp/gitlab'.format(dlab_path)):
            run('mkdir -p {}tmp/gitlab'.format(dlab_path))
        with cd('{}tmp/gitlab'.format(dlab_path)):
            put('/root/scripts/gitlab_deploy.py', 'gitlab_deploy.py')
            put('/root/scripts/configure_gitlab.py', 'configure_gitlab.py')
            run('chmod +x gitlab_deploy.py configure_gitlab.py')
            put('/root/templates/gitlab.rb', 'gitlab.rb')
            put('/root/templates/gitlab.ini', 'gitlab.ini')
            run('sed -i "s/CONF_OS_USER/{}/g" gitlab.ini'.format(os.environ['conf_os_user']))
            run('sed -i "s/CONF_OS_FAMILY/{}/g" gitlab.ini'.format(os.environ['conf_os_family']))
            run('sed -i "s/CONF_KEY_NAME/{}/g" gitlab.ini'.format(os.environ['conf_key_name']))
            run('sed -i "s,CONF_DLAB_PATH,{},g" gitlab.ini'.format(dlab_path))
            run('sed -i "s/SERVICE_BASE_NAME/{}/g" gitlab.ini'.format(os.environ['conf_service_base_name']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy gitlab scripts: ', str(err))
        sys.exit(1)


def creating_service_directories(dlab_path, os_user):
    try:
        if not exists(dlab_path):
            sudo('mkdir -p ' + dlab_path)
            sudo('mkdir -p ' + dlab_path + 'conf')
            sudo('mkdir -p ' + dlab_path + 'webapp/lib')
            sudo('mkdir -p ' + dlab_path + 'webapp/static')
            sudo('mkdir -p ' + dlab_path + 'template')
            sudo('mkdir -p ' + dlab_path + 'tmp')
            sudo('mkdir -p ' + dlab_path + 'tmp/result')
            sudo('mkdir -p ' + dlab_path + 'sources')
            sudo('mkdir -p /var/opt/dlab/log/ssn')
            sudo('mkdir -p /var/opt/dlab/log/edge')
            sudo('mkdir -p /var/opt/dlab/log/notebook')
            sudo('mkdir -p /var/opt/dlab/log/dataengine-service')
            sudo('mkdir -p /var/opt/dlab/log/dataengine')
            sudo('ln -s ' + dlab_path + 'conf /etc/opt/dlab')
            sudo('ln -s /var/opt/dlab/log /var/log/dlab')
            sudo('chown -R ' + os_user + ':' + os_user + ' /var/opt/dlab/log')
            sudo('chown -R ' + os_user + ':' + os_user + ' ' + dlab_path)
    except Exception as err:
        traceback.print_exc()
        print('Failed to create service directories: ', str(err))
        sys.exit(1)


def configure_ssl_certs(hostname, custom_ssl_cert):
    try:
        if custom_ssl_cert:
            put('/root/certs/dlab.crt', 'dlab.crt')
            put('/root/certs/dlab.key', 'dlab.key')
            sudo('mv dlab.crt /etc/ssl/certs/dlab.crt')
            sudo('mv dlab.key /etc/ssl/certs/dlab.key')
        else:
            sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/dlab.key \
                 -out /etc/ssl/certs/dlab.crt -subj "/C=US/ST=US/L=US/O=dlab/CN={}"'.format(hostname))
        sudo('openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048')
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure SSL certificates: ', str(err))
        sys.exit(1)


##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = args.os_user + '@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)
    if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and 'ssn_subdomain' in os.environ:
        domain_created = True
    else:
        domain_created = False

    if os.path.exists('/root/certs/dlab.crt') and os.path.exists('/root/certs/dlab.key'):
        custom_ssl_cert = True
    else:
        custom_ssl_cert = False

    print("Creating service directories.")
    creating_service_directories(args.dlab_path, args.os_user)

    if domain_created:
        print("Setting hostname")
        set_hostname(os.environ['ssn_subdomain'], os.environ['ssn_hosted_zone_name'])
        args.hostname = "{0}.{1}".format(os.environ['ssn_subdomain'], os.environ['ssn_hosted_zone_name'])

    print("Installing nginx as frontend.")
    ensure_nginx(args.dlab_path)

    print("Configuring ssl key and cert for nginx.")
    configure_ssl_certs(args.hostname, custom_ssl_cert)

    print("Configuring nginx.")
    configure_nginx(deeper_config, args.dlab_path, args.hostname)

    print("Installing jenkins.")
    ensure_jenkins(args.dlab_path)

    print("Configuring jenkins.")
    configure_jenkins(args.dlab_path, args.os_user, deeper_config, args.tag_resource_id)

    print("Copying key")
    cp_key(args.keyfile, env.host_string, args.os_user)

    print("Copying backup scripts")
    cp_backup_scripts(args.dlab_path)

    print("Copying gitlab scripts & files")
    cp_gitlab_scripts(args.dlab_path)

    print("Ensuring safest ssh ciphers")
    ensure_ciphers()
