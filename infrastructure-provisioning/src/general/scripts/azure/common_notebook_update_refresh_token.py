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

from fabric.api import *
import argparse
import os
import sys
import time
from fabric.api import lcd
from fabric.contrib.files import exists
from dlab.notebook_lib import *
from dlab.actions_lib import *
from dlab.fab import *
from dlab.common_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--refresh_token', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    update_refresh_token_on_notebook(args.refresh_token)

