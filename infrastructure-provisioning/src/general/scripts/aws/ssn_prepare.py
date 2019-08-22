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

from dlab.fab import *
from dlab.actions_lib import *
import sys, os
from fabric.api import *
from dlab.ssn_lib import *


if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] +  "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'
    pre_defined_vpc = False
    pre_defined_subnet = False
    pre_defined_sg = False
    pre_defined_vpc2 = False
    try:
        logging.info('[CREATE AWS CONFIG FILE]')
        print('[CREATE AWS CONFIG FILE]')
        if 'aws_access_key' in os.environ and 'aws_secret_access_key' in os.environ:
            create_aws_config_files(generate_full_config=True)
        else:
            create_aws_config_files()
    except Exception as err:
        print('Error: {0}'.format(err))
        logging.info('Unable to create configuration')
        append_result("Unable to create configuration")
        traceback.print_exc()
        sys.exit(1)

    try:
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')
        service_base_name = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
        role_name = service_base_name.lower().replace('-', '_') + '-ssn-Role'
        role_profile_name = service_base_name.lower().replace('-', '_') + '-ssn-Profile'
        policy_name = service_base_name.lower().replace('-', '_') + '-ssn-Policy'
        ssn_bucket_name_tag = service_base_name + '-ssn-bucket'
        shared_bucket_name_tag = service_base_name + '-shared-bucket'
        ssn_bucket_name = ssn_bucket_name_tag.lower().replace('_', '-')
        shared_bucket_name = shared_bucket_name_tag.lower().replace('_', '-')
        tag_name = service_base_name + '-Tag'
        tag2_name = service_base_name + '-secondary-Tag'
        instance_name = service_base_name + '-ssn'
        region = os.environ['aws_region']
        zone_full = os.environ['aws_region'] + os.environ['aws_zone']
        ssn_image_name = os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])]
        ssn_ami_id = get_ami_id(ssn_image_name)
        policy_path = '/root/files/ssn_policy.json'
        vpc_cidr = os.environ['conf_vpc_cidr']
        vpc2_cidr = os.environ['conf_vpc2_cidr']
        vpc_name = '{}-VPC'.format(service_base_name)
        vpc2_name = '{}-secondary-VPC'.format(service_base_name)
        subnet_name = '{}-subnet'.format(service_base_name)
        allowed_ip_cidr = list()
        for cidr in os.environ['conf_allowed_ip_cidr'].split(','):
            allowed_ip_cidr.append({"CidrIp": cidr.replace(' ','')})
        sg_name = instance_name + '-sg'
        network_type = os.environ['conf_network_type']
        all_ip_cidr = '0.0.0.0/0'
        elastic_ip_name = '{0}-ssn-EIP'.format(service_base_name)

        if get_instance_by_name(tag_name, instance_name):
            print("Service base name should be unique and less or equal 12 symbols. Please try again.")
            sys.exit(1)

        try:
            if not os.environ['aws_vpc_id']:
                raise KeyError
        except KeyError:
            try:
                pre_defined_vpc = True
                logging.info('[CREATE VPC AND ROUTE TABLE]')
                print('[CREATE VPC AND ROUTE TABLE]')
                params = "--vpc {} --region {} --infra_tag_name {} --infra_tag_value {} --vpc_name {}".format(
                    vpc_cidr, region, tag_name, service_base_name, vpc_name)
                try:
                    local("~/scripts/{}.py {}".format('ssn_create_vpc', params))
                except:
                    traceback.print_exc()
                    raise Exception
                os.environ['aws_vpc_id'] = get_vpc_by_tag(tag_name, service_base_name)
            except Exception as err:
                print('Error: {0}'.format(err))
                append_result("Failed to create VPC. Exception:" + str(err))
                sys.exit(1)

        allowed_vpc_cidr_ip_ranges = list()
        for cidr in get_vpc_cidr_by_id(os.environ['aws_vpc_id']):
            allowed_vpc_cidr_ip_ranges.append({"CidrIp": cidr})

        try:
            if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_vpc2_id']:
                raise KeyError
        except KeyError:
            try:
                pre_defined_vpc2 = True
                logging.info('[CREATE SECONDARY VPC AND ROUTE TABLE]')
                print('[CREATE SECONDARY VPC AND ROUTE TABLE]')
                params = "--vpc {} --region {} --infra_tag_name {} --infra_tag_value {} --secondary " \
                         "--vpc_name {}".format(vpc2_cidr, region, tag2_name, service_base_name, vpc2_name)
                try:
                    local("~/scripts/{}.py {}".format('ssn_create_vpc', params))
                except:
                    traceback.print_exc()
                    raise Exception
                os.environ['aws_vpc2_id'] = get_vpc_by_tag(tag2_name, service_base_name)
            except Exception as err:
                print('Error: {0}'.format(err))
                append_result("Failed to create secondary VPC. Exception:" + str(err))
                if pre_defined_vpc:
                    remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                    remove_route_tables(tag_name, True)
                    remove_vpc(os.environ['aws_vpc_id'])
                sys.exit(1)

        try:
            if os.environ['aws_subnet_id'] == '':
                raise KeyError
        except KeyError:
            try:
                pre_defined_subnet = True
                logging.info('[CREATE SUBNET]')
                print('[CREATE SUBNET]')
                params = "--vpc_id {0} --username {1} --infra_tag_name {2} --infra_tag_value {3} --prefix {4} " \
                         "--ssn {5} --zone {6} --subnet_name {7}".format(os.environ['aws_vpc_id'], 'ssn', tag_name,
                                                             service_base_name, '20', True, zone_full, subnet_name)
                try:
                    local("~/scripts/{}.py {}".format('common_create_subnet', params))
                except:
                    traceback.print_exc()
                    raise Exception
                with open('/tmp/ssn_subnet_id', 'r') as f:
                    os.environ['aws_subnet_id'] = f.read()
                enable_auto_assign_ip(os.environ['aws_subnet_id'])
            except Exception as err:
                print('Error: {0}'.format(err))
                append_result("Failed to create Subnet.", str(err))
                if pre_defined_vpc:
                    remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                    remove_route_tables(tag_name, True)
                    try:
                        remove_subnets(service_base_name + "-subnet")
                    except:
                        print("Subnet hasn't been created.")
                    remove_vpc(os.environ['aws_vpc_id'])
                if pre_defined_vpc2:
                    try:
                        remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                    except:
                        print("There are no VPC Endpoints")
                    remove_route_tables(tag2_name, True)
                    remove_vpc(os.environ['aws_vpc2_id'])
                sys.exit(1)

        try:
            if os.environ['conf_duo_vpc_enable'] == 'true' and os.environ['aws_vpc_id'] and os.environ['aws_vpc2_id']:
                raise KeyError
        except KeyError:
            try:
                logging.info('[CREATE PEERING CONNECTION]')
                print('[CREATE PEERING CONNECTION]')
                os.environ['aws_peering_id'] = create_peering_connection(os.environ['aws_vpc_id'],
                                                                         os.environ['aws_vpc2_id'], service_base_name)
                print('PEERING CONNECTION ID:' + os.environ['aws_peering_id'])
                create_route_by_id(os.environ['aws_subnet_id'], os.environ['aws_vpc_id'], os.environ['aws_peering_id'],
                                   get_cidr_by_vpc(os.environ['aws_vpc2_id']))
            except Exception as err:
                print('Error: {0}'.format(err))
                append_result("Failed to create peering connection.", str(err))
                if pre_defined_vpc:
                    remove_route_tables(tag_name, True)
                    try:
                        remove_subnets(service_base_name + "-subnet")
                    except:
                        print("Subnet hasn't been created.")
                    remove_vpc(os.environ['aws_vpc_id'])
                if pre_defined_vpc2:
                    remove_peering('*')
                    try:
                        remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                    except:
                        print("There are no VPC Endpoints")
                    remove_route_tables(tag2_name, True)
                    remove_vpc(os.environ['aws_vpc2_id'])
                sys.exit(1)

        try:
            if os.environ['aws_security_groups_ids'] == '':
                raise KeyError
        except KeyError:
            try:
                pre_defined_sg = True
                logging.info('[CREATE SG FOR SSN]')
                print('[CREATE SG FOR SSN]')
                ingress_sg_rules_template = format_sg([
                    {
                        "PrefixListIds": [],
                        "FromPort": 80,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 22,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 443,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": -1,
                        "IpRanges": allowed_ip_cidr,
                        "ToPort": -1, "IpProtocol": "icmp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 80,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 443,
                        "IpRanges": allowed_vpc_cidr_ip_ranges,
                        "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    }
                ])
                egress_sg_rules_template = format_sg([
                    {"IpProtocol": "-1", "IpRanges": [{"CidrIp": all_ip_cidr}], "UserIdGroupPairs": [], "PrefixListIds": []}
                ])
                params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} " \
                         "--infra_tag_value {} --force {} --ssn {}". \
                    format(sg_name, os.environ['aws_vpc_id'], json.dumps(ingress_sg_rules_template),
                           json.dumps(egress_sg_rules_template), service_base_name, tag_name, False, True)
                try:
                    local("~/scripts/{}.py {}".format('common_create_security_group', params))
                except:
                    traceback.print_exc()
                    raise Exception
                with open('/tmp/ssn_sg_id', 'r') as f:
                    os.environ['aws_security_groups_ids'] = f.read()
            except Exception as err:
                print('Error: {0}'.format(err))
                append_result("Failed creating security group for SSN.", str(err))
                if pre_defined_vpc:
                    remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                    remove_subnets(service_base_name + "-subnet")
                    remove_route_tables(tag_name, True)
                    remove_vpc(os.environ['aws_vpc_id'])
                if pre_defined_vpc2:
                    remove_peering('*')
                    try:
                        remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                    except:
                        print("There are no VPC Endpoints")
                    remove_route_tables(tag2_name, True)
                    remove_vpc(os.environ['aws_vpc2_id'])
                sys.exit(1)
        logging.info('[CREATE ROLES]')
        print('[CREATE ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --policy_file_name {} --region {} " \
                 "--infra_tag_name {} --infra_tag_value {}".\
            format(role_name, role_profile_name, policy_name, policy_path, os.environ['aws_region'], tag_name,
                   service_base_name)
        try:
            local("~/scripts/{}.py {}".format('common_create_role_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create roles.", str(err))
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[CREATE ENDPOINT AND ROUTE-TABLE]')
        print('[CREATE ENDPOINT AND ROUTE-TABLE]')
        params = "--vpc_id {} --region {} --infra_tag_name {} --infra_tag_value {}".format(
            os.environ['aws_vpc_id'], os.environ['aws_region'], tag_name, service_base_name)
        try:
            local("~/scripts/{}.py {}".format('ssn_create_endpoint', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create an endpoint.", str(err))
        remove_all_iam_resources(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    if os.environ['conf_duo_vpc_enable'] == 'true':
        try:
            logging.info('[CREATE ENDPOINT AND ROUTE-TABLE FOR NOTEBOOK VPC]')
            print('[CREATE ENDPOINT AND ROUTE-TABLE FOR NOTEBOOK VPC]')
            params = "--vpc_id {} --region {} --infra_tag_name {} --infra_tag_value {}".format(
                os.environ['aws_vpc2_id'], os.environ['aws_region'], tag2_name, service_base_name)
            try:
                local("~/scripts/{}.py {}".format('ssn_create_endpoint', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Unable to create secondary endpoint.", str(err))
            remove_all_iam_resources(instance)
            if pre_defined_sg:
                remove_sgroups(tag_name)
            if pre_defined_subnet:
                remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                remove_subnets(service_base_name + "-subnet")
            if pre_defined_vpc:
                remove_route_tables(tag_name, True)
                remove_vpc(os.environ['aws_vpc_id'])
            if pre_defined_vpc2:
                remove_peering('*')
                try:
                    remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                remove_route_tables(tag2_name, True)
                remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)
    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {} --infra_tag_name {} --infra_tag_value {} --region {} --bucket_name_tag {}". \
                 format(ssn_bucket_name, tag_name, ssn_bucket_name, region, ssn_bucket_name_tag)

        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception

        params = "--bucket_name {} --infra_tag_name {} --infra_tag_value {} --region {} --bucket_name_tag {}". \
                 format(shared_bucket_name, tag_name, shared_bucket_name, region, shared_bucket_name_tag)

        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create bucket.", str(err))
        remove_all_iam_resources(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[CREATE SSN INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--node_name {0} --ami_id {1} --instance_type {2} --key_name {3} --security_group_ids {4} --subnet_id {5} " \
                 "--iam_profile {6} --infra_tag_name {7} --infra_tag_value {8} --instance_class {9} --primary_disk_size {10}".\
            format(instance_name, ssn_ami_id, os.environ['aws_ssn_instance_size'], os.environ['conf_key_name'],
                   os.environ['aws_security_groups_ids'], os.environ['aws_subnet_id'],
                   role_profile_name, tag_name, instance_name, 'ssn', '20')

        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        print('Error: {0}'.format(err))
        append_result("Unable to create ssn instance.", str(err))
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    if network_type == 'public':
        try:
            logging.info('[ASSOCIATING ELASTIC IP]')
            print('[ASSOCIATING ELASTIC IP]')
            ssn_id = get_instance_by_name(tag_name, instance_name)
            try:
                elastic_ip = os.environ['ssn_elastic_ip']
            except:
                elastic_ip = 'None'
            params = "--elastic_ip {} --ssn_id {} --infra_tag_name {} --infra_tag_value {}".format(
                elastic_ip, ssn_id, tag_name, elastic_ip_name)
            try:
                local("~/scripts/{}.py {}".format('ssn_associate_elastic_ip', params))
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            print('Error: {0}'.format(err))
            append_result("Failed to associate elastic ip.", str(err))
            remove_ec2(tag_name, instance_name)
            remove_all_iam_resources(instance)
            remove_s3(instance)
            if pre_defined_sg:
                remove_sgroups(tag_name)
            if pre_defined_subnet:
                remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                remove_subnets(service_base_name + "-subnet")
            if pre_defined_vpc:
                remove_vpc_endpoints(os.environ['aws_vpc_id'])
                remove_route_tables(tag_name, True)
                remove_vpc(os.environ['aws_vpc_id'])
            if pre_defined_vpc2:
                remove_peering('*')
                try:
                    remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                remove_route_tables(tag2_name, True)
                remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)

    if network_type == 'private':
        instance_ip = get_instance_ip_address(tag_name, instance_name).get('Private')
    else:
        instance_ip = get_instance_ip_address(tag_name, instance_name).get('Public')

    if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and 'ssn_subdomain' in os.environ:
        try:
            logging.info('[CREATING ROUTE53 RECORD]')
            print('[CREATING ROUTE53 RECORD]')
            try:
                create_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                       os.environ['ssn_subdomain'], instance_ip)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            append_result("Failed to create route53 record.", str(err))
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
            remove_ec2(tag_name, instance_name)
            remove_all_iam_resources(instance)
            remove_s3(instance)
            if pre_defined_sg:
                remove_sgroups(tag_name)
            if pre_defined_subnet:
                remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                remove_subnets(service_base_name + "-subnet")
            if pre_defined_vpc:
                remove_vpc_endpoints(os.environ['aws_vpc_id'])
                remove_route_tables(tag_name, True)
                remove_vpc(os.environ['aws_vpc_id'])
            if pre_defined_vpc2:
                remove_peering('*')
                try:
                    remove_vpc_endpoints(os.environ['aws_vpc2_id'])
                except:
                    print("There are no VPC Endpoints")
                remove_route_tables(tag2_name, True)
                remove_vpc(os.environ['aws_vpc2_id'])
            sys.exit(1)
