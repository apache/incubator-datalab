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

import json
import argparse
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys
from botocore.exceptions import ClientError


parser = argparse.ArgumentParser()
parser.add_argument('--name', type=str, default='')
parser.add_argument('--vpc_id', type=str, default='')
parser.add_argument('--security_group_rules', type=str, default='[]')
parser.add_argument('--egress', type=str, default='[]')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--force', type=bool, default=False)
parser.add_argument('--nb_sg_name', type=str, default='')
parser.add_argument('--resource', type=str, default='')
parser.add_argument('--ssn', type=bool, default=False)
args = parser.parse_args()


if __name__ == "__main__":
    try:
        rules = json.loads(args.security_group_rules)
        egress = json.loads(args.egress)
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)
    tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    nb_sg_id = get_security_group_by_name(args.nb_sg_name + '-sg')
    if args.name != '':
        try:
            security_group_id = get_security_group_by_name(args.name)
            if security_group_id == '':
                print("Creating security group {0} for vpc {1} with tag {2}.".format(args.name, args.vpc_id,
                                                                                     json.dumps(tag)))
                security_group_id = create_security_group(args.name, args.vpc_id, rules, egress, tag)
                if nb_sg_id != '' and args.resource == 'edge':
                    print("Updating Notebook security group {}".format(nb_sg_id))
                    rule = {'IpProtocol': '-1', 'FromPort': -1, 'ToPort': -1,
                            'UserIdGroupPairs': [{'GroupId': security_group_id}]}
                    add_inbound_sg_rule(nb_sg_id, rule)
                    add_outbound_sg_rule(nb_sg_id, rule)
            else:
                if nb_sg_id != '' and args.resource == 'edge':
                    print("Updating Notebook security group {}".format(nb_sg_id))
                    rule = {'IpProtocol': '-1', 'FromPort': -1, 'ToPort': -1,
                            'UserIdGroupPairs': [{'GroupId': security_group_id}]}
                    add_inbound_sg_rule(nb_sg_id, rule)
                    add_outbound_sg_rule(nb_sg_id, rule)
                print("REQUESTED SECURITY GROUP WITH NAME {} ALREADY EXISTS".format(args.name))
            print("SECURITY_GROUP_ID: {}".format(security_group_id))
            if args.ssn:
                with open('/tmp/ssn_sg_id', 'w') as f:
                    f.write(security_group_id)
        except Exception as err:
            print('Error: {0}'.format(err))
    else:
        parser.print_help()
        sys.exit(2)
