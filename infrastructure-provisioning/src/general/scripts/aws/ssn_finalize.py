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
import boto3
import sys
from datalab.ssn_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--key_id', type=str, default='')
args = parser.parse_args()


def cleanup(key_id):
    try:
        iam = boto3.resource('iam')
        current_user = iam.CurrentUser()
        for user_key in current_user.access_keys.all():
            if user_key.id == key_id:
                print("Deleted key {}".format(user_key.id))
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
