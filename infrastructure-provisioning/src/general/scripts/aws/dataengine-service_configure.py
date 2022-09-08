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

import argparse
import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
import multiprocessing
import os
import sys
import traceback
from datalab.common_lib import manage_pkg
from fabric import *
from datalab.logger import logging
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
parser.add_argument('--access_password', type=str, default='')
args = parser.parse_args()


def configure_dataengine_service(instance, emr_conf):
    emr_conf['instance_ip'] = instance.get('PrivateIpAddress')
    try:
        logging.info('[CREATING DATALAB SSH USER ON DATAENGINE SERVICE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format \
            (emr_conf['instance_ip'], emr_conf['key_path'], emr_conf['initial_user'],
             emr_conf['os_user'], emr_conf['sudo_group'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create DataLab ssh user.", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)

    # configuring proxy on Data Engine service
    try:
        logging.info('[CONFIGURE PROXY ON DATAENGINE SERVICE]')
        additional_config = {"proxy_host": emr_conf['edge_instance_hostname'], "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}" \
            .format(emr_conf['instance_ip'], emr_conf['cluster_name'], emr_conf['key_path'],
                    json.dumps(additional_config), emr_conf['os_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure proxy.", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE DATAENGINE SERVICE]')
        try:
            datalab.fab.configure_data_engine_service_pip(emr_conf['instance_ip'], emr_conf['os_user'],
                                                          emr_conf['key_path'], True)
            global conn
            conn = datalab.fab.init_datalab_connection(emr_conf['instance_ip'], emr_conf['os_user'], emr_conf['key_path'])
            conn.sudo('''bash -c 'echo "[main]" > /etc/yum/pluginconf.d/priorities.conf ; echo "enabled = 0" >> /etc/yum/pluginconf.d/priorities.conf' ''')
            manage_pkg('-y install', 'remote', 'R-devel')
            conn.close()
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure dataengine service.", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)


    try:
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        cluster_master_instances = emr_conf['cluster_master_instances']
        slaves = []
        for idx, instance in enumerate(emr_conf['cluster_core_instances']):
            slave = {
                'name': 'datanode{}'.format(idx + 1),
                'ip': instance.get('PrivateIpAddress'),
                'dns': instance.get('PrivateDnsName')
            }
            slaves.append(slave)
        additional_info = {
            "computational_name": emr_conf['computational_name'],
            "master_ip": cluster_master_instances[0].get('PrivateIpAddress'),
            "master_dns": cluster_master_instances[0].get('PrivateDnsName'),
            "slaves": slaves,
            "tensor": False
        }
        params = "--edge_hostname {} " \
                 "--keyfile {} " \
                 "--os_user {} " \
                 "--type {} " \
                 "--exploratory_name {} " \
                 "--additional_info '{}'"\
            .format(emr_conf['edge_instance_hostname'],
                    emr_conf['key_path'],
                    emr_conf['os_user'],
                    'dataengine-service',
                    emr_conf['exploratory_name'],
                    json.dumps(additional_info))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed edge reverse proxy template", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": emr_conf['user_keyname'], "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            emr_conf['instance_ip'], emr_conf['key_path'], json.dumps(additional_config), emr_conf['os_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed installing users key", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.INFO,
                        filename=local_log_filepath)

    try:
        datalab.actions_lib.create_aws_config_files()
        logging.info('Generating infrastructure names and tags')
        emr_conf = dict()
        if 'exploratory_name' in os.environ:
            emr_conf['exploratory_name'] = os.environ['exploratory_name'].lower()
        else:
            emr_conf['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            emr_conf['computational_name'] = os.environ['computational_name'].lower()
        else:
            emr_conf['computational_name'] = ''
        emr_conf['apps'] = 'Hadoop Hive Hue Spark Livy'
        emr_conf['service_base_name'] = os.environ['conf_service_base_name']
        emr_conf['project_name'] = os.environ['project_name']
        emr_conf['endpoint_name'] = os.environ['endpoint_name']
        emr_conf['tag_name'] = emr_conf['service_base_name'] + '-tag'
        emr_conf['key_name'] = os.environ['conf_key_name']
        emr_conf['region'] = os.environ['aws_region']
        emr_conf['release_label'] = os.environ['emr_version']
        emr_conf['master_instance_type'] = os.environ['emr_master_instance_type']
        emr_conf['slave_instance_type'] = os.environ['emr_slave_instance_type']
        emr_conf['instance_count'] = os.environ['emr_instance_count']
        emr_conf['notebook_ip'] = datalab.meta_lib.get_instance_ip_address(
            emr_conf['tag_name'], os.environ['notebook_instance_name']).get('Private')
        emr_conf['network_type'] = os.environ['conf_network_type']
        emr_conf['role_service_name'] = os.environ['emr_service_role']
        emr_conf['role_ec2_name'] = os.environ['emr_ec2_role']
        emr_conf['tags'] = "Name={0}-{1}-{2}-des-{3}-{4}," \
                           "{0}-tag={0}-{1}-{2}-des-{3}-{4}," \
                           "Notebook={5}," \
                           "State=not-configured," \
                           "Endpoint_tag={2}".format(
            emr_conf['service_base_name'], emr_conf['project_name'], emr_conf['endpoint_name'],
            emr_conf['exploratory_name'], args.uuid, os.environ['notebook_instance_name'])
        emr_conf['cluster_name'] = '{0}-{1}-{2}-des-{3}-{4}' \
            .format(emr_conf['service_base_name'],
                    emr_conf['project_name'],
                    emr_conf['endpoint_name'],
                    emr_conf['computational_name'],
                    args.uuid)
        emr_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(emr_conf['service_base_name'], emr_conf['project_name'],
                                                               emr_conf['endpoint_name']).lower().replace('_', '-')
        tag = {"Key": "{}-tag".format(emr_conf['service_base_name']), "Value": "{}-{}-{}-subnet".format(
            emr_conf['service_base_name'], emr_conf['project_name'], emr_conf['endpoint_name'])}
        emr_conf['subnet_cidr'] = datalab.meta_lib.get_subnet_by_tag(tag)
        emr_conf['key_path'] = '{}/{}.pem'.format(os.environ['conf_key_dir'],
                                                  os.environ['conf_key_name'])
        emr_conf['all_ip_cidr'] = '0.0.0.0/0'
        emr_conf['additional_emr_sg_name'] = '{}-{}-{}-de-se-additional-sg'.format(emr_conf['service_base_name'],
                                                                                   emr_conf['project_name'],
                                                                                   emr_conf['endpoint_name'])
        emr_conf['vpc_id'] = os.environ['aws_vpc_id']
        emr_conf['cluster_id'] = datalab.meta_lib.get_emr_id_by_name(emr_conf['cluster_name'])
        emr_conf['cluster_instances'] = datalab.meta_lib.get_emr_instances_list(emr_conf['cluster_id'])
        emr_conf['cluster_master_instances'] = datalab.meta_lib.get_emr_instances_list(emr_conf['cluster_id'], 'MASTER')
        emr_conf['cluster_core_instances'] = datalab.meta_lib.get_emr_instances_list(emr_conf['cluster_id'], 'CORE')
        emr_conf['edge_instance_name'] = '{0}-{1}-{2}-edge'.format(emr_conf['service_base_name'],
                                                                   emr_conf['project_name'], emr_conf['endpoint_name'])
        emr_conf['edge_instance_hostname'] = datalab.meta_lib.get_instance_private_ip_address(
            emr_conf['tag_name'], emr_conf['edge_instance_name'])
        emr_conf['edge_instance_hostname'] = datalab.meta_lib.get_instance_hostname(emr_conf['tag_name'],
                                                                                    emr_conf['edge_instance_name'])
        emr_conf['user_keyname'] = emr_conf['project_name']
        emr_conf['os_user'] = os.environ['conf_os_user']
        emr_conf['initial_user'] = 'ec2-user'
        emr_conf['sudo_group'] = 'wheel'
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)

    try:
        jobs = []
        for instance in emr_conf['cluster_instances']:
            p = multiprocessing.Process(target=configure_dataengine_service, args=(instance, emr_conf))
            jobs.append(p)
            p.start()
        for job in jobs:
            job.join()
        for job in jobs:
            if job.exitcode != 0:
                raise Exception
    except:
        traceback.print_exc()
        raise Exception

    try:
        logging.info('[SUMMARY]')
        ip_address = emr_conf['cluster_master_instances'][0].get('PrivateIpAddress')
        emr_master_url = "http://" + ip_address + ":8088"
        emr_master_acces_url = "https://{}/{}_{}/".format(emr_conf['edge_instance_hostname'],
                                                          emr_conf['exploratory_name'],
                                                          emr_conf['computational_name'])
        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(emr_conf['service_base_name']))
        logging.info("Cluster name: {}".format(emr_conf['cluster_name']))
        logging.info("Cluster id: {}".format(datalab.meta_lib.get_emr_id_by_name(emr_conf['cluster_name'])))
        logging.info("Key name: {}".format(emr_conf['key_name']))
        logging.info("Region: {}".format(emr_conf['region']))
        logging.info("EMR version: {}".format(emr_conf['release_label']))
        logging.info("EMR master node shape: {}".format(emr_conf['master_instance_type']))
        logging.info("EMR slave node shape: {}".format(emr_conf['slave_instance_type']))
        logging.info("Instance count: {}".format(emr_conf['instance_count']))
        logging.info("Notebook IP address: {}".format(emr_conf['notebook_ip']))
        logging.info("Bucket name: {}".format(emr_conf['bucket_name']))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": emr_conf['cluster_name'],
                   "instance_id": datalab.meta_lib.get_emr_id_by_name(emr_conf['cluster_name']),
                   "key_name": emr_conf['key_name'],
                   "user_own_bucket_name": emr_conf['bucket_name'],
                   "Action": "Create new EMR cluster",
                   "computational_url": [
                       {"description": "EMR Master",
                        "url": emr_master_acces_url},
                       # {"description": "EMR Master (via tunnl)",
                       # "url": emr_master_url}
                   ]}
            print(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        datalab.actions_lib.terminate_emr(emr_conf['cluster_id'])
        sys.exit(1)
