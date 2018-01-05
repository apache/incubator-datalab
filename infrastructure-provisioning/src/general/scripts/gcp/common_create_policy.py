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
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys
import boto3, botocore

parser = argparse.ArgumentParser()
parser.add_argument('--bucket_name', type=str, default='')
parser.add_argument('--ssn_bucket_name', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--username', type=str, default='')
parser.add_argument('--edge_role_name', type=str, default='')
parser.add_argument('--notebook_role_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    success = False
    if args.bucket_name:
        try:
            handler = open('/root/templates/edge_s3_policy.json', 'r')
            policy = handler.read()
            policy = policy.replace('BUCKET_NAME', args.bucket_name)
            policy = policy.replace('SSN_BUCK', args.ssn_bucket_name)
            if args.region == 'cn-north-1':
                policy = policy.replace('aws', 'aws-cn')
        except OSError:
            print("Failed to open policy template")
            success = False

        try:
            iam = boto3.client('iam')
            try:
                response = iam.create_policy(PolicyName='{}-{}-strict_to_S3-Policy'.format(args.service_base_name, args.username), PolicyDocument=policy)
                time.sleep(10)
                arn = response.get('Policy').get('Arn')
            except botocore.exceptions.ClientError as cle:
                if cle.response['Error']['Code'] == 'EntityAlreadyExists':
                    print("Policy {}-{}-strict_to_S3-Policy already exists. Reusing it.".format(args.service_base_name,
                                                                                                args.username))
                    list = iam.list_policies().get('Policies')
                    for i in list:
                        if '{}-{}-strict_to_S3-Policy'.format(args.service_base_name, args.username) == i.get('PolicyName'):
                            arn = i.get('Arn')
            try:
                iam.attach_role_policy(RoleName=args.edge_role_name, PolicyArn=arn)
                print('POLICY_NAME "{0}-{1}-strict_to_S3-Policy" has been attached to role "{2}"'.format(
                    args.service_base_name, args.username, args.edge_role_name))
                time.sleep(5)
                iam.attach_role_policy(RoleName=args.notebook_role_name, PolicyArn=arn)
                print('POLICY_NAME "{0}-{1}-strict_to_S3-Policy" has been attached to role "{2}"'.format(
                    args.service_base_name, args.username, args.notebook_role_name))
                time.sleep(5)
                success = True
            except botocore.exceptions.ClientError as e:
                print(e.response['Error']['Message'])
                success = False
        except Exception as ex:
            print(ex)
            success = False
    else:
        parser.print_help()
        sys.exit(2)

    if success:
        sys.exit(0)
    else:
        sys.exit(1)