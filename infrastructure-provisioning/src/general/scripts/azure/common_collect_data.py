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
import ast
import json
import sys
import traceback
from datalab.actions_lib import *
from datalab.fab import *
from datalab.meta_lib import *
from datalab.logger import logging
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--resource_group_name', type=str, default='')
parser.add_argument('--list_resources', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    data = ast.literal_eval(args.list_resources.replace('\'', '"'))
    statuses = {}
    try:
        try:
            data_instances = AzureMeta().get_list_instance_statuses(args.resource_group_name, data.get('host'))
            statuses['host'] = data_instances
        except:
            logging.error("Hosts JSON wasn't been provided")
        try:
            data_images = AzureMeta().get_image_statuses(args.resource_group_name, data.get('image'))
            statuses['image'] = data_images
        except:
            logging.error("Images JSON wasn't been provided")
        try:
            data_clusters = AzureMeta().list_hdinsight_statuses(args.resource_group_name, data.get('cluster'))
            statuses['cluster'] = data_clusters
        except:
            logging.error("Clusters JSON wasn't been provided")
        with open('/root/result.json', 'w') as outfile:
            json.dump(statuses, outfile)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed getting resources statuses.", str(err))
        sys.exit(1)