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
import sys
from datalab.fab import *
from datalab.meta_lib import get_instance_private_ip_address
from fabric import *
from jinja2 import Environment, FileSystemLoader
from datalab.fab import *

parser = argparse.ArgumentParser()
parser.add_argument('--edge_hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--type', type=str, default='')
parser.add_argument('--exploratory_name', type=str, default='')
parser.add_argument('--additional_info', type=str, default='')
args = parser.parse_args()


def make_template():
    conf_file_name = args.exploratory_name
    additional_info = json.loads(args.additional_info)
    environment = Environment(loader=FileSystemLoader('/root/locations'), trim_blocks=True,
                              lstrip_blocks=True)
    template = environment.get_template('{}.conf'.format(args.type))
    ungit_template = environment.get_template('{}.conf'.format('ungit'))
    tf_template = environment.get_template('{}.conf'.format('tensor'))
    config = {}
    if args.type != 'dataengine-service' and args.type != 'spark':
        config['NAME'] = args.exploratory_name
        config['IP'] = additional_info['instance_hostname']
    elif args.type == 'spark':
        config['CLUSTER_NAME'] = '{}_{}'.format(
            args.exploratory_name, additional_info['computational_name'])
        config['MASTER_IP'] = get_instance_private_ip_address('Name', additional_info['master_node_name'])
        config['MASTER_DNS'] = additional_info['master_node_hostname']
        config['NOTEBOOK_IP'] = additional_info['notebook_instance_ip']
        slaves = []
        for i in range(additional_info['instance_count'] - 1):
            slave_name = additional_info['slave_node_name'] + '{}'.format(i + 1)
            slave_hostname = get_instance_private_ip_address('Name', slave_name)
            slave = {
                'name': 'datanode{}'.format(i + 1),
                'ip': slave_hostname
            }
            slaves.append(slave)
        config['slaves'] = slaves
        conf_file_name = config['CLUSTER_NAME']
    elif args.type == 'dataengine-service':
        config['CLUSTER_NAME'] = '{}_{}'.format(
            args.exploratory_name, additional_info['computational_name'])
        config['MASTER_IP'] = additional_info['master_ip']
        config['MASTER_DNS'] = additional_info['master_dns']
        config['slaves'] = additional_info['slaves']
        conf_file_name = config['CLUSTER_NAME']

    # Render the template with data and print the output
    f = open('/tmp/{}.conf'.format(conf_file_name), 'w')
    f.write(template.render(config))
    f.close()
    if args.type != 'dataengine-service' and args.type != 'spark':
        f = open('/tmp/{}.conf'.format(conf_file_name), 'a')
        f.write(ungit_template.render(config))
        f.close()
    if additional_info['tensor']:
        f = open('/tmp/{}.conf'.format(conf_file_name), 'a')
        f.write(tf_template.render(config))
        f.close()
    return conf_file_name


##############
# Run script #
##############
if __name__ == "__main__":
    print("Make template")

    try:
        conf_file_name = make_template()
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    print("Configure connections")
    global conn
    conn = datalab.fab.init_datalab_connection(args.edge_hostname, args.os_user, args.keyfile)
    conn.put('/tmp/{}.conf'.format(conf_file_name), '/tmp/{}.conf'.format(conf_file_name))
    conn.sudo('cp -f /tmp/{}.conf /usr/local/openresty/nginx/conf/locations'.format(conf_file_name))
    conn.sudo('service openresty reload')

    conn.close()

