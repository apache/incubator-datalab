#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

import argparse
import json
from dlab.aws_actions import *
from dlab.aws_meta import *
import sys


parser = argparse.ArgumentParser()
parser.add_argument('--node_name', type=str, default='')
parser.add_argument('--ami_id', type=str, default='')
parser.add_argument('--instance_type', type=str, default='')
parser.add_argument('--key_name', type=str, default='')
parser.add_argument('--security_group_ids', type=str, default='')
parser.add_argument('--subnet_id', type=str, default='')
parser.add_argument('--iam_profile', type=str, default='')
parser.add_argument('--infra_tag_name', type=str, default='')
parser.add_argument('--infra_tag_value', type=str, default='')
parser.add_argument('--user_data_file', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    success = False
    instance_tag = {"Key": args.infra_tag_name, "Value": args.infra_tag_value}
    if args.node_name != '':
        try:
            instance_id = get_instance_by_name(args.node_name)
            if instance_id == '':
                print "Creating instance %s of type %s in subnet %s with tag %s." % \
                      (args.node_name, args.instance_type, args.subnet_id, json.dumps(instance_tag))
                instance_id = create_instance(args, instance_tag)
            else:
                print "REQUESTED INSTANCE ALREADY EXISTS AND RUNNING"
            print "Instance_id " + instance_id
            print "Public_hostname " + get_instance_attr(instance_id, 'public_dns_name')
            print "Private_hostname " + get_instance_attr(instance_id, 'private_dns_name')
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
