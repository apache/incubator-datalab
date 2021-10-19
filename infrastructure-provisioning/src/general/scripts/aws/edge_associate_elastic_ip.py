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
parser.add_argument('--elastic_ip', type=str, default='')
parser.add_argument('--edge_id', type=str)
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    try:
        if args.elastic_ip == 'None':
            logging.info("Allocating Elastic IP")
            allocation_id = allocate_elastic_ip()
            tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
            tag_name = {"Key": "Name", "Value": args.infra_tag_value}
            create_tag(allocation_id, tag)
            create_tag(allocation_id, tag_name)
        else:
            allocation_id = get_allocation_id_by_elastic_ip(args.elastic_ip)

        logging.info("Associating Elastic IP to Edge")
        associate_elastic_ip(args.edge_id, allocation_id)
    except Exception as err:
        logging.error('Error: {0}'.format(err))
        sys.exit(1)
