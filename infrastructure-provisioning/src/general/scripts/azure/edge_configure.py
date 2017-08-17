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

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print 'Generating infrastructure names and tags'
    edge_conf = dict()

    edge_conf['service_base_name'] = os.environ['conf_service_base_name']
    edge_conf['key_name'] = os.environ['conf_key_name']
    edge_conf['vpc_name'] = os.environ['azure_vpc_name']
    edge_conf['subnet_name'] = os.environ['azure_subnet_name']
    edge_conf['user_keyname'] = os.environ['edge_user_name']
    edge_conf['private_subnet_name'] = edge_conf['service_base_name'] + '-' + os.environ['edge_user_name'] + '-subnet'
    edge_conf['instance_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-edge'
    edge_conf['container_name'] = (edge_conf['service_base_name'] + '-' + os.environ['edge_user_name']).lower().\
        replace('_', '-')
    edge_conf['shared_container_name'] = (edge_conf['service_base_name'] + '-shared').lower().replace('_', '')
    edge_conf['edge_security_group_name'] = edge_conf['instance_name'] + '-sg'
    edge_conf['notebook_security_group_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + \
                                                '-nb-sg'
    edge_conf['private_subnet_cidr'] = AzureMeta().get_subnet(edge_conf['service_base_name'], edge_conf['vpc_name'],
                                                              edge_conf['subnet_name']).address_prefix
    edge_conf['edge_public_ip'] = AzureMeta().get_instance_public_ip_address(edge_conf['service_base_name'],
                                                                   edge_conf['instance_name'])
    edge_conf['edge_private_ip'] = AzureMeta().get_instance_private_ip_address(edge_conf['service_base_name'],
                                                                               edge_conf['instance_name'])
    edge_conf['dlab_ssh_user'] = os.environ['conf_os_user']

    instance_hostname = AzureMeta().get_instance_public_ip_address(edge_conf['service_base_name'],
                                                                   edge_conf['instance_name'])
    keyfile_name = "/root/keys/{}.pem".format(edge_conf['key_name'])

    try:
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (instance_hostname, "/root/keys/" + os.environ['conf_key_name'] + ".pem", initial_user,
             edge_conf['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed creating ssh user 'dlab'.", str(err))
        AzureActions().remove_subnet(edge_conf['service_base_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['notebook_security_group_name'])
        AzureActions().remove_storage_account(edge_conf['service_base_name'], edge_conf['storage_account_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['instance_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['static_public_ip_name'])
        sys.exit(1)

    try:
        print '[INSTALLING PREREQUISITES]'
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {} --region {}".\
            format(instance_hostname, keyfile_name, edge_conf['dlab_ssh_user'], os.environ['azure_region'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing apps: apt & pip.", str(err))
        AzureActions().remove_subnet(edge_conf['service_base_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['notebook_security_group_name'])
        AzureActions().remove_storage_account(edge_conf['service_base_name'], edge_conf['storage_account_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['instance_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['static_public_ip_name'])
        sys.exit(1)

    try:
        print '[INSTALLING HTTP PROXY]'
        logging.info('[INSTALLING HTTP PROXY]')
        additional_config = {"exploratory_subnet": edge_conf['private_subnet_cidr'],
                             "template_file": "/root/templates/squid.conf"}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}" \
                 .format(instance_hostname, keyfile_name, json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_http_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing http proxy.", str(err))
        AzureActions().remove_subnet(edge_conf['service_base_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['notebook_security_group_name'])
        AzureActions().remove_storage_account(edge_conf['service_base_name'], edge_conf['storage_account_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['instance_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['static_public_ip_name'])
        sys.exit(1)


    try:
        print '[INSTALLING USERs KEY]'
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": edge_conf['user_keyname'],
                             "user_keydir": "/root/keys/"}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), edge_conf['dlab_ssh_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing users key. Excpeption: " + str(err))
        AzureActions().remove_subnet(edge_conf['service_base_name'], edge_conf['vpc_name'],
                                     edge_conf['private_subnet_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['edge_security_group_name'])
        AzureActions().remove_security_group(edge_conf['service_base_name'], edge_conf['notebook_security_group_name'])
        AzureActions().remove_storage_account(edge_conf['service_base_name'], edge_conf['storage_account_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['instance_name'])
        AzureActions().remove_instance(edge_conf['service_base_name'], edge_conf['static_public_ip_name'])
        sys.exit(1)

    try:
        print '[SUMMARY]'
        logging.info('[SUMMARY]')
        print "Instance name: " + edge_conf['instance_name']
        print "Hostname: " + instance_hostname
        print "Public IP: " + edge_conf['edge_public_ip']
        print "Private IP: " + edge_conf['edge_private_ip']
        print "Key name: " + edge_conf['key_name']
        print "Container name: " + edge_conf['bucketcontainer_name_name']
        print "Shared bucket name: " + edge_conf['shared_container_name']
        print "Notebook SG: " + edge_conf['notebook_security_group_name']
        print "Edge SG: " + edge_conf['edge_security_group_name']
        print "Notebook subnet: " + edge_conf['private_subnet_cidr']
        with open("/root/result.json", 'w') as result:
            res = {"hostname": instance_hostname,
                   "public_ip": edge_conf['edge_public_ip'],
                   "ip": edge_conf['edge_private_ip'],
                   "key_name": edge_conf['key_name'],
                   "user_own_bicket_name": edge_conf['container_name'],
                   "shared_bucket_name": edge_conf['shared_container_name'],
                   "tunnel_port": "22",
                   "socks_port": "1080",
                   "notebook_sg": edge_conf['notebook_security_group_name'],
                   "edge_sg": edge_conf['edge_security_group_name'],
                   "notebook_subnet": edge_conf['private_subnet_cidr'],
                   "full_edge_conf": edge_conf,
                   "Action": "Create new EDGE server"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)

    sys.exit(0)