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

import argparse
import json
import os
import sys
import yaml
from fabric.api import *
from time import gmtime, strftime

parser = argparse.ArgumentParser(description="Backup script for DataLab configs, keys, certs, jars, database & logs")
parser.add_argument('--user', type=str, default='datalab-user', help='System username')
parser.add_argument('--datalab_path', type=str, default='/opt/datalab/', help='Path to DataLab. Default: /opt/datalab/')
parser.add_argument('--configs', type=str, default='skip',
                    help='Comma separated names of config files, like "security.yml", etc. Default: skip. Also available: all')
parser.add_argument('--keys', type=str, default='skip',
                    help='Comma separated names of keys, like "user_name.pub". Default: skip. Also available: all')
parser.add_argument('--certs', type=str, default='skip',
                    help='Comma separated names of SSL certificates and keys, like "datalab.crt", etc. Default: skip. Also available: all')
parser.add_argument('--jars', type=str, default='skip',
                    help='Comma separated names of jar application, like "self-service" (without .jar), etc. Default: skip. Also available: all')
parser.add_argument('--db', action='store_true', default=False,
                    help='Mongo DB. Key without arguments. Default: disable')
parser.add_argument('--logs', action='store_true', default=False,
                    help='All logs (include docker). Key without arguments. Default: disable')
parser.add_argument('--request_id', type=str, default='', help='Uniq request ID for response and backup')
parser.add_argument('--result_path', type=str, default='/opt/datalab/tmp/result',
                    help='Path to store backup and response files')
args = parser.parse_args()


def backup_prepare():
    try:
        local('mkdir {}'.format(temp_folder))
        if args.configs != 'skip':
            local('mkdir -p {0}conf'.format(temp_folder))
        if args.keys != 'skip':
            local('mkdir -p {}keys'.format(temp_folder))
        if args.certs != 'skip':
            local('mkdir -p {}certs'.format(temp_folder))
        if args.jars != 'skip':
            local('mkdir -p {}jars'.format(temp_folder))
        if args.logs:
            local('mkdir -p {}logs'.format(temp_folder))
            local('mkdir -p {}logs/docker'.format(temp_folder))
    except Exception as err:
        append_result(error='Failed to create temp folder. {}'.format(str(err)))
        sys.exit(1)


def backup_configs():
    try:
        print('Backup configs: {}'.format(args.configs))
        if args.configs == 'skip':
            print('Skipped config backup.')
        elif args.configs == 'all':
            local("find {0}{2} -name '*yml' -exec cp {3} {1}{2} \;".format(args.datalab_path, temp_folder, conf_folder,
                                                                           "{}"))
        else:
            for conf_file in args.configs.split(','):
                local('cp {0}{2}{3} {1}{2}'.format(args.datalab_path, temp_folder, conf_folder, conf_file))
    except:
        append_result(error='Backup configs failed.')
        sys.exit(1)


def backup_keys():
    try:
        print('Backup keys: {}'.format(args.keys))
        if args.keys == 'skip':
            print('Skipped keys backup.')
        elif args.keys == 'all':
            local('cp {0}* {1}keys'.format(keys_folder, temp_folder))
        else:
            for key_file in args.keys.split(','):
                local('cp {0}{1} {2}keys'.format(keys_folder, key_file, temp_folder))
    except:
        append_result(error='Backup keys failed.')
        sys.exit(1)


def backup_certs():
    try:
        print('Backup certs: {}'.format(args.certs))
        if args.certs == 'skip':
            print('Skipped certs backup.')
        elif args.certs == 'all':
            for cert in all_certs:
                local('sudo cp {0}{1} {2}certs'.format(certs_folder, cert, temp_folder))
                local('sudo chown {0}:{0} {1}certs/{2} '.format(os_user, temp_folder, cert))
        else:
            for cert in args.certs.split(','):
                local('cp {0}{1} {2}certs'.format(certs_folder, cert, temp_folder))
                local('sudo chown {0}:{0} {1}certs/{2} '.format(os_user, temp_folder, cert))
    except:
        append_result(error='Backup certs failed.')
        sys.exit(1)


