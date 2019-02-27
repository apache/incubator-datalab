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

import argparse
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--role_name', type=str, default='')
parser.add_argument('--role_profile_name', type=str, default='')
parser.add_argument('--policy_name', type=str, default='')
parser.add_argument('--policy_arn', type=str, default='')
parser.add_argument('--policy_file_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.role_name != '':
        try:
            role_name = get_role_by_name(args.role_name)
            if role_name == '':
                print("Creating role {0}, profile name {1}".format(args.role_name, args.role_profile_name))
                create_iam_role(args.role_name, args.role_profile_name, args.region)
            else:
                print("ROLE AND ROLE PROFILE ARE ALREADY CREATED")
            print("ROLE {} created. IAM group {} created".format(args.role_name, args.role_profile_name))

            print("ATTACHING POLICIES TO ROLE")
            if args.policy_file_name != '':
                create_attach_policy(args.policy_name, args.role_name, args.policy_file_name)
            else:
                if args.policy_arn == '':
                    print("POLICY ARN is empty, there is nothing to attach.")
                    success = True
                else:
                    policy_arn_bits = eval(args.policy_arn)
                    for bit in policy_arn_bits:
                        attach_policy(args.role_name, bit)
            print("POLICY {} created".format(args.policy_name))
        except Exception as err:
            print('Error: {0}'.format(err))
    else:
        parser.print_help()
        sys.exit(2)
