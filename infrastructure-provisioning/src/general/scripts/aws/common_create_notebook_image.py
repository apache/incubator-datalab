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

import dlab.fab
import dlab.actions_lib
import dlab.meta_lib
import sys
import json
import uuid
import os


if __name__ == "__main__":
    try:
        image_conf = dict()
        dlab.actions_lib.create_aws_config_files()
        image_conf['service_base_name'] = os.environ['conf_service_base_name'] = dlab.actions_lib.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        image_conf['project_name'] = os.environ['project_name']
        image_conf['project_tag'] = os.environ['project_name']
        image_conf['endpoint_name'] = os.environ['endpoint_name']
        image_conf['instance_name'] = os.environ['notebook_instance_name']
        image_conf['instance_tag'] = '{}-tag'.format(image_conf['service_base_name'])
        image_conf['application'] = os.environ['application']
        image_conf['image_name'] = os.environ['notebook_image_name']
        image_conf['full_image_name'] = '{}-{}-{}-{}-{}'.format(image_conf['service_base_name'],
                                                                image_conf['project_name'],
                                                                image_conf['endpoint_name'],
                                                                image_conf['application'],
                                                                image_conf['image_name'])
        image_conf['tags'] = {"Name": image_conf['full_image_name'],
                              "SBN": image_conf['service_base_name'],
                              "Project": image_conf['project_name'],
                              "Image": image_conf['image_name'],
                              "FIN": image_conf['full_image_name'],
                              os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}

        ami_id = dlab.meta_lib.get_ami_id_by_name(image_conf['full_image_name'])
        if ami_id == '':
            try:
                os.environ['conf_additional_tags'] = os.environ['conf_additional_tags'] + \
                                                     ';project_tag:{0};endpoint_tag:{1};'.format(
                                                         os.environ['project_name'], os.environ['endpoint_name'])
            except KeyError:
                os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(
                    os.environ['project_name'], os.environ['endpoint_name'])
            image_id = dlab.actions_lib.create_image_from_instance(tag_name=image_conf['instance_tag'],
                                                                   instance_name=image_conf['instance_name'],
                                                                   image_name=image_conf['full_image_name'],
                                                                   tags=json.dumps(image_conf['tags']))
            print("Image was successfully created. It's name is {}".format(image_conf['full_image_name']))

            with open("/root/result.json", 'w') as result:
                res = {"notebook_image_name": image_conf['image_name'],
                       "full_image_name": image_conf['full_image_name'],
                       "project_name": image_conf['project_name'],
                       "application": image_conf['application'],
                       "status": "created",
                       "Action": "Create image from notebook"}
                result.write(json.dumps(res))
    except Exception as err:
        dlab.fab.append_result("Failed to create image from notebook", str(err))
        sys.exit(1)
