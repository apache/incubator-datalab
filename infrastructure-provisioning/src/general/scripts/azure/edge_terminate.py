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
import sys, time, os
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *


def terminate_edge_node(resource_group_name, user_env_prefix, storage_account_name, subnet_name):
    # print 'Terminating EMR cluster'
    # try:
    #     clusters_list = get_emr_list(tag_name)
    #     if clusters_list:
    #         for cluster_id in clusters_list:
    #             client = boto3.client('emr')
    #             cluster = client.describe_cluster(ClusterId=cluster_id)
    #             cluster = cluster.get("Cluster")
    #             emr_name = cluster.get('Name')
    #             terminate_emr(cluster_id)
    #             print "The EMR cluster " + emr_name + " has been terminated successfully"
    #     else:
    #         print "There are no EMR clusters to terminate."
    # except:
    #     sys.exit(1)

    # print "Deregistering notebook's AMI"
    # try:
    #     deregister_image(user_name)
    # except:
    #     sys.exit(1)

    print "Terminating EDGE and notebook instances, network interfaces"
    try:
        for vm in AzureMeta().compute_client.virtual_machines.list(resource_group_name):
            if user_env_prefix in vm.name:
                AzureActions().remove_instance(resource_group_name, vm.name)
                print "Instance {} has been terminated".format(vm.name)
                AzureActions().delete_network_if(resource_group_name, vm.name + '-nif')
                print "Network interface {} has been terminated".format(vm.name + '-nif')
    except:
        sys.exit(1)

    print "Removing storage account"
    try:
        AzureActions().remove_storage_account(resource_group_name, storage_account_name)
    except:
        sys.exit(1)

    # print "Removing IAM roles and profiles"
    # try:
    #     remove_all_iam_resources('notebook', user_name)
    #     remove_all_iam_resources('edge', user_name)
    # except:
    #     sys.exit(1)

    print "Removing security groups"
    try:
        for sg in AzureMeta().network_client.network_security_groups.list(resource_group_name):
            if user_env_prefix in sg.name:
                AzureActions().remove_security_group(resource_group_name, sg.name)
    except:
        sys.exit(1)

    print "Removing private subnet"
    try:
        for vpc in AzureActions().network_client.virtual_networks.list("dlab-1808"):
            AzureActions().remove_subnet(resource_group_name, vpc.name, subnet_name)
    except:
        sys.exit(1)

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print 'Generating infrastructure names and tags'
    edge_conf = dict()
    edge_conf['resource_group_name'] = os.environ['azure_resource_group_name']
    edge_conf['user_name'] = os.environ['edge_user_name']
    edge_conf['user_env_prefix'] = os.environ['conf_service_base_name'] + "-" + edge_conf['user_name']
    edge_conf['storage_account_name'] = (os.environ['conf_service_base_name'] + edge_conf['user_name']).lower().\
        replace('_', '').replace('-', '')
    edge_conf['private_subnet_name'] = os.environ['conf_service_base_name'] + "-" + edge_conf['user_name'] + '-subnet'


    try:
        logging.info('[TERMINATE EDGE]')
        print '[TERMINATE EDGE]'
        try:
            terminate_edge_node(edge_conf['resource_group_name'], edge_conf['user_env_prefix'],
                                edge_conf['storage_account_name'], edge_conf['private_subnet_name'])
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
