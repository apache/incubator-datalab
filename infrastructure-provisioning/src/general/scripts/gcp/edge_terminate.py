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
from dlab.fab import *
from dlab.meta_lib import *
import sys, time, os
from dlab.actions_lib import *


def terminate_edge_node(user_name, service_base_name, region, zone):
    print "Terminating EDGE and notebook instances"
    try:
        instances = GCPMeta().get_list_instances(zone, service_base_name + '-' + user_name)
        if 'items' in instances:
            for i in instances['items']:
                GCPActions().remove_instance(i['name'], zone)
    except:
        sys.exit(1)

    print "Removing static addresses"
    try:
        static_addresses = GCPMeta().get_list_static_addresses(region, service_base_name + '-' + user_name)
        if 'items' in static_addresses:
            for i in static_addresses['items']:
                GCPActions().remove_static_address(i['name'], region)
    except:
        sys.exit(1)

    print "Removing s3 bucket"
    try:
        buckets = GCPMeta().get_list_buckets(service_base_name + '-' + user_name)
        if 'items' in buckets:
            for i in buckets['items']:
                GCPActions().remove_bucket(i['name'])
    except:
        sys.exit(1)

    print "Removing firewalls"
    try:
        firewalls = GCPMeta().get_list_firewalls(service_base_name + '-' + user_name)
        if 'items' in firewalls:
            for i in firewalls['items']:
                GCPActions().remove_firewall(i['name'])
    except:
        sys.exit(1)

    print "Removing subnets"
    try:
        list_subnets = GCPMeta().get_list_subnetworks(region, '', service_base_name + '-' + user_name)
        if 'items' in list_subnets:
            vpc_selflink = list_subnets['items'][0]['network']
            vpc_name = vpc_selflink.split('/')[-1]
            subnets = GCPMeta().get_list_subnetworks(region, vpc_name, service_base_name + '-' + user_name)
            for i in subnets['items']:
                GCPActions().remove_subnet(i['name'], region)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    print 'Generating infrastructure names and tags'
    edge_conf = dict()
    edge_conf['service_base_name'] = (os.environ['conf_service_base_name']).lower().replace('_', '-')
    edge_conf['user_name'] = (os.environ['edge_user_name']).lower().replace('_', '-')
    edge_conf['region'] = os.environ['gcp_region']
    edge_conf['zone'] = os.environ['gcp_zone']

    try:
        logging.info('[TERMINATE EDGE]')
        print '[TERMINATE EDGE]'
        try:
            terminate_edge_node(edge_conf['user_name'], edge_conf['service_base_name'], edge_conf['region'],
                                edge_conf['zone'])
        except Exception as err:
            traceback.print_exc()
            append_result("Failed to terminate edge.", str(err))
    except:
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": edge_conf['service_base_name'],
                   "user_name": edge_conf['user_name'],
                   "Action": "Terminate edge node"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)