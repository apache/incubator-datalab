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
parser.add_argument('--aws_access_key', type=str, default='', help='AWS Access Key ID')
parser.add_argument('--aws_secret_access_key', type=str, default='', help='AWS Secret Access Key')
parser.add_argument('--region', type=str, default='', help='Cloud region')
parser.add_argument('--zone', type=str, default='', help='Cloud zone')
parser.add_argument('--conf_os_family', type=str, default='',
                    help='Operating system type. Available options: debian, redhat')
parser.add_argument('--conf_cloud_provider', type=str, default='',
                    help='Where DLab should be deployed. Available options: aws, gcp')
parser.add_argument('--aws_vpc_id', type=str, default='', help='AWS VPC ID')
parser.add_argument('--gcp_vpc_name', type=str, default='', help='GCP VPC Name')
parser.add_argument('--aws_subnet_id', type=str, default='', help='AWS Subnet ID')
parser.add_argument('--gcp_subnet_name', type=str, default='', help='GCP Subnet Name')
parser.add_argument('--aws_security_groups_ids', type=str, default='', help='One of more comma-separated Security groups IDs for SSN')
parser.add_argument('--gcp_firewall_rules', type=str, default='', help='One of more comma-separated GCP Firewall rules for SSN')
parser.add_argument('--key_path', type=str, default='', help='Path to admin key (WITHOUT KEY NAME)')
parser.add_argument('--conf_key_name', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--workspace_path', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--conf_tag_resource_id', type=str, default='dlab', help='The name of user tag')
parser.add_argument('--ssn_instance_size', type=str, default='t2.medium', help='The SSN instance shape')
parser.add_argument('--aws_account_id', type=str, default='', help='The ID of Amazon account')
parser.add_argument('--aws_billing_bucket', type=str, default='', help='The name of S3 bucket where billing reports will be placed.')
parser.add_argument('--aws_report_path', type=str, default='', help='The path to billing reports directory in S3 bucket')
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
    if args.conf_cloud_provider == 'gcp':
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
        local('sudo npm install gulp')
        local('sudo npm install')
        local('sudo npm run build.prod')
        local('sudo chown -R {} {}/*'.format(os.environ['USER'], args.workspace_path))


def build_services():
    # Building provisioning-service, security-service, self-service, billing
    local('mvn -DskipTests package')


def build_docker_images(args):
    # Building base and ssn docker images
    with lcd(args.workspace_path + '/infrastructure-provisioning/src/'):
        local('sudo docker build --build-arg OS={} --build-arg CLOUD={} --file base/Dockerfile '
              '-t docker.dlab-base .'.format(args.conf_os_family, args.conf_cloud_provider))
        local('sudo docker build --build-arg OS={} --build-arg CLOUD={} --file ssn/Dockerfile '
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
    local('cp {0}/services/billing/billing.yml {0}/web_app/billing/'.format(args.workspace_path))
    local('cp {0}/services/billing/target/billing-*.jar {0}/web_app/billing/'.
          format(args.workspace_path))
    # Creating SSN node
    docker_command = generate_docker_command()
    local(docker_command)
    # local('sudo docker run -i -v {0}{1}.pem:/root/keys/{1}.pem -v {2}/web_app:/root/web_app -e "conf_os_family={3}" '
    #       '-e "conf_cloud_provider={4}" -e "conf_resource=ssn" '
    #       '-e "ssn_instance_size=t2.medium" -e "region={5}" -e "aws_vpc_id={6}" -e "aws_subnet_id={7}" '
    #       '-e "aws_security_groups_ids={8}" -e "conf_key_name={1}" -e "conf_service_base_name={9}" '
    #       '-e "aws_access_key={10}" -e "aws_secret_access_key={11}" -e "conf_tag_resource_id={13}" '
    #       '-e "aws_account_id={14}" -e "aws_billing_bucket={15}" -e "aws_report_path={16}" '
    #       'docker.dlab-ssn --action {12}'.format(args.key_path, args.conf_key_name, args.workspace_path, args.conf_os_family,
    #                                              args.conf_cloud_provider, args.region, args.vpc_id,
    #                                              args.subnet_id, args.sg_ids, args.infrastructure_tag,
    #                                              args.access_key_id, args.secret_access_key, args.action,
    #                                              args.tag_resource_id, args.aws_account_id, args.aws_billing_bucket,
    #                                              args.aws_report_path))


def terminate_dlab(args):
    # Dropping Dlab environment with selected infrastructure tag
    local('sudo docker run -i -v {0}{1}.pem:/root/keys/{1}.pem -e "region={2}" -e "conf_service_base_name={3}" '
          '-e "conf_resource=ssn" -e "aws_access_key={4}" -e "aws_secret_access_key={5}" '
          'docker.dlab-ssn --action {6}'.
          format(args.key_path, args.conf_key_name, args.region, args.infrastructure_tag, args.access_key_id,
                 args.secret_access_key, args.action))

if __name__ == "__main__":
    if not args.workspace_path:
        print "Workspace path isn't set, using current directory: " + os.environ['PWD']
        args.workspace_path = os.environ['PWD']
    if args.action == 'build':
        build_front_end(args)
        build_services()
        build_docker_images(args)
    elif args.action == 'deploy':
        deploy_dlab(args)
    elif args.action == 'create':
        #build_front_end(args)
        #build_services()
        build_docker_images(args)
        deploy_dlab(args)
    elif args.action == 'terminate':
        build_docker_images(args)
        terminate_dlab(args)