def backup_jars():
    try:
        print('Backup jars: {}'.format(args.jars))
        if args.jars == 'skip':
            print('Skipped jars backup.')
        elif args.jars == 'all':
            for root, dirs, files in os.walk('{0}{1}'.format(args.datalab_path, jars_folder)):
                for service in dirs:
                    local('cp -RP {0}{1}{2}* {3}jars'.format(args.datalab_path, jars_folder, service, temp_folder))
        else:
            for service in args.jars.split(','):
                local('cp -RP {0}{1}{2}* {3}jars'.format(args.datalab_path, jars_folder, service, temp_folder))
    except:
        append_result(error='Backup jars failed.')
        sys.exit(1)


def backup_database():
    try:
        print('Backup db: {}'.format(args.db))
        if args.db:
            ssn_conf = open('{0}{1}ssn.yml'.format(args.datalab_path, conf_folder)).read()
            data = yaml.load('mongo{}'.format(ssn_conf.split('mongo')[-1]))
            with settings(hide('running')):
                local("mongodump --host {0} --port {1} --username {2} --password '{3}' --db={4} --archive={5}mongo.db" \
                    .format(data['mongo']['host'], data['mongo']['port'], data['mongo']['username'],
                            data['mongo']['password'], data['mongo']['database'], temp_folder))
    except:
        append_result(error='Backup db failed.')
        sys.exit(1)


def backup_logs():
    try:
        print('Backup logs: {}'.format(args.logs))
        if args.logs:
            print('Backup DataLab logs')
            local('cp -R {0}* {1}logs'.format(datalab_logs_folder, temp_folder))
            print('Backup docker logs')
            local("sudo find {0} -name '*log' -exec cp {2} {1}logs/docker \;".format(docker_logs_folder, temp_folder,
                                                                                     "{}"))
            local('sudo chown -R {0}:{0} {1}logs/docker'.format(os_user, temp_folder))
    except:
        append_result(error='Backup logs failed.')
        print('Backup logs failed.')
        sys.exit(1)


def backup_finalize():
    try:
        print('Compressing all files to archive...')
        local('cd {0} && tar -zcf {1} .'.format(temp_folder, dest_file))
    except Exception as err:
        append_result(error='Compressing backup failed. {}'.format(str(err)))
        sys.exit(1)

    try:
        print('Clear temp folder...')
        if temp_folder != '/':
            local('rm -rf {}'.format(temp_folder))
    except Exception as err:
        append_result(error='Clear temp folder failed. {}'.format(str(err)))
        sys.exit(1)


def append_result(status='failed', error='', backup_file=''):
    with open(dest_result, 'w') as result:
        res = {"status": status,
               "request_id": args.request_id}
        if status == 'failed':
            print(error)
            res['error_message'] = error
        elif status == 'created':
            print('Successfully created backup file: {}'.format(backup_file))
            res['backup_file'] = backup_file
        print(json.dumps(res))
        result.write(json.dumps(res))


if __name__ == "__main__":
    backup_time = strftime('%d_%b_%Y_%H-%M-%S', gmtime())
    os_user = args.user
    temp_folder = '/tmp/datalab_backup-{}/'.format(backup_time)
    conf_folder = 'conf/'
    keys_folder = '/home/{}/keys/'.format(os_user)
    certs_folder = '/etc/ssl/certs/'
    all_certs = ['dhparam.pem', 'datalab.crt', 'datalab.key']
    jars_folder = 'webapp/lib/'
    datalab_logs_folder = '/var/log/datalab/'
    docker_logs_folder = '/var/lib/docker/containers/'
    dest_result = '{0}/backup_{1}.json'.format(args.result_path, args.request_id)
    dest_file = '{0}/backup_{1}.tar.gz'.format(args.result_path, args.request_id)

    # Backup file section
    backup_prepare()

    # Backup section
    backup_configs()
    backup_keys()
    backup_certs()
    backup_jars()
    backup_database()
    backup_logs()

    # Compressing & cleaning tmp folder
    backup_finalize()

    append_result(status='created', error='Backup keys failed.', backup_file=dest_file)