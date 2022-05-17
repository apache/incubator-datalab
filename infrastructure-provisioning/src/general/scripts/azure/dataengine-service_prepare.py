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
from azure.mgmt.hdinsight.models import *



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
                        hardware_profile=HardwareProfile(vm_size="Standard_E8_V3"),
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
                        hardware_profile=HardwareProfile(vm_size="Standard_E8_V3"),
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