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
from datalab.fab import *
from fabric import *
import subprocess

@task
def run(ctx):
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        subprocess.run("~/scripts/{}.py".format('project_prepare'), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing Project.", str(err))
        sys.exit(1)

    try:
        subprocess.run("~/scripts/{}.py".format('edge_configure'), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring Edge node.", str(err))
        sys.exit(1)

# Main function for terminating EDGE node and exploratory environment if exists
@task
def terminate(ctx):
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    try:
        subprocess.run("~/scripts/{}.py".format('project_terminate'), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed terminating Edge node.", str(err))
        sys.exit(1)

# Main function for EDGE node creation if it was terminated or failed
@task
def recreate(ctx):
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'],  os.environ['project_name'], os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        subprocess.run("~/scripts/{}.py".format('project_prepare'), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing Edge node.", str(err))
        sys.exit(1)

    try:
        subprocess.run("~/scripts/{}.py".format('edge_configure'), shell=True, check=True)
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring Edge node.", str(err))
        sys.exit(1)