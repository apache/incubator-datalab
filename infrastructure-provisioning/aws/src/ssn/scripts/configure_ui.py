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

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='edge')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


web_path = '/tmp/web_app/'
local_log_filename = "{}_UI.log".format(os.environ['request_id'])
local_log_filepath = "/response/" + local_log_filename
logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                    level=logging.INFO,
                    filename=local_log_filepath)


def ensure_mongo():
    try:
        if not exists('/tmp/mongo_ensured'):
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927')
            sudo('ver=`lsb_release -cs`; echo "deb http://repo.mongodb.org/apt/ubuntu $ver/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list; apt-get update')
            sudo('apt-get -y install mongodb-org')
            sudo('sysv-rc-conf mongod on')
            sudo('touch /tmp/mongo_ensured')
        return True
    except:
        return False


def configure_mongo():
    try:
        if not exists("/lib/systemd/system/mongod.service"):
            local('scp -i {} /root/templates/mongod.service_template {}:/tmp/mongod.service'.format(args.keyfile, env.host_string))
            sudo('mv /tmp/mongod.service /lib/systemd/system/mongod.service')
        local('scp -i {} /root/templates/instance_shapes.lst {}:/tmp/instance_shapes.lst'.format(args.keyfile, env.host_string))
        local('scp -i {} /root/scripts/configure_mongo.py {}:/tmp/configure_mongo.py'.format(args.keyfile, env.host_string))
        sudo('python /tmp/configure_mongo.py --region {} --base_name {} --sg "{}"'.format(os.environ['creds_region'], os.environ['conf_service_base_name'], os.environ['creds_security_groups_ids'].replace(" ", "")))
        return True
    except:
        return False


def start_ss():
    try:
        if not exists('/tmp/ss_started'):
            put('/root/templates/proxy_location_webapp_template.conf', '/tmp/proxy_location_webapp_template.conf')
            sudo('cp /tmp/proxy_location_webapp_template.conf /etc/nginx/locations/proxy_location_webapp.conf')
            sudo('mkdir -p ' + web_path)
            sudo('mkdir -p ' + web_path + 'provisioning-service/')
            sudo('mkdir -p ' + web_path + 'security-service/')
            sudo('mkdir -p ' + web_path + 'self-service/')
            sudo('chown -R ubuntu:ubuntu ' + web_path)
            try:
                local('scp -i {} /root/web_app/self-service/* {}:'.format(args.keyfile, env.host_string) + web_path + 'self-service/')
                local('scp -i {} /root/web_app/security-service/* {}:'.format(args.keyfile, env.host_string) + web_path + 'security-service/')
                local('scp -i {} /root/web_app/provisioning-service/* {}:'.format(args.keyfile, env.host_string) + web_path + 'provisioning-service/')
            except:
                with open("/root/result.json", 'w') as result:
                    res = {"error": "Unable to upload webapp jars", "conf": os.environ.__dict__}
                    print json.dumps(res)
                    result.write(json.dumps(res))
                sys.exit(1)
            run('screen -d -m java -Xmx1024M -jar ' + web_path + 'self-service/self-service-1.0.jar server ' + web_path + 'self-service/application.yml; sleep 5')
            run('screen -d -m java -Xmx1024M -jar ' + web_path + 'security-service/security-service-1.0.jar server ' + web_path + 'security-service/application.yml; sleep 5')
            run('screen -d -m java -Xmx1024M -jar ' + web_path + 'provisioning-service/provisioning-service-1.0.jar server ' + web_path + 'provisioning-service/application.yml; sleep 5')
            sudo('service nginx restart')
            sudo('touch /tmp/ss_started')
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
        env.host_string = 'ubuntu@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)

    print "Installing MongoDB"
    if not ensure_mongo():
        logging.error('Failed to install MongoDB')
        sys.exit(1)

    print "Configuring MongoDB"
    if not configure_mongo():
        logging.error('MongoDB configuration script has failed.')
        sys.exit(1)

    print "Starting Self-Service(UI)"
    if not start_ss():
        logging.error('Failed to start UI')
        sys.exit(1)

    sys.exit(0)
