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
import json
import os
import sys

if __name__ == "__main__":
    try:
        datalab.actions_lib.create_aws_config_files()
        image_conf = dict()
        image_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        image_conf['project_name'] = os.environ['project_name']
        image_conf['endpoint_name'] = os.environ['endpoint_name']
        image_conf['application'] = os.environ['application']
        image_conf['image_name'] = os.environ['notebook_image_name']
        image_conf['full_image_name'] = '{}-{}-{}-{}-{}'.format(image_conf['service_base_name'],
                                                                image_conf['project_name'],
                                                                image_conf['endpoint_name'],
                                                                image_conf['application'],
                                                                image_conf['image_name'])
        image_id = datalab.meta_lib.get_ami_id_by_name(image_conf['full_image_name'], 'available')
        if image_id != '':
            datalab.actions_lib.deregister_image(image_conf['full_image_name'])

        with open("/root/result.json", 'w') as result:
            res = {"notebook_image_name": image_conf['full_image_name'],
                   "endpoint": image_conf['endpoint_name'],
                   "project": image_conf['project_name'],
                   "imageName": image_conf['image_name'],
                   "status": "terminated",
                   "Action": "Delete existing notebook image"}
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Failed to delete existing notebook image", str(err))
        sys.exit(1)
