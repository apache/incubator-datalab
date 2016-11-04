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

import boto3
import argparse
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--key_id', type=str, default='')
args = parser.parse_args()


def cleanup(key_id):
    try:
        iam = boto3.resource('iam')
        current_user = iam.CurrentUser()
        for user_key in current_user.access_keys.all():
            if user_key.id == key_id:
                print "Deleted key " + user_key.id
                user_key.delete()
        return True
    except:
        return False

##############
# Run script #
##############

if __name__ == "__main__":
    if not cleanup(args.key_id):
        sys.exit(1)
    else:
        sys.exit(0)
