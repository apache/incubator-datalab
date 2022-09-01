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
import json
import sys
from datalab.actions_lib import *
from datalab.meta_lib import *
from datalab.logger import logging
from fabric import *
from azure.mgmt.hdinsight.models import *
from azure.mgmt.core import *
from azure.common import *
from azure.core import *

parser = argparse.ArgumentParser()
parser.add_argument('--resource_group_name', type=str, help='')
parser.add_argument('--cluster_name', type=str, help='')
parser.add_argument('--cluster_version', type=str, help='')
parser.add_argument('--location', type=str, help='')
parser.add_argument('--master_instance_type', type=str, help='')
parser.add_argument('--worker_instance_type', type=str, help='')
parser.add_argument('--worker_count', type=str, help='')
parser.add_argument('--storage_account_name', type=str, help='')
parser.add_argument('--storage_account_key', type=str, help='')
parser.add_argument('--container_name', type=str, help='')
parser.add_argument('--tags', type=str, help='')
parser.add_argument('--public_key', type=str, help='')
args = parser.parse_args()

def build_hdinsight_cluster(resource_group_name, cluster_name, params):
    logging.info("Will be created cluster: {}".format(cluster_name))
    return datalab.actions_lib.AzureActions().create_hdinsight_cluster(resource_group_name, cluster_name, params)

def create_cluster_parameters(location, tags, cluster_version, cluster_login_username, password, master_instance_type,
                              worker_count, worker_instance_type, storage_account_name, storage_account_key,
                              container_name, public_key):

    # Returns cluster parameters

    return ClusterCreateParametersExtended(
        location=location,
        tags=tags,
        properties=ClusterCreateProperties(
            cluster_version=cluster_version,
            os_type=OSType.linux,
            tier=Tier.standard,
            cluster_definition=ClusterDefinition(
                kind="Spark",
                configurations={
                    "gateway": {
                        "restAuthCredential.isEnabled": "true",
                        "restAuthCredential.username": cluster_login_username,
                        "restAuthCredential.password": password
                    }
                }
            ),
            compute_profile=ComputeProfile(
                roles=[
                    Role(
                        name="headnode",
                        target_instance_count=2,
                        hardware_profile=HardwareProfile(vm_size=master_instance_type),
                        os_profile=OsProfile(
                            linux_operating_system_profile=LinuxOperatingSystemProfile(
                                username=cluster_login_username,
                                ssh_profile={
                                    "publicKeys": [
                                        {"certificateData": public_key}
                                    ]
                                }
                            )
                        )
                    ),
                    Role(
                        name="workernode",
                        target_instance_count=int(worker_count),
                        hardware_profile=HardwareProfile(vm_size=worker_instance_type),
                        os_profile=OsProfile(
                            linux_operating_system_profile=LinuxOperatingSystemProfile(
                                username=cluster_login_username,
                                ssh_profile={
                                    "publicKeys": [
                                        {"certificateData": public_key}
                                    ]
                                }
                            )
                        )
                    )
                ]
            ),
            storage_profile=StorageProfile(
                storageaccounts=[
                    StorageAccount(
                        name=storage_account_name + ".blob.core.windows.net",
                        key=storage_account_key,
                        container=container_name.lower(),
                        is_default=True
                    )
                ]
            )
        )
    )

##############
# Run script #
##############

if __name__ == "__main__":
    parser.print_help()
    password = ''
    params = create_cluster_parameters(args.location, json.loads(args.tags), args.cluster_version, 'datalab-user',
                                       password, args.master_instance_type, args.worker_count,
                                       args.worker_instance_type, args.storage_account_name, args.storage_account_key,
                                       args.container_name, args.public_key)
    build_hdinsight_cluster(args.resource_group_name, args.cluster_name, params)

    logfile = '{}_creation.log'.format(args.cluster_name)
    logpath = '/response/' + logfile
    out = open(logpath, 'w')
    out.close()

    sys.exit(0)
