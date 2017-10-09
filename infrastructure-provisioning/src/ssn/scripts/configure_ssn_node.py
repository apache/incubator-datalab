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
from dlab.ssn_lib import *
from dlab.fab import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--dlab_path', type=str, default='')
parser.add_argument('--tag_resource_id', type=str, default='')
args = parser.parse_args()


def cp_key(keyfile, host_string, os_user):
    try:
        key_name=keyfile.split("/")
        sudo('mkdir -p /home/' + os_user + '/keys')
        sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/keys')
        local('scp -r -q -i {0} {0} {1}:/home/{3}/keys/{2}'.format(keyfile, host_string, key_name[-1], os_user))
        sudo('chmod 600 /home/' + os_user + '/keys/*.pem')
        return True
    except:
        return False


def cp_backup_scripts(dlab_path):
    try:
        with cd(dlab_path + "tmp/"):
            put('/root/scripts/backup.py', "backup.py")
            put('/root/scripts/restore.py', "restore.py")
            run('chmod +x backup.py restore.py')
        return True
    except:
        return False


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
        return True
    except:
        return False


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

        return True
    except:
        return False


def generate_ssl(hostname):
    try:
        sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/dlab-selfsigned.key \
             -out /etc/ssl/certs/dlab-selfsigned.crt -subj "/C=US/ST=US/L=US/O=dlab/CN={}"'.format(hostname))
        sudo('openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048')
        return True
    except:
        return False


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

    print("Creating service directories.")
    if not creating_service_directories(args.dlab_path, args.os_user):
        sys.exit(1)

    print("Installing nginx as frontend.")
    if not ensure_nginx(args.dlab_path):
        sys.exit(1)

    print("Generating ssl key and cert for nginx.")
    if not generate_ssl(args.hostname):
        sys.exit(1)

    print("Configuring nginx.")
    if not configure_nginx(deeper_config, args.dlab_path, args.hostname):
        sys.exit(1)

    print("Installing jenkins.")
    if not ensure_jenkins(args.dlab_path):
        sys.exit(1)

    print("Configuring jenkins.")
    if not configure_jenkins(args.dlab_path, args.os_user, deeper_config, args.tag_resource_id):
        sys.exit(1)

    print("Copying key")
    if not cp_key(args.keyfile, env.host_string, args.os_user):
        sys.exit(1)

    print("Copying backup scripts")
    if not cp_backup_scripts(args.dlab_path):
        sys.exit(1)

    print("Copying gitlab scripts & files")
    if not cp_gitlab_scripts(args.dlab_path):
        sys.exit(1)

    print("Ensuring safest ssh ciphers")
    try:
        ensure_ciphers()
    except:
        sys.exit(1)

    sys.exit(0)
