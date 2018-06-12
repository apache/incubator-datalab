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
import time
from fabric.api import *
from dlab.fab import *
from dlab.meta_lib import *
from dlab.actions_lib import *
import argparse
import sys
import os
import logging


parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)
    try:
        os.environ['exploratory_name']
    except:
        os.environ['exploratory_name'] = ''
    if os.path.exists('/response/.emr_creating_{}'.format(os.environ['exploratory_name'])):
        time.sleep(30)
    create_aws_config_files()
    edge_status = get_instance_status(os.environ['conf_service_base_name'] + '-Tag',
        os.environ['conf_service_base_name'] + '-' + os.environ['edge_user_name'] + '-edge')
    if edge_status != 'running':
        logging.info('ERROR: Edge node is unavailable! Aborting...')
        print('ERROR: Edge node is unavailable! Aborting...')
        ssn_hostname = get_instance_hostname(
            os.environ['conf_service_base_name'] + '-Tag',
            os.environ['conf_service_base_name'] + '-ssn')
        put_resource_status('edge', 'Unavailable',
                            os.environ['ssn_dlab_path'],
                            os.environ['conf_os_user'], ssn_hostname)
        append_result("Edge node is unavailable")
        sys.exit(1)
    print('Generating infrastructure names and tags')
    emr_conf = dict()
    try:
        emr_conf['exploratory_name'] = os.environ['exploratory_name']
    except:
        emr_conf['exploratory_name'] = ''
    try:
        emr_conf['computational_name'] = os.environ['computational_name']
    except:
        emr_conf['computational_name'] = ''
    emr_conf['apps'] = 'Hadoop Hive Hue Spark'
    emr_conf['service_base_name'] = os.environ['conf_service_base_name']
    emr_conf['tag_name'] = emr_conf['service_base_name'] + '-Tag'
    emr_conf['key_name'] = os.environ['conf_key_name']
    emr_conf['region'] = os.environ['aws_region']
    emr_conf['release_label'] = os.environ['emr_version']
    emr_conf['edge_instance_name'] = emr_conf['service_base_name'] + \
                                     "-" + os.environ['edge_user_name'] + '-edge'
    emr_conf['edge_security_group_name'] = emr_conf['edge_instance_name'] + '-SG'
    emr_conf['master_instance_type'] = os.environ['emr_master_instance_type']
    emr_conf['slave_instance_type'] = os.environ['emr_slave_instance_type']
    emr_conf['instance_count'] = os.environ['emr_instance_count']
    emr_conf['notebook_ip'] = get_instance_ip_address(
        emr_conf['tag_name'], os.environ['notebook_instance_name']).get('Private')
    emr_conf['role_service_name'] = os.environ['emr_service_role']
    emr_conf['role_ec2_name'] = os.environ['emr_ec2_role']
    emr_conf['tags'] = 'Name=' + \
                       emr_conf['service_base_name'] + '-' + \
                       os.environ['edge_user_name'] + '-des-' + \
                       emr_conf['exploratory_name'] + '-' + \
                       emr_conf['computational_name'] + '-' + \
                       args.uuid + ', ' + \
                       emr_conf['service_base_name'] + '-Tag=' + \
                       emr_conf['service_base_name'] + '-' + \
                       os.environ['edge_user_name'] + '-des-' + \
                       emr_conf['exploratory_name'] + '-' + \
                       emr_conf['computational_name'] + '-' + \
                       args.uuid + ', Notebook=' + \
                       os.environ['notebook_instance_name'] + \
                       ', State=not-configured'
    emr_conf['cluster_name'] = emr_conf['service_base_name'] + \
                               '-' + os.environ['edge_user_name'] + \
                               '-des-' + emr_conf['exploratory_name'] + \
                               '-' + emr_conf['computational_name'] + \
                               '-' + args.uuid
    emr_conf['bucket_name'] = (emr_conf['service_base_name'] +
                               '-ssn-bucket').lower().replace('_', '-')

    tag = {"Key": "{}-Tag".format(emr_conf['service_base_name']),
           "Value": "{}-{}-subnet".format(emr_conf['service_base_name'],
                                          os.environ['edge_user_name'])}
    emr_conf['subnet_cidr'] = get_subnet_by_tag(tag)
    emr_conf['key_path'] = os.environ['conf_key_dir'] + \
                           os.environ['conf_key_name'] + '.pem'
    emr_conf['all_ip_cidr'] = '0.0.0.0/0'
    emr_conf['additional_emr_sg_name'] = \
        '{}-{}-de-se-additional-sg'.format(emr_conf['service_base_name'],
                                           os.environ['edge_user_name'])
    emr_conf['vpc_id'] = os.environ['aws_vpc_id']
    if os.environ['emr_slave_instance_spot'] == 'True':
        emr_conf['slave_bid_price'] = (
                                          float(get_ec2_price(
            emr_conf['slave_instance_type'], emr_conf['region'])) *
                                       int(os.environ['emr_slave_instance_spot_pct_price']))/100
    else:
        emr_conf['slave_bid_price'] = 0

    try:
        emr_conf['emr_timeout'] = os.environ['emr_timeout']
    except:
        emr_conf['emr_timeout'] = "1200"

    print("Will create exploratory environment with edge node "
          "as access point as following: {}".
          format(json.dumps(emr_conf,
                            sort_keys=True,
                            indent=4,
                            separators=(',', ': '))))
    logging.info(json.dumps(emr_conf))

    with open('/root/result.json', 'w') as f:
        data = {"hostname": emr_conf['cluster_name'], "error": ""}
        json.dump(data, f)

    try:
        emr_waiter(os.environ['notebook_instance_name'])
        local('touch /response/.emr_creating_{}'.format(os.environ['exploratory_name']))
    except Exception as err:
        traceback.print_exc()
        append_result("EMR waiter fail.", str(err))
        sys.exit(1)

    with open('/root/result.json', 'w') as f:
        data = {"hostname": emr_conf['cluster_name'], "error": ""}
        json.dump(data, f)

    logging.info('[CREATING ADDITIONAL SECURITY GROUPS FOR EMR]')
    print("[CREATING ADDITIONAL SECURITY GROUPS FOR EMR]")
    try:
        edge_group_id = check_security_group(emr_conf['edge_security_group_name'])
        cluster_sg_ingress = [
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": emr_conf['subnet_cidr']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [],
                "UserIdGroupPairs": [{"GroupId": edge_group_id}],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": get_instance_ip_address(
                    emr_conf['tag_name'], '{}-ssn'.format(
                    emr_conf['service_base_name'])).get('Private') + "/32"}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            }
        ]
        cluster_sg_egress = [
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": emr_conf['subnet_cidr']}],
                "UserIdGroupPairs": [],
                "PrefixListIds": []
            },
            {
                "IpProtocol": "-1",
                "IpRanges": [{"CidrIp": get_instance_ip_address(
                    emr_conf['tag_name'], '{}-ssn'.format(
                    emr_conf['service_base_name'])).get('Private') + "/32"}],
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
                "IpRanges": [{"CidrIp": emr_conf['all_ip_cidr']}],
                "FromPort": 443,
                "ToPort": 443,
                "UserIdGroupPairs": [],
                "PrefixListIds": [],
            }
        ]

        params = "--name {} " \
                 "--vpc_id {} " \
                 "--security_group_rules '{}' " \
                 "--egress '{}' " \
                 "--infra_tag_name {} " \
                 "--infra_tag_value {} " \
                 "--force {}". \
            format(emr_conf['additional_emr_sg_name'],
                   emr_conf['vpc_id'],
                   json.dumps(cluster_sg_ingress),
                   json.dumps(cluster_sg_egress),
                   emr_conf['service_base_name'],
                   emr_conf['cluster_name'], True)
        try:
            local("~/scripts/{}.py {}".format('common_create_security_group', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to create sg.", str(err))
        sys.exit(1)

    local("echo Waiting for changes to propagate; sleep 10")

    try:
        logging.info('[Creating EMR Cluster]')
        print('[Creating EMR Cluster]')
        params = "--name {} " \
                 "--applications '{}' " \
                 "--master_instance_type {} " \
                 "--slave_instance_type {} " \
                 "--instance_count {} " \
                 "--ssh_key {} " \
                 "--release_label {} " \
                 "--emr_timeout {} " \
                 "--subnet {} " \
                 "--service_role {} " \
                 "--ec2_role {} " \
                 "--nbs_ip {} " \
                 "--nbs_user {} " \
                 "--s3_bucket {} " \
                 "--region {} " \
                 "--tags '{}' " \
                 "--key_dir {} " \
                 "--edge_user_name {} " \
                 "--slave_instance_spot {} " \
                 "--bid_price {} " \
                 "--service_base_name {} " \
                 "--additional_emr_sg {}"\
            .format(emr_conf['cluster_name'],
                    emr_conf['apps'],
                    emr_conf['master_instance_type'],
                    emr_conf['slave_instance_type'],
                    emr_conf['instance_count'],
                    emr_conf['key_name'],
                    emr_conf['release_label'],
                    emr_conf['emr_timeout'],
                    emr_conf['subnet_cidr'],
                    emr_conf['role_service_name'],
                    emr_conf['role_ec2_name'],
                    emr_conf['notebook_ip'],
                    os.environ['conf_os_user'],
                    emr_conf['bucket_name'],
                    emr_conf['region'],
                    emr_conf['tags'],
                    os.environ['conf_key_dir'],
                    os.environ['edge_user_name'],
                    os.environ['emr_slave_instance_spot'],
                    str(emr_conf['slave_bid_price']),
                    emr_conf['service_base_name'],
                    emr_conf['additional_emr_sg_name'])
        try:
            local("~/scripts/{}.py {}".format('dataengine-service_create', params))
        except:
            traceback.print_exc()
            raise Exception

        cluster_name = emr_conf['cluster_name']
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], emr_conf['key_name'])
        local('rm /response/.emr_creating_{}'.format(os.environ['exploratory_name']))
    except Exception as err:
        append_result("Failed to create EMR Cluster.", str(err))
        local('rm /response/.emr_creating_{}'.format(os.environ['exploratory_name']))
        emr_id = get_emr_id_by_name(emr_conf['cluster_name'])
        terminate_emr(emr_id)
        sys.exit(1)
    sys.exit(0)