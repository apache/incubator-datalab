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

import json
from fabric.api import *
import logging
import sys
import os
from dlab.fab import *
import traceback


def run():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                            os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        local("~/scripts/{}.py".format('project_prepare'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing Project.", str(err))
        sys.exit(1)

    try:
        local("~/scripts/{}.py".format('edge_configure'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring Edge node.", str(err))
        sys.exit(1)

# Main function for terminating EDGE node and exploratory environment if exists
def terminate():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        local("~/scripts/{}.py".format('project_terminate'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed terminating Edge node.", str(err))
        sys.exit(1)