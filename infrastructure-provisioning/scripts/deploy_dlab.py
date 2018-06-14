#!/usr/bin/python
# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************


from fabric.api import *
import argparse
import os

parser = argparse.ArgumentParser()
parser.add_argument('--conf_service_base_name', type=str, help='unique name for DLab environment')
parser.add_argument('--conf_network_type', type=str, default='', help='Define in which network DLab will be deployed. Possible options: public|private')
parser.add_argument('--conf_vpc_cidr', type=str, default='', help='CIDR of VPC')
parser.add_argument('--conf_allowed_ip_cidr', type=str, default='', help='CIDR of IPs which will have access to SSN')
parser.add_argument('--conf_user_subnets_range', type=str, default='', help='Range of subnets which will be using for users environments. For example: 10.10.0.0/24 - 10.10.10.0/24')
parser.add_argument('--conf_additional_tags', type=str, default='', help='Additional tags in format "Key1:Value1;Key2:Value2"')
parser.add_argument('--aws_user_predefined_s3_policies', type=str, default='', help='Predefined policies for users instances')
parser.add_argument('--aws_access_key', type=str, default='', help='AWS Access Key ID')
parser.add_argument('--aws_secret_access_key', type=str, default='', help='AWS Secret Access Key')
parser.add_argument('--aws_region', type=str, default='', help='AWS region')
parser.add_argument('--azure_region', type=str, default='', help='Azure region')
parser.add_argument('--gcp_region', type=str, default='', help='GCP region')
parser.add_argument('--gcp_zone', type=str, default='', help='GCP zone')
parser.add_argument('--conf_os_family', type=str, default='',
                    help='Operating system type. Available options: debian, redhat')
parser.add_argument('--conf_cloud_provider', type=str, default='',
                    help='Where DLab should be deployed. Available options: aws, azure, gcp')
parser.add_argument('--aws_vpc_id', type=str, default='', help='AWS VPC ID')
parser.add_argument('--azure_vpc_name', type=str, default='', help='Azure VPC Name')
parser.add_argument('--gcp_vpc_name', type=str, default='', help='GCP VPC Name')
parser.add_argument('--aws_subnet_id', type=str, default='', help='AWS Subnet ID')
parser.add_argument('--azure_subnet_name', type=str, default='', help='Azure Subnet Name')
parser.add_argument('--gcp_subnet_name', type=str, default='', help='GCP Subnet Name')
parser.add_argument('--aws_security_groups_ids', type=str, default='', help='One of more comma-separated Security groups IDs for SSN')
parser.add_argument('--azure_security_group_name', type=str, default='', help='One of more comma-separated Security groups names for SSN')
parser.add_argument('--gcp_firewall_name', type=str, default='', help='One of more comma-separated GCP Firewall rules for SSN')
parser.add_argument('--key_path', type=str, default='', help='Path to admin key (WITHOUT KEY NAME)')
parser.add_argument('--conf_key_name', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--workspace_path', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--conf_tag_resource_id', type=str, default='dlab', help='The name of user tag')
parser.add_argument('--aws_ssn_instance_size', type=str, default='t2.large', help='The SSN instance shape')
parser.add_argument('--azure_ssn_instance_size', type=str, default='Standard_DS2_v2', help='The SSN instance shape')
parser.add_argument('--gcp_ssn_instance_size', type=str, default='n1-standard-2', help='The SSN instance shape')
parser.add_argument('--aws_account_id', type=str, default='', help='The ID of Amazon account')
parser.add_argument('--aws_billing_bucket', type=str, default='', help='The name of S3 bucket where billing reports will be placed.')
parser.add_argument('--aws_report_path', type=str, default='', help='The path to billing reports directory in S3 bucket')
parser.add_argument('--azure_resource_group_name', type=str, default='', help='Name of Resource group in Azure')
parser.add_argument('--azure_auth_path', type=str, default='', help='Full path to Azure credentials JSON file')
parser.add_argument('--azure_datalake_enable', type=str, default='', help='Provision DataLake storage account')
parser.add_argument('--azure_ad_group_id', type=str, default='', help='ID of Azure AD group')
parser.add_argument('--azure_offer_number', type=str, default='', help='Azure offer number')
parser.add_argument('--azure_currency', type=str, default='', help='Azure currency code')
parser.add_argument('--azure_locale', type=str, default='', help='Azure locale')
parser.add_argument('--azure_application_id', type=str, default='', help='Azure login application ID')
parser.add_argument('--azure_validate_permission_scope', type=str, default='true', help='Azure permission scope validation(true|false).')
parser.add_argument('--azure_oauth2_enabled', type=str, default='false', help='Using OAuth2 for logging in DLab')
parser.add_argument('--azure_region_info', type=str, default='', help='Azure region info')
parser.add_argument('--gcp_project_id', type=str, default='', help='The project ID in Google Cloud Platform')
parser.add_argument('--gcp_service_account_path', type=str, default='', help='The project ID in Google Cloud Platform')
parser.add_argument('--action', required=True, type=str, default='', choices=['build', 'deploy', 'create', 'terminate'],
                    help='Available options: build, deploy, create, terminate')
