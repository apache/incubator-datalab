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
    edge_conf['user_keyname'] = os.environ['edge_user_name']
    edge_conf['instance_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-edge'
    edge_conf['tag_name'] = edge_conf['service_base_name'] + '-Tag'
    edge_conf['bucket_name'] = (edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-bucket').lower().replace('_', '-')
    edge_conf['edge_security_group_name'] = edge_conf['instance_name'] + '-SG'
    edge_conf['notebook_instance_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-nb'
    edge_conf['notebook_role_profile_name'] = edge_conf['service_base_name'].lower().replace('-', '_') + "-" + \
                                              os.environ['edge_user_name'] + '-nb-Profile'
    edge_conf['notebook_security_group_name'] = edge_conf['service_base_name'] + "-" + os.environ[
        'edge_user_name'] + '-nb-SG'
    tag = {"Key": edge_conf['tag_name'], "Value": "{}-{}-subnet".format(edge_conf['service_base_name'], os.environ['edge_user_name'])}
    edge_conf['private_subnet_cidr'] = get_subnet_by_tag(tag)
    edge_conf['edge_public_ip'] = get_instance_ip_address(edge_conf['instance_name']).get('Public')
    edge_conf['edge_private_ip'] = get_instance_ip_address(edge_conf['instance_name']).get('Private')
    edge_conf['allocation_id'] = get_allocation_id_by_elastic_ip(edge_conf['edge_public_ip'])

    instance_hostname = get_instance_hostname(edge_conf['instance_name'])
    keyfile_name = "/root/keys/{}.pem".format(edge_conf['key_name'])

    try:
        print '[INSTALLING PREREQUISITES]'
        logging.info('[INSTALLING PREREQUISITES]')
        params = "--hostname {} --keyfile {} --user {}".format(instance_hostname, keyfile_name, os.environ['conf_os_user'])
        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing apps: apt & pip.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['edge_user_name'])
        sys.exit(1)

    try:
        print '[INSTALLING HTTP PROXY]'
        logging.info('[INSTALLING HTTP PROXY]')
        additional_config = {"exploratory_subnet": edge_conf['private_subnet_cidr'],
                             "template_file": "/root/templates/squid.conf"}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}" \
                 .format(instance_hostname, keyfile_name, json.dumps(additional_config), os.environ['conf_os_user'])
        try:
            local("~/scripts/{}.py {}".format('configure_http_proxy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing http proxy.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['edge_user_name'])
        sys.exit(1)


    try:
        print '[INSTALLING USERs KEY]'
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": edge_conf['user_keyname'],
                             "user_keydir": "/root/keys/"}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            instance_hostname, keyfile_name, json.dumps(additional_config), os.environ['conf_os_user'])
        try:
            local("~/scripts/{}.py {}".format('install_user_key', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing users key. Excpeption: " + str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['edge_user_name'])
        sys.exit(1)

    try:
        print '[SUMMARY]'
        logging.info('[SUMMARY]')
        print "Instance name: " + edge_conf['instance_name']
        print "Hostname: " + instance_hostname
        print "Public IP: " + edge_conf['edge_public_ip']
        print "Private IP: " + edge_conf['edge_private_ip']
        print "Instance ID: " + get_instance_by_name(edge_conf['instance_name'])
        print "Key name: " + edge_conf['key_name']
        print "Bucket name: " + edge_conf['bucket_name']
        print "Notebook SG: " + edge_conf['notebook_security_group_name']
        print "Notebook profiles: " + edge_conf['notebook_role_profile_name']
        print "Edge SG: " + edge_conf['edge_security_group_name']
        print "Notebook subnet: " + edge_conf['private_subnet_cidr']
        with open("/root/result.json", 'w') as result:
            res = {"hostname": instance_hostname,
                   "public_ip": edge_conf['edge_public_ip'],
                   "ip": edge_conf['edge_private_ip'],
                   "instance_id": get_instance_by_name(edge_conf['instance_name']),
                   "key_name": edge_conf['key_name'],
                   "user_own_bicket_name": edge_conf['bucket_name'],
                   "tunnel_port": "22",
                   "socks_port": "1080",
                   "notebook_sg": edge_conf['notebook_security_group_name'],
                   "notebook_profile": edge_conf['notebook_role_profile_name'],
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