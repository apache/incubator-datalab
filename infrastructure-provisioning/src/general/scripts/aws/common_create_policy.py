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
import boto3
import botocore
import sys
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--bucket_name', type=str, default='')
parser.add_argument('--shared_bucket_name', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--username', type=str, default='')
parser.add_argument('--edge_role_name', type=str, default='')
parser.add_argument('--notebook_role_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--endpoint_name', type=str, default='')
parser.add_argument('--user_predefined_s3_policies', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.bucket_name:
        try:
            handler = open('/root/templates/edge_s3_policy.json', 'r')
            policy = handler.read()
            policy = policy.replace('BUCKET_NAME', args.bucket_name)
            policy = policy.replace('SHARED_BUCK', args.shared_bucket_name)
            if args.region == 'cn-north-1':
                policy = policy.replace('aws', 'aws-cn')
        except OSError:
            logging.error("Failed to open policy template")
            sys.exit(1)

        list_policies_arn = []
        if args.user_predefined_s3_policies != 'None':
            list_predefined_policies = args.user_predefined_s3_policies.split(',')

        try:
            iam = boto3.client('iam')
            try:
                if args.user_predefined_s3_policies != 'None':
                    list = iam.list_policies().get('Policies')
                    for i in list:
                        if i.get('PolicyName') in list_predefined_policies:
                            list_policies_arn.append(i.get('Arn'))
                response = iam.create_policy(PolicyName='{}-{}-{}-strict_to_S3-Policy'.
                                             format(args.service_base_name, args.username, args.endpoint_name),
                                             PolicyDocument=policy)
                time.sleep(10)
                list_policies_arn.append(response.get('Policy').get('Arn'))
            except botocore.exceptions.ClientError as cle:
                if cle.response['Error']['Code'] == 'EntityAlreadyExists':
                    logging.info("Policy {}-{}-{}-strict_to_S3-Policy already exists. Reusing it.".
                          format(args.service_base_name, args.username, args.endpoint_name))
                    list = iam.list_policies().get('Policies')
                    for i in list:
                        if '{}-{}-{}-strict_to_S3-Policy'.format(
                                args.service_base_name, args.username, args.endpoint_name) == i.get('PolicyName') or (
                                args.user_predefined_s3_policies != 'None' and i.get('PolicyName') in
                                list_predefined_policies):
                            list_policies_arn.append(i.get('Arn'))
            try:
                for arn in list_policies_arn:
                    iam.attach_role_policy(RoleName=args.edge_role_name, PolicyArn=arn)
                    logging.info('POLICY "{0}" has been attached to role "{1}"'.format(arn, args.edge_role_name))
                    time.sleep(5)
                    iam.attach_role_policy(RoleName=args.notebook_role_name, PolicyArn=arn)
                    logging.info('POLICY "{0}" has been attached to role "{1}"'.format(arn, args.notebook_role_name))
                    time.sleep(5)
            except botocore.exceptions.ClientError as e:
                logging.error(e.response['Error']['Message'])
                sys.exit(1)
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
