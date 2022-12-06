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
from datalab.logger import logging
import multiprocessing
import os
import sys
import traceback
import subprocess
from Crypto.PublicKey import RSA
from fabric import *
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--uuid', type=str, default='')
parser.add_argument('--access_password', type=str, default='')
args = parser.parse_args()


def add_notebook_secret(resource_group_name, instance_name, os_user, keyfile, computational_name, cluster_password):
    private_ip = AzureMeta.get_private_ip_address(resource_group_name, instance_name)
    global conn
    conn = datalab.fab.init_datalab_connection(private_ip, os_user, keyfile)
    datalab.actions_lib.ensure_hdinsight_secret(os_user, computational_name, cluster_password)


if __name__ == "__main__":
    try:
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
        logging.info('Generating infrastructure names and tags')
        hdinsight_conf = dict()
        hdinsight_conf['service_base_name'] = os.environ['conf_service_base_name']
        hdinsight_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        hdinsight_conf['region'] = os.environ['azure_region']
        hdinsight_conf['key_name'] = os.environ['conf_key_name']
        hdinsight_conf['vpc_name'] = os.environ['azure_vpc_name']
        hdinsight_conf['user_name'] = os.environ['edge_user_name']
        hdinsight_conf['project_name'] = os.environ['project_name']
        hdinsight_conf['project_tag'] = hdinsight_conf['project_name']
        hdinsight_conf['endpoint_name'] = os.environ['endpoint_name']
        hdinsight_conf['endpoint_tag'] = hdinsight_conf['endpoint_name']
        hdinsight_conf['hdinsight_master_instance_type'] = os.environ['hdinsight_master_instance_type']
        hdinsight_conf['hdinsight_slave_instance_type'] = os.environ['hdinsight_slave_instance_type']
        hdinsight_conf['key_path'] = '{}/{}.pem'.format(os.environ['conf_key_dir'],
                                                        os.environ['conf_key_name'])
        if 'computational_name' in os.environ:
            hdinsight_conf['computational_name'] = os.environ['computational_name'].lower().replace('_', '-')
        else:
            hdinsight_conf['computational_name'] = ''

        hdinsight_conf['cluster_name'] = '{}-{}-{}-des-{}'.format(hdinsight_conf['service_base_name'],
                                                                  hdinsight_conf['project_name'],
                                                                  hdinsight_conf['endpoint_name'],
                                                                  hdinsight_conf['computational_name'])

        hdinsight_conf['full_cluster_name'] = '{}-{}-{}-{}-des-{}'.format(args.uuid, hdinsight_conf['service_base_name'],
                                                                     hdinsight_conf['project_name'],
                                                                     hdinsight_conf['endpoint_name'],
                                                                     hdinsight_conf['computational_name'])
        hdinsight_conf["instance_id"] = hdinsight_conf["full_cluster_name"]
        hdinsight_conf['cluster_url'] = 'https://{}.azurehdinsight.net'.format(hdinsight_conf['full_cluster_name'])
        hdinsight_conf['cluster_jupyter_url'] = '{}/jupyter/'.format(hdinsight_conf['cluster_url'])
        hdinsight_conf['cluster_sparkhistory_url'] = '{}/sparkhistory/'.format(hdinsight_conf['cluster_url'])
        hdinsight_conf['cluster_zeppelin_url'] = '{}/zeppelin/'.format(hdinsight_conf['cluster_url'])

        if os.environ["application"] == "rstudio":
            add_notebook_secret(hdinsight_conf['resource_group_name'], os.environ["notebook_instance_name"],
                                os.environ["conf_os_user"], hdinsight_conf['key_path'],
                                hdinsight_conf['computational_name'], args.access_password)
            hdinsight_conf['rstudio_livy_connection'] = 'library(sparklyr); ' \
                                                        'sc <- spark_connect(master = "{}/livy/", ' \
                                                        'version = "3.1.1", method = "livy", ' \
                                                        'config = livy_config(username = "{}", ' \
                                                        'password = Sys.getenv("{}-access-password")))' \
                .format(hdinsight_conf['cluster_url'], os.environ["conf_os_user"], hdinsight_conf['computational_name'])
        else:
            hdinsight_conf['rstudio_livy_connection'] = ''

        logging.info('[SUMMARY]')
        logging.info("Service base name: {}".format(hdinsight_conf['service_base_name']))
        logging.info("Region: {}".format(hdinsight_conf['region']))
        logging.info("Cluster name: {}".format(hdinsight_conf['full_cluster_name']))
        logging.info("Master node shape: {}".format(hdinsight_conf['hdinsight_master_instance_type']))
        logging.info("Slave node shape: {}".format(hdinsight_conf['hdinsight_slave_instance_type']))
        logging.info("Instance count: {}".format(str(os.environ['hdinsight_count'])))
        logging.info("URL access username: datalab-user")
        logging.info("URL access password: {}".format(args.access_password))
        logging.info("Cluster URL: {}".format(hdinsight_conf['cluster_url']))
        logging.info("Spark History URL: {}".format(hdinsight_conf['cluster_sparkhistory_url']))
        logging.info("Jupyter URL: {}".format(hdinsight_conf['cluster_jupyter_url']))
        logging.info("Zeppelin URL: {}".format(hdinsight_conf['cluster_zeppelin_url']))

        with open("/root/result.json", 'w') as result:
            res = {"hostname": hdinsight_conf['full_cluster_name'],
                   "key_name": hdinsight_conf['key_name'],
                   "instance_id": hdinsight_conf["instance_id"],
                   "Action": "Create new HDInsight cluster",
                   "computational_url": [
                       # {"description": "HDInsight cluster",
                       #  "url": hdinsight_conf['cluster_url']},
                       {"description": "Apache Spark History",
                        "url": hdinsight_conf['cluster_sparkhistory_url']},
                       {"description": "Connection string",
                        "url": hdinsight_conf['rstudio_livy_connection']}
                       # {"description": "Jupyter notebook",
                       #  "url": hdinsight_conf['cluster_jupyter_url']},
                       # {"description": "Zeppelin notebook",
                       #  "url": hdinsight_conf['cluster_zeppelin_url']}
                   ],
                   }
            result.write(json.dumps(res))
    except Exception as err:
        traceback.print_exc()
        datalab.fab.append_result("Error with writing results", str(err))
        AzureActions.terminate_hdinsight_cluster(hdinsight_conf['resource_group_name'],
                                                 hdinsight_conf['full_cluster_name'])
        sys.exit(1)
