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
import os
import sys
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--address_name', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    try:
        if GCPMeta().get_static_address(args.region, args.address_name):
            logging.info("REQUESTED STATIC ADDRESS {} ALREADY EXISTS".format(args.address_name))
        else:
            logging.info("Creating Elastic IP")
            GCPActions().create_static_address(args.address_name, args.region)
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        sys.exit(1)
