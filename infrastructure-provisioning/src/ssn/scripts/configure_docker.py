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
import base64

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
parser.add_argument('--gcr_creds', type=str, default='')
parser.add_argument('--odahu_image', type=str, default='')
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


def add_china_repository(dlab_path):
    with cd('{}sources/infrastructure-provisioning/src/base/'.format(dlab_path)):
        sudo('sed -i "/pip install/s/$/ -i https\:\/\/{0}\/simple --trusted-host {0} --timeout 60000/g" '
             'Dockerfile'.format(os.environ['conf_pypi_mirror']))
        sudo('sed -i "/pip install/s/jupyter/ipython==5.0.0 jupyter==1.0.0/g" Dockerfile')
        sudo('sed -i "22i COPY general/files/os/debian/sources.list /etc/apt/sources.list" Dockerfile')

def login_in_gcr(os_user, gcr_creds, odahu_image, dlab_path):
    if os.environ['conf_cloud_provider'] != 'gcp':
        try:
            sudo('echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt '
                  'cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list')
            sudo('apt-get -y install apt-transport-https ca-certificates gnupg')
            sudo('curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -')
            sudo('apt-get update')
            sudo('apt-get -y install google-cloud-sdk')
        except Exception as err:
            traceback.print_exc()
            print('Failed to install gcloud: ', str(err))
            sys.exit(1)
    try:
        #with open('/tmp/config', 'w') as f:
        #    f.write(gcr_creds)
        #local('scp -i {} /tmp/config {}:/tmp/config'.format(args.keyfile, env.host_string))
        sudo('mkdir /home/{}/.docker'.format(os_user))
        with open('/home/{}/.docker/config.json'.format(os_user), 'w') as f:
            f.write(base64.b64decode(gcr_creds) + "==")
        #sudo('cat /tmp/config')
        #sudo('cat /tmp/config | base64 --decode > /home/{}/.docker/config.json'.format(os_user))
        sudo('sed -i "s|ODAHU_IMAGE|{}|" {}sources/infrastructure-provisioning/src/general/files/gcp/odahu_Dockerfile'.format(odahu_image, dlab_path))
    except Exception as err:
        traceback.print_exc()
        print('Failed to prepare odahu image: ', str(err))
        sys.exit(1)

def build_docker_images(image_list, region, dlab_path):
    try:
        if os.environ['conf_cloud_provider'] == 'azure':
            local('scp -i {} /root/azure_auth.json {}:{}sources/infrastructure-provisioning/src/base/'
                  'azure_auth.json'.format(args.keyfile, env.host_string, args.dlab_path))
            sudo('cp {0}sources/infrastructure-provisioning/src/base/azure_auth.json '
                 '/home/{1}/keys/azure_auth.json'.format(args.dlab_path, args.os_user))
        if region == 'cn-north-1':
            add_china_repository(dlab_path)
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


def configure_guacamole():
    try:
        mysql_pass = id_generator()
        sudo('docker run --name guacd --restart unless-stopped -d -p 4822:4822 guacamole/guacd')
        sudo('docker run --rm guacamole/guacamole /opt/guacamole/bin/initdb.sh --mysql > initdb.sql')
        sudo('mkdir /tmp/scripts')
        sudo('cp initdb.sql /tmp/scripts')
        sudo('mkdir /opt/mysql')
        sudo('docker run --name guac-mysql --restart unless-stopped -v /tmp/scripts:/tmp/scripts '\
             ' -v /opt/mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD={} -d mysql:latest'.format(mysql_pass))
        time.sleep(180)
        sudo('touch /opt/mysql/dock-query.sql')
        sudo("""echo "CREATE DATABASE guacamole; CREATE USER 'guacamole' IDENTIFIED BY '{}';"""\
             """ GRANT SELECT,INSERT,UPDATE,DELETE ON guacamole.* TO 'guacamole';" > /opt/mysql/dock-query.sql"""\
             .format(mysql_pass))
        sudo('docker exec -i guac-mysql /bin/bash -c "mysql -u root -p{} < /var/lib/mysql/dock-query.sql"'\
             .format(mysql_pass))
        sudo('docker exec -i guac-mysql /bin/bash -c "cat /tmp/scripts/initdb.sql | mysql -u root -p{} guacamole"'\
             .format(mysql_pass))
        sudo("docker run --name guacamole --restart unless-stopped --link guacd:guacd --link guac-mysql:mysql"\
             " -e MYSQL_DATABASE='guacamole' -e MYSQL_USER='guacamole' -e MYSQL_PASSWORD='{}'"\
             " -d -p 8080:8080 guacamole/guacamole".format(mysql_pass))
        #create cronjob for run containers on reboot
        sudo('mkdir /opt/dlab/cron')
        sudo('touch /opt/dlab/cron/mysql.sh')
        sudo('chmod 755 /opt/dlab/cron/mysql.sh')
        sudo('echo "docker start guacd" >> /opt/dlab/cron/mysql.sh')
        sudo('echo "docker start guac-mysql" >> /opt/dlab/cron/mysql.sh')
        sudo('echo "docker rm guacamole" >> /opt/dlab/cron/mysql.sh')
        sudo("""echo "docker run --name guacamole --restart unless-stopped --link guacd:guacd --link guac-mysql:mysql"""\
             """ -e MYSQL_DATABASE='guacamole' -e MYSQL_USER='guacamole' -e MYSQL_PASSWORD='{}' -d"""\
             """ -p 8080:8080 guacamole/guacamole" >> /opt/dlab/cron/mysql.sh""".format(mysql_pass))
        sudo('(crontab -l 2>/dev/null; echo "@reboot sh /opt/dlab/cron/mysql.sh") | crontab -')
        return True
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure guacamole: ', str(err))
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

    print("Login in Google Container Registry")
    login_in_gcr(args.os_user, args.gcr_creds, args.odahu_image, args.dlab_path)

    print("Building dlab images")
    count = 0
    while not build_docker_images(deeper_config, args.region, args.dlab_path) and count < 5:
        count += 1
        time.sleep(5)

    print("Configuring guacamole")
    if not configure_guacamole():
        sys.exit(1)

    sys.exit(0)
