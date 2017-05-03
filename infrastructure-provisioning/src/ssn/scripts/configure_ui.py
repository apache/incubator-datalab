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
parser.add_argument('--region', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--security_groups_ids', type=str, default='')
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--subnet_id', type=str, default='')
parser.add_argument('--tag_resource_id', type=str, default='')
parser.add_argument('--account_id', type=str, default='')
parser.add_argument('--billing_bucket', type=str, default='')
parser.add_argument('--report_path', type=str, default='')
parser.add_argument('--billing_enabled', type=bool, default=False)
args = parser.parse_args()

dlab_conf_dir = args.dlab_path + 'conf/'
web_path = args.dlab_path + 'webapp/lib/'
local_log_filename = "{}_UI.log".format(args.request_id)
local_log_filepath = "/logs/" + args.resource + "/" + local_log_filename
logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                    level=logging.INFO,
                    filename=local_log_filepath)
mongo_passwd = id_generator()


def configure_mongo(mongo_passwd):
    try:
        if not exists("/lib/systemd/system/mongod.service"):
            local('scp -i {} /root/templates/mongod.service_template {}:/tmp/mongod.service'.format(args.keyfile,
                                                                                                    env.host_string))
            sudo('mv /tmp/mongod.service /lib/systemd/system/mongod.service')
        local('scp -i {} /root/files/ssn_instance_shapes.lst {}:/tmp/ssn_instance_shapes.lst'.format(args.keyfile,
                                                                                                 env.host_string))
        sudo('mv /tmp/ssn_instance_shapes.lst ' + args.dlab_path + 'tmp/')
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
        sudo('python ' + args.dlab_path + 'tmp/configure_mongo.py --region {} --base_name {} --sg "{}" --vpc {} --subnet {} --dlab_path {} --os_user {} --os_family {} --tag_resource_id {}'.format(args.region, args.service_base_name, args.security_groups_ids.replace(" ", ""), args.vpc_id, args.subnet_id, args.dlab_path, args.os_user, args.os_family, args.tag_resource_id))
        return True
    except:
        return False


def configure_billing(args):
    try:
        local('scp -i {} /root/scripts/configure_billing.py {}:/tmp/configure_billing.py'.format(args.keyfile,
                                                                                                 env.host_string))
        sudo('python /tmp/configure_billing.py --cloud_provider {} --infrastructure_tag {} --tag_resource_id {} --account_id {} --billing_bucket {} --report_path "{}" --mongo_password {} --dlab_dir {}'.
             format(args.cloud_provider, args.service_base_name, args.tag_resource_id, args.account_id,
                    args.billing_bucket, args.report_path, mongo_passwd, args.dlab_path))
        return True
    except:
        return False


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = args.os_user + '@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)

    print "Installing Supervisor"
    if not ensure_supervisor():
        logging.error('Failed to install Supervisor')
        sys.exit(1)

    print "Installing MongoDB"
    if not ensure_mongo():
        logging.error('Failed to install MongoDB')
        sys.exit(1)

    print "Configuring MongoDB"
    if not configure_mongo(mongo_passwd):
        logging.error('MongoDB configuration script has failed.')
        sys.exit(1)

    sudo('echo DLAB_CONF_DIR={} >> /etc/profile'.format(dlab_conf_dir))
    sudo('echo export DLAB_CONF_DIR >> /etc/profile')

    if args.billing_enabled:
        print "Configuring billing"
        if not configure_billing(args):
            logging.error('Failed configure billing')
            sys.exit(1)

    print "Starting Self-Service(UI)"
    if not start_ss(args.keyfile, env.host_string, dlab_conf_dir, web_path, args.os_user, mongo_passwd):
        logging.error('Failed to start UI')
        sys.exit(1)

    sys.exit(0)
