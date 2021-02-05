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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
import logging
import os
import sys
import traceback
import subprocess
from fabric import *

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    ssn_conf = dict()
    ssn_conf['instance'] = 'ssn'
    ssn_conf['pre_defined_vpc'] = False
    ssn_conf['pre_defined_subnet'] = False
    ssn_conf['pre_defined_sg'] = False
    ssn_conf['pre_defined_vpc2'] = False
    try:
        logging.info('[CREATE AWS CONFIG FILE]')
        print('[CREATE AWS CONFIG FILE]')
        if 'aws_access_key' in os.environ and 'aws_secret_access_key' in os.environ:
            datalab.actions_lib.create_aws_config_files(generate_full_config=True)
        else:
            datalab.actions_lib.create_aws_config_files()
    except Exception as err:
        logging.info('Unable to create configuration')
        datalab.fab.append_result("Unable to create configuration", err)
        traceback.print_exc()
        sys.exit(1)

    try:
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'] = datalab.fab.replace_multi_symbols(
            os.environ['conf_service_base_name'][:20], '-', True)
        ssn_conf['role_name'] = '{}-ssn-role'.format(ssn_conf['service_base_name'])
        ssn_conf['role_profile_name'] = '{}-ssn-profile'.format(ssn_conf['service_base_name'])
        ssn_conf['policy_name'] = '{}-ssn-policy'.format(ssn_conf['service_base_name'])
        ssn_conf['tag_name'] = '{}-tag'.format(ssn_conf['service_base_name'])
        ssn_conf['tag2_name'] = '{}-secondary-tag'.format(ssn_conf['service_base_name'])
        ssn_conf['user_tag'] = "{0}:{0}-ssn-role".format(ssn_conf['service_base_name'])
        ssn_conf['instance_name'] = '{}-ssn'.format(ssn_conf['service_base_name'])
        ssn_conf['region'] = os.environ['aws_region']
        ssn_conf['zone_full'] = os.environ['aws_region'] + os.environ['aws_zone']
        ssn_conf['ssn_image_name'] = os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])]
        ssn_conf['ssn_ami_id'] = datalab.meta_lib.get_ami_id(ssn_conf['ssn_image_name'])
        ssn_conf['policy_path'] = '/root/files/ssn_policy.json'
        ssn_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
        ssn_conf['vpc2_cidr'] = os.environ['conf_vpc2_cidr']
        ssn_conf['vpc_name'] = '{}-vpc'.format(ssn_conf['service_base_name'])
        ssn_conf['vpc2_name'] = '{}-vpc2'.format(ssn_conf['service_base_name'])
        ssn_conf['subnet_name'] = '{}-subnet'.format(ssn_conf['service_base_name'])
        ssn_conf['allowed_ip_cidr'] = list()
        for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
            ssn_conf['allowed_ip_cidr'].append({"CidrIp": cidr.replace(' ','')})
        ssn_conf['sg_name'] = '{}-ssn-sg'.format(ssn_conf['service_base_name'])
        ssn_conf['network_type'] = os.environ['conf_network_type']
        ssn_conf['all_ip_cidr'] = '0.0.0.0/0'
        ssn_conf['elastic_ip_name'] = '{0}-ssn-static-ip'.format(ssn_conf['service_base_name'])
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    if datalab.meta_lib.get_instance_by_name(ssn_conf['tag_name'], ssn_conf['instance_name']):
        print("Service base name should be unique and less or equal 20 symbols. Please try again.")
        sys.exit(1)

    try:
        if not os.environ['aws_vpc_id']:
            raise KeyError
    except KeyError:
        try:
            ssn_conf['pre_defined_vpc'] = True
            logging.info('[CREATE VPC AND ROUTE TABLE]')
            print('[CREATE VPC AND ROUTE TABLE]')
            params = "--vpc {} --region {} --infra_tag_name {} --infra_tag_value {} --vpc_name {}".format(
                ssn_conf['vpc_cidr'], ssn_conf['region'], ssn_conf['tag_name'], ssn_conf['service_base_name'],
                ssn_conf['vpc_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('ssn_create_vpc', params), shell=True)
            except:
                traceback.print_exc()
                raise Exception
            os.environ['aws_vpc_id'] = datalab.meta_lib.get_vpc_by_tag(ssn_conf['tag_name'],
                                                                       ssn_conf['service_base_name'])
        except Exception as err:
            datalab.fab.append_result("Failed to create VPC", str(err))
            sys.exit(1)

    ssn_conf['allowed_vpc_cidr_ip_ranges'] = list()
    for cidr in datalab.meta_lib.get_vpc_cidr_by_id(os.environ['aws_vpc_id']):
        ssn_conf['allowed_vpc_cidr_ip_ranges'].append({"CidrIp": cidr})

    try:
        if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_vpc2_id']:
            raise KeyError
    except KeyError:
        try:
            ssn_conf['pre_defined_vpc2'] = True
            logging.info('[CREATE SECONDARY VPC AND ROUTE TABLE]')
            print('[CREATE SECONDARY VPC AND ROUTE TABLE]')
            params = "--vpc {} --region {} --infra_tag_name {} --infra_tag_value {} --secondary " \
                     "--vpc_name {}".format(ssn_conf['vpc2_cidr'], ssn_conf['region'], ssn_conf['tag2_name'],
                                            ssn_conf['service_base_name'], ssn_conf['vpc2_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('ssn_create_vpc', params), shell=True)
            except:
                traceback.print_exc()
                raise Exception
            os.environ['aws_vpc2_id'] = datalab.meta_lib.get_vpc_by_tag(ssn_conf['tag2_name'],
                                                                        ssn_conf['service_base_name'])
        except Exception as err:
            datalab.fab.append_result("Failed to create secondary VPC.", str(err))
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                             ssn_conf['service_base_name'])
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            sys.exit(1)

    try:
        if os.environ['aws_subnet_id'] == '':
            raise KeyError
    except KeyError:
        try:
            ssn_conf['pre_defined_subnet'] = True
            logging.info('[CREATE SUBNET]')
            print('[CREATE SUBNET]')
            params = "--vpc_id {0} --username {1} --infra_tag_name {2} --infra_tag_value {3} --prefix {4} " \
                     "--ssn {5} --zone {6} --subnet_name {7}".format(
                      os.environ['aws_vpc_id'], 'ssn', ssn_conf['tag_name'],ssn_conf['service_base_name'], '20',
                      True, ssn_conf['zone_full'], ssn_conf['subnet_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_subnet', params), shell=True)
            except:
                traceback.print_exc()
                raise Exception
            with open('/tmp/ssn_subnet_id', 'r') as f:
                os.environ['aws_subnet_id'] = f.read()
            datalab.actions_lib.enable_auto_assign_ip(os.environ['aws_subnet_id'])
        except Exception as err:
            datalab.fab.append_result("Failed to create Subnet.", str(err))
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                             ssn_conf['service_base_name'])
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                try:
                    datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
                except:
                    print("Subnet hasn't been created.")
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            if ssn_conf['pre_defined_vpc2']:
                try:
                    datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)

    try:
        if os.environ['conf_duo_vpc_enable'] == 'true' and os.environ['aws_vpc_id'] and os.environ['aws_vpc2_id']:
            raise KeyError
    except KeyError:
        try:
            logging.info('[CREATE PEERING CONNECTION]')
            print('[CREATE PEERING CONNECTION]')
            os.environ['aws_peering_id'] = datalab.actions_lib.create_peering_connection(
                os.environ['aws_vpc_id'], os.environ['aws_vpc2_id'], ssn_conf['service_base_name'])
            print('PEERING CONNECTION ID:' + os.environ['aws_peering_id'])
            datalab.actions_lib.create_route_by_id(os.environ['aws_subnet_id'], os.environ['aws_vpc_id'],
                                                   os.environ['aws_peering_id'],
                                                   datalab.meta_lib.get_cidr_by_vpc(os.environ['aws_vpc2_id']))
        except Exception as err:
            datalab.fab.append_result("Failed to create peering connection.", str(err))
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                try:
                    datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
                except:
                    print("Subnet hasn't been created.")
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            if ssn_conf['pre_defined_vpc2']:
                datalab.actions_lib.remove_peering('*')
                try:
                    datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)

    try:
        if os.environ['aws_security_groups_ids'] == '':
            raise KeyError
    except KeyError:
        try:
            ssn_conf['pre_defined_sg'] = True
            logging.info('[CREATE SG FOR SSN]')
            print('[CREATE SG FOR SSN]')
            ssn_conf['ingress_sg_rules_template'] = datalab.meta_lib.format_sg([
                {
                    "PrefixListIds": [],
                    "FromPort": 80,
                    "IpRanges": ssn_conf['allowed_ip_cidr'],
                    "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                },
                {
                    "PrefixListIds": [],
                    "FromPort": 22,
                    "IpRanges": ssn_conf['allowed_ip_cidr'],
                    "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
                },
                {
                    "PrefixListIds": [],
                    "FromPort": 443,
                    "IpRanges": ssn_conf['allowed_ip_cidr'],
                    "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                },
                {
                    "PrefixListIds": [],
                    "FromPort": -1,
                    "IpRanges": ssn_conf['allowed_ip_cidr'],
                    "ToPort": -1, "IpProtocol": "icmp", "UserIdGroupPairs": []
                },
                {
                    "PrefixListIds": [],
                    "FromPort": 80,
                    "IpRanges": ssn_conf['allowed_vpc_cidr_ip_ranges'],
                    "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                },
                {
                    "PrefixListIds": [],
                    "FromPort": 443,
                    "IpRanges": ssn_conf['allowed_vpc_cidr_ip_ranges'],
                    "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                }
            ])
            egress_sg_rules_template = datalab.meta_lib.format_sg([
                {"IpProtocol": "-1", "IpRanges": [{"CidrIp": ssn_conf['all_ip_cidr']}], "UserIdGroupPairs": [],
                 "PrefixListIds": []}
            ])
            params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                     "--infra_tag_value {} --force {} --ssn {}". \
                format(ssn_conf['sg_name'], os.environ['aws_vpc_id'],
                       json.dumps(ssn_conf['ingress_sg_rules_template']), json.dumps(egress_sg_rules_template),
                       ssn_conf['service_base_name'], ssn_conf['tag_name'], False, True)
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_security_group', params), shell=True)
            except:
                traceback.print_exc()
                raise Exception
            with open('/tmp/ssn_sg_id', 'r') as f:
                os.environ['aws_security_groups_ids'] = f.read()
        except Exception as err:
            datalab.gab_lib.append_result("Failed creating security group for SSN.", str(err))
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                             ssn_conf['service_base_name'])
                datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            if ssn_conf['pre_defined_vpc2']:
                datalab.actions_lib.remove_peering('*')
                try:
                    datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)

    try:
        logging.info('[CREATE ROLES]')
        print('[CREATE ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --policy_file_name {} --region {} " \
                 "--infra_tag_name {} --infra_tag_value {} --user_tag_value {}".\
            format(ssn_conf['role_name'], ssn_conf['role_profile_name'], ssn_conf['policy_name'],
                   ssn_conf['policy_path'], os.environ['aws_region'], ssn_conf['tag_name'],
                   ssn_conf['service_base_name'], ssn_conf['user_tag'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_role_policy', params), shell=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Unable to create roles.", str(err))
        if ssn_conf['pre_defined_sg']:
            datalab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
        if ssn_conf['pre_defined_subnet']:
            datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                         ssn_conf['service_base_name'])
            datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
        if ssn_conf['pre_defined_vpc']:
            datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
            datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
        if ssn_conf['pre_defined_vpc2']:
            datalab.actions_lib.remove_peering('*')
            try:
                datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
            datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[CREATE ENDPOINT AND ROUTE-TABLE]')
        print('[CREATE ENDPOINT AND ROUTE-TABLE]')
        params = "--vpc_id {} --region {} --infra_tag_name {} --infra_tag_value {}".format(
            os.environ['aws_vpc_id'], os.environ['aws_region'], ssn_conf['tag_name'], ssn_conf['service_base_name'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('ssn_create_endpoint', params), shell=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Unable to create an endpoint.", str(err))
        datalab.actions_lib.remove_all_iam_resources(ssn_conf['instance'])
        if ssn_conf['pre_defined_sg']:
            datalab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
        if ssn_conf['pre_defined_subnet']:
            datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                         ssn_conf['service_base_name'])
            datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
        if ssn_conf['pre_defined_vpc']:
            datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
            datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
        if ssn_conf['pre_defined_vpc2']:
            datalab.actions_lib.remove_peering('*')
            try:
                datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
            datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    if os.environ['conf_duo_vpc_enable'] == 'true':
        try:
            logging.info('[CREATE ENDPOINT AND ROUTE-TABLE FOR NOTEBOOK VPC]')
            print('[CREATE ENDPOINT AND ROUTE-TABLE FOR NOTEBOOK VPC]')
            params = "--vpc_id {} --region {} --infra_tag_name {} --infra_tag_value {}".format(
                os.environ['aws_vpc2_id'], os.environ['aws_region'], ssn_conf['tag2_name'],
                ssn_conf['service_base_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('ssn_create_endpoint', params), shell=True)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Unable to create secondary endpoint.", str(err))
            datalab.actions_lib.remove_all_iam_resources(ssn_conf['instance'])
            if ssn_conf['pre_defined_sg']:
                datalab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
            if ssn_conf['pre_defined_subnet']:
                datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                             ssn_conf['service_base_name'])
                datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            if ssn_conf['pre_defined_vpc2']:
                datalab.actions_lib.remove_peering('*')
                try:
                    datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)

    try:
        logging.info('[CREATE SSN INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--node_name {0} --ami_id {1} --instance_type {2} --key_name {3} --security_group_ids {4} " \
                 "--subnet_id {5} --iam_profile {6} --infra_tag_name {7} --infra_tag_value {8} --instance_class {9} " \
                 "--primary_disk_size {10}".\
            format(ssn_conf['instance_name'], ssn_conf['ssn_ami_id'], os.environ['aws_ssn_instance_size'],
                   os.environ['conf_key_name'], os.environ['aws_security_groups_ids'], os.environ['aws_subnet_id'],
                   ssn_conf['role_profile_name'], ssn_conf['tag_name'], ssn_conf['instance_name'], 'ssn', '20')

        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Unable to create ssn instance.", str(err))
        datalab.actions_lib.remove_all_iam_resources(ssn_conf['instance'])
        datalab.actions_lib.remove_s3(ssn_conf['instance'])
        if ssn_conf['pre_defined_sg']:
            datalab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
        if ssn_conf['pre_defined_subnet']:
            datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                         ssn_conf['service_base_name'])
            datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
        if ssn_conf['pre_defined_vpc']:
            datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc_id'])
            datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
            datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
        if ssn_conf['pre_defined_vpc2']:
            datalab.actions_lib.remove_peering('*')
            try:
                datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
            datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    if ssn_conf['network_type'] == 'public':
        try:
            logging.info('[ASSOCIATING ELASTIC IP]')
            print('[ASSOCIATING ELASTIC IP]')
            ssn_conf['ssn_id'] = datalab.meta_lib.get_instance_by_name(ssn_conf['tag_name'], ssn_conf['instance_name'])
            try:
                ssn_conf['elastic_ip'] = os.environ['ssn_elastic_ip']
            except:
                ssn_conf['elastic_ip'] = 'None'
            params = "--elastic_ip {} --ssn_id {} --infra_tag_name {} --infra_tag_value {}".format(
                ssn_conf['elastic_ip'], ssn_conf['ssn_id'], ssn_conf['tag_name'], ssn_conf['elastic_ip_name'])
            try:
                subprocess.run("~/scripts/{}.py {}".format('ssn_associate_elastic_ip', params), shell=True)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Failed to associate elastic ip.", str(err))
            datalab.actions_lib.remove_ec2(ssn_conf['tag_name'], ssn_conf['instance_name'])
            datalab.actions_lib.remove_all_iam_resources(ssn_conf['instance'])
            datalab.actions_lib.remove_s3(ssn_conf['instance'])
            if ssn_conf['pre_defined_sg']:
                datalab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
            if ssn_conf['pre_defined_subnet']:
                datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                             ssn_conf['service_base_name'])
                datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc_id'])
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            if ssn_conf['pre_defined_vpc2']:
                datalab.actions_lib.remove_peering('*')
                try:
                    datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)

    if ssn_conf['network_type'] == 'private':
        ssn_conf['instance_ip'] = datalab.meta_lib.get_instance_ip_address(ssn_conf['tag_name'],
                                                                           ssn_conf['instance_name']).get('Private')
    else:
        ssn_conf['instance_ip'] = datalab.meta_lib.get_instance_ip_address(ssn_conf['tag_name'],
                                                                           ssn_conf['instance_name']).get('Public')

    if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and 'ssn_subdomain' in os.environ:
        try:
            logging.info('[CREATING ROUTE53 RECORD]')
            print('[CREATING ROUTE53 RECORD]')
            try:
                datalab.actions_lib.create_route_53_record(os.environ['ssn_hosted_zone_id'],
                                                           os.environ['ssn_hosted_zone_name'],
                                                           os.environ['ssn_subdomain'], ssn_conf['instance_ip'])
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Failed to create route53 record.", str(err))
            datalab.actions_lib.remove_route_53_record(os.environ['ssn_hosted_zone_id'],
                                                       os.environ['ssn_hosted_zone_name'],
                                                       os.environ['ssn_subdomain'])
            datalab.actions_lib.remove_ec2(ssn_conf['tag_name'], ssn_conf['instance_name'])
            datalab.actions_lib.remove_all_iam_resources(ssn_conf['instance'])
            datalab.actions_lib.remove_s3(ssn_conf['instance'])
            if ssn_conf['pre_defined_sg']:
                datalab.actions_lib.remove_sgroups(ssn_conf['tag_name'])
            if ssn_conf['pre_defined_subnet']:
                datalab.actions_lib.remove_internet_gateways(os.environ['aws_vpc_id'], ssn_conf['tag_name'],
                                                             ssn_conf['service_base_name'])
                datalab.actions_lib.remove_subnets(ssn_conf['subnet_name'])
            if ssn_conf['pre_defined_vpc']:
                datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc_id'])
                datalab.actions_lib.remove_route_tables(ssn_conf['tag_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc_id'])
            if ssn_conf['pre_defined_vpc2']:
                datalab.actions_lib.remove_peering('*')
                try:
                    datalab.actions_lib.remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                datalab.actions_lib.remove_route_tables(ssn_conf['tag2_name'], True)
                datalab.actions_lib.remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)
