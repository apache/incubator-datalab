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

import argparse
from dlab.actions_lib import *
from dlab.meta_lib import *
from dlab.fab import *
import sys
import json
import uuid


parser = argparse.ArgumentParser()
parser.add_argument('--image_name', type=str, default='')
parser.add_argument('--application', type=str, default='')
parser.add_argument('--instance_name', type=str, default='')
parser.add_argument('--edge_user_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    try:
        image_id = ''
        image_conf = dict()
        image_conf['uuid'] = str(uuid.uuid4())[:5]
        image_conf['service_base_name'] = os.environ['conf_service_base_name']
        image_conf['full_image_name'] = '{}-{}-{}-{}-{}'.format(image_conf['service_base_name'],
                                                                args.edge_user_name,
                                                                args.application,
                                                                args.image_name,
                                                                image_conf['uuid']).lower()
        image_conf['tags'] = {"Name": args.image_name,
                              "SBN": image_conf['service_base_name'],
                              "User": args.edge_user_name}
        image_conf['instance_tag'] = '{}-Tag'.format(image_conf['service_base_name'])
        image_conf['instance_name'] = args.instance_name
        ami_id = get_ami_id_by_name(image_conf['full_image_name'])
        if ami_id == '':
            print("Looks like it's first time we configure notebook server. Creating image.")
            image_id = create_image_from_instance(tag_name=image_conf['instance_tag'],
                                                  instance_name=image_conf['instance_name'],
                                                  image_name=image_conf['full_image_name'],
                                                  tags=json.dumps(image_conf['tags']))
            if image_id != '':
                print("Image was successfully created. It's ID is {}".format(image_id))

        with open("/root/result.json", 'w') as result:
            res = {"image_name": args.image_name,
                   "full_image_name": image_conf['full_image_name'],
                   "user_name": args.edge_user_name,
                   "application": args.application,
                   "image_id": image_id}
            result.write(json.dumps(res))
    except Exception as err:
        append_result("Failed to create image from notebook", str(err))
        sys.exit(1)