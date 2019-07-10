#!/usr/bin/python
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


from fabric.api import *
import yaml, json, sys
import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument('--cloud_provider', type=str,
                    help='Where DLab should be deployed. Available options: aws, azure')
parser.add_argument('--infrastructure_tag', type=str, help='unique name for DLab environment')
parser.add_argument('--access_key_id', default='', type=str, help='AWS Access Key ID')
parser.add_argument('--secret_access_key', default='', type=str, help='AWS Secret Access Key')
parser.add_argument('--tag_resource_id', type=str, default='user:tag', help='The name of user tag')
parser.add_argument('--account_id', type=str, help='The ID of ASW linked account')
parser.add_argument('--billing_bucket', type=str, help='The name of bucket')
parser.add_argument('--aws_job_enabled', type=str, default='false', help='Billing format. Available options: true (aws), false(epam)')
parser.add_argument('--report_path', type=str, default='', help='The path to report folder')
parser.add_argument('--client_id', type=str, default='', help='Azure client ID')
parser.add_argument('--client_secret', type=str, default='', help='Azure client secret')
parser.add_argument('--tenant_id', type=str, default='', help='Azure tenant ID')
parser.add_argument('--subscription_id', type=str, default='', help='Azure subscription ID')
parser.add_argument('--authentication_file', type=str, default='', help='Azure authentication file')
parser.add_argument('--offer_number', type=str, default='', help='Azure offer number')
parser.add_argument('--currency', type=str, default='', help='Azure currency for billing')
parser.add_argument('--locale', type=str, default='', help='Azure locale')
parser.add_argument('--region_info', type=str, default='', help='Azure region info')
parser.add_argument('--mongo_password', type=str, help='The password for Mongo DB')
parser.add_argument('--dlab_dir', type=str, help='The path to dlab dir')
parser.add_argument('--dlab_id', type=str, default='', help='Column name in report file that contains dlab id tag')
parser.add_argument('--usage_date', type=str, default='', help='Column name in report file that contains usage date tag')
parser.add_argument('--product', type=str, default='', help='Column name in report file that contains product name tag')
parser.add_argument('--usage_type', type=str, default='', help='Column name in report file that contains usage type tag')
parser.add_argument('--usage', type=str, default='', help='Column name in report file that contains usage tag')
parser.add_argument('--cost', type=str, default='', help='Column name in report file that contains cost tag')
parser.add_argument('--resource_id', type=str, default='', help='Column name in report file that contains dlab resource id tag')
parser.add_argument('--tags', type=str, default='', help='Column name in report file that contains tags')
args = parser.parse_args()


def yml_billing(path):
    try:
        with open(path, 'r') as config_yml_r:
            config_orig = config_yml_r.read()

        config_orig = config_orig.replace('billingEnabled: false', 'billingEnabled: true')
        if args.cloud_provider == 'aws':
            if args.aws_job_enabled == 'true':
                args.tag_resource_id =  'resourceTags' + ':' + args.tag_resource_id
            config_orig = config_orig.replace('<BILLING_BUCKET_NAME>', args.billing_bucket)
            config_orig = config_orig.replace('<AWS_JOB_ENABLED>', args.aws_job_enabled)
            config_orig = config_orig.replace('<REPORT_PATH>', args.report_path)
            config_orig = config_orig.replace('<ACCOUNT_ID>', args.account_id)
            config_orig = config_orig.replace('<ACCESS_KEY_ID>', args.access_key_id)
            config_orig = config_orig.replace('<SECRET_ACCESS_KEY>', args.secret_access_key)
            config_orig = config_orig.replace('<CONF_TAG_RESOURCE_ID>', args.tag_resource_id)
            config_orig = config_orig.replace('<CONF_SERVICE_BASE_NAME>', args.infrastructure_tag)
            config_orig = config_orig.replace('<MONGODB_PASSWORD>', args.mongo_password)
            config_orig = config_orig.replace('<DLAB_ID>', args.dlab_id)
            config_orig = config_orig.replace('<USAGE_DATE>', args.usage_date)
            config_orig = config_orig.replace('<PRODUCT>', args.product)
            config_orig = config_orig.replace('<USAGE_TYPE>', args.usage_type)
            config_orig = config_orig.replace('<USAGE>', args.usage)
            config_orig = config_orig.replace('<COST>', args.cost)
            config_orig = config_orig.replace('<RESOURCE_ID>', args.resource_id)
            config_orig = config_orig.replace('<TAGS>', args.tags)
        elif args.cloud_provider == 'azure':
            config_orig = config_orig.replace('<CLIENT_ID>', args.client_id)
            config_orig = config_orig.replace('<CLIENT_SECRET>', args.client_secret)
            config_orig = config_orig.replace('<TENANT_ID>', args.tenant_id)
            config_orig = config_orig.replace('<SUBSCRIPTION_ID>', args.subscription_id)
            config_orig = config_orig.replace('<AUTHENTICATION_FILE>', args.authentication_file)
            config_orig = config_orig.replace('<OFFER_NUMBER>', args.offer_number)
            config_orig = config_orig.replace('<CURRENCY>', args.currency)
            config_orig = config_orig.replace('<LOCALE>', args.locale)
            config_orig = config_orig.replace('<REGION_INFO>', args.region_info)
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
        print('Error configure billing')
        sys.exit(1)

    sys.exit(0)
