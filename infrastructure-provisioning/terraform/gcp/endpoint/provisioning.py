#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

import argparse
import logging
import sys
import time
import traceback
from fabric import Connection
from patchwork.files import exists
from patchwork import files

conn = None
args = None
java_home = None


def create_user():
    initial_user = 'ubuntu'
    sudo_group = 'sudo'
    with Connection(host=args.hostname, user=initial_user,
                    connect_kwargs={'key_filename': args.pkey}) as conn:
        try:
            if not exists(conn,
                          '/home/{}/.ssh_user_ensured'.format(initial_user)):
                conn.sudo('useradd -m -G {1} -s /bin/bash {0}'
                          .format(args.os_user, sudo_group))
                conn.sudo(
                    'bash -c \'echo "{} ALL = NOPASSWD:ALL" >> /etc/sudoers\''.format(args.os_user, initial_user))
                conn.sudo('mkdir /home/{}/.ssh'.format(args.os_user))
                conn.sudo('chown -R {0}:{0} /home/{1}/.ssh/'
                          .format(initial_user, args.os_user))
                conn.sudo('cat /home/{0}/.ssh/authorized_keys > '
                          '/home/{1}/.ssh/authorized_keys'
                          .format(initial_user, args.os_user))
                conn.sudo(
                    'chown -R {0}:{0} /home/{0}/.ssh/'.format(args.os_user))
                conn.sudo('chmod 700 /home/{0}/.ssh'.format(args.os_user))
                conn.sudo('chmod 600 /home/{0}/.ssh/authorized_keys'
                          .format(args.os_user))
                conn.sudo(
                    'touch /home/{}/.ssh_user_ensured'.format(initial_user))
        except Exception as err:
            logging.error('Failed to create new os_user: ', str(err))
            sys.exit(1)


