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
import json
import sys
from dlab.ssn_lib import *
import os
import time

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_family', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--dlab_path', type=str, default='')
parser.add_argument('--cloud_provider', type=str, default='')
parser.add_argument('--resource', type=str, default='')
parser.add_argument('--region', type=str, default='')
args = parser.parse_args()


def modify_conf_file(args):
    if os.environ['conf_duo_vpc_enable'] == 'true':
        os.environ['conf_vpc2_cidr'] = get_cidr_by_vpc(os.environ['aws_vpc2_id'])
    variables_list = {}
    for os_var in os.environ:
        if "'" not in os.environ[os_var] and os_var != 'aws_access_key' and os_var != 'aws_secret_access_key':
            variables_list[os_var] = os.environ[os_var]
    local('scp -r -i {} /project_tree/* {}:{}sources/'.format(args.keyfile, env.host_string, args.dlab_path))
    local('scp -i {} /root/scripts/configure_conf_file.py {}:/tmp/configure_conf_file.py'.format(args.keyfile,
                                                                                                 env.host_string))
    sudo("python /tmp/configure_conf_file.py --dlab_dir {} --variables_list '{}'".format(
        args.dlab_path, json.dumps(variables_list)))

def download_toree():
    toree_path = '/opt/dlab/sources/infrastructure-provisioning/src/general/files/os/'
    tarball_link = 'https://archive.apache.org/dist/incubator/toree/0.2.0-incubating/toree/toree-0.2.0-incubating-bin.tar.gz'
    jar_link = 'https://repo1.maven.org/maven2/org/apache/toree/toree-assembly/0.2.0-incubating/toree-assembly-0.2.0-incubating.jar'
    try:
        run('wget {}'.format(tarball_link))
        run('wget {}'.format(jar_link))
        run('mv toree-0.2.0-incubating-bin.tar.gz {}toree_kernel.tar.gz'.format(toree_path))
        run('mv toree-assembly-0.2.0-incubating.jar {}toree-assembly-0.2.0.jar'.format(toree_path))
    except Exception as err:
        traceback.print_exc()
        print('Failed to download toree: ', str(err))
        sys.exit(1)

def update_repository(dlab_path, repository_host, region):
    with cd('{}sources/infrastructure-provisioning/src/general/files/aws/'.format(dlab_path)):
        if region == 'cn-north-1':
            sudo('sed -i "/pip install/s/$/ -i https\:\/\/{0}\/simple --trusted-host {0} --timeout 60000/g" '
                 'base_Dockerfile'.format(repository_host))
            sudo('sed -i "/pip install/s/jupyter/ipython==5.0.0 jupyter==1.0.0/g" base_Dockerfile')
            sudo('sed -i "22i COPY general/files/os/debian/sources.list /etc/apt/sources.list" base_Dockerfile')
        if os.environ['local_repository_enabled'] == 'True':
            sudo('sed -i "s|^FROM ubuntu.*|FROM {0}/dlab-pre-base|g" base_Dockerfile'.format(
                os.environ['local_repository_docker_internal_repo']))
            sudo('sed -i "/pip install/d;/apt-get/d" base_Dockerfile')
            if 'local_repository_user_name' in os.environ and 'local_repository_user_password' in os.environ:
                sudo('docker login -u {1} -p {2} {0}'.format(os.environ['local_repository_docker_internal_repo'],
                                                             os.environ['local_repository_user_name'],
                                                             os.environ['local_repository_user_password']))
            else:
                sudo('docker login -u anonymous -p anonymous {0}'.format(
                    os.environ['local_repository_docker_internal_repo']))
        else:
            sudo('''sed -i "23i RUN sed -i 's|REPOSITORY_UBUNTU|{}|g' /etc/apt/sources.list" base_Dockerfile'''.format(
                repository_host))
            sudo('''sed -i "24i RUN sed -i 's|REPOSITORY_SECURITY_UBUNTU|{}|g' /etc/apt/sources.list" '''
                 '''base_Dockerfile'''.format(repository_host))


def build_docker_images(image_list, region, dlab_path):
    try:
        if os.environ['conf_cloud_provider'] == 'azure':
            local('scp -i {} /root/azure_auth.json {}:{}sources/infrastructure-provisioning/src/base/'
                  'azure_auth.json'.format(args.keyfile, env.host_string, args.dlab_path))
            sudo('cp {0}sources/infrastructure-provisioning/src/base/azure_auth.json '
                 '/home/{1}/keys/azure_auth.json'.format(args.dlab_path, args.os_user))
        if region == 'cn-north-1':
            update_repository(dlab_path, os.environ['conf_pypi_mirror'], region)
        if os.environ['local_repository_enabled'] == 'True':
            update_repository(dlab_path, os.environ['local_repository_host'], region)
        if os.environ['local_repository_enabled'] == 'True':
            if 'local_repository_cert_path' in os.environ:
                sudo('mkdir -p {}sources/infrastructure-provisioning/src/base/certs'.format(args.dlab_path))
                put('/root/certs/repository.crt', '{}sources/infrastructure-provisioning/src/base/certs/'
                                                  'repository.crt'.format(args.dlab_path), use_sudo=True)
        for image in image_list:
            name = image['name']
            tag = image['tag']
            sudo('cd {0}sources/infrastructure-provisioning/src/; cp general/files/{1}/{2}_description.json '
                 '{2}/description.json'.format(args.dlab_path, args.cloud_provider, name))
            if name == 'base':
                sudo("cd {4}sources/infrastructure-provisioning/src/; docker build --build-arg OS={2} "
                     "--build-arg SRC_PATH="" --file general/files/{3}/{0}_Dockerfile "
                     "-t docker.dlab-{0}:{1} .".format(name, tag, args.os_family, args.cloud_provider, args.dlab_path))
            else:
                sudo("cd {4}sources/infrastructure-provisioning/src/; docker build --build-arg OS={2} "
                     "--file general/files/{3}/{0}_Dockerfile -t docker.dlab-{0}:{1} .".format(name, tag,
                                                                                               args.os_family,
                                                                                               args.cloud_provider,
                                                                                               args.dlab_path))
        sudo('rm -f {}sources/infrastructure-provisioning/src/base/azure_auth.json'.format(args.dlab_path))
        return True
    except:
        return False


##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = args.os_user + '@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)

    print('Modifying configuration files')
    try:
        modify_conf_file(args)
    except Exception as err:
        print('Error:', str(err))
        sys.exit(1)

    print("Downloading Apache Toree")
    download_toree()

    print("Installing docker daemon")
    if not ensure_docker_daemon(args.dlab_path, args.os_user, args.region):
        sys.exit(1)

    print("Building dlab images")
    if not build_docker_images(deeper_config, args.region, args.dlab_path):
        sys.exit(1)

    sys.exit(0)
