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


from fabric.api import *
import argparse
import os
import sys
import time
from fabric.api import lcd
from fabric.contrib.files import exists
from datalab.notebook_lib import *
from datalab.actions_lib import *
from datalab.fab import *
from datalab.common_lib import *

parser = argparse.ArgumentParser()
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--jars_dir', type=str, default='')
parser.add_argument('--cluster_dir', type=str, default='')
parser.add_argument('--datalake_enabled', type=str, default='')
parser.add_argument('--spark_configurations', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    configure_dataengine_spark(args.cluster_name, args.jars_dir, args.cluster_dir, args.datalake_enabled,
                               args.spark_configurations)
