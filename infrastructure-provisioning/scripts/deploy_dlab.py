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


from fabric.api import *
import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument('--conf_service_base_name', type=str, help='unique name for DLab environment')
parser.add_argument('--conf_network_type', type=str, default='', help='Define in which network DLab will be deployed. '
                                                                      'Possible options: public|private')
parser.add_argument('--conf_vpc_cidr', type=str, default='', help='CIDR of VPC')
parser.add_argument('--conf_vpc2_cidr', type=str, default='', help='CIDR of secondary VPC')
parser.add_argument('--conf_allowed_ip_cidr', type=str, default='', help='Comma-separated CIDR of IPs which will have '
                                                                         'access to SSN')
parser.add_argument('--conf_user_subnets_range', type=str, default='', help='Range of subnets which will be using for '
                                                                            'users environments. For example: '
                                                                            '10.10.0.0/24 - 10.10.10.0/24')
parser.add_argument('--conf_additional_tags', type=str, default='', help='Additional tags in format '
                                                                         '"Key1:Value1;Key2:Value2"')
parser.add_argument('--aws_user_predefined_s3_policies', type=str, default='', help='Predefined policies for users '
                                                                                    'instances')
parser.add_argument('--aws_access_key', type=str, default='', help='AWS Access Key ID')
parser.add_argument('--aws_secret_access_key', type=str, default='', help='AWS Secret Access Key')
parser.add_argument('--aws_region', type=str, default='', help='AWS region')
parser.add_argument('--aws_zone', type=str, default='', help='AWS zone')
parser.add_argument('--azure_region', type=str, default='', help='Azure region')
parser.add_argument('--gcp_region', type=str, default='', help='GCP region')
parser.add_argument('--gcp_zone', type=str, default='', help='GCP zone')
parser.add_argument('--conf_os_family', type=str, default='',
                    help='Operating system type. Available options: debian, redhat')
parser.add_argument('--conf_cloud_provider', type=str, default='',
                    help='Where DLab should be deployed. Available options: aws, azure, gcp')
parser.add_argument('--ssn_hosted_zone_name', type=str, default='', help='Name of hosted zone')
parser.add_argument('--ssn_hosted_zone_id', type=str, default='', help='ID of hosted zone')
parser.add_argument('--ssn_subdomain', type=str, default='', help='Subdomain name')
parser.add_argument('--ssn_assume_role_arn', type=str, default='', help='Role ARN for creating Route53 record in '
                                                                        'different AWS account')
parser.add_argument('--ssl_cert_path', type=str, default='', help='Full path to SSL certificate')
parser.add_argument('--ssl_key_path', type=str, default='', help='Full path to SSL certificate')
parser.add_argument('--aws_vpc_id', type=str, default='', help='AWS VPC ID')
parser.add_argument('--conf_duo_vpc_enable', type=str, default='false', help='Duo VPC scheme enable(true|false)')
parser.add_argument('--aws_vpc2_id', type=str, default='', help='Secondary AWS VPC ID')
parser.add_argument('--aws_peering_id', type=str, default='', help='Amazon peering connection id')
parser.add_argument('--azure_vpc_name', type=str, default='', help='Azure VPC Name')
parser.add_argument('--gcp_vpc_name', type=str, default='', help='GCP VPC Name')
parser.add_argument('--aws_subnet_id', type=str, default='', help='AWS Subnet ID')
parser.add_argument('--azure_subnet_name', type=str, default='', help='Azure Subnet Name')
parser.add_argument('--gcp_subnet_name', type=str, default='', help='GCP Subnet Name')
parser.add_argument('--aws_security_groups_ids', type=str, default='', help='One of more comma-separated Security '
                                                                            'groups IDs for SSN')
parser.add_argument('--azure_security_group_name', type=str, default='', help='One of more comma-separated Security '
                                                                              'groups names for SSN')
parser.add_argument('--gcp_firewall_name', type=str, default='', help='One of more comma-separated GCP Firewall rules '
                                                                      'for SSN')
parser.add_argument('--key_path', type=str, default='', help='Path to admin key (WITHOUT KEY NAME)')
parser.add_argument('--conf_key_name', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--workspace_path', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--conf_tag_resource_id', type=str, default='dlab', help='The name of user tag')
parser.add_argument('--aws_ssn_instance_size', type=str, default='t2.large', help='The SSN instance shape')
parser.add_argument('--azure_ssn_instance_size', type=str, default='Standard_DS2_v2', help='The SSN instance shape')
parser.add_argument('--gcp_ssn_instance_size', type=str, default='n1-standard-2', help='The SSN instance shape')
parser.add_argument('--aws_account_id', type=str, default='', help='The ID of Amazon account')
parser.add_argument('--aws_billing_bucket', type=str, default='', help='The name of S3 bucket where billing reports '
                                                                       'will be placed.')
parser.add_argument('--aws_job_enabled', type=str, default='false', help='Billing format. Available options: '
                                                                         'true (aws), false(epam)')
parser.add_argument('--aws_report_path', type=str, default='', help='The path to billing reports directory in S3 '
                                                                    'bucket')
parser.add_argument('--azure_resource_group_name', type=str, default='', help='Name of Resource group in Azure')
parser.add_argument('--azure_auth_path', type=str, default='', help='Full path to Azure credentials JSON file')
parser.add_argument('--azure_datalake_enable', type=str, default='', help='Provision DataLake storage account')
parser.add_argument('--azure_ad_group_id', type=str, default='', help='ID of Azure AD group')
parser.add_argument('--azure_offer_number', type=str, default='', help='Azure offer number')
parser.add_argument('--azure_currency', type=str, default='', help='Azure currency code')
parser.add_argument('--azure_locale', type=str, default='', help='Azure locale')
parser.add_argument('--azure_application_id', type=str, default='', help='Azure login application ID')
parser.add_argument('--azure_validate_permission_scope', type=str, default='true', help='Azure permission scope '
                                                                                        'validation(true|false).')
