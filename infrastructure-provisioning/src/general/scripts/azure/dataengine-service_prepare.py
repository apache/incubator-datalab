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
import datalab.logger
import multiprocessing
import os
import sys
import traceback
import subprocess
import Crypto.PublicKey
import fabric
import azure.mgmt.hdinsight.models
from azure.mgmt.hdinsight.models import *
from azure.mgmt.core import *
from azure.common import *
from azure.core import *
from datalab.actions_lib import *

if __name__ == "__main__":
    try:
        AzureMeta = datalab.meta_lib.AzureMeta()
        logging.info('Generating infrastructure names and tags')
        hdinsight_conf = dict()
        if 'exploratory_name' in os.environ:
            hdinsight_conf['exploratory_name'] = os.environ['exploratory_name'].replace('_', '-').lower()
        else:
            hdinsight_conf['exploratory_name'] = ''
        if 'computational_name' in os.environ:
            hdinsight_conf['computational_name'] = os.environ['computational_name'].replace('_', '-').lower()
        else:
            hdinsight_conf['computational_name'] = ''
        hdinsight_conf['hdinsight_worker_count'] = int(os.environ['dataengine_instance_count']) - 2
        hdinsight_conf['service_base_name'] = (os.environ['conf_service_base_name'])
        hdinsight_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        hdinsight_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        hdinsight_conf['key_name'] = os.environ['conf_key_name']
        hdinsight_conf['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        hdinsight_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        hdinsight_conf['region'] = os.environ['azure_region']
        hdinsight_conf['tag_name'] = hdinsight_conf['service_base_name'] + '-tag'
        hdinsight_conf['cluster_name'] = '{}-{}-{}-des-{}'.format(hdinsight_conf['service_base_name'],
                                                                  hdinsight_conf['project_name'],
                                                                  hdinsight_conf['endpoint_name'],
                                                                  hdinsight_conf['computational_name'])

        hdinsight_conf['cluster_tags'] = {
            "name": hdinsight_conf['cluster_name'],
            "sbn": hdinsight_conf['service_base_name'],
            "notebook_name": os.environ['notebook_instance_name'],
            "product": "datalab",
            "computational_name": hdinsight_conf['computational_name'],
            "project": hdinsight_conf['project_name'],
            "endpoint": hdinsight_conf['endpoint_name']
        }

        hdinsight_conf['custom_tag'] = json.loads(os.environ['tags'].replace("'", '"'))['custom_tag']
        if hdinsight_conf['custom_tag']:
            hdinsight_conf['cluster_tags']['custom_tag'] = hdinsight_conf['custom_tag']

        hdinsight_conf['release_label'] = os.environ['azure_hdinsight_version']
        key = RSA.importKey(open(hdinsight_conf['key_path'], 'rb').read())
        ssh_admin_pubkey = key.publickey().exportKey("OpenSSH").decode('UTF-8')
        hdinsight_conf['container_name'] = ('{0}-{1}-{2}-{3}-bucket'.format(hdinsight_conf['service_base_name'],
                                                                            hdinsight_conf['project_name'],
                                                                            hdinsight_conf['endpoint_name'],
                                                                            hdinsight_conf['cluster_name'])).lower()
        hdinsight_conf['storage_account_name_tag'] = ('{0}-{1}-{2}-{3}-bucket'.format(hdinsight_conf['service_base_name'],
                                                                                      hdinsight_conf['project_name'],
                                                                                      hdinsight_conf['endpoint_name'],
                                                                                      hdinsight_conf['cluster_name']
                                                                                      )).lower()
        hdinsight_conf['storage_account_tags'] = {"Name": hdinsight_conf['storage_account_name_tag'],
                                                  "SBN": hdinsight_conf['service_base_name'],
                                                  "project_tag": hdinsight_conf['project_name'],
                                                  "endpoint_tag": hdinsight_conf['endpoint_name'],
                                                  os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value'],
                                                  hdinsight_conf['tag_name']: hdinsight_conf['storage_account_name_tag']}
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary. Exception:" + str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE STORAGE ACCOUNT AND CONTAINERS]')

        params = "--container_name {} --account_tags '{}' --resource_group_name {} --region {}". \
            format(hdinsight_conf['container_name'], json.dumps(hdinsight_conf['storage_account_tags']),
                   hdinsight_conf['resource_group_name'], hdinsight_conf['region'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_storage_account', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create storage account.", str(err))
        for storage_account in AzureMeta.list_storage_accounts(hdinsight_conf['resource_group_name']):
            if hdinsight_conf['storage_account_name_tag'] == storage_account.tags["Name"]:
                AzureActions.remove_storage_account(hdinsight_conf['resource_group_name'], storage_account.name)
        sys.exit(1)

    try:
        logging.info('[Creating HDInsight Cluster]')
        for storage_account in AzureMeta.list_storage_accounts(hdinsight_conf['resource_group_name']):
            if hdinsight_conf['storage_account_name_tag'] == storage_account.tags["Name"]:
                hdinsight_conf['storage_account_name'] = storage_account.name
        hdinsight_conf['storage_account_key'] = AzureMeta.list_storage_keys(
            hdinsight_conf['resource_group_name'], hdinsight_conf['storage_account_name'])[0]
        params = "--resource_group_name {} --cluster_name {} " \
                 "--cluster_version {} --location {} " \
                 "--master_instance_type {} --worker_instance_type {} " \
                 "--worker_count {} --storage_account_name {} " \
                 "--storage_account_key {} --container_name {} " \
                 "--tags '{}' --public_key {}"\
            .format(hdinsight_conf['resource_group_name'], hdinsight_conf['cluster_name'],
                    hdinsight_conf['release_label'], hdinsight_conf['region'],
                    os.environ['azure_dataengine_master_size'], os.environ['azure_dataengine_slave_size'],
                    hdinsight_conf['hdinsight_worker_count'], hdinsight_conf['storage_account_name'],
                    hdinsight_conf['storage_account_key'], hdinsight_conf['container_name'],
                    json.dumps(hdinsight_conf['cluster_tags']), ssh_admin_pubkey)

        try:
            subprocess.run("~/scripts/{}.py {}".format('dataengine-service_create', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception

        keyfile_name = "/root/keys/{}.pem".format(hdinsight_conf['key_name'])
        subprocess.run('rm /response/.hdinsight_creating_{}'.format(os.environ['exploratory_name']), shell=True, check=True)
    except Exception as err:
        datalab.fab.append_result("Failed to create hdinsight Cluster.", str(err))
        subprocess.run('rm /response/.hdinsight_creating_{}'.format(os.environ['exploratory_name']), shell=True, check=True)
        sys.exit(1)
