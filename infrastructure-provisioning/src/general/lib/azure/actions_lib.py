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
from azure.mgmt.network import NetworkManagementClient
from azure.mgmt.storage import StorageManagementClient
from azure.mgmt.authorization import AuthorizationManagementClient
from azure.storage import CloudStorageAccount
from azure.storage.blob import BlockBlobService
from azure.storage.blob import ContentSettings
from azure.storage.blob.models import ContentSettings, PublicAccess
import azure.common.exceptions as AzureExceptions
import logging
import traceback
import sys, time
import os


class AzureActions:
    def __init__(self):
        if os.environ['conf_resource'] == 'ssn':
            os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
            self.compute_client = get_client_from_auth_file(ComputeManagementClient)
            self.resource_client = get_client_from_auth_file(ResourceManagementClient)
            self.network_client = get_client_from_auth_file(NetworkManagementClient)
            self.storage_client = get_client_from_auth_file(StorageManagementClient)
            self.authorization_client = get_client_from_auth_file(AuthorizationManagementClient)

    def create_resource_group(self, resource_group_name, region):
        try:
            result = self.resource_client.resource_groups.create_or_update(
                resource_group_name,
                {
                    'location': region
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create Resource Group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Resource Group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_resource_group(self, resource_group_name, region):
        try:
            result = self.resource_client.resource_groups.delete(
                resource_group_name,
                {
                    'location': region
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Resource Group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Resource Group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_vpc(self, resource_group_name, vpc_name, region, vpc_cidr):
        try:
            result = self.network_client.virtual_networks.create_or_update(
                resource_group_name,
                vpc_name,
                {
                    'location': region,
                    'address_space': {
                        'address_prefixes': [vpc_cidr]
                    }
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create Virtual Network: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Virtual Network",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_vpc(self, resource_group_name, vpc_name):
        try:
            result = self.network_client.virtual_networks.delete(
                resource_group_name,
                vpc_name
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Virtual Network: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Virtual Network",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_subnet(self, resource_group_name, vpc_name, subnet_name, subnet_cidr):
        try:
            result = self.network_client.subnets.create_or_update(
                resource_group_name,
                vpc_name,
                subnet_name,
                {
                    'address_prefix': subnet_cidr
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Subnet",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_subnet(self, resource_group_name, vpc_name, subnet_name):
        try:
            result = self.network_client.subnets.delete(
                resource_group_name,
                vpc_name,
                subnet_name
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Subnet",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_security_group(self, resource_group_name, network_security_group_name, list_rules, region):
        try:
            result = self.network_client.network_security_groups.create_or_update(
                resource_group_name,
                network_security_group_name,
                {
                    'location': region
                }
            )
            for rule in list_rules:
                self.network_client.security_rules.create_or_update(
                    resource_group_name,
                    network_security_group_name,
                    security_rule_name=rule['name'],
                    security_rule_parameters=rule
                )
            return result
        except Exception as err:
            logging.info(
                "Unable to create security group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create security group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_security_group(self, resource_group_name, network_security_group_name):
        try:
            result = self.network_client.network_security_groups.delete(
                resource_group_name,
                network_security_group_name
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to remove security group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove security group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_storage_account(self, resource_group_name, account_name, region):
        try:
            result = self.storage_client.storage_accounts.create(
                resource_group_name,
                account_name,
                {
                    "sku": {"name": "Standard_LRS"},
                    "kind": "BlobStorage",
                    "location":  region,
                    "access_tier": "Hot"
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create Storage account: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Storage account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_storage_account(self, resource_group_name, account_name):
        try:
            result = self.storage_client.storage_accounts.delete(
                resource_group_name,
                account_name
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Storage account: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Storage account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_blob_container(self, resource_group_name, account_name, container_name):
        try:
            secret_key = AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
            block_blob_service = BlockBlobService(account_name=account_name, account_key=secret_key)
            result = block_blob_service.create_container(container_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to create blob container: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create blob container",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def upload_to_container(self, resource_group_name, account_name, container_name, files):
        try:
            secret_key = AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
            block_blob_service = BlockBlobService(account_name=account_name, account_key=secret_key)
            for filename in files:
                block_blob_service.create_blob_from_path(container_name, filename, filename)
            return ''
        except Exception as err:
            logging.info(
                "Unable to upload files to container: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to upload files to container",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def download_from_container(self, resource_group_name, account_name, container_name, files):
        try:
            secret_key = AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
            block_blob_service = BlockBlobService(account_name=account_name, account_key=secret_key)
            for filename in files:
                block_blob_service.get_blob_to_path(container_name, filename, filename)
            return ''
        except Exception as err:
            logging.info(
                "Unable to download files from container: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to download files from container",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_static_ip(self, resource_group_name, ip_name, region):
        try:
            result = self.network_client.public_ip_addresses.create_or_update(
                resource_group_name,
                ip_name,
                {
                    "location": region,
                    "public_ip_allocation_method": "static",
                    "public_ip_address_version": "IPv4"
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def delete_static_ip(self, resource_group_name, ip_name):
        try:
            result = self.network_client.public_ip_addresses.delete(
                resource_group_name,
                ip_name
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to delete static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to delete static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)