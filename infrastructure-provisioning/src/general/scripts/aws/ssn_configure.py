#!/usr/bin/python

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

from dlab.fab import *
from dlab.actions_lib import *
import sys, os, json
from fabric.api import *
from dlab.ssn_lib import *
import traceback

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'

    try:
        logging.info('[DERIVING NAMES]')
        print('[DERIVING NAMES]')
        service_base_name = os.environ['conf_service_base_name'] = replace_multi_symbols(
            os.environ['conf_service_base_name'].lower()[:12], '-', True)
        role_name = service_base_name.lower().replace('-', '_') + '-ssn-Role'
        role_profile_name = service_base_name.lower().replace('-', '_') + '-ssn-Profile'
        policy_name = service_base_name.lower().replace('-', '_') + '-ssn-Policy'
        ssn_bucket_name_tag = service_base_name + '-ssn-bucket'
        shared_bucket_name_tag = service_base_name + '-shared-bucket'
        ssn_bucket_name = ssn_bucket_name_tag.lower().replace('_', '-')
        shared_bucket_name = shared_bucket_name_tag.lower().replace('_', '-')
        tag_name = service_base_name + '-Tag'
        tag2_name = service_base_name + '-secondary-Tag'
        instance_name = service_base_name + '-ssn'
        region = os.environ['aws_region']
        ssn_image_name = os.environ['aws_{}_image_name'.format(os.environ['conf_os_family'])]
        ssn_ami_id = get_ami_id(ssn_image_name)
        policy_path = '/root/files/ssn_policy.json'
        vpc_cidr = os.environ['conf_vpc_cidr']
        vpc2_cidr = os.environ['conf_vpc2_cidr']
        sg_name = instance_name + '-sg'
        pre_defined_vpc = False
        pre_defined_subnet = False
        pre_defined_sg = False
        billing_enabled = True
        dlab_ssh_user = os.environ['conf_os_user']
        network_type = os.environ['conf_network_type']
        if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and \
                'ssn_subdomain' in os.environ:
            domain_created = True
        else:
            domain_created = False

        try:
            if os.environ['aws_vpc_id'] == '':
                raise KeyError
        except KeyError:
            tag = {"Key": tag_name, "Value": "{}-subnet".format(service_base_name)}
            os.environ['aws_vpc_id'] = get_vpc_by_tag(tag_name, service_base_name)
            pre_defined_vpc = True
        try:
            if os.environ['aws_subnet_id'] == '':
                raise KeyError
        except KeyError:
            tag = {"Key": tag_name, "Value": "{}-subnet".format(service_base_name)}
            os.environ['aws_subnet_id'] = get_subnet_by_tag(tag, True)
            pre_defined_subnet = True
        try:
            if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_vpc2_id']:
                raise KeyError
        except KeyError:
            tag = {"Key": tag2_name, "Value": "{}-subnet".format(service_base_name)}
            os.environ['aws_vpc2_id'] = get_vpc_by_tag(tag2_name, service_base_name)
            pre_defined_vpc2 = True
        try:
            if os.environ['conf_duo_vpc_enable'] == 'true' and not os.environ['aws_peering_id']:
                raise KeyError
        except KeyError:
            os.environ['aws_peering_id'] = get_peering_by_tag(tag_name, service_base_name)
            pre_defined_peering = True
        try:
            if os.environ['aws_security_groups_ids'] == '':
                raise KeyError
        except KeyError:
            os.environ['aws_security_groups_ids'] = get_security_group_by_name(sg_name)
            pre_defined_sg = True
        try:
            if os.environ['aws_account_id'] == '':
                raise KeyError
            if os.environ['aws_billing_bucket'] == '':
                raise KeyError
        except KeyError:
            billing_enabled = False
        if not billing_enabled:
            os.environ['aws_account_id'] = 'None'
            os.environ['aws_billing_bucket'] = 'None'
        try:
            if not os.environ['aws_report_path']:
                raise KeyError
        except KeyError:
            os.environ['aws_report_path'] = ''
    except Exception as err:
        print('Error: {0}'.format(err))
        sys.exit(1)

    try:
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'

        if network_type == 'private':
            instance_hostname = get_instance_ip_address(tag_name, instance_name).get('Private')
        else:
            instance_hostname = get_instance_hostname(tag_name, instance_name)

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (instance_hostname, os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem", initial_user,
             dlab_ssh_user, sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed creating ssh user 'dlab'.", str(err))
        if domain_created:
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
        remove_ec2(tag_name, instance_name)
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        print('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages 'boto3 backoff argparse fabric==1.14.0 awscli pymongo " \
                 "pyyaml jinja2' --user {} --region {}". \
            format(instance_hostname, os.environ['conf_key_dir'] + os.environ['conf_key_name'] + ".pem", dlab_ssh_user,
                   os.environ['aws_region'])

        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing software: pip, packages.", str(err))
        if domain_created:
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
        remove_ec2(tag_name, instance_name)
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        print('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/", "service_base_name": service_base_name,
                             "security_group_id": os.environ['aws_security_groups_ids'],
                             "vpc_id": os.environ['aws_vpc_id'], "subnet_id": os.environ['aws_subnet_id'],
                             "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --dlab_path {} " \
                 "--tag_resource_id {}".format(instance_hostname, "{}{}.pem".format(os.environ['conf_key_dir'],
                                                                                    os.environ['conf_key_name']),
                                               json.dumps(additional_config), dlab_ssh_user,
                                               os.environ['ssn_dlab_path'], os.environ['conf_tag_resource_id'])

        try:
            local("~/scripts/{}.py {}".format('configure_ssn_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed configuring ssn.", str(err))
        if domain_created:
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
        remove_ec2(tag_name, instance_name)
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        print('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "project", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "tensor-rstudio", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine-service", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --dlab_path {} " \
                 "--cloud_provider {} --region {}".format(instance_hostname,
                                                          "{}{}.pem".format(os.environ['conf_key_dir'],
                                                                            os.environ['conf_key_name']),
                                                          json.dumps(additional_config), os.environ['conf_os_family'],
                                                          dlab_ssh_user, os.environ['ssn_dlab_path'],
                                                          os.environ['conf_cloud_provider'], os.environ['aws_region'])

        try:
            local("~/scripts/{}.py {}".format('configure_docker', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Unable to configure docker.", str(err))
        if domain_created:
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
        remove_ec2(tag_name, instance_name)
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        # mongo_parameters = {
        #     "aws_region": os.environ['aws_region'],
        #     "aws_vpc_id": os.environ['aws_vpc_id'],
        #     "aws_subnet_id": os.environ['aws_subnet_id'],
        #     "conf_service_base_name": service_base_name,
        #     "aws_security_groups_ids": os.environ['aws_security_groups_ids'].replace(" ", ""),
        #     "conf_os_family": os.environ['conf_os_family'],
        #     "conf_tag_resource_id": os.environ['conf_tag_resource_id'],
        #     "conf_key_dir": os.environ['conf_key_dir'],
        #     "ssn_instance_size": os.environ['aws_ssn_instance_size'],
        #     "edge_instance_size": os.environ['aws_edge_instance_size']
        # }
        # if os.environ['conf_duo_vpc_enable'] == 'true':
        #     secondary_parameters = {
        #         "aws_notebook_vpc_id": os.environ['aws_vpc2_id'],
        #         "aws_notebook_subnet_id": os.environ['aws_subnet_id'],
        #         "aws_peering_id": os.environ['aws_peering_id']
        #     }
        # else:
        #     secondary_parameters = {
        #         "aws_notebook_vpc_id": os.environ['aws_vpc_id'],
        #         "aws_notebook_subnet_id": os.environ['aws_subnet_id'],
        #     }
        # mongo_parameters.update(secondary_parameters)
        cloud_params = [
            {
                'key': 'KEYCLOAK_REALM_NAME',
                'value': os.environ['keycloak_realm_name']
            },
            {
                'key': 'KEYCLOAK_AUTH_SERVER_URL',
                'value': os.environ['keycloak_auth_server_url']
            },
            {
                'key': 'KEYCLOAK_CLIENT_NAME',
                'value': os.environ['keycloak_client_name']
            },
            {
                'key': 'KEYCLOAK_CLIENT_SECRET',
                'value': os.environ['keycloak_client_secret']
            },
            {
                'key': 'CONF_OS',
                'value': os.environ['conf_os_family']
            },
            {
                'key': 'SERVICE_BASE_NAME',
                'value': os.environ['conf_service_base_name']
            },
            {
                'key': 'EDGE_INSTANCE_SIZE',
                'value': os.environ['aws_edge_instance_size']
            },
            {
                'key': 'SUBNET_ID',
                'value': os.environ['aws_subnet_id']
            },
            {
                'key': 'REGION',
                'value': os.environ['aws_region']
            },
            {
                'key': 'ZONE',
                'value': os.environ['aws_zone']
            },
            {
                'key': 'TAG_RESOURCE_ID',
                'value': os.environ['conf_tag_resource_id']
            },
            {
                'key': 'SG_IDS',
                'value': os.environ['aws_security_groups_ids']
            },
            {
                'key': 'SSN_INSTANCE_SIZE',
                'value': os.environ['aws_ssn_instance_size']
            },
            {
                'key': 'VPC_ID',
                'value': os.environ['aws_vpc_id']
            },
            {
                'key': 'CONF_KEY_DIR',
                'value': os.environ['conf_key_dir']
            },
            {
                'key': 'LDAP_HOST',
                'value': os.environ['ldap_hostname']
            },
            {
                'key': 'LDAP_DN',
                'value': os.environ['ldap_dn']
            },
            {
                'key': 'LDAP_OU',
                'value': os.environ['ldap_ou']
            },
            {
                'key': 'LDAP_USER_NAME',
                'value': os.environ['ldap_service_username']
            },
            {
                'key': 'LDAP_USER_PASSWORD',
                'value': os.environ['ldap_service_password']
            },
            {
                'key': 'AZURE_RESOURCE_GROUP_NAME',
                'value': ''
            },
            {
                'key': 'AZURE_SSN_STORAGE_ACCOUNT_TAG',
                'value': ''
            },
            {
                'key': 'AZURE_SHARED_STORAGE_ACCOUNT_TAG',
                'value': ''
            },
            {
                'key': 'AZURE_DATALAKE_TAG',
                'value': ''
            },
            {
                'key': 'AZURE_CLIENT_ID',
                'value': ''
            }
        ]
        if os.environ['conf_duo_vpc_enable'] == 'true':
            cloud_params.append(
                {
                    'key': 'SUBNET2_ID',
                    'value': os.environ['aws_subnet_id']
                })
            cloud_params.append(
                {
                    'key': 'VPC2_ID',
                    'value': os.environ['aws_vpc2_id']
                })
            cloud_params.append(
                {
                    'key': 'PEERING_ID',
                    'value': os.environ['aws_peering_id']
                })
        else:
            cloud_params.append(
                {
                    'key': 'SUBNET2_ID',
                    'value': os.environ['aws_subnet_id']
                })
            cloud_params.append(
                {
                    'key': 'VPC2_ID',
                    'value': os.environ['aws_vpc_id']
                })
            cloud_params.append(
                {
                    'key': 'PEERING_ID',
                    'value': ''
                })
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')
        params = "--hostname {} " \
                 "--keyfile {} " \
                 "--dlab_path {} " \
                 "--os_user {} " \
                 "--os_family {} " \
                 "--request_id {} " \
                 "--resource {} " \
                 "--service_base_name {} " \
                 "--tag_resource_id {} " \
                 "--billing_tag {} " \
                 "--cloud_provider {} " \
                 "--account_id {} " \
                 "--billing_bucket {} " \
                 "--aws_job_enabled {} " \
                 "--report_path '{}' " \
                 "--billing_enabled {} " \
                 "--cloud_params '{}' " \
                 "--dlab_id '{}' " \
                 "--usage_date {} " \
                 "--product {} " \
                 "--usage_type {} " \
                 "--usage {} " \
                 "--cost {} " \
                 "--resource_id {} " \
                 "--tags {}". \
            format(instance_hostname,
                   "{}{}.pem".format(os.environ['conf_key_dir'], os.environ['conf_key_name']),
                   os.environ['ssn_dlab_path'],
                   dlab_ssh_user,
                   os.environ['conf_os_family'],
                   os.environ['request_id'],
                   os.environ['conf_resource'],
                   service_base_name,
                   os.environ['conf_tag_resource_id'],
                   os.environ['conf_billing_tag'],
                   os.environ['conf_cloud_provider'],
                   os.environ['aws_account_id'],
                   os.environ['aws_billing_bucket'],
                   os.environ['aws_job_enabled'],
                   os.environ['aws_report_path'],
                   billing_enabled,
                   json.dumps(cloud_params),
                   os.environ['dlab_id'],
                   os.environ['usage_date'],
                   os.environ['product'],
                   os.environ['usage_type'],
                   os.environ['usage'],
                   os.environ['cost'],
                   os.environ['resource_id'],
                   os.environ['tags'])
        try:
            local("~/scripts/{}.py {}".format('configure_ui', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Unable to configure UI.", str(err))
        print(err)
        if domain_created:
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
        remove_ec2(tag_name, instance_name)
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        print('[SUMMARY]')
        print("Service base name: {}".format(service_base_name))
        print("SSN Name: {}".format(instance_name))
        print("SSN Hostname: {}".format(instance_hostname))
        print("Role name: {}".format(role_name))
        print("Role profile name: {}".format(role_profile_name))
        print("Policy name: {}".format(policy_name))
        print("Key name: {}".format(os.environ['conf_key_name']))
        print("VPC ID: {}".format(os.environ['aws_vpc_id']))
        print("Subnet ID: {}".format(os.environ['aws_subnet_id']))
        print("Security IDs: {}".format(os.environ['aws_security_groups_ids']))
        print("SSN instance shape: {}".format(os.environ['aws_ssn_instance_size']))
        print("SSN AMI name: {}".format(ssn_image_name))
        print("SSN bucket name: {}".format(ssn_bucket_name))
        print("Shared bucket name: {}".format(shared_bucket_name))
        print("Region: {}".format(region))
        jenkins_url = "http://{}/jenkins".format(get_instance_hostname(tag_name, instance_name))
        jenkins_url_https = "https://{}/jenkins".format(get_instance_hostname(tag_name, instance_name))
        print("Jenkins URL: {}".format(jenkins_url))
        print("Jenkins URL HTTPS: {}".format(jenkins_url_https))
        print("DLab UI HTTP URL: http://{}".format(get_instance_hostname(tag_name, instance_name)))
        print("DLab UI HTTPS URL: https://{}".format(get_instance_hostname(tag_name, instance_name)))
        try:
            with open('jenkins_creds.txt') as f:
                print(f.read())
        except:
            print("Jenkins is either configured already or have issues in configuration routine.")

        with open("/root/result.json", 'w') as f:
            res = {"service_base_name": service_base_name,
                   "instance_name": instance_name,
                   "instance_hostname": get_instance_hostname(tag_name, instance_name),
                   "role_name": role_name,
                   "role_profile_name": role_profile_name,
                   "policy_name": policy_name,
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": os.environ['aws_vpc_id'],
                   "subnet_id": os.environ['aws_subnet_id'],
                   "security_id": os.environ['aws_security_groups_ids'],
                   "instance_shape": os.environ['aws_ssn_instance_size'],
                   "bucket_name": ssn_bucket_name,
                   "shared_bucket_name": shared_bucket_name,
                   "region": region,
                   "action": "Create SSN instance"}
            f.write(json.dumps(res))

        print('Upload response file')
        params = "--instance_name {} --local_log_filepath {} --os_user {} --instance_hostname {}".\
            format(instance_name, local_log_filepath, dlab_ssh_user, instance_hostname)
        local("~/scripts/{}.py {}".format('upload_response_file', params))

        logging.info('[FINALIZE]')
        print('[FINALIZE]')
        params = ""
        if os.environ['conf_lifecycle_stage'] == 'prod':
            params += "--key_id {}".format(os.environ['aws_access_key'])
            local("~/scripts/{}.py {}".format('ssn_finalize', params))
    except:
        if domain_created:
            remove_route_53_record(os.environ['ssn_hosted_zone_id'], os.environ['ssn_hosted_zone_name'],
                                   os.environ['ssn_subdomain'])
        remove_ec2(tag_name, instance_name)
        remove_all_iam_resources(instance)
        remove_s3(instance)
        if pre_defined_sg:
            remove_sgroups(tag_name)
        if pre_defined_subnet:
            remove_internet_gateways(os.environ['aws_vpc_id'], tag_name, service_base_name)
            remove_subnets(service_base_name + "-subnet")
        if pre_defined_vpc:
            remove_vpc_endpoints(os.environ['aws_vpc_id'])
            remove_route_tables(tag_name, True)
            remove_vpc(os.environ['aws_vpc_id'])
        if pre_defined_vpc2:
            remove_peering('*')
            try:
                remove_vpc_endpoints(os.environ['aws_vpc2_id'])
            except:
                print("There are no VPC Endpoints")
            remove_route_tables(tag2_name, True)
            remove_vpc(os.environ['aws_vpc2_id'])
        sys.exit(1)
