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
import yaml, json, sys
import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument('--cloud_provider', type=str,
                    help='Where DLab should be deployed. Available options: aws')
parser.add_argument('--infrastructure_tag', type=str, help='unique name for DLab environment')
parser.add_argument('--access_key_id', type=str, help='AWS Access Key ID')
parser.add_argument('--secret_access_key', type=str, help='AWS Secret Access Key')
parser.add_argument('--tag_resource_id', type=str, default='dlab', help='The name of user tag')

parser.add_argument('--account_id', type=str, help='The ID of ASW linked account')
parser.add_argument('--billing_bucket', type=str, help='The name of bucket')
parser.add_argument('--report_path', type=str, default='', help='The path to report folder')

parser.add_argument('--mongo_password', type=str, help='The password for Mongo DB')
args = parser.parse_args()

def yml_billing(path):
    try:
        try:
            with open(path, 'r') as config_yml_r:
                config_orig = yaml.load(config_yml_r)
        except:
            config_orig = {}
        
        section = 'adapterIn'
        if args.cloud_provider == 'aws':
            config_orig[section].update({'bucket':args.billing_bucket})
            config_orig[section].update({'path':args.report_path})
            config_orig[section].update({'accountId':args.account_id})
            config_orig[section].update({'accessKeyId':args.access_key_id})
            config_orig[section].update({'secretAccessKey':args.secret_access_key})
        
        section = 'adapterOut'
        config_orig[section].update({'password':args.mongo_password})
        
        section = 'filter'
        if args.cloud_provider == 'aws':
            config_orig[section].update({'columnDlabTag':args.tag_resource_id})
            config_orig[section].update({'serviceBaseName':args.infrastructure_tag})

        with open(path, 'w') as outfile_yml_w:
            yaml.dump(config_orig, outfile_yml_w, default_flow_style=False)
        return True
    except:
        print "Could not write the target file " + path


def yml_self_service(path):
    try:
        try:
            with open(path, 'r') as config_yml_r:
                config_orig = yaml.load(config_yml_r)
        except:
            config_orig = {}
        
        config_orig.update({'billingSchedulerEnabled':true})

        with open(path, 'w') as outfile_yml_w:
            yaml.dump(config_orig, outfile_yml_w, default_flow_style=False)
    except:
        print "Could not write the target file " + path


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure billing"
    # Check cloud provider
    # Access to the bucket without credentials?
    try:
        yml_billing('PATH/billing.yml')
        yml_self_service('PATH/self-service.yml')
    except:
        sys.exit(1)

    sys.exit(0)
