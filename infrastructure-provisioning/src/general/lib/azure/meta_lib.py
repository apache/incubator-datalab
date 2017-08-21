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
from azure.storage.blob import BlockBlobService
from azure.storage import CloudStorageAccount
from azure.storage.blob.models import ContentSettings, PublicAccess
import azure.common.exceptions as AzureExceptions
import meta_lib
import logging
import traceback
import sys, time
import os


class AzureMeta:
    def __init__(self):
        os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
        self.compute_client = get_client_from_auth_file(ComputeManagementClient)
        self.resource_client = get_client_from_auth_file(ResourceManagementClient)
        self.network_client = get_client_from_auth_file(NetworkManagementClient)
        self.storage_client = get_client_from_auth_file(StorageManagementClient)
        self.authorization_client = get_client_from_auth_file(AuthorizationManagementClient)

    def get_resource_group(self, resource_group_name):
        try:
            result = self.resource_client.resource_groups.get(resource_group_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Resource Group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Resource Group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_vpc(self, resource_group_name, vpc_name):
        try:
            result = self.network_client.virtual_networks.get(resource_group_name, vpc_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Virtual Network: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Virtual Network",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_subnet(self, resource_group_name, vpc_name, subnet_name):
        try:
            result = self.network_client.subnets.get(resource_group_name, vpc_name, subnet_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Subnet",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_security_group(self, resource_group_name, network_security_group_name):
        try:
            result = self.network_client.network_security_groups.get(
                resource_group_name,
                network_security_group_name
            )
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get security group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get security group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_security_group_rule(self, resource_group_name, network_security_group_name, rule_name):
        try:
            result = self.network_client.security_rules.get(resource_group_name, network_security_group_name,
                                                            rule_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Security Group rule: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Security Group rule",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_subnets(self, resource_group_name, vpc_name):
        try:
            result = self.network_client.subnets.list(resource_group_name, vpc_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get list of Subnets: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get list of Subnets",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_instance(self, resource_group_name, instance_name):
        try:
            result = self.compute_client.virtual_machines.get(resource_group_name, instance_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_storage_account(self, resource_group_name, account_name):
        try:
            result = self.storage_client.storage_accounts.get_properties(
                resource_group_name,
                account_name
            )
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Storage account: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Storage account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def check_account_availability(self, account_name):
        try:
            result = self.storage_client.storage_accounts.check_name_availability(account_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to check Storage account name availability: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to check Storage account name availability",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_storage_keys(self, resource_group_name, account_name):
        try:
            result = []
            get_keys = self.storage_client.storage_accounts.list_keys(resource_group_name, account_name).keys
            for key in get_keys:
                result.append(key.value)
            return result
        except Exception as err:
            logging.info(
                "Unable to list storage account keys: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list storage account keys",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_container_content(self, resource_group_name, account_name, container_name):
        try:
            result = []
            secret_key = list_storage_keys(resource_group_name, account_name)[0]
            block_blob_service = BlockBlobService(account_name=account_name, account_key=secret_key)
            content = block_blob_service.list_blobs(container_name)
            for blob in content:
                result.append(blob.name)
            return result
        except Exception as err:
            logging.info(
                "Unable to list container content: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list container content",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_static_ip(self, resource_group_name, ip_name):
        try:
            result = self.network_client.public_ip_addresses.get(
                resource_group_name,
                ip_name
            )
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def check_free_ip(self, resource_group_name, vpc_name, ip_address):
        try:
            result = self.network_client.virtual_networks.check_ip_address_availability(
                resource_group_name,
                vpc_name,
                ip_address
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to check private ip: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to check private ip",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_instance_public_ip_address(self, resource_group_name, instance_name):
        try:
            instance = self.compute_client.virtual_machines.get(resource_group_name, instance_name)
            for i in instance.network_profile.network_interfaces:
                network_interface = self.network_client.network_interfaces.get(resource_group_name, i.id.split('/')[-1])
                for j in network_interface.ip_configurations:
                    public_ip_address = self.network_client.public_ip_addresses.get(resource_group_name,
                                                                          j.public_ip_address.id.split('/')[-1])
                    return public_ip_address.ip_address
        except Exception as err:
            logging.info(
                "Unable to get instance public IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instance public IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_instance_private_ip_address(self, resource_group_name, instance_name):
        try:
            instance = self.compute_client.virtual_machines.get(resource_group_name, instance_name)
            for i in instance.network_profile.network_interfaces:
                network_interface = self.network_client.network_interfaces.get(resource_group_name, i.id.split('/')[-1])
                for j in network_interface.ip_configurations:
                    return j.private_ip_address
        except Exception as err:
            logging.info(
                "Unable to get instance private IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instance private IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_network_interface(self, resource_group_name, network_interface_name):
        try:
            result = self.network_client.network_interfaces.get(resource_group_name, network_interface_name)
            return result
        except AzureExceptions.CloudError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Network interface: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Network interface",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_list_instance_statuses(self, resource_group_name, instance_name_list):
        data = []
        for instance_name in instance_name_list:
            host = {}
            try:
                request = self.compute_client.virtual_machines.get(resource_group_name, instance_name,
                                                                   expand='instanceView')
                host['id'] = instance_name
                try:
                    host['status'] = request.instance_view.statuses[1].display_status.split(' ')[1].replace("deallocat",
                                                                                                            "stopp")
                    data.append(host)
                except:
                    host['status'] = request.instance_view.statuses[0].display_status.lower()
                    data.append(host)
            except:
                host['resource_type'] = 'host'
                host['id'] = instance_name
                host['status'] = 'terminated'
                data.append(host)
        return data

