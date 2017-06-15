#!/usr/bin/python

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

from fabric.api import *
import argparse
import json
import sys
from dlab.ssn_lib import *
import os

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


def add_china_repository(dlab_path):
    with cd('{}sources/base/'.format(dlab_path)):
        sudo('sed -i "/pip install/s/$/ -i https\:\/\/{0}\/simple --trusted-host {0} --timeout 60000/g" Dockerfile'.format(os.environ['conf_pypi_mirror']))
        sudo('sed -i "/pip install/s/jupyter/ipython==5.0.0 jupyter==1.0.0/g" Dockerfile')
        sudo('sed -i "22i COPY general/files/os/debian/sources.list /etc/apt/sources.list" Dockerfile')


def build_docker_images(image_list, region, dlab_path):
    try:
        local('scp -r -i {} /project_tree/* {}:{}sources/'.format(args.keyfile, env.host_string, args.dlab_path))
        if region == 'cn-north-1':
            add_china_repository(dlab_path)
        for image in image_list:
            name = image['name']
            tag = image['tag']
            sudo("cd {4}sources/; docker build --build-arg OS={2} --build-arg CLOUD={3} --file {0}/Dockerfile -t docker.dlab-{0}:{1} ."
                 .format(name, tag, args.os_family, args.cloud_provider, args.dlab_path))
        return True
    except:
        return False


##############
# Run script #
##############
if __name__ == "__main__":
    print "Configure connections"
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = args.os_user + '@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)

    print "Installing docker daemon"
    if not ensure_docker_daemon(args.dlab_path, args.os_user, args.region):
        sys.exit(1)

    print "Building dlab images"
    if not build_docker_images(deeper_config, args.region, args.dlab_path):
        sys.exit(1)

    sys.exit(0)
