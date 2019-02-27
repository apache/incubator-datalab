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
import json
from dlab.fab import *
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys


parser = argparse.ArgumentParser()
parser.add_argument('--container_name', type=str, default='')
parser.add_argument('--account_tags', type=str, default='{"empty":"string"}')
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    try:
        check_account = False
        account_tags = json.loads(args.account_tags)
        for storage_account in AzureMeta().list_storage_accounts(args.resource_group_name):
            if account_tags["Name"] == storage_account.tags["Name"]:
                check_account = True
                print("REQUESTED STORAGE ACCOUNT {} ALREADY EXISTS".format(storage_account.name))
        if not check_account:
            account_name = id_generator().lower()
            check = AzureMeta().check_account_availability(account_name)
            if check.name_available:
                print("Creating storage account {}.".format(account_name))
                storage_account = AzureActions().create_storage_account(args.resource_group_name, account_name,
                                                                        args.region, account_tags)
                blob_container = AzureActions().create_blob_container(args.resource_group_name, account_name,
                                                                      args.container_name)
                print("STORAGE ACCOUNT {} has been created".format(account_name))
                print("CONTAINER {} has been created".format(args.container_name))
            else:
                print("STORAGE ACCOUNT with name {0} could not be created. {1}".format(account_name, check.message))
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)