parser.add_argument('--azure_oauth2_enabled', type=str, default='false', help='Using OAuth2 for logging in DLab')
parser.add_argument('--azure_region_info', type=str, default='', help='Azure region info')
parser.add_argument('--azure_source_vpc_name', type=str, default='', help='Azure VPC source Name')
parser.add_argument('--azure_source_resource_group_name', type=str, default='', help='Azure source resource group')
parser.add_argument('--gcp_project_id', type=str, default='', help='The project ID in Google Cloud Platform')
parser.add_argument('--gcp_service_account_path', type=str, default='', help='The project ID in Google Cloud Platform')
parser.add_argument('--dlab_id', type=str, default="'resource_tags_user_user_tag'", help='Column name in report file that contains '
                                                                           'dlab id tag')
parser.add_argument('--usage_date', type=str, default='line_item_usage_start_date', help='Column name in report file that contains '
                                                                             'usage date tag')
parser.add_argument('--product', type=str, default='product_product_name', help='Column name in report file that contains '
                                                                       'product name tag')
parser.add_argument('--usage_type', type=str, default='line_item_usage_type', help='Column name in report file that contains '
                                                                        'usage type tag')
parser.add_argument('--usage', type=str, default='line_item_usage_amount', help='Column name in report file that contains '
                                                                       'usage tag')
parser.add_argument('--cost', type=str, default='line_item_blended_cost', help='Column name in report file that contains cost tag')
parser.add_argument('--resource_id', type=str, default='line_item_resource_id', help='Column name in report file that contains '
                                                                          'dlab resource id tag')
parser.add_argument('--ldap_hostname', type=str, default='localhost', help='Ldap instance hostname')
parser.add_argument('--ldap_dn', type=str, default='dc=example,dc=com',
                    help='Ldap distinguished name')
parser.add_argument('--ldap_ou', type=str, default='ou=People', help='Ldap organisation unit')
parser.add_argument('--ldap_service_username', type=str, default='cn=service-user', help='Ldap service user name')
parser.add_argument('--ldap_service_password', type=str, default='service-user-password',
                    help='Ldap password for admin user')
parser.add_argument('--tags', type=str, default='line_item_operation,line_item_line_item_description', help='Column name in report file that '
                                                                                  'contains tags')
parser.add_argument('--action', required=True, type=str, default='', choices=['build', 'deploy', 'create', 'terminate'],
                    help='Available options: build, deploy, create, terminate')
args = parser.parse_args()


def generate_docker_command():
    docker_command = ''
    command = []
    if args.action == 'terminate':
        command.append('sudo docker run -i ')
    else:
        command.append('sudo docker run -i -v {0}{1}.pem:/root/keys/{1}.pem -v {2}/web_app:/root/web_app '.
                       format(args.key_path, args.conf_key_name, args.workspace_path))
    if args.conf_cloud_provider == 'azure':
        command.append('-v {}:/root/azure_auth.json '.format(args.azure_auth_path))
    elif args.conf_cloud_provider == 'gcp':
        command.append('-v {}:/root/service_account.json '.format(args.gcp_service_account_path))
    if args.ssl_cert_path != '' and args.ssl_key_path != '':
        command.append('-v {}:/root/certs/dlab.crt -v {}:/root/certs/dlab.key '.format(args.ssl_cert_path,
                                                                                       args.ssl_key_path))
    attrs = vars(args)
    skipped_parameters = ['action', 'key_path', 'workspace_path', 'gcp_service_account_path', 'ssl_cert_path',
                          'ssl_key_path']
    for i in attrs:
        if attrs[i] and i not in skipped_parameters:
            command.append("-e '{}={}' ".format(i, attrs[i]))
    command.append('-e "conf_resource=ssn" ')
    command.append('docker.dlab-ssn ')
    command.append('--action {} '.format(args.action))
    return docker_command.join(command)


def build_docker_images(args):
    # Building base and ssn docker images
    with lcd(args.workspace_path):
        local('sudo docker build --build-arg OS={0} --build-arg SRC_PATH="infrastructure-provisioning/src/" --file '
              'infrastructure-provisioning/src/general/files/{1}/'
              'base_Dockerfile -t docker.dlab-base .'.format(args.conf_os_family, args.conf_cloud_provider))
        local('sudo docker build --build-arg OS={0} --file infrastructure-provisioning/src/general/files/{1}/'
              'ssn_Dockerfile -t docker.dlab-ssn .'.format(args.conf_os_family, args.conf_cloud_provider))


def deploy_dlab(args):
    # Creating SSN node
    docker_command = generate_docker_command()
    local(docker_command)


def terminate_dlab(args):
    # Dropping Dlab environment with selected infrastructure tag
    docker_command = generate_docker_command()
    local(docker_command)


if __name__ == "__main__":
    if not args.workspace_path:
        print("Workspace path isn't set, using current directory: {}".format(os.environ['PWD']))
        args.workspace_path = os.environ['PWD']
    if args.action == 'build':
        build_docker_images(args)
    elif args.action == 'deploy':
        deploy_dlab(args)
    elif args.action == 'create':
        build_docker_images(args)
        deploy_dlab(args)
    elif args.action == 'terminate':
        build_docker_images(args)
        terminate_dlab(args)
