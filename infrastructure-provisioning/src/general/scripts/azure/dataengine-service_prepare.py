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
#from Crypto.PublicKey import RSA
#from fabric import *
from azure.mgmt.hdinsight.models import *
from azure.mgmt.core import *
from azure.common import *
from azure.core import *
from datalab.actions_lib import *


CLUSTER_NAME = 'hdinsight-test'
# The name of your existing Resource Group
RESOURCE_GROUP_NAME = 'dlab-resource-group'
# Choose a region. i.e. "East US 2".
LOCATION = 'West US 2'
# Cluster login username
CLUSTER_LOGIN_USER_NAME = 'datalab-user'
# (SSH) user username
SSH_USER_NAME = 'datalab-user'
# Cluster admin password
PASSWORD = ''
# The name of blob storage account
STORAGE_ACCOUNT_NAME = 'hdinsight'
# Blob storage account key
STORAGE_ACCOUNT_KEY = ''
# Blob storage account container name
CONTAINER_NAME = 'hdinsight'
# Blob Storage endpoint suffix.
BLOB_ENDPOINT_SUFFIX = '.blob.core.windows.net'

def create_cluster_parameters(LOCATION, CLUSTER_LOGIN_USER_NAME, PASSWORD, SSH_USER_NAME):

    # Returns cluster parameters

    return ClusterCreateParametersExtended(
        location=LOCATION,
        tags={},
        properties=ClusterCreateProperties(
            cluster_version="4.0",
            os_type=OSType.linux,
            tier=Tier.standard,
            cluster_definition=ClusterDefinition(
                kind="Spark",
                configurations={
                    "gateway": {
                        "restAuthCredential.isEnabled": "true",
                        "restAuthCredential.username": CLUSTER_LOGIN_USER_NAME,
                        "restAuthCredential.password": PASSWORD
                    }
                }
            ),
            compute_profile=ComputeProfile(
                roles=[
                    Role(
                        name="headnode",
                        target_instance_count=2,
                        hardware_profile=HardwareProfile(vm_size="Standard_A4_v2"),
                        os_profile=OsProfile(
                            linux_operating_system_profile=LinuxOperatingSystemProfile(
                                username=SSH_USER_NAME,
                                password=PASSWORD
                            )
                        )
                    ),
                    Role(
                        name="workernode",
                        target_instance_count=2,
                        hardware_profile=HardwareProfile(vm_size="Standard_A4_v2"),
                        os_profile=OsProfile(
                            linux_operating_system_profile=LinuxOperatingSystemProfile(
                                username=SSH_USER_NAME,
                                password=PASSWORD
                            )
                        )
                    )
                ]
            ),
            storage_profile=StorageProfile(
                storageaccounts=[
                    StorageAccount(
                        name=STORAGE_ACCOUNT_NAME + ".blob.core.windows.net",
                        key=STORAGE_ACCOUNT_KEY,
                        container=CONTAINER_NAME.lower(),
                        is_default=True
                    )
                ]
            )
        )
    )

if __name__ == "__main__":
    #params = create_cluster_parameters()
    #create_hdinsight_cluster(RESOURCE_GROUP_NAME,CLUSTER_NAME, params)
    
    try:
        AzureMeta = datalab.meta_lib.AzureMeta()
        AzureActions = datalab.actions_lib.AzureActions()
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

        hdinsight_conf['service_base_name'] = (os.environ['conf_service_base_name'])
        hdinsight_conf['edge_user_name'] = (os.environ['edge_user_name'])
        hdinsight_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        hdinsight_conf['project_tag'] = hdinsight_conf['project_name']
        hdinsight_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        hdinsight_conf['endpoint_tag'] = hdinsight_conf['endpoint_name']
        hdinsight_conf['key_name'] = os.environ['conf_key_name']
        hdinsight_conf['key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        hdinsight_conf['zone'] = os.environ['gcp_zone']
        hdinsight_conf['resource_group_name'] = os.environ['azure_resource_group_name']
        hdinsight_conf['region'] = os.environ['azure_region']
        data_engine['vpc_name'] = os.environ['azure_vpc_name']
        data_engine['private_subnet_name'] = '{}-{}-{}-subnet'.format(data_engine['service_base_name'],
                                                                      data_engine['project_name'],
                                                                      data_engine['endpoint_name'])
        data_engine['private_subnet_cidr'] = AzureMeta.get_subnet(data_engine['resource_group_name'],
                                                                  data_engine['vpc_name'],
                                                                  data_engine['private_subnet_name']).address_prefix
        data_engine['cluster_name'] = '{}-{}-{}-des-{}'.format(data_engine['service_base_name'],
                                                              data_engine['project_name'],
                                                              data_engine['endpoint_name'],
                                                              data_engine['computational_name'])



        hdinsight_conf['subnet'] = '{0}-{1}-{2}-subnet'.format(hdinsight_conf['service_base_name'],
                                                              hdinsight_conf['project_name'],
                                                              hdinsight_conf['endpoint_name'])
        hdinsight_conf['cluster_name'] = '{0}-{1}-{2}-des-{3}'.format(hdinsight_conf['service_base_name'],
                                                                     hdinsight_conf['project_name'],
                                                                     hdinsight_conf['endpoint_name'],
                                                                     hdinsight_conf['computational_name'])
        hdinsight_conf['cluster_tag'] = '{0}-{1}-{2}-ps'.format(hdinsight_conf['service_base_name'],
                                                               hdinsight_conf['project_name'],
                                                               hdinsight_conf['endpoint_name'])
        hdinsight_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(hdinsight_conf['service_base_name'],
                                                                   hdinsight_conf['project_name'],
                                                                   hdinsight_conf['endpoint_name'])

        hdinsight_conf['edge_instance_hostname'] = '{0}-{1}-{2}-edge'.format(hdinsight_conf['service_base_name'],
                                                                            hdinsight_conf['project_name'],
                                                                            hdinsight_conf['endpoint_name'])
        hdinsight_conf['datalab_ssh_user'] = os.environ['conf_os_user']
    except Exception as err:
        datalab.fab.append_result("Failed to generate variables dictionary. Exception:" + str(err))
        sys.exit(1)

    try:
        logging.info('[Creating HDInsight Cluster]')
        params = "--region {0} --bucket {1} --params '{2}'".format(hdinsight_conf['region'],
                                                                   hdinsight_conf['bucket_name'],
                                                                   json.dumps(hdinsight_cluster))

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
