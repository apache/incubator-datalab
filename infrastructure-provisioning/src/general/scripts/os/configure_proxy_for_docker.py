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
import sys
from fabric import *
from datalab.fab import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()

http_file = '/etc/systemd/system/docker.service.d/http-proxy.conf'
https_file = '/etc/systemd/system/docker.service.d/https-proxy.conf'

if __name__ == "__main__":
    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)
    print("Configuring proxy for docker")
    try:
        conn.sudo('mkdir -p /etc/systemd/system/docker.service.d')
        conn.sudo('touch {}'.format(http_file))
        conn.sudo('''bash -l -c 'echo -e \'[Service] \nEnvironment=\"HTTP_PROXY=\'$http_proxy\'\"\' > {}' '''.format(http_file))
        conn.sudo('touch {}'.format(https_file))
        conn.sudo('''bash -l -c 'echo -e \'[Service] \nEnvironment=\"HTTPS_PROXY=\'$http_proxy\'\"\' > {}' '''.format(https_file))
        conn.sudo('mkdir /home/{}/.docker'.format(args.os_user))
        conn.sudo('touch /home/{}/.docker/config.json'.format(args.os_user))
        http_proxy = conn.sudo('''bash -l -c 'echo $http_proxy' ''').stdout.replace('\n','')
        conn.sudo('''echo -e '{\n "proxies":\n {\n   "default":\n   {\n     "httpProxy":"'''+http_proxy+'''",\n     "httpsProxy":"'''+http_proxy+'''"\n   }\n }\n}'  > /tmp/docker_config.json''')
        conn.sudo('cp /tmp/docker_config.json /home/{}/.docker/config.json'.format(args.os_user))
        conn.sudo('usermod -a -G docker ' + args.os_user)
        conn.sudo('update-rc.d docker defaults')
        conn.sudo('update-rc.d docker enable')
        conn.sudo('systemctl restart docker')
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)
    conn.close()