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
parser.add_argument('--infrastructure_tag', type=str, help='unique name for DLab environment')
parser.add_argument('--access_key_id', type=str, default='', help='AWS Access Key ID')
parser.add_argument('--secret_access_key', type=str, default='', help='AWS Secret Access Key')
parser.add_argument('--region', type=str, default='', help='AWS region')
parser.add_argument('--os_family', type=str, default='',
                    help='Operating system type. Available options: debian, redhat')
parser.add_argument('--cloud_provider', type=str, default='',
                    help='Where DLab should be deployed. Available options: aws')
parser.add_argument('--os_user', type=str, default='',
                    help='Name of OS user. By default for Debian - ubuntu, RedHat - ec2-user')
parser.add_argument('--vpc_id', type=str, default='', help='AWS VPC ID')
parser.add_argument('--subnet_id', type=str, default='', help='AWS Subnet ID')
parser.add_argument('--sg_ids', type=str, default='', help='One of more comma-separated Security groups IDs for SSN')
parser.add_argument('--key_path', type=str, default='', help='Path to admin key (WITHOUT KEY NAME)')
parser.add_argument('--key_name', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--workspace_path', type=str, default='', help='Admin key name (WITHOUT ".pem")')
parser.add_argument('--tag_resource_id', type=str, default='dlab', help='The name of user tag')
parser.add_argument('--aws_account_id', type=str, default='', help='The ID of Amazon account')
parser.add_argument('--aws_billing_bucket', type=str, default='', help='The name of S3 bucket where billing reports will be placed.')
parser.add_argument('--aws_report_path', type=str, default='', help='The path to billing reports directory in S3 bucket')
parser.add_argument('--action', required=True, type=str, default='', choices=['build', 'deploy', 'create', 'terminate'],
                    help='Available options: build, deploy, create, terminate')
args = parser.parse_args()


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
              '-t docker.dlab-base .'.format(args.os_family, args.cloud_provider))
        local('sudo docker build --build-arg OS={} --build-arg CLOUD={} --file ssn/Dockerfile '
              '-t docker.dlab-ssn .'.format(args.os_family, args.cloud_provider))


def deploy_dlab(args):
    # Preparing files for deployment
    local('mkdir -p {}/web_app'.format(args.workspace_path))
    local('mkdir -p {}/web_app/provisioning-service/'.format(args.workspace_path))
    local('mkdir -p {}/web_app/security-service/'.format(args.workspace_path))
    local('mkdir -p {}/web_app/self-service/'.format(args.workspace_path))
    local('mkdir -p {}/web_app/billing/'.format(args.workspace_path))
    local('cp {0}/services/self-service/self-service.yml {0}/web_app/self-service/'.format(args.workspace_path))
    local('cp {0}/services/self-service/target/self-service-1.0.jar {0}/web_app/self-service/'.
          format(args.workspace_path))
    local('cp {0}/services/provisioning-service/provisioning.yml {0}/web_app/provisioning-service/'.
          format(args.workspace_path))
    local('cp {0}/services/provisioning-service/target/provisioning-service-1.0.jar {0}/web_app/provisioning-service/'.
          format(args.workspace_path))
    local('cp {0}/services/security-service/security.yml {0}/web_app/security-service/'.format(args.workspace_path))
    local('cp {0}/services/security-service/target/security-service-1.0.jar {0}/web_app/security-service/'.
          format(args.workspace_path))
    local('cp {0}/services/billing/billing.yml {0}/web_app/billing/'.format(args.workspace_path))
    local('cp {0}/services/billing/target/billing-1.0.jar {0}/web_app/billing/'.
          format(args.workspace_path))
    # Creating SSN node
    local('sudo docker run -i -v {0}{1}.pem:/root/keys/{1}.pem -v {2}/web_app:/root/web_app -e "conf_os_family={3}" '
          '-e "conf_os_user={4}" -e "conf_cloud_provider={5}" -e "conf_resource=ssn" '
          '-e "aws_ssn_instance_size=t2.medium" -e "aws_region={6}" -e "aws_vpc_id={7}" -e "aws_subnet_id={8}" '
          '-e "aws_security_groups_ids={9}" -e "conf_key_name={1}" -e "conf_service_base_name={10}" '
          '-e "aws_access_key={11}" -e "aws_secret_access_key={12}" -e "conf_tag_resource_id={14}" '
          '-e "aws_account_id={15}" -e "aws_billing_bucket={16}" -e "aws_report_path={17}" '
          'docker.dlab-ssn --action {13}'.format(args.key_path, args.key_name, args.workspace_path, args.os_family,
                                                 args.os_user, args.cloud_provider, args.region, args.vpc_id,
                                                 args.subnet_id, args.sg_ids, args.infrastructure_tag,
                                                 args.access_key_id, args.secret_access_key, args.action,
                                                 args.tag_resource_id, args.aws_account_id, args.aws_billing_bucket,
                                                 args.aws_report_path))


def terminate_dlab(args):
    # Dropping Dlab environment with selected infrastructure tag
    local('sudo docker run -i -v {0}{1}.pem:/root/keys/{1}.pem -e "aws_region={2}" -e "conf_service_base_name={3}" '
          '-e "conf_resource=ssn" -e "aws_access_key={4}" -e "aws_secret_access_key={5}" '
          'docker.dlab-ssn --action {6}'.
          format(args.key_path, args.key_name, args.region, args.infrastructure_tag, args.access_key_id,
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
        build_front_end(args)
        build_services()
        build_docker_images(args)
        deploy_dlab(args)
    elif args.action == 'terminate':
        terminate_dlab(args)
