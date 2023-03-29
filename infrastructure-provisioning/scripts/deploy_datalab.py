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

"""Examples How to deploy DataLab for different cloud providers.

``GCP`` example::

        $ infrastructure-provisioning/scripts/deploy_datalab.py \
        --conf_service_base_name <SERVICE_NAME> \
        --conf_os_family debian \
        --action create \
        --key_path /home/ubuntu/.ssh \
        --conf_key_name gcp \
        --billing_dataset_name billing \
        gcp \
        --gcp_ssn_instance_size n1-standard-2 \
        --gcp_project_id <PROJECT_ID>\
        --gcp_service_account_path /home/ubuntu/secret.json\
        --gcp_region us-west1\
        --gcp_zone us-west1-a

``AWS`` example::

        $ infrastructure-provisioning/scripts/deploy_datalab.py\
        --conf_service_base_name datalab-test\
        --conf_os_family debian\
        --action create \
        --key_path /path/to/key/\
        --conf_key_name key_name\
        --conf_tag_resource_id datalab\
        aws\
        --aws_vpc_id vpc-xxxxx\
        --aws_subnet_id subnet-xxxxx\
        --aws_security_groups_ids sg-xxxxx,sg-xxxx\
        --aws_access_key XXXXXXX\
        --aws_secret_access_key XXXXXXXXXX\
        --aws_region xx-xxxxx-x\
        --aws_account_id xxxxxxxx\
        --aws_billing_bucket billing_bucket\
        --aws_report_path /billing/directory/\

"""

import argparse
import os
import subprocess
import sys

BOOL_CHOICES_LIST = ['true', 'false']
OS_DISTRO_LIST = ['debian', 'redhat']
NETWORK_TYPE_LIST = ['public', 'private']


def build_parser():
    parser = argparse.ArgumentParser(description='DataLab Self-Service Node deployment',
                                     prog='deploy_datalab')
    # optional arguments
    parser.add_argument('--conf_network_type', type=str, default='public',
                        help='''Type of network. Define in which network DataLab will be deployed.
                        (valid choices: %s)''' % NETWORK_TYPE_LIST,
                        choices=NETWORK_TYPE_LIST)
    parser.add_argument('--conf_vpc_cidr', type=str, default='172.31.0.0/16', help='CIDR of VPC')
    parser.add_argument('--conf_vpc2_cidr', type=str, help='CIDR of secondary VPC')
    parser.add_argument('--conf_allowed_ip_cidr', type=str, default='0.0.0.0/0',
                        help='Comma-separated CIDR of IPs which will have access to SSN')
    parser.add_argument('--conf_user_subnets_range', type=str,
                        help='''Range of subnets which will be using for users environments.
                        For example: 10.10.0.0/24 - 10.10.10.0/24''')
    parser.add_argument('--conf_private_subnet_prefix', type=str, default='24', help='Private subnet prefix')
    parser.add_argument('--conf_additional_tags', type=str,
                        help='Additional tags in format "Key1:Value1;Key2:Value2"')
    parser.add_argument('--conf_image_enabled', type=str,
                        help='Enable or Disable creating image at first time')
    parser.add_argument('--conf_os_family', type=str, default='debian', choices=OS_DISTRO_LIST,
                        help='Operating system distribution. (valid choices: %s)' % OS_DISTRO_LIST)
    parser.add_argument('--ssn_hosted_zone_name', type=str, help='Name of hosted zone')
    parser.add_argument('--ssn_hosted_zone_id', type=str, help='ID of hosted zone')
    parser.add_argument('--ssn_subdomain', type=str, help='Subdomain name')
    parser.add_argument('--ssl_cert_path', type=str, help='Full path to SSL certificate')
    parser.add_argument('--ssl_key_path', type=str, help='Full path to key for SSL certificate')
    parser.add_argument('--workspace_path', type=str, default='', help='Docker workspace path')
    parser.add_argument('--conf_tag_resource_id', type=str, default='datalab', help='The name of user tag')
    parser.add_argument('--conf_billing_tag', type=str, default='datalab', help='Billing tag')
    parser.add_argument('--datalab_id', type=str, default='resource_tags_user_user_tag',
                        help='Column name in report file that contains datalab id tag')
    parser.add_argument('--usage_date', type=str, default='line_item_usage_start_date',
                        help='Column name in report file that contains usage date tag')
    parser.add_argument('--product', type=str, default='product_product_name',
                        help='Column name in report file that contains product name tag')
    parser.add_argument('--usage_type', type=str, default='line_item_usage_type',
                        help='Column name in report file that contains usage type tag')
    parser.add_argument('--usage', type=str, default='line_item_usage_amount',
                        help='Column name in report file that contains usage tag')
    parser.add_argument('--cost', type=str, default='line_item_blended_cost',
                        help='Column name in report file that contains cost tag')
    parser.add_argument('--resource_id', type=str, default='line_item_resource_id',
                        help='Column name in report file that contains datalab resource id tag')
    parser.add_argument('--conf_bucket_versioning_enabled', type=str, default='true', choices=BOOL_CHOICES_LIST,
                            help='Versioning for S3 bucket (valid choices: %s)' % BOOL_CHOICES_LIST)

    parser.add_argument('--tags', type=str, default='line_item_operation,line_item_line_item_description',
                        help='Column name in report file that contains tags')
    parser.add_argument('--conf_stepcerts_enabled', type=str, default='false',
                        help='Enable or disable step certificates. (valid choices: %s)' % BOOL_CHOICES_LIST,
                        choices=BOOL_CHOICES_LIST)
    parser.add_argument('--conf_stepcerts_root_ca', type=str, help='Step root CA')
    parser.add_argument('--conf_stepcerts_kid', type=str, help='Step KID')
    parser.add_argument('--conf_stepcerts_kid_password', type=str, help='Step KID password')
    parser.add_argument('--conf_stepcerts_ca_url', type=str, help='Step CA URL')
    parser.add_argument('--conf_letsencrypt_enabled', type=str, default='false',
                        help='Enable or disable Let`s Encrypt certificates. (valid choices: %s)' % BOOL_CHOICES_LIST,
                        choices=BOOL_CHOICES_LIST)
    parser.add_argument('--conf_letsencrypt_domain_name', type=str,
                        help='''Domain names to apply. For multiple domains enter a comma separated list of domains
        as a parameter. ssn.domain_name will be used for ssn_node,DNS A record have to exist during deployment''')
    parser.add_argument('--conf_letsencrypt_email', type=str, help='''Email that will be entered during
        certificate obtaining and can be user for urgent renewal and security notices. Use comma to register
        multiple emails, e.g. u1@example.com,u2@example.com.''')
    parser.add_argument('--conf_repository_user', type=str, default='',
                        help='user to access repository (used for jars download)')
    parser.add_argument('--conf_release_tag', type=str, default='2.5.2',
                        help='tag used for jars download')
    parser.add_argument('--conf_repository_pass', type=str, default='',
                        help='password to access repository (used for jars download)')
    parser.add_argument('--conf_repository_address', type=str, default='',
                        help='address to access repository (used for jars download)')
    parser.add_argument('--conf_repository_port', type=str, default='',
                        help='port to access repository (used for docker images download)')
    parser.add_argument('--conf_download_docker_images', type=str, default='false',
                        help='true if download docker images from repository')
    parser.add_argument('--conf_download_jars', type=str, default='false',
                        help='true if download jars from repository')
    parser.add_argument('--default_endpoint_name', type=str, default='local',
                               help='Name of localhost provisioning service, that created by default')
    parser.add_argument('--keycloak_client_name', type=str, default='',
                               help='Keycloak client name')
    parser.add_argument('--keycloak_client_secret', type=str, default='',
                               help='Keycloak client secret')
    parser.add_argument('--ldap_hostname', type=str, default='localhost', help='Ldap instance hostname')
    parser.add_argument('--ldap_dn', type=str, default='dc=example,dc=com',
                        help='Ldap distinguished name')
    parser.add_argument('--ldap_ou', type=str, default='ou=People', help='Ldap organisation unit')
    parser.add_argument('--ldap_service_username', type=str, default='cn=service-user', help='Ldap service user name')
    parser.add_argument('--ldap_service_password', type=str, default='service-user-password',
                        help='Ldap password for admin user')

    required_args = parser.add_argument_group('Required arguments')
    required_args.add_argument('--conf_service_base_name', type=str,
                               help='Unique name for DataLab environment', required=True)
    required_args.add_argument('--action', type=str, help='Action to perform',
                               choices=['build', 'deploy', 'create', 'terminate'], required=True)
    required_args.add_argument('--key_path', type=str, help='Path to admin key (WITHOUT KEY NAME)', required=True)
    required_args.add_argument('--conf_key_name', type=str, help='Admin key name (WITHOUT ".pem")', required=True)
    required_args.add_argument('--keycloak_auth_server_url', type=str, default='datalab',
                               help='Keycloak auth server URL', required=True)
    required_args.add_argument('--keycloak_realm_name', type=str, help='Keycloak Realm name', required=True)
    required_args.add_argument('--keycloak_user', type=str, default='datalab', help='Keycloak user', required=True)
    required_args.add_argument('--keycloak_user_password', type=str, default='keycloak-user-password',
                               help='Keycloak user password', required=True)


    # subparsers
    subparsers = parser.add_subparsers(dest='conf_cloud_provider', required=True, help='sub-command help',
                                       description='''These are the subcommands for deploying resources
                                       in a specific cloud provider''')

    # --------- aws subcommand ----------------------
    aws_parser = subparsers.add_parser('aws')
    aws_parser.add_argument('--aws_user_predefined_s3_policies', type=str,
                            help='Predefined policies for users instances')
    aws_parser.add_argument('--aws_access_key', type=str,
                            help='''AWS Access Key ID. reuqired in case of deployment with IAM user DataLab
                            deployment script is executed on local machine and uses
                            IAM user permissions to create resources in AWS.''')
    aws_parser.add_argument('--aws_secret_access_key', type=str, help='AWS Secret Access Key')
    aws_parser.add_argument('--aws_ssn_instance_size', type=str, default='t2.large',
                                   help='The SSN instance shape')
    aws_parser.add_argument('--ssn_assume_role_arn', type=str,
                            help='Role ARN for creating Route53 record in different AWS account')
    aws_parser.add_argument('--aws_vpc_id', type=str, help='AWS VPC ID')
    aws_parser.add_argument('--conf_duo_vpc_enable', type=str, default='false',
                            help='Duo VPC scheme enable. (valid choices: %s)' % BOOL_CHOICES_LIST,
                            choices=BOOL_CHOICES_LIST)
    aws_parser.add_argument('--aws_vpc2_id', type=str, help='Secondary AWS VPC ID')
    aws_parser.add_argument('--aws_peering_id', type=str, help='Amazon peering connection id')
    aws_parser.add_argument('--aws_subnet_id', type=str, help='AWS Subnet ID')
    aws_parser.add_argument('--aws_security_groups_ids', type=str,
                            help='One of more comma-separated Security groups IDs for SSN')
    aws_parser.add_argument('--aws_billing_bucket', type=str,
                            help='The name of S3 bucket where billing reports will be placed.')
    aws_parser.add_argument('--aws_job_enabled', type=str, default='false', choices=BOOL_CHOICES_LIST,
                            help='Billing format. (valid choices: %s)' % BOOL_CHOICES_LIST)
    aws_parser.add_argument('--aws_report_path', type=str, help='The path to billing reports directory in S3 bucket')
    aws_parser.add_argument('--aws_permissions_boundary_arn', type=str, default='',
                            help='Permission boundary to be attached to new roles')
    aws_parser.add_argument('--aws_ssn_instance_role', type=str, default='',
                            help='Role to be attached to SSN instance')

    aws_required_args = aws_parser.add_argument_group('Required arguments')
    aws_required_args.add_argument('--aws_region', type=str, required=True, help='AWS region')
    aws_required_args.add_argument('--aws_zone', type=str, required=True, help='AWS zone')
    aws_required_args.add_argument('--aws_account_id', type=str, required=True, help='The ID of Amazon account')

    # --------azure subcommand -------------------------
    azure_parser = subparsers.add_parser('azure')
    azure_parser.add_argument('--azure_vpc_name', type=str, help='Azure VPC Name')
    azure_parser.add_argument('--azure_subnet_name', type=str, help='Azure Subnet Name')
    azure_parser.add_argument('--azure_security_group_name', type=str, help='One Security group name for SSN')
    azure_parser.add_argument('--azure_edge_security_group_name', type=str,
                              help='One Security group name for Edge node')
    azure_parser.add_argument('--azure_resource_group_name', type=str, help='Name of Resource group in Azure')
    azure_parser.add_argument('--azure_datalake_enable', type=str, default='false', choices=BOOL_CHOICES_LIST,
                              help='Provision DataLake storage account. (valid choices: %s)' % BOOL_CHOICES_LIST)
    azure_parser.add_argument('--azure_ad_group_id', type=str, help='ID of Azure AD group')
    azure_parser.add_argument('--azure_offer_number', type=str, help='Azure offer number')
    azure_parser.add_argument('--azure_currency', type=str, help='Azure currency code')
    azure_parser.add_argument('--azure_locale', type=str, help='Azure locale')
    azure_parser.add_argument('--azure_application_id', type=str, help='Azure login application ID')
    azure_parser.add_argument('--azure_validate_permission_scope', type=str, default='true',
                              choices=BOOL_CHOICES_LIST,
                              help='Azure permission scope validation. (valid choices: %s)' % BOOL_CHOICES_LIST)
    azure_parser.add_argument('--azure_oauth2_enabled', type=str, default='false', choices=BOOL_CHOICES_LIST,
                              help='Using OAuth2 for logging in DataLab. (valid choices: %s)' % BOOL_CHOICES_LIST)
    azure_parser.add_argument('--azure_region_info', type=str, help='Azure region info')
    azure_parser.add_argument('--azure_source_vpc_name', type=str, help='Azure VPC source Name')
    azure_parser.add_argument('--azure_source_resource_group_name', type=str, help='Azure source resource group')
    azure_parser.add_argument('--azure_ssn_instance_size', type=str, default='Standard_DS2_v2',
                                     help='The SSN instance shape')

    azure_required_args = azure_parser.add_argument_group('Required arguments')
    azure_required_args.add_argument('--azure_region', type=str, required=True, help='Azure region')
    azure_required_args.add_argument('--azure_auth_path', type=str, required=True,
                                     help='Full path to Azure credentials JSON file')

    # --------gcp subcommand -----------------------------
    gcp_parser = subparsers.add_parser('gcp')
    gcp_parser.add_argument('--billing_dataset_name', type=str,
                            help='Name of GCP dataset (BigQuery service) for billing')
    gcp_parser.add_argument('--gcp_subnet_name', type=str, help='GCP Subnet Name')
    gcp_parser.add_argument('--gcp_vpc_name', type=str, help='GCP VPC Name')
    gcp_parser.add_argument('--gcp_firewall_name', type=str,
                            help='One of more comma-separated GCP Firewall rules for SSN')
    gcp_parser.add_argument('--gcp_ssn_instance_size', type=str, default='n1-standard-2',
                                   help='The SSN instance shape')
    gcp_parser.add_argument('--gcp_os_login_enabled', type=str, default='FALSE',
                            help='"TRUE" to enable os login for gcp instances')
    gcp_parser.add_argument('--gcp_block_project_ssh_keys', type=str, default='FALSE',
                            help='"TRUE" to block project ssh keys for gcp instances')
    gcp_parser.add_argument('--gcp_cmek_resource_name', type=str, default='',
                            help='customer managed encryption key resource name '
                            'e.g. projects/{project_name}/locations/{us}/keyRings/{keyring_name}/cryptoKeys/{key_name}')
    gcp_parser.add_argument('--gcp_additional_network_tag', type=str, default='',
                            help='Additional_network_tag')
    gcp_parser.add_argument('--gcp_storage_lifecycle_rules', type=str, default='',
                            help='storage bucket lifecycle rules')
    gcp_parser.add_argument('--gcp_wrapped_csek', type=str, default='',
                            help='customer supplied encryption key for disk/image encryption in RFC 4648 base64 '
                                 'encoded, RSA-wrapped 2048-bit format as rsaEncryptedKey')
    gcp_parser.add_argument('--gcp_jupyter_gpu_type', type=str, default='nvidia-tesla-a100',
                            help='gpu type for jupyter gpu notebooks with a2-highgpu-1g shape')

    gcp_required_args = gcp_parser.add_argument_group('Required arguments')
    gcp_required_args.add_argument('--gcp_region', type=str, required=True, help='GCP region')
    gcp_required_args.add_argument('--gcp_zone', type=str, required=True, help='GCP zone')
    gcp_required_args.add_argument('--gcp_project_id', type=str, required=True,
                                   help='The project ID in Google Cloud Platform')
    gcp_required_args.add_argument('--gcp_service_account_path', type=str, required=True,
                                   help='The project ID in Google Cloud Platform')
    return parser


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
    if args.ssl_cert_path and args.ssl_cert_path != '' and args.ssl_key_path and args.ssl_key_path != '':
        command.append('-v {}:/root/certs/datalab.crt -v {}:/root/certs/datalab.key '.format(args.ssl_cert_path,
                                                                                             args.ssl_key_path))
    attrs = vars(args)
    skipped_parameters = ['action', 'key_path', 'workspace_path', 'gcp_service_account_path', 'ssl_cert_path',
                          'ssl_key_path']
    for i in attrs:
        if attrs[i] and i not in skipped_parameters:
            command.append("-e '{}={}' ".format(i, attrs[i]))
    command.append('-e "conf_resource=ssn" ')
    command.append('docker.datalab-ssn ')
    command.append('--action {} '.format(args.action))
    return docker_command.join(command)


def build_docker_images(args):
    if args.conf_repository_user and args.conf_repository_pass and args.conf_repository_port and args.conf_repository_address and args.conf_download_docker_images == 'true':
        subprocess.run('sudo docker login -u {0} -p {1} {2}:{3}'
                        .format(args.conf_repository_user, args.conf_repository_pass, args.conf_repository_address, args.conf_repository_port), shell=True, check=True)
        subprocess.run('sudo docker pull {}:{}/docker.datalab-ssn-{}'.format(args.conf_repository_address, args.conf_repository_port, args.conf_cloud_provider), shell=True, check=True)
        subprocess.run('sudo docker image tag {}:{}/docker.datalab-ssn-{} docker.datalab-ssn'.format(args.conf_repository_address, args.conf_repository_port, args.conf_cloud_provider), shell=True, check=True)
        subprocess.run('sudo docker image rm {}:{}/docker.datalab-ssn-{}'.format(args.conf_repository_address, args.conf_repository_port, args.conf_cloud_provider), shell=True, check=True)
    else:
        # Building base and ssn docker images
        subprocess.run(
            'cd {2}; sudo docker build --build-arg OS={0} --build-arg SRC_PATH="infrastructure-provisioning/src/" --file '
            'infrastructure-provisioning/src/general/files/{1}/'
            'base_Dockerfile -t docker.datalab-base .'.format(args.conf_os_family, args.conf_cloud_provider,
                                                              args.workspace_path), shell=True, check=True)
        subprocess.run(
            'cd {2}; sudo docker build --build-arg OS={0} --file infrastructure-provisioning/src/general/files/{1}/'
            'ssn_Dockerfile -t docker.datalab-ssn .'.format(args.conf_os_family, args.conf_cloud_provider,
                                                            args.workspace_path), shell=True, check=True)


def deploy_datalab(args):
    # Creating SSN node
    docker_command = generate_docker_command()
    print('Docker command: {}'.format(docker_command ))
    subprocess.run(docker_command, shell=True, check=True)


def terminate_datalab(args):
    # Dropping datalab environment with selected infrastructure tag
    docker_command = generate_docker_command()
    subprocess.run(docker_command, shell=True, check=True)


if __name__ == "__main__":
    parser = build_parser()
    args = parser.parse_args()

    if args.conf_cloud_provider == 'aws' and not (args.aws_secret_access_key and args.aws_access_key):
        sys.exit('Please provide both arguments: --aws_secret_access_key and --aws_access_key')

    if not args.workspace_path:
        print("Workspace path isn't set, using current directory: {}".format(os.environ['PWD']))
        args.workspace_path = os.environ['PWD']
    if args.action == 'build':
        build_docker_images(args)
    elif args.action == 'deploy':
        deploy_datalab(args)
    elif args.action == 'create':
        build_docker_images(args)
        deploy_datalab(args)
    elif args.action == 'terminate':
        build_docker_images(args)
        terminate_datalab(args)
