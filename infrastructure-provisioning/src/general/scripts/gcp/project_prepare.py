#!/usr/bin/python3

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

import datalab.fab
import datalab.actions_lib
import datalab.meta_lib
import json
from datalab.logger import logging
import os
import sys
import traceback
import uuid
import subprocess
from fabric import *

if __name__ == "__main__":
    try:
        GCPMeta = datalab.meta_lib.GCPMeta()
        GCPActions = datalab.actions_lib.GCPActions()
        logging.info('Generating infrastructure names and tags')
        project_conf = dict()
        project_conf['edge_unique_index'] = str(uuid.uuid4())[:5]
        project_conf['ps_unique_index'] = str(uuid.uuid4())[:5]
        project_conf['service_base_name'] = (os.environ['conf_service_base_name'])
        project_conf['key_name'] = os.environ['conf_key_name']
        project_conf['project_name'] = (os.environ['project_name']).replace('_', '-').lower()
        project_conf['user_keyname'] = project_conf['project_name']
        project_conf['project_tag'] = (project_conf['project_name'])
        project_conf['endpoint_name'] = (os.environ['endpoint_name']).replace('_', '-').lower()
        project_conf['endpoint_tag'] = project_conf['endpoint_name']
        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
            else:
                project_conf['vpc_name'] = os.environ['gcp_vpc_name']
        except KeyError:
            project_conf['vpc_name'] = project_conf['service_base_name'] + '-vpc'
        project_conf['vpc_cidr'] = os.environ['conf_vpc_cidr']
        project_conf['private_subnet_name'] = '{0}-{1}-{2}-subnet'.format(project_conf['service_base_name'],
                                                                          project_conf['project_name'],
                                                                          project_conf['endpoint_name'])
        project_conf['subnet_name'] = os.environ['gcp_subnet_name']
        project_conf['region'] = os.environ['gcp_region']
        project_conf['zone'] = os.environ['gcp_zone']
        project_conf['vpc_selflink'] = GCPMeta.get_vpc(project_conf['vpc_name'])['selfLink']
        project_conf['private_subnet_prefix'] = os.environ['conf_private_subnet_prefix']
        project_conf['edge_service_account_name'] = '{}-{}-{}-edge-sa'.format(project_conf['service_base_name'],
                                                                              project_conf['project_name'],
                                                                              project_conf['endpoint_name'])
        project_conf['edge_role_name'] = '{}-{}-{}-{}-edge-role'.format(project_conf['service_base_name'],
                                                                        project_conf['project_name'],
                                                                        project_conf['endpoint_name'],
                                                                        project_conf['edge_unique_index'])
        project_conf['ps_service_account_name'] = '{}-{}-{}-ps-sa'.format(project_conf['service_base_name'],
                                                                          project_conf['project_name'],
                                                                          project_conf['endpoint_name'])
        project_conf['ps_role_name'] = '{}-{}-{}-{}-ps-role'.format(project_conf['service_base_name'],
                                                                    project_conf['project_name'],
                                                                    project_conf['endpoint_name'],
                                                                    project_conf['ps_unique_index'])
        project_conf['ps_policy_path'] = '/root/files/ps_policy.json'
        project_conf['ps_roles_path'] = '/root/files/ps_roles.json'
        project_conf['instance_name'] = '{0}-{1}-{2}-edge'.format(project_conf['service_base_name'],
                                                                  project_conf['project_name'],
                                                                  project_conf['endpoint_name'])
        project_conf['ssn_instance_name'] = '{}-ssn'.format(project_conf['service_base_name'])
        project_conf['bucket_name'] = '{0}-{1}-{2}-bucket'.format(project_conf['service_base_name'],
                                                                  project_conf['project_name'],
                                                                  project_conf['endpoint_name'])
        project_conf['shared_bucket_name'] = '{0}-{1}-shared-bucket'.format(project_conf['service_base_name'],
                                                                            project_conf['endpoint_name'])
        project_conf['instance_size'] = os.environ['gcp_edge_instance_size']
        project_conf['ssh_key_path'] = '{0}{1}.pem'.format(os.environ['conf_key_dir'], os.environ['conf_key_name'])
        project_conf['image_name'] = os.environ['gcp_{}_image_name'.format(os.environ['conf_os_family'])]
        project_conf['static_address_name'] = '{0}-{1}-{2}-static-ip'.format(project_conf['service_base_name'],
                                                                             project_conf['project_name'],
                                                                             project_conf['endpoint_name'])
        project_conf['fw_edge_ingress_public'] = '{}-sg-ingress-public'.format(project_conf['instance_name'])
        project_conf['fw_edge_ingress_internal'] = '{}-sg-ingress-internal'.format(project_conf['instance_name'])
        project_conf['fw_edge_egress_public'] = '{}-sg-egress-public'.format(project_conf['instance_name'])
        project_conf['fw_edge_egress_internal'] = '{}-sg-egress-internal'.format(project_conf['instance_name'])
        project_conf['ps_firewall_target'] = '{0}-{1}-{2}-ps'.format(project_conf['service_base_name'],
                                                                     project_conf['project_name'],
                                                                     project_conf['endpoint_name'])
        project_conf['fw_common_name'] = '{}-{}-{}-ps'.format(project_conf['service_base_name'],
                                                              project_conf['project_name'],
                                                              project_conf['endpoint_name'])
        project_conf['fw_ps_ingress'] = '{}-sg-ingress'.format(project_conf['fw_common_name'])
        project_conf['fw_ps_egress_private'] = '{}-sg-egress-private'.format(project_conf['fw_common_name'])
        project_conf['fw_ps_egress_public'] = '{}-sg-egress-public'.format(project_conf['fw_common_name'])
        project_conf['network_tags'] = list(os.environ['gcp_additional_network_tag'].split(","))
        project_conf['network_tags'].append(project_conf['instance_name'])
        project_conf['network_tags'].append('edge')
        project_conf['network_tags'].append('datalab')
        project_conf['instance_labels'] = {"name": project_conf['instance_name'],
                                           "sbn": project_conf['service_base_name'],
                                           "project_tag": project_conf['project_tag'],
                                           "endpoint_tag": project_conf['endpoint_tag'],
                                           "product": "datalab"}
        project_conf['tag_name'] = project_conf['service_base_name'] + '-tag'
        project_conf['allowed_ip_cidr'] = os.environ['conf_allowed_ip_cidr']
        if 'conf_user_subnets_range' in os.environ:
            project_conf['user_subnets_range'] = os.environ['conf_user_subnets_range']
        else:
            project_conf['user_subnets_range'] = ''

        project_conf['gcp_bucket_enable_versioning'] = os.environ['conf_bucket_versioning_enabled']
        if 'gcp_cmek_resource_name' in os.environ:
            project_conf['gcp_cmek_resource_name'] = os.environ['gcp_cmek_resource_name']
        else:
            project_conf['gcp_cmek_resource_name'] = ''

        if 'gcp_storage_lifecycle_rules' in os.environ:
            project_conf['gcp_storage_lifecycle_rules'] = os.environ['gcp_storage_lifecycle_rules']
        else:
            project_conf['gcp_storage_lifecycle_rules'] = ''

        # FUSE in case of absence of user's key
        try:
            project_conf['user_key'] = os.environ['key']
            try:
                subprocess.run('echo "{0}" >> {1}{2}.pub'.format(project_conf['user_key'], os.environ['conf_key_dir'],
                                                        project_conf['project_name']), shell=True, check=True)
            except:
                logging.info("ADMINSs PUBLIC KEY DOES NOT INSTALLED")
        except KeyError:
            logging.info("ADMINSs PUBLIC KEY DOES NOT UPLOADED")
            sys.exit(1)

        logging.info("Will create exploratory environment with edge node as access point as following: ".format(json.dumps(
            project_conf, sort_keys=True, indent=4, separators=(',', ': '))))
        logging.info(json.dumps(project_conf))
    except Exception as err:
        datalab.fab.append_result("Failed to generate infrastructure names", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE SUBNET]')
        params = "--subnet_name {} --region {} --vpc_selflink {} --prefix {} --vpc_cidr {} --user_subnets_range '{}'" \
                 .format(project_conf['private_subnet_name'], project_conf['region'], project_conf['vpc_selflink'],
                         project_conf['private_subnet_prefix'], project_conf['vpc_cidr'],
                         project_conf['user_subnets_range'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_subnet', params), shell=True, check=True)
            project_conf['private_subnet_cidr'] = GCPMeta.get_subnet(project_conf['private_subnet_name'],
                                                                     project_conf['region'])['ipCidrRange']
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        except:
            logging.info("Subnet hasn't been created.")
        datalab.fab.append_result("Failed to create subnet.", str(err))
        sys.exit(1)

    logging.info('NEW SUBNET CIDR CREATED: {}'.format(project_conf['private_subnet_cidr']))

    try:
        logging.info('[CREATE SERVICE ACCOUNT AND ROLE FOR EDGE NODE]')
        params = "--service_account_name {} --role_name {} --unique_index {} --service_base_name {}".format(
            project_conf['edge_service_account_name'], project_conf['edge_role_name'],
            project_conf['edge_unique_index'], project_conf['service_base_name'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_service_account', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                              project_conf['service_base_name'])
            GCPActions.remove_role(project_conf['edge_role_name'])
        except:
            logging.info("Service account or role hasn't been created")
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        datalab.fab.append_result("Failed to creating service account and role.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE SERVICE ACCOUNT AND ROLE FOR PRIVATE SUBNET]')
        params = "--service_account_name {} --role_name {} --policy_path {} --roles_path {} --unique_index {} " \
                 "--service_base_name {}".format(
                  project_conf['ps_service_account_name'], project_conf['ps_role_name'], project_conf['ps_policy_path'],
                  project_conf['ps_roles_path'], project_conf['ps_unique_index'], project_conf['service_base_name'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_service_account', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        try:
            GCPActions.remove_service_account(project_conf['ps_service_account_name'],
                                              project_conf['service_base_name'])
            GCPActions.remove_role(project_conf['ps_role_name'])
        except:
            logging.info("Service account or role hasn't been created")
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        datalab.fab.append_result("Failed to creating service account and role.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATE FIREWALL FOR EDGE NODE]')
        firewall_rules = dict()
        firewall_rules['ingress'] = []
        firewall_rules['egress'] = []
        # ingress_rule = dict()
        # if os.environ['conf_allowed_ip_cidr'] != '0.0.0.0/0' and project_conf['endpoint_name'] == 'local':
        #     ssn_public_ip = GCPMeta.get_instance_public_ip_by_name('{}-ssn'.format(project_conf['service_base_name']))
        #     project_conf['allowed_ip_cidr'] = '{}, {}/32'.format(project_conf['allowed_ip_cidr'], ssn_public_ip).split(', ')
        # elif os.environ['conf_allowed_ip_cidr'] != '0.0.0.0/0' and project_conf['endpoint_name'] != 'local':
        #     endpoint_public_ip = GCPMeta.get_instance_public_ip_by_name('{}-{}-endpoint'.format(project_conf['service_base_name'], project_conf['endpoint_name']))
        #     project_conf['allowed_ip_cidr'] = '{}, {}/32'.format(project_conf['allowed_ip_cidr'], endpoint_public_ip).split(', ')
        # else:
        #     project_conf['allowed_ip_cidr'] = [os.environ['conf_allowed_ip_cidr']]
        # ingress_rule['name'] = project_conf['fw_edge_ingress_public']
        # ingress_rule['targetTags'] = [project_conf['instance_name']]
        # ingress_rule['sourceRanges'] = project_conf['allowed_ip_cidr']
        # rules = [
        #     {
        #         'IPProtocol': 'tcp',
        #         'ports': ['22', '80', '443', '3128']
        #     }
        # ]
        # ingress_rule['allowed'] = rules
        # ingress_rule['network'] = project_conf['vpc_selflink']
        # ingress_rule['direction'] = 'INGRESS'
        # firewall_rules['ingress'].append(ingress_rule)

        ingress_rule = dict()
        ingress_rule['name'] = project_conf['fw_edge_ingress_internal']
        ingress_rule['targetTags'] = [project_conf['instance_name']]
        if os.environ['gcp_subnet_name']:
            project_conf['ssn_subnet'] = os.environ['gcp_subnet_name']
        else:
            project_conf['ssn_subnet'] = '{}-subnet'.format(project_conf['service_base_name'])
        ingress_rule['sourceRanges'] = [project_conf['private_subnet_cidr'],
                                        GCPMeta.get_subnet(project_conf['subnet_name'],
                                                           project_conf['region'])['ipCidrRange']]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        ingress_rule['allowed'] = rules
        ingress_rule['network'] = project_conf['vpc_selflink']
        ingress_rule['direction'] = 'INGRESS'
        firewall_rules['ingress'].append(ingress_rule)

        egress_rule = dict()
        egress_rule['name'] = project_conf['fw_edge_egress_public']
        egress_rule['targetTags'] = [project_conf['instance_name']]
        egress_rule['destinationRanges'] = project_conf['allowed_ip_cidr']
        rules = [
            {
                'IPProtocol': 'udp',
                'ports': ['53', '123']
            },
            {
                'IPProtocol': 'tcp',
                'ports': ['22', '80', '443']
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = project_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        egress_rule = dict()
        egress_rule['name'] = project_conf['fw_edge_egress_internal']
        egress_rule['targetTags'] = [project_conf['instance_name']]
        egress_rule['destinationRanges'] = [project_conf['private_subnet_cidr']]
        rules = [
            {
                'IPProtocol': 'tcp',
                'ports': ['22', '389', '8888', '8080', '8787', '6006', '20888', '8042', '8088', '18080', '50070',
                          '8085', '8081', '4040-4045']
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = project_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        params = "--firewall '{}'".format(json.dumps(firewall_rules))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_firewall', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        GCPActions.remove_service_account(project_conf['ps_service_account_name'], project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['ps_role_name'])
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        datalab.fab.append_result("Failed to create firewall for Edge node.", str(err))
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATE FIREWALL FOR PRIVATE SUBNET]')
        firewall_rules = dict()
        firewall_rules['ingress'] = []
        firewall_rules['egress'] = []

        ingress_rule = dict()
        ingress_rule['name'] = project_conf['fw_ps_ingress']
        ingress_rule['targetTags'] = [
            project_conf['ps_firewall_target']
        ]
        ingress_rule['sourceRanges'] = [project_conf['private_subnet_cidr'],
                                        GCPMeta.get_subnet(project_conf['subnet_name'],
                                                           project_conf['region'])['ipCidrRange']
                                        ]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        ingress_rule['allowed'] = rules
        ingress_rule['network'] = project_conf['vpc_selflink']
        ingress_rule['direction'] = 'INGRESS'
        firewall_rules['ingress'].append(ingress_rule)

        egress_rule = dict()
        egress_rule['name'] = project_conf['fw_ps_egress_private']
        egress_rule['targetTags'] = [
            project_conf['ps_firewall_target']
        ]
        egress_rule['destinationRanges'] = [project_conf['private_subnet_cidr'],
                                            GCPMeta.get_subnet(project_conf['subnet_name'],
                                                               project_conf['region'])['ipCidrRange']
                                            ]
        rules = [
            {
                'IPProtocol': 'all'
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = project_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        egress_rule = dict()
        egress_rule['name'] = project_conf['fw_ps_egress_public']
        egress_rule['targetTags'] = [
            project_conf['ps_firewall_target']
        ]
        egress_rule['destinationRanges'] = project_conf['allowed_ip_cidr']
        rules = [
            {
                'IPProtocol': 'tcp',
                'ports': ['443']
            }
        ]
        egress_rule['allowed'] = rules
        egress_rule['network'] = project_conf['vpc_selflink']
        egress_rule['direction'] = 'EGRESS'
        firewall_rules['egress'].append(egress_rule)

        params = "--firewall '{}'".format(json.dumps(firewall_rules))
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_firewall', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create firewall for private subnet.", str(err))
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_internal'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_internal'])
        GCPActions.remove_service_account(project_conf['ps_service_account_name'], project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['ps_role_name'])
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATE BUCKETS]')
        project_conf['shared_bucket_tags'] = {
            project_conf['tag_name']: project_conf['shared_bucket_name'],
            "endpoint_tag": project_conf['endpoint_tag'],
            os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value'],
            "sbn": project_conf['service_base_name'],
            "name": project_conf['shared_bucket_name']}
        params = "--bucket_name {} --tags '{}' --versioning_enabled {} --lifecycle_rules '{}'".format(
            project_conf['shared_bucket_name'], json.dumps(project_conf['shared_bucket_tags']),
            project_conf['gcp_bucket_enable_versioning'], json.dumps(project_conf['gcp_storage_lifecycle_rules']))

        if project_conf['gcp_cmek_resource_name'] != '':
            params = '{} --cmek_resource_name {}'.format(params, project_conf['gcp_cmek_resource_name'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_bucket', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception

        project_conf['bucket_tags'] = {
            project_conf['tag_name']: project_conf['bucket_name'],
            "endpoint_tag": project_conf['endpoint_tag'],
            os.environ['conf_billing_tag_key']: os.environ['conf_billing_tag_value'],
            "sbn": project_conf['service_base_name'],
            "project_tag": project_conf['project_tag'],
            "name": project_conf['bucket_name']}
        params = "--bucket_name {} --tags '{}' --versioning_enabled {} --lifecycle_rules '{}'".format(
            project_conf['bucket_name'], json.dumps(project_conf['bucket_tags']),
            project_conf['gcp_bucket_enable_versioning'], json.dumps(project_conf['gcp_storage_lifecycle_rules']))

        if project_conf['gcp_cmek_resource_name'] != '':
            params = '{} --cmek_resource_name {}'.format(params, project_conf['gcp_cmek_resource_name'])

        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_bucket', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Unable to create bucket.", str(err))
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_internal'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_internal'])
        GCPActions.remove_firewall(project_conf['fw_ps_ingress'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_private'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_public'])
        GCPActions.remove_service_account(project_conf['ps_service_account_name'], project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['ps_role_name'])
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        sys.exit(1)

    try:
        logging.info('[SET PERMISSIONS FOR USER AND SHARED BUCKETS]')
        GCPActions.set_bucket_owner(project_conf['bucket_name'], project_conf['ps_service_account_name'],
                                    project_conf['service_base_name'])
        GCPActions.set_bucket_owner(project_conf['shared_bucket_name'], project_conf['ps_service_account_name'],
                                    project_conf['service_base_name'])
    except Exception as err:
        datalab.fab.append_result("Failed to set bucket permissions.", str(err))
        GCPActions.remove_bucket(project_conf['bucket_name'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_internal'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_internal'])
        GCPActions.remove_firewall(project_conf['fw_ps_ingress'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_private'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_public'])
        GCPActions.remove_service_account(project_conf['ps_service_account_name'], project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['ps_role_name'])
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        sys.exit(1)

    try:
        logging.info('[CREATING STATIC IP ADDRESS]')
        params = "--address_name {} --region {}".format(project_conf['static_address_name'], project_conf['region'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('edge_create_static_ip', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create static ip.", str(err))
        try:
            GCPActions.remove_static_address(project_conf['static_address_name'], project_conf['region'])
        except:
            logging.info("Static IP address hasn't been created.")
        GCPActions.remove_bucket(project_conf['bucket_name'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_internal'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_internal'])
        GCPActions.remove_firewall(project_conf['fw_ps_ingress'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_private'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_public'])
        GCPActions.remove_service_account(project_conf['ps_service_account_name'], project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['ps_role_name'])
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        sys.exit(1)

    if os.environ['conf_os_family'] == 'debian':
        project_conf['initial_user'] = 'ubuntu'
        project_conf['sudo_group'] = 'sudo'
    if os.environ['conf_os_family'] == 'redhat':
        project_conf['initial_user'] = 'ec2-user'
        project_conf['sudo_group'] = 'wheel'

    project_conf['gcp_os_login_enabled'] = os.environ['gcp_os_login_enabled']
    project_conf['gcp_block_project_ssh_keys'] = os.environ['gcp_block_project_ssh_keys']
    if "gcp_wrapped_csek" in os.environ:
        project_conf['gcp_wrapped_csek'] = os.environ['gcp_wrapped_csek']
    else:
        project_conf['gcp_wrapped_csek'] = ''

    try:
        project_conf['static_ip'] = \
            GCPMeta.get_static_address(project_conf['region'], project_conf['static_address_name'])['address']
        logging.info('[CREATE EDGE INSTANCE]')
        params = "--instance_name {} --region {} --zone {} --vpc_name {} --subnet_name {} --instance_size {} " \
                 "--ssh_key_path {} --initial_user {} --service_account_name {} --image_name {} --instance_class {} " \
                 "--static_ip {} --network_tag {} --labels '{}' --service_base_name {} --os_login_enabled {} " \
                 "--block_project_ssh_keys {} --rsa_encrypted_csek '{}'".format(
                  project_conf['instance_name'], project_conf['region'], project_conf['zone'], project_conf['vpc_name'],
                  project_conf['subnet_name'], project_conf['instance_size'], project_conf['ssh_key_path'],
                  project_conf['initial_user'], project_conf['edge_service_account_name'], project_conf['image_name'],
                  'edge', project_conf['static_ip'], ','.join(project_conf['network_tags']),
                  json.dumps(project_conf['instance_labels']), project_conf['service_base_name'],
                  project_conf['gcp_os_login_enabled'], project_conf['gcp_block_project_ssh_keys'],
                  project_conf['gcp_wrapped_csek'])
        try:
            subprocess.run("~/scripts/{}.py {}".format('common_create_instance', params), shell=True, check=True)
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        datalab.fab.append_result("Failed to create instance.", str(err))
        GCPActions.remove_static_address(project_conf['static_address_name'], project_conf['region'])
        GCPActions.remove_bucket(project_conf['bucket_name'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_ingress_internal'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_public'])
        GCPActions.remove_firewall(project_conf['fw_edge_egress_internal'])
        GCPActions.remove_firewall(project_conf['fw_ps_ingress'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_private'])
        GCPActions.remove_firewall(project_conf['fw_ps_egress_public'])
        GCPActions.remove_service_account(project_conf['ps_service_account_name'], project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['ps_role_name'])
        GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                          project_conf['service_base_name'])
        GCPActions.remove_role(project_conf['edge_role_name'])
        GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
        sys.exit(1)

    if os.environ['edge_is_nat'] == 'true':
        try:
            logging.info('[CREATE NAT ROUTE]')
            nat_route_name = '{0}-{1}-{2}-nat-route'.format(project_conf['service_base_name'],
                                                                  project_conf['project_name'],
                                                                  project_conf['endpoint_name'])
            edge_instance = GCPMeta.get_instance(project_conf['instance_name'])['selfLink']
            params = "--nat_route_name {} --vpc {} --tag {} --edge_instance {}".format(nat_route_name,
                                                                                                       project_conf['vpc_selflink'],
                                                                                                       project_conf['ps_firewall_target'],
                                                                                                       edge_instance)
            try:
                subprocess.run("~/scripts/{}.py {}".format('common_create_nat_route', params), shell=True, check=True)
            except:
                traceback.print_exc()
                raise Exception
        except Exception as err:
            datalab.fab.append_result("Failed to create nat route.", str(err))
            GCPActions.remove_instance(project_conf['instance_name'], project_conf['zone'])
            GCPActions.remove_static_address(project_conf['static_address_name'], project_conf['region'])
            GCPActions.remove_bucket(project_conf['bucket_name'])
            GCPActions.remove_firewall(project_conf['fw_edge_ingress_public'])
            GCPActions.remove_firewall(project_conf['fw_edge_ingress_internal'])
            GCPActions.remove_firewall(project_conf['fw_edge_egress_public'])
            GCPActions.remove_firewall(project_conf['fw_edge_egress_internal'])
            GCPActions.remove_firewall(project_conf['fw_ps_ingress'])
            GCPActions.remove_firewall(project_conf['fw_ps_egress_private'])
            GCPActions.remove_firewall(project_conf['fw_ps_egress_public'])
            GCPActions.remove_service_account(project_conf['ps_service_account_name'],
                                              project_conf['service_base_name'])
            GCPActions.remove_role(project_conf['ps_role_name'])
            GCPActions.remove_service_account(project_conf['edge_service_account_name'],
                                              project_conf['service_base_name'])
            GCPActions.remove_role(project_conf['edge_role_name'])
            GCPActions.remove_subnet(project_conf['private_subnet_name'], project_conf['region'])
            sys.exit(1)