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
import os


if __name__ == "__main__":
    try:
        image_conf = dict()
        image_conf['full_image_name'] = os.environ['notebook_image_name']
        image = dlab.meta_lib.GCPMeta.get_list_images(image_conf['full_image_name'])
        if image != '':
            dlab.actions_lib.GCPActions.remove_image(image)
            with open("/root/result.json", 'w') as result:
                res = {"notebook_image_name": image_conf['full_image_name'],
                       "status": "terminated",
                       "Action": "Delete existing notebook image"}
                result.write(json.dumps(res))

    except Exception as err:
        dlab.fab.append_result("Failed to delete existing notebook image", str(err))
        sys.exit(1)
