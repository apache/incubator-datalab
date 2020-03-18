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

import logging
import json
import sys
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import os

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print('Generating infrastructure names and tags')
    odahu_conf = dict()
    odahu_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    odahu_conf['odahu_cluster_name'] = (os.environ['odahu_cluster_name']).lower().replace('_', '-')
    odahu_conf['tag_name'] = '{}-tag'.format(odahu_conf['service_base_name'])
    odahu_conf['endpoint_tag'] = (os.environ['endpoint_name']).lower().replace('_', '-')
    odahu_conf['project_tag'] = (os.environ['project_name']).lower().replace('_', '-')
    odahu_conf['region'] = os.environ['gcp_region']
    odahu_conf['bucket_name'] = "{}-tfstate".format((os.environ['odahu_cluster_name']).lower().replace('_', '-'))
    odahu_conf['static_address_name'] = "{}-nat-gw".format((os.environ['odahu_cluster_name']).lower().replace('_', '-'))


    try:
        logging.info('[CREATE STATE BUCKETS]')
        print('[CREATE STATE BUCKETS]')

        odahu_conf['bucket_tags'] = {
            odahu_conf['tag_name']: odahu_conf['bucket_name'],
            "endpoint_tag": odahu_conf['endpoint_tag'],
            os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value'],
            "sbn": odahu_conf['service_base_name'],
            "project_tag": odahu_conf['project_tag']}

        params = "--bucket_name {} --tags '{}'".format(odahu_conf['bucket_name'], json.dumps(odahu_conf['bucket_tags']))

        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create bucket.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE NAT GATEWAY]')
        print('[CREATE NAT GATEWAY]')
        GCPActions().create_static_address(odahu_conf['static_address_name'], odahu_conf['region'])
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to reserve static ip.", str(err))
        GCPActions().remove_bucket(odahu_conf['bucket_name'])
        sys.exit(1)