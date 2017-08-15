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
import json
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys


parser = argparse.ArgumentParser()
parser.add_argument('--container_name', type=str, default='')
parser.add_argument('--account_name', type=str, default='')
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--shared_container_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    try:
        check = AzureMeta().check_account_availability(args.account_name)
        if check.name_available:
            print "Creating storage account {}.".format(args.account_name)
            storage_account = AzureActions().create_storage_account(args.resource_group_name, args.account_name,
                                                                    args.region)
            # while AzureMeta().get_storage_account(args.resource_group_name,
            #                                      args.account_name).provisioning_state._value_ != "Succeeded":
            time.sleep(50)

            blob_container = AzureActions().create_blob_container(args.resource_group_name, args.account_name,
                                                                  args.container_name)
            print "STORAGE ACCOUNT {} has been created".format(storage_account)
            print "CONTAINER {} has been created".format(blob_container)
            if args.shared_container_name != '':
                shared_blob_container = AzureActions().create_blob_container(args.resource_group_name,
                                                                             args.account_name,
                                                                             args.shared_container_name)
                print "SHARED CONTAINER {} has been created".format(shared_blob_container)
        else:
            print "STORAGE ACCOUNT with name {} could not be created. ".format(args.account_name), check.message
            sys.exit(1)
    except:
        sys.exit(1)