args = parser.parse_args()


def generate_docker_command():
    docker_command = ''
    command = []
    command.append('sudo docker run -i -v {0}{1}.pem:/root/keys/{1}.pem -v {2}/web_app:/root/web_app '.
                   format(args.key_path, args.conf_key_name, args.workspace_path))
    if args.conf_cloud_provider == 'azure':
        command.append('-v {}:/root/azure_auth.json '.format(args.azure_auth_path))
    elif args.conf_cloud_provider == 'gcp':
        command.append('-v {}:/root/service_account.json '.format(args.gcp_service_account_path))
    attrs = vars(args)
    for i in attrs:
        if attrs[i] and i != 'action' and i != 'key_path' and i != 'workspace_path' and i != 'gcp_service_account_path':
            command.append('-e "{}={}" '.format(i, attrs[i]))
    command.append('-e "conf_resource=ssn" ')
    command.append('docker.dlab-ssn ')
    command.append('--action {} '.format(args.action))
    return docker_command.join(command)


def build_front_end(args):
    # Building front-end
    with lcd(args.workspace_path + '/services/self-service/src/main/resources/webapp/'):
        local('sed -i "s|CLOUD_PROVIDER|{}|g" src/dictionary/global.dictionary.ts'.format(args.conf_cloud_provider))

        if args.conf_cloud_provider == 'azure' and args.azure_datalake_enable == 'true':
            local('sed -i "s|\'use_ldap\': true|{}|g" src/dictionary/azure.dictionary.ts'.format('\'use_ldap\': false'))

        local('npm install')
        local('npm run build.prod')
        local('sudo chown -R {} {}/*'.format(os.environ['USER'], args.workspace_path))


def build_services():
    # Building provisioning-service, security-service, self-service, billing
    local('mvn -P{} -DskipTests package'.format(args.conf_cloud_provider))


def build_docker_images(args):
    # Building base and ssn docker images
    with lcd(args.workspace_path + '/infrastructure-provisioning/src/'):
        local('sudo docker build --build-arg OS={0} --file general/files/{1}/base_Dockerfile '
              '-t docker.dlab-base .'.format(args.conf_os_family, args.conf_cloud_provider))
        local('sudo docker build --build-arg OS={0} --file general/files/{1}/ssn_Dockerfile '
              '-t docker.dlab-ssn .'.format(args.conf_os_family, args.conf_cloud_provider))


def deploy_dlab(args):
    # Preparing files for deployment

    local('mkdir -p {}/web_app'.format(args.workspace_path))
    local('mkdir -p {}/web_app/provisioning-service/'.format(args.workspace_path))
    local('mkdir -p {}/web_app/security-service/'.format(args.workspace_path))
    local('mkdir -p {}/web_app/self-service/'.format(args.workspace_path))
    local('mkdir -p {}/web_app/billing/'.format(args.workspace_path))
    local('cp {0}/services/self-service/self-service.yml {0}/web_app/self-service/'.format(args.workspace_path))
    local('cp {0}/services/self-service/target/self-service-*.jar {0}/web_app/self-service/'.
        format(args.workspace_path))
    local('cp {0}/services/provisioning-service/provisioning.yml {0}/web_app/provisioning-service/'.
        format(args.workspace_path))
    local('cp {0}/services/provisioning-service/target/provisioning-service-*.jar {0}/web_app/provisioning-service/'.
        format(args.workspace_path))
    local('cp {0}/services/security-service/security.yml {0}/web_app/security-service/'.format(args.workspace_path))
    local('cp {0}/services/security-service/target/security-service-*.jar {0}/web_app/security-service/'.
        format(args.workspace_path))

    if args.conf_cloud_provider == 'azure':
        local('cp {0}/services/billing-azure/billing.yml {0}/web_app/billing/'.format(args.workspace_path))
        local('cp {0}/services/billing-azure/target/billing-azure*.jar {0}/web_app/billing/'.format(args.workspace_path))
    elif args.conf_cloud_provider == 'aws':
        local('cp {0}/services/billing-aws/billing.yml {0}/web_app/billing/'.format(args.workspace_path))
        local('cp {0}/services/billing-aws/target/billing-aws*.jar {0}/web_app/billing/'.format(args.workspace_path))
    elif args.conf_cloud_provider == 'gcp':
        local('cp {0}/services/billing-gcp/billing.yml {0}/web_app/billing/'.format(args.workspace_path))
        local('cp {0}/services/billing-gcp/target/billing-gcp*.jar {0}/web_app/billing/'.format(args.workspace_path))

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
        build_front_end(args)
        build_services()
        build_docker_images(args)
    elif args.action == 'deploy':
        deploy_dlab(args)
    elif args.action == 'create':
        build_front_end(args)
        build_services()
        build_docker_images(args)
        deploy_dlab(args)
    elif args.action == 'terminate':
        build_docker_images(args)
        terminate_dlab(args)
