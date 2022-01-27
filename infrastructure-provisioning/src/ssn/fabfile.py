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

import logging
import os
import sys
import traceback
import uuid
from datalab.fab import *
from fabric import *

@task
def run(ctx):
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    ssn_config = dict()
    ssn_config['ssn_unique_index'] = str(uuid.uuid4())[:5]
    try:
        subprocess.run("~/scripts/{}.py --ssn_unique_index {}".format('ssn_prepare', ssn_config['ssn_unique_index']),
                       shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing SSN node.", str(err))
        sys.exit(1)

    try:
        subprocess.run("~/scripts/{}.py --ssn_unique_index {}".format('ssn_configure', ssn_config['ssn_unique_index']),
                       shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring SSN node.", str(err))
        sys.exit(1)

@task
def terminate(ctx):
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        subprocess.run("~/scripts/{}.py".format('ssn_terminate'), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed terminating SSN node.", str(err))
        sys.exit(1)