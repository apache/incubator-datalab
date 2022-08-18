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

import datalab.actions_lib
import datalab.fab
import datalab.meta_lib
import json
import multiprocessing
import os
import sys
import traceback
from fabric import *
from datalab.logger import logging
import subprocess

def configure_slave(slave_number, data_engine):
    slave_name = data_engine['slave_node_name'] + '{}'.format(slave_number + 1)
    slave_hostname = datalab.meta_lib.get_instance_private_ip_address(data_engine['tag_name'], slave_name)
    try:
        logging.info('[CREATING DATALAB SSH USER ON SLAVE NODE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            master_node_hostname, "{}{}.pem".format(os.environ['conf_key_dir'], data_engine['key_name']),
            data_engine['initial_user'], data_engine['datalab_ssh_user'], data_engine['sudo_group'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to create ssh user on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[CLEANING INSTANCE FOR SLAVE NODE]')
        params = '--hostname {} --keyfile {} --os_user {} --application {}' \
            .format(slave_hostname, keyfile_name, data_engine['datalab_ssh_user'], os.environ['application'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_clean_instance', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to clean slave instance.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE PROXY ON SLAVE NODE]')
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(slave_hostname, slave_name, keyfile_name, json.dumps(additional_config),
                    data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure proxy on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES ON SLAVE NODE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(slave_hostname, keyfile_name, data_engine['datalab_ssh_user'], data_engine['region'],
                   edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to install prerequisites on slave.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SLAVE NODE {}]'.format(slave + 1))
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} " \
                 "--scala_version {} --master_ip {} --node_type {}". \
            format(slave_hostname, keyfile_name, data_engine['region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], data_engine['datalab_ssh_user'],
                   os.environ['notebook_scala_version'], master_node_hostname,
                   'slave')
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_dataengine', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure slave node.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": data_engine['user_keyname'], "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            slave_hostname, keyfile_name, json.dumps(additional_config), data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed install users key on slave node.", str(err))
        sys.exit(1)


def clear_resources():
    datalab.actions_lib.remove_ec2(data_engine['tag_name'], data_engine['master_node_name'])
    for i in range(data_engine['instance_count'] - 1):
        slave_name = data_engine['slave_node_name'] + '{}'.format(i + 1)
        datalab.actions_lib.remove_ec2(data_engine['tag_name'], slave_name)


if __name__ == "__main__":
    try:
        logging.info('Generating infrastructure names and tags')
        data_engine = dict()
        if 'exploratory_name' in os.environ:
            data_engine['exploratory_name'] = os.environ['exploratory_name'].lower()
        else:
            data_engine['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            data_engine['computational_name'] = os.environ['computational_name'].lower()
        else:
            data_engine['computational_name'] = ''
        data_engine['service_base_name'] = (os.environ['conf_service_base_name'])
        data_engine['project_name'] = os.environ['project_name']
        data_engine['endpoint_name'] = os.environ['endpoint_name']
        data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
        data_engine['key_name'] = os.environ['conf_key_name']
        data_engine['region'] = os.environ['aws_region']
        data_engine['network_type'] = os.environ['conf_network_type']
        data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
        data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
        data_engine['master_size'] = os.environ['aws_dataengine_master_shape']
        data_engine['slave_size'] = os.environ['aws_dataengine_slave_shape']
        data_engine['dataengine_master_security_group_name'] = '{}-{}-{}-de-master-sg' \
            .format(data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['dataengine_slave_security_group_name'] = '{}-{}-{}-de-slave-sg' \
            .format(data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
        tag = {"Key": data_engine['tag_name'],
               "Value": "{}-{}-{}-subnet".format(data_engine['service_base_name'], data_engine['project_name'],
                                                 data_engine['endpoint_name'])}
        data_engine['subnet_cidr'] = datalab.meta_lib.get_subnet_by_tag(tag)
        data_engine['notebook_dataengine_role_profile_name'] = '{}-{}-{}-nb-de-profile' \
            .format(data_engine['service_base_name'], data_engine['project_name'], data_engine['endpoint_name'])
        data_engine['instance_count'] = int(os.environ['dataengine_instance_count'])
        master_node_hostname = datalab.meta_lib.get_instance_hostname(data_engine['tag_name'],
                                                                      data_engine['master_node_name'])
        data_engine['datalab_ssh_user'] = os.environ['conf_os_user']
        data_engine['user_keyname'] = data_engine['project_name']
        keyfile_name = "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        edge_instance_name = '{0}-{1}-{2}-edge'.format(data_engine['service_base_name'],
                                                       data_engine['project_name'], data_engine['endpoint_name'])
        edge_instance_hostname = datalab.meta_lib.get_instance_hostname(data_engine['tag_name'], edge_instance_name)
        edge_instance_private_ip = datalab.meta_lib.get_instance_ip_address(data_engine['tag_name'],
                                                                            edge_instance_name).get('Private')
        data_engine['edge_instance_hostname'] = datalab.meta_lib.get_instance_hostname(data_engine['tag_name'],
                                                                                       edge_instance_name)
        if os.environ['conf_os_family'] == 'debian':
            data_engine['initial_user'] = 'ubuntu'
            data_engine['sudo_group'] = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            data_engine['initial_user'] = 'ec2-user'
            data_engine['sudo_group'] = 'wheel'
    except Exception as err:
        data_engine['tag_name'] = data_engine['service_base_name'] + '-tag'
        data_engine['cluster_name'] = "{}-{}-{}-de-{}".format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])
        data_engine['master_node_name'] = data_engine['cluster_name'] + '-m'
        data_engine['slave_node_name'] = data_engine['cluster_name'] + '-s'
        clear_resources()
        datalab.fab.append_result("Failed to generate variables dictionary.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATING DATALAB SSH USER ON MASTER NODE]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format(
            master_node_hostname, "{}{}.pem".format(os.environ['conf_key_dir'], data_engine['key_name']),
            data_engine['initial_user'], data_engine['datalab_ssh_user'], data_engine['sudo_group'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('create_ssh_user', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to create ssh user on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[CLEANING INSTANCE FOR MASTER NODE]')
        params = '--hostname {} --keyfile {} --os_user {} --application {}' \
            .format(master_node_hostname, keyfile_name, data_engine['datalab_ssh_user'], os.environ['application'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_clean_instance', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to clean master instance.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE PROXY ON MASTER NODE]')
        additional_config = {"proxy_host": edge_instance_hostname, "proxy_port": "3128"}
        params = "--hostname {} --instance_name {} --keyfile {} --additional_config '{}' --os_user {}"\
            .format(master_node_hostname, data_engine['master_node_name'], keyfile_name, json.dumps(additional_config),
                    data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_proxy', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to configure proxy on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES ON MASTER NODE]')
        params = "--hostname {} --keyfile {} --user {} --region {} --edge_private_ip {}". \
            format(master_node_hostname, keyfile_name, data_engine['datalab_ssh_user'], data_engine['region'],
                   edge_instance_private_ip)
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_prerequisites', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed to install prerequisites on master.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING USERs KEY on MASTER NODE]')
        logging.info('[INSTALLING USERs KEY]')
        additional_config = {"user_keyname": data_engine['user_keyname'], "user_keydir": os.environ['conf_key_dir']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --user {}".format(
            master_node_hostname, keyfile_name, json.dumps(additional_config), data_engine['datalab_ssh_user'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('install_user_key', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        clear_resources()
        datalab.fab.append_result("Failed install users key on master node.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE MASTER NODE]')
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} " \
                 "--scala_version {} --master_ip {} --node_type {}".\
            format(master_node_hostname, keyfile_name, data_engine['region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], data_engine['datalab_ssh_user'],
                   os.environ['notebook_scala_version'], master_node_hostname,
                   'master')
        try:
            subprocess.run("~/scripts/{}.py {}".format('configure_dataengine', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure master node", str(err))
        clear_resources()
        sys.exit(1)

    try:
        jobs = []
        for slave in range(data_engine['instance_count'] - 1):
            p = multiprocessing.Process(target=configure_slave, args=(slave, data_engine))
            jobs.append(p)
            p.start()
        for job in jobs:
            job.join()
        for job in jobs:
            if job.exitcode != 0:
                raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure slave nodes.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        logging.info('[SETUP EDGE REVERSE PROXY TEMPLATE]')
        notebook_instance_ip = datalab.meta_lib.get_instance_private_ip_address('Name',
                                                                                os.environ['notebook_instance_name'])
        additional_info = {
            "computational_name": data_engine['computational_name'],
            "master_node_hostname": master_node_hostname,
            "notebook_instance_ip": notebook_instance_ip,
            "instance_count": data_engine['instance_count'],
            "master_node_name": data_engine['master_node_name'],
            "slave_node_name": data_engine['slave_node_name'],
            "tensor": False
        }
        params = "--edge_hostname {} " \
                 "--keyfile {} " \
                 "--os_user {} " \
                 "--type {} " \
                 "--exploratory_name {} " \
                 "--additional_info '{}'"\
            .format(edge_instance_hostname,
                    keyfile_name,
                    data_engine['datalab_ssh_user'],
                    'spark',
                    data_engine['exploratory_name'],
                    json.dumps(additional_info))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_configure_reverse_proxy', params), shell=True, check=True)
        except:
            datalab.fab.append_result("Failed edge reverse proxy template")
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to configure reverse proxy.", str(err))
        clear_resources()
        sys.exit(1)

    try:
        ip_address = datalab.meta_lib.get_instance_ip_address(data_engine['tag_name'],
                                                              data_engine['master_node_name']).get('Private')
        spark_master_url = "http://" + ip_address + ":8080"
        spark_master_access_url = "https://{}/{}_{}/".format(data_engine['edge_instance_hostname'],
                                                             data_engine['exploratory_name'],
                                                             data_engine['computational_name'])
        logging.info('[SUMMARY]')
        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(data_engine['service_base_name']))
        logging.info("Region: {}".format(data_engine['region']))
        logging.info("Cluster name: {}".format(data_engine['cluster_name']))
        logging.info("Master node shape: {}".format(data_engine['master_size']))
        logging.info("Slave node shape: {}".format(data_engine['slave_size']))
        logging.info("Instance count: {}".format(str(data_engine['instance_count'])))
        with open("/root/result.json", 'w') as result:
            res = {"hostname": data_engine['cluster_name'],
                   "instance_id": datalab.meta_lib.get_instance_by_name(data_engine['tag_name'],
                                                                        data_engine['master_node_name']),
                   "key_name": data_engine['key_name'],
                   "Action": "Create new Data Engine",
                   "computational_url": [
                       {"description": "Apache Spark Master",
                        "url": spark_master_access_url},
                       #{"description": "Apache Spark Master (via tunnel)",
                        #"url": spark_master_url}
                   ]}
            logging.info(json.dumps(res))
            result.write(json.dumps(res))
    except Exception as err:
        datalab.fab.append_result("Error with writing results", str(err))
        clear_resources()
        sys.exit(1)
