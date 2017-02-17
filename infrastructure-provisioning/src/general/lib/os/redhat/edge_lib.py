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

def configure_http_proxy_server(config):
    try:
        if not exists('/tmp/http_proxy_ensured'):
            sudo('yum -y install squid')
            template_file = config['template_file']
            proxy_subnet = config['exploratory_subnet']
            with open("/tmp/tmpsquid.conf", 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line.replace('PROXY_SUBNET', proxy_subnet))
            put('/tmp/tmpsquid.conf', '/tmp/squid.conf')
            sudo('\cp /tmp/squid.conf /etc/squid/squid.conf')
            sudo('systemctl restart squid')
            sudo('chkconfig squid on')
            sudo('touch /tmp/http_proxy_ensured')
        return True
    except:
        return False
