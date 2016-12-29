#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

import json
import argparse
from dlab.aws_actions import *
from dlab.aws_meta import *
import sys


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
args = parser.parse_args()


def create_security_group(security_group_name, vpc_id, security_group_rules, egress, tag):
    ec2 = boto3.resource('ec2')
    group = ec2.create_security_group(GroupName=security_group_name, Description='security_group_name', VpcId=vpc_id)
    time.sleep(10)
    group.create_tags(Tags=[tag])
    try:
        group.revoke_egress(IpPermissions=[{"IpProtocol": "-1", "IpRanges": [{"CidrIp": "0.0.0.0/0"}], "UserIdGroupPairs": [], "PrefixListIds": []}])
    except:
        print "Mentioned rule does not exist"
    for rule in security_group_rules:
        group.authorize_ingress(IpPermissions=[rule])
    for rule in egress:
        group.authorize_egress(IpPermissions=[rule])
    return group.id

if __name__ == "__main__":
    success = False
    try:
        rules = json.loads(args.security_group_rules)
        egress = json.loads(args.egress)
    except:
        sys.exit(1)
    tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    if args.name != '':
        try:
            security_group_id = get_security_group_by_name(args.name)
            if security_group_id == '':
                print "Creating security group %s for vpc %s with tag %s." % (args.name, args.vpc_id, json.dumps(tag))
                security_group_id = create_security_group(args.name, args.vpc_id, rules, egress, tag)
            elif args.force == True:
                print "Removing old security groups."
                if args.resource == 'edge':
                    remove_sgroups(args.nb_sg_name)
                remove_sgroups(args.infra_tag_value)
                print "Creating security group %s for vpc %s with tag %s." % (args.name, args.vpc_id, json.dumps(tag))
                security_group_id = create_security_group(args.name, args.vpc_id, rules, egress, tag)
            else:
                print "REQUESTED SECURITY GROUP WITH NAME %s ALREADY EXISTS" % args.name
            print "SECURITY_GROUP_ID " + security_group_id
            success = True
        except:
            success = False
    else:
        parser.print_help()
        sys.exit(2)

    if success:
        sys.exit(0)
    else:
        sys.exit(1)
