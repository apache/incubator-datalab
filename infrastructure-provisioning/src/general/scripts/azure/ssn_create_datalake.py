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
import json
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--datalake_name', type=str, default='')
parser.add_argument('--datalake_tags', type=str, default='{"empty":"string"}')
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    try:
        check_datalake = False
        datalake_tags = json.loads(args.datalake_tags)
        for datalake in AzureMeta().list_datalakes(args.resource_group_name):
            if datalake["Name"] == datalake.tags["Name"]:
                check_datalake = True
                logging.info("REQUESTED DATA LAKE {} ALREADY EXISTS".format(datalake.name))
        if not check_datalake:
            datalake_name = id_generator().lower()
            logging.info("Creating DataLake {}.".format(datalake_name))
            datalake = AzureActions().create_datalake_store(args.resource_group_name, datalake_name, args.region,
                                                            datalake_tags)
            logging.info("DATA LAKE {} has been created".format(datalake_name))

    except Exception as err:
        logging.info('Error: {0}'.format(err))
        sys.exit(1)