def copy_keys():
    try:
        conn.put(args.pkey, '/home/{0}/keys/'.format(args.os_user))
        conn.sudo('chown -R {0}:{0} /home/{0}/keys'.format(args.os_user))
    except Exception as err:
        logging.error('Failed to copy admin key: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_dir_endpoint():
    try:
        if not exists(conn, '/home/{}/.ensure_dir'.format(args.os_user)):
            conn.sudo('mkdir /home/{}/.ensure_dir'.format(args.os_user))
    except Exception as err:
        logging.error('Failed to create ~/.ensure_dir/: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_logs_endpoint():
    log_root_dir = "/var/opt/datalab/log"
    supervisor_log_file = "/var/log/application/provision-service.log"
    try:
        if not exists(conn, '/home/' + args.os_user + '/.ensure_dir/logs_ensured'):
            if not exists(conn, args.datalab_path):
                conn.sudo("mkdir -p " + args.datalab_path)
                conn.sudo("chown -R " + args.os_user + ' ' + args.datalab_path)
            if not exists(conn, log_root_dir):
                conn.sudo('mkdir -p ' + log_root_dir + '/provisioning')
                conn.sudo('touch ' + log_root_dir + '/provisioning/provisioning.log')
            if not exists(conn, supervisor_log_file):
                conn.sudo("mkdir -p /var/log/application")
                conn.sudo("touch " + supervisor_log_file)
            conn.sudo("chown -R {0} {1}".format(args.os_user, log_root_dir))
            conn.sudo('touch /home/' + args.os_user + '/.ensure_dir/logs_ensured')
    except Exception as err:
        print('Failed to configure logs and DataLab directory: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_jre_jdk_endpoint():
    try:
        if not exists(conn, '/home/{}/.ensure_dir/jre_jdk_ensured'.format(args.os_user)):
            conn.sudo('apt-get install -y openjdk-8-jre-headless')
            conn.sudo('apt-get install -y openjdk-8-jdk-headless')
            conn.sudo('touch /home/{}/.ensure_dir/jre_jdk_ensured'
                      .format(args.os_user))
    except Exception as err:
        logging.error('Failed to install Java JDK: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_supervisor_endpoint():
    try:
        if not exists(conn, '/home/{}/.ensure_dir/superv_ensured'.format(args.os_user)):
            conn.sudo('apt-get -y install supervisor')
            conn.sudo('update-rc.d supervisor defaults')
            conn.sudo('update-rc.d supervisor enable')
            conn.sudo('touch /home/{}/.ensure_dir/superv_ensured'
                      .format(args.os_user))
    except Exception as err:
        logging.error('Failed to install Supervisor: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_docker_endpoint():
    try:
        if not exists(conn, '/home/{}/.ensure_dir/docker_ensured'.format(args.os_user)):
            conn.sudo("bash -c "
                      "'curl -fsSL https://download.docker.com/linux/ubuntu/gpg"
                      " | apt-key add -'")
            conn.sudo('add-apt-repository "deb [arch=amd64] '
                      'https://download.docker.com/linux/ubuntu '
                      '$(lsb_release -cs) stable"')
            conn.sudo('apt-get update')
            conn.sudo('apt-cache policy docker-ce')
            conn.sudo('apt-get install -y docker-ce={}'
                      .format(args.docker_version))
            if not exists(conn, '{}/tmp'.format(args.datalab_path)):
                conn.run('mkdir -p {}/tmp'.format(args.datalab_path))
            conn.put('./daemon.json',
                     '{}/tmp/daemon.json'.format(args.datalab_path))
            conn.sudo('sed -i "s|REPOSITORY|{}:{}|g" {}/tmp/daemon.json'
                      .format(args.repository_address,
                              args.repository_port,
                              args.datalab_path))
            if args.cloud_provider == "aws":
                dns_ip_resolve = (conn.run("systemd-resolve --status "
                                           "| grep -A 5 'Current Scopes: DNS' "
                                           "| grep 'DNS Servers:' "
                                           "| awk '{print $3}'")
                                  .stdout.rstrip("\n\r"))
                conn.sudo('sed -i "s|DNS_IP_RESOLVE|\"dns\": [{0}],|g" {1}/tmp/daemon.json'
                          .format(dns_ip_resolve, args.datalab_path))
            elif args.cloud_provider == "gcp":
                dns_ip_resolve = ""
                conn.sudo('sed -i "s|DNS_IP_RESOLVE||g" {1}/tmp/daemon.json'
                          .format(dns_ip_resolve, args.datalab_path))
            conn.sudo('mv {}/tmp/daemon.json /etc/docker'
                      .format(args.datalab_path))
            conn.sudo('usermod -a -G docker ' + args.os_user)
            conn.sudo('update-rc.d docker defaults')
            conn.sudo('update-rc.d docker enable')
            conn.sudo('service docker restart')
            conn.sudo('touch /home/{}/.ensure_dir/docker_ensured'
                      .format(args.os_user))
    except Exception as err:
        logging.error('Failed to install Docker: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def create_key_dir_endpoint():
    try:
        if not exists(conn, '/home/{}/keys'.format(args.os_user)):
            conn.run('mkdir /home/{}/keys'.format(args.os_user))
    except Exception as err:
        logging.error('Failed create keys directory as ~/keys: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_keystore_endpoint(os_user):
    try:
        # TEMPORARY COMENTED!!!
        if args.cloud_provider == "aws":
            conn.sudo('apt-get install -y awscli')
            if not exists(conn, '/home/' + args.os_user + '/keys/endpoint.keystore.jks'):
                conn.sudo('aws s3 cp s3://{0}/datalab/certs/endpoint/endpoint.keystore.jks '
                          '/home/{1}/keys/endpoint.keystore.jks'
                          .format(args.ssn_bucket_name, args.os_user))
            if not exists(conn, '/home/' + args.os_user + '/keys/datalab.crt'):
                conn.sudo('aws s3 cp s3://{0}/datalab/certs/endpoint/endpoint.crt'
                          ' /home/{1}/keys/endpoint.crt'.format(args.ssn_bucket_name, args.os_user))
        #     if not exists(conn, '/home/' + args.os_user + '/keys/ssn.crt'):
        #         conn.sudo('aws s3 cp '
        #                   's3://{0}/datalab/certs/ssn/ssn.crt /home/{1}/keys/ssn.crt'
        #                   .format(args.ssn_bucket_name, args.os_user))
        elif args.cloud_provider == "gcp":
            if not exists(conn, '/home/' + args.os_user + '/keys/endpoint.keystore.jks'):
                conn.sudo('gsutil -m cp -r gs://{0}/datalab/certs/endpoint/endpoint.keystore.jks '
                          '/home/{1}/keys/'
                          .format(args.ssn_bucket_name, args.os_user))
            if not exists(conn, '/home/' + args.os_user + '/keys/datalab.crt'):
                conn.sudo('gsutil -m cp -r gs://{0}/datalab/certs/endpoint/endpoint.crt'
                          ' /home/{1}/keys/'.format(args.ssn_bucket_name, args.os_user))
        #     if not exists(conn, '/home/' + args.os_user + '/keys/ssn.crt'):
        #         conn.sudo('gsutil -m cp -r '
        #                   'gs://{0}/datalab/certs/ssn/ssn.crt /home/{1}/keys/'
        #                   .format(args.ssn_bucket_name, args.os_user))
        if not exists(conn, '/home/' + args.os_user + '/.ensure_dir/cert_imported'):
            conn.sudo('keytool -importcert -trustcacerts -alias datalab -file /home/{0}/keys/endpoint.crt -noprompt \
                 -storepass changeit -keystore {1}/lib/security/cacerts'.format(os_user, java_home))
        #     conn.sudo('keytool -importcert -trustcacerts -file /home/{0}/keys/ssn.crt -noprompt \
        #          -storepass changeit -keystore {1}/lib/security/cacerts'.format(os_user, java_home))
            conn.sudo('touch /home/' + args.os_user + '/.ensure_dir/cert_imported')
        print("Certificates are imported.")
    except Exception as err:
        print('Failed to configure Keystore certificates: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_supervisor_endpoint():
    try:
        if not exists(conn,
                      '/home/{}/.ensure_dir/configure_supervisor_ensured'.format(args.os_user)):
            supervisor_conf = '/etc/supervisor/conf.d/supervisor_svc.conf'
            if not exists(conn, '{}/tmp'.format(args.datalab_path)):
                conn.run('mkdir -p {}/tmp'.format(args.datalab_path))
            conn.put('./supervisor_svc.conf',
                     '{}/tmp/supervisor_svc.conf'.format(args.datalab_path))
            datalab_conf_dir = '{}/conf/'.format(args.datalab_path)
            if not exists(conn, datalab_conf_dir):
                conn.run('mkdir -p {}'.format(datalab_conf_dir))
            web_path = '{}/webapp'.format(args.datalab_path)
            if not exists(conn, web_path):
                conn.run('mkdir -p {}'.format(web_path))
            conn.sudo('sed -i "s|OS_USR|{}|g" {}/tmp/supervisor_svc.conf'
                      .format(args.os_user, args.datalab_path))
            conn.sudo('sed -i "s|WEB_CONF|{}|g" {}/tmp/supervisor_svc.conf'
                      .format(datalab_conf_dir, args.datalab_path))
            conn.sudo('sed -i \'s=WEB_APP_DIR={}=\' {}/tmp/supervisor_svc.conf'
                      .format(web_path, args.datalab_path))
            conn.sudo('cp {}/tmp/supervisor_svc.conf {}'
                      .format(args.datalab_path, supervisor_conf))
            conn.put('./provisioning.yml', '{}provisioning.yml'
                     .format(datalab_conf_dir))
            conn.sudo('sed -i "s|KEYNAME|{}|g" {}provisioning.yml'
                      .format(args.key_name, datalab_conf_dir))
            conn.sudo('sed -i "s|KEYSTORE_PASSWORD|{}|g" {}provisioning.yml'
                      .format(args.endpoint_keystore_password, datalab_conf_dir))
            conn.sudo('sed -i "s|JRE_HOME|{}|g" {}provisioning.yml'
                      .format(java_home, datalab_conf_dir))
            conn.sudo('sed -i "s|CLOUD_PROVIDER|{}|g" {}provisioning.yml'
                      .format(args.cloud_provider, datalab_conf_dir))

            conn.sudo('sed -i "s|MONGO_HOST|{}|g" {}provisioning.yml'
                      .format(args.mongo_host, datalab_conf_dir))
            conn.sudo('sed -i "s|MONGO_PORT|{}|g" {}provisioning.yml'
                      .format(args.mongo_port, datalab_conf_dir))
            conn.sudo('sed -i "s|SS_HOST|{}|g" {}provisioning.yml'
                      .format(args.ss_host, datalab_conf_dir))
            conn.sudo('sed -i "s|SS_PORT|{}|g" {}provisioning.yml'
                      .format(args.ss_port, datalab_conf_dir))
            conn.sudo('sed -i "s|KEYCLOACK_HOST|{}|g" {}provisioning.yml'
                      .format(args.keycloack_host, datalab_conf_dir))

            conn.sudo('sed -i "s|CLIENT_SECRET|{}|g" {}provisioning.yml'
                      .format(args.keycloak_client_secret, datalab_conf_dir))
            # conn.sudo('sed -i "s|MONGO_PASSWORD|{}|g" {}provisioning.yml'
            #           .format(args.mongo_password, datalab_conf_dir))
            conn.sudo('sed -i "s|CONF_OS|{}|g" {}provisioning.yml'
                      .format(args.conf_os, datalab_conf_dir))
            conn.sudo('sed -i "s|SERVICE_BASE_NAME|{}|g" {}provisioning.yml'
                      .format(args.service_base_name, datalab_conf_dir))
            conn.sudo('sed -i "s|EDGE_INSTANCE_SIZE|{}|g" {}provisioning.yml'
                      .format(args.edge_instence_size, datalab_conf_dir))
            conn.sudo('sed -i "s|SUBNET_ID|{}|g" {}provisioning.yml'
                      .format(args.subnet_id, datalab_conf_dir))
            conn.sudo('sed -i "s|REGION|{}|g" {}provisioning.yml'
                      .format(args.region, datalab_conf_dir))
            conn.sudo('sed -i "s|ZONE|{}|g" {}provisioning.yml'
                      .format(args.zone, datalab_conf_dir))
            conn.sudo('sed -i "s|TAG_RESOURCE_ID|{}|g" {}provisioning.yml'
                      .format(args.tag_resource_id, datalab_conf_dir))
            conn.sudo('sed -i "s|SG_IDS|{}|g" {}provisioning.yml'
                      .format(args.sg_ids, datalab_conf_dir))
            conn.sudo('sed -i "s|SSN_INSTANCE_SIZE|{}|g" {}provisioning.yml'
                      .format(args.ssn_instance_size, datalab_conf_dir))
            conn.sudo('sed -i "s|VPC2_ID|{}|g" {}provisioning.yml'
                      .format(args.vpc2_id, datalab_conf_dir))
            conn.sudo('sed -i "s|SUBNET2_ID|{}|g" {}provisioning.yml'
                      .format(args.subnet2_id, datalab_conf_dir))
            conn.sudo('sed -i "s|CONF_KEY_DIR|{}|g" {}provisioning.yml'
                      .format(args.conf_key_dir, datalab_conf_dir))
            conn.sudo('sed -i "s|VPC_ID|{}|g" {}provisioning.yml'
                      .format(args.vpc_id, datalab_conf_dir))
            conn.sudo('sed -i "s|PEERING_ID|{}|g" {}provisioning.yml'
                      .format(args.peering_id, datalab_conf_dir))
            conn.sudo('sed -i "s|AZURE_RESOURCE_GROUP_NAME|{}|g" {}provisioning.yml'
                      .format(args.azure_resource_group_name, datalab_conf_dir))
            conn.sudo('sed -i "s|AZURE_SSN_STORAGE_ACCOUNT_TAG|{}|g" {}provisioning.yml'
                      .format(args.azure_ssn_storage_account_tag, datalab_conf_dir))
            conn.sudo('sed -i "s|AZURE_SHARED_STORAGE_ACCOUNT_TAG|{}|g" {}provisioning.yml'
                      .format(args.azure_shared_storage_account_tag, datalab_conf_dir))
            conn.sudo('sed -i "s|AZURE_DATALAKE_TAG|{}|g" {}provisioning.yml'
                      .format(args.azure_datalake_tag, datalab_conf_dir))
            conn.sudo('sed -i "s|AZURE_CLIENT_ID|{}|g" {}provisioning.yml'
                      .format(args.azure_client_id, datalab_conf_dir))
            conn.sudo('sed -i "s|GCP_PROJECT_ID|{}|g" {}provisioning.yml'
                      .format(args.gcp_project_id, datalab_conf_dir))
            conn.sudo('sed -i "s|LDAP_HOST|{}|g" {}provisioning.yml'
                      .format(args.ldap_host, datalab_conf_dir))
            conn.sudo('sed -i "s|LDAP_DN|{}|g" {}provisioning.yml'
                      .format(args.ldap_dn, datalab_conf_dir))
            conn.sudo('sed -i "s|LDAP_OU|{}|g" {}provisioning.yml'
                      .format(args.ldap_ou, datalab_conf_dir))
            conn.sudo('sed -i "s|LDAP_USER_NAME|{}|g" {}provisioning.yml'
                      .format(args.ldap_user_name, datalab_conf_dir))
            conn.sudo('sed -i "s|LDAP_USER_PASSWORD|{}|g" {}provisioning.yml'
                      .format(args.ldap_user_password, datalab_conf_dir))
            conn.sudo('touch /home/{}/.ensure_dir/configure_supervisor_ensured'
                      .format(args.os_user))
    except Exception as err:
        logging.error('Failed to configure Supervisor: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def ensure_jar_endpoint():
    try:
        ensure_file = ('/home/{}/.ensure_dir/backend_jar_ensured'
                       .format(args.os_user))
        if not exists(conn, ensure_file):
            web_path = '{}/webapp'.format(args.datalab_path)
            if not exists(conn, web_path):
                conn.run('mkdir -p {}'.format(web_path))
            if args.cloud_provider == "aws":
                if 'Failed' in conn.run('wget -P {}  --user={} --password={} '
                         'https://{}/repository/packages/aws/provisioning-service-'
                         '2.1.jar --no-check-certificate 2>&1 | tee /tmp/tee.tmp; if grep -w -i -E  "ERROR|Failed" /tmp/tee.tmp; then echo -e "==============\nFailed jar download.\n=============="; fi'
                         .format(web_path, args.repository_user,
                                 args.repository_pass, args.repository_address)).stdout:
                    sys.exit(1)
            elif args.cloud_provider == "gcp":
                if 'Failed' in conn.run('wget -P {}  --user={} --password={} '
                         'https://{}/repository/packages/gcp/provisioning-service-'
                         '2.1.jar --no-check-certificate 2>&1 | tee /tmp/tee.tmp; if grep -w -i -E  "ERROR|Failed" /tmp/tee.tmp; then echo -e "==============\nFailed jar download.\n=============="; fi'
                         .format(web_path, args.repository_user,
                                 args.repository_pass, args.repository_address)).stdout:
                    sys.exit(1)
            conn.run('mv {0}/*.jar {0}/provisioning-service.jar'
                     .format(web_path))
            conn.sudo('touch {}'.format(ensure_file))
    except Exception as err:
        logging.error('Failed to download jar-provisioner: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def start_supervisor_endpoint():
    try:
        conn.sudo("service supervisor restart")
    except Exception as err:
        logging.error('Unable to start Supervisor: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def get_sources():
    try:
        conn.run("git clone https://github.com/apache/incubator-datalab.git {0}/sources".format(args.datalab_path))
        if args.branch_name != "":
            conn.run("cd {0}/sources && git checkout {1} && cd".format(args.datalab_path, args.branch_name))
    except Exception as err:
        logging.error('Failed to download sources: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def pull_docker_images():
    try:
        ensure_file = ('/home/{}/.ensure_dir/docker_images_pulled'
                       .format(args.os_user))
        if not exists(conn, ensure_file):
            conn.sudo('docker login -u {} -p {} {}:{} 2>&1 | tee /tmp/tee.tmp; if grep -w -i -E  "ERROR" /tmp/tee.tmp; then echo -e "==============\nFailed docker login.\n=============="; fi'
                      .format(args.repository_user,
                              args.repository_pass,
                              args.repository_address,
                              args.repository_port))
            conn.sudo('docker pull {}:{}/docker.datalab-base-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-edge-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-project-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-jupyter-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-jupyter-gpu-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-rstudio-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-zeppelin-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-tensor-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-tensor-rstudio-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-tensor-jupyterlab-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-deeplearning-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-dataengine-service-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker pull {}:{}/docker.datalab-dataengine-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-base-{} docker.datalab-base'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-edge-{} docker.datalab-edge'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-project-{} docker.datalab-project'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-jupyter-{} docker.datalab-jupyter'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-rstudio-{} docker.datalab-rstudio'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-zeppelin-{} '
                      'docker.datalab-zeppelin'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-tensor-{} docker.datalab-tensor'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-tensor-rstudio-{} '
                      'docker.datalab-tensor-rstudio'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-tensor-jupyterlab-{} '
                      'docker.datalab-tensor-jupyterlab'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-deeplearning-{} '
                      'docker.datalab-deeplearning'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-dataengine-service-{} '
                      'docker.datalab-dataengine-service'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker tag {}:{}/docker.datalab-dataengine-{} '
                      'docker.datalab-dataengine'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-base-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-edge-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-project-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-jupyter-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-rstudio-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-zeppelin-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-tensor-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-tensor-rstudio-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-tensor-jupyterlab-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-deeplearning-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-dataengine-service-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('docker rmi {}:{}/docker.datalab-dataengine-{}'
                      .format(args.repository_address, args.repository_port, args.cloud_provider))
            conn.sudo('chown -R {0}:docker /home/{0}/.docker/'
                      .format(args.os_user))
            conn.sudo('touch {}'.format(ensure_file))
    except Exception as err:
        logging.error('Failed to pull Docker images: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def init_args():
    global args
    parser = argparse.ArgumentParser()
    parser.add_argument('--datalab_path', type=str, default='/opt/datalab')
    parser.add_argument('--key_name', type=str, default='', help='Name of admin key without .pem extension')
    parser.add_argument('--endpoint_eip_address', type=str)
    parser.add_argument('--pkey', type=str, default='')
    parser.add_argument('--hostname', type=str, default='')
    parser.add_argument('--os_user', type=str, default='datalab-user')
    parser.add_argument('--cloud_provider', type=str, default='')

    parser.add_argument('--mongo_host', type=str, default='MONGO_HOST')
    parser.add_argument('--mongo_port', type=str, default='27017')
    parser.add_argument('--ss_host', type=str, default='')
    parser.add_argument('--ss_port', type=str, default='8443')
    parser.add_argument('--keycloack_host', type=str, default='')

    # parser.add_argument('--mongo_password', type=str, default='')
    parser.add_argument('--repository_address', type=str, default='')
    parser.add_argument('--repository_port', type=str, default='')
    parser.add_argument('--repository_user', type=str, default='')
    parser.add_argument('--repository_pass', type=str, default='')
    parser.add_argument('--docker_version', type=str,
                        default='5:20.10.2~3-0~ubuntu-focal')
    parser.add_argument('--ssn_bucket_name', type=str, default='')
    parser.add_argument('--endpoint_keystore_password', type=str, default='')
    parser.add_argument('--keycloak_client_secret', type=str, default='')
    parser.add_argument('--branch_name', type=str, default='master')  # change default

    parser.add_argument('--conf_os', type=str, default='debian')
    parser.add_argument('--service_base_name', type=str, default='')
    parser.add_argument('--edge_instence_size', type=str, default='')
    parser.add_argument('--subnet_id', type=str, default='')
    parser.add_argument('--region', type=str, default='')
    parser.add_argument('--zone', type=str, default='')
    parser.add_argument('--tag_resource_id', type=str, default='')
    parser.add_argument('--sg_ids', type=str, default='')
    parser.add_argument('--ssn_instance_size', type=str, default='')
    parser.add_argument('--vpc2_id', type=str, default='')
    parser.add_argument('--subnet2_id', type=str, default='')
    parser.add_argument('--conf_key_dir', type=str, default='/root/keys/', help='Should end by symbol /')
    parser.add_argument('--vpc_id', type=str, default='')
    parser.add_argument('--peering_id', type=str, default='')
    parser.add_argument('--azure_resource_group_name', type=str, default='')
    parser.add_argument('--azure_ssn_storage_account_tag', type=str, default='')
    parser.add_argument('--azure_shared_storage_account_tag', type=str, default='')
    parser.add_argument('--azure_datalake_tag', type=str, default='')
    parser.add_argument('--azure_client_id', type=str, default='')
    parser.add_argument('--gcp_project_id', type=str, default='')
    parser.add_argument('--ldap_host', type=str, default='')
    parser.add_argument('--ldap_dn', type=str, default='')
    parser.add_argument('--ldap_ou', type=str, default='')
    parser.add_argument('--ldap_user_name', type=str, default='')
    parser.add_argument('--ldap_user_password', type=str, default='')
    print(parser.parse_known_args())
    args = parser.parse_known_args()[0]


def update_system():
    conn.sudo('apt-get update')


def init_datalab_connection(ip=None, user=None,
                            pkey=None):
    global conn
    if not ip:
        ip = args.hostname
    if not user:
        user = args.os_user
    if not pkey:
        pkey = args.pkey
    try:
        conn = Connection(ip, user, connect_kwargs={'key_filename': pkey})
    except Exception as err:
        logging.error('Failed connect as datalab-user: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def set_java_home():
    global java_home
    command = ('bash -c "update-alternatives --query java | grep \'Value: \' '
               '| grep -o \'/.*/jre\'" ')
    java_home = (conn.sudo(command).stdout.rstrip("\n\r"))


def close_connection():
    global conn
    conn.close()


def start_deploy():
    global args
    init_args()
    print(args)
    if args.hostname == "":
        args.hostname = args.endpoint_eip_address

    print("Start provisioning of Endpoint.")
    time.sleep(40)

    print(args)
    logging.info("Creating datalab-user")
    create_user()

    init_datalab_connection()
    update_system()

    logging.info("Configuring ensure dir")
    ensure_dir_endpoint()

    logging.info("Configuring Logs")
    ensure_logs_endpoint()

    logging.info("Installing Java")
    ensure_jre_jdk_endpoint()

    set_java_home()

    logging.info("Installing Supervisor")
    ensure_supervisor_endpoint()

    logging.info("Installing Docker")
    ensure_docker_endpoint()

    logging.info("Configuring Supervisor")
    configure_supervisor_endpoint()

    logging.info("Creating key directory")
    create_key_dir_endpoint()

    logging.info("Copying admin key")
    copy_keys()

    logging.info("Configuring certificates")
    configure_keystore_endpoint(args.os_user)

    logging.info("Ensure jar")
    ensure_jar_endpoint()

    logging.info("Downloading sources")
    get_sources()

    logging.info("Pulling docker images")
    pull_docker_images()

    logging.info("Starting supervisor")
    start_supervisor_endpoint()

    close_connection()
    print("Done provisioning of Endpoint.")


if __name__ == "__main__":
    start_deploy()
