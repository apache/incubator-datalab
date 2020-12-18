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

import logging
from fabric.api import *
import argparse
import sys
import os
from fabric.contrib.files import exists

parser = argparse.ArgumentParser()
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--public_ip_address', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--keycloak_realm_name', type=str, default='')
parser.add_argument('--keycloak_user', type=str, default='')
parser.add_argument('--keycloak_user_password', type=str, default='')
args = parser.parse_args()

keycloak_version = "8.0.1"
templates_dir = './infrastructure-provisioning/scripts/deploy_keycloak/templates/'
external_port = "80"
internal_port = "8080"
private_ip_address = "127.0.0.1"

def ensure_jre_jdk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/jre_jdk_ensured'):
        try:
            sudo('mkdir -p /home/' + os_user + '/.ensure_dir')
            sudo('apt-get update')
            sudo('apt-get install -y default-jre')
            sudo('apt-get install -y default-jdk')
            sudo('touch /home/' + os_user + '/.ensure_dir/jre_jdk_ensured')
        except:
            sys.exit(1)

def configure_keycloak():
    sudo('wget https://downloads.jboss.org/keycloak/' + keycloak_version + '/keycloak-' + keycloak_version + '.tar.gz -O /tmp/keycloak-' + keycloak_version + '.tar.gz')
    sudo('tar -zxvf /tmp/keycloak-' + keycloak_version + '.tar.gz -C /opt/')
    sudo('ln -s /opt/keycloak-' + keycloak_version + ' /opt/keycloak')
    sudo('chown ' + args.os_user + ':' + args.os_user + ' -R /opt/keycloak-' + keycloak_version)
    sudo('/opt/keycloak/bin/add-user-keycloak.sh -r master -u ' + args.keycloak_user + ' -p ' + args.keycloak_user_password) #create initial admin user in master realm
    put(templates_dir + 'realm.json', '/tmp/' + args.keycloak_realm_name + '-realm.json')
    put(templates_dir + 'keycloak.service', '/tmp/keycloak.service')
    sudo("cp /tmp/keycloak.service /etc/systemd/system/keycloak.service")
    sudo("sed -i 's|realm-name|" + args.keycloak_realm_name + "|' /tmp/" + args.keycloak_realm_name + "-realm.json")
    sudo("sed -i 's|OS_USER|" + args.os_user + "|' /etc/systemd/system/keycloak.service")
    sudo("sed -i 's|private_ip_address|" + private_ip_address + "|' /etc/systemd/system/keycloak.service")
    sudo("sed -i 's|keycloak_realm_name|" + args.keycloak_realm_name + "|' /etc/systemd/system/keycloak.service")
    sudo("systemctl daemon-reload")
    sudo("systemctl enable keycloak")
    sudo("systemctl start keycloak")

def configure_nginx():
    sudo('apt install -y nginx')
    put(templates_dir + 'nginx.conf', '/tmp/nginx.conf')
    sudo("cp /tmp/nginx.conf /etc/nginx/conf.d/nginx.conf")
    sudo("sed -i 's|80|81|' /etc/nginx/sites-enabled/default")
    sudo("sed -i 's|external_port|" + external_port + "|' /etc/nginx/conf.d/nginx.conf")
    sudo("sed -i 's|internal_port|" + internal_port + "|' /etc/nginx/conf.d/nginx.conf")
    sudo("sed -i 's|private_ip_address|" + private_ip_address + "|' /etc/nginx/conf.d/nginx.conf")
    sudo("systemctl daemon-reload")
    sudo("systemctl enable nginx")
    sudo("systemctl restart nginx")

if __name__ == "__main__":
    local("sudo mkdir /logs/keycloak -p")
    local('sudo chown ' + args.os_user + ':' + args.os_user + ' -R /logs/keycloak')
    local_log_filename = "keycloak_deployment_script.log"
    local_log_filepath = "/logs/keycloak/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print("Configure connections")
    if args.public_ip_address != '':
        try:
            env['connection_attempts'] = 100
            env.key_filename = [args.keyfile]
            env.host_string = '{}@{}'.format(args.os_user, args.public_ip_address)
        except Exception as err:
            print("Failed establish connection. Excpeption: " + str(err))
            sys.exit(1)
    else:
        try:
            env['connection_attempts'] = 100
            env.key_filename = [args.keyfile]
            env.host_string = '{}@{}'.format(args.os_user, private_ip_address)
        except Exception as err:
            print("Failed establish connection. Excpeption: " + str(err))
            sys.exit(1)

    print("Install Java")
    ensure_jre_jdk(args.os_user)

    try:
        print("installing Keycloak")
        configure_keycloak()
    except Exception as err:
        print("Failed keycloak install: " + str(err))
        sys.exit(1)

    try:
        print("installing nginx")
        configure_nginx()
    except Exception as err:
        print("Failed nginx install: " + str(err))
        sys.exit(1)
