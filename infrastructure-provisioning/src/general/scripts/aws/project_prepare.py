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

import json
from dlab.fab import *
from dlab.meta_lib import *
import sys, time, os
from dlab.actions_lib import *
import traceback

if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_tag'], os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    create_aws_config_files()
    print('Generating infrastructure names and tags')
    project_conf = dict()

# Subnet creation

    project_conf['service_base_name'] = os.environ['conf_service_base_name']
    project_conf['project_tag_value'] = os.environ['project_tag']
    project_conf['vpc_id'] = os.environ['aws_vpc_id']
    project_conf['region'] = os.environ['aws_region']
    project_conf['zone'] = os.environ['aws_region'] + os.environ['aws_zone']
    project_conf['tag_name'] = '{}-Tag'.format(project_conf['service_base_name'])
    project_conf['tag_name_value'] = '{0}-{1}-subnet'.format(project_conf['service_base_name'], project_conf['project_tag_value'])
    project_conf['private_subnet_prefix'] = os.environ['aws_private_subnet_prefix']
    project_conf['private_subnet_name'] = '{0}-{1}-subnet'.format(project_conf['service_base_name'], project_conf['project_tag_value'])

    try:
        project_conf['vpc2_id'] = os.environ['aws_vpc2_id']
        project_conf['tag_name'] = '{}-secondary-Tag'.format(project_conf['service_base_name'])
    except KeyError:
        project_conf['vpc2_id'] = project_conf['vpc_id']

    try:
        if os.environ['conf_user_subnets_range'] == '':
            raise KeyError
    except KeyError:
        os.environ['conf_user_subnets_range'] = ''


    print("Will create exploratory environment as following: {}".
          format(json.dumps(project_conf, sort_keys=True, indent=4, separators=(',', ': '))))
    logging.info(json.dumps(project_conf))


    try:
        logging.info('[CREATE SUBNET]')
        print('[CREATE SUBNET]')
        params = "--vpc_id '{}' --infra_tag_name {} --infra_tag_value {} --prefix {} " \
                 "--user_subnets_range '{}' --subnet_name {} --zone {}".format(
            project_conf['vpc2_id'], project_conf['tag_name'], project_conf['service_base_name'],
            project_conf['private_subnet_prefix'], os.environ['conf_user_subnets_range'], project_conf['private_subnet_name'],
            project_conf['zone'])
        try:
            local("~/scripts/{}.py {}".format('common_create_subnet', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    tag = {"Key": project_conf['tag_name'], "Value": "{0}-{1}-subnet".format(project_conf['service_base_name'], project_conf['project_tag_value'])}
    project_conf['private_subnet_cidr'] = get_subnet_by_tag(tag)
    project_tag = {"Key": 'project_tag', "Value": project_conf['project_tag_value']}
    subnet_id = get_subnet_by_cidr(project_conf['private_subnet_cidr'])
    print('subnet id: {}'.format(subnet_id))
    create_tag(subnet_id, project_tag)
    print('NEW SUBNET CIDR CREATED: {}'.format(project_conf['private_subnet_cidr']))

#End of subnet creation

#Project roles creation

    project_conf['role_name'] = '{0}-{1}-project-Role'.format(edge_conf['service_base_name'].lower().replace('-', '_'),
                                                      os.environ['project_tag'])
    project_conf['role_profile_name'] = '{0}-{1}-project-Profile'.format(project_conf['service_base_name'].lower().replace('-',
                                                                                                                '_'),
                                                                 os.environ['project_tag'])
    project_conf['policy_name'] = '{0}-{1}-project-Policy'.format(project_conf['service_base_name'].lower().replace('-', '_'),
                                                          os.environ['project_tag'])

    try:
        logging.info('[CREATE PROJECT ROLES]')
        print('[CREATE PROJECT ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {} --infra_tag_name {} " \
                 "--infra_tag_value {}" \
                 .format(project_conf['role_name'], project_conf['role_profile_name'],
                         project_conf['policy_name'], os.environ['aws_region'], project_conf['tag_name'],
                         project_conf['service_base_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_role_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to creating roles.", str(err))
        sys.exit(1)

#End of project roles creation

#Creating security group for EDGE node

    project_conf['edge_instance_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'], os.environ['project_tag'])
    project_conf['edge_security_group_name'] = '{}-SG'.format(edge_conf['instance_name'])
    project_conf['notebook_instance_name'] = '{}-{}-nb'.format(edge_conf['service_base_name'],
                                                            os.environ['project_tag'])

    project_conf['allowed_ip_cidr'] = list()
    for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
        project_conf['allowed_ip_cidr'].append({"CidrIp": cidr.replace(' ','')})

    try:
        logging.info('[CREATE SECURITY GROUP FOR EDGE NODE]')
        print('[CREATE SECURITY GROUPS FOR EDGE]')
        edge_sg_ingress = format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "UserIdGroupPairs": [], "PrefixListIds": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 22,
                "IpRanges": project_conf['allowed_ip_cidr'],
                "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 3128,
                "IpRanges": project_conf['allowed_ip_cidr'],
                "ToPort": 3128, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 80,
                "IpRanges": project_conf['allowed_ip_cidr'],
                "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": get_instance_ip_address(project_conf['tag_name'], '{}-ssn'.format(
                    project_conf['service_base_name'])).get('Private') + "/32"}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            }
        ])
        edge_sg_egress = format_sg([
            {
                "PrefixListIds": [],
                "FromPort": 22,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8888,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8888, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8080,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8080, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8787,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8787, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 6006,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 6006, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 20888,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 20888, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8042,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8042, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8088,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8088, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8081,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8081, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 4040,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 4140, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 18080,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 18080, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 50070,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 50070, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 53,
                "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                "ToPort": 53, "IpProtocol": "udp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 80,
                "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 123,
                "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                "ToPort": 123, "IpProtocol": "udp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 443,
                "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8085,
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "ToPort": 8085, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 389,
                "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                "ToPort": 389, "IpProtocol": "tcp", "UserIdGroupPairs": []
            }
        ])
        params = "--name {} --vpc_id {} --security_group_rules '{}' --infra_tag_name {} --infra_tag_value {} \
            --egress '{}' --force {} --nb_sg_name {} --resource {}".\
            format(project_conf['edge_security_group_name'], project_conf['vpc_id'], json.dumps(edge_sg_ingress),
                   project_conf['service_base_name'], project_conf['edge_instance_name'], json.dumps(edge_sg_egress),
                   True, project_conf['notebook_instance_name'], 'edge')
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except Exception as err:
            traceback.print_exc()
            append_result("Failed creating security group for edge node.", str(err))
            raise Exception

        with hide('stderr', 'running', 'warnings'):
            print('Waiting for changes to propagate')
            time.sleep(10)
    except:
        remove_all_iam_resources('notebook', os.environ['project_tag'])
        remove_all_iam_resources('edge', os.environ['project_tag'])
        sys.exit(1)

#End of security groupe for EDGE node creating