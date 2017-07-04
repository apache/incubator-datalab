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

import argparse
from dlab.actions_lib import *
from dlab.meta_lib import *


parser = argparse.ArgumentParser()
parser.add_argument('--vpc_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    if args.vpc_name != '':
        if GCPMeta().get_vpc(args.vpc_name):
            print "REQUESTED VPC {} ALREADY EXISTS".format(args.vpc_name)
        else:
            print "Creating VPC {}".format(args.vpc_name)
            GCPActions().create_vpc(args.vpc_name)
        print "VPC name - {} ".format(args.vpc_name)
    else:
        sys.exit(1)