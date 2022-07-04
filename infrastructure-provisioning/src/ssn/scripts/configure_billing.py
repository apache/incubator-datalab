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

parser = argparse.ArgumentParser()
parser.add_argument('--cloud_provider', type=str,
                    help='Where DataLab should be deployed. Available options: aws, azure')
parser.add_argument('--infrastructure_tag', type=str, help='unique name for DataLab environment')
parser.add_argument('--access_key_id', default='', type=str, help='AWS Access Key ID')
parser.add_argument('--secret_access_key', default='', type=str, help='AWS Secret Access Key')
parser.add_argument('--tag_resource_id', type=str, default='user:tag', help='The name of user tag')
parser.add_argument('--billing_tag', type=str, default='datalab', help='Billing tag')
parser.add_argument('--account_id', type=str, help='The ID of ASW linked account')
parser.add_argument('--billing_bucket', type=str, help='The name of bucket')
parser.add_argument('--aws_job_enabled', type=str, default='false',
                    help='Billing format. Available options: true (aws), false(epam)')
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
parser.add_argument('--datalab_dir', type=str, help='The path to DataLab dir')
parser.add_argument('--datalab_id', type=str, default='resource_tags_user_user_tag',
                    help='Column name in report file that contains DataLab id tag')
parser.add_argument('--usage_date', type=str, default='line_item_usage_start_date',
                    help='Column name in report file that contains usage date tag')
parser.add_argument('--product', type=str, default='product_product_name',
                    help='Column name in report file that contains product name tag')
parser.add_argument('--usage_type', type=str, default='line_item_usage_type',
                    help='Column name in report file that contains usage type tag')
parser.add_argument('--usage', type=str, default='line_item_usage_amount',
                    help='Column name in report file that contains usage tag')
parser.add_argument('--cost', type=str, default='line_item_blended_cost',
                    help='Column name in report file that contains cost tag')
parser.add_argument('--resource_id', type=str, default='line_item_resource_id',
                    help='Column name in report file that contains DataLab resource id tag')
parser.add_argument('--tags', type=str, default='line_item_operation,line_item_line_item_description',
                    help='Column name in report file that contains tags')
parser.add_argument('--billing_dataset_name', type=str, default='',
                    help='Name of gcp billing dataset (in big query service')

parser.add_argument('--mongo_host', type=str, default='localhost', help='Mongo DB host')
parser.add_argument('--mongo_port', type=str, default='27017', help='Mongo DB port')
parser.add_argument('--service_base_name', type=str, help='Service Base Name')
parser.add_argument('--os_user', type=str, help='DataLab user')
parser.add_argument('--keystore_password', type=str, help='Keystore password')
parser.add_argument('--keycloak_client_id', type=str, help='Keycloak client id')
parser.add_argument('--keycloak_client_secret', type=str, help='Keycloak client secret')
parser.add_argument('--keycloak_auth_server_url', type=str, help='Keycloak auth server url')
parser.add_argument('--keycloak_realm_name', type=str, help='Keycloak Realm name')
args = parser.parse_args()


def yml_billing(path):
    try:
        with open(path, 'r') as config_yml_r:
            config_orig = config_yml_r.read()

        config_orig = config_orig.replace('billingEnabled: false', 'billingEnabled: true')
        if args.cloud_provider == 'aws':
            if args.aws_job_enabled == 'true':
                args.tag_resource_id =  'resourceTags' + ':' + args.tag_resource_id
            config_orig = config_orig.replace('MONGO_HOST', args.mongo_host)
            config_orig = config_orig.replace('MONGO_PASSWORD', args.mongo_password)
            config_orig = config_orig.replace('MONGO_PORT', args.mongo_port)
            config_orig = config_orig.replace('BILLING_BUCKET_NAME', args.billing_bucket)
            config_orig = config_orig.replace('REPORT_PATH', args.report_path)
            config_orig = config_orig.replace('AWS_JOB_ENABLED', args.aws_job_enabled)
            config_orig = config_orig.replace('ACCOUNT_ID', args.account_id)
            config_orig = config_orig.replace('ACCESS_KEY_ID', args.access_key_id)
            config_orig = config_orig.replace('SECRET_ACCESS_KEY', args.secret_access_key)
            config_orig = config_orig.replace('CONF_BILLING_TAG', args.billing_tag)
            config_orig = config_orig.replace('SERVICE_BASE_NAME', args.service_base_name)
            config_orig = config_orig.replace('DATALAB_ID', args.datalab_id)
            config_orig = config_orig.replace('USAGE_DATE', args.usage_date)
            config_orig = config_orig.replace('PRODUCT', args.product)
            config_orig = config_orig.replace('USAGE_TYPE', args.usage_type)
            config_orig = config_orig.replace('USAGE', args.usage)
            config_orig = config_orig.replace('COST', args.cost)
            config_orig = config_orig.replace('RESOURCE_ID', args.resource_id)
            config_orig = config_orig.replace('TAGS', args.tags)
            config_orig = config_orig.replace('KEYCLOAK_REALM_NAME', args.keycloak_realm_name)
        elif args.cloud_provider == 'azure':
            config_orig = config_orig.replace('SERVICE_BASE_NAME', args.service_base_name)
            config_orig = config_orig.replace('OS_USER', args.os_user)
            config_orig = config_orig.replace('MONGO_PASSWORD', args.mongo_password)
            config_orig = config_orig.replace('MONGO_PORT', args.mongo_port)
            config_orig = config_orig.replace('MONGO_HOST', args.mongo_host)
            config_orig = config_orig.replace('KEY_STORE_PASSWORD', args.keystore_password)
            config_orig = config_orig.replace('KEYCLOAK_CLIENT_ID', args.keycloak_client_id)
            config_orig = config_orig.replace('KEYCLOAK_CLIENT_SECRET', args.keycloak_client_secret)
            config_orig = config_orig.replace('KEYCLOAK_AUTH_SERVER_URL', args.keycloak_auth_server_url)
            config_orig = config_orig.replace('CLIENT_ID', args.client_id)
            config_orig = config_orig.replace('CLIENT_SECRET', args.client_secret)
            config_orig = config_orig.replace('TENANT_ID', args.tenant_id)
            config_orig = config_orig.replace('SUBSCRIPTION_ID', args.subscription_id)
            config_orig = config_orig.replace('AUTHENTICATION_FILE', args.authentication_file)
            config_orig = config_orig.replace('OFFER_NUMBER', args.offer_number)
            config_orig = config_orig.replace('CURRENCY', args.currency)
            config_orig = config_orig.replace('LOCALE', args.locale)
            config_orig = config_orig.replace('REGION_INFO', args.region_info)
            config_orig = config_orig.replace('KEYCLOAK_REALM_NAME', args.keycloak_realm_name)
        elif args.cloud_provider == 'gcp':
            config_orig = config_orig.replace('SERVICE_BASE_NAME', args.service_base_name)
            config_orig = config_orig.replace('OS_USER', args.os_user)
            config_orig = config_orig.replace('MONGO_PASSWORD', args.mongo_password)
            config_orig = config_orig.replace('MONGO_PORT', args.mongo_port)
            config_orig = config_orig.replace('MONGO_HOST', args.mongo_host)
            config_orig = config_orig.replace('KEY_STORE_PASSWORD', args.keystore_password)
            config_orig = config_orig.replace('DATASET_NAME', args.billing_dataset_name)
            config_orig = config_orig.replace('KEYCLOAK_CLIENT_ID', args.keycloak_client_id)
            config_orig = config_orig.replace('KEYCLOAK_CLIENT_SECRET', args.keycloak_client_secret)
            config_orig = config_orig.replace('KEYCLOAK_AUTH_SERVER_URL', args.keycloak_auth_server_url)
            config_orig = config_orig.replace('KEYCLOAK_REALM_NAME', args.keycloak_realm_name)
        f = open(path, 'w')
        f.write(config_orig)
        f.close()
    except:
        print("Could not write the target file {}".format(path))
        sys.exit(1)

def yml_billing_app(path):
    try:
        with open(path, 'r') as config_yml_r:
            config_orig = config_yml_r.read()

        config_orig = config_orig.replace('MONGO_HOST', args.mongo_host)
        config_orig = config_orig.replace('MONGO_PASSWORD', args.mongo_password)
        config_orig = config_orig.replace('MONGO_PORT', args.mongo_port)
        config_orig = config_orig.replace('OS_USER', args.os_user)
        config_orig = config_orig.replace('KEY_STORE_PASSWORD', args.keystore_password)
        config_orig = config_orig.replace('KEYCLOAK_CLIENT_ID', args.keycloak_client_id)
        config_orig = config_orig.replace('KEYCLOAK_CLIENT_SECRET', args.keycloak_client_secret)
        config_orig = config_orig.replace('KEYCLOAK_AUTH_SERVER_URL', args.keycloak_auth_server_url)
        config_orig = config_orig.replace('KEYCLOAK_REALM_NAME', args.keycloak_realm_name)

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
        yml_billing(args.datalab_dir + 'conf/billing.yml')
        if args.cloud_provider == 'aws':
            yml_billing_app(args.datalab_dir + 'conf/billing_app.yml')
        yml_self_service(args.datalab_dir + 'conf/self-service.yml')
    except:
        print('Error configure billing')
        sys.exit(1)

    sys.exit(0)
