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
import time
from fabric.api import *
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import sys
import os
import uuid
import logging


def run():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        local("~/scripts/{}.py".format('dataengine_prepare'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing Data Engine.", str(err))
        sys.exit(1)

    try:
        local("~/scripts/{}.py".format('dataengine_configure'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring Data Engine.", str(err))
        sys.exit(1)


def start():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        local("~/scripts/{}.py".format('dataengine_start'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed starting Data Engine.", str(err))
        sys.exit(1)


# Main function for installing additional libraries for Dataengine
def install_libs():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        local("~/scripts/{}.py".format('dataengine_install_libs'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed installing additional libs for DataEngine.", str(err))
        sys.exit(1)


# Main function for get available libraries for Data Engine
def list_libs():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        local("~/scripts/{}.py".format('dataengine_list_libs'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed get available libraries for Data Engine.", str(err))
        sys.exit(1)


def stop():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        local("~/scripts/{}.py".format('dataengine_stop'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed stopping Data Engine.", str(err))
        sys.exit(1)


def terminate():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    try:
        local("~/scripts/{}.py".format('dataengine_terminate'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed terminating Data Engine.", str(err))
        sys.exit(1)