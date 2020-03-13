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

import logging
from fabric.api import *
import argparse
import sys
import os
from fabric.contrib.files import exists

parser = argparse.ArgumentParser()
parser.add_argument('--node_type', type=str, default='', required=True,
                    help='Type of the node where Lets Encrypt certificate should be installed. '
                         'Available options: ssn_node, edge_node')
parser.add_argument('--os_family', type=str, default='', required=True,
                    help='Operating system type. Available options: debian, redhat')
parser.add_argument('--domain_name', type=str, default='', required=True,
                    help='Domain names to apply. '
                         'For multiple domains enter a comma separated list of domains as a parameter')
parser.add_argument('--email', type=str, default='',
                    help='Email that will be entered during certificate obtaining '
                         'and can be user for urgent renewal and security notices. '
                         'Use comma to register multiple emails, e.g. u1@example.com,u2@example.com.')
args = parser.parse_args()

os_user = dlab_user

def install_certbot(os_family):
    try:
        print('Installing Certbot')
        if os_family == 'debian':
            local('sudo apt-get -y update')
            local('sudo apt-get -y install software-properties-common')
            local('sudo add-apt-repository -y universe')
            local('sudo add-apt-repository -y ppa:certbot/certbot')
            local('sudo apt-get -y update')
            local('sudo apt-get -y install certbot python-certbot-nginx')
        elif os_family == 'redhat':
            print('This OS family is not supported yet')
    except Exception as err:
        print('Failed Certbot install: ' + str(err))
        sys.exit(1)

def run_certbot(domain_name, email):
    try:
        print('Running  Certbot')
        local('sudo service nginx stop')
        if email != '':
            local('sudo certbot certonly --standalone -n -d {} -m {}'.format(domain_name, email))
        else:
            local('sudo certbot certonly --standalone -n -d {} --register-unsafely-without-email'.format(domain_name))
    except Exception as err:
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)

def find_replace_line(file_path, searched_str, replacement_line):
    try:
        with open(file_path, 'r') as file:
            lines = file.readlines()
            for line in lines:
                if searched_str in line:
                    line = replacement_line
            with open(file_path, 'w') as file:
                file.writelines(data)
    except Exception as err:
        print('Failed to replace string: ' + str(err))
        sys.exit(1)

def configure_nginx(domain_name, node_type):
    try:
        server_name_line ='     server_name  {};'.format(domain_name)
        cert_path_line = '    ssl_certificate  /etc/letsencrypt/live/{}/fullchain.pem;'.format(domain_name)
        cert_key_line = '    ssl_certificate_key /etc/letsencrypt/live/{}/privkey.pem;'.format(domain_name)
        certbot_service = 'ExecStart = /usr/bin/certbot -q renew --pre-hook "service nginx stop" --post-hook "service nginx start"'
        certbot_service_path = '/lib/systemd/system/certbot.service'
        if node_type == 'ssn_node':
            file_path = '/etc/nginx/conf.d/nginx_proxy.conf'
        else:
            file_path = '/etc/nginx/conf.d/proxy.conf'
        find_replace_line(file_path,'server_name' ,server_name_line)
        find_replace_line(file_path,'ssl_certificate' ,cert_path_line)
        find_replace_line(file_path,'ssl_certificate_key' ,cert_key_line)
        find_replace_line(certbot_service_path, 'ExecStart', certbot_service)
        local('sudo systemctl restart nginx')
    except Exception as err:
        print('Failed to run Certbot: ' + str(err))
        sys.exit(1)

if __name__ == "__main__":
    try:
        if args.node_type != 'ssn_node' and  node_type != 'edge_node':
            print('Valid node type should be specified. Available options: ssn_node, edge_node')
            sys.exit(1)
        if args.os_family != 'debian' and args.os_family != 'redhat':
            print('Valid os family should be specified. Available options: debian, redhat')
            sys.exit(1)
        install_certbot(args.os_family)
        run_certbot(args.domain_name, args.email)
        configure_nginx(args.domain_name, args.node_type)
    except Exception as err:
        print(str(err))
        sys.exit(1)
