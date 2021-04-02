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

import ast
import azure.common
import backoff
import datalab.common_lib
import datalab.fab
import datalab.meta_lib
import json
import logging
import os
import sys
import time
import traceback
import urllib3
import subprocess
from azure.common.client_factory import get_client_from_auth_file
from azure.datalake.store import core, lib
from azure.mgmt.authorization import AuthorizationManagementClient
from azure.mgmt.compute import ComputeManagementClient
from azure.mgmt.datalake.store import DataLakeStoreAccountManagementClient
from azure.mgmt.network import NetworkManagementClient
from azure.mgmt.resource import ResourceManagementClient
from azure.mgmt.storage import StorageManagementClient
from azure.storage.blob import BlockBlobService
from fabric import *
from patchwork.files import exists


class AzureActions:
    def __init__(self):
        os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
        self.compute_client = get_client_from_auth_file(ComputeManagementClient)
        self.resource_client = get_client_from_auth_file(ResourceManagementClient)
        self.network_client = get_client_from_auth_file(NetworkManagementClient)
        self.storage_client = get_client_from_auth_file(StorageManagementClient)
        self.datalake_client = get_client_from_auth_file(DataLakeStoreAccountManagementClient)
        #self.authorization_client = get_client_from_auth_file(AuthorizationManagementClient)
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
                        'Name': vpc_name,
                        os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value']
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

    def create_virtual_network_peerings(self, resource_group_name,
                                        virtual_network_name,
                                        virtual_network_peering_name,
                                        vnet_id
                                        ):
        try:
            result = self.network_client.virtual_network_peerings.create_or_update(
                resource_group_name,
                virtual_network_name,
                virtual_network_peering_name,
                {
                    "allow_virtual_network_access": True,
                    "allow_forwarded_traffic": True,
                    "remote_virtual_network": {
                        "id": vnet_id
                    }
                }
            ).wait(60)
            return result
        except Exception as err:
            logging.info(
                "Unable to create Virtual Network peering: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Virtual Network peering",
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

    def create_security_group(self, resource_group_name, network_security_group_name, region, tags, list_rules, preexisting_sg = False):
        try:
            result = ''
            if not preexisting_sg:
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
            if result:
                return result
        except Exception as err:
            logging.info(
                "Unable to create security group: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create security group",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_security_rules(self, network_security_group, resource_group, security_rule):
        try:
            result = self.network_client.security_rules.delete(
                network_security_group_name = network_security_group,
                resource_group_name = resource_group,
                security_rule_name = security_rule).wait()
            return result
        except Exception as err:
            logging.info(
                "Unable to remove security rule: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove security rule",
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

    def create_instance(self, region, instance_size, service_base_name, instance_name, datalab_ssh_user_name,
                        public_key,
                        network_interface_resource_id, resource_group_name, primary_disk_size, instance_type,
                        image_full_name, tags, project_name='', create_option='fromImage', disk_id='',
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
                            'name': '{}-ssn-volume-primary'.format(service_base_name),
                            'create_option': 'fromImage',
                            'disk_size_gb': int(primary_disk_size),
                            'tags': tags,
                            'managed_disk': {
                                'storage_account_type': instance_storage_account_type,
                            }
                        }
                    },
                    'os_profile': {
                        'computer_name': instance_name.replace('_', '-'),
                        'admin_username': datalab_ssh_user_name,
                        'linux_configuration': {
                            'disable_password_authentication': True,
                            'ssh': {
                                'public_keys': [{
                                    'path': '/home/{}/.ssh/authorized_keys'.format(datalab_ssh_user_name),
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
                                'name': '{}-{}-{}-edge-volume-primary'.format(service_base_name, project_name,
                                                                              os.environ['endpoint_name'].lower()),
                                'create_option': create_option,
                                'disk_size_gb': int(primary_disk_size),
                                'tags': tags,
                                'managed_disk': {
                                    'storage_account_type': instance_storage_account_type
                                }
                            }
                        },
                        'os_profile': {
                            'computer_name': instance_name.replace('_', '-'),
                            'admin_username': datalab_ssh_user_name,
                            'linux_configuration': {
                                'disable_password_authentication': True,
                                'ssh': {
                                    'public_keys': [{
                                        'path': '/home/{}/.ssh/authorized_keys'.format(datalab_ssh_user_name),
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
                                'name': '{}-{}-{}-edge-volume-primary'.format(service_base_name, project_name,
                                                                              os.environ['endpoint_name'].lower()),
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
                            'name': '{}-volume-primary'.format(instance_name),
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
                                'name': '{}-volume-secondary'.format(instance_name),
                                'create_option': 'empty',
                                'disk_size_gb': 32,
                                'tags': {
                                    'Name': '{}-volume-secondary'.format(instance_name)
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
                            'name': '{}-volume-primary'.format(instance_name),
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
                        'computer_name': instance_name.replace('_', '-'),
                        'admin_username': datalab_ssh_user_name,
                        'linux_configuration': {
                            'disable_password_authentication': True,
                            'ssh': {
                                'public_keys': [{
                                    'path': '/home/{}/.ssh/authorized_keys'.format(datalab_ssh_user_name),
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
                            'name': '{}-volume-primary'.format(instance_name),
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
                            'name': '{}-volume-primary'.format(instance_name),
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
                        'computer_name': instance_name.replace('_', '-'),
                        'admin_username': datalab_ssh_user_name,
                        'linux_configuration': {
                            'disable_password_authentication': True,
                            'ssh': {
                                'public_keys': [{
                                    'path': '/home/{}/.ssh/authorized_keys'.format(datalab_ssh_user_name),
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

    @backoff.on_exception(backoff.expo,
                          TypeError,
                          max_tries=5)
    def create_network_if(self, resource_group_name, vpc_name, subnet_name, interface_name, region, security_group_name,
                          tags, public_ip_name="None"):
        try:
            subnet_cidr = meta_lib.AzureMeta().get_subnet(resource_group_name, vpc_name, subnet_name).address_prefix.split('/')[0]
            private_ip = meta_lib.AzureMeta().check_free_ip(resource_group_name, vpc_name, subnet_cidr)
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
            ).wait()
            network_interface_id = meta_lib.AzureMeta().get_network_interface(
                resource_group_name,
                interface_name
            ).id
            return network_interface_id
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
            conn.sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(os_user, cluster_name))
            if exists(conn, '/home/{}/.ensure_dir/dataengine_{}_interpreter_ensured'.format(os_user, cluster_name)):
                if os.environ['notebook_multiple_clusters'] == 'true':
                    try:
                        livy_port = conn.sudo("cat /opt/" + cluster_name +
                                         "/livy/conf/livy.conf | grep livy.server.port | tail -n 1 | awk '{printf $3}'").stdout.replace('\n','')
                        process_number = conn.sudo("netstat -natp 2>/dev/null | grep ':" + livy_port +
                                              "' | awk '{print $7}' | sed 's|/.*||g'").stdout.replace('\n','')
                        conn.sudo('kill -9 ' + process_number)
                        conn.sudo('systemctl disable livy-server-' + livy_port)
                    except:
                        print("Wasn't able to find Livy server for this dataengine!")
                conn.sudo(
                    'sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
                conn.sudo("rm -rf /home/{}/.ensure_dir/dataengine_interpreter_ensure".format(os_user))
                zeppelin_url = 'http://' + private + ':8080/api/interpreter/setting/'
                opener = urllib3.build_opener(urllib3.ProxyHandler({}))
                req = opener.open(urllib3.Request(zeppelin_url))
                r_text = req.read()
                interpreter_json = json.loads(r_text)
                interpreter_prefix = cluster_name
                for interpreter in interpreter_json['body']:
                    if interpreter_prefix in interpreter['name']:
                        print("Interpreter with ID: {0} and name: {1} will be removed from zeppelin!".
                              format(interpreter['id'], interpreter['name']))
                        request = urllib3.Request(zeppelin_url + interpreter['id'], data='')
                        request.get_method = lambda: 'DELETE'
                        url = opener.open(request)
                        print(url.read())
                conn.sudo('chown ' + os_user + ':' + os_user + ' -R /opt/zeppelin/')
                conn.sudo('systemctl daemon-reload')
                conn.sudo("service zeppelin-notebook stop")
                conn.sudo("service zeppelin-notebook start")
                zeppelin_restarted = False
                while not zeppelin_restarted:
                    conn.sudo('sleep 5')
                    result = conn.sudo('nmap -p 8080 localhost | grep "closed" > /dev/null; echo $?').stdout
                    result = result[:1]
                    if result == '1':
                        zeppelin_restarted = True
                conn.sudo('sleep 5')
                conn.sudo('rm -rf /home/{}/.ensure_dir/dataengine_{}_interpreter_ensured'.format(os_user, cluster_name))
            if exists(conn, '/home/{}/.ensure_dir/rstudio_dataengine_ensured'.format(os_user)):
                datalab.fab.remove_rstudio_dataengines_kernel(os.environ['computational_name'], os_user)
            conn.sudo('rm -rf  /opt/' + cluster_name + '/')
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
    if not exists(conn,'/home/{}/.ensure_dir/local_jars_ensured'.format(os_user)):
        try:
            hadoop_version = conn.sudo("ls /opt/spark/jars/hadoop-common* | sed -n 's/.*\([0-9]\.[0-9]\.[0-9]\).*/\\1/p'").stdout.replace('\n','')
            print("Downloading local jars for Azure")
            conn.sudo('mkdir -p {}'.format(jars_dir))
            if os.environ['azure_datalake_enable'] == 'false':
                conn.sudo('wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-azure/{0}/hadoop-azure-{0}.jar -O \
                                 {1}hadoop-azure-{0}.jar'.format(hadoop_version, jars_dir))
                conn.sudo('wget https://repo1.maven.org/maven2/com/microsoft/azure/azure-storage/{0}/azure-storage-{0}.jar \
                    -O {1}azure-storage-{0}.jar'.format('2.2.0', jars_dir))
            else:
                conn.sudo('wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-azure/{0}/hadoop-azure-{0}.jar -O \
                                 {1}hadoop-azure-{0}.jar'.format('3.0.0', jars_dir))
                conn.sudo('wget https://repo1.maven.org/maven2/com/microsoft/azure/azure-storage/{0}/azure-storage-{0}.jar \
                                    -O {1}azure-storage-{0}.jar'.format('6.1.0', jars_dir))
                conn.sudo('wget https://repo1.maven.org/maven2/com/microsoft/azure/azure-data-lake-store-sdk/{0}/azure-data-lake-store-sdk-{0}.jar \
                    -O {1}azure-data-lake-store-sdk-{0}.jar'.format('2.2.3', jars_dir))
                conn.sudo('wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-azure-datalake/{0}/hadoop-azure-datalake-{0}.jar \
                    -O {1}hadoop-azure-datalake-{0}.jar'.format('3.0.0', jars_dir))
            if os.environ['application'] == 'tensor' or os.environ['application'] == 'deeplearning':
                conn.sudo('wget https://dl.bintray.com/spark-packages/maven/tapanalyticstoolkit/spark-tensorflow-connector/{0}/spark-tensorflow-connector-{0}.jar \
                     -O {1}spark-tensorflow-connector-{0}.jar'.format('1.0.0-s_2.11', jars_dir))
            conn.sudo('touch /home/{}/.ensure_dir/local_jars_ensured'.format(os_user))
        except Exception as err:
            logging.info(
                "Unable to download local jars: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to download local jars",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def configure_local_spark(jars_dir, templates_dir, memory_type='driver'):
    try:
        # Checking if spark.jars parameter was generated previously
        spark_jars_paths = None
        if exists(conn, '/opt/spark/conf/spark-defaults.conf'):
            try:
                spark_jars_paths = conn.sudo('cat /opt/spark/conf/spark-defaults.conf | grep -e "^spark.jars " ').stdout
            except:
                spark_jars_paths = None
        user_storage_account_tag = "{}-{}-{}-bucket".format(os.environ['conf_service_base_name'],
                                                            os.environ['project_name'].lower(),
                                                            os.environ['endpoint_name'].lower())
        shared_storage_account_tag = '{0}-{1}-shared-bucket'.format(os.environ['conf_service_base_name'],
                                                                    os.environ['endpoint_name'].lower())
        for storage_account in meta_lib.AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if user_storage_account_tag == storage_account.tags["Name"]:
                user_storage_account_name = storage_account.name
                user_storage_account_key = meta_lib.AzureMeta().list_storage_keys(
                    os.environ['azure_resource_group_name'], user_storage_account_name)[0]
            if shared_storage_account_tag == storage_account.tags["Name"]:
                shared_storage_account_name = storage_account.name
                shared_storage_account_key = meta_lib.AzureMeta().list_storage_keys(
                    os.environ['azure_resource_group_name'], shared_storage_account_name)[0]
        if os.environ['azure_datalake_enable'] == 'false':
            conn.put(templates_dir + 'core-site-storage.xml', '/tmp/core-site.xml')
        else:
            conn.put(templates_dir + 'core-site-datalake.xml', '/tmp/core-site.xml')
        conn.sudo('sed -i "s|USER_STORAGE_ACCOUNT|{}|g" /tmp/core-site.xml'.format(user_storage_account_name))
        conn.sudo('sed -i "s|SHARED_STORAGE_ACCOUNT|{}|g" /tmp/core-site.xml'.format(shared_storage_account_name))
        conn.sudo('sed -i "s|USER_ACCOUNT_KEY|{}|g" /tmp/core-site.xml'.format(user_storage_account_key))
        conn.sudo('sed -i "s|SHARED_ACCOUNT_KEY|{}|g" /tmp/core-site.xml'.format(shared_storage_account_key))
        if os.environ['azure_datalake_enable'] == 'true':
            client_id = os.environ['azure_application_id']
            refresh_token = os.environ['azure_user_refresh_token']
            conn.sudo('sed -i "s|CLIENT_ID|{}|g" /tmp/core-site.xml'.format(client_id))
            conn.sudo('sed -i "s|REFRESH_TOKEN|{}|g" /tmp/core-site.xml'.format(refresh_token))
        if os.environ['azure_datalake_enable'] == 'false':
            conn.sudo('rm -f /opt/spark/conf/core-site.xml')
            conn.sudo('mv /tmp/core-site.xml /opt/spark/conf/core-site.xml')
        else:
            conn.sudo('rm -f /opt/hadoop/etc/hadoop/core-site.xml')
            conn.sudo('mv /tmp/core-site.xml /opt/hadoop/etc/hadoop/core-site.xml')
        conn.put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
        conn.sudo("jar_list=`find {} -name '*.jar' | tr '\\n' ','` ; echo \"spark.jars   $jar_list\" >> \
              /tmp/notebook_spark-defaults_local.conf".format(jars_dir))
        conn.sudo('\cp -f /tmp/notebook_spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
        if memory_type == 'driver':
            spark_memory = datalab.fab.get_spark_memory()
            conn.sudo('sed -i "/spark.*.memory/d" /opt/spark/conf/spark-defaults.conf')
            conn.sudo('''bash -c 'echo "spark.{0}.memory {1}m" >> /opt/spark/conf/spark-defaults.conf' '''.format(memory_type,
                                                                                              spark_memory))
        if not exists(conn,'/opt/spark/conf/spark-env.sh'):
            conn.sudo('mv /opt/spark/conf/spark-env.sh.template /opt/spark/conf/spark-env.sh')
        java_home = conn.run("update-alternatives --query java | grep -o --color=never \'/.*/java-8.*/jre\'").stdout.splitlines()[0].replace('\n','')
        conn.sudo("echo 'export JAVA_HOME=\'{}\'' >> /opt/spark/conf/spark-env.sh".format(java_home))
        if 'spark_configurations' in os.environ:
            datalab_header = conn.sudo('cat /tmp/notebook_spark-defaults_local.conf | grep "^#"').stdout
            spark_configurations = ast.literal_eval(os.environ['spark_configurations'])
            new_spark_defaults = list()
            spark_defaults = conn.sudo('cat /opt/spark/conf/spark-defaults.conf').stdout
            current_spark_properties = spark_defaults.split('\n')
            for param in current_spark_properties:
                if param.split(' ')[0] != '#':
                    for config in spark_configurations:
                        if config['Classification'] == 'spark-defaults':
                            for property in config['Properties']:
                                if property == param.split(' ')[0]:
                                    param = property + ' ' + config['Properties'][property]
                                else:
                                    new_spark_defaults.append(property + ' ' + config['Properties'][property])
                    new_spark_defaults.append(param)
            new_spark_defaults = set(new_spark_defaults)
            conn.sudo("echo '{}' > /opt/spark/conf/spark-defaults.conf".format(datalab_header))
            for prop in new_spark_defaults:
                prop = prop.rstrip()
                conn.sudo('''bash -c 'echo "{}" >> /opt/spark/conf/spark-defaults.conf' '''.format(prop))
            conn.sudo('sed -i "/^\s*$/d" /opt/spark/conf/spark-defaults.conf')
            if spark_jars_paths:
                conn.sudo('''bash -c 'echo "{}" >> /opt/spark/conf/spark-defaults.conf' '''.format(spark_jars_paths))
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)


def configure_dataengine_spark(cluster_name, jars_dir, cluster_dir, datalake_enabled, spark_configs=''):
    subprocess.run("jar_list=`find {0} -name '*.jar' | tr '\\n' ',' | sed 's/,$//'` ; echo \"spark.jars $jar_list\" >> \
          /tmp/{1}/notebook_spark-defaults_local.conf".format(jars_dir, cluster_name), shell=True, check=True)
    if os.path.exists('{0}spark/conf/spark-defaults.conf'.format(cluster_dir)):
        additional_spark_properties = subprocess.run('diff --changed-group-format="%>" --unchanged-group-format="" '
                                            '/tmp/{0}/notebook_spark-defaults_local.conf '
                                            '{1}spark/conf/spark-defaults.conf | grep -v "^#"'.format(
                                             cluster_name, cluster_dir), capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        for property in additional_spark_properties.split('\n'):
            subprocess.run('echo "{0}" >> /tmp/{1}/notebook_spark-defaults_local.conf'.format(property, cluster_name), shell=True, check=True)
    if os.path.exists('{0}'.format(cluster_dir)):
        subprocess.run('cp -f /tmp/{0}/notebook_spark-defaults_local.conf  {1}spark/conf/spark-defaults.conf'.format(cluster_name,
                                                                                                        cluster_dir), shell=True, check=True)
    if datalake_enabled == 'false':
        subprocess.run('cp -f /opt/spark/conf/core-site.xml {}spark/conf/'.format(cluster_dir), shell=True, check=True)
    else:
        subprocess.run('cp -f /opt/hadoop/etc/hadoop/core-site.xml {}hadoop/etc/hadoop/core-site.xml'.format(cluster_dir), shell=True, check=True)
    if spark_configs and os.path.exists('{0}'.format(cluster_dir)):
        datalab_header = subprocess.run('cat /tmp/{0}/notebook_spark-defaults_local.conf | grep "^#"'.format(cluster_name),
                               capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        spark_configurations = ast.literal_eval(spark_configs)
        new_spark_defaults = list()
        spark_defaults = subprocess.run('cat {0}spark/conf/spark-defaults.conf'.format(cluster_dir), capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
        current_spark_properties = spark_defaults.split('\n')
        for param in current_spark_properties:
            if param.split(' ')[0] != '#':
                for config in spark_configurations:
                    if config['Classification'] == 'spark-defaults':
                        for property in config['Properties']:
                            if property == param.split(' ')[0]:
                                param = property + ' ' + config['Properties'][property]
                            else:
                                new_spark_defaults.append(property + ' ' + config['Properties'][property])
                new_spark_defaults.append(param)
        new_spark_defaults = set(new_spark_defaults)
        subprocess.run("echo '{0}' > {1}/spark/conf/spark-defaults.conf".format(datalab_header, cluster_dir), shell=True, check=True)
        for prop in new_spark_defaults:
            prop = prop.rstrip()
            subprocess.run('echo "{0}" >> {1}/spark/conf/spark-defaults.conf'.format(prop, cluster_dir), shell=True, check=True)
        subprocess.run('sed -i "/^\s*$/d" {0}/spark/conf/spark-defaults.conf'.format(cluster_dir), shell=True, check=True)


def remount_azure_disk(creds=False, os_user='', hostname='', keyfile=''):
    if creds:
        global conn
        conn = datalab.fab.init_datalab_connection(hostname, os_user, keyfile)
    conn.sudo('sed -i "/azure_resource-part1/ s|/mnt|/media|g" /etc/fstab')
    conn.sudo('grep "azure_resource-part1" /etc/fstab > /dev/null &&  umount -f /mnt/ || true')
    conn.sudo('mount -a')
    if creds:
        conn.close()


def prepare_vm_for_image(creds=False, os_user='', hostname='', keyfile=''):
    if creds:
        global conn
        conn = datalab.fab.init_datalab_connection(hostname, os_user, keyfile)
    conn.sudo('waagent -deprovision -force')
    if creds:
        conn.close()


def prepare_disk(os_user):
    if not exists(conn,'/home/' + os_user + '/.ensure_dir/disk_ensured'):
        try:
            allow = False
            counter = 0
            remount_azure_disk()
            disk_name = conn.sudo("lsblk | grep disk | awk '{print $1}' | sort | tail -n 1").stdout.replace('\n','')
            conn.sudo('umount -l /dev/{}1'.format(disk_name), warn=True)
            while not allow:
                if counter > 4:
                    print("Unable to prepare disk")
                    sys.exit(1)
                else:
                    conn.sudo('''bash -c 'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/{}' 2>&1 | tee /tmp/tee.tmp '''.format(
                        disk_name), warn_only=True)
                    out = conn.sudo('cat /tmp/tee.tmp').stdout
                    if 'Syncing disks' in out:
                        allow = True
                    elif 'The kernel still uses the old table.' in out:
                        if conn.sudo('partprobe'):
                            conn.sudo('reboot', warn=True)
                        allow = True
                    else:
                        counter += 1
                        time.sleep(5)
            conn.sudo('umount -l /dev/{}1'.format(disk_name), warn_only=True)
            conn.sudo('mkfs.ext4 -F /dev/{}1'.format(disk_name))
            conn.sudo('mount /dev/{}1 /opt/'.format(disk_name))
            conn.sudo(''' bash -c "echo '/dev/{}1 /opt/ ext4 errors=remount-ro 0 1' >> /etc/fstab" '''.format(
                disk_name))
            conn.sudo('touch /home/' + os_user + '/.ensure_dir/disk_ensured')
        except Exception as err:
            traceback.print_exc()
            print('Error:', str(err))
            sys.exit(1)


def ensure_local_spark(os_user, spark_link, spark_version, hadoop_version, local_spark_path):
    if not exists(conn,'/home/' + os_user + '/.ensure_dir/local_spark_ensured'):
        try:
            if os.environ['azure_datalake_enable'] == 'false':
                conn.sudo('wget ' + spark_link + ' -O /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz')
                conn.sudo('tar -zxvf /tmp/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/')
                conn.sudo('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + local_spark_path)
                conn.sudo('chown -R ' + os_user + ':' + os_user + ' ' + local_spark_path)
                conn.sudo('touch /home/' + os_user + '/.ensure_dir/local_spark_ensured')
            else:
                # Downloading Spark without Hadoop
                conn.sudo('wget https://archive.apache.org/dist/spark/spark-{0}/spark-{0}-bin-without-hadoop.tgz -O /tmp/spark-{0}-bin-without-hadoop.tgz'
                    .format(spark_version))
                conn.sudo('tar -zxvf /tmp/spark-{}-bin-without-hadoop.tgz -C /opt/'.format(spark_version))
                conn.sudo('mv /opt/spark-{}-bin-without-hadoop {}'.format(spark_version, local_spark_path))
                conn.sudo('chown -R {0}:{0} {1}'.format(os_user, local_spark_path))
                # Downloading Hadoop
                hadoop_version = '3.0.0'
                conn.sudo('wget https://archive.apache.org/dist/hadoop/common/hadoop-{0}/hadoop-{0}.tar.gz -O /tmp/hadoop-{0}.tar.gz'
                    .format(hadoop_version))
                conn.sudo('tar -zxvf /tmp/hadoop-{0}.tar.gz -C /opt/'.format(hadoop_version))
                conn.sudo('mv /opt/hadoop-{0} /opt/hadoop/'.format(hadoop_version))
                conn.sudo('chown -R {0}:{0} /opt/hadoop/'.format(os_user))
                # Configuring Hadoop and Spark
                java_path = datalab.common_lib.find_java_path_remote()
                conn.sudo('echo "export JAVA_HOME={}" >> /opt/hadoop/etc/hadoop/hadoop-env.sh'.format(java_path))
                conn.sudo("""echo 'export HADOOP_CLASSPATH="$HADOOP_HOME/share/hadoop/tools/lib/*"' >> /opt/hadoop/etc/hadoop/hadoop-env.sh""")
                conn.sudo('echo "export HADOOP_HOME=/opt/hadoop/" >> /opt/spark/conf/spark-env.sh')
                conn.sudo('echo "export SPARK_HOME=/opt/spark/" >> /opt/spark/conf/spark-env.sh')
                spark_dist_classpath = conn.sudo('/opt/hadoop/bin/hadoop classpath').stdout
                conn.sudo('echo "export SPARK_DIST_CLASSPATH={}" >> /opt/spark/conf/spark-env.sh'.format(
                    spark_dist_classpath))
                conn.sudo('touch /home/{}/.ensure_dir/local_spark_ensured'.format(os_user))
        except Exception as err:
            print('Error:', str(err))
            sys.exit(1)


def install_dataengine_spark(cluster_name, spark_link, spark_version, hadoop_version, cluster_dir, os_user, datalake_enabled):
    try:
        if datalake_enabled == 'false':
            subprocess.run('wget ' + spark_link + ' -O /tmp/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz', shell=True, check=True)
            subprocess.run('tar -zxvf /tmp/' + cluster_name + '/spark-' + spark_version + '-bin-hadoop' + hadoop_version + '.tgz -C /opt/', shell=True, check=True)
            subprocess.run('mv /opt/spark-' + spark_version + '-bin-hadoop' + hadoop_version + ' ' + cluster_dir + 'spark/', shell=True, check=True)
            subprocess.run('chown -R ' + os_user + ':' + os_user + ' ' + cluster_dir + 'spark/', shell=True, check=True)
        else:
            # Downloading Spark without Hadoop
            subprocess.run('wget https://archive.apache.org/dist/spark/spark-{0}/spark-{0}-bin-without-hadoop.tgz -O /tmp/{1}/spark-{0}-bin-without-hadoop.tgz'
                 .format(spark_version, cluster_name), shell=True, check=True)
            subprocess.run('tar -zxvf /tmp/' + cluster_name + '/spark-{}-bin-without-hadoop.tgz -C /opt/'.format(spark_version), shell=True, check=True)
            subprocess.run('mv /opt/spark-{}-bin-without-hadoop {}spark/'.format(spark_version, cluster_dir), shell=True, check=True)
            subprocess.run('chown -R {0}:{0} {1}/spark/'.format(os_user, cluster_dir), shell=True, check=True)
            # Downloading Hadoop
            hadoop_version = '3.0.0'
            subprocess.run('wget https://archive.apache.org/dist/hadoop/common/hadoop-{0}/hadoop-{0}.tar.gz -O /tmp/{1}/hadoop-{0}.tar.gz'
                 .format(hadoop_version, cluster_name), shell=True, check=True)
            subprocess.run('tar -zxvf /tmp/' + cluster_name + '/hadoop-{0}.tar.gz -C /opt/'.format(hadoop_version), shell=True, check=True)
            subprocess.run('mv /opt/hadoop-{0} {1}hadoop/'.format(hadoop_version, cluster_dir), shell=True, check=True)
            subprocess.run('chown -R {0}:{0} {1}hadoop/'.format(os_user, cluster_dir), shell=True, check=True)
            # Configuring Hadoop and Spark
            java_path = datalab.common_lib.find_java_path_local()
            subprocess.run('echo "export JAVA_HOME={}" >> {}hadoop/etc/hadoop/hadoop-env.sh'.format(java_path, cluster_dir), shell=True, check=True)
            subprocess.run("""echo 'export HADOOP_CLASSPATH="$HADOOP_HOME/share/hadoop/tools/lib/*"' >> {}hadoop/etc/hadoop/hadoop-env.sh""".format(cluster_dir), shell=True, check=True)
            subprocess.run('echo "export HADOOP_HOME={0}hadoop/" >> {0}spark/conf/spark-env.sh'.format(cluster_dir), shell=True, check=True)
            subprocess.run('echo "export SPARK_HOME={0}spark/" >> {0}spark/conf/spark-env.sh'.format(cluster_dir), shell=True, check=True)
            spark_dist_classpath = subprocess.run('{}hadoop/bin/hadoop classpath'.format(cluster_dir), capture_output=True, shell=True, check=True).stdout.decode('UTF-8').rstrip("\n\r")
            subprocess.run('echo "export SPARK_DIST_CLASSPATH={}" >> {}spark/conf/spark-env.sh'.format(
                spark_dist_classpath, cluster_dir), shell=True, check=True)
    except:
        sys.exit(1)


def find_des_jars(all_jars, des_path):
    try:
        # Use this method to filter cloud jars (see an example in aws method)
        return all_jars
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)
