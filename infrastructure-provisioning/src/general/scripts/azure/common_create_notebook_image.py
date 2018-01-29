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

from dlab.actions_lib import *
from dlab.meta_lib import *
from dlab.fab import *
import sys
import json
import uuid


if __name__ == "__main__":
    try:
        image_id = ''
        image_conf = dict()
        image_conf['service_base_name'] = os.environ['conf_service_base_name']
        image_conf['user_name'] = os.environ['edge_user_name']
        image_conf['instance_name'] = os.environ['notebook_instance_name']
        image_conf['instance_tag'] = '{}-Tag'.format(image_conf['service_base_name'])
        image_conf['application'] = os.environ['application']
        image_conf['image_name'] = os.environ['notebook_image_name'].lower().replace('_', '-')
        image_conf['full_image_name'] = '{}-{}-{}-{}'.format(image_conf['service_base_name'],
                                                             image_conf['user_name'],
                                                             image_conf['application'],
                                                             image_conf['image_name']).lower()
        image_conf['tags'] = {"Name": image_conf['service_base_name'],
                              "SBN": image_conf['service_base_name'],
                              "User": image_conf['user_name'],
                              "Image": image_conf['image_name'],
                              "FIN": image_conf['full_image_name']}

        ami_id = get_ami_id_by_name(image_conf['full_image_name'])
        if ami_id == '':
            image_id = create_image_from_instance(tag_name=image_conf['instance_tag'],
                                                  instance_name=image_conf['instance_name'],
                                                  image_name=image_conf['full_image_name'],
                                                  tags=json.dumps(image_conf['tags']))
            if image_id != '':
                print("Image was successfully created. It's ID is {}".format(image_id))
        else:
            print("Couldn't find Image ID by name")

        with open("/root/result.json", 'w') as result:
            res = {"notebook_image_name": image_conf['image_name'],
                   "full_image_name": image_conf['full_image_name'],
                   "user_name": image_conf['user_name'],
                   "application": image_conf['application'],
                   "image_id": image_id,
                   "status": "created",
                   "Action": "Create image from notebook"}
            result.write(json.dumps(res))
    except Exception as err:
        append_result("Failed to create image from notebook", str(err))
        sys.exit(1)