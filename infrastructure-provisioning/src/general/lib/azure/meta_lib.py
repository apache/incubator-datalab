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

from azure.common.client_factory import get_client_from_auth_file
from azure.mgmt.compute import ComputeManagementClient
from azure.mgmt.resource import ResourceManagementClient
from azure.mgmt.network import NetworkManagementClient
from azure.mgmt.storage import StorageManagementClient
from azure.storage.blob import BlobServiceClient
from azure.mgmt.datalake.store import DataLakeStoreAccountManagementClient
from azure.datalake.store import core, lib
from azure.identity import ClientSecretCredential
from azure.core.exceptions import ResourceNotFoundError
from azure.mgmt.hdinsight import HDInsightManagementClient
import logging
import traceback
import sys
import os
import json


class AzureMeta:
    def __init__(self):
        os.environ['AZURE_AUTH_LOCATION'] = '/root/azure_auth.json'
        with open('/root/azure_auth.json') as json_file:
            json_dict = json.load(json_file)

        credential = ClientSecretCredential(
            tenant_id=json_dict["tenantId"],
            client_id=json_dict["clientId"],
            client_secret=json_dict["clientSecret"],
            authority=json_dict["activeDirectoryEndpointUrl"]
        )

        self.compute_client = ComputeManagementClient(
            credential,
            json_dict["subscriptionId"],
            base_url=json_dict["resourceManagerEndpointUrl"],
            credential_scopes=["{}/.default".format(json_dict["resourceManagerEndpointUrl"])]
        )
        self.resource_client = ResourceManagementClient(
            credential,
            json_dict["subscriptionId"],
            base_url=json_dict["resourceManagerEndpointUrl"],
            credential_scopes=["{}/.default".format(json_dict["resourceManagerEndpointUrl"])]
        )
        self.network_client = NetworkManagementClient(
            credential,
            json_dict["subscriptionId"],
            base_url=json_dict["resourceManagerEndpointUrl"],
            credential_scopes=["{}/.default".format(json_dict["resourceManagerEndpointUrl"])]
        )
        self.storage_client = StorageManagementClient(
            credential,
            json_dict["subscriptionId"],
            base_url=json_dict["resourceManagerEndpointUrl"],
            credential_scopes=["{}/.default".format(json_dict["resourceManagerEndpointUrl"])]
        )
        self.datalake_client = DataLakeStoreAccountManagementClient(
            credential,
            json_dict["subscriptionId"],
            base_url=json_dict["resourceManagerEndpointUrl"]
        )
        self.hdinsight_client = HDInsightManagementClient(
            credential,
            json_dict["subscriptionId"],
            base_url=json_dict["resourceManagerEndpointUrl"]
        )
        self.sp_creds = json.loads(open(os.environ['AZURE_AUTH_LOCATION']).read())
        self.dl_filesystem_creds = lib.auth(tenant_id=json.dumps(self.sp_creds['tenantId']).replace('"', ''),
                                            client_secret=json.dumps(self.sp_creds['clientSecret']).replace('"', ''),
                                            client_id=json.dumps(self.sp_creds['clientId']).replace('"', ''),
                                            resource='https://datalake.azure.net/')
        logger = logging.getLogger('azure')
        logger.setLevel(logging.ERROR)

    def get_resource_group(self, resource_group_name):
        try:
            result = self.resource_client.resource_groups.get(resource_group_name)
            return result
        except ResourceNotFoundError as err:
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
        except ResourceNotFoundError as err:
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
        except ResourceNotFoundError as err:
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
        except ResourceNotFoundError as err:
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
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Security Group rule: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Security Group rule",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_security_group_rules(self, resource_group_name, sg_name):
        try:
            result = self.network_client.security_rules.list(resource_group_name, sg_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get list of security group rules: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get list of rules",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_subnets(self, resource_group_name, vpc_name):
        try:
            result = self.network_client.subnets.list(resource_group_name, vpc_name)
            return result
        except ResourceNotFoundError as err:
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
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_instances_name_by_tag(self, resource_group_name, tag, value):
        try:
            list = []
            for vm in self.compute_client.virtual_machines.list(resource_group_name):
                if vm.tags.get(tag) == value:
                    list.append(vm.name)
            return list
        except Exception as err:
            logging.info(
                "Unable to get instances by tag name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instances",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_datalake(self, resource_group_name, datalake_name):
        try:
            result = self.datalake_client.account.get(
                resource_group_name,
                datalake_name
            )
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Data Lake account: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Data Lake account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_datalakes(self, resource_group_name):
        try:
            result = self.datalake_client.accounts.list_by_resource_group(resource_group_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to list Data Lake accounts: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list Data Lake accounts",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def verify_datalake_directory(self, datalake_name, dir_name):
        try:
            datalake_client = core.AzureDLFileSystem(self.dl_filesystem_creds, store_name=datalake_name)
            result = datalake_client.exists(dir_name)
            return result
        except Exception as err:
            logging.info(
                "Unable to verify Data Lake directory: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to verify Data Lake directory",
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
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Storage account: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Storage account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_storage_accounts(self, resource_group_name):
        try:
            result = self.storage_client.storage_accounts.list_by_resource_group(resource_group_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to list storage accounts: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list Data Lake accounts",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def check_account_availability(self, account_name):
        try:
            result = self.storage_client.storage_accounts.check_name_availability(
                { "name": account_name }
            )
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
            block_blob_service = BlobServiceClient(account_url="https://" + account_name + ".blob.core.windows.net/", credential=secret_key)
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
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_static_ips(self, resource_group_name):
        try:
            result = self.network_client.public_ip_addresses.list(resource_group_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to list static IP addresses: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list static IP addresses",
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
            if not result.available:
                return self.check_free_ip(resource_group_name, vpc_name, result.available_ip_addresses[0])
            if result.available:
                return ip_address
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

    def get_private_ip_address(self, resource_group_name, instance_name):
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
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Network interface: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Network interface",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_network_interfaces(self, resource_group_name):
        try:
            result = self.network_client.network_interfaces.list(resource_group_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to list Network interfaces: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable list Network interfaces",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_network_peering_status(self, resource_group_name,
                                   virtual_network_name,
                                   virtual_network_peering_name):
        try:
            result = self.network_client.virtual_network_peerings.get(resource_group_name,
                                                                        virtual_network_name,
                                                                        virtual_network_peering_name)
            return result.peering_state
        except Exception as err:
            logging.info(
                "Unable to get peering status: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get peering status", "error_message": str(
                err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_disk(self, resource_group_name, disk_name):
        try:
            result = self.compute_client.disks.get(resource_group_name, disk_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get instance Disk: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instance Disk",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_disks(self, resource_group_name):
        try:
            result = self.compute_client.disks.list_by_resource_group(resource_group_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to list instance Disks: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list instance Disks",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_list_instance_statuses(self, resource_group_name, instance_name_list):
        data = []
        for instance_name in instance_name_list:
            instance_name = instance_name['id']
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
                host['id'] = instance_name
                host['status'] = 'terminated'
                data.append(host)
        return data

    def get_image_statuses(self, resource_group_name, image_name_list):
        data = []
        for image_name in image_name_list:
            image_name = image_name['id']
            host = {}
            try:
                request = self.compute_client.images.get(resource_group_name, image_name)
                host['id'] = image_name
                if request.provisioning_state == 'Succeeded':
                    host['status'] = 'ACTIVE'
                elif request.provisioning_state == 'Deleting':
                    host['status'] = 'TERMINATING'
                elif request.provisioning_state == 'Canceled':
                    host['status'] = 'FAILED'
                elif request.provisioning_state == 'Creating':
                    host['status'] = 'CREATING'
                elif request.provisioning_state == 'Locked':
                    host['status'] = 'FAILED'
                data.append(host)
            except:
                host['id'] = image_name
                host['status'] = 'TERMINATED'
                data.append(host)
        return data


    def get_instance_status(self, resource_group_name, instance_name):
        try:
            request = self.compute_client.virtual_machines.get(resource_group_name, instance_name, expand='instanceView')
            try:
                status = request.instance_view.statuses[1].display_status.split(' ')[1].replace("deallocat", "stopp")
            except:
                status = request.instance_view.statuses[0].display_status.lower()
        except:
            status = 'terminated'
        return status

    def get_application(self, application_object_id):
        try:
            result = graphrbac_client.applications.get(application_object_id)
            return result
        except Exception as err:
            logging.info(
                "Unable to get application: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get application",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_image(self, resource_group_name, image_name):
        try:
            return self.compute_client.images.get(resource_group_name, image_name)
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get image: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get image",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_instance_image(self, resource_group_name, instance_name):
        try:
            instance = self.compute_client.virtual_machines.get(
                resource_group_name, instance_name)
            image = instance.storage_profile.image_reference
            if image.id == None:
                return ('default')
            image = image.id.split("/")
            image = image[-1]
            return (image)
        except Exception as err:
            logging.info(
                "Unable to get instance image: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get instance image",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_images(self):
        try:
            return self.compute_client.images.list()
        except Exception as err:
            logging.info(
                "Unable to get list images: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get list images",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_list_private_ip_by_conf_type_and_id(self, conf_type, instance_id):
        try:
            private_list_ip = []
            if conf_type == 'edge_node' or conf_type == 'exploratory':
                private_list_ip.append(AzureMeta().get_private_ip_address(os.environ['conf_service_base_name'], instance_id))
            elif conf_type == 'computational_resource':
                instance_list = AzureMeta().get_instances_name_by_tag(os.environ['conf_service_base_name'], 'Name', instance_id)
                for instance in instance_list:
                    private_list_ip.append(AzureMeta().get_private_ip_address(
                        os.environ['conf_service_base_name'], instance))
            return private_list_ip
        except Exception as err:
            logging.info(
                "Error getting private ip by conf_type and id: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error getting private ip by conf_type and id",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_vm_disks(self, resource_group_name, instance_name):
        try:
            disk_list = []
            virtual_machine = self.compute_client.virtual_machines.get(resource_group_name,
                                                                       instance_name)
            os_disk_name = virtual_machine.storage_profile.os_disk.name
            os_disk = self.compute_client.disks.get(resource_group_name, os_disk_name)
            disk_list.append(os_disk)
            data_disks = virtual_machine.storage_profile.data_disks
            for disk in data_disks:
                data_disk = self.compute_client.disks.get(resource_group_name, disk.name)
                disk_list.append(data_disk)
            return disk_list
        except Exception as err:
            logging.info(
                "Unable to get disk list: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get disk list",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_hdinsight_cluster(self, resource_group_name, cluster_name):
        try:
            result = self.hdinsight_client.clusters.get(resource_group_name, cluster_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get hdinsight cluster: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get hdinsight cluster",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def list_hdinsight_clusters(self, resource_group_name):
        try:
            result = self.hdinsight_client.clusters.list_by_resource_group(resource_group_name)
            return result
        except ResourceNotFoundError as err:
            if err.status_code == 404:
                return ''
        except Exception as err:
            logging.info(
                "Unable to list hdinsight clusters: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to list hdinsight clusters",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


    def list_hdinsight_statuses(self, resource_group_name, cluster_name_list):
        data = []
        for cluster_name in cluster_name_list:
            cluster_name = cluster_name['id']
            host = {}
            try:
                request = self.hdinsight_client.clusters.get(resource_group_name, cluster_name)
                host['id'] = cluster_name
                if request.properties.cluster_state == 'Accepted' or request.properties.cluster_state == 'HdInsightConfiguration' or request.properties.cluster_state == 'ClusterStorageProvisioned' or request.properties.cluster_state == 'ReadyForDeployment':
                    host['status'] = 'creating'
                elif request.properties.cluster_state == 'AzureVMConfiguration' or request.properties.cluster_state == 'Operational' or request.properties.cluster_state == 'ClusterCustomization':
                    host['status'] = 'creating'
                elif request.properties.cluster_state == 'DeletePending' or request.properties.cluster_state == 'Deleting':
                    host['status'] = 'terminating'
                elif request.properties.cluster_state == 'Error' or request.properties.cluster_state == 'TimedOut' or request.properties.cluster_state == 'Unknown':
                    host['status'] = 'failed'
                elif request.properties.cluster_state == 'Running':
                    host['status'] = 'running'
                data.append(host)
            except:
                host['id'] = cluster_name
                host['status'] = 'terminated'
                data.append(host)
        return data


def get_instance_private_ip_address(tag_name, instance_name):
    try:
        resource_group_name = os.environ['azure_resource_group_name']
        return AzureMeta().get_private_ip_address(resource_group_name, instance_name)
    except Exception as err:
            logging.info("Error with getting private ip address by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error with getting private ip address by name",
                       "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''


def node_count(cluster_name):
    try:
        node_list = []
        resource_group_name = os.environ['azure_resource_group_name']
        for node in AzureMeta().compute_client.virtual_machines.list(resource_group_name):
            if "Name" in node.tags:
                if cluster_name == node.tags["Name"]:
                    node_list.append(node.name)
        result = len(node_list)
        return result
    except Exception as err:
        logging.info("Error with counting nodes in cluster: " + str(err) + "\n Traceback: " + traceback.print_exc(
            file=sys.stdout))
        append_result(str({"error": "Error with counting nodes in cluster",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)


