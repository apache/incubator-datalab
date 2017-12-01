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

from pprint import pprint
from googleapiclient.discovery import build
from oauth2client.client import GoogleCredentials
from oauth2client.service_account import ServiceAccountCredentials
from google.cloud import exceptions
from google.cloud import storage
from googleapiclient import errors
from dlab.fab import *
import meta_lib
import os
import json
import logging
import traceback
import sys, time
from Crypto.PublicKey import RSA
from fabric.api import *
import urllib2


class GCPActions:
    def __init__(self, auth_type='service_account'):

        self.auth_type = auth_type
        self.project = os.environ['gcp_project_id']
        if os.environ['conf_resource'] == 'ssn':
            self.key_file = '/root/service_account.json'
            credentials = ServiceAccountCredentials.from_json_keyfile_name(
                self.key_file, scopes=('https://www.googleapis.com/auth/compute', 'https://www.googleapis.com/auth/iam',
                                       'https://www.googleapis.com/auth/cloud-platform'))
            self.service = build('compute', 'v1', credentials=credentials)
            self.service_iam = build('iam', 'v1', credentials=credentials)
            self.service_storage = build('storage', 'v1', credentials=credentials)
            self.storage_client = storage.Client.from_service_account_json('/root/service_account.json')
            self.service_resource = build('cloudresourcemanager', 'v1', credentials=credentials)
        else:
            self.service = build('compute', 'v1')
            self.service_iam = build('iam', 'v1')
            self.dataproc = build('dataproc', 'v1')
            self.service_storage = build('storage', 'v1')
            self.storage_client = storage.Client()
            self.service_resource = build('cloudresourcemanager', 'v1')

    def create_vpc(self, vpc_name):
        network_params = {'name': vpc_name, 'autoCreateSubnetworks': False}
        request = self.service.networks().insert(project=self.project, body=network_params)
        try:
            print("Create VPC {}".format(vpc_name))
            result = request.execute()
            wait_for_operation(os.environ['gcp_zone'], result['name'])
            print("VPC {} has been created".format(vpc_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to create VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create VPC",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_vpc(self, vpc_name):
        request = self.service.networks().delete(project=self.project, network=vpc_name)
        try:
            result = request.execute()
            vpc_removed = meta_lib.GCPMeta().get_vpc(vpc_name)
            while vpc_removed:
                time.sleep(5)
                vpc_removed = meta_lib.GCPMeta().get_vpc(vpc_name)
            time.sleep(30)
            print("VPC {} has been removed".format(vpc_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to remove VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove VPC",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_subnet(self, subnet_name, subnet_cidr, vpc_selflink, region):
        subnetwork_params = {
            'name': subnet_name,
            'ipCidrRange': subnet_cidr,
            'privateIpGoogleAccess': 'true',
            'network': vpc_selflink
        }
        request = self.service.subnetworks().insert(
            project=self.project, region=region, body=subnetwork_params)
        try:
            print("Create subnet {}".format(subnet_name))
            result = request.execute()
            time.sleep(5)
            subnet_created = meta_lib.GCPMeta().get_subnet(subnet_name, region)
            while not subnet_created:
                print("Subnet {} is still being created".format(subnet_name))
                time.sleep(5)
                subnet_created = meta_lib.GCPMeta().get_subnet(subnet_name, region)
            time.sleep(30)
            print("Subnet {} has been created".format(subnet_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to create Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Subnet",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_subnet(self, subnet_name, region):
        request = self.service.subnetworks().delete(project=self.project, region=region, subnetwork=subnet_name)
        try:
            result = request.execute()
            subnet_removed = meta_lib.GCPMeta().get_subnet(subnet_name, region)
            while subnet_removed:
                time.sleep(5)
                subnet_removed = meta_lib.GCPMeta().get_subnet(subnet_name, region)
            print("Subnet {} has been removed".format(subnet_name))
            time.sleep(30)
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Subnet",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_firewall(self, firewall_params):
        request = self.service.firewalls().insert(project=self.project, body=firewall_params)
        try:
            result = request.execute()
            firewall_created = meta_lib.GCPMeta().get_firewall(firewall_params['name'])
            while not firewall_created:
                time.sleep(5)
                firewall_created = meta_lib.GCPMeta().get_firewall(firewall_params['name'])
            time.sleep(30)
            print('Firewall {} created.'.format(firewall_params['name']))
            return result
        except Exception as err:
            logging.info(
                "Unable to create Firewall: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Firewall",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_firewall(self, firewall_name):
        request = self.service.firewalls().delete(project=self.project, firewall=firewall_name)
        try:
            result = request.execute()
            firewall_removed = meta_lib.GCPMeta().get_firewall(firewall_name)
            while firewall_removed:
                time.sleep(5)
                firewall_removed = meta_lib.GCPMeta().get_firewall(firewall_name)
            time.sleep(30)
            print('Firewall {} removed.'.format(firewall_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Firewall: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Firewall",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_bucket(self, bucket_name):
        try:
            bucket = self.storage_client.create_bucket(bucket_name)
            print('Bucket {} created.'.format(bucket.name))
        except Exception as err:
            logging.info(
                "Unable to create Bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Bucket",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_bucket(self, bucket_name):
        try:
            storage_resource = storage.Bucket(self.storage_client, bucket_name)
            storage_resource.delete(force=True)
            print('Bucket {} removed.'.format(bucket_name))
        except Exception as err:
            logging.info(
                "Unable to remove Bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Bucket",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def bucket_cleanup(self, bucket_name, user_name, cluster_name):
        try:
            bucket = self.storage_client.get_bucket(bucket_name)
            list_files = bucket.list_blobs(prefix='{0}/{1}'.format(user_name, cluster_name))
            for item in list_files:
                print("Deleting:{}".format(item.name))
                blob = bucket.blob(item.name)
                blob.delete()
        except Exception as err:
            logging.info(
                "Unable to remove files from bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove files from bucket",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_disk(self, instance_name, zone, size):
        try:
            params = {"sizeGb": size, "name": instance_name + '-secondary',
                      "type": "projects/{0}/zones/{1}/diskTypes/pd-ssd".format(self.project, zone)}
            request = self.service.disks().insert(project=self.project, zone=zone, body=params)
            request.execute()
            while meta_lib.GCPMeta().get_disk(instance_name + '-secondary')["status"] != "READY":
                time.sleep(10)
            print('Disk {}-secondary created.'.format(instance_name))
            return request
        except Exception as err:
            logging.info(
                "Unable to create disk: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create disk",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_disk(self, instance_name, zone):
        try:
            request = self.service.disks().delete(project=self.project, zone=zone, disk=instance_name + '-secondary')
            request.execute()
            print('Disk {}-secondary removed.'.format(instance_name))
            return request
        except Exception as err:
            logging.info(
                "Unable to remove disk: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove disk",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_instance(self, instance_name, region, zone, vpc_name, subnet_name, instance_size, ssh_key_path,
                        initial_user, ami_name, service_account_name, instance_class, static_ip='',
                        primary_disk_size='12', secondary_disk_size='30', gpu_accelerator_type='None'):
        key = RSA.importKey(open(ssh_key_path, 'rb').read())
        ssh_key = key.publickey().exportKey("OpenSSH")
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        access_configs = ''
        # if instance_class == 'ssn' or instance_class == 'notebook':
        #     access_configs = [{"type": "ONE_TO_ONE_NAT"}]
        if instance_class == 'ssn' or instance_class == 'edge':
            access_configs = [{
                "type": "ONE_TO_ONE_NAT",
                "name": "External NAT",
                "natIP": static_ip
            }]
        if instance_class == 'notebook':
            GCPActions().create_disk(instance_name, zone, secondary_disk_size)
            disks = [
                {
                    "deviceName": instance_name + '-primary',
                    "autoDelete": "true",
                    "boot": "true",
                    "mode": "READ_WRITE",
                    "type": "PERSISTENT",
                    "initializeParams": {
                        "diskSizeGb": primary_disk_size,
                        "sourceImage": ami_name
                    }
                },
                {
                    "deviceName": instance_name + '-secondary',
                    "autoDelete": "true",
                    "boot": "false",
                    "mode": "READ_WRITE",
                    "type": "PERSISTENT",
                    "interface": "SCSI",
                    "source": "projects/{0}/zones/{1}/disks/{2}-secondary".format(self.project, zone, instance_name)
                }
            ]
        else:
            disks = [{
                "deviceName": instance_name + '-primary',
                "autoDelete": 'true',
                "initializeParams": {
                    "diskSizeGb": primary_disk_size,
                    "sourceImage": ami_name
                },
                "boot": 'true',
                "mode": "READ_WRITE"
            }]
        instance_params = {
            "name": instance_name,
            "machineType": "zones/{}/machineTypes/{}".format(zone, instance_size),
            "networkInterfaces": [
                {
                    "network": "global/networks/{}".format(vpc_name),
                    "subnetwork": "regions/{}/subnetworks/{}".format(region, subnet_name),
                    "accessConfigs": access_configs
                },
            ],
            "metadata":
                {"items": [
                    {
                        "key": "ssh-keys",
                        "value": "{}:{}".format(initial_user, ssh_key)
                    }
                ]
                },
            "disks": disks,
            "serviceAccounts": [
                {
                    "email": service_account_email,
                    "scopes": ["https://www.googleapis.com/auth/cloud-platform"]
                }
            ]
        }
        if instance_class == 'notebook':
            del instance_params['networkInterfaces'][0]['accessConfigs']
        if gpu_accelerator_type != 'None':
            instance_params['guestAccelerators'] = [
                {
                    "acceleratorCount": 1,
                    "acceleratorType": "projects/{0}/zones/{1}/acceleratorTypes/{2}".format(self.project, zone, gpu_accelerator_type)
                }
            ]
            instance_params['scheduling'] = {
                "onHostMaintenance": "terminate",
                "automaticRestart": "true"
            }
        request = self.service.instances().insert(project=self.project, zone=zone, body=instance_params)
        try:
            result = request.execute()
            instance_created = meta_lib.GCPMeta().get_instance(instance_name)
            while not instance_created:
                time.sleep(5)
                instance_created = meta_lib.GCPMeta().get_instance(instance_name)
            instance_started = meta_lib.GCPMeta().get_instance_status(instance_name)
            while instance_started != 'RUNNING':
                time.sleep(5)
                instance_started = meta_lib.GCPMeta().get_instance_status(instance_name)
            time.sleep(5)
            print('Instance {} created.'.format(instance_name))
            request = self.service.instances().get(instance=instance_name, project=self.project, zone=zone)
            res = request.execute()
            tag = ''
            if 'ssn' in instance_name or 'edge' in instance_name:
                tag = instance_name
            elif 'nb' in instance_name:
                tag = instance_name[:instance_name.index("nb") + 2].replace('_', '-')
            instance_tag = {"items": [tag], "fingerprint": res['tags']['fingerprint']}
            request = self.service.instances().setTags(instance=instance_name, project=self.project, zone=zone,
                                                        body=instance_tag)
            res = request.execute()
            return result
        except Exception as err:
            logging.info(
                "Unable to create Instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to create Instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_instance(self, instance_name, zone):
        request = self.service.instances().delete(project=self.project, zone=zone,
                                                  instance=instance_name)
        try:
            result = request.execute()
            instance_removed = meta_lib.GCPMeta().get_instance(instance_name)
            while instance_removed:
                time.sleep(5)
                instance_removed = meta_lib.GCPMeta().get_instance(instance_name)
            time.sleep(30)
            print('Instance {} removed.'.format(instance_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to remove Instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def stop_instance(self, instance_name, zone):
        request = self.service.instances().stop(project=self.project, zone=zone, instance=instance_name)
        try:
            request.execute()
            instance_stopped = meta_lib.GCPMeta().get_instance_status(instance_name)
            while instance_stopped != 'TERMINATED':
                time.sleep(5)
                instance_stopped = meta_lib.GCPMeta().get_instance_status(instance_name)
            time.sleep(5)
            return True
        except Exception as err:
            logging.info(
                "Unable to stop Instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to stop Instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def start_instance(self, instance_name, zone):
        request = self.service.instances().start(project=self.project, zone=zone, instance=instance_name)
        try:
            request.execute()
            instance_started = meta_lib.GCPMeta().get_instance_status(instance_name)
            while instance_started != 'RUNNING':
                time.sleep(5)
                instance_started = meta_lib.GCPMeta().get_instance_status(instance_name)
            time.sleep(5)
            return True
        except Exception as err:
            logging.info(
                "Unable to start Instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to start Instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_service_account(self, service_account_name):
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        request = self.service_iam.projects().serviceAccounts().delete(
            name='projects/{}/serviceAccounts/{}'.format(self.project, service_account_email))
        try:
            result = request.execute()
            service_account_removed = meta_lib.GCPMeta().get_service_account(service_account_name)
            while service_account_removed:
                time.sleep(5)
                service_account_removed = meta_lib.GCPMeta().get_service_account(service_account_name)
            time.sleep(30)
            print('Service account {} removed.'.format(service_account_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Service account: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to remove Service account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_service_account(self, service_account_name):
        params = {"accountId": service_account_name, "serviceAccount": {"displayName": service_account_name}}
        request = self.service_iam.projects().serviceAccounts().create(name='projects/{}'.format(self.project),
                                                                       body=params)
        try:
            result = request.execute()
            service_account_created = meta_lib.GCPMeta().get_service_account(service_account_name)
            while not service_account_created:
                time.sleep(5)
                service_account_created = meta_lib.GCPMeta().get_service_account(service_account_name)
            time.sleep(30)
            print('Service account {} created.'.format(service_account_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to create Service account: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to create Service account",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def set_role_to_service_account(self, service_account_name, role_name):
        request = GCPActions().service_resource.projects().getIamPolicy(resource=self.project, body={})
        project_policy = request.execute()
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        params = {
            "role": "projects/{}/roles/{}".format(self.project, role_name.replace('-', '_')),
            "members": [
                "serviceAccount:{}".format(service_account_email)
            ]
        }
        project_policy['bindings'].append(params)
        params = {
            "policy": {
                "bindings": project_policy['bindings']
            }
        }
        request = self.service_resource.projects().setIamPolicy(resource=self.project, body=params)
        try:
            return request.execute()
        except Exception as err:
            logging.info(
                "Unable to set Service account policy: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to set Service account policy",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_role(self, role_name, permissions):
        request = self.service_iam.projects().roles().create(parent="projects/{}".format(self.project),
                                                             body=
                                                             {
                                                                 "roleId": role_name.replace('-', '_'),
                                                                 "role": {
                                                                     "title": role_name,
                                                                     "includedPermissions": permissions
                                                                 }})
        try:
            result = request.execute()
            role_created = meta_lib.GCPMeta().get_role(role_name)
            while not role_created:
                time.sleep(5)
                role_created = meta_lib.GCPMeta().get_role(role_name)
            time.sleep(30)
            print('IAM role {} created.'.format(role_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to create IAM role: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to create IAM role",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_role(self, role_name):
        request = self.service_iam.projects().roles().delete(
            name='projects/{}/roles/{}'.format(self.project, role_name.replace('-', '_')))
        try:
            result = request.execute()
            role_removed = meta_lib.GCPMeta().get_role(role_name)
            while role_removed:
                time.sleep(5)
                role_removed = meta_lib.GCPMeta().get_role(role_name)
            time.sleep(30)
            print('IAM role {} removed.'.format(role_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to remove IAM role: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to remove IAM role",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def set_service_account_to_instance(self, service_account_name, instance_name):
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        params = {
            "email": service_account_email
        }
        request = self.service.instances().setServiceAccount(
            project=self.project, zone=os.environ['gcp_zone'], instance=instance_name, body=params)
        try:
            return request.execute()
        except Exception as err:
            logging.info(
                "Unable to set Service account to instance: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to set Service account to instance",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_static_address(self, address_name, region):
        params = {"name": address_name}
        request = self.service.addresses().insert(project=self.project, region=region, body=params)
        try:
            result = request.execute()
            static_address_created = meta_lib.GCPMeta().get_static_address(region, address_name)
            while not static_address_created:
                time.sleep(5)
                static_address_created = meta_lib.GCPMeta().get_static_address(region, address_name)
            time.sleep(30)
            print('Static address {} created.'.format(address_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to create Static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to create Static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def remove_static_address(self, address_name, region):
        request = self.service.addresses().delete(project=self.project, region=region, address=address_name)
        try:
            result = request.execute()
            static_address_removed = meta_lib.GCPMeta().get_static_address(region, address_name)
            while static_address_removed:
                time.sleep(5)
                static_address_removed = meta_lib.GCPMeta().get_static_address(region, address_name)
            time.sleep(30)
            print('Static address {} removed.'.format(address_name))
            return result
        except Exception as err:
            logging.info(
                "Unable to remove Static IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to remove Static IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def create_image_from_instance_disk(self, image_name, source_name, zone):
        params = {"name": image_name, "sourceDisk": source_disk}
        request = self.service.images().insert(project=self.project, body=params)
        try:
            GCPActions().stop_instance(self, source_name, zone)
            result = request.execute()
            while result["status"] == 'DONE':
                time.sleep(20)
                print('The image is being created... Please wait')
            return result
        except Exception as err:
            logging.info(
                "Unable to create image from disk: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to create image from disk",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def put_to_bucket(self, bucket_name, local_file, dest_file):
        try:
            bucket = self.storage_client.get_bucket(bucket_name)
            blob = bucket.blob(dest_file)
            blob.upload_from_filename(local_file)
            return True
        except:
            return False

    def get_from_bucket(self, bucket_name, dest_file, local_file):
        try:
            bucket = self.storage_client.get_bucket(bucket_name)
            blob = bucket.blob(dest_file)
            if blob.exists():
                blob.download_to_filename(local_file)
                return True
            else:
                return False
        except exceptions.NotFound:
            return False

    def get_gitlab_cert(self, bucket_name, certfile):
        try:
            bucket = self.storage_client.get_bucket(bucket_name)
            blob = bucket.blob(certfile)
            if blob.exists():
                blob.download_to_filename(certfile)
                return True
            else:
                return False
        except exceptions.NotFound:
            return False

    def create_dataproc_cluster(self, cluster_name, region, params):
        request = self.dataproc.projects().regions().clusters().create(projectId=self.project, region=region, body=params)
        try:
            result = request.execute()
            time.sleep(5)
            cluster_status = meta_lib.GCPMeta().get_list_cluster_statuses([cluster_name])
            while cluster_status[0]['status'] != 'running':
                time.sleep(5)
                print('The cluster is being created... Please wait')
                cluster_status = meta_lib.GCPMeta().get_list_cluster_statuses([cluster_name])
                if cluster_status[0]['status'] == 'terminated':
                    raise Exception
            return result
        except Exception as err:
            logging.info(
                "Unable to create dataproc cluster: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to create dataproc cluster",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def delete_dataproc_cluster(self, cluster_name, region):
        request = self.dataproc.projects().regions().clusters().delete(projectId=self.project, region=region, clusterName=cluster_name)
        try:
            result = request.execute()
            cluster_status = meta_lib.GCPMeta().get_list_cluster_statuses([cluster_name])
            while cluster_status[0]['status'] != 'terminated':
                time.sleep(5)
                print('The cluster is being terminated... Please wait')
                cluster_status = meta_lib.GCPMeta().get_list_cluster_statuses([cluster_name])
            return result
        except Exception as err:
            logging.info(
                "Unable to delete dataproc cluster: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to delete dataproc cluster",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def update_dataproc_cluster(self, cluster_name, notebook_instance_name):
        body = {"labels": {notebook_instance_name: "configured"}}
        request = self.dataproc.projects().regions().clusters().patch(projectId=self.project,
                                                                      region=os.environ['gcp_region'],
                                                                      clusterName=cluster_name,
                                                                      updateMask='labels',
                                                                      body=body)
        try:
            result = request.execute()
            time.sleep(15)
            return result
        except Exception as err:
            logging.info(
                "Unable to update dataproc cluster: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to update dataproc cluster",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def submit_dataproc_job(self, job_body):
        request = self.dataproc.projects().regions().jobs().submit(projectId=self.project,
                                                                   region=os.environ['gcp_region'],
                                                                   body=job_body)
        try:
            res = request.execute()
            print("Job ID: {}".format(res['reference']['jobId']))
            job_status = meta_lib.GCPMeta().get_dataproc_job_status(res['reference']['jobId'])
            while job_status != 'done':
                time.sleep(5)
                job_status = meta_lib.GCPMeta().get_dataproc_job_status(res['reference']['jobId'])
                if job_status == 'failed':
                    raise Exception
            return job_status
        except Exception as err:
            logging.info(
                "Unable to submit dataproc job: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to submit dataproc job",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            sys.exit(1)

    def get_cluster_app_version(self, bucket, user_name, cluster_name, application):
        try:
            version_file = '{0}/{1}/{2}_version'.format(user_name, cluster_name, application)
            if GCPActions().get_from_bucket(bucket, version_file, '/tmp/{}_version'.format(application)):
                with file('/tmp/{}_version'.format(application)) as f:
                    version = f.read()
                return version[0:5]
            else:
                raise Exception
        except Exception as err:
            logging.info(
                "Unable to get software version: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to get software version",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def jars(self, args, dataproc_dir):
        print("Downloading jars...")
        GCPActions().get_from_bucket(args.bucket, 'jars/{0}/jars.tar.gz'.format(args.dataproc_version), '/tmp/jars.tar.gz')
        GCPActions().get_from_bucket(args.bucket, 'jars/{0}/jars-checksum.chk'.format(args.dataproc_version), '/tmp/jars-checksum.chk')
        if 'WARNING' in local('md5sum -c /tmp/jars-checksum.chk', capture=True):
            local('rm -f /tmp/jars.tar.gz')
            GCPActions().get_from_bucket(args.bucket, 'jars/{0}/jars.tar.gz'.format(args.cluster_name), '/tmp/jars.tar.gz')
            if 'WARNING' in local('md5sum -c /tmp/jars-checksum.chk', capture=True):
                print("The checksum of jars.tar.gz is mismatched. It could be caused by gcp network issue.")
                sys.exit(1)
        local('tar -zhxvf /tmp/jars.tar.gz -C {}'.format(dataproc_dir))

    def yarn(self, args, yarn_dir):
        print("Downloading yarn configuration...")
        bucket = self.storage_client.get_bucket(args.bucket)
        list_files = bucket.list_blobs(prefix='{0}/{1}/config/'.format(args.user_name, args.cluster_name))
        local('mkdir -p /tmp/{0}/{1}/config/'.format(args.user_name, args.cluster_name))
        for item in list_files:
            local_file = '/tmp/{0}/{1}/config/{2}'.format(args.user_name, args.cluster_name, item.name.split("/")[-1:][0])
            GCPActions().get_from_bucket(args.bucket, item.name, local_file)
        local('sudo mv /tmp/{0}/{1}/config/* {2}'.format(args.user_name, args.cluster_name, yarn_dir))
        local('sudo rm -rf /tmp/{}'.format(args.user_name))

    def install_dataproc_spark(self, args):
        print("Installing spark...")
        GCPActions().get_from_bucket(args.bucket, '{0}/{1}/spark.tar.gz'.format(args.user_name, args.cluster_name), '/tmp/spark.tar.gz')
        GCPActions().get_from_bucket(args.bucket, '{0}/{1}/spark-checksum.chk'.format(args.user_name, args.cluster_name), '/tmp/spark-checksum.chk')
        if 'WARNING' in local('md5sum -c /tmp/spark-checksum.chk', capture=True):
            local('rm -f /tmp/spark.tar.gz')
            GCPActions().get_from_bucket(args.bucket, '{0}/{1}/spark.tar.gz'.format(args.user_name, args.cluster_name), '/tmp/spark.tar.gz')
            if 'WARNING' in local('md5sum -c /tmp/spark-checksum.chk', capture=True):
                print("The checksum of spark.tar.gz is mismatched. It could be caused by gcp network issue.")
                sys.exit(1)
        local('sudo tar -zhxvf /tmp/spark.tar.gz -C /opt/{0}/{1}/'.format(args.dataproc_version, args.cluster_name))

    def spark_defaults(self, args):
        spark_def_path = '/opt/{0}/{1}/spark/conf/spark-env.sh'.format(args.dataproc_version, args.cluster_name)
        local(""" sudo bash -c " sed -i '/#/d' {}" """.format(spark_def_path))
        local(""" sudo bash -c " sed -i '/^\s*$/d' {}" """.format(spark_def_path))
        local(""" sudo bash -c " sed -i 's|/usr/lib/hadoop|/opt/{0}/jars/usr/lib/hadoop|g' {1}" """.format(args.dataproc_version, spark_def_path))
        local(""" sudo bash -c " sed -i 's|/etc/hadoop/conf|/opt/{0}/{1}/conf|g' {2}" """.format(args.dataproc_version, args.cluster_name, spark_def_path))
        local(""" sudo bash -c " sed -i '/\$HADOOP_HOME\/\*/a SPARK_DIST_CLASSPATH=\\"\$SPARK_DIST_CLASSPATH:\$HADOOP_HOME\/client\/*\\"' {}" """.format(spark_def_path))
        local(""" sudo bash -c " sed -i '/\$HADOOP_YARN_HOME\/\*/a SPARK_DIST_CLASSPATH=\\"\$SPARK_DIST_CLASSPATH:\/opt\/jars\/\*\\"' {}" """.format(spark_def_path))
        local(""" sudo bash -c " sed -i 's|/hadoop/spark/work|/tmp/hadoop/spark/work|g' {}" """.format(spark_def_path))
        local(""" sudo bash -c " sed -i 's|/hadoop/spark/tmp|/tmp/hadoop/spark/tmp|g' {}" """.format(spark_def_path))
        local(""" sudo bash -c " sed -i 's/STANDALONE_SPARK_MASTER_HOST.*/STANDALONE_SPARK_MASTER_HOST={0}-m/g' {1}" """.format(args.cluster_name, spark_def_path))
        local(""" sudo bash -c " sed -i 's|/hadoop_gcs_connector_metadata_cache|/tmp/hadoop_gcs_connector_metadata_cache|g' /opt/{0}/{1}/conf/core-site.xml" """.format(args.dataproc_version, args.cluster_name))

    def remove_kernels(self, notebook_name, dataproc_name, dataproc_version, ssh_user, key_path):
        try:
            notebook_ip = meta_lib.GCPMeta().get_private_ip_address(notebook_name)
            env.hosts = "{}".format(notebook_ip)
            env.user = "{}".format(ssh_user)
            env.key_filename = "{}".format(key_path)
            env.host_string = env.user + "@" + env.hosts
            sudo('rm -rf /home/{}/.local/share/jupyter/kernels/*_{}'.format(ssh_user, dataproc_name))
            if exists('/home/{}/.ensure_dir/dataengine-service_{}_interpreter_ensured'.format(ssh_user, dataproc_name)):
                if os.environ['notebook_multiple_clusters'] == 'true':
                    try:
                        livy_port = sudo("cat /opt/" + dataproc_version + "/" + dataproc_name
                                         + "/livy/conf/livy.conf | grep livy.server.port | tail -n 1 | awk '{printf $3}'")
                        process_number = sudo("netstat -natp 2>/dev/null | grep ':" + livy_port +
                                              "' | awk '{print $7}' | sed 's|/.*||g'")
                        sudo('kill -9 ' + process_number)
                        sudo('systemctl disable livy-server-' + livy_port)
                    except:
                        print("Wasn't able to find Livy server for this EMR!")
                sudo('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh')
                sudo("rm -rf /home/{}/.ensure_dir/dataengine-service_interpreter_ensure".format(ssh_user))
                zeppelin_url = 'http://' + notebook_ip + ':8080/api/interpreter/setting/'
                opener = urllib2.build_opener(urllib2.ProxyHandler({}))
                req = opener.open(urllib2.Request(zeppelin_url))
                r_text = req.read()
                interpreter_json = json.loads(r_text)
                interpreter_prefix = dataproc_name
                for interpreter in interpreter_json['body']:
                    if interpreter_prefix in interpreter['name']:
                        print("Interpreter with ID: {} and name: {} will be removed from zeppelin!".format(
                            interpreter['id'], interpreter['name']))
                        request = urllib2.Request(zeppelin_url + interpreter['id'], data='')
                        request.get_method = lambda: 'DELETE'
                        url = opener.open(request)
                        print(url.read())
                sudo('chown {0}:{0} -R /opt/zeppelin/'.format(ssh_user))
                sudo('systemctl restart zeppelin-notebook.service')
                zeppelin_restarted = False
                while not zeppelin_restarted:
                    sudo('sleep 5')
                    result = sudo('nmap -p 8080 localhost | grep "closed" > /dev/null; echo $?')
                    result = result[:1]
                    if result == '1':
                        zeppelin_restarted = True
                sudo('sleep 5')
                sudo('rm -rf /home/{}/.ensure_dir/dataengine-service_{}_interpreter_ensured'.format(ssh_user, dataproc_name))
            if exists('/home/{}/.ensure_dir/rstudio_dataproc_ensured'.format(ssh_user)):
                sudo("sed -i '/{0}/d' /home/{1}/.Renviron".format(dataproc_name, ssh_user))
                if not sudo("sed -n '/^SPARK_HOME/p' /home/{}/.Renviron".format(ssh_user)):
                    sudo("sed -i '1!G;h;$!d;' /home/{0}/.Renviron; sed -i '1,3s/#//;1!G;h;$!d' /home/{0}/.Renviron".format(ssh_user))
                sudo("sed -i 's|/opt/{0}/{1}/spark//R/lib:||g' /home/{2}/.bashrc".format(dataproc_version, dataproc_name, ssh_user))
            sudo('rm -rf  /opt/{0}/{1}/'.format(dataproc_version, dataproc_name))
            print("Notebook's {} kernels were removed".format(env.hosts))
        except Exception as err:
            logging.info(
                "Unable to delete dataproc kernels from notebook: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to delete dataproc kernels from notebook",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def configure_zeppelin_dataproc_interpreter(self, dataproc_version, cluster_name, spark_dir,
                                                os_user, yarn_dir, bucket, user_name, multiple_clusters):
        try:
            port_number_found = False
            zeppelin_restarted = False
            default_port = 8998
            GCPActions().get_cluster_app_version(bucket, user_name, cluster_name, 'python')
            with file('/tmp/python_version') as f:
                python_version = f.read()
            python_version = python_version[0:5]
            livy_port = ''
            livy_path = '/opt/{0}/{1}/livy/'.format(dataproc_version, cluster_name)
            local('echo \"Configuring dataproc path for Zeppelin\"')
            local('sed -i \"s/^export SPARK_HOME.*/export SPARK_HOME=\/opt\/{0}\/{1}\/spark/\" /opt/zeppelin/conf/zeppelin-env.sh'
                  .format(dataproc_version, cluster_name))
            local('sed -i \"s/^export HADOOP_CONF_DIR.*/export HADOOP_CONF_DIR=\/opt\/{0}\/{1}\/conf/\" /opt/{0}/{1}/spark/conf/spark-env.sh'
                  .format(dataproc_version, cluster_name))
            local('sed -i "/spark.executorEnv.PYTHONPATH/d" /opt/{0}/{1}/spark/conf/spark-defaults.conf'.format(dataproc_version, cluster_name))
            local('sed -i "/spark.yarn.dist.files/d" /opt/{0}/{1}/spark/conf/spark-defaults.conf'.format(dataproc_version, cluster_name))
            local('sudo chown {0}:{0} -R /opt/zeppelin/'.format(os_user))
            local('sudo systemctl restart zeppelin-notebook.service')
            while not zeppelin_restarted:
                local('sleep 5')
                result = local('sudo bash -c "nmap -p 8080 localhost | grep closed > /dev/null" ; echo $?', capture=True)
                result = result[:1]
                if result == '1':
                    zeppelin_restarted = True
            local('sleep 5')
            local('echo \"Configuring dataproc spark interpreter for Zeppelin\"')
            if multiple_clusters == 'true':
                while not port_number_found:
                    port_free = local('sudo bash -c "nmap -p ' + str(default_port) +
                                      ' localhost | grep closed > /dev/null" ; echo $?', capture=True)
                    port_free = port_free[:1]
                    if port_free == '0':
                        livy_port = default_port
                        port_number_found = True
                    else:
                        default_port += 1
                local('sudo echo "livy.server.port = {0}" >> {1}conf/livy.conf'.format(str(livy_port), livy_path))
                local('sudo echo "livy.spark.master = yarn" >> {}conf/livy.conf'.format(livy_path))
                if os.path.exists('{}conf/spark-blacklist.conf'.format(livy_path)):
                    local('sudo sed -i "s/^/#/g" {}conf/spark-blacklist.conf'.format(livy_path))
                local('sudo echo "export SPARK_HOME={0}" >> {1}conf/livy-env.sh'.format(spark_dir, livy_path))
                local('sudo echo "export HADOOP_CONF_DIR={0}" >> {1}conf/livy-env.sh'.format(yarn_dir, livy_path))
                local('sudo echo "export PYSPARK3_PYTHON=python{0}" >> {1}conf/livy-env.sh'.format(python_version[0:3], livy_path))
                template_file = "/tmp/dataengine-service_interpreter.json"
                fr = open(template_file, 'r+')
                text = fr.read()
                text = text.replace('CLUSTER_NAME', cluster_name)
                text = text.replace('SPARK_HOME', spark_dir)
                text = text.replace('LIVY_PORT', str(livy_port))
                fw = open(template_file, 'w')
                fw.write(text)
                fw.close()
                for _ in range(5):
                    try:
                        local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                              "@/tmp/dataengine-service_interpreter.json http://localhost:8080/api/interpreter/setting")
                        break
                    except:
                        local('sleep 5')
                        pass
                local('sudo cp /opt/livy-server-cluster.service /etc/systemd/system/livy-server-{}.service'.format(str(livy_port)))
                local("sudo sed -i 's|OS_USER|{0}|' /etc/systemd/system/livy-server-{1}.service".format(os_user, str(livy_port)))
                local("sudo sed -i 's|LIVY_PATH|{0}|' /etc/systemd/system/livy-server-{1}.service".format(livy_path, str(livy_port)))
                local('sudo chmod 644 /etc/systemd/system/livy-server-{}.service'.format(str(livy_port)))
                local('sudo systemctl daemon-reload')
                local('sudo systemctl enable livy-server-{}'.format(str(livy_port)))
                local('sudo systemctl start livy-server-{}'.format(str(livy_port)))
            else:
                template_file = "/tmp/dataengine-service_interpreter.json"
                p_versions = ["2", python_version[:3]]
                for p_version in p_versions:
                    fr = open(template_file, 'r+')
                    text = fr.read()
                    text = text.replace('CLUSTERNAME', cluster_name)
                    text = text.replace('PYTHONVERSION', p_version)
                    text = text.replace('SPARK_HOME', spark_dir)
                    text = text.replace('PYTHONVER_SHORT', p_version[:1])
                    text = text.replace('DATAENGINE-SERVICE_VERSION', dataproc_version)
                    tmp_file = '/tmp/dataproc_spark_py{}_interpreter.json'.format(p_version)
                    fw = open(tmp_file, 'w')
                    fw.write(text)
                    fw.close()
                    for _ in range(5):
                        try:
                            local("curl --noproxy localhost -H 'Content-Type: application/json' -X POST -d " +
                                  "@/tmp/dataproc_spark_py{}_interpreter.json http://localhost:8080/api/interpreter/setting".format(p_version))
                            break
                        except:
                            local('sleep 5')
                            pass
            local('touch /home/{0}/.ensure_dir/dataengine-service_{1}_interpreter_ensured'.format(os_user, cluster_name))
        except:
            sys.exit(1)

    def install_python(self, bucket, user_name, cluster_name, application):
        try:
            GCPActions().get_cluster_app_version(bucket, user_name, cluster_name, 'python')
            with file('/tmp/python_version') as f:
                python_version = f.read()
            python_version = python_version[0:5]
            if not os.path.exists('/opt/python/python{}'.format(python_version)):
                local('wget https://www.python.org/ftp/python/{0}/Python-{0}.tgz -O /tmp/Python-{0}.tgz'.format(python_version))
                local('tar zxvf /tmp/Python-{}.tgz -C /tmp/'.format(python_version))
                with lcd('/tmp/Python-{}'.format(python_version)):
                    local('./configure --prefix=/opt/python/python{} --with-zlib-dir=/usr/local/lib/ --with-ensurepip=install'.format(python_version))
                    local('sudo make altinstall')
                with lcd('/tmp/'):
                    local('sudo rm -rf Python-{}/'.format(python_version))
                local('sudo -i virtualenv /opt/python/python{}'.format(python_version))
                venv_command = '/bin/bash /opt/python/python{}/bin/activate'.format(python_version)
                pip_command = '/opt/python/python{0}/bin/pip{1}'.format(python_version, python_version[:3])
                local('{0} && sudo -i {1} install -U pip'.format(venv_command, pip_command))
                local('{0} && sudo -i {1} install ipython ipykernel --no-cache-dir'.format(venv_command, pip_command))
                local('{0} && sudo -i {1} install boto boto3 NumPy SciPy Matplotlib pandas Sympy Pillow sklearn --no-cache-dir'
                      .format(venv_command, pip_command))
                if application == 'deeplearning':
                    local('{0} && sudo -i {1} install mxnet-cu80 opencv-python keras Theano --no-cache-dir'.format(venv_command, pip_command))
                    python_without_dots = python_version.replace('.', '')
                    local('{0} && sudo -i {1} install  https://cntk.ai/PythonWheel/GPU/cntk-2.0rc3-cp{2}-cp{2}m-linux_x86_64.whl --no-cache-dir'
                          .format(venv_command, pip_command, python_without_dots[:2]))
                local('sudo rm -rf /usr/bin/python{}'.format(python_version[0:3]))
                local('sudo ln -fs /opt/python/python{0}/bin/python{1} /usr/bin/python{1}'.format(python_version, python_version[0:3]))
        except Exception as err:
            logging.info(
                "Unable to install python: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to install python",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''


def ensure_local_jars(os_user, jars_dir, files_dir, region, templates_dir):
    if not exists('/home/{}/.ensure_dir/gs_kernel_ensured'.format(os_user)):
        try:
            sudo('mkdir -p {}'.format(jars_dir))
            sudo('wget https://storage.googleapis.com/hadoop-lib/gcs/{0} -O {1}{0}'
                 .format('gcs-connector-latest-hadoop2.jar', jars_dir))
            sudo('wget http://central.maven.org/maven2/org/apache/hadoop/hadoop-yarn-server-web-proxy/2.7.4/{0} -O {1}{0}'
                 .format('hadoop-yarn-server-web-proxy-2.7.4.jar', jars_dir))
            put(templates_dir + 'core-site.xml', '/tmp/core-site.xml')
            sudo('sed -i "s|GCP_PROJECT_ID|{}|g" /tmp/core-site.xml'.format(os.environ['gcp_project_id']))
            sudo('mv /tmp/core-site.xml /opt/spark/conf/core-site.xml')
            put(templates_dir + 'notebook_spark-defaults_local.conf', '/tmp/notebook_spark-defaults_local.conf')
            if os.environ['application'] == 'zeppelin':
                sudo('echo \"spark.jars $(ls -1 ' + jars_dir + '* | tr \'\\n\' \',\')\" >> /tmp/notebook_spark-defaults_local.conf')
            sudo('\cp /tmp/notebook_spark-defaults_local.conf /opt/spark/conf/spark-defaults.conf')
            sudo('touch /home/{}/.ensure_dir/gs_kernel_ensured'.format(os_user))
        except:
            sys.exit(1)


def get_cluster_python_version(region, bucket, user_name, cluster_name):
    try:
        GCPActions().get_cluster_app_version(bucket, user_name, cluster_name, 'python')
    except:
        sys.exit(1)


def installing_python(region, bucket, user_name, cluster_name, application='', pip_mirror=''):
    try:
        GCPActions().install_python(bucket, user_name, cluster_name, application)
    except:
        sys.exit(1)


def prepare_disk(os_user):
    if not exists('/home/' + os_user + '/.ensure_dir/disk_ensured'):
        try:
            disk_name = sudo("lsblk | grep disk | awk '{print $1}' | sort | tail -n 1")
            sudo('''bash -c 'echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/{}' '''.format(disk_name))
            sudo('mkfs.ext4 -F /dev/{}1'.format(disk_name))
            sudo('mount /dev/{}1 /opt/'.format(disk_name))
            sudo(''' bash -c "echo '/dev/{}1 /opt/ ext4 errors=remount-ro 0 1' >> /etc/fstab" '''.format(disk_name))
            sudo('touch /home/' + os_user + '/.ensure_dir/disk_ensured')
        except:
            sys.exit(1)
