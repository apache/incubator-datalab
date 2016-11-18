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
import argparse
import json
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='edge')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


def configure_socks_proxy_server(config):
    try:
        if not exists('/tmp/socks_proxy_ensured'):
            sudo('apt-get -y install dante-server')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            with open("/tmp/danted.conf", 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line.replace('PROXY_SUBNET', proxy_subnet))
            put('/tmp/danted.conf', '/tmp/danted.conf')
            sudo('\cp /tmp/danted.conf /etc/danted.conf')
            sudo('service danted restart')
            sudo('sysv-rc-conf danted on')
            sudo('touch /tmp/socks_proxy_ensured')
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


    print "Installing socks proxy."
    if configure_socks_proxy_server(deeper_config):
        sys.exit(0)
    else:
        sys.exit(1)
