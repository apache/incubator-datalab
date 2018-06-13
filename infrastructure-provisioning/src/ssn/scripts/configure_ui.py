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
from fabric.contrib.files import exists
import logging
import argparse
import json
import sys
import os
from dlab.ssn_lib import *
from dlab.fab import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--dlab_path', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--cloud_provider', type=str, default='')
parser.add_argument('--os_family', type=str, default='')
parser.add_argument('--request_id', type=str, default='')
parser.add_argument('--resource', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--tag_resource_id', type=str, default=None)
parser.add_argument('--account_id', type=str, default=None)
parser.add_argument('--billing_bucket', type=str, default=None)
parser.add_argument('--aws_job_enabled', type=str, default=None)
parser.add_argument('--report_path', type=str, default=None)
parser.add_argument('--authentication_file', type=str, default=None)
parser.add_argument('--offer_number', type=str, default=None)
parser.add_argument('--currency', type=str, default=None)
parser.add_argument('--locale', type=str, default=None)
parser.add_argument('--region_info', type=str, default=None)
parser.add_argument('--billing_enabled', type=str, default=False)
parser.add_argument('--ldap_login', type=str, default=None)
parser.add_argument('--tenant_id', type=str, default=None)
parser.add_argument('--application_id', type=str, default=None)
parser.add_argument('--subscription_id', type=str, default=None)
parser.add_argument('--datalake_store_name', type=str, default=None)
parser.add_argument('--validate_permission_scope', type=str, default=None)
parser.add_argument('--mongo_parameters', type=str, default='')
parser.add_argument('--dlab_id', type=str, default=None)
parser.add_argument('--usage_date', type=str, default=None)
parser.add_argument('--product', type=str, default=None)
parser.add_argument('--usage_type', type=str, default=None)
parser.add_argument('--usage', type=str, default=None)
parser.add_argument('--cost', type=str, default=None)
parser.add_argument('--resource_id', type=str, default=None)
parser.add_argument('--tags', type=str, default=None)
args = parser.parse_args()

dlab_conf_dir = args.dlab_path + 'conf/'
web_path = args.dlab_path + 'webapp/lib/'
local_log_filename = "{}_UI.log".format(args.request_id)
local_log_filepath = "/logs/" + args.resource + "/" + local_log_filename
logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                    level=logging.INFO,
                    filename=local_log_filepath)
mongo_passwd = id_generator()
keystore_passwd = id_generator()


def copy_ssn_libraries():
    sudo('mkdir -p /usr/lib/python2.7/dlab/')
    run('mkdir -p /tmp/dlab_libs/')
    local('scp -i {} /usr/lib/python2.7/dlab/* {}:/tmp/dlab_libs/'.format(args.keyfile, env.host_string))
    run('chmod a+x /tmp/dlab_libs/*')
    sudo('mv /tmp/dlab_libs/* /usr/lib/python2.7/dlab/')
    if exists('/usr/lib64'):
        sudo('ln -fs /usr/lib/python2.7/dlab /usr/lib64/python2.7/dlab')
    return True


def configure_mongo(mongo_passwd):
    try:
        if not exists("/lib/systemd/system/mongod.service"):
            if os.environ['conf_os_family'] == 'debian':
                local('sed -i "s/MONGO_USR/mongodb/g" /root/templates/mongod.service_template')
            elif os.environ['conf_os_family'] == 'redhat':
                local('sed -i "s/MONGO_USR/mongod/g" /root/templates/mongod.service_template')
            local('scp -i {} /root/templates/mongod.service_template {}:/tmp/mongod.service'.format(args.keyfile,
                                                                                                    env.host_string))
            sudo('mv /tmp/mongod.service /lib/systemd/system/mongod.service')
            sudo('systemctl daemon-reload')
            sudo('systemctl enable mongod.service')
        local('sed -i "s|PASSWORD|{}|g" /root/scripts/resource_status.py'.format(mongo_passwd))
        local('scp -i {} /root/scripts/resource_status.py {}:/tmp/resource_status.py'.format(args.keyfile,
                                                                                                      env.host_string))
        sudo('mv /tmp/resource_status.py ' + os.environ['ssn_dlab_path'] + 'tmp/')
        local('sed -i "s|PASSWORD|{}|g" /root/scripts/configure_mongo.py'.format(mongo_passwd))
        local('scp -i {} /root/scripts/configure_mongo.py {}:/tmp/configure_mongo.py'.format(args.keyfile,
                                                                                             env.host_string))
        sudo('mv /tmp/configure_mongo.py ' + args.dlab_path + 'tmp/')
        local('scp -i {} /root/files/mongo_roles.json {}:/tmp/mongo_roles.json'.format(args.keyfile,
                                                                                             env.host_string))
        sudo('mv /tmp/mongo_roles.json ' + args.dlab_path + 'tmp/')
        mongo_parameters = json.loads(args.mongo_parameters)
        sudo("python " + args.dlab_path + "tmp/configure_mongo.py --dlab_path {} --mongo_parameters '{}'".format(
            args.dlab_path, json.dumps(mongo_parameters)))
        return True
    except Exception as err:
        print(err)
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

    print("Copying DLab libraries to SSN")
    if not copy_ssn_libraries():
        logging.error('Failed to copy DLab libraries')
        sys.exit(1)

    print("Installing Supervisor")
    if not ensure_supervisor():
        logging.error('Failed to install Supervisor')
        sys.exit(1)

    print("Installing MongoDB")
    if not ensure_mongo():
        logging.error('Failed to install MongoDB')
        sys.exit(1)

    print("Configuring MongoDB")
    if not configure_mongo(mongo_passwd):
        logging.error('MongoDB configuration script has failed.')
        sys.exit(1)

    sudo('echo DLAB_CONF_DIR={} >> /etc/profile'.format(dlab_conf_dir))
    sudo('echo export DLAB_CONF_DIR >> /etc/profile')

    print("Starting Self-Service(UI)")
    if not start_ss(args.keyfile, env.host_string, dlab_conf_dir, web_path,
                    args.os_user, mongo_passwd, keystore_passwd, args.cloud_provider,
                    args.service_base_name, args.tag_resource_id, args.account_id,
                    args.billing_bucket, args.aws_job_enabled, args.dlab_path, args.billing_enabled,
                    args.authentication_file, args.offer_number, args.currency, args.locale,
                    args.region_info, args.ldap_login, args.tenant_id, args.application_id,
                    args.hostname, args.datalake_store_name, args.subscription_id, args.validate_permission_scope,
                    args.dlab_id, args.usage_date, args.product, args.usage_type,
                    args.usage, args.cost, args.resource_id, args.tags):
        logging.error('Failed to start UI')
        print('Failed to UI')
        sys.exit(1)

    sys.exit(0)
