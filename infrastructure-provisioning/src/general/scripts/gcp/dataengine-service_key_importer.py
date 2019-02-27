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

# Not used in GCP

import argparse
import subprocess


parser = argparse.ArgumentParser()
parser.add_argument('--user_name', type=str, default='')
args = parser.parse_args()

if __name__ == "__main__":
    subprocess.Popen('cat /tmp/{}.pub >> /home/hadoop/.ssh/authorized_keys'.format(args.user_name), shell=True)
