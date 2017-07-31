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
from google.cloud import exceptions
import actions_lib
import os
from googleapiclient import errors
import logging
import traceback
import sys, time


class GCPMeta:
    def __init__(self, auth_type='service_account'):

        self.auth_type = auth_type
        self.project = os.environ['gcp_project_id']
        if os.environ['conf_resource'] == 'ssn':
            self.key_file = '/root/service_account.json'
            credentials = ServiceAccountCredentials.from_json_keyfile_name(
                self.key_file, scopes=('https://www.googleapis.com/auth/compute', 'https://www.googleapis.com/auth/iam',
                                       'https://www.googleapis.com/auth/cloud-platform'))
            self.service = build('compute', 'v1', credentials = credentials)
            self.service_iam = build('iam', 'v1', credentials=credentials)
            self.storage_client = storage.Client.from_service_account_json('/root/service_account.json')
        else:
            self.service = build('compute', 'v1')
            self.service_iam = build('iam', 'v1')
            self.storage_client = storage.Client()

    def get_vpc(self, network_name):
        request = self.service.networks().get(
            project=self.project,
            network=network_name
        )
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
                logging.info(
                    "Unable to get VPC: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get VPC",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_subnet(self, subnet_name, region):
        request = self.service.subnetworks().get(
            project=self.project,
            region=region,
            subnetwork=subnet_name)
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
                logging.info(
                    "Unable to get Subnet: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Subnet",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_firewall(self, firewall_name):
        request = self.service.firewalls().get(
            project=self.project,
            firewall=firewall_name)
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
                logging.info(
                    "Unable to get Firewall: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Firewall",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_bucket(self, bucket_name):
        try:
            bucket = self.storage_client.get_bucket(bucket_name)
            return bucket
        except exceptions.NotFound:
                return ''
        except Exception as err:
                logging.info(
                    "Unable to get Firewall: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Firewall",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_instance(self, instance_name):
        request = self.service.instances().get(project=self.project, zone=os.environ['gcp_zone'], instance=instance_name)
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
                logging.info(
                    "Unable to get Firewall: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Firewall",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_instance_status(self, instance_name):
        request = self.service.instances().get(project=self.project, zone=os.environ['gcp_zone'], instance=instance_name)
        try:
            result = request.execute()
            return result.get('status')
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
                logging.info(
                    "Unable to get Firewall: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Firewall",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_instance_public_ip_by_name(self, instance_name):
        try:
            result = GCPMeta().get_instance(instance_name)
            if result:
                for i in result.get('networkInterfaces'):
                    for j in i.get('accessConfigs'):
                        return j.get('natIP')
            else:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Instance IP: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get Instance IP",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_service_account(self, service_account_name):
        service_account_email = "{}@{}.iam.gserviceaccount.com".format(service_account_name, self.project)
        request = self.service_iam.projects().serviceAccounts().get(
            name='projects/{}/serviceAccounts/{}'.format(self.project, service_account_email))
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
                logging.info(
                    "Unable to get Service account: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Service account",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_role(self, role_name):
        print "Role name -> {}".format(role_name)
        return role_name

    def get_static_address(self, region, static_address_name):
        request = self.service.addresses().get(project=self.project, region=region, address=static_address_name)
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err

    def get_private_ip_address(self, instance_name):
        try:
            result = GCPMeta().get_instance(instance_name)
            for i in result['networkInterfaces']:
                return i['networkIP']
        except Exception as err:
                logging.info(
                    "Unable to get Private IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
                append_result(str({"error": "Unable to get Private IP address",
                                   "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                       file=sys.stdout)}))
                traceback.print_exc(file=sys.stdout)

    def get_ami_by_name(self, ami_name):
        try:
            request = self.service.images().get(project=self.project, image=ami_name)
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting image by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error with getting image by name",
                       "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_disk(self, disk_name):
        try:
            request = self.service.disks().get(project=self.project, zone=os.environ['gcp_zone'], disk=disk_name)
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting disk by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error with getting disk by name",
                       "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''


def get_instance_private_ip_address(tag_name, instance_name):
    try:
        return GCPMeta().get_private_ip_address(instance_name)
    except Exception as err:
            logging.info("Error with getting private ip address by name: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error with getting private ip address by name",
                       "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

