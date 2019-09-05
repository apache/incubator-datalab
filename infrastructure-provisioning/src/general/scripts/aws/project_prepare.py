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
import boto3


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/project/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    create_aws_config_files()
    print('Generating infrastructure names and tags')
    project_conf = dict()
    project_conf['service_base_name'] = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
    project_conf['endpoint_name'] = '{0}-{1}-endpoint'.format(project_conf['service_base_name'],
                                                              os.environ['endpoint_name'])
    project_conf['endpoint_tag'] = os.environ['endpoint_name']
    project_conf['project_name'] = os.environ['project_name']
    project_conf['project_tag'] = os.environ['project_name']
    project_conf['key_name'] = os.environ['conf_key_name']
    project_conf['public_subnet_id'] = os.environ['aws_subnet_id']
    project_conf['vpc_id'] = os.environ['aws_vpc_id']
    project_conf['region'] = os.environ['aws_region']
    project_conf['ami_id'] = get_ami_id(os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])])
    project_conf['instance_size'] = os.environ['aws_edge_instance_size']
    project_conf['sg_ids'] = os.environ['aws_security_groups_ids']
    project_conf['edge_instance_name'] = '{}-{}-edge'.format(project_conf['service_base_name'],
                                                             os.environ['project_name'])
    project_conf['tag_name'] = '{}-Tag'.format(project_conf['service_base_name'])
    project_conf['bucket_name_tag'] = '{}-{}-bucket'.format(project_conf['service_base_name'],
                                                     os.environ['project_name'])
    project_conf['bucket_name'] = project_conf['bucket_name_tag'].lower().replace('_', '-')
    project_conf['ssn_bucket_name'] = '{}-ssn-bucket'.format(
        project_conf['service_base_name']).lower().replace('_', '-')
    project_conf['shared_bucket_name'] = '{}-shared-bucket'.format(
        project_conf['service_base_name']).lower().replace('_', '-')
    project_conf['edge_role_name'] = '{}-{}-edge-Role'.format(
        project_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    project_conf['edge_role_profile_name'] = '{}-{}-edge-Profile'.format(
        project_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    project_conf['edge_policy_name'] = '{}-{}-edge-Policy'.format(
        project_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    project_conf['edge_security_group_name'] = '{}-sg'.format(project_conf['edge_instance_name'])
    project_conf['notebook_instance_name'] = '{}-{}-nb'.format(project_conf['service_base_name'],
                                                            os.environ['project_name'])
    project_conf['dataengine_instances_name'] = '{}-{}-dataengine' \
        .format(project_conf['service_base_name'], os.environ['project_name'])
    project_conf['notebook_dataengine_role_name'] = '{}-{}-nb-de-Role' \
        .format(project_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    project_conf['notebook_dataengine_policy_name'] = '{}-{}-nb-de-Policy' \
        .format(project_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    project_conf['notebook_dataengine_role_profile_name'] = '{}-{}-nb-de-Profile' \
        .format(project_conf['service_base_name'].lower().replace('-', '_'), os.environ['project_name'])
    project_conf['notebook_security_group_name'] = '{}-{}-nb-sg'.format(project_conf['service_base_name'],
                                                                     os.environ['project_name'])
    project_conf['private_subnet_prefix'] = os.environ['aws_private_subnet_prefix']
    project_conf['private_subnet_name'] = '{0}-{1}-subnet'.format(project_conf['service_base_name'],
                                                               os.environ['project_name'])
    project_conf['dataengine_master_security_group_name'] = '{}-{}-dataengine-master-sg' \
        .format(project_conf['service_base_name'], os.environ['project_name'])
    project_conf['dataengine_slave_security_group_name'] = '{}-{}-dataengine-slave-sg' \
        .format(project_conf['service_base_name'], os.environ['project_name'])
    project_conf['allowed_ip_cidr'] = list()
    for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
        project_conf['allowed_ip_cidr'].append({"CidrIp": cidr.replace(' ','')})
    project_conf['network_type'] = os.environ['conf_network_type']
    project_conf['all_ip_cidr'] = '0.0.0.0/0'
    project_conf['zone'] = os.environ['aws_region'] + os.environ['aws_zone']
    project_conf['elastic_ip_name'] = '{0}-{1}-edge-EIP'.format(project_conf['service_base_name'],
                                                             os.environ['project_name'])
    project_conf['provision_instance_ip'] = None
    try:
        project_conf['provision_instance_ip'] = get_instance_ip_address(
            project_conf['tag_name'], '{0}-{1}-endpoint'.format(project_conf['service_base_name'],
                                                                os.environ['endpoint_name'])).get('Private') + "/32"
    except:
        project_conf['provision_instance_ip'] = get_instance_ip_address(project_conf['tag_name'], '{0}-ssn'.format(
            project_conf['service_base_name'])).get('Private') + "/32"
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
            local('echo "{0}" >> {1}{2}.pub'.format(project_conf['user_key'], os.environ['conf_key_dir'],
                                                    project_conf['project_name']))
        except:
            print("ADMINSs PUBLIC KEY DOES NOT INSTALLED")
    except KeyError:
        print("ADMINSs PUBLIC KEY DOES NOT UPLOADED")
        sys.exit(1)

    print("Will create exploratory environment with edge node as access point as following: {}".
          format(json.dumps(project_conf, sort_keys=True, indent=4, separators=(',', ': '))))
    logging.info(json.dumps(project_conf))

    if 'conf_additional_tags' in os.environ:
        os.environ['conf_additional_tags'] = os.environ['conf_additional_tags'] + \
                                             ';project_tag:{0};endpoint_tag:{1};'.format(
                                                 project_conf['project_tag'], project_conf['endpoint_tag'])
    else:
        os.environ['conf_additional_tags'] = 'project_tag:{0};endpoint_tag:{1}'.format(project_conf['project_tag'],
                                                                                       project_conf['endpoint_tag'])
    print('Additional tags will be added: {}'.format(os.environ['conf_additional_tags']))

    # attach project_tag and endpoint_tag to endpoint
    try:
        endpoint_id = get_instance_by_name(project_conf['tag_name'], project_conf['endpoint_name'])
        print("Endpoint id: " + endpoint_id)
        ec2 = boto3.client('ec2')
        ec2.create_tags(Resources=[endpoint_id], Tags=[{'Key': 'project_tag', 'Value': project_conf['project_tag']},
                                                       {'Key': 'endpoint_tag', 'Value': project_conf['endpoint_tag']}])
    except Exception as err:
        print("Failed to attach Project tag to Endpoint", str(err))
#        traceback.print_exc()
#        sys.exit(1)

    try:
        project_conf['vpc2_id'] = os.environ['aws_vpc2_id']
        project_conf['tag_name'] = '{}-secondary-Tag'.format(project_conf['service_base_name'])
    except KeyError:
        project_conf['vpc2_id'] = project_conf['vpc_id']

    try:
        logging.info('[CREATE SUBNET]')
        print('[CREATE SUBNET]')
        params = "--vpc_id '{}' --infra_tag_name {} --infra_tag_value {} --prefix {} " \
                 "--user_subnets_range '{}' --subnet_name {} --zone {}".format(
            project_conf['vpc2_id'], project_conf['tag_name'], project_conf['service_base_name'],
            project_conf['private_subnet_prefix'], os.environ['conf_user_subnets_range'],
            project_conf['private_subnet_name'],
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

    tag = {"Key": project_conf['tag_name'],
           "Value": "{0}-{1}-subnet".format(project_conf['service_base_name'], project_conf['project_name'])}
    project_conf['private_subnet_cidr'] = get_subnet_by_tag(tag)
    subnet_id = get_subnet_by_cidr(project_conf['private_subnet_cidr'])
    print('subnet id: {}'.format(subnet_id))

    print('NEW SUBNET CIDR CREATED: {}'.format(project_conf['private_subnet_cidr']))

    try:
        logging.info('[CREATE EDGE ROLES]')
        print('[CREATE EDGE ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {} --infra_tag_name {} " \
                 "--infra_tag_value {}" \
                 .format(project_conf['edge_role_name'], project_conf['edge_role_profile_name'],
                         project_conf['edge_policy_name'], os.environ['aws_region'], project_conf['tag_name'],
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

    try:
        logging.info('[CREATE BACKEND (NOTEBOOK) ROLES]')
        print('[CREATE BACKEND (NOTEBOOK) ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {} --infra_tag_name {} " \
                 "--infra_tag_value {}" \
                 .format(project_conf['notebook_dataengine_role_name'],
                         project_conf['notebook_dataengine_role_profile_name'],
                         project_conf['notebook_dataengine_policy_name'], os.environ['aws_region'],
                         project_conf['tag_name'], project_conf['service_base_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_role_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to creating roles.", str(err))
        remove_all_iam_resources('edge', os.environ['project_name'])
        sys.exit(1)

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
                "IpRanges": [{"CidrIp": project_conf['provision_instance_ip']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            }
        ])
        edge_sg_egress = format_sg([
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
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE SECURITY GROUP FOR PRIVATE SUBNET]')
        print('[CREATE SECURITY GROUP FOR PRIVATE SUBNET]')
        project_group_id = check_security_group(project_conf['edge_security_group_name'])
        sg_list = project_conf['sg_ids'].replace(" ", "").split(',')
        rules_list = []
        for i in sg_list:
            rules_list.append({"GroupId": i})
        private_sg_ingress = format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [],
                "UserIdGroupPairs": [{"GroupId": project_group_id}],
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

        private_sg_egress = format_sg([
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
                "UserIdGroupPairs": [{"GroupId": project_group_id}],
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
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception

        with hide('stderr', 'running', 'warnings'):
            print('Waiting for changes to propagate')
            time.sleep(10)
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed creating security group for private subnet.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_sgroups(project_conf['notebook_instance_name'])
        remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR MASTER NODE]')
    print("[CREATING SECURITY GROUPS FOR MASTER NODE]")
    try:
        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                 "--infra_tag_value {} --force {}".format(project_conf['dataengine_master_security_group_name'],
                                                          project_conf['vpc2_id'], json.dumps(private_sg_ingress),
                                                          json.dumps(private_sg_egress),
                                                          project_conf['service_base_name'],
                                                          project_conf['dataengine_instances_name'], True)
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create sg.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_sgroups(project_conf['notebook_instance_name'])
        remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR SLAVE NODES]')
    print("[CREATING SECURITY GROUPS FOR SLAVE NODES]")
    try:
        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                 "--infra_tag_value {} --force {}".format(project_conf['dataengine_slave_security_group_name'],
                                                          project_conf['vpc2_id'], json.dumps(private_sg_ingress),
                                                          json.dumps(private_sg_egress),
                                                          project_conf['service_base_name'],
                                                          project_conf['dataengine_instances_name'], True)
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create bucket.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_sgroups(project_conf['dataengine_instances_name'])
        remove_sgroups(project_conf['notebook_instance_name'])
        remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {} --infra_tag_name {} --infra_tag_value {} --region {} --bucket_name_tag {}" \
                 .format(project_conf['bucket_name'], project_conf['tag_name'], project_conf['bucket_name'],
                         project_conf['region'], project_conf['bucket_name_tag'])
        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create bucket.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_sgroups(project_conf['dataengine_instances_name'])
        remove_sgroups(project_conf['notebook_instance_name'])
        remove_sgroups(project_conf['edge_instance_name'])
        sys.exit(1)

    try:
        logging.info('[CREATING BUCKET POLICY FOR PROJECT INSTANCES]')
        print('[CREATING BUCKET POLICY FOR USER INSTANCES]')
        params = '--bucket_name {} --ssn_bucket_name {} --shared_bucket_name {} --username {} --edge_role_name {} ' \
                 '--notebook_role_name {} --service_base_name {} --region {} ' \
                 '--user_predefined_s3_policies "{}"'.format(project_conf['bucket_name'],
                                                             project_conf['ssn_bucket_name'],
                                                             project_conf['shared_bucket_name'],
                                                             os.environ['project_name'], project_conf['edge_role_name'],
                                                             project_conf['notebook_dataengine_role_name'],
                                                             project_conf['service_base_name'], project_conf['region'],
                                                             os.environ['aws_user_predefined_s3_policies'])
        try:
            local("~/scripts/{}.py {}".format('common_create_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create bucket policy.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_sgroups(project_conf['dataengine_instances_name'])
        remove_sgroups(project_conf['notebook_instance_name'])
        remove_sgroups(project_conf['edge_instance_name'])
        remove_s3('edge', os.environ['project_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE EDGE INSTANCE]')
        print('[CREATE EDGE INSTANCE]')
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} " \
                 "--subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {}" \
            .format(project_conf['edge_instance_name'], project_conf['ami_id'], project_conf['instance_size'],
                    project_conf['key_name'], project_group_id, project_conf['public_subnet_id'],
                    project_conf['edge_role_profile_name'], project_conf['tag_name'],
                    project_conf['edge_instance_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
            edge_instance = get_instance_by_name(project_conf['tag_name'], project_conf['edge_instance_name'])
        except:
            traceback.print_exc()
            raise Exception

    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create instance.", str(err))
        remove_all_iam_resources('notebook', os.environ['project_name'])
        remove_all_iam_resources('edge', os.environ['project_name'])
        remove_sgroups(project_conf['dataengine_instances_name'])
        remove_sgroups(project_conf['notebook_instance_name'])
        remove_sgroups(project_conf['edge_instance_name'])
        remove_s3('edge', os.environ['project_name'])
        sys.exit(1)

    if project_conf['network_type'] == 'public':
        try:
            logging.info('[ASSOCIATING ELASTIC IP]')
            print('[ASSOCIATING ELASTIC IP]')
            project_conf['edge_id'] = get_instance_by_name(project_conf['tag_name'], project_conf['edge_instance_name'])
            try:
                project_conf['elastic_ip'] = os.environ['edge_elastic_ip']
            except:
                project_conf['elastic_ip'] = 'None'
            params = "--elastic_ip {} --edge_id {}  --infra_tag_name {} --infra_tag_value {}".format(
                project_conf['elastic_ip'], project_conf['edge_id'], project_conf['tag_name'],
                project_conf['elastic_ip_name'])
            try:
                local("~/scripts/{}.py {}".format('edge_associate_elastic_ip', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to associate elastic ip.", str(err))
            try:
                project_conf['edge_public_ip'] = get_instance_ip_address(project_conf['tag_name'],
                                                                      project_conf['edge_instance_name']).get('Public')
                project_conf['allocation_id'] = get_allocation_id_by_elastic_ip(project_conf['edge_public_ip'])
            except:
                print("No Elastic IPs to release!")
            remove_ec2(project_conf['tag_name'], project_conf['edge_instance_name'])
            remove_all_iam_resources('notebook', os.environ['project_name'])
            remove_all_iam_resources('edge', os.environ['project_name'])
            remove_sgroups(project_conf['dataengine_instances_name'])
            remove_sgroups(project_conf['notebook_instance_name'])
            remove_sgroups(project_conf['edge_instance_name'])
            remove_s3('edge', os.environ['project_name'])
            sys.exit(1)