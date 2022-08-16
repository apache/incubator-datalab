#!/usr/bin/python3

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

def create_cluster_parameters():

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
                        name=STORAGE_ACCOUNT_NAME + BLOB_ENDPOINT_SUFFIX,
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