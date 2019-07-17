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

from pprint import pprint
from googleapiclient.discovery import build
from google.cloud import storage
from google.cloud import exceptions
import google.auth
from dlab.fab import *
import actions_lib
import os, re
from googleapiclient import errors
import logging
import traceback
import sys, time
import backoff


class GCPMeta:
    def __init__(self, auth_type='service_account'):
        @backoff.on_exception(backoff.expo,
                              google.auth.exceptions.DefaultCredentialsError,
                              max_tries=15)
        def get_gcp_cred():
            credentials, project = google.auth.default()
            return credentials, project

        self.auth_type = auth_type
        self.project = os.environ['gcp_project_id']

        if os.environ['conf_resource'] == 'ssn':
            os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = "/root/service_account.json"
            credentials, project = google.auth.default()
            if credentials.requires_scopes:
                credentials = credentials.with_scopes(
                    ['https://www.googleapis.com/auth/compute',
                     'https://www.googleapis.com/auth/iam',
                     'https://www.googleapis.com/auth/cloud-platform'])
            self.service = build('compute', 'v1', credentials=credentials)
            self.service_iam = build('iam', 'v1', credentials=credentials)
            self.dataproc = build('dataproc', 'v1', credentials=credentials)
            self.service_storage = build('storage', 'v1', credentials=credentials)
            self.storage_client = storage.Client(project=project, credentials=credentials)
            self.service_resource = build('cloudresourcemanager', 'v1', credentials=credentials)
        else:
            credentials, project = get_gcp_cred()
            self.service = build('compute', 'v1', credentials=credentials)
            self.service_iam = build('iam', 'v1', credentials=credentials)
            self.dataproc = build('dataproc', 'v1', credentials=credentials)
            self.service_storage = build('storage', 'v1', credentials=credentials)
            self.storage_client = storage.Client(project=project, credentials=credentials)
            self.service_resource = build('cloudresourcemanager', 'v1', credentials=credentials)

    def wait_for_operation(self, operation, region='', zone=''):
        print('Waiting for operation to finish...')
        execution = False
        while not execution:
            try:
                if region != '':
                    result = self.service.regionOperations().get(
                        project=self.project,
                        operation=operation,
                        region=region).execute()
                elif zone != '':
                    result = self.service.zoneOperations().get(
                        project=self.project,
                        operation=operation,
                        zone=zone).execute()
                else:
                    result = self.service.globalOperations().get(
                        project=self.project,
                        operation=operation).execute()
                if result['status'] == 'DONE':
                    print("Done.")
                    execution = True
                time.sleep(1)
            except errors.HttpError as err:
                if err.resp.status == 404:
                    print(err)
                else:
                    raise err

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
        request = self.service.instances().get(project=self.project, zone=os.environ['gcp_zone'],
                                               instance=instance_name)
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
        request = self.service.instances().get(project=self.project, zone=os.environ['gcp_zone'],
                                               instance=instance_name)
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
        request = self.service_iam.projects().roles().get(name='projects/{}/roles/{}'.format(self.project,
                                                                                             role_name.replace('-',
                                                                                                               '_')))
        try:
            return request.execute()
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
            logging.info(
                "Unable to get IAM role: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get IAM role",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_role_status(self, role_name):
        request = self.service_iam.projects().roles().list(parent='projects/{}'.format(self.project,),showDeleted='true').execute()
        rn = 'projects/{}/roles/{}'.format(self.project,role_name.replace('-','_'))
        try:
            for role in request['roles']:
                if role['name'] == rn:
                    return role['deleted']
        except errors.HttpError as err:
            if err.resp.status == 404:
                return ''
            else:
                raise err
        except Exception as err:
            logging.info(
                "Unable to get IAM role: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Unable to get IAM role",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

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
                "Unable to get Private IP address: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to get Private IP address",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_image_by_name(self, image_name):
        try:
            request = self.service.images().get(project=self.project, image=image_name)
            try:
                return request.execute()
            except errors.HttpError as err:
                if err.resp.status == 404:
                    return ''
                else:
                    raise err
        except Exception as err:
            logging.info("Error with getting image by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting image by name",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)

    def get_disk(self, disk_name):
        try:
            request = self.service.disks().get(project=self.project, zone=os.environ['gcp_zone'], disk=disk_name)
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting disk by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting disk by name",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_service_accounts(self):
        try:
            service_account_names = []
            result = self.service_iam.projects().serviceAccounts().list(
                name='projects/{}'.format(self.project)).execute()
            for account in result['accounts']:
                service_account_names.append(account['displayName'])
            if 'nextPageToken' in result:
                next_page = True
                page_token = result['nextPageToken']
            else:
                next_page = False
            while next_page:
                result2 = self.service_iam.projects().serviceAccounts().list(name='projects/{}'.format(self.project),
                                                                             pageToken=page_token).execute()
                if result2:
                    for account in result2['accounts']:
                        service_account_names.append(account['displayName'])
                    if 'nextPageToken' in result2:
                        page_token = result2['nextPageToken']
                    else:
                        next_page = False
                else:
                    next_page = False
            return service_account_names
        except Exception as err:
            logging.info("Error with getting list service accounts: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error with getting list service accounts",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_roles(self):
        try:
            role_names = []
            result = self.service_iam.projects().roles().list(parent='projects/{}'.format(self.project)).execute()
            for role in result['roles']:
                role_names.append(role['title'])
            if 'nextPageToken' in result:
                next_page = True
                page_token = result['nextPageToken']
            else:
                next_page = False
            while next_page:
                result2 = self.service_iam.projects().roles().list(parent='projects/{}'.format(self.project),
                                                                   pageToken=page_token).execute()
                for role in result2['roles']:
                    role_names.append(role['title'])
                if 'nextPageToken' in result2:
                    page_token = result2['nextPageToken']
                else:
                    next_page = False
            return role_names
        except Exception as err:
            logging.info("Error with getting list service accounts: " + str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout))
            append_result(str({"error": "Error with getting list service accounts",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_instances(self, zone, filter_string=''):
        try:
            if not filter_string:
                raise Exception("There are no filter_string was added for list instances")
            else:
                request = self.service.instances().list(project=self.project, zone=zone, filter='name eq {}-.*'.
                                                        format(filter_string))
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting list instances: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting list instances",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_instances_by_label(self, zone, filter_string=''):
        try:
            if not filter_string:
                raise Exception("There are no filter_string was added for list instances by label")
            else:
                request = self.service.instances().list(project=self.project, zone=zone,
                                                        filter='labels.notebook_name eq {}'.format(filter_string))
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting list instances by label: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting list instances by label",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''


    def get_list_images(self, filter_string=''):
        try:
            if not filter_string:
                raise Exception("There are no filter_string was added for list images")
            else:
                request = self.service.images().list(project=self.project, filter='name eq {}-.*'.
                                                        format(filter_string))
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting list instances: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting list instances",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_firewalls(self, filter_string=''):
        try:
            if not filter_string:
                raise Exception("There are no filter_string was added for list firewalls")
            else:
                request = self.service.firewalls().list(project=self.project, filter='name eq {}.*'.format(
                    filter_string))
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting list firewalls: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting list firewalls",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_subnetworks(self, region, vpc_name='', filter_string=''):
        try:
            if not filter_string and not vpc_name:
                raise Exception("There are no filter_string or vpc_name was added for list subnetworks")
            elif vpc_name and not filter_string:
                request = self.service.subnetworks().list(
                    project=self.project, region=region,
                    filter=
                    '(network eq https://www.googleapis.com/compute/v1/projects/{}/global/networks/{}) (name eq .*)'.format(
                        self.project, vpc_name))
            elif filter_string and vpc_name:
                request = self.service.subnetworks().list(
                    project=self.project, region=region,
                    filter=
                    '(network eq https://www.googleapis.com/compute/v1/projects/{}/global/networks/{}) (name eq {}-.*)'.format(
                        self.project, vpc_name, filter_string))
            elif filter_string and not vpc_name:
                request = self.service.subnetworks().list(
                    project=self.project, region=region,
                    filter='name eq {}-.*'.format(filter_string))
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting list subnetworks: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting list subnetworks",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_buckets(self, prefix=''):
        try:
            if not prefix:
                raise Exception("There are no prefix was added for list buckets")
            else:
                request = self.service_storage.buckets().list(project=self.project, prefix='{}'.format(prefix))
            result = request.execute()
            return result
        except Exception as err:
            logging.info("Error with getting list buckets: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
            append_result(str({"error": "Error with getting list buckets",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_static_addresses(self, region, filter_string=''):
        try:
            if not filter_string:
                raise Exception("There are no filter_string was added for list static adress")
            else:
                request = self.service.addresses().list(project=self.project, region=region,
                                                        filter='name eq {}.*'.format(filter_string))
            result = request.execute()
            return result
        except Exception as err:
            logging.info(
                "Error with getting list static addresses: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Error with getting list static addresses",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_instance_statuses(self, instance_name_list):
        data = []
        for instance in instance_name_list:
            host = {}
            try:
                request = self.service.instances().get(project=self.project, zone=os.environ['gcp_zone'],
                                                       instance=instance)
                result = request.execute()
                host['id'] = instance
                host['status'] = result.get('status').lower().replace("terminated", "stopped")
                data.append(host)
            except:
                host['id'] = instance
                host['status'] = 'terminated'
                data.append(host)
        return data

    def get_list_cluster_statuses(self, cluster_names, full_check=True):
        data = []
        for cluster in cluster_names:
            host = {}
            try:
                request = self.dataproc.projects().regions().clusters().get(projectId=self.project,
                                                                            region=os.environ['gcp_region'],
                                                                            clusterName=cluster)
                result = request.execute()
                host['id'] = cluster
                if full_check:
                    host['version'] = result.get('config').get('softwareConfig').get('imageVersion')[:3]
                host['status'] = result.get('status').get('state').lower()
                data.append(host)
            except:
                host['id'] = cluster
                host['status'] = 'terminated'
                data.append(host)
        return data

    def get_cluster(self, cluster_name):
        try:
            request = self.dataproc.projects().regions().clusters().get(projectId=self.project,
                                                                        region=os.environ['gcp_region'],
                                                                        clusterName=cluster_name)
            result = request.execute()
            return result
        except Exception as err:
            logging.info(
                "Unable to get Dataproc: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to get Dataproc",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_dataproc_job_status(self, job_id):
        request = self.dataproc.projects().regions().jobs().get(projectId=self.project,
                                                                region=os.environ['gcp_region'],
                                                                jobId=job_id)
        try:
            res = request.execute()
            print("Job status: {}".format(res['status']['state'].lower()))
            return res['status']['state'].lower()
        except Exception as err:
            logging.info(
                "Unable to get Dataproc job status: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to get Dataproc job status",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_dataproc_list(self, labels):
        filter_string = ''
        for label in labels:
            for key in label.keys():
                filter_string += 'labels.{}:{}'.format(key, label[key])
            filter_string += ' AND '

        filter_string = re.sub('AND $', '', filter_string)
        request = self.dataproc.projects().regions().clusters().list(projectId=self.project,
                                                                     region=os.environ['gcp_region'],
                                                                     filter=filter_string)
        try:
            res = request.execute()
            if res != dict():
                return [i['clusterName'] for i in res['clusters']]
            else:
                return ''
        except Exception as err:
            logging.info(
                "Unable to get Dataproc list clusters: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Unable to get Dataproc list clusters",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_not_configured_dataproc(self, notebook_instance_name):
        cluster_filter = 'labels.{}:not-configured'.format(notebook_instance_name)
        request = self.dataproc.projects().regions().clusters().list(projectId=self.project,
                                                                     region=os.environ['gcp_region'],
                                                                     filter=cluster_filter)
        try:
            res = request.execute()
            if res != dict():
                return res['clusters'][0]['clusterName']
            else:
                print("No not-configured clusters")
                return ''
        except Exception as err:
            logging.info(
                "Error with getting not configured cluster: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Error with getting not configured cluster",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_dataproc_jobs(self):
        jobs = []
        try:
            res = self.dataproc.projects().regions().jobs().list(projectId=self.project,
                                                                 region=os.environ['gcp_region']).execute()
            jobs = [job for job in res['jobs']]
            page_token = res.get('nextPageToken')
            while page_token != 'None':
                res2 = self.dataproc.projects().regions().jobs().list(projectId=self.project,
                                                                      region=os.environ['gcp_region'],
                                                                      pageToken=page_token).execute()
                jobs.extend([job for job in res2['jobs']])
                page_token = str(res2.get('nextPageToken'))
            return jobs
        except KeyError:
            return jobs
        except Exception as err:
            logging.info(
                "Error with getting cluster jobs: " + str(err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Error with getting cluster jobs",
                               "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)
            return ''

    def get_list_private_ip_by_conf_type_and_id(self, conf_type, instance_id):
        try:
            private_list_ip = []
            if conf_type == 'edge_node' or conf_type == 'exploratory':
                private_list_ip.append(GCPMeta().get_private_ip_address(
                instance_id))
            elif conf_type == 'computational_resource':
                instance_list = GCPMeta().get_list_instances_by_label(
                    os.environ['gcp_zone'], instance_id)
                for instance in instance_list.get('items'):
                    private_list_ip.append(instance.get('networkInterfaces')[0].get('networkIP'))
            return private_list_ip
        except Exception as err:
            logging.info(
                "Error getting private ip by conf_type and id: " + str(
                    err) + "\n Traceback: " + traceback.print_exc(
                    file=sys.stdout))
            append_result(str({"error": "Error getting private ip by conf_type and id",
                               "error_message": str(
                                   err) + "\n Traceback: " + traceback.print_exc(
                                   file=sys.stdout)}))
            traceback.print_exc(file=sys.stdout)


def get_instance_private_ip_address(tag_name, instance_name):
    try:
        return GCPMeta().get_private_ip_address(instance_name)
    except Exception as err:
        logging.info(
            "Error with getting private ip address by name: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
        append_result(str({"error": "Error with getting private ip address by name",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return ''


def node_count(cluster_name):
    try:
        list_instances = GCPMeta().get_list_instances(os.environ['gcp_zone'], cluster_name)
        if list_instances.get('items') is None:
            raise Exception
        else:
            return len(list_instances.get('items'))
    except Exception as err:
        logging.info(
            "Error with getting node count: " + str(err) + "\n Traceback: " + traceback.print_exc(
                file=sys.stdout))
        append_result(str({"error": "Error with getting noide count",
                           "error_message": str(err) + "\n Traceback: " + traceback.print_exc(file=sys.stdout)}))
        traceback.print_exc(file=sys.stdout)
        return ''

