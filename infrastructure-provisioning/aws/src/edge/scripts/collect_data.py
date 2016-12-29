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

import argparse
import json
import datetime
from fabric.api import *
from dlab.aws_actions import *
from dlab.aws_meta import *


parser = argparse.ArgumentParser()
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--request_id', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    data = []
    edge = {}
    edge['environment_tag'] = args.service_base_name

    # Get EDGE id
    edge['id'] = get_instance_by_name('{}-{}-edge'.format(args.service_base_name, args.user_name))

    # Get Notebook List
    notebooks = []
    nbs_list = get_ec2_list('{}-Tag'.format(args.service_base_name), '{}-{}-nb'.format(args.service_base_name, args.user_name))
    for i in nbs_list:
        notebook = {}
        notebook['Id'] = i.id
        notebook['Exploratory_fqdn'] = i.private_dns_name
        for tag in i.tags:
            if tag['Key'] == 'Name':
                notebook['Name'] = tag['Value']
        notebook['Shape'] = i.instance_type
        notebook['Status'] = i.state['Name']
        nbs_start_time = i.launch_time.replace(tzinfo=None)
        notebook['Exploratory_uptime'] = str(datetime.datetime.now() - nbs_start_time)
        emr_list = get_emr_list(notebook['Name'], 'Value')
        resources = []
        for j in emr_list:
            emr = {}
            emr['id'] = j
            emr['name'] = get_emr_info(j, 'Name')
            emr['status'] = get_emr_info(j, 'Status')['State']
            counter = 0
            for instance in get_ec2_list('Notebook', notebook['Name']):
                counter +=1
                emr['shape'] = instance.instance_type
            emr['nodes_count'] = counter
            emr['type'] = get_emr_info(j, 'ReleaseLabel')
            emr_start_time = get_emr_info(j, 'Status')['Timeline']['CreationDateTime'].replace(tzinfo=None)
            emr['computational_uptime'] = str(datetime.datetime.now() - emr_start_time)
            resources.append(emr)
        notebook['computeresources'] = resources
        notebooks.append(notebook)

    edge['Notebooks'] = notebooks
    data.append(edge)

    # filename = '{}.json'.format(args.request_id)
    filename = 'result.json'
    with open('/root/' + filename, 'w') as outfile:
        json.dump(data, outfile)

    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = 'ubuntu@' + args.hostname
    #put('/root/' + filename, '/tmp/' + filename, mode=0644)
    #sudo('mv /tmp/' + filename + ' /home/ubuntu/' + filename)