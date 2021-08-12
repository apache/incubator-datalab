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
from datalab.meta_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--nat_route_name', type=str)
parser.add_argument('--vpc', type=str)
parser.add_argument('--tag', type=str)
parser.add_argument('--edge_instance', type=str)
args = parser.parse_args()

if __name__ == "__main__":
    if GCPMeta().get_route(args.nat_route_name):
        print("REQUESTED ROUTE {} ALREADY EXISTS".format(args.nat_route_name))
    else:
        print("Creating NAT ROUTE {}".format(args.nat_route_name))
        params = {
            "destRange": "0.0.0.0/0",
            "name": args.nat_route_name,
            "network": args.vpc,
            "priority": 0,
            "tags": [
                args.tag
            ],
            "nextHopInstance": args.edge_instance
        }
        GCPActions().create_nat_route(params)
else:
    parser.print_help()
    sys.exit(2)