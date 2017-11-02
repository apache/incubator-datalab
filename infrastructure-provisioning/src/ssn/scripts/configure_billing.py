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
                    help='Where DLab should be deployed. Available options: aws, azure')
parser.add_argument('--infrastructure_tag', type=str, help='unique name for DLab environment')
parser.add_argument('--aws_access_key_id', default='', type=str, help='AWS Access Key ID')
parser.add_argument('--aws_secret_access_key', default='', type=str, help='AWS Secret Access Key')
parser.add_argument('--aws_tag_resource_id', type=str, default='user:tag', help='The name of user tag')
parser.add_argument('--aws_account_id', type=str, help='The ID of ASW linked account')
parser.add_argument('--aws_billing_bucket', type=str, help='The name of bucket')
parser.add_argument('--aws_report_path', type=str, default='', help='The path to report folder')
parser.add_argument('--azure_authentication_file', type=str, default='', help='Azure authentication file')
parser.add_argument('--azure_offer_number', type=str, default='', help='Azure offer number')
parser.add_argument('--azure_currency', type=str, default='', help='Azure currency for billing')
parser.add_argument('--azure_locale', type=str, default='', help='Azure locale')
parser.add_argument('--azure_region_info', type=str, default='', help='Azure region')
parser.add_argument('--mongo_password', type=str, help='The password for Mongo DB')
parser.add_argument('--dlab_dir', type=str, help='The path to dlab dir')
args = parser.parse_args()


def yml_billing(path):
    try:
        with open(path, 'r') as config_yml_r:
            config_orig = config_yml_r.read()
        if args.cloud_provider == 'aws':
            config_orig = config_orig.replace('<BILLING_BUCKET_NAME>', args.aws_billing_bucket)
            config_orig = config_orig.replace('<REPORT_PATH>', args.aws_report_path)
            config_orig = config_orig.replace('<ACCOUNT_ID>', args.aws_account_id)
            config_orig = config_orig.replace('<ACCESS_KEY_ID>', args.aws_access_key_id)
            config_orig = config_orig.replace('<SECRET_ACCESS_KEY>', args.aws_secret_access_key)
            config_orig = config_orig.replace('<CONF_TAG_RESOURCE_ID>', args.aws_tag_resource_id)
            config_orig = config_orig.replace('<CONF_SERVICE_BASE_NAME>', args.aws_infrastructure_tag)
        elif args.cloud_provider == 'azure':
            config_orig = config_orig.replace('<AUTHENTICATION_FILE>', args.azure_authentication_file)
            config_orig = config_orig.replace('<OFFER_NUMBER>', args.azure_authentication_file)
            config_orig = config_orig.replace('<CURRENCY>', args.azure_currency)
            config_orig = config_orig.replace('<LOCALE>', args.azure_locale)
            config_orig = config_orig.replace('<REGION_INFO>', args.azure_region_info)
        config_orig = config_orig.replace('<MONGODB_PASSWORD>', args.mongo_password)
        f = open(path, 'w')
        f.write(config_orig)
        f.close()
    except:
        print("Could not write the target file {}".format(path))
        sys.exit(1)


def yml_self_service(path):
    try:
        with open(path, 'r') as config_yml_r:
            config_orig = config_yml_r.read()

        config_orig = config_orig.replace('billingSchedulerEnabled: false', 'billingSchedulerEnabled: true')

        f = open(path, 'w')
        f.write(config_orig)
        f.close()
    except:
        print("Could not write the target file {}".format(path))
        sys.exit(1)


##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure billing")
    # Check cloud provider
    # Access to the bucket without credentials?
    try:
        yml_billing(args.dlab_dir + 'conf/billing.yml')
        yml_self_service(args.dlab_dir + 'conf/self-service.yml')
    except:
        sys.exit(1)

    sys.exit(0)
