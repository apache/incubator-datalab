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
parser.add_argument('--service_account_name', type=str, default='')
parser.add_argument('--role_name', type=str, default='')
parser.add_argument('--policy_path', type=str, default='')
parser.add_argument('--roles_path', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.service_account_name != '':
        if GCPMeta().get_service_account(args.service_account_name):
            print("REQUESTED SERVICE ACCOUNT {} ALREADY EXISTS".format(args.service_account_name))
        else:
            print("Creating Service account {}".format(args.service_account_name))
            GCPActions().create_service_account(args.service_account_name)
            if GCPMeta().get_role(args.role_name):
                if GCPMeta().get_role_status(args.role_name) == True:
                    print('Restoring deleted role')
                    GCPActions().undelete_role(args.role_name)
                else:
                    print("REQUESTED ROLE {} ALREADY EXISTS".format(args.role_name))
            else:
                if args.policy_path == '':
                    permissions = []
                else:
                    with open(args.policy_path, 'r') as f:
                        json_file = f.read()
                    permissions = json.loads(json_file)
                print("Creating Role {}".format(args.role_name))
                GCPActions().create_role(args.role_name, permissions)
            print("Assigning custom role to Service account.")
            GCPActions().set_role_to_service_account(args.service_account_name, args.role_name)
            if args.roles_path != '':
                print("Assigning predefined roles to Service account.")
                with open(args.roles_path, 'r') as f:
                    json_file = f.read()
                predefined_roles = json.loads(json_file)
                for role in predefined_roles:
                    GCPActions().set_role_to_service_account(args.service_account_name, role, 'predefined')
    else:
        parser.print_help()
        sys.exit(2)


