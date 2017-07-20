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
    try:
        logging.info('[CREATE AWS CONFIG FILE]')
        print '[CREATE AWS CONFIG FILE]'
        if not create_aws_config_files(generate_full_config=True):
            logging.info('Unable to create configuration')
            append_result("Unable to create configuration")
            traceback.print_exc()
            sys.exit(1)
    except:
        sys.exit(1)

    try:
        logging.info('[DERIVING NAMES]')
        print '[DERIVING NAMES]'
        service_base_name = os.environ['conf_service_base_name']
        role_name = service_base_name.lower().replace('-', '_') + '-ssn-Role'
        role_profile_name = service_base_name.lower().replace('-', '_') + '-ssn-Profile'
        policy_name = service_base_name.lower().replace('-', '_') + '-ssn-Policy'
        user_bucket_name = (service_base_name + '-ssn-bucket').lower().replace('_', '-')
        tag_name = service_base_name + '-Tag'
        instance_name = service_base_name + '-ssn'
        region = os.environ['aws_region']
        ssn_ami_name = os.environ['aws_' + os.environ['conf_os_family'] + '_ami_name']
        ssn_ami_id = get_ami_id(ssn_ami_name)
        policy_path = '/root/files/ssn_policy.json'
        vpc_cidr = '172.31.0.0/16'
        sg_name = instance_name + '-SG'

        try:
            if os.environ['aws_vpc_id'] == '':
                raise KeyError
        except KeyError:
            try:
                pre_defined_vpc = True
                logging.info('[CREATE VPC AND ROUTE TABLE]')
                print '[CREATE VPC AND ROUTE TABLE]'
                params = "--vpc {} --region {} --infra_tag_name {} --infra_tag_value {}".format(vpc_cidr, region, tag_name, service_base_name)
                try:
                    local("~/scripts/{}.py {}".format('ssn_create_vpc', params))
                except:
                    traceback.print_exc()
                    raise Exception
                os.environ['aws_vpc_id'] = get_vpc_by_tag(tag_name, service_base_name)
            except Exception as err:
                append_result("Failed to create VPC. Exception:" + str(err))
                sys.exit(1)

        try:
            if os.environ['aws_subnet_id'] == '':
                raise KeyError
        except KeyError:
            try:
                pre_defined_subnet = True
                logging.info('[CREATE SUBNET]')
                print '[CREATE SUBNET]'
                params = "--vpc_id {} --username {} --infra_tag_name {} --infra_tag_value {} --prefix {} --ssn {}".format(os.environ['aws_vpc_id'], 'ssn', tag_name, service_base_name, '20', True)
                try:
                    local("~/scripts/{}.py {}".format('common_create_subnet', params))
                except:
                    traceback.print_exc()
                    raise Exception
                with open('/tmp/ssn_subnet_id', 'r') as f:
                    os.environ['aws_subnet_id'] = f.read()
                enable_auto_assign_ip(os.environ['aws_subnet_id'])
            except Exception as err:
                append_result("Failed to create Subnet.", str(err))
                if pre_defined_vpc:
                    remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                    remove_route_tables(tag_name, True)
                    try:
                        remove_subnets(service_base_name + "-subnet")
                    except:
                        print "Subnet hasn't been created."
                    remove_vpc(os.environ['aws_vpc_id'])
                sys.exit(1)

        try:
            if os.environ['aws_security_groups_ids'] == '':
                raise KeyError
        except KeyError:
            try:
                pre_defined_sg = True
                logging.info('[CREATE SG FOR SSN]')
                print '[CREATE SG FOR SSN]'
                ingress_sg_rules_template = [
                    {
                        "PrefixListIds": [],
                        "FromPort": 80,
                        "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                        "ToPort": 80, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 8080,
                        "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                        "ToPort": 8080, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 22,
                        "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                        "ToPort": 22, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 3128,
                        "IpRanges": [{"CidrIp": vpc_cidr}],
                        "ToPort": 3128, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": 443,
                        "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                        "ToPort": 443, "IpProtocol": "tcp", "UserIdGroupPairs": []
                    },
                    {
                        "PrefixListIds": [],
                        "FromPort": -1,
                        "IpRanges": [{"CidrIp": "0.0.0.0/0"}],
                        "ToPort": -1, "IpProtocol": "icmp", "UserIdGroupPairs": []
                    }
                ]
                egress_sg_rules_template = [
                    {"IpProtocol": "-1", "IpRanges": [{"CidrIp": "0.0.0.0/0"}], "UserIdGroupPairs": [], "PrefixListIds": []}
                ]
                params = "--name {} --vpc_id {} --security_group_rules '{}' --egress '{}' --infra_tag_name {} --infra_tag_value {} --force {} --ssn {}". \
                    format(sg_name, os.environ['aws_vpc_id'], json.dumps(ingress_sg_rules_template), json.dumps(egress_sg_rules_template), service_base_name, tag_name, False, True)
                try:
                    local("~/scripts/{}.py {}".format('common_create_security_group', params))
                except:
                    traceback.print_exc()
                    raise Exception
                with open('/tmp/ssn_sg_id', 'r') as f:
                    os.environ['aws_security_groups_ids'] = f.read()
            except Exception as err:
                append_result("Failed creating security group for SSN.", str(err))
                if pre_defined_vpc:
                    remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
                    remove_subnets(service_base_name + "-subnet")
                    remove_route_tables(tag_name, True)
                    remove_vpc(os.environ['aws_vpc_id'])
                sys.exit(1)
        logging.info('[CREATE ROLES]')
        print('[CREATE ROLES]')
        params = "--role_name {} --role_profile_name {} --policy_name {} --policy_file_name {} --region {}".\
            format(role_name, role_profile_name, policy_name, policy_path, os.environ['aws_region'])
        try:
            local("~/scripts/{}.py {}".format('common_create_role_policy', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Unable to create roles.", str(err))
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
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
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {} --infra_tag_name {} --infra_tag_value {} --region {}". \
                 format(user_bucket_name, tag_name, user_bucket_name, region)

        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
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
        sys.exit(1)

    try:
        logging.info('[CREATE SSN INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} --subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {}".\
            format(instance_name, ssn_ami_id, os.environ['ssn_instance_size'], os.environ['conf_key_name'],
                   os.environ['aws_security_groups_ids'], os.environ['aws_subnet_id'],
                   role_profile_name, tag_name, instance_name)

        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
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
        sys.exit(1)