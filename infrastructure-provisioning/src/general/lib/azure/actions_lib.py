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
import azure.common
from azure.mgmt.compute import ComputeManagementClient
from azure.mgmt.resource import ResourceManagementClient
from azure.mgmt.network import NetworkManagementClient
from azure.mgmt.storage import StorageManagementClient
from azure.mgmt.authorization import AuthorizationManagementClient
from azure.mgmt.datalake.store import DataLakeStoreAccountManagementClient
from azure.datalake.store import core, lib, multithread
from azure.storage import CloudStorageAccount
from azure.storage import SharedAccessSignature
from azure.storage.blob import BlockBlobService
import azure.common.exceptions as AzureExceptions
from fabric.api import *
from fabric.contrib.files import exists
import urllib2
import meta_lib
import logging
import traceback
import sys, time
import os, json


class AzureActions:
    def __init__(self):
        os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
        self.compute_client = get_client_from_auth_file(ComputeManagementClient)
        self.resource_client = get_client_from_auth_file(ResourceManagementClient)
        self.network_client = get_client_from_auth_file(NetworkManagementClient)
        self.storage_client = get_client_from_auth_file(StorageManagementClient)
        self.datalake_client = get_client_from_auth_file(DataLakeStoreAccountManagementClient)
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

    def create_security_group(self, resource_group_name, network_security_group_name, region, list_rules):
        try:
            result = self.network_client.network_security_groups.create_or_update(
                resource_group_name,
                network_security_group_name,
                {
                    'location': region
                }
            ).wait()
            for rule in list_rules:
                self.network_client.security_rules.create_or_update(
                    resource_group_name,
                    network_security_group_name,
                    security_rule_name=rule['name'],
                    security_rule_parameters=rule
                ).wait()
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

    def create_datalake_store(self, resource_group_name, datalake_name, region):
        try:
            result = self.datalake_client.account.create(
                resource_group_name,
                datalake_name,
                {
                    "location": region,
                    "encryption_state": "Enabled",
                    "encryption_config": {
                        "type": "ServiceManaged"
                    }
                }
            ).wait()
            return result
        except Exception as err:
            logging.info(
                "Unable to create Data Lake store: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Data Lake store",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def delete_datalake_store(self, resource_group_name, datalake_name):
        try:
            result = self.datalake_client.account.delete(
                resource_group_name,
                datalake_name
            ).wait()
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Data Lake store: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Data Lake store",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_datalake_directory(self, datalake_name, dir_name):
        try:
            token = lib.auth(tenant_id='', client_secret='', client_id='')
            adlsFileSystemClient = core.AzureDLFileSystem(token, store_name=datalake_name)
            result = adlsFileSystemClient.mkdir(dir_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to create Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_datalake_directory(self, datalake_name, dir_name):
        try:
            token = lib.auth(tenant_id='', client_secret='', client_id='')
            adlsFileSystemClient = core.AzureDLFileSystem(token, store_name=datalake_name)
            result = adlsFileSystemClient.rm(dir_name, recursive=True)
            return result
        except Exception as err:
            logging.info(
                "Unable to create Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Data Lake directory",
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
                    "access_tier": "Hot",
                    "encryption": {
                        "services": {"blob": {"enabled": True}}
                    }
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
            secret_key = meta_lib.AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
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
            secret_key = meta_lib.AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
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
            secret_key = meta_lib.AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
            block_blob_service = BlockBlobService(account_name=account_name, account_key=secret_key)
            for filename in files:
                block_blob_service.get_blob_to_path(container_name, filename, filename)
            return ''
        except azure.common.AzureMissingResourceHttpError:
            return ''
        except Exception as err:
            logging.info(
                "Unable to download files from container: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to download files from container",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def generate_container_sas(self, resource_group_name, account_name, container_name, source_ip_range):
        try:
            secret_key = meta_lib.AzureMeta().list_storage_keys(resource_group_name, account_name)[0]
            sas = SharedAccessSignature(account_name=account_name, account_key=secret_key)
            result = sas.generate_container(container_name, permission='rw', ip=source_ip_range)
            return result
        except Exception as err:
            logging.info(
                "Unable to generate SAS for container: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to generate SAS for container",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_static_public_ip(self, resource_group_name, ip_name, region, instance_name):
        try:
            self.network_client.public_ip_addresses.create_or_update(
                resource_group_name,
                ip_name,
                {
                    "location": region,
                    "public_ip_allocation_method": "static",
                    "public_ip_address_version": "IPv4",
                    "dns_settings": {
                        "domain_name_label": instance_name
                    }
                }
            )
            while meta_lib.AzureMeta().get_static_ip(resource_group_name, ip_name).provisioning_state != 'Succeeded':
                time.sleep(5)
            return meta_lib.AzureMeta().get_static_ip(resource_group_name, ip_name).ip_address
        except Exception as err:
            logging.info(
                "Unable to create static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def delete_static_public_ip(self, resource_group_name, ip_name):
        try:
            result = self.network_client.public_ip_addresses.delete(
                resource_group_name,
                ip_name
            )
            while meta_lib.AzureMeta().get_static_ip(resource_group_name, ip_name):
                time.sleep(5)
            return result
        except Exception as err:
            logging.info(
                "Unable to delete static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to delete static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_instance(self, region, instance_size, service_base_name, instance_name, dlab_ssh_user_name, public_key,
                        network_interface_resource_id, resource_group_name, primary_disk_size, instance_type,
                        user_name='', create_option='fromImage', disk_id='', instance_storage_account_type='Premium_LRS'):
        try:
            if instance_type == 'ssn':
                parameters = {
                    'location': region,
                    'hardware_profile': {
                        'vm_size': instance_size
                    },
                    'storage_profile': {
                        'image_reference': {
                            'publisher': 'Canonical',
                            'offer': 'UbuntuServer',
                            'sku': '16.04-LTS',
                            'version': 'latest'
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-ssn-primary-disk'.format(service_base_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        }
                    },
                    'os_profile': {
                        'computer_name': instance_name,
                        'admin_username': dlab_ssh_user_name,
                        'linux_configuration': {
                            'disable_password_authentication': True,
                            'ssh': {
                                'public_keys': [{
                                    'path': '/home/{}/.ssh/authorized_keys'.format(dlab_ssh_user_name),
                                    'key_data': public_key
                                }]
                            }
                        }
                    },
                    'network_profile': {
                        'network_interfaces': [
                            {
                                'id': network_interface_resource_id
                            }
                        ]
                    }
                }
            elif instance_type == 'edge':
                if create_option == 'fromImage':
                    parameters = {
                        'location': region,
                        'hardware_profile': {
                            'vm_size': instance_size
                        },
                        'storage_profile': {
                            'image_reference': {
                                'publisher': 'Canonical',
                                'offer': 'UbuntuServer',
                                'sku': '16.04-LTS',
                                'version': 'latest'
                            },
                            'os_disk': {
                                'os_type': 'Linux',
                                'name': '{}-{}-edge-primary-disk'.format(service_base_name, user_name),
                                'create_option': create_option,
                                'disk_size_gb': int(primary_disk_size),
                                'managed_disk': {
                                    'storage_account_type': instance_storage_account_type
                                }
                            }
                        },
                        'os_profile': {
                            'computer_name': instance_name,
                            'admin_username': dlab_ssh_user_name,
                            'linux_configuration': {
                                'disable_password_authentication': True,
                                'ssh': {
                                    'public_keys': [{
                                        'path': '/home/{}/.ssh/authorized_keys'.format(dlab_ssh_user_name),
                                        'key_data': public_key
                                    }]
                                }
                            }
                        },
                        'network_profile': {
                            'network_interfaces': [
                                {
                                    'id': network_interface_resource_id
                                }
                            ]
                        }
                    }
                elif create_option == 'attach':
                    parameters = {
                        'location': region,
                        'hardware_profile': {
                            'vm_size': instance_size
                        },
                        'storage_profile': {
                            'os_disk': {
                                'os_type': 'Linux',
                                'name': '{}-{}-edge-primary-disk'.format(service_base_name, user_name),
                                'create_option': create_option,
                                'disk_size_gb': int(primary_disk_size),
                                'managed_disk': {
                                    'id': disk_id,
                                    'storage_account_type': instance_storage_account_type
                                }
                            }
                        },
                        'network_profile': {
                            'network_interfaces': [
                                {
                                    'id': network_interface_resource_id
                                }
                            ]
                        }
                    }
            elif instance_type == 'notebook':
                parameters = {
                    'location': region,
                    'hardware_profile': {
                        'vm_size': instance_size
                    },
                    'storage_profile': {
                        'image_reference': {
                            'publisher': 'Canonical',
                            'offer': 'UbuntuServer',
                            'sku': '16.04-LTS',
                            'version': 'latest'
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-primary-disk'.format(instance_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        },
                        'data_disks': [
                            {
                                'lun': 1,
                                'name': '{}-secondary-disk'.format(instance_name),
                                'create_option': 'empty',
                                'disk_size_gb': 32,
                                'managed_disk': {
                                    'storage_account_type': instance_storage_account_type
                                }
                            }
                        ]
                    },
                    'os_profile': {
                        'computer_name': instance_name,
                        'admin_username': dlab_ssh_user_name,
                        'linux_configuration': {
                            'disable_password_authentication': True,
                            'ssh': {
                                'public_keys': [{
                                    'path': '/home/{}/.ssh/authorized_keys'.format(dlab_ssh_user_name),
                                    'key_data': public_key
                                }]
                            }
                        }
                    },
                    'network_profile': {
                        'network_interfaces': [
                            {
                                'id': network_interface_resource_id
                            }
                        ]
                    }
                }
            elif instance_type == 'dataengine':
                parameters = {
                    'location': region,
                    'hardware_profile': {
                        'vm_size': instance_size
                    },
                    'storage_profile': {
                        'image_reference': {
                            'publisher': 'Canonical',
                            'offer': 'UbuntuServer',
                            'sku': '16.04-LTS',
                            'version': 'latest'
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-primary-disk'.format(instance_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        }
                    },
                    'os_profile': {
                        'computer_name': instance_name,
                        'admin_username': dlab_ssh_user_name,
                        'linux_configuration': {
                            'disable_password_authentication': True,
                            'ssh': {
                                'public_keys': [{
                                    'path': '/home/{}/.ssh/authorized_keys'.format(dlab_ssh_user_name),
                                    'key_data': public_key
                                }]
                            }
                        }
                    },
                    'network_profile': {
                        'network_interfaces': [
                            {
                                'id': network_interface_resource_id
                            }
                        ]
                    }
                }
            else:
                parameters = {}
            result = self.compute_client.virtual_machines.create_or_update(
                resource_group_name, instance_name, parameters
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def stop_instance(self, resource_group_name, instance_name):
        try:
            result = self.compute_client.virtual_machines.deallocate(resource_group_name, instance_name)
            while meta_lib.AzureMeta().get_instance(resource_group_name, instance_name).provisioning_state != "Succeeded":
                time.sleep(5)
                print "Instance {} is being deallocated...".format(instance_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to stop instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to stop instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def start_instance(self, resource_group_name, instance_name):
        try:
            result = self.compute_client.virtual_machines.start(resource_group_name, instance_name)
            while meta_lib.AzureMeta().get_instance(resource_group_name, instance_name).provisioning_state != "Succeeded":
                time.sleep(5)
                print "Instance {} is being started...".format(instance_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to start instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to start instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_instance(self, resource_group_name, instance_name):
        try:
            instance_parameters = meta_lib.AzureMeta().get_instance(resource_group_name, instance_name)
            result = self.compute_client.virtual_machines.delete(resource_group_name, instance_name)
            while meta_lib.AzureMeta().get_instance(resource_group_name, instance_name):
                time.sleep(5)
            print "Instance {} has been removed".format(instance_name)
            # Removing instance disks
            disk_names = []
            disk_names.append(instance_parameters.storage_profile.os_disk.name)
            if instance_parameters.storage_profile.data_disks:
                for i in instance_parameters.storage_profile.data_disks:
                    disk_names.append(i.name)
            for i in disk_names:
                self.remove_disk(resource_group_name, i)
                print "Disk {} has been removed".format(i)
            # Removing public static IP address and network interfaces
            for i in instance_parameters.network_profile.network_interfaces:
                network_interface_name = i.id.split('/')[-1]
                for j in meta_lib.AzureMeta().get_network_interface(resource_group_name,
                                                                    network_interface_name).ip_configurations:
                    self.delete_network_if(resource_group_name, network_interface_name)
                    print "Network interface {} has been removed".format(network_interface_name)
                    if j.public_ip_address:
                        static_ip_name = j.public_ip_address.id.split('/')[-1]
                        self.delete_static_public_ip(resource_group_name, static_ip_name)
                        print "Static IP address {} has been removed".format(static_ip_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to remove instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_disk(self, resource_group_name, disk_name):
        try:
            result = self.compute_client.disks.delete(resource_group_name, disk_name)
            while meta_lib.AzureMeta().get_disk(resource_group_name, disk_name):
                time.sleep(5)
            return result
        except Exception as err:
            logging.info(
                "Unable to remove disk: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove disk",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_network_if(self, resource_group_name, vpc_name, subnet_name, interface_name, region, security_group_name,
                          public_ip_name="None"):
        try:
            subnet_cidr = meta_lib.AzureMeta().get_subnet(resource_group_name, vpc_name, subnet_name).address_prefix.split('/')[0]
            private_ip = meta_lib.AzureMeta().check_free_ip(resource_group_name, vpc_name, subnet_cidr).available_ip_addresses[0]
            subnet_id = meta_lib.AzureMeta().get_subnet(resource_group_name, vpc_name, subnet_name).id
            security_group_id = meta_lib.AzureMeta().get_security_group(resource_group_name, security_group_name).id
            if public_ip_name == "None":
                ip_params = [{
                    "name": interface_name,
                    "private_ip_allocation_method": "Static",
                    "private_ip_address": private_ip,
                    "private_ip_address_version": "IPv4",
                    "subnet": {
                        "id": subnet_id
                    }
                }]
            else:
                public_ip_id = meta_lib.AzureMeta().get_static_ip(resource_group_name, public_ip_name).id
                ip_params = [{
                    "name": interface_name,
                    "private_ip_allocation_method": "Static",
                    "private_ip_address": private_ip,
                    "private_ip_address_version": "IPv4",
                    "public_ip_address": {
                        "id": public_ip_id
                    },
                    "subnet": {
                        "id": subnet_id
                    }
                }]
            result = self.network_client.network_interfaces.create_or_update(
                resource_group_name,
                interface_name,
                {
                    "location": region,
                    "network_security_group": {
                        "id": security_group_id
                    },
                    "ip_configurations": ip_params
                }
            )
            return result._operation.resource.id
        except Exception as err:
            logging.info(
                "Unable to create network interface: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create network interface",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def delete_network_if(self, resource_group_name, interface_name):
        try:
            result = self.network_client.network_interfaces.delete(resource_group_name, interface_name)
            while meta_lib.AzureMeta().get_network_interface(resource_group_name, interface_name):
                time.sleep(5)
            return result
        except Exception as err:
            logging.info(
                "Unable to delete network interface: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to delete network interface",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_dataengine_kernels(self, resource_group_name, notebook_name, os_user, key_path, cluster_name):
        try:
            private = meta_lib.AzureMeta().get_private_ip_address(resource_group_name, notebook_name)
            env.hosts = "{}".format(private)
            env.user = "{}".format(os_user)
            env.key_filename = "{}".format(key_path)
            env.host_string = env.user + "@" + env.hosts
            sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(os_user, cluster_name))
            if exists('/home/{}/.ensure_dir/dataengine_{}_interpreter_ensured'.format(os_user, cluster_name)):
                if os.environ['notebook_multiple_clusters'] == 'true':
                    try:
                        livy_port = sudo("cat /opt/" + cluster_name +
                                         "/livy/conf/livy.conf | grep livy.server.port | tail -n 1 | awk '{printf $3}'")
                        process_number = sudo("netstat -natp 2>/dev/null | grep ':" + livy_port +
                                              "' | awk '{print $7}' | sed 's|/.*||g'")
                        sudo('kill -9 ' + process_number)
                        sudo('systemctl disable livy-server-' + livy_port)
                    except:
                        print "Wasn't able to find Livy server for this EMR!"
                sudo(
                    'sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
                sudo("rm -rf /home/{}/.ensure_dir/dataengine_interpreter_ensure".format(os_user))
                zeppelin_url = 'http://' + private + ':8080/api/interpreter/setting/'
                opener = urllib2.build_opener(urllib2.ProxyHandler({}))
                req = opener.open(urllib2.Request(zeppelin_url))
                r_text = req.read()
                interpreter_json = json.loads(r_text)
                interpreter_prefix = cluster_name
                for interpreter in interpreter_json['body']:
                    if interpreter_prefix in interpreter['name']:
                        print "Interpreter with ID:", interpreter['id'], "and name:", interpreter['name'], \
                            "will be removed from zeppelin!"
                        request = urllib2.Request(zeppelin_url + interpreter['id'], data='')
                        request.get_method = lambda: 'DELETE'
                        url = opener.open(request)
                        print url.read()
                sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin/')
                sudo('systemctl daemon-reload')
                sudo("service zeppelin-notebook stop")
                sudo("service zeppelin-notebook start")
                zeppelin_restarted = False
                while not zeppelin_restarted:
                    sudo('sleep 5')
                    result = sudo('nmap -p 8080 localhost | grep "closed" > /dev/null; echo $?')
                    result = result[:1]
                    if result == '1':
                        zeppelin_restarted = True
                sudo('sleep 5')
                sudo('rm -rf /home/{}/.ensure_dir/dataengine_{}_interpreter_ensured'.format(os_user, cluster_name))
            if exists('/home/{}/.ensure_dir/rstudio_dataengine_ensured'.format(os_user)):
                sudo("sed -i '/" + cluster_name + "/d' /home/{}/.Renviron".format(os_user))
                sudo("sed -i '/" + cluster_name + "/d' /home/{}/.Rprofile".format(os_user))
                if not sudo("sed -n '/^SPARK_HOME/p' /home/{}/.Renviron".format(os_user)):
                    sudo(
                        "sed -i '1!G;h;$!d;' /home/{0}/.Renviron; sed -i '1,3s/#//;1!G;h;$!d' /home/{0}/.Renviron".
                            format(os_user))
                if not sudo("sed -n '/^master/p' /home/{}/.Rprofile".format(os_user)):
                    sudo(
                        "sed -i '1!G;h;$!d;' /home/{0}/.Rprofile; sed -i '1,3s/#//;1!G;h;$!d' /home/{0}/.Rprofile".
                            format(os_user))
                sudo("sed -i 's|/opt/" + cluster_name + "/spark//R/lib:||g' /home/{}/.bashrc".format(os_user))
            sudo('rm -rf  /opt/' + cluster_name + '/')
            print "Notebook's " + env.hosts + " kernels were removed"
        except Exception as err:
            logging.info("Unable to remove kernels on Notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Unable to remove kernels on Notebook",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_application(self, app_name):
        try:
            result = graphrbac_client.applications.create(
                {
                    "available_to_other_tenants": False,
                    "display_name": app_name,
                    "identifier_uris": ["http://{}".format(app_name)]
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create application: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create application",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def delete_application(self, application_object_id):
        try:
            graphrbac_client.applications.delete(application_object_id)
            return ''
        except Exception as err:
            logging.info(
                "Unable to delete application: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to delete application",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_service_principal(self, application_id):
        try:
            result = graphrbac_client.service_principals.create(
                {
                    "app_id": application_id,
                    "account_enabled": "True"
                }
            )
            return result
        except Exception as err:
            logging.info(
                "Unable to create service principal: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create service principal",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def ensure_local_jars(os_user, jars_dir, files_dir, region, templates_dir):
    if not exists('/home/{}/.ensure_dir/s3_kernel_ensured'.format(os_user)):
        try:
            user_container_name = (os.environ['conf_service_base_name'] + '-' + os.environ['edge_user_name'] + '-container').lower().replace('_', '-')
            shared_container_name = (os.environ['conf_service_base_name'] + '-shared-container').lower().replace('_', '-')
            user_storage_account_name = (os.environ['conf_service_base_name'] + os.environ['edge_user_name']).lower().replace('_', '').replace('-', '')
            ssn_storage_account_name = (os.environ['conf_service_base_name']).lower().replace('_', '').replace('-', '')
            source_ip_range = meta_lib.AzureMeta().get_subnet(os.environ['azure_resource_group_name'], os.environ['azure_vpc_name'],
                                                              os.environ['conf_service_base_name'] + '-' +
                                                              os.environ['edge_user_name'] + '-subnet').address_prefix
            user_container_sas = AzureActions().generate_container_sas(os.environ['azure_resource_group_name'],
                                                                       user_storage_account_name, user_container_name,
                                                                       source_ip_range).replace('&', '\&amp;')
            ssn_container_sas = AzureActions().generate_container_sas(os.environ['azure_resource_group_name'],
                                                                      ssn_storage_account_name, shared_container_name,
                                                                      source_ip_range).replace('&', '\&amp;')
            print "Downloading local jars for Azure"
            sudo('mkdir -p ' + jars_dir)
            sudo('wget http://central.maven.org/maven2/org/apache/hadoop/hadoop-azure/2.7.3/hadoop-azure-2.7.3.jar -O ' +
                 jars_dir + 'hadoop-azure-2.7.3.jar')
            sudo(
                'wget http://central.maven.org/maven2/com/microsoft/azure/azure-storage/5.5.0/azure-storage-5.5.0.jar -O ' +
                jars_dir + 'azure-storage-5.5.0.jar')
            put(templates_dir + 'core-site.xml', '/tmp/core-site.xml')
            sudo('sed -i "s|USER_CONTAINER|{}|g" /tmp/core-site.xml'.format(user_container_name))
            sudo('sed -i "s|USER_STORAGE_ACCOUNT|{}|g" /tmp/core-site.xml'.format(user_storage_account_name))
            sudo('sed -i "s|USER_CONTAINER_SAS|{}|g" /tmp/core-site.xml'.format(user_container_sas))
            sudo('sed -i "s|SHARED_CONTAINER|{}|g" /tmp/core-site.xml'.format(shared_container_name))
            sudo('sed -i "s|SSN_STORAGE_ACCOUNT|{}|g" /tmp/core-site.xml'.format(ssn_storage_account_name))
            sudo('sed -i "s|SSN_CONTAINER_SAS|{}|g" /tmp/core-site.xml'.format(ssn_container_sas))
            sudo('mv /tmp/core-site.xml /opt/spark/conf/core-site.xml')
            put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
            sudo('\cp /tmp/notebook_spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/{}/.ensure_dir/s3_kernel_ensured'.format(os_user))
        except:
            sys.exit(1)