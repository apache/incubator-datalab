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
from dlab.aws_actions import *
from dlab.aws_meta import *
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--role_name', type=str, default='')
parser.add_argument('--role_profile_name', type=str, default='')
parser.add_argument('--policy_name', type=str, default='')
parser.add_argument('--policy_arn', type=str, default='')
parser.add_argument('--policy_file_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    success = False
    if args.role_name != '':
        try:
            role_name = get_role_by_name(args.role_name)
            if role_name == '':
                print "Creating role %s, profile name %s" % (args.role_name, args.role_profile_name)
                create_iam_role(args.role_name, args.role_profile_name)
            else:
                print "ROLE AND ROLE PROFILE ARE ALREADY CREATED"
            print "ROLE %s created. IAM group %s created" % (args.role_name, args.role_profile_name)

            print "ATTACHING POLICIES TO ROLE"
            if args.policy_file_name != '':
                create_attach_policy(args.policy_name, args.role_name, args.policy_file_name)
            else:
                policy_arn_bits = eval(args.policy_arn)
                for bit in policy_arn_bits:
                    attach_policy(bit, args.role_name)
            print "POLICY %s created " % args.policy_name
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

