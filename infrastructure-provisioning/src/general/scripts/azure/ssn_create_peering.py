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
import time
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--source_resource_group_name', type=str, default='')
parser.add_argument('--destination_resource_group_name', type=str, default='')
parser.add_argument('--source_virtual_network_name', type=str, default='')
parser.add_argument('--destination_virtual_network_name', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    try:
        source_virtual_network_peering_name = '{}_to_{}'.format(args.source_virtual_network_name, args.destination_virtual_network_name)
        destination_virtual_network_peering_name = '{}_to_{}'.format(args.destination_virtual_network_name, args.source_virtual_network_name)

        destination_vnet_id = AzureMeta().get_vpc(
            args.destination_resource_group_name,
            args.destination_virtual_network_name,
        ).id

        source_vnet_id = AzureMeta().get_vpc(
            args.source_resource_group_name,
            args.source_virtual_network_name,
        ).id

        logging.info("Creating Virtual Network peering {} and {}".format(source_virtual_network_peering_name, destination_virtual_network_peering_name))
        AzureActions().create_virtual_network_peerings(
                args.source_resource_group_name,
                args.source_virtual_network_name,
                source_virtual_network_peering_name,
                destination_vnet_id)
        AzureActions().create_virtual_network_peerings(
                args.destination_resource_group_name,
                args.destination_virtual_network_name,
                destination_virtual_network_peering_name,
                source_vnet_id)
        time.sleep(250)
    except Exception as err:
        logging.error("Error creating vpc peering: " + str(err))
        sys.exit(1)
