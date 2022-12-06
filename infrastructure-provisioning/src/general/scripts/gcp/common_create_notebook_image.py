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
from datalab.logger import logging
import json
import os
import sys

if __name__ == "__main__":
    try:
        image_conf = dict()
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        try:
            image_conf['exploratory_name'] = (os.environ['exploratory_name']).replace('_', '-').lower()
        except:
            image_conf['exploratory_name'] = ''
        image_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True).lower()
        image_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        image_conf['endpoint_tag'] = image_conf['endpoint_name']
        image_conf['project_name'] = os.environ['project_name'].lower()
        image_conf['project_tag'] = image_conf['project_name']
        image_conf['instance_name'] = os.environ['notebook_instance_name']
        image_conf['instance_tag'] = '{}-tag'.format(image_conf['service_base_name'])
        image_conf['application'] = os.environ['application']
        image_conf['image_name'] = os.environ['notebook_image_name'].replace('_', '-').lower()
        image_conf['expected_primary_image_name'] = '{}-{}-{}-{}-primary-image-{}'.format(
            image_conf['service_base_name'], image_conf['project_name'], image_conf['endpoint_name'],
            os.environ['application'], image_conf['image_name'])
        image_conf['expected_secondary_image_name'] = '{}-{}-{}-{}-secondary-image-{}'.format(
            image_conf['service_base_name'], image_conf['project_name'], image_conf['endpoint_name'],
            os.environ['application'], image_conf['image_name'])
        image_conf['image_labels'] = {"sbn": image_conf['service_base_name'],
                                           "endpoint_tag": image_conf['endpoint_tag'],
                                           "project_tag": image_conf['project_tag'],
                                           "image": image_conf['image_name'],
                                           os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']}
        image_conf['instance_name'] = '{0}-{1}-{2}-nb-{3}'.format(image_conf['service_base_name'],
                                                                  image_conf['project_name'],
                                                                  image_conf['endpoint_name'],
                                                                  image_conf['exploratory_name'])

        if "gcp_wrapped_csek" in os.environ:
            image_conf['gcp_wrapped_csek'] = os.environ['gcp_wrapped_csek']
        else:
            image_conf['gcp_wrapped_csek'] = ''

        image_conf['zone'] = os.environ['gcp_zone']
        logging.info('[CREATING IMAGE]')
        primary_image_id = GCPMeta.get_image_by_name(image_conf['expected_primary_image_name'])
        if primary_image_id == '':
            image_id_list = GCPActions.create_image_from_instance_disks(
                image_conf['expected_primary_image_name'], image_conf['expected_secondary_image_name'],
                image_conf['instance_name'], image_conf['zone'], image_conf['image_labels'],
                image_conf['gcp_wrapped_csek'])
            if image_id_list and image_id_list[0] != '':
                logging.info("Image of primary disk was successfully created. It's ID is {}".format(image_id_list[0]))
            else:
                logging.info("Looks like another image creating operation for your template have been started a "
                      "moment ago.")
            if image_id_list and image_id_list[1] != '':
                logging.info("Image of secondary disk was successfully created. It's ID is {}".format(image_id_list[1]))

            with open("/root/result.json", 'w') as result:
                res = {"primary_image_name": image_conf['expected_primary_image_name'],
                       "secondary_image_name": image_conf['expected_secondary_image_name'],
                       "full_image_name": image_conf['expected_primary_image_name'],
                       "project_name": image_conf['project_name'],
                       "application": image_conf['application'],
                       "status": "active",
                       "Action": "Create image from notebook"}
                result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Failed to create image from notebook", str(err))
        sys.exit(1)
