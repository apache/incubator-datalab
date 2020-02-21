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
parser.add_argument('--bucket_tags', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--bucket_name_tag', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.bucket_name != '':
        try:
            bucket = get_bucket_by_name(args.bucket_name)
            if bucket == '':
                print("Creating bucket {0} with tags {1}.".format(args.bucket_name, args.bucket_tags))
                bucket = create_s3_bucket(args.bucket_name, args.bucket_tags, args.region, args.bucket_name_tag)
            else:
                print("REQUESTED BUCKET ALREADY EXISTS")
            print("BUCKET_NAME {}".format(bucket))
        except Exception as err:
            print('Error: {0}'.format(err))
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
