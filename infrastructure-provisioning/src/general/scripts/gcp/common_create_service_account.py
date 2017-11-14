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

parser = argparse.ArgumentParser()
parser.add_argument('--service_account_name', type=str, default='')
parser.add_argument('--role_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.service_account_name != '':
        if GCPMeta().get_service_account(args.service_account_name):
            print "REQUESTED SERVICE ACCOUNT {} ALREADY EXISTS".format(args.service_account_name)
        else:
            print "Creating Service account {}".format(args.service_account_name)
            GCPActions().create_service_account(args.service_account_name)
            if GCPMeta().get_role(args.role_name):
                print "REQUESTED ROLE {} ALREADY EXISTS".format(args.role_name)
            else:
                print "Creating Role {}".format(args.role_name)
            print "Assigning policy to Service account."
            GCPActions().set_policy_to_service_account(args.service_account_name, args.role_name)
    else:
        sys.exit(1)

