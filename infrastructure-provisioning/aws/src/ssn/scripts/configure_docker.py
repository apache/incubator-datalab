#!/usr/bin/python

# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/

from fabric.api import *
from fabric.contrib.files import exists
import argparse
import json
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
args = parser.parse_args()


def ensure_docker_daemon():
    try:
        if not exists('/tmp/docker_daemon_ensured'):
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D')
            sudo('echo "deb https://apt.dockerproject.org/repo ubuntu-xenial main" | sudo tee /etc/apt/sources.list.d/docker.list')
            sudo('apt-get update')
            sudo('apt-cache policy docker-engine')
            sudo('apt-get install -y docker-engine')
            sudo('usermod -a -G docker ubuntu')
            sudo('sysv-rc-conf docker on')
            sudo('touch /tmp/docker_daemon_ensured')
        return True
    except:
        return False


def configure_docker_daemon():
    return True


def pull_docker_images(image_list):
    return True


def build_docker_images(image_list):
    try:
        sudo('mkdir /project_images; chown ubuntu /project_images')
        local('scp -r -i %s /project_tree/* %s:/project_images/' % (args.keyfile, env.host_string))
        for image in image_list:
            name = image['name']
            tag = image['tag']
            sudo("cd /project_images/%s; docker build "
                 "-t docker.epmc-bdcc.projects.epam.com/dlab-aws-%s:%s ." % (name, name, tag))
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
        env.host_string = 'ubuntu@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)

    print "Installing docker daemon"
    if not ensure_docker_daemon():
        sys.exit(1)

    print "Configuring docker daemon"
    if not configure_docker_daemon():
        sys.exit(1)

    print "Building dlab images"
    if not build_docker_images(deeper_config):
        sys.exit(1)

    sys.exit(0)
