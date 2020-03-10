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
import json
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys


parser = argparse.ArgumentParser()
parser.add_argument('--bucket_name', type=str, default='')
parser.add_argument('--tags', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.bucket_name:
        if GCPMeta().get_bucket(args.bucket_name):
            print("REQUESTED BUCKET {} ALREADY EXISTS".format(args.bucket_name))
        else:
            print("Creating Bucket {}".format(args.bucket_name))
            GCPActions().create_bucket(args.bucket_name)
            GCPActions().add_bucket_labels(args.bucket_name, json.loads(args.tags))
    else:
        parser.print_help()
        sys.exit(2)
