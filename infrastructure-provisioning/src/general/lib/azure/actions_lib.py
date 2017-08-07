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

from azure.common.client_factory import get_client_from_auth_file
from azure.mgmt.compute import ComputeManagementClient
from azure.mgmt.resource import ResourceManagementClient
from azure.mgmt.storage import StorageManagementClient
from azure.storage import CloudStorageAccount
from azure.storage.blob.models import ContentSettings, PublicAccess
import os


class AzureActions:
    def __init__(self):
        if os.environ['conf_resource'] == 'ssn':
            os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
            self.compute_client = get_client_from_auth_file(ComputeManagementClient)
            self.resource_client = get_client_from_auth_file(ResourceManagementClient)
            self.network_client = get_client_from_auth_file(NetworkManagementClient)

    def create_resource_group(self, resource_group_name, region):
        result = self.resource_client.resource_groups.create_or_update(
            resource_group_name,
            {
                'location': region
            }
        )
        return result

    def remove_resource_group(self, resource_group_name, region):
        result = self.resource_client.resource_groups.delete(
            resource_group_name,
            {
                'location': region
            }
        )
        return result

    def create_security_group(self, resource_group_name, network_security_group_name):
        result = self.network_client.resource_groups.create_or_update(
            resource_group_name, network_security_group_name)
        return result