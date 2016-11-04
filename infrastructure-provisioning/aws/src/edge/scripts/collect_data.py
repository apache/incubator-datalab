#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

import argparse
import json
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
    nbs_list = get_ec2_list('{}-Tag'.format(args.service_base_name))
    for i in nbs_list:
        notebook = {}
        notebook['Id'] = i.id
        for tag in i.tags:
            if tag['Key'] == 'Name':
                notebook['Name'] = tag['Value']
        notebook['Shape'] = i.instance_type
        notebook['Status'] = i.state['Name']
        emr_list = get_emr_list(notebook['Name'], 'Value')
        resources = []
        for j in emr_list:
            emr = {}
            emr['id'] = j
            emr['status'] =  get_emr_info(j, 'Status')['State']
            counter = 0
            for instance in get_ec2_list('Notebook', notebook['Name']):
                counter +=1
                emr['shape'] = instance.instance_type
            emr['nodes_count'] = counter
            emr['type'] =  get_emr_info(j, 'ReleaseLabel')
            resources.append(emr)
        notebook['computeresources'] = resources
        notebooks.append(notebook)

    edge['Notebooks'] = notebooks
    data.append(edge)

    filename = '{}.json'.format(args.request_id)
    with open('/root/' + filename, 'w') as outfile:
        json.dump(data, outfile)

    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = 'ubuntu@' + args.hostname
    #put('/root/' + filename, '/tmp/' + filename, mode=0644)
    #sudo('mv /tmp/' + filename + ' /home/ubuntu/' + filename)