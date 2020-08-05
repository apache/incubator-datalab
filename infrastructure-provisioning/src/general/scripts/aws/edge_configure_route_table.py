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
from dlab.actions_lib import *
from dlab.meta_lib import *


parser = argparse.ArgumentParser()
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--edge_instance_id', type=str, default='')
parser.add_argument('--private_subnet_id', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    rt_id = create_nat_rt(args.vpc_id, args.infra_tag_value, args.edge_instance_id, args.private_subnet_id)

