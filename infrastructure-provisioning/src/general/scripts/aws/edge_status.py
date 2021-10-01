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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import os
import sys
import traceback
from fabric import *
from datalab.logger import logging
import subprocess

if __name__ == "__main__":
    datalab.actions_lib.create_aws_config_files()
    logging.info('Getting statuses of DataLab resources')

    try:
        logging.info('[COLLECT DATA]')
        params = '--list_resources "{}"'.format(os.environ['edge_list_resources'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_collect_data', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to collect necessary information.", str(err))
        sys.exit(1)
