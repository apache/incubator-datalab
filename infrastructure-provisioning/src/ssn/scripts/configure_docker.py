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

import argparse
import json
import os
import sys
import time
from datalab.ssn_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_family', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--datalab_path', type=str, default='')
parser.add_argument('--cloud_provider', type=str, default='')
parser.add_argument('--resource', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--odahu_image', type=str, default='')
parser.add_argument('--gcr_creds', type=str, default='')
args = parser.parse_args()


def modify_conf_file(args):
    if os.environ['conf_duo_vpc_enable'] == 'true':
        os.environ['conf_vpc2_cidr'] = get_cidr_by_vpc(os.environ['aws_vpc2_id'])
    variables_list = {}
    host_string = '{}@{}'.format(args.os_user, args.hostname)
    for os_var in os.environ:
        if "'" not in os.environ[os_var] and os_var != 'aws_access_key' and os_var != 'aws_secret_access_key':
            variables_list[os_var] = os.environ[os_var]
    conn.local('rsync -r -e "ssh -i {}" /project_tree/* {}:{}sources/'.format(args.keyfile, host_string, args.datalab_path))
    conn.local('rsync -e "ssh -i {}" /root/scripts/configure_conf_file.py {}:/tmp/configure_conf_file.py'.format(args.keyfile,
                                                                                                 host_string))
    conn.sudo("python3 /tmp/configure_conf_file.py --datalab_dir {} --variables_list '{}'".format(
        args.datalab_path, json.dumps(variables_list)))


def download_toree():
    toree_path = '/opt/datalab/sources/infrastructure-provisioning/src/general/files/os/'
    tarball_link = 'https://dist.apache.org/repos/dist/dev/incubator/toree/0.5.0-incubating-rc1/toree/toree-0.5.0-incubating-bin.tar.gz'
    try:
        conn.run('wget {}'.format(tarball_link))
        conn.run('tar -xzf toree-0.5.0-incubating-bin.tar.gz')
        conn.run('mv toree-0.5.0-incubating-bin.tar.gz {}toree_kernel.tar.gz'.format(toree_path))
        conn.run('mv ./toree-0.5.0-incubating/lib/toree-assembly-0.5.0-incubating.jar {}toree-assembly-0.5.0.jar'.format(toree_path))
    except Exception as err:
        traceback.print_exc()
        print('Failed to download toree: ', str(err))
        sys.exit(1)


def login_in_gcr(os_user, gcr_creds, odahu_image, datalab_path, cloud_provider):
    if gcr_creds != '':
        try:
            if os.environ['conf_cloud_provider'] != 'gcp':
                try:
                    conn.sudo('echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt '
                          'cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list')
                    conn.sudo('apt-get -y install apt-transport-https ca-certificates gnupg')
                    conn.sudo('curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -')
                    conn.sudo('apt-get update')
                    conn.sudo('apt-get -y install google-cloud-sdk')
                except Exception as err:
                    traceback.print_exc()
                    print('Failed to install gcloud: ', str(err))
                    sys.exit(1)
            try:
                host_string = '{}@{}'.format(args.os_user, args.hostname)
                with open('/tmp/config', 'w') as f:
                    f.write(base64.b64decode(gcr_creds))
                conn.local('rsync -e "ssh -i {}" /tmp/config {}:/tmp/config'.format(args.keyfile, host_string, os_user))
                conn.sudo('mkdir /home/{}/.docker'.format(os_user))
                conn.sudo('cp /tmp/config /home/{}/.docker/config.json'.format(os_user))
                conn.sudo('sed -i "s|ODAHU_IMAGE|{}|" {}sources/infrastructure-provisioning/src/general/files/{}/odahu_Dockerfile'
                     .format(odahu_image, datalab_path, cloud_provider))
            except Exception as err:
                traceback.print_exc()
                print('Failed to prepare odahu image: ', str(err))
                sys.exit(1)
        except Exception as err:
            traceback.print_exc()
            print('Failed to prepare odahu image: ', str(err))
            sys.exit(1)

def build_docker_images(image_list):
    try:
        host_string = '{}@{}'.format(args.os_user, args.hostname)
        if os.environ['conf_cloud_provider'] == 'azure':
            conn.local('rsync -e "ssh -i {}" /root/azure_auth.json {}:{}sources/infrastructure-provisioning/src/base/'
                       'azure_auth.json'.format(args.keyfile, host_string, args.datalab_path))
            conn.sudo('cp {0}sources/infrastructure-provisioning/src/base/azure_auth.json '
                      '/home/{1}/keys/azure_auth.json'.format(args.datalab_path, args.os_user))
        if 'conf_repository_user' in os.environ and 'conf_repository_port' in os.environ and 'conf_repository_pass' in os.environ and 'conf_repository_address' in os.environ and os.environ['conf_download_docker_images'] == 'true':
            conn.sudo('sudo docker login -u {0} -p {1} {2}:{3}'
                      .format(os.environ['conf_repository_user'], os.environ['conf_repository_pass'], os.environ['conf_repository_address'], os.environ['conf_repository_port']))
            for image in image_list:
                name = image['name']
                tag = image['tag']
                conn.sudo('docker pull {0}:{4}/docker.datalab-{2}-{1}:{3}'
                          .format(os.environ['conf_repository_address'], os.environ['conf_cloud_provider'], name, tag, os.environ['conf_repository_port']))
                conn.sudo('docker image tag {0}:{4}/docker.datalab-{2}-{1}:{3} docker.datalab-{2}:{3}'
                          .format(os.environ['conf_repository_address'], os.environ['conf_cloud_provider'], name, tag, os.environ['conf_repository_port']))
                conn.sudo('docker image rm {0}:{4}/docker.datalab-{2}-{1}:{3}'
                          .format(os.environ['conf_repository_address'], os.environ['conf_cloud_provider'], name, tag, os.environ['conf_repository_port']))
            return True
        else:
            for image in image_list:
                name = image['name']
                tag = image['tag']
                conn.sudo('cp {0}sources/infrastructure-provisioning/src/general/files/{1}/{2}_description.json '
                 '{0}sources/infrastructure-provisioning/src/{2}/description.json'.format(args.datalab_path, args.cloud_provider, name))
                if name == 'base':
                    conn.sudo("bash -c 'cd {4}sources/infrastructure-provisioning/src/; docker build --build-arg OS={2} "
                          "--build-arg SRC_PATH=\"\" --file general/files/{3}/{0}_Dockerfile -t docker.datalab-{0}:{1} "
                          ".'".format(name, tag, args.os_family, args.cloud_provider, args.datalab_path))
                else:
                    conn.sudo("bash -c 'cd {4}sources/infrastructure-provisioning/src/; docker build --build-arg OS={2} "
                          "--file general/files/{3}/{0}_Dockerfile -t docker.datalab-{0}:{1} .'".format(name, tag, args.os_family, args.cloud_provider, args.datalab_path))
            conn.sudo('rm -f {}sources/infrastructure-provisioning/src/base/azure_auth.json'.format(args.datalab_path))
            return True
    except:
        return False


def configure_guacamole():
    try:
        mysql_pass = id_generator()
        conn.sudo('docker run --name guacd --restart unless-stopped -d -p 4822:4822 guacamole/guacd')
        conn.sudo('docker run --rm guacamole/guacamole:{} /opt/guacamole/bin/initdb.sh --mysql > initdb.sql'
                  .format(os.environ['ssn_guacamole_image_tag']))
        conn.sudo('mkdir /tmp/scripts')
        conn.sudo('cp initdb.sql /tmp/scripts')
        conn.sudo('mkdir /opt/mysql')
        conn.sudo('docker run --name guac-mysql --restart unless-stopped -v /tmp/scripts:/tmp/scripts '\
             ' -v /opt/mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD={} -d mysql:latest'.format(mysql_pass))
        time.sleep(180)
        conn.sudo('touch /opt/mysql/dock-query.sql')
        conn.sudo('''bash -c "echo \\"CREATE DATABASE guacamole; CREATE USER 'guacamole' IDENTIFIED BY '{}';''' \
             ''' GRANT SELECT,INSERT,UPDATE,DELETE ON guacamole.* TO 'guacamole';\\" > /opt/mysql/dock-query.sql"''' \
             .format(mysql_pass))
        conn.sudo('docker exec -i guac-mysql /bin/bash -c "mysql -u root -p{} < /var/lib/mysql/dock-query.sql"' \
             .format(mysql_pass))
        conn.sudo('docker exec -i guac-mysql /bin/bash -c "cat /tmp/scripts/initdb.sql | mysql -u root -p{} guacamole"' \
             .format(mysql_pass))
        conn.sudo("docker run --name guacamole --restart unless-stopped --link guacd:guacd --link guac-mysql:mysql" \
             " -e MYSQL_DATABASE='guacamole' -e MYSQL_USER='guacamole' -e MYSQL_PASSWORD='{}'" \
             " -d -p 8080:8080 guacamole/guacamole:{}".format(mysql_pass, os.environ['ssn_guacamole_image_tag']))
        # create cronjob for run containers on reboot
        conn.sudo('mkdir /opt/datalab/cron')
        conn.sudo('touch /opt/datalab/cron/mysql.sh')
        conn.sudo('chmod 755 /opt/datalab/cron/mysql.sh')
        conn.sudo('bash -c "echo \"docker start guacd\" >> /opt/datalab/cron/mysql.sh"')
        conn.sudo('bash -c "echo \"docker start guac-mysql\" >> /opt/datalab/cron/mysql.sh"')
        conn.sudo('bash -c "echo \"docker rm guacamole\" >> /opt/datalab/cron/mysql.sh"')
        conn.sudo('''bash -c "echo \\"docker run --name guacamole --restart unless-stopped --link guacd:guacd --link ''' \
                  '''guac-mysql:mysql -e MYSQL_DATABASE='guacamole' -e MYSQL_USER='guacamole' -e MYSQL_PASSWORD='{}' '''\
                  '''-d -p 8080:8080 guacamole/guacamole:{}\\" >> /opt/datalab/cron/mysql.sh"'''
                  .format(mysql_pass, os.environ['ssn_guacamole_image_tag']))
        conn.sudo("bash -c '(crontab -l 2>/dev/null; echo \"@reboot sh /opt/datalab/cron/mysql.sh\") | crontab -'")
        return True
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure guacamole: ', str(err))
        return False

def status_container_removal_cron():
    try:
        conn.sudo('bash -c \'echo "*/15 * * * * datalab-user docker container prune -f --filter until=50m --filter label=edge_status" >> /etc/crontab\'')
    except Exception as err:
        traceback.print_exc()
        print('Failed to create admin status container removal cron: ', str(err))
        sys.exit(1)

##############
# Run script #
##############
if __name__ == "__main__":
    print("Configure connections")
    try:
        global conn
        conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)
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
    if not ensure_docker_daemon(args.datalab_path, args.os_user, args.region):
        sys.exit(1)

    print("Login in Google Container Registry")
    login_in_gcr(args.os_user, args.gcr_creds, args.odahu_image, args.datalab_path, args.cloud_provider)

    print("Building Datalab images")
    count = 0
    while not build_docker_images(deeper_config) and count < 5:
        count += 1
        time.sleep(5)

    print("Configuring guacamole")
    if not configure_guacamole():
        sys.exit(1)

    print("Adding cron to remove edge status containers")
    status_container_removal_cron()

    conn.close()
    sys.exit(0)
