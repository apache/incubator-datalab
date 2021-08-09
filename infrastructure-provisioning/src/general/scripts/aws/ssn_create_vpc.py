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
parser.add_argument('--vpc', type=str, default='')
parser.add_argument('--secondary', dest='secondary', action='store_true')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--vpc_name', type=str, default='')
parser.set_defaults(secondary=False)
args = parser.parse_args()

sec_str = ''
if args.secondary:
    sec_str = 'SECONDARY '


if __name__ == "__main__":
    tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    vpc_tag_name = {"Key": "Name", "Value": args.vpc_name}
    rt_tag_name = {"Key": "Name", "Value": "{0}-rt".format(args.vpc_name)}
    if args.vpc != '':
        try:
            vpc_id = get_vpc_by_tag(args.infra_tag_name, args.infra_tag_value)
            if vpc_id == '':
                logging.info("Creating {3}vpc {0} in region {1} with tag {2}".format(args.vpc, args.region, json.dumps(tag), sec_str))
                vpc_id = create_vpc(args.vpc, tag)
                create_tag(vpc_id, vpc_tag_name)
                enable_vpc_dns(vpc_id)
                rt_id = create_rt(vpc_id, args.infra_tag_name, args.infra_tag_value, args.secondary)
                create_tag(rt_id, rt_tag_name)
            else:
                logging.info("REQUESTED {} VPC ALREADY EXISTS".format(sec_str))
            logging.info("{0}VPC_ID: {1}".format(sec_str, vpc_id))
            args.vpc_id = vpc_id
        except Exception as err:
            logging.error('Error: {0}'.format(err))
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(2)
