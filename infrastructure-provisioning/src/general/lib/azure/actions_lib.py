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
from azure.mgmt.authorization import AuthorizationManagementClient
from azure.mgmt.compute import ComputeManagementClient
from azure.mgmt.resource import ResourceManagementClient
from azure.mgmt.network import NetworkManagementClient
from azure.mgmt.storage import StorageManagementClient
from azure.storage.blob import BlockBlobService
from azure.mgmt.datalake.store import DataLakeStoreAccountManagementClient
from azure.datalake.store import core, lib
from azure.graphrbac import GraphRbacManagementClient
from azure.common.credentials import ServicePrincipalCredentials
import azure.common.exceptions as AzureExceptions
from fabric.api import *
from fabric.contrib.files import exists
import urllib2
import meta_lib
import logging
import traceback
import sys, time
import os, json
import dlab.fab
import dlab.common_lib


class AzureActions:
    def __init__(self):
        os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
        self.compute_client = get_client_from_auth_file(ComputeManagementClient)
        self.resource_client = get_client_from_auth_file(ResourceManagementClient)
        self.network_client = get_client_from_auth_file(NetworkManagementClient)
        self.storage_client = get_client_from_auth_file(StorageManagementClient)
        self.datalake_client = get_client_from_auth_file(DataLakeStoreAccountManagementClient)
        self.authorization_client = get_client_from_auth_file(AuthorizationManagementClient)
        self.sp_creds = json.loads(open(os.environ['AZURE_AUTH_LOCATION']).read())
        self.dl_filesystem_creds = lib.auth(tenant_id=json.dumps(self.sp_creds['tenantId']).replace('"', ''),
                                            client_secret=json.dumps(self.sp_creds['clientSecret']).replace('"', ''),
                                            client_id=json.dumps(self.sp_creds['clientId']).replace('"', ''),
                                            resource='https://datalake.azure.net/')

    def create_resource_group(self, resource_group_name, region):
        try:
            result = self.resource_client.resource_groups.create_or_update(
                resource_group_name,
                {
                    'location': region,
                    'tags': {
                        'Name': resource_group_name
                    }
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
                    'tags': {
                        'Name': vpc_name
                    },
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

    def create_security_group(self, resource_group_name, network_security_group_name, region, tags, list_rules):
        try:
            result = self.network_client.network_security_groups.create_or_update(
                resource_group_name,
                network_security_group_name,
                {
                    'location': region,
                    'tags': tags,
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

    def create_datalake_store(self, resource_group_name, datalake_name, region, tags):
        try:
            result = self.datalake_client.account.create(
                resource_group_name,
                datalake_name,
                {
                    "location": region,
                    "tags": tags,
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
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            result = datalake_client.mkdir(dir_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to create Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def chown_datalake_directory(self, datalake_name, dir_name, ad_user='', ad_group=''):
        try:
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            if ad_user and ad_group:
                result = datalake_client.chown(dir_name, owner=ad_user, group=ad_group)
            elif ad_user and not ad_group:
                result = datalake_client.chown(dir_name, owner=ad_user)
            elif not ad_user and ad_group:
                result = datalake_client.chown(dir_name, group=ad_group)
            return result
        except Exception as err:
            logging.info(
                "Unable to chown Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to chown Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def chmod_datalake_directory(self, datalake_name, dir_name, mod):
        try:
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            result = datalake_client.chmod(dir_name, mod)
            return result
        except Exception as err:
            logging.info(
                "Unable to chmod Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to chmod Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def set_user_permissions_to_datalake_directory(self, datalake_name, dir_name, ad_user, mod='rwx'):
        try:
            acl_specification = 'user:{}:{}'.format(ad_user, mod)
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            result = datalake_client.modify_acl_entries(path=dir_name, acl_spec=acl_specification)
            return result
        except Exception as err:
            logging.info(
                "Unable to set user permission to Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to set user permission to Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def unset_user_permissions_to_datalake_directory(self, datalake_name, dir_name, ad_user):
        try:
            acl_specification = 'user:{}'.format(ad_user)
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            result = datalake_client.remove_acl_entries(path=dir_name, acl_spec=acl_specification)
            return result
        except Exception as err:
            logging.info(
                "Unable to unset user permission to Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to unset user permission to Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_datalake_directory(self, datalake_name, dir_name):
        try:
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            result = datalake_client.rm(dir_name, recursive=True)
            return result
        except Exception as err:
            logging.info(
                "Unable to delete Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to delete Data Lake directory",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_storage_account(self, resource_group_name, account_name, region, tags):
        try:
            result = self.storage_client.storage_accounts.create(
                resource_group_name,
                account_name,
                {
                    "sku": {"name": "Standard_LRS"},
                    "kind": "BlobStorage",
                    "location":  region,
                    "tags": tags,
                    "access_tier": "Hot",
                    "encryption": {
                        "services": {"blob": {"enabled": True}}
                    }
                }
            ).wait()
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

    def create_static_public_ip(self, resource_group_name, ip_name, region, instance_name, tags):
        try:
            self.network_client.public_ip_addresses.create_or_update(
                resource_group_name,
                ip_name,
                {
                    "location": region,
                    'tags': tags,
                    "public_ip_allocation_method": "static",
                    "public_ip_address_version": "IPv4",
                    "dns_settings": {
                        "domain_name_label": "host-" + instance_name.lower()
                    }
                }
            ).wait()
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
            ).wait()
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
                        image_full_name, tags, user_name='', create_option='fromImage', disk_id='',
                        instance_storage_account_type='Premium_LRS', image_type='default'):
        if image_type == 'pre-configured':
            image_id = meta_lib.AzureMeta().get_image(resource_group_name, image_full_name).id
        else:
            image_name = image_full_name.split('_')
            publisher = image_name[0]
            offer = image_name[1]
            sku = image_name[2]
        try:
            if instance_type == 'ssn':
                parameters = {
                    'location': region,
                    'tags': tags,
                    'hardware_profile': {
                        'vm_size': instance_size
                    },
                    'storage_profile': {
                        'image_reference': {
                            'publisher': publisher,
                            'offer': offer,
                            'sku': sku,
                            'version': 'latest'
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-ssn-disk0'.format(service_base_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'tags': tags,
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type,
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
                        'tags': tags,
                        'hardware_profile': {
                            'vm_size': instance_size
                        },
                        'storage_profile': {
                            'image_reference': {
                                'publisher': publisher,
                                'offer': offer,
                                'sku': sku,
                                'version': 'latest'
                            },
                            'os_disk': {
                                'os_type': 'Linux',
                                'name': '{}-{}-edge-disk0'.format(service_base_name, user_name),
                                'create_option': create_option,
                                'disk_size_gb': int(primary_disk_size),
                                'tags': tags,
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
                        'tags': tags,
                        'hardware_profile': {
                            'vm_size': instance_size
                        },
                        'storage_profile': {
                            'os_disk': {
                                'os_type': 'Linux',
                                'name': '{}-{}-edge-disk0'.format(service_base_name, user_name),
                                'create_option': create_option,
                                'disk_size_gb': int(primary_disk_size),
                                'tags': tags,
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
                if image_type == 'default':
                    storage_profile = {
                        'image_reference': {
                            'publisher': publisher,
                            'offer': offer,
                            'sku': sku,
                            'version': 'latest'
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-disk0'.format(instance_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'tags': tags,
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        },
                        'data_disks': [
                            {
                                'lun': 1,
                                'name': '{}-disk1'.format(instance_name),
                                'create_option': 'empty',
                                'disk_size_gb': 32,
                                'tags': {
                                    'Name': '{}-disk1'.format(instance_name)
                                },
                                'managed_disk': {
                                    'storage_account_type': instance_storage_account_type
                                }
                            }
                        ]
                    }
                elif image_type == 'pre-configured':
                    storage_profile = {
                        'image_reference': {
                            'id': image_id
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-disk0'.format(instance_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'tags': tags,
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        }
                    }
                parameters = {
                    'location': region,
                    'tags': tags,
                    'hardware_profile': {
                        'vm_size': instance_size
                    },
                    'storage_profile': storage_profile,
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
                if image_type == 'pre-configured':
                    storage_profile = {
                        'image_reference': {
                            'id': image_id
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-disk0'.format(instance_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'tags': tags,
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        }
                    }
                elif image_type == 'default':
                    storage_profile = {
                        'image_reference': {
                            'publisher': publisher,
                            'offer': offer,
                            'sku': sku,
                            'version': 'latest'
                        },
                        'os_disk': {
                            'os_type': 'Linux',
                            'name': '{}-disk0'.format(instance_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'tags': tags,
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type
                            }
                        }
                    }
                parameters = {
                    'location': region,
                    'tags': tags,
                    'hardware_profile': {
                        'vm_size': instance_size
                    },
                    'storage_profile': storage_profile,
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
            ).wait()
            AzureActions().tag_disks(resource_group_name, instance_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to create instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def tag_disks(self, resource_group_name, instance_name):
        postfix_list = ['-volume-primary', '-volume-secondary', '-volume-tertiary']
        disk_list = meta_lib.AzureMeta().get_vm_disks(resource_group_name, instance_name)
        for inx, disk in enumerate(disk_list):
            tags_copy = disk.tags.copy()
            tags_copy['Name'] = tags_copy['Name'] + postfix_list[inx]
            disk.tags = tags_copy
            self.compute_client.disks.create_or_update(resource_group_name, disk.name, disk)

    def stop_instance(self, resource_group_name, instance_name):
        try:
            result = self.compute_client.virtual_machines.deallocate(resource_group_name, instance_name).wait()
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
            result = self.compute_client.virtual_machines.start(resource_group_name, instance_name).wait()
            return result
        except Exception as err:
            logging.info(
                "Unable to start instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to start instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def set_tag_to_instance(self, resource_group_name, instance_name, tags):
        try:
            instance_parameters = self.compute_client.virtual_machines.get(resource_group_name, instance_name)
            instance_parameters.tags = tags
            result = self.compute_client.virtual_machines.create_or_update(resource_group_name, instance_name,
                                                                           instance_parameters)
            return result
        except Exception as err:
            logging.info(
                "Unable to set instance tags: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to set instance tags",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_instance(self, resource_group_name, instance_name):
        try:
            result = self.compute_client.virtual_machines.delete(resource_group_name, instance_name).wait()
            print("Instance {} has been removed".format(instance_name))
            # Removing instance disks
            disk_names = []
            resource_group_disks = self.compute_client.disks.list_by_resource_group(resource_group_name)
            for disk in resource_group_disks:
                if instance_name in disk.name:
                    disk_names.append(disk.name)
            for i in disk_names:
                self.remove_disk(resource_group_name, i)
                print("Disk {} has been removed".format(i))
            # Removing public static IP address and network interfaces
            network_interface_name = instance_name + '-nif'
            for j in meta_lib.AzureMeta().get_network_interface(resource_group_name,
                                                                network_interface_name).ip_configurations:
                self.delete_network_if(resource_group_name, network_interface_name)
                print("Network interface {} has been removed".format(network_interface_name))
                if j.public_ip_address:
                    static_ip_name = j.public_ip_address.id.split('/')[-1]
                    self.delete_static_public_ip(resource_group_name, static_ip_name)
                    print("Static IP address {} has been removed".format(static_ip_name))
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
            result = self.compute_client.disks.delete(resource_group_name, disk_name).wait()
            return result
        except Exception as err:
            logging.info(
                "Unable to remove disk: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove disk",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_network_if(self, resource_group_name, vpc_name, subnet_name, interface_name, region, security_group_name,
                          tags, public_ip_name="None"):
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
                    "tags": tags,
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
            result = self.network_client.network_interfaces.delete(resource_group_name, interface_name).wait()
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
                        print("Wasn't able to find Livy server for this dataengine!")
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
                        print("Interpreter with ID: {0} and name: {1} will be removed from zeppelin!".
                              format(interpreter['id'], interpreter['name']))
                        request = urllib2.Request(zeppelin_url + interpreter['id'], data='')
                        request.get_method = lambda: 'DELETE'
                        url = opener.open(request)
                        print(url.read())
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
                dlab.fab.remove_rstudio_dataengines_kernel(cluster_name, os_user)
            sudo('rm -rf  /opt/' + cluster_name + '/')
            print("Notebook's {} kernels were removed".format(env.hosts))
        except Exception as err:
            logging.info("Unable to remove kernels on Notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Unable to remove kernels on Notebook",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_image_from_instance(self, resource_group_name, instance_name, region, image_name, tags):
        try:
            instance_id = meta_lib.AzureMeta().get_instance(resource_group_name, instance_name).id
            self.compute_client.virtual_machines.deallocate(resource_group_name, instance_name).wait()
            self.compute_client.virtual_machines.generalize(resource_group_name, instance_name)
            if not meta_lib.AzureMeta().get_image(resource_group_name, image_name):
                self.compute_client.images.create_or_update(resource_group_name, image_name, parameters={
                    "location": region,
                    "tags": json.loads(tags),
                    "source_virtual_machine": {
                        "id": instance_id
                    }
                }).wait()
            AzureActions().remove_instance(resource_group_name, instance_name)
        except Exception as err:
            logging.info(
                "Unable to create image: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create image",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_image(self, resource_group_name, image_name):
        try:
            return self.compute_client.images.delete(resource_group_name, image_name)
        except Exception as err:
            logging.info(
                "Unable to remove image: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove image",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def ensure_local_jars(os_user, jars_dir):
    if not exists('/home/{}/.ensure_dir/local_jars_ensured'.format(os_user)):
        try:
            hadoop_version = sudo("ls /opt/spark/jars/hadoop-common* | sed -n 's/.*\([0-9]\.[0-9]\.[0-9]\).*/\\1/p'")
            print("Downloading local jars for Azure")
            sudo('mkdir -p {}'.format(jars_dir))
            if os.environ['azure_datalake_enable'] == 'false':
                sudo('wget http://central.maven.org/maven2/org/apache/hadoop/hadoop-azure/{0}/hadoop-azure-{0}.jar -O \
                                 {1}hadoop-azure-{0}.jar'.format(hadoop_version, jars_dir))
                sudo('wget http://central.maven.org/maven2/com/microsoft/azure/azure-storage/{0}/azure-storage-{0}.jar \
                    -O {1}azure-storage-{0}.jar'.format('2.2.0', jars_dir))
            else:
                sudo('wget http://central.maven.org/maven2/org/apache/hadoop/hadoop-azure/{0}/hadoop-azure-{0}.jar -O \
                                 {1}hadoop-azure-{0}.jar'.format('3.0.0', jars_dir))
                sudo('wget http://central.maven.org/maven2/com/microsoft/azure/azure-storage/{0}/azure-storage-{0}.jar \
                                    -O {1}azure-storage-{0}.jar'.format('6.1.0', jars_dir))
                sudo('wget http://central.maven.org/maven2/com/microsoft/azure/azure-data-lake-store-sdk/{0}/azure-data-lake-store-sdk-{0}.jar \
                    -O {1}azure-data-lake-store-sdk-{0}.jar'.format('2.2.3', jars_dir))
                sudo('wget http://central.maven.org/maven2/org/apache/hadoop/hadoop-azure-datalake/{0}/hadoop-azure-datalake-{0}.jar \
                    -O {1}hadoop-azure-datalake-{0}.jar'.format('3.0.0', jars_dir))
            if os.environ['application'] == 'tensor' or os.environ['application'] == 'deeplearning':
                sudo('wget https://dl.bintray.com/spark-packages/maven/tapanalyticstoolkit/spark-tensorflow-connector/{0}/spark-tensorflow-connector-{0}.jar \
                     -O {1}spark-tensorflow-connector-{0}.jar'.format('1.0.0-s_2.11', jars_dir))
            sudo('touch /home/{}/.ensure_dir/local_jars_ensured'.format(os_user))
        except Exception as err:
            logging.info(
                "Unable to download local jars: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to download local jars",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def configure_local_spark(os_user, jars_dir, region, templates_dir, memory_type='driver'):
    try:
        user_storage_account_tag = os.environ['conf_service_base_name'] + '-' + (os.environ['edge_user_name']).\
            replace('_', '-') + '-storage'
        shared_storage_account_tag = os.environ['conf_service_base_name'] + '-shared-storage'
        for storage_account in meta_lib.AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if user_storage_account_tag == storage_account.tags["Name"]:
                user_storage_account_name = storage_account.name
                user_storage_account_key = meta_lib.AzureMeta().list_storage_keys(os.environ['azure_resource_group_name'],
                                                                                  user_storage_account_name)[0]
            if shared_storage_account_tag == storage_account.tags["Name"]:
                shared_storage_account_name = storage_account.name
                shared_storage_account_key = meta_lib.AzureMeta().list_storage_keys(os.environ['azure_resource_group_name'],
                                                                                    shared_storage_account_name)[0]
        if os.environ['azure_datalake_enable'] == 'false':
            put(templates_dir + 'core-site-storage.xml', '/tmp/core-site.xml')
        else:
            put(templates_dir + 'core-site-datalake.xml', '/tmp/core-site.xml')
        sudo('sed -i "s|USER_STORAGE_ACCOUNT|{}|g" /tmp/core-site.xml'.format(user_storage_account_name))
        sudo('sed -i "s|SHARED_STORAGE_ACCOUNT|{}|g" /tmp/core-site.xml'.format(shared_storage_account_name))
        sudo('sed -i "s|USER_ACCOUNT_KEY|{}|g" /tmp/core-site.xml'.format(user_storage_account_key))
        sudo('sed -i "s|SHARED_ACCOUNT_KEY|{}|g" /tmp/core-site.xml'.format(shared_storage_account_key))
        if os.environ['azure_datalake_enable'] == 'true':
            client_id = os.environ['azure_client_id']
            refresh_token = os.environ['azure_user_refresh_token']
            sudo('sed -i "s|CLIENT_ID|{}|g" /tmp/core-site.xml'.format(client_id))
            sudo('sed -i "s|REFRESH_TOKEN|{}|g" /tmp/core-site.xml'.format(refresh_token))
        if os.environ['azure_datalake_enable'] == 'false':
            sudo('rm -f /opt/spark/conf/core-site.xml')
            sudo('mv /tmp/core-site.xml /opt/spark/conf/core-site.xml')
        else:
            sudo('rm -f /opt/hadoop/etc/hadoop/core-site.xml')
            sudo('mv /tmp/core-site.xml /opt/hadoop/etc/hadoop/core-site.xml')
        if not exists('/home/{}/.ensure_dir/local_spark_configured'.format(os_user)):
            put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
            sudo("jar_list=`find {} -name '*.jar' | tr '\\n' ','` ; echo \"spark.jars   $jar_list\" >> \
                  /tmp/notebook_spark-defaults_local.conf".format(jars_dir))
            sudo('\cp /tmp/notebook_spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/{}/.ensure_dir/local_spark_configured'.format(os_user))
    except Exception as err:
        logging.info(
            "Unable to configure Spark: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
        append_result(str({"error": "Unable to configure Spark",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                               file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
    try:
        if memory_type == 'driver':
            spark_memory = dlab.fab.get_spark_memory()
            sudo('sed -i "/spark.*.memory/d" /opt/spark/conf/spark-defaults.conf')
            sudo('echo "spark.{0}.memory {1}m" >> /opt/spark/conf/spark-defaults.conf'.format(memory_type, spark_memory))
    except:
        sys.exit(1)


def configure_dataengine_spark(cluster_name, jars_dir, cluster_dir, region, datalake_enabled):
    local("jar_list=`find {0} -name '*.jar' | tr '\\n' ','` ; echo \"spark.jars   $jar_list\" >> \
          /tmp/{1}/notebook_spark-defaults_local.conf".format(jars_dir, cluster_name))
    local('mv /tmp/{0}/notebook_spark-defaults_local.conf  {1}spark/conf/spark-defaults.conf'.format(cluster_name, cluster_dir))
    if datalake_enabled == 'false':
        local('cp /opt/spark/conf/core-site.xml {}spark/conf/'.format(cluster_dir))
    else:
        local('cp -f /opt/hadoop/etc/hadoop/core-site.xml {}hadoop/etc/hadoop/core-site.xml'.format(cluster_dir))


def remount_azure_disk(creds=False, os_user='', hostname='', keyfile=''):
    if creds:
        env['connection_attempts'] = 100
        env.key_filename = [keyfile]
        env.host_string = os_user + '@' + hostname
    sudo('sed -i "/azure_resource-part1/ s|/mnt|/media|g" /etc/fstab')
    sudo('grep "azure_resource-part1" /etc/fstab > /dev/null &&  umount -f /mnt/ || true')
    sudo('mount -a')


def prepare_vm_for_image(creds=False, os_user='', hostname='', keyfile=''):
    if creds:
        env['connection_attempts'] = 100
        env.key_filename = [keyfile]
        env.host_string = os_user + '@' + hostname
    sudo('waagent -deprovision -force')


def prepare_disk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/disk_ensured'):
        try:
            remount_azure_disk()
            disk_name = sudo("lsblk | grep disk | awk '{print $1}' | sort | tail -n 1")
            with settings(warn_only=True):
                sudo('umount -l /dev/{}1'.format(disk_name))
            sudo('''bash -c 'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/{}' '''.format(disk_name))
            sudo('mkfs.ext4 -F /dev/{}1'.format(disk_name))
            sudo('mount /dev/{}1 /opt/'.format(disk_name))
            sudo(''' bash -c "echo '/dev/{}1 /opt/ ext4 errors=remount-ro 0 1' >> /etc/fstab" '''.format(disk_name))
            sudo('touch /home/' + os_user + '/.ensure_dir/disk_ensured')
        except:
            sys.exit(1)


def ensure_local_spark(os_user, spark_link, spark_version, hadoop_version, local_spark_path):
    if not exists('/home/' + os_user + '/.ensure_dir/local_spark_ensured'):
        try:
            if os.environ['azure_datalake_enable'] == 'false':
                sudo('wget ' + spark_link + ' -O /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
                sudo('tar -zxvf /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
                sudo('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + local_spark_path)
                sudo('chown -R ' + os_user + ':' + os_user + ' ' + local_spark_path)
                sudo('touch /home/' + os_user + '/.ensure_dir/local_spark_ensured')
            else:
                # Downloading Spark without Hadoop
                sudo('wget https://archive.apache.org/dist/spark/spark-{0}/spark-{0}-bin-without-hadoop.tgz -O /tmp/spark-{0}-bin-without-hadoop.tgz'
                     .format(spark_version))
                sudo('tar -zxvf /tmp/spark-{}-bin-without-hadoop.tgz -C /opt/'.format(spark_version))
                sudo('mv /opt/spark-{}-bin-without-hadoop {}'.format(spark_version, local_spark_path))
                sudo('chown -R {0}:{0} {1}'.format(os_user, local_spark_path))
                # Downloading Hadoop
                hadoop_version = '3.0.0'
                sudo('wget https://archive.apache.org/dist/hadoop/common/hadoop-{0}/hadoop-{0}.tar.gz -O /tmp/hadoop-{0}.tar.gz'
                     .format(hadoop_version))
                sudo('tar -zxvf /tmp/hadoop-{0}.tar.gz -C /opt/'.format(hadoop_version))
                sudo('mv /opt/hadoop-{0} /opt/hadoop/'.format(hadoop_version))
                sudo('chown -R {0}:{0} /opt/hadoop/'.format(os_user))
                # Configuring Hadoop and Spark
                java_path = dlab.common_lib.find_java_path_remote()
                sudo('echo "export JAVA_HOME={}" >> /opt/hadoop/etc/hadoop/hadoop-env.sh'.format(java_path))
                sudo("""echo 'export HADOOP_CLASSPATH="$HADOOP_HOME/share/hadoop/tools/lib/*"' >> /opt/hadoop/etc/hadoop/hadoop-env.sh""")
                sudo('echo "export HADOOP_HOME=/opt/hadoop/" >> /opt/spark/conf/spark-env.sh')
                sudo('echo "export SPARK_HOME=/opt/spark/" >> /opt/spark/conf/spark-env.sh')
                spark_dist_classpath = sudo('/opt/hadoop/bin/hadoop classpath')
                sudo('echo "export SPARK_DIST_CLASSPATH={}" >> /opt/spark/conf/spark-env.sh'.format(
                    spark_dist_classpath))
                sudo('touch /home/{}/.ensure_dir/local_spark_ensured'.format(os_user))
        except:
            sys.exit(1)


def install_dataengine_spark(cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, os_user, datalake_enabled):
    try:
        if datalake_enabled == 'false':
            local('wget ' + spark_link + ' -O /tmp/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
            local('tar -zxvf /tmp/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
            local('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + cluster_dir + 'spark/')
            local('chown -R ' + os_user + ':' + os_user + ' ' + cluster_dir + 'spark/')
        else:
            # Downloading Spark without Hadoop
            local('wget https://archive.apache.org/dist/spark/spark-{0}/spark-{0}-bin-without-hadoop.tgz -O /tmp/{1}/spark-{0}-bin-without-hadoop.tgz'
                 .format(spark_version, cluster_name))
            local('tar -zxvf /tmp/' + cluster_name + '/spark-{}-bin-without-hadoop.tgz -C /opt/'.format(spark_version))
            local('mv /opt/spark-{}-bin-without-hadoop {}spark/'.format(spark_version, cluster_dir))
            local('chown -R {0}:{0} {1}/spark/'.format(os_user, cluster_dir))
            # Downloading Hadoop
            hadoop_version = '3.0.0'
            local('wget https://archive.apache.org/dist/hadoop/common/hadoop-{0}/hadoop-{0}.tar.gz -O /tmp/{1}/hadoop-{0}.tar.gz'
                 .format(hadoop_version, cluster_name))
            local('tar -zxvf /tmp/' + cluster_name + '/hadoop-{0}.tar.gz -C /opt/'.format(hadoop_version))
            local('mv /opt/hadoop-{0} {1}hadoop/'.format(hadoop_version, cluster_dir))
            local('chown -R {0}:{0} {1}hadoop/'.format(os_user, cluster_dir))
            # Configuring Hadoop and Spark
            java_path = dlab.common_lib.find_java_path_local()
            local('echo "export JAVA_HOME={}" >> {}hadoop/etc/hadoop/hadoop-env.sh'.format(java_path, cluster_dir))
            local("""echo 'export HADOOP_CLASSPATH="$HADOOP_HOME/share/hadoop/tools/lib/*"' >> {}hadoop/etc/hadoop/hadoop-env.sh""".format(cluster_dir))
            local('echo "export HADOOP_HOME={0}hadoop/" >> {0}spark/conf/spark-env.sh'.format(cluster_dir))
            local('echo "export SPARK_HOME={0}spark/" >> {0}spark/conf/spark-env.sh'.format(cluster_dir))
            spark_dist_classpath = local('{}hadoop/bin/hadoop classpath'.format(cluster_dir), capture=True)
            local('echo "export SPARK_DIST_CLASSPATH={}" >> {}spark/conf/spark-env.sh'.format(
                spark_dist_classpath, cluster_dir))
    except:
        sys.exit(1)
