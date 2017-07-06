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
from google.cloud import storage
from googleapiclient import errors
import meta_lib
import os
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
                self.key_file, scopes='https://www.googleapis.com/auth/compute')
            self.service = build('compute', 'v1', credentials=credentials)
            self.storage_client = storage.Client.from_service_account_json('/root/service_account.json')
        else:
            self.service = build('compute', 'v1')
            self.storage_client = storage.Client()

    def create_vpc(self, vpc_name):
        network_params = {'name': vpc_name, 'autoCreateSubnetworks': False}
        request = self.service.networks().insert(project=self.project, body=network_params)
        try:
            result = request.execute()
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
            result = request.execute()
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
                    "Unable to create Bucket: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to create Bucket",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def create_instance(self, instance_name, region, zone, vpc_name, subnet_name, instance_size, ssh_key_path,
                        initial_user):
        key = RSA.importKey(open(ssh_key_path, 'rb').read())
        ssh_key = key.publickey().exportKey("OpenSSH")
        instance_params = {
            "name": instance_name,
            "machineType": "zones/{}/machineTypes/{}".format(zone, instance_size),
            "networkInterfaces": [
                {
                    "network": "global/networks/{}".format(vpc_name),
                    "subnetwork": "regions/{}/subnetworks/{}".format(region, subnet_name),
                    "accessConfigs": [
                        {
                            "type": "ONE_TO_ONE_NAT"
                        }
                    ]
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
            "disks": [
                {
                    "deviceName": instance_name,
                    "autoDelete": 'true',
                    "initializeParams": {
                        "diskSizeGb": "10",
                        "sourceImage": "/projects/ubuntu-os-cloud/global/images/ubuntu-1604-xenial-v20170502"
                    },
                    "boot": 'true',
                    "mode": "READ_WRITE"
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
            time.sleep(60)
            print('Instance {} created.'.format(instance_name))
            return result
        except Exception as err:
                logging.info(
                    "Unable to create Instance: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to create Instance",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def remove_instance(self, instance_name):
        request = self.service.instances().delete(project=self.project, zone=os.environ['zone'], instance=instance_name)
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
