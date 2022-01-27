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
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--vpc_name', type=str, default='')
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--vpc_cidr', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    if args.vpc_name != '':
        if AzureMeta().get_vpc(args.resource_group_name, args.vpc_name):
            logging.info("REQUESTED VIRTUAL NETWORK {} EXISTS".format(args.vpc_name))
        else:
            logging.info("Creating Virtual Network {}".format(args.vpc_name))
            AzureActions().create_vpc(args.resource_group_name, args.vpc_name, args.region, args.vpc_cidr)
    else:
        logging.error("VPC name can't be empty.")
        sys.exit(1)
