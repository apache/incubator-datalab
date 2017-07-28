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
import traceback


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/edge/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print 'Generating infrastructure names and tags'
    edge_conf = dict()
    edge_conf['service_base_name'] = os.environ['conf_service_base_name']
    edge_conf['key_name'] = os.environ['conf_key_name']
    edge_conf['user_keyname'] = os.environ['edge_user_name']
    try:
        if os.environ['gcp_vpc_name'] == '':
            raise KeyError
        else:
            edge_conf['vpc_name'] = os.environ['gcp_vpc_name']
    except KeyError:
        edge_conf['vpc_name'] = edge_conf['service_base_name'] + '-ssn-vpc'
    edge_conf['vpc_cidr'] = '10.10.0.0/16'
    edge_conf['private_subnet_name'] = edge_conf['service_base_name'] + '-' + os.environ['edge_user_name']
    edge_conf['subnet_name'] = os.environ['gcp_subnet_name']
    edge_conf['region'] = os.environ['gcp_region']
    edge_conf['zone'] = os.environ['gcp_zone']
    edge_conf['vpc_selflink'] = GCPMeta().get_vpc(edge_conf['vpc_name'])['selfLink']
    edge_conf['private_subnet_prefix'] = os.environ['gcp_private_subnet_prefix']
    edge_conf['edge_service_account_name'] = 'dlabowner' # edge_conf['service_base_name'].lower().replace('-', '_') + "-" + os.environ[
        # 'edge_user_name'] + '-edge-Role'
    edge_conf['notebook_service_account_name'] = 'dlabowner' # edge_conf['service_base_name'].lower().replace('-', '_') + "-" + os.environ[
        # 'edge_user_name'] + '-nb-Role'
    edge_conf['instance_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-edge'
    edge_conf['firewall_name'] = edge_conf['instance_name'] + '-firewall'
    edge_conf['notebook_firewall_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + \
                                          '-nb-firewall'
    edge_conf['bucket_name'] = (
    edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-bucket').lower().replace('_', '-')
    edge_conf['instance_size'] = os.environ['gcp_edge_instance_size']
    edge_conf['ssh_key_path'] = '/root/keys/' + os.environ['conf_key_name'] + '.pem'
    edge_conf['ami_name'] = os.environ['gcp_' + os.environ['conf_os_family'] + '_ami_name']
    edge_conf['static_address_name'] = edge_conf['service_base_name'] + "-" + os.environ['edge_user_name'] + '-ip'

    # FUSE in case of absence of user's key
    fname = "/root/keys/{}.pub".format(edge_conf['user_keyname'])
    if not os.path.isfile(fname):
        print "USERs PUBLIC KEY DOES NOT EXIST in {}".format(fname)
        sys.exit(1)

    print "Will create exploratory environment with edge node as access point as following: " + \
          json.dumps(edge_conf, sort_keys=True, indent=4, separators=(',', ': '))
    logging.info(json.dumps(edge_conf))

    try:
        logging.info('[CREATE SUBNET]')
        print '[CREATE SUBNET]'
        params = "--subnet_name {} --region {} --vpc_selflink {} --prefix {} --vpc_cidr {}" \
                 .format(edge_conf['private_subnet_name'], edge_conf['region'], edge_conf['vpc_selflink'],
                         edge_conf['private_subnet_prefix'], edge_conf['vpc_cidr'])
        try:
            local("~/scripts/{}.py {}".format('common_create_subnet', params))
            edge_conf['private_subnet_cidr'] = GCPMeta().get_subnet(edge_conf['private_subnet_name'],
                                                                    edge_conf['region'])['ipCidrRange']
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        except:
            print "Subnet hasn't been created."
        append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    print 'NEW SUBNET CIDR CREATED: {}'.format(edge_conf['private_subnet_cidr'])

    # try:
    #     logging.info('[CREATE EDGE ROLES]')
    #     print '[CREATE EDGE ROLES]'
    #     params = "--role_name {} --role_profile_name {} --policy_name {} --region {}" \
    #              .format(edge_conf['role_name'], edge_conf['role_profile_name'],
    #                      edge_conf['policy_name'], os.environ['gcp_region'])
    #     try:
    #         local("~/scripts/{}.py {}".format('common_create_role_policy', params))
    #     except:
    #         traceback.print_exc()
    #         raise Exception
    # except Exception as err:
    #     append_result("Failed to creating roles.", str(err))
    #     sys.exit(1)
    #
    # try:
    #     logging.info('[CREATE BACKEND (NOTEBOOK) ROLES]')
    #     print '[CREATE BACKEND (NOTEBOOK) ROLES]'
    #     params = "--role_name {} --role_profile_name {} --policy_name {} --region {}" \
    #              .format(edge_conf['notebook_role_name'], edge_conf['notebook_role_profile_name'],
    #                      edge_conf['notebook_policy_name'], os.environ['gcp_region'])
    #     try:
    #         local("~/scripts/{}.py {}".format('common_create_role_policy', params))
    #     except:
    #         traceback.print_exc()
    #         raise Exception
    # except Exception as err:
    #     append_result("Failed to creating roles.", str(err))
    #     remove_all_iam_resources('edge', os.environ['edge_user_name'])
    #     sys.exit(1)

    try:
        pre_defined_firewall = True
        logging.info('[CREATE FIREWALL FOR EDGE NODE]')
        print '[CREATE FIREWALL]'
        firewall = {}
        firewall['name'] = edge_conf['firewall_name']
        firewall['targetTags'] = [edge_conf['instance_name']]
        firewall['sourceRanges'] = ['0.0.0.0/0']
        rules = [
            {
                'IPProtocol': 'tcp',
                'ports': ['22']
            },
            {
                'IPProtocol': 'icmp'
            }
        ]
        firewall['allowed'] = rules
        firewall['network'] = edge_conf['vpc_selflink']

        params = "--firewall '{}'".format(json.dumps(firewall))
        try:
            local("~/scripts/{}.py {}".format('common_create_firewall', params))
        except:
            traceback.print_exc()
            raise Exception

        firewall['name'] = edge_conf['firewall_name'] + '-internal'
        firewall['targetTags'] = [edge_conf['instance_name']]
        firewall['sourceRanges'] = [edge_conf['private_subnet_cidr']]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        firewall['allowed'] = rules
        firewall['network'] = edge_conf['vpc_selflink']

        params = "--firewall '{}'".format(json.dumps(firewall))
        try:
            local("~/scripts/{}.py {}".format('common_create_firewall', params))
        except:
            traceback.print_exc()
            raise Exception

    except Exception as err:
        append_result("Failed to create Firewall.", str(err))
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATE FIREWALL FOR PRIVATE SUBNET]')
        print '[CREATE FIREWALL FOR PRIVATE SUBNET]'
        firewall = {}
        firewall['name'] = edge_conf['notebook_firewall_name']
        firewall['sourceRanges'] = [edge_conf['private_subnet_cidr']]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        firewall['allowed'] = rules
        firewall['network'] = edge_conf['vpc_selflink']

        params = "--firewall '{}'".format(json.dumps(firewall))
        try:
            local("~/scripts/{}.py {}".format('common_create_firewall', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        GCPActions().remove_firewall(edge_conf['firewall_name'])
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        print('[CREATE BUCKETS]')
        params = "--bucket_name {}".format(edge_conf['bucket_name'])

        try:
            local("~/scripts/{}.py {}".format('common_create_bucket', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Unable to create bucket.", str(err))
        GCPActions().remove_firewall(edge_conf['notebook_firewall_name'])
        GCPActions().remove_firewall(edge_conf['firewall_name'])
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    # try:
    #     logging.info('[CREATING BUCKET POLICY FOR USER INSTANCES]')
    #     print('[CREATING BUCKET POLICY FOR USER INSTANCES]')
    #     params = '--bucket_name {} --ssn_bucket_name {} --username {} --edge_role_name {} --notebook_role_name {} --service_base_name {} --region {}'.format(
    #         edge_conf['bucket_name'], edge_conf['ssn_bucket_name'], os.environ['edge_user_name'],
    #         edge_conf['role_name'], edge_conf['notebook_role_name'],  edge_conf['service_base_name'],
    #         edge_conf['region'])
    #     try:
    #         local("~/scripts/{}.py {}".format('common_create_policy', params))
    #     except:
    #         traceback.print_exc()
    # except Exception as err:
    #     append_result("Failed to create bucket policy.", str(err))
    #     remove_all_iam_resources('notebook', os.environ['edge_user_name'])
    #     remove_all_iam_resources('edge', os.environ['edge_user_name'])
    #     remove_sgroups(edge_conf['notebook_instance_name'])
    #     remove_sgroups(edge_conf['instance_name'])
    #     remove_s3('edge', os.environ['edge_user_name'])
    #     sys.exit(1)

    try:
        logging.info('[CREATING ELASTIC IP ADDRESS]')
        print '[CREATING ELASTIC IP ADDRESS]'
        try:
            edge_conf['elastic_ip'] = os.environ['edge_elastic_ip']
        except:
            params = "--address_name {} --region {}".format(edge_conf['static_address_name'], edge_conf['region'])
            try:
                local("~/scripts/{}.py {}".format('edge_create_elastic_ip', params))
            except:
                traceback.print_exc()
                raise Exception
    except Exception as err:
        append_result("Failed to create elastic ip.", str(err))
        try:
            GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        except:
            print "Elastic IP address hasn't been created."
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['notebook_firewall_name'])
        GCPActions().remove_firewall(edge_conf['firewall_name'])
        GCPActions().remove_subnet(edge_conf['private_subnet_name'], edge_conf['region'])
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        initial_user = 'ubuntu'
        sudo_group = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        initial_user = 'ec2-user'
        sudo_group = 'wheel'

    try:
        edge_conf['elastic_ip'] = \
            GCPMeta().get_static_address(edge_conf['region'], edge_conf['static_address_name'])['address']
        logging.info('[CREATE EDGE INSTANCE]')
        print('[CREATE SSN INSTANCE]')
        params = "--instance_name {} --region {} --zone {} --vpc_name {} --subnet_name {} --instance_size {} --ssh_key_path {} --initial_user {} --service_account_name {} --ami_name {} --instance_class {} --elastic_ip {}".\
            format(edge_conf['instance_name'], edge_conf['region'], edge_conf['zone'], edge_conf['vpc_name'],
                   edge_conf['subnet_name'], edge_conf['instance_size'], edge_conf['ssh_key_path'], initial_user,
                   edge_conf['edge_service_account_name'], edge_conf['ami_name'], 'edge', edge_conf['elastic_ip'])
        try:
            local("~/scripts/{}.py {}".format('common_create_instance', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        GCPActions().remove_static_address(edge_conf['static_address_name'], edge_conf['region'])
        GCPActions().remove_bucket(edge_conf['bucket_name'])
        GCPActions().remove_firewall(edge_conf['notebook_firewall_name'])
        GCPActions().remove_firewall(edge_conf['firewall_name'])
        GCPActions().remove_subnet(edge_conf['subnet_name'], edge_conf['region'])
        sys.exit(1)