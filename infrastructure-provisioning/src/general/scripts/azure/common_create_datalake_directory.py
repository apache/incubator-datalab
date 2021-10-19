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
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--datalake_name', type=str, default='')
parser.add_argument('--directory_name', type=str, default='')
parser.add_argument('--ad_user', type=str, default='')
parser.add_argument('--ad_group', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    try:
        datalake_exists = False
        for datalake in AzureMeta().list_datalakes(args.resource_group_name):
            if args.datalake_name == datalake.tags["Name"]:
                if AzureMeta().verify_datalake_directory(datalake.name, args.directory_name):
                    logging.info("Data Lake Store Directory '{}' already exist".format(args.directory_name))
                else:
                    AzureActions().create_datalake_directory(datalake.name, args.directory_name)
                    logging.info("Data Lake Store Directory '{}' has been created".format(args.directory_name))
                    if args.ad_user != '':
                       AzureActions().set_user_permissions_to_datalake_directory(
                           datalake.name, '/{}'.format(args.directory_name), args.ad_user)
                       AzureActions().set_user_permissions_to_datalake_directory(datalake.name, '/', args.ad_user,
                                                                                 'r-x')
                    else:
                        AzureActions().chown_datalake_directory(datalake_name=datalake.name,
                                                                dir_name='/{}'.format(args.directory_name),
                                                                ad_group=args.ad_group)
                datalake_exists = True
        if not datalake_exists:
            logging.info("Requested Data Lake Store '{}' is missing".format(datalake.name))
            sys.exit(1)
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        sys.exit(1)
