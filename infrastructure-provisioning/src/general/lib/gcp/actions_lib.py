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
import meta_lib
import os
import json
import logging
import traceback
import sys, time
from Crypto.PublicKey import RSA


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
        else:
            self.service = build('compute', 'v1')
            self.service_iam = build('iam', 'v1')
            self.service = build('dataproc', 'v1')
            self.service_storage = build('storage', 'v1')
            self.storage_client = storage.Client()

    def create_vpc(self, vpc_name):
        network_params = {'name': vpc_name, 'autoCreateSubnetworks': False}
        request = self.service.networks().insert(project=self.project, body=network_params)
        try:
            print "Create VPC {}".format(vpc_name)
            result = request.execute()
            time.sleep(5)
            vpc_created = meta_lib.GCPMeta().get_vpc(vpc_name)
            while not vpc_created:
                print "VPC {} is still being created".format(vpc_name)
                time.sleep(5)
                vpc_created = meta_lib.GCPMeta().get_vpc(vpc_name)
            time.sleep(30)
            print "VPC {} has been created".format(vpc_name)
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
            print "VPC {} has been removed".format(vpc_name)
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
            'network': vpc_selflink
        }
        request = self.service.subnetworks().insert(
            project=self.project, region=region, body=subnetwork_params)
        try:
            print "Create subnet {}".format(subnet_name)
            result = request.execute()
            time.sleep(5)
            subnet_created = meta_lib.GCPMeta().get_subnet(subnet_name, region)
            while not subnet_created:
                print "Subnet {} is still being created".format(subnet_name)
                time.sleep(5)
                subnet_created = meta_lib.GCPMeta().get_subnet(subnet_name, region)
            time.sleep(30)
            print "Subnet {} has been created".format(subnet_name)
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
            print "Subnet {} has been removed".format(subnet_name)
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
                        initial_user, ami_name, service_account_name, instance_class, elastic_ip='',
                        primary_disk_size='12', secondary_disk_size='30'):
        key = RSA.importKey(open(ssh_key_path, 'rb').read())
        ssh_key = key.publickey().exportKey("OpenSSH")
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        if instance_class == 'ssn' or instance_class == 'notebook':
            access_configs = [{"type": "ONE_TO_ONE_NAT"}]
        elif instance_class == 'edge':
            access_configs = [{
                "type": "ONE_TO_ONE_NAT",
                "name": "External NAT",
                "natIP": elastic_ip
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

    def set_policy_to_service_account(self, service_account_name, role_name):
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        params = {
            "policy":
                {
                    "bindings": [
                        {
                            "role": "projects/{}/roles/{}".format(self.project, role_name),
                            "members": [
                                "serviceAccount:{}".format(service_account_email)
                            ]
                        }
                    ]
                }
        }
        request = self.service_iam.projects().serviceAccounts().setIamPolicy(resource=
                                                                             'projects/{}/serviceAccounts/{}'.
                                                                             format(self.project,
                                                                                    service_account_email),
                                                                             body=params)
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
                print 'The image is being created... Please wait'
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

    def get_gitlab_cert(self, bucket_name, certfile):
        try:
            bucket = self.storage_client.get_bucket(bucket_name)
            blob = bucket.blob(certfile)
            blob.download_to_filename(certfile)
            return True
        except exceptions.NotFound:
            return False

    def create_dataproc_cluster(self, cluster_name, region, params):
        request = self.service.projects().regions().clusters().create(projectId=self.project, region=region, body=params)
        try:
            result = request.execute()
            cluster_status = meta_lib.GCPMeta().get_list_cluster_statuses([cluster_name])
            while cluster_status[0]['status'] != 'running':
                time.sleep(5)
                print 'The cluster is being created... Please wait'
                cluster_status = meta_lib.GCPMeta().get_list_cluster_statuses([cluster_name])
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