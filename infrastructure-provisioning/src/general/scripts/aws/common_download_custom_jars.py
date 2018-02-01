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
import sys
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
from fabric.api import *
import os
import boto3
import botocore
from botocore.client import Config


parser = argparse.ArgumentParser()
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--notebook_ip', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    env.hosts = "{}".format(args.notebook_ip)
    env['connection_attempts'] = 100
    env.user = args.os_user
    env.key_filename = "{}".format(args.keyfile)
    env.host_string = env.user + "@" + env.hosts

    jars_conf = dict()
    jars_conf['service_base_name'] = os.environ['conf_service_base_name']
    jars_conf['jars_bucket'] = '{}-ssn-bucket'.format(jars_conf['service_base_name'])
    jars_conf['jars_dir'] = os.environ['conf_custom_jars_dir']
    jars_conf['tmp_dir'] = '/tmp/{}/'.format(jars_conf['jars_dir'])

    try:
        if os.environ['aws_region'] == 'cn-north-1':
            s3client = boto3.client('s3', config=Config(signature_version='s3v4'),
                                    endpoint_url='https://s3.cn-north-1.amazonaws.com.cn',
                                    region_name=os.environ['aws_region'])
            s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'),
                                        endpoint_url='https://s3.cn-north-1.amazonaws.com.cn',
                                        region_name=os.environ['aws_region'])
        else:
            s3client = boto3.client('s3', config=Config(signature_version='s3v4'), region_name=os.environ['aws_region'])
            s3resource = boto3.resource('s3', config=Config(signature_version='s3v4'))
        run('mkdir -p '.format(jars_conf['tmp_dir']))
        get_files(s3client, s3resource, jars_conf['jars_dir'], jars_conf['jars_bucket'], '/tmp/')
    except Exception as err:
        append_result("Failed to download custom jars from bucket.", str(err))
        sys.exit(1)

    try:
        if os.path.exists(jars_conf['tmp_dir']):
            for filename in os.listdir(jars_conf['tmp_dir']):
                if filename.endswith('.jar'):
                    put('{0}{1}'.format(jars_conf['tmp_dir'], filename),
                        '{0}{1}'.format(jars_conf['tmp_dir'], filename))
            sudo('rm -rf /opt/{}'.format(jars_conf['jars_dir']))
            sudo('mv /tmp/{0} /opt/{1}'.format(jars_conf['tmp_dir'], jars_conf['jars_dir']))
    except Exception as err:
        append_result("Failed to setup custom jars.", str(err))
        sys.exit(1)
