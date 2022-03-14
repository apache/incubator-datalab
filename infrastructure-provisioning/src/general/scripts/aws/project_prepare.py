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

import boto3
import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
import os
import sys
import time
import traceback
import subprocess
from fabric import *
from datalab.logger import logging

if __name__ == "__main__":
    try:
        datalab.actions_lib.create_aws_config_files()
        logging.info('Generating infrastructure names and tags')
        project_conf = dict()
        project_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        project_conf['endpoint_name'] = os.environ['endpoint_name']
        project_conf['endpoint_tag'] = project_conf['endpoint_name']
        project_conf['project_name'] = os.environ['project_name']
        project_conf['project_tag'] = project_conf['project_name']
        project_conf['key_name'] = os.environ['conf_key_name']
        project_conf['public_subnet_id'] = os.environ['aws_subnet_id']
        project_conf['vpc_id'] = os.environ['aws_vpc_id']
        project_conf['region'] = os.environ['aws_region']
        project_conf['ami_id'] = datalab.meta_lib.get_ami_id(os.environ['aws_{}_image_name'.format(
            os.environ['conf_os_family'])])
        project_conf['instance_size'] = os.environ['aws_edge_instance_size']
        project_conf['sg_ids'] = os.environ['aws_security_groups_ids']
        project_conf['instance_class'] = 'edge'
        project_conf['edge_instance_name'] = '{}-{}-{}-edge'.format(project_conf['service_base_name'],
                                                                    project_conf['project_name'],
                                                                    project_conf['endpoint_name'])
        project_conf['tag_name'] = '{}-tag'.format(project_conf['service_base_name'])
        project_conf['bucket_name_tag'] = '{0}-{1}-{2}-bucket'.format(project_conf['service_base_name'],
                                                                      project_conf['project_name'],
                                                                      project_conf['endpoint_name'])
        project_conf['bucket_name'] = project_conf['bucket_name_tag'].lower().replace('_', '-')
        project_conf['bucket_versioning_enabled'] = os.environ['conf_bucket_versioning_enabled']
        project_conf['shared_bucket_name_tag'] = '{0}-{1}-shared-bucket'.format(
            project_conf['service_base_name'], project_conf['endpoint_tag'])
        project_conf['shared_bucket_name'] = project_conf['shared_bucket_name_tag'].lower().replace('_', '-')
        project_conf['edge_role_name'] = '{}-{}-{}-edge-role'.format(project_conf['service_base_name'],
                                                                     project_conf['project_name'],
                                                                     project_conf['endpoint_name'])
        project_conf['edge_role_profile_name'] = '{}-{}-{}-edge-profile'.format(
            project_conf['service_base_name'], project_conf['project_name'], project_conf['endpoint_name'])
        project_conf['edge_policy_name'] = '{}-{}-{}-edge-policy'.format(
            project_conf['service_base_name'], project_conf['project_name'], project_conf['endpoint_name'])
        project_conf['edge_security_group_name'] = '{}-{}-{}-edge-sg'.format(project_conf['service_base_name'],
                                                                             project_conf['project_name'],
                                                                             project_conf['endpoint_name'])
        project_conf['notebook_instance_name'] = '{}-{}-{}-nb'.format(project_conf['service_base_name'],
                                                                      project_conf['project_name'],
                                                                      project_conf['endpoint_name'])
        project_conf['dataengine_instances_name'] = '{}-{}-{}-de'.format(project_conf['service_base_name'],
                                                                         project_conf['project_name'],
                                                                         project_conf['endpoint_name'])
        project_conf['notebook_dataengine_role_name'] = '{}-{}-{}-nb-de-role'.format(project_conf['service_base_name'],
                                                                                     project_conf['project_name'],
                                                                                     project_conf['endpoint_name'])
        project_conf['notebook_dataengine_policy_name'] = '{}-{}-{}-nb-de-policy'.format(
            project_conf['service_base_name'], project_conf['project_name'], project_conf['endpoint_name'])
        project_conf['notebook_dataengine_role_profile_name'] = '{}-{}-{}-nb-de-profile'.format(
            project_conf['service_base_name'], project_conf['project_name'], project_conf['endpoint_name'])
        project_conf['notebook_security_group_name'] = '{}-{}-{}-nb-sg'.format(project_conf['service_base_name'],
                                                                               project_conf['project_name'],
                                                                               project_conf['endpoint_name'])
        project_conf['private_subnet_prefix'] = os.environ['conf_private_subnet_prefix']
        project_conf['private_subnet_name'] = '{0}-{1}-{2}-subnet'.format(project_conf['service_base_name'],
                                                                          project_conf['project_name'],
                                                                          project_conf['endpoint_name'])
        project_conf['dataengine_master_security_group_name'] = '{}-{}-{}-de-master-sg'.format(
            project_conf['service_base_name'], project_conf['project_name'], project_conf['endpoint_name'])
        project_conf['dataengine_slave_security_group_name'] = '{}-{}-{}-de-slave-sg'.format(
            project_conf['service_base_name'], project_conf['project_name'], project_conf['endpoint_name'])
        project_conf['allowed_ip_cidr'] = list()
        for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
            project_conf['allowed_ip_cidr'].append({"CidrIp": cidr.replace(' ', '')})
        project_conf['network_type'] = os.environ['conf_network_type']
        project_conf['all_ip_cidr'] = '0.0.0.0/0'
        project_conf['zone'] = os.environ['aws_region'] + os.environ['aws_zone']
        project_conf['elastic_ip_name'] = '{0}-{1}-{2}-edge-static-ip'.format(project_conf['service_base_name'],
                                                                              project_conf['project_name'],
                                                                              project_conf['endpoint_name'])
        project_conf['provision_instance_ip'] = None
        project_conf['local_endpoint'] = False
        try:
            project_conf['provision_instance_ip'] = '{}/32'.format(datalab.meta_lib.get_instance_ip_address(
                project_conf['tag_name'], '{0}-{1}-endpoint'.format(project_conf['service_base_name'],
                                                                    project_conf['endpoint_name'])).get('Private'))
        except:
            project_conf['provision_instance_ip'] = '{}/32'.format(datalab.meta_lib.get_instance_ip_address(
                project_conf['tag_name'], '{0}-ssn'.format(project_conf['service_base_name'])).get('Private'))
            project_conf['local_endpoint'] = True
        if 'aws_user_predefined_s3_policies' not in os.environ:
            os.environ['aws_user_predefined_s3_policies'] = 'None'

        try:
            if os.environ['conf_user_subnets_range'] == '':
                raise KeyError
        except KeyError:
            os.environ['conf_user_subnets_range'] = ''

        # FUSE in case of absence of user's key
        try:
            project_conf['user_key'] = os.environ['key']
            try:
                subprocess.run('echo "{0}" >> {1}{2}.pub'.format(project_conf['user_key'], os.environ['conf_key_dir'],
                                                        project_conf['project_name']), shell=True, check=True)
            except:
                logging.error("ADMINSs PUBLIC KEY DOES NOT INSTALLED")
        except KeyError:
            logging.error("ADMINSs PUBLIC KEY DOES NOT UPLOADED")
            sys.exit(1)

        logging.info("Will create exploratory environment with edge node as access point as following: {}".
              format(json.dumps(project_conf, sort_keys=True, indent=4, separators=(',', ': '))))
        logging.info(json.dumps(project_conf))

        if 'conf_additional_tags' in os.environ:
            project_conf['bucket_additional_tags'] = ';' + os.environ['conf_additional_tags']
            os.environ['conf_additional_tags'] = os.environ['conf_additional_tags'] + \
                                                 ';project_tag:{0};endpoint_tag:{1};'.format(
                                                     project_conf['project_tag'], project_conf['endpoint_tag'])
        else:
            project_conf['bucket_additional_tags'] = ''
            os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(project_conf['project_tag'],
                                                                                           project_conf['endpoint_tag'])
        logging.info('Additional tags will be added: {}'.format(os.environ['conf_additional_tags']))
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    if not project_conf['local_endpoint']:
        # attach project_tag and endpoint_tag to endpoint
        try:
            endpoint_id = datalab.meta_lib.get_instance_by_name(project_conf['tag_name'], '{0}-{1}-endpoint'.format(
                project_conf['service_base_name'], project_conf['endpoint_name']))
            logging.info("Endpoint id: " + endpoint_id)
            ec2 = boto3.client('ec2')
            ec2.create_tags(Resources=[endpoint_id], Tags=[
                {'Key': 'project_tag', 'Value': project_conf['project_tag']},
                {'Key': 'endpoint_tag', 'Value': project_conf['endpoint_tag']}])
        except Exception as err:
            logging.error("Failed to attach Project tag to Endpoint", str(err))
            traceback.print_exc()
            sys.exit(1)

    try:
        project_conf['vpc2_id'] = os.environ['aws_vpc2_id']
        project_conf['tag_name'] = '{}-secondary-tag'.format(project_conf['service_base_name'])
    except KeyError:
        project_conf['vpc2_id'] = project_conf['vpc_id']



    try:
        logging.info('[CREATE SUBNET]')
        params = "--vpc_id '{}' --infra_tag_name {} --infra_tag_value {} --prefix {} " \
                 "--user_subnets_range '{}' --subnet_name {} --zone {}".format(
                  project_conf['vpc2_id'], project_conf['tag_name'], project_conf['service_base_name'],
                  project_conf['private_subnet_prefix'], os.environ['conf_user_subnets_range'],
                  project_conf['private_subnet_name'],
                  project_conf['zone'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_subnet', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    tag = {"Key": project_conf['tag_name'],
           "Value": "{0}-{1}-{2}-subnet".format(project_conf['service_base_name'], project_conf['project_name'],
                                                project_conf['endpoint_name'])}
    project_conf['private_subnet_cidr'] = datalab.meta_lib.get_subnet_by_tag(tag)
    subnet_id = datalab.meta_lib.get_subnet_by_cidr(project_conf['private_subnet_cidr'], project_conf['vpc2_id'])
    logging.info('Subnet id: {}'.format(subnet_id))
    logging.info('NEW SUBNET CIDR CREATED: {}'.format(project_conf['private_subnet_cidr']))

    try:
        logging.info('[CREATE EDGE ROLES]')
        user_tag = "{0}:{0}-{1}-{2}-edge-role".format(project_conf['service_base_name'], project_conf['project_name'],
                                                      project_conf['endpoint_name'])
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {} --infra_tag_name {} " \
                 "--infra_tag_value {} --user_tag_value {}" \
            .format(project_conf['edge_role_name'], project_conf['edge_role_profile_name'],
                         project_conf['edge_policy_name'], os.environ['aws_region'], project_conf['tag_name'],
                         project_conf['service_base_name'], user_tag)
        if 'aws_permissions_boundary_arn' in os.environ:
            params = '{} --permissions_boundary_arn {}'.format(params, os.environ['aws_permissions_boundary_arn'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_role_policy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to creating roles.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE BACKEND (NOTEBOOK) ROLES]')
        user_tag = "{0}:{0}-{1}-{2}-nb-de-role".format(project_conf['service_base_name'], project_conf['project_name'],
                                                       project_conf['endpoint_name'])
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {} --infra_tag_name {} " \
                 "--infra_tag_value {} --user_tag_value {}" \
                 .format(project_conf['notebook_dataengine_role_name'],
                         project_conf['notebook_dataengine_role_profile_name'],
                         project_conf['notebook_dataengine_policy_name'], os.environ['aws_region'],
                         project_conf['tag_name'], project_conf['service_base_name'], user_tag)
        if 'aws_permissions_boundary_arn' in os.environ:
            params = '{} --permissions_boundary_arn {}'.format(params, os.environ['aws_permissions_boundary_arn'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_role_policy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to creating roles.", str(err))
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        sys.exit(1)

    try:
        if os.environ['aws_security_groups_ids'] == '':
            raise KeyError
    except KeyError:
        try:
            logging.info('[CREATE SECURITY GROUP FOR EDGE NODE]')
            edge_sg_ingress = datalab.meta_lib.format_sg([
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
                    "PrefixListIds": [],
                    "FromPort": 443,
                    "IpRanges": project_conf['allowed_ip_cidr'],
                    "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                },
                {
                    "IpProtocol": "-1",
                    "IpRanges": [{"CidrIp": project_conf['provision_instance_ip']}],
                    "UserIdGroupPairs": [],
                    "PrefixListIds": []
                }
            ])
            edge_sg_egress = datalab.meta_lib.format_sg([
                {
                    "PrefixListIds": [],
                    "FromPort": 22,
                    "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
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
                },
                {
                    "PrefixListIds": [],
                    "FromPort": -1,
                    "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                    "ToPort": -1, "IpProtocol": "icmp", "UserIdGroupPairs": []
                }
            ])
            params = "--name {} --vpc_id {} --security_group_rules '{}' --infra_tag_name {} --infra_tag_value {} \
                --egress '{}' --force {} --nb_sg_name {} --resource {}".\
                format(project_conf['edge_security_group_name'], project_conf['vpc_id'], json.dumps(edge_sg_ingress),
                       project_conf['service_base_name'], project_conf['edge_instance_name'], json.dumps(edge_sg_egress),
                       True, project_conf['notebook_instance_name'], 'edge')
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
            except Exception as err:
                traceback.print_exc()
                datalab.fab.append_result("Failed creating security group for edge node.", str(err))
                raise Exception

            logging.info('Waiting for changes to propagate')
            time.sleep(10)
        except:
            datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
            datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
            sys.exit(1)

    try:
        logging.info('[CREATE SECURITY GROUP FOR PRIVATE SUBNET]')
        rules_list = []
        sg_list = project_conf['sg_ids'].replace(" ", "").split(',')
        if os.environ['aws_security_groups_ids'] == '':
            project_group_id = datalab.meta_lib.check_security_group(project_conf['edge_security_group_name'])
            rules_list.append({"GroupId": project_group_id})
        else:
            for i in sg_list:
                rules_list.append({"GroupId": i})
        private_sg_ingress = datalab.meta_lib.format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [],
                "UserIdGroupPairs": rules_list,
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": project_conf['provision_instance_ip']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            }
        ])

        private_sg_egress = datalab.meta_lib.format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": project_conf['private_subnet_cidr']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": project_conf['provision_instance_ip']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": [],
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [],
                "UserIdGroupPairs": rules_list,
                "PrefixListIds": []
            },
            {
                "IpProtocol": "tcp",
                "IpRanges": [{"CidrIp": project_conf['all_ip_cidr']}],
                "FromPort": 443,
                "ToPort": 443,
                "UserIdGroupPairs": [],
                "PrefixListIds": [],
            }
        ])

        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                 "--infra_tag_value {} --force {}".format(project_conf['notebook_security_group_name'],
                                                          project_conf['vpc2_id'], json.dumps(private_sg_ingress),
                                                          json.dumps(private_sg_egress),
                                                          project_conf['service_base_name'],
                                                          project_conf['notebook_instance_name'], True)
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception

        logging.info('Waiting for changes to propagate')
        time.sleep(10)
    except Exception as err:
        datalab.fab.append_result("Failed creating security group for private subnet.", str(err))
        datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
        datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR MASTER NODE]')
    try:
        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                 "--infra_tag_value {} --force {}".format(project_conf['dataengine_master_security_group_name'],
                                                          project_conf['vpc2_id'], json.dumps(private_sg_ingress),
                                                          json.dumps(private_sg_egress),
                                                          project_conf['service_base_name'],
                                                          project_conf['dataengine_instances_name'], True)
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create sg.", str(err))
        datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
        datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR SLAVE NODES]')
    try:
        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                 "--infra_tag_value {} --force {}".format(project_conf['dataengine_slave_security_group_name'],
                                                          project_conf['vpc2_id'], json.dumps(private_sg_ingress),
                                                          json.dumps(private_sg_egress),
                                                          project_conf['service_base_name'],
                                                          project_conf['dataengine_instances_name'], True)
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create security group.", str(err))
        datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        datalab.actions_lib.remove_sgroups(project_conf['dataengine_instances_name'])
        datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
        datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        project_conf['shared_bucket_tags'] = 'endpoint_tag:{0};{1}:{2};{3}:{4}{5}'.format(
            project_conf['endpoint_tag'], os.environ['conf_billing_tag_key'], os.environ['conf_billing_tag_value'],
            project_conf['tag_name'], project_conf['shared_bucket_name'],
            project_conf['bucket_additional_tags']).replace(';', ',')
        params = "--bucket_name {} --bucket_tags {} --region {} --bucket_name_tag {} --bucket_versioning_enabled {}" \
            .format(project_conf['shared_bucket_name'], project_conf['shared_bucket_tags'], project_conf['region'],
                    project_conf['shared_bucket_name_tag'], project_conf['bucket_versioning_enabled'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_bucket', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
        project_conf['bucket_tags'] = 'endpoint_tag:{0};{1}:{2};project_tag:{3};{4}:{5}{6}'.format(
            project_conf['endpoint_tag'], os.environ['conf_billing_tag_key'], os.environ['conf_billing_tag_value'],
            project_conf['project_tag'], project_conf['tag_name'], project_conf['bucket_name'],
            project_conf['bucket_additional_tags']).replace(';', ',')
        params = "--bucket_name {} --bucket_tags {} --region {} --bucket_name_tag {} --bucket_versioning_enabled {}" \
            .format(project_conf['bucket_name'], project_conf['bucket_tags'], project_conf['region'],
                    project_conf['bucket_name_tag'], project_conf['bucket_versioning_enabled'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_bucket', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create buckets.", str(err))
        datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        datalab.actions_lib.remove_sgroups(project_conf['dataengine_instances_name'])
        datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
        datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    try:
        logging.info('[CREATING BUCKET POLICY FOR PROJECT INSTANCES]')
        params = '--bucket_name {} --shared_bucket_name {} --username {} --edge_role_name {} ' \
                 '--notebook_role_name {} --service_base_name {} --region {} ' \
                 '--user_predefined_s3_policies "{}" --endpoint_name {}'.format(
                  project_conf['bucket_name'], project_conf['shared_bucket_name'], project_conf['project_name'],
                  project_conf['edge_role_name'], project_conf['notebook_dataengine_role_name'],
                  project_conf['service_base_name'], project_conf['region'],
                  os.environ['aws_user_predefined_s3_policies'], project_conf['endpoint_name'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_policy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create bucket policy.", str(err))
        datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        datalab.actions_lib.remove_sgroups(project_conf['dataengine_instances_name'])
        datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
        datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
        datalab.actions_lib.remove_s3('edge', project_conf['project_name'])
        sys.exit(1)

    try:
        if os.environ['aws_security_groups_ids'] == '':
            edge_group_id = datalab.meta_lib.check_security_group(project_conf['edge_security_group_name'])
        else:
            edge_group_id = os.environ['aws_security_groups_ids']
        logging.info('[CREATE EDGE INSTANCE]')
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} " \
                 "--subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {}" \
            .format(project_conf['edge_instance_name'], project_conf['ami_id'], project_conf['instance_size'],
                    project_conf['key_name'], edge_group_id, project_conf['public_subnet_id'],
                    project_conf['edge_role_profile_name'], project_conf['tag_name'],
                    project_conf['edge_instance_name'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
            edge_instance = datalab.meta_lib.get_instance_by_name(project_conf['tag_name'],
                                                                  project_conf['edge_instance_name'])
            if os.environ['edge_is_nat']:
                try:
                    datalab.actions_lib.modify_instance_sourcedescheck(edge_instance)
                except:
                    traceback.print_exc()
                    raise Exception
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create instance.", str(err))
        datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
        datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
        datalab.actions_lib.remove_sgroups(project_conf['dataengine_instances_name'])
        datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
        datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
        datalab.actions_lib.remove_s3('edge', project_conf['project_name'])
        sys.exit(1)

    if project_conf['network_type'] == 'public':
        try:
            logging.info('[ASSOCIATING ELASTIC IP]')
            project_conf['edge_id'] = datalab.meta_lib.get_instance_by_name(project_conf['tag_name'],
                                                                            project_conf['edge_instance_name'])
            try:
                project_conf['elastic_ip'] = os.environ['edge_elastic_ip']
            except:
                project_conf['elastic_ip'] = 'None'
            params = "--elastic_ip {} --edge_id {}  --infra_tag_name {} --infra_tag_value {}".format(
                project_conf['elastic_ip'], project_conf['edge_id'], project_conf['tag_name'],
                project_conf['elastic_ip_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('edge_associate_elastic_ip', params), shell=True, check=True)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Failed to associate elastic ip.", str(err))
            try:
                project_conf['edge_public_ip'] = datalab.meta_lib.get_instance_ip_address(
                    project_conf['tag_name'], project_conf['edge_instance_name']).get('Public')
                project_conf['allocation_id'] = datalab.meta_lib.get_allocation_id_by_elastic_ip(
                    project_conf['edge_public_ip'])
            except:
                logging.error("No Elastic IPs to release!")
            datalab.actions_lib.remove_ec2(project_conf['tag_name'], project_conf['edge_instance_name'])
            datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
            datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
            datalab.actions_lib.remove_sgroups(project_conf['dataengine_instances_name'])
            datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
            datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
            datalab.actions_lib.remove_s3('edge', project_conf['project_name'])
            sys.exit(1)

    if os.environ['edge_is_nat'] == 'true':
        try:
            logging.info('[CONFIGURING ROUTE TABLE FOR NAT]')
            project_conf['nat_rt_name'] = '{0}-{1}-{2}-nat-rt'.format(project_conf['service_base_name'],
                                                                              project_conf['project_name'],
                                                                              project_conf['endpoint_name'])
            params = "--vpc_id {} --infra_tag_value {} --edge_instance_id {} --private_subnet_id {} --sbn {}".format(
                project_conf['vpc2_id'], project_conf['nat_rt_name'], edge_instance, subnet_id, project_conf['service_base_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('edge_configure_route_table', params), shell=True, check=True)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Failed to configure route table.", str(err))
            try:
                project_conf['edge_public_ip'] = datalab.meta_lib.get_instance_ip_address(
                    project_conf['tag_name'], project_conf['edge_instance_name']).get('Public')
                project_conf['allocation_id'] = datalab.meta_lib.get_allocation_id_by_elastic_ip(
                    project_conf['edge_public_ip'])
            except:
                logging.error("No Elastic IPs to release!")
            datalab.actions_lib.remove_ec2(project_conf['tag_name'], project_conf['edge_instance_name'])
            datalab.actions_lib.remove_all_iam_resources('notebook', project_conf['project_name'])
            datalab.actions_lib.remove_all_iam_resources('edge', project_conf['project_name'])
            datalab.actions_lib.remove_sgroups(project_conf['dataengine_instances_name'])
            datalab.actions_lib.remove_sgroups(project_conf['notebook_instance_name'])
            datalab.actions_lib.remove_sgroups(project_conf['edge_instance_name'])
            datalab.actions_lib.remove_s3('edge', project_conf['project_name'])
            datalab.actions_lib.remove_route_tables("Name", False,
                                                    '{}-{}-{}-nat-rt'.format(service_base_name, project_name,
                                                                             endpoint_name))
            sys.exit(1)
