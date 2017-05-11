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
import os
import sys
import logging
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
from fabric.operations import *

if __name__ == "__main__":
    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    notebook_config = dict()
    notebook_config['notebook_name'] = os.environ['notebook_instance_name']
    notebook_config['os_user'] = os.environ['conf_os_user']
    notebook_config['notebook_ip'] = get_instance_ip_address(notebook_config['notebook_name']).get('Private')
    notebook_config['keyfile'] = os.environ['conf_key_dir'] + '/' + os.environ['conf_key_name'] + '.pem'

    try:
        logging.info('[GETTING ALL AVAILABLE PACKAGES]')
        print '[GETTING ALL AVAILABLE PACKAGES]'
        params = "--os_user {} --notebook_ip {} --keyfile '{}'" \
            .format(notebook_config['os_user'], notebook_config['notebook_ip'], notebook_config['keyfile'])
        try:
            # Run script to get available libs
            local("~/scripts/{}.py {}".format('get_all_available_pkgs', params))

        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to get available libraries. Exception: " + str(err))
        sys.exit(1)
