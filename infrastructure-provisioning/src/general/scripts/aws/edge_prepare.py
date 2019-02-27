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
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    create_aws_config_files()
    print('Generating infrastructure names and tags')
    edge_conf = dict()
    edge_conf['service_base_name'] = os.environ['conf_service_base_name']
    edge_conf['key_name'] = os.environ['conf_key_name']
    edge_conf['user_keyname'] = os.environ['edge_user_name']
    edge_conf['public_subnet_id'] = os.environ['aws_subnet_id']
    edge_conf['vpc_id'] = os.environ['aws_vpc_id']
    edge_conf['vpc2_id'] = os.environ['aws_notebook_vpc_id']
    edge_conf['region'] = os.environ['aws_region']
    edge_conf['ami_id'] = get_ami_id(os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])])
    edge_conf['instance_size'] = os.environ['aws_edge_instance_size']
    edge_conf['sg_ids'] = os.environ['aws_security_groups_ids']
    edge_conf['instance_name'] = '{}-{}-edge'.format(edge_conf['service_base_name'], os.environ['edge_user_name'])
    edge_conf['tag_name'] = '{}-Tag'.format(edge_conf['service_base_name'])
    edge_conf['bucket_name'] = '{}-{}-bucket'.format(edge_conf['service_base_name'],
                                                     os.environ['edge_user_name']).lower().replace('_', '-')
    edge_conf['ssn_bucket_name'] = '{}-ssn-bucket'.format(edge_conf['service_base_name']).lower().replace('_', '-')
    edge_conf['shared_bucket_name'] = '{}-shared-bucket'.format(edge_conf['service_base_name']).lower().replace('_',
                                                                                                                '-')
    edge_conf['role_name'] = '{}-{}-edge-Role'.format(edge_conf['service_base_name'].lower().replace('-', '_'),
                                                      os.environ['edge_user_name'])
    edge_conf['role_profile_name'] = '{}-{}-edge-Profile'.format(edge_conf['service_base_name'].lower().replace('-',
                                                                                                                '_'),
                                                                 os.environ['edge_user_name'])
    edge_conf['policy_name'] = '{}-{}-edge-Policy'.format(edge_conf['service_base_name'].lower().replace('-', '_'),
                                                          os.environ['edge_user_name'])
    edge_conf['edge_security_group_name'] = '{}-SG'.format(edge_conf['instance_name'])
    edge_conf['notebook_instance_name'] = '{}-{}-nb'.format(edge_conf['service_base_name'],
                                                            os.environ['edge_user_name'])
    edge_conf['dataengine_instances_name'] = '{}-{}-dataengine' \
        .format(edge_conf['service_base_name'], os.environ['edge_user_name'])
    edge_conf['notebook_dataengine_role_name'] = '{}-{}-nb-de-Role' \
        .format(edge_conf['service_base_name'].lower().replace('-', '_'), os.environ['edge_user_name'])
    edge_conf['notebook_dataengine_policy_name'] = '{}-{}-nb-de-Policy' \
        .format(edge_conf['service_base_name'].lower().replace('-', '_'), os.environ['edge_user_name'])
    edge_conf['notebook_dataengine_role_profile_name'] = '{}-{}-nb-de-Profile' \
        .format(edge_conf['service_base_name'].lower().replace('-', '_'), os.environ['edge_user_name'])
    edge_conf['notebook_security_group_name'] = '{}-{}-nb-SG'.format(edge_conf['service_base_name'],
                                                                     os.environ['edge_user_name'])
    edge_conf['private_subnet_prefix'] = os.environ['aws_private_subnet_prefix']
    edge_conf['dataengine_master_security_group_name'] = '{}-{}-dataengine-master-sg' \
        .format(edge_conf['service_base_name'], os.environ['edge_user_name'])
    edge_conf['dataengine_slave_security_group_name'] = '{}-{}-dataengine-slave-sg' \
        .format(edge_conf['service_base_name'], os.environ['edge_user_name'])
    edge_conf['allowed_ip_cidr'] = list()
    for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
        edge_conf['allowed_ip_cidr'].append({"CidrIp": cidr.replace(' ','')})
    edge_conf['network_type'] = os.environ['conf_network_type']
    edge_conf['all_ip_cidr'] = '0.0.0.0/0'
    if 'aws_user_predefined_s3_policies' not in os.environ:
        os.environ['aws_user_predefined_s3_policies'] = 'None'

    try:
        if os.environ['conf_user_subnets_range'] == '':
            raise KeyError
    except KeyError:
        os.environ['conf_user_subnets_range'] = ''

    # FUSE in case of absence of user's key
    fname = "{}{}.pub".format(os.environ['conf_key_dir'], edge_conf['user_keyname'])
    if not os.path.isfile(fname):
        print("USERs PUBLIC KEY DOES NOT EXIST in {}".format(fname))
        sys.exit(1)

    print("Will create exploratory environment with edge node as access point as following: {}".
          format(json.dumps(edge_conf, sort_keys=True, indent=4, separators=(',', ': '))))
    logging.info(json.dumps(edge_conf))

    try:
        logging.info('[CREATE SUBNET]')
        print('[CREATE SUBNET]')
        params = "--vpc_id '{}' --infra_tag_name {} --infra_tag_value {} --username {} --prefix {} --user_subnets_range '{}'" \
                 .format(edge_conf['vpc2_id'], edge_conf['tag_name'], edge_conf['service_base_name'],
                         os.environ['edge_user_name'], edge_conf['private_subnet_prefix'],
                         os.environ['conf_user_subnets_range'])
        try:
            local("~/scripts/{}.py {}".format('common_create_subnet', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    tag = {"Key": edge_conf['tag_name'], "Value": "{}-{}-subnet".format(edge_conf['service_base_name'],
                                                                        os.environ['edge_user_name'])}
    edge_conf['private_subnet_cidr'] = get_subnet_by_tag(tag)
    print('NEW SUBNET CIDR CREATED: {}'.format(edge_conf['private_subnet_cidr']))

    try:
        logging.info('[CREATE EDGE ROLES]')
        print('[CREATE EDGE ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {}" \
                 .format(edge_conf['role_name'], edge_conf['role_profile_name'],
                         edge_conf['policy_name'], os.environ['aws_region'])
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
        params = "--role_name {} --role_profile_name {} --policy_name {} --region {}" \
                 .format(edge_conf['notebook_dataengine_role_name'], edge_conf['notebook_dataengine_role_profile_name'],
                         edge_conf['notebook_dataengine_policy_name'], os.environ['aws_region'])
        try:
            local("~/scripts/{}.py {}".format('common_create_role_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to creating roles.", str(err))
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE SECURITY GROUP FOR EDGE NODE]')
        print('[CREATE SECURITY GROUPS FOR EDGE]')
        edge_sg_ingress = format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "UserIdGroupPairs": [], "PrefixListIds": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 22,
                "IpRanges": edge_conf['allowed_ip_cidr'],
                "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 3128,
                "IpRanges": edge_conf['allowed_ip_cidr'],
                "ToPort": 3128, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 80,
                "IpRanges": edge_conf['allowed_ip_cidr'],
                "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": get_instance_ip_address(edge_conf['tag_name'], '{}-ssn'.format(
                    edge_conf['service_base_name'])).get('Private') + "/32"}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            }
        ])
        edge_sg_egress = format_sg([
            {
                "PrefixListIds": [],
                "FromPort": 22,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8888,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8888, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8080,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8080, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8787,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8787, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 6006,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 6006, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 20888,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 20888, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8042,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8042, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8088,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8088, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8081,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8081, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 4040,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 4045, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 18080,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 18080, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 50070,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 50070, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 53,
                "IpRanges": [{"CidrIp": edge_conf['all_ip_cidr']}],
                "ToPort": 53, "IpProtocol": "udp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 80,
                "IpRanges": [{"CidrIp": edge_conf['all_ip_cidr']}],
                "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 443,
                "IpRanges": [{"CidrIp": edge_conf['all_ip_cidr']}],
                "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 8085,
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "ToPort": 8085, "IpProtocol": "tcp", "UserIdGroupPairs": []
            },
            {
                "PrefixListIds": [],
                "FromPort": 389,
                "IpRanges": [{"CidrIp": edge_conf['all_ip_cidr']}],
                "ToPort": 389, "IpProtocol": "tcp", "UserIdGroupPairs": []
            }
        ])
        params = "--name {} --vpc_id {} --security_group_rules '{}' --infra_tag_name {} --infra_tag_value {} \
            --egress '{}' --force {} --nb_sg_name {} --resource {}".\
            format(edge_conf['edge_security_group_name'], edge_conf['vpc_id'], json.dumps(edge_sg_ingress),
                   edge_conf['service_base_name'], edge_conf['instance_name'], json.dumps(edge_sg_egress),
                   True, edge_conf['notebook_instance_name'], 'edge')
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
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE SECURITY GROUP FOR PRIVATE SUBNET]')
        print('[CREATE SECURITY GROUP FOR PRIVATE SUBNET]')
        edge_group_id = check_security_group(edge_conf['edge_security_group_name'])
        sg_list = edge_conf['sg_ids'].replace(" ", "").split(',')
        rules_list = []
        for i in sg_list:
            rules_list.append({"GroupId": i})
        private_sg_ingress = format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [],
                "UserIdGroupPairs": [{"GroupId": edge_group_id}],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": get_instance_ip_address(edge_conf['tag_name'], '{}-ssn'.format(
                    edge_conf['service_base_name'])).get('Private') + "/32"}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            }
        ])

        private_sg_egress = format_sg([
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": edge_conf['private_subnet_cidr']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": get_instance_ip_address(edge_conf['tag_name'], '{}-ssn'.format(
                    edge_conf['service_base_name'])).get('Private') + "/32"}],
                "UserIdGroupPairs": [],
                "PrefixListIds": [],
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [],
                "UserIdGroupPairs": [{"GroupId": edge_group_id}],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "tcp",
                "IpRanges": [{"CidrIp": edge_conf['all_ip_cidr']}],
                "FromPort": 443,
                "ToPort": 443,
                "UserIdGroupPairs": [],
                "PrefixListIds": [],
            }
        ])

        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} --infra_tag_value {} --force {}".\
            format(edge_conf['notebook_security_group_name'], edge_conf['vpc2_id'], json.dumps(private_sg_ingress),
                   json.dumps(private_sg_egress), edge_conf['service_base_name'], edge_conf['notebook_instance_name'], True)
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
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR MASTER NODE]')
    print("[CREATING SECURITY GROUPS FOR MASTER NODE]")
    try:
        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} --infra_tag_value {} --force {}". \
            format(edge_conf['dataengine_master_security_group_name'], edge_conf['vpc2_id'],
                   json.dumps(private_sg_ingress), json.dumps(private_sg_egress), edge_conf['service_base_name'],
                   edge_conf['dataengine_instances_name'], True)
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create sg.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        sys.exit(1)

    logging.info('[CREATING SECURITY GROUPS FOR SLAVE NODES]')
    print("[CREATING SECURITY GROUPS FOR SLAVE NODES]")
    try:
        params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} --infra_tag_value {} --force {}". \
            format(edge_conf['dataengine_slave_security_group_name'], edge_conf['vpc2_id'],
                   json.dumps(private_sg_ingress), json.dumps(private_sg_egress), edge_conf['service_base_name'],
                   edge_conf['dataengine_instances_name'], True)
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create bucket.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {} --infra_tag_name {} --infra_tag_value {} --region {}" \
                 .format(edge_conf['bucket_name'], edge_conf['tag_name'], edge_conf['bucket_name'],
                  edge_conf['region'])
        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create bucket.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        sys.exit(1)

    try:
        logging.info('[CREATING BUCKET POLICY FOR USER INSTANCES]')
        print('[CREATING BUCKET POLICY FOR USER INSTANCES]')
        params = '--bucket_name {} --ssn_bucket_name {} --shared_bucket_name {} --username {} --edge_role_name {} --notebook_role_name {} --service_base_name {} --region {} --user_predefined_s3_policies "{}"'.format(
            edge_conf['bucket_name'], edge_conf['ssn_bucket_name'], edge_conf['shared_bucket_name'],
            os.environ['edge_user_name'], edge_conf['role_name'], edge_conf['notebook_dataengine_role_name'],
            edge_conf['service_base_name'], edge_conf['region'], os.environ['aws_user_predefined_s3_policies'])
        try:
            local("~/scripts/{}.py {}".format('common_create_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create bucket policy.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['edge_user_name'])
        sys.exit(1)

    try:
        logging.info('[CREATE EDGE INSTANCE]')
        print('[CREATE EDGE INSTANCE]')
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} " \
                 "--subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {}" \
            .format(edge_conf['instance_name'], edge_conf['ami_id'], edge_conf['instance_size'], edge_conf['key_name'],
                    edge_group_id, edge_conf['public_subnet_id'], edge_conf['role_profile_name'],
                    edge_conf['tag_name'], edge_conf['instance_name'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception

    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Failed to create instance.", str(err))
        remove_all_iam_resources('notebook', os.environ['edge_user_name'])
        remove_all_iam_resources('edge', os.environ['edge_user_name'])
        remove_sgroups(edge_conf['dataengine_instances_name'])
        remove_sgroups(edge_conf['notebook_instance_name'])
        remove_sgroups(edge_conf['instance_name'])
        remove_s3('edge', os.environ['edge_user_name'])
        sys.exit(1)

    if edge_conf['network_type'] == 'public':
        try:
            logging.info('[ASSOCIATING ELASTIC IP]')
            print('[ASSOCIATING ELASTIC IP]')
            edge_conf['edge_id'] = get_instance_by_name(edge_conf['tag_name'], edge_conf['instance_name'])
            try:
                edge_conf['elastic_ip'] = os.environ['edge_elastic_ip']
            except:
                edge_conf['elastic_ip'] = 'None'
            params = "--elastic_ip {} --edge_id {}".format(edge_conf['elastic_ip'], edge_conf['edge_id'])
            try:
                local("~/scripts/{}.py {}".format('edge_associate_elastic_ip', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to associate elastic ip.", str(err))
            try:
                edge_conf['edge_public_ip'] = get_instance_ip_address(edge_conf['tag_name'], edge_conf['instance_name']).get('Public')
                edge_conf['allocation_id'] = get_allocation_id_by_elastic_ip(edge_conf['edge_public_ip'])
            except:
                print("No Elastic IPs to release!")
            remove_ec2(edge_conf['tag_name'], edge_conf['instance_name'])
            remove_all_iam_resources('notebook', os.environ['edge_user_name'])
            remove_all_iam_resources('edge', os.environ['edge_user_name'])
            remove_sgroups(edge_conf['dataengine_instances_name'])
            remove_sgroups(edge_conf['notebook_instance_name'])
            remove_sgroups(edge_conf['instance_name'])
            remove_s3('edge', os.environ['edge_user_name'])
            sys.exit(1)
