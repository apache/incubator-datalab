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
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys
from botocore.exceptions import ClientError


parser = argparse.ArgumentParser()
parser.add_argument('--firewall', type=dict)
args = parser.parse_args()


if __name__ == "__main__":
    if not args.firewall:
        if GCPMeta().get_firewall(args.firewall['name']):
            print "REQUESTED FIREWALL {} ALREADY EXISTS".format(args.firewall['name'])
        else:
            print "Creating Firewall {}".format(args.firewall['name'])
            GCPActions().create_firewall(args.firewall)
        print "Firewall name - {} ".format(args.firewall['name'])
    else:
        sys.exit(1)