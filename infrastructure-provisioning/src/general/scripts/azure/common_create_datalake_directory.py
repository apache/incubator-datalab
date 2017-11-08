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
from dlab.fab import *
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--datalake_name', type=str, default='')
parser.add_argument('--directory_name', type=str, default='')
parser.add_argument('--ad_user', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    try:
        datalake_exists = False
        for datalake in AzureMeta().list_datalakes(args.resource_group_name):
            if args.datalake_name == datalake.tags["Name"]:
                if AzureMeta().verify_datalake_directory(datalake.name, args.directory_name):
                    print("Data Lake Store Directory '{}' already exist".format(args.directory_name))
                else:
                    AzureActions().create_datalake_directory(datalake.name, args.directory_name)
                    print("Data Lake Store Directory '{}' has been created".format(args.directory_name))
                    if args.ad_user == '':
                        AzureActions().chmod_datalake_directory(datalake.name, '0777')
                    else:
                        AzureActions().chown_datalake_directory(datalake.name, args.directory_name, args.ad_user)
                datalake_exists = True
        if not datalake_exists:
            print("Requested Data Lake Store '{}' is missing".format(datalake.name))
            sys.exit(1)
    except:
        sys.exit(1)
