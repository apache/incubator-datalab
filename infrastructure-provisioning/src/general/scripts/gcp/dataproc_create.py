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


import boto3
from botocore.client import Config
import argparse
import re
import time
import sys
from fabric.api import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import json

parser = argparse.ArgumentParser()
# parser.add_argument('--dry_run', action='store_true', help='Print all variables')
parser.add_argument('--region', type=str, help='Region for deploy cluster')
parser.add_argument('--params', type=str, help='Params to be applied to Cluster ( MANDATORY !!! )')
args = parser.parse_args()



def upload_jars_parser(args):
    s3 = boto3.resource('s3', config=Config(signature_version='s3v4'))
    s3.meta.client.upload_file('/root/scripts/jars_parser.py', args.s3_bucket, 'jars_parser.py')


def get_instance_by_ip(ip):
    ec2 = boto3.resource('ec2')
    check = bool(re.match(r"^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$", ip))
    if check:
        instances = ec2.instances.filter(Filters=[{'Name': 'private-ip-address', 'Values': [ip]}])
    else:
        instances = ec2.instances.filter(Filters=[{'Name': 'private-dns-name', 'Values': [ip]}])
    for instance in instances:
        return instance


def read_json(path):
    try:
        with open(path) as json_data:
            data = json.load(json_data)
    except:
        data=[]
    return data


def build_emr_cluster(args):
    params = json.loads(args.params)
    cluster_name = params['clusterName']
    print "Will be created cluster:" + json.dumps(params, sort_keys=True, indent=4, separators=(',', ': '))

    return actions_lib.GCPActions().create_dataproc_cluster(cluster_name, args.region, params)


##############
# Run script #
##############

if __name__ == "__main__":

    if args.name == '':
        parser.print_help()
    else:

        pass

    sys.exit(0)