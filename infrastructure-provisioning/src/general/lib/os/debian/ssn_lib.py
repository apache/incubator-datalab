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
import crypt
import yaml
from dlab.fab import *
from dlab.meta_lib import *
import os
import json
import traceback
import sys


def ensure_docker_daemon(dlab_path, os_user, region):
    try:
        if not exists(dlab_path + 'tmp/docker_daemon_ensured'):
            docker_version = os.environ['ssn_docker_version']
            sudo('curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -')
            sudo('add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) \
                  stable"')
            sudo('apt-get update')
            sudo('apt-cache policy docker-ce')
            sudo('apt-get install -y docker-ce={}~ce-0~ubuntu'.format(docker_version))
            sudo('usermod -a -G docker ' + os_user)
            sudo('update-rc.d docker defaults')
            sudo('update-rc.d docker enable')
            sudo('touch ' + dlab_path + 'tmp/docker_daemon_ensured')
        return True
    except:
        return False


def ensure_nginx(dlab_path):
    try:
        if not exists(dlab_path + 'tmp/nginx_ensured'):
            sudo('apt-get -y install nginx')
            sudo('service nginx restart')
            sudo('update-rc.d nginx defaults')
            sudo('update-rc.d nginx enable')
            sudo('touch ' + dlab_path + 'tmp/nginx_ensured')
    except Exception as err:
        traceback.print_exc()
        print('Failed to ensure Nginx: ', str(err))
        sys.exit(1)


def ensure_jenkins(dlab_path):
    try:
        if not exists(dlab_path + 'tmp/jenkins_ensured'):
            sudo('wget -q -O - https://pkg.jenkins.io/debian/jenkins-ci.org.key | apt-key add -')
            sudo('echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list')
            sudo('apt-get -y update')
            sudo('apt-get -y install openjdk-8-jdk')
            sudo('apt-get -y install jenkins')
            sudo('touch ' + dlab_path + 'tmp/jenkins_ensured')
    except Exception as err:
        traceback.print_exc()
        print('Failed to ensure Jenkins: ', str(err))
        sys.exit(1)


def configure_jenkins(dlab_path, os_user, config, tag_resource_id):
    try:
        if not exists(dlab_path + 'tmp/jenkins_configured'):
            sudo('echo \'JENKINS_ARGS="--prefix=/jenkins --httpPort=8070"\' >> /etc/default/jenkins')
            sudo('rm -rf /var/lib/jenkins/*')
            sudo('mkdir -p /var/lib/jenkins/jobs/')
            sudo('chown -R ' + os_user + ':' + os_user + ' /var/lib/jenkins/')
            put('/root/templates/jenkins_jobs/*', '/var/lib/jenkins/jobs/')
            sudo("find /var/lib/jenkins/jobs/ -type f | xargs sed -i \'s/OS_USR/{}/g; s/SBN/{}/g; s/CTUN/{}/g; s/SGI/{}/g; s/VPC/{}/g; s/SNI/{}/g; s/AKEY/{}/g\'".format(os_user, config['service_base_name'], tag_resource_id, config['security_group_id'], config['vpc_id'], config['subnet_id'], config['admin_key']))
            sudo('chown -R jenkins:jenkins /var/lib/jenkins')
            sudo('/etc/init.d/jenkins stop; sleep 5')
            sudo('sysv-rc-conf jenkins on')
            sudo('service jenkins start')
            sudo('touch ' + dlab_path + '/tmp/jenkins_configured')
            sudo('echo "jenkins ALL = NOPASSWD:ALL" >> /etc/sudoers')
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure Jenkins: ', str(err))
        sys.exit(1)


def configure_nginx(config, dlab_path, hostname):
    try:
        random_file_part = id_generator(size=20)
        if not exists("/etc/nginx/conf.d/nginx_proxy.conf"):
            sudo('rm -f /etc/nginx/conf.d/*')
            put(config['nginx_template_dir'] + 'nginx_proxy.conf', '/tmp/nginx_proxy.conf')
            sudo("sed -i 's|SSN_HOSTNAME|" + hostname + "|' /tmp/nginx_proxy.conf")
            sudo('mv /tmp/nginx_proxy.conf ' + dlab_path + 'tmp/')
            sudo('\cp ' + dlab_path + 'tmp/nginx_proxy.conf /etc/nginx/conf.d/')
            sudo('mkdir -p /etc/nginx/locations')
            sudo('rm -f /etc/nginx/sites-enabled/default')
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure Nginx: ', str(err))
        sys.exit(1)

    try:
        if not exists("/etc/nginx/locations/proxy_location_jenkins.conf"):
            nginx_password = id_generator()
            template_file = config['nginx_template_dir'] + 'proxy_location_jenkins_template.conf'
            with open("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line)
            put("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, '/tmp/proxy_location_jenkins.conf')
            sudo('mv /tmp/proxy_location_jenkins.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            sudo('\cp ' + os.environ['ssn_dlab_path'] + 'tmp/proxy_location_jenkins.conf /etc/nginx/locations/')
            sudo("echo 'engineer:" + crypt.crypt(nginx_password, id_generator()) + "' > /etc/nginx/htpasswd")
            with open('jenkins_creds.txt', 'w+') as f:
                f.write("Jenkins credentials: engineer  / " + nginx_password)
    except:
        return False

    try:
        sudo('service nginx reload')
        return True
    except:
        return False


def ensure_supervisor():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/superv_ensured'):
            sudo('apt-get -y install supervisor')
            sudo('update-rc.d supervisor defaults')
            sudo('update-rc.d supervisor enable')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/superv_ensured')
    except Exception as err:
        traceback.print_exc()
        print('Failed to install Supervisor: ', str(err))
        sys.exit(1)


def ensure_mongo():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/mongo_ensured'):
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927')
            sudo('ver=`lsb_release -cs`; echo "deb http://repo.mongodb.org/apt/ubuntu $ver/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list; apt-get update')
            sudo('apt-get -y --allow-unauthenticated install mongodb-org')
            sudo('systemctl enable mongod.service')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/mongo_ensured')
    except Exception as err:
        traceback.print_exc()
        print('Failed to install MongoDB: ', str(err))
        sys.exit(1)


def start_ss(keyfile, host_string, dlab_conf_dir, web_path,
             os_user, mongo_passwd, keystore_passwd, cloud_provider,
             service_base_name, tag_resource_id, billing_tag, account_id, billing_bucket,
             aws_job_enabled, dlab_path, billing_enabled, cloud_params,
             authentication_file, offer_number, currency,
             locale, region_info, ldap_login, tenant_id,
             application_id, hostname, data_lake_name, subscription_id,
             validate_permission_scope, dlab_id, usage_date, product,
             usage_type, usage, cost, resource_id, tags, report_path=''):
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/ss_started'):
            java_path = sudo("update-alternatives --query java | grep 'Value: ' | grep -o '/.*/jre'")
            supervisor_conf = '/etc/supervisor/conf.d/supervisor_svc.conf'
            local('sed -i "s|MONGO_PASSWORD|{}|g" /root/templates/ssn.yml'.format(mongo_passwd))
            local('sed -i "s|KEYSTORE_PASSWORD|{}|g" /root/templates/ssn.yml'.format(keystore_passwd))
            local('sed -i "s|CLOUD_PROVIDER|{}|g" /root/templates/ssn.yml'.format(cloud_provider))
            local('sed -i "s|\${JRE_HOME}|' + java_path + '|g" /root/templates/ssn.yml')
            sudo('sed -i "s|KEYNAME|{}|g" {}/webapp/provisioning-service/conf/provisioning.yml'.
                 format(os.environ['conf_key_name'], dlab_path))
            sudo('sed -i "s|KEYCLOAK_REALM_NAME|{}|g" {}/webapp/provisioning-service/conf/provisioning.yml'.
                 format(os.environ['keycloak_realm_name'], dlab_path))
            sudo('sed -i "s|KEYCLOAK_AUTH_SERVER_URL|{}|g" {}/webapp/provisioning-service/conf/provisioning.yml'.
                 format(os.environ['keycloak_auth_server_url'], dlab_path))
            sudo('sed -i "s|KEYCLOAK_CLIENT_NAME|{}|g" {}/webapp/provisioning-service/conf/provisioning.yml'.
                 format(os.environ['keycloak_client_name'], dlab_path))
            sudo('sed -i "s|KEYCLOAK_CLIENT_SECRET|{}|g" {}/webapp/provisioning-service/conf/provisioning.yml'.
                 format(os.environ['keycloak_client_secret'], dlab_path))
            put('/root/templates/ssn.yml', '/tmp/ssn.yml')
            sudo('mv /tmp/ssn.yml ' + os.environ['ssn_dlab_path'] + 'conf/')
            put('/root/templates/proxy_location_webapp_template.conf', '/tmp/proxy_location_webapp_template.conf')
            sudo('mv /tmp/proxy_location_webapp_template.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            with open('/root/templates/supervisor_svc.conf', 'r') as f:
                text = f.read()
            text = text.replace('WEB_CONF', dlab_conf_dir).replace('OS_USR', os_user)
            with open('/root/templates/supervisor_svc.conf', 'w') as f:
                f.write(text)
            put('/root/templates/supervisor_svc.conf', '/tmp/supervisor_svc.conf')
            sudo('mv /tmp/supervisor_svc.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            sudo('cp ' + os.environ['ssn_dlab_path'] +
                 'tmp/proxy_location_webapp_template.conf /etc/nginx/locations/proxy_location_webapp.conf')
            sudo('cp ' + os.environ['ssn_dlab_path'] + 'tmp/supervisor_svc.conf {}'.format(supervisor_conf))
            sudo('sed -i \'s=WEB_APP_DIR={}=\' {}'.format(web_path, supervisor_conf))
            try:
                sudo('mkdir -p /var/log/application')
                run('mkdir -p /tmp/yml_tmp/')
                for service in ['self-service', 'provisioning-service', 'billing']:
                    jar = sudo('cd {0}{1}/lib/; find {1}*.jar -type f'.format(web_path, service))
                    sudo('ln -s {0}{2}/lib/{1} {0}{2}/{2}.jar '.format(web_path, jar, service))
                    sudo('cp {0}/webapp/{1}/conf/*.yml /tmp/yml_tmp/'.format(dlab_path, service))
                # Replacing Keycloak and cloud parameters
                for item in json.loads(cloud_params):
                    sudo('sed -i "s|{0}|{1}|g" /tmp/yml_tmp/self-service.yml'.format(
                        item['key'], item['value']))

                if cloud_provider == 'azure':
                    sudo('sed -i "s|<LOGIN_USE_LDAP>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(ldap_login))
                    sudo('sed -i "s|<LOGIN_TENANT_ID>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(tenant_id))
                    sudo('sed -i "s|<LOGIN_APPLICATION_ID>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(application_id))
                    sudo('sed -i "s|<DLAB_SUBSCRIPTION_ID>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(subscription_id))
                    sudo('sed -i "s|<MANAGEMENT_API_AUTH_FILE>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(
                        authentication_file))
                    sudo('sed -i "s|<VALIDATE_PERMISSION_SCOPE>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(
                        validate_permission_scope))
                    sudo('sed -i "s|<LOGIN_APPLICATION_REDIRECT_URL>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(
                        hostname))
                    sudo('sed -i "s|<LOGIN_PAGE>|{0}|g" /tmp/yml_tmp/self-service.yml'.format(hostname))
                    # if os.environ['azure_datalake_enable'] == 'true':
                    #     permission_scope = 'subscriptions/{}/resourceGroups/{}/providers/Microsoft.DataLakeStore/accounts/{}/providers/Microsoft.Authorization/'.format(
                    #         subscription_id, service_base_name, data_lake_name)
                    # else:
                    #     permission_scope = 'subscriptions/{}/resourceGroups/{}/providers/Microsoft.Authorization/'.format(
                    #         subscription_id, service_base_name
                    #     )
                sudo('mv /tmp/yml_tmp/* ' + dlab_conf_dir)
                sudo('rmdir /tmp/yml_tmp/')
            except:
                append_result("Unable to upload webapp jars")
                sys.exit(1)
            if billing_enabled:
                local('scp -i {} /root/scripts/configure_billing.py {}:/tmp/configure_billing.py'.format(keyfile, host_string))
                params = '--cloud_provider {} ' \
                         '--infrastructure_tag {} ' \
                         '--tag_resource_id {} ' \
                         '--billing_tag {} ' \
                         '--account_id {} ' \
                         '--billing_bucket {} ' \
                         '--aws_job_enabled {} ' \
                         '--report_path "{}" ' \
                         '--mongo_password {} ' \
                         '--dlab_dir {} ' \
                         '--authentication_file "{}" ' \
                         '--offer_number {} ' \
                         '--currency {} ' \
                         '--locale {} ' \
                         '--region_info {} ' \
                         '--dlab_id {} ' \
                         '--usage_date {} ' \
                         '--product {} ' \
                         '--usage_type {} ' \
                         '--usage {} ' \
                         '--cost {} ' \
                         '--resource_id {} ' \
                         '--tags {}'.\
                            format(cloud_provider,
                                   service_base_name,
                                   tag_resource_id,
                                   billing_tag,
                                   account_id,
                                   billing_bucket,
                                   aws_job_enabled,
                                   report_path,
                                   mongo_passwd,
                                   dlab_path,
                                   authentication_file,
                                   offer_number,
                                   currency,
                                   locale,
                                   region_info,
                                   dlab_id,
                                   usage_date,
                                   product,
                                   usage_type,
                                   usage,
                                   cost,
                                   resource_id,
                                   tags)
                sudo('python /tmp/configure_billing.py {}'.format(params))
            try:
                sudo('keytool -genkeypair -alias dlab -keyalg RSA -validity 730 -storepass {1} -keypass {1} \
                     -keystore /home/{0}/keys/dlab.keystore.jks -keysize 2048 -dname "CN=localhost"'.format(os_user, keystore_passwd))
                sudo('keytool -exportcert -alias dlab -storepass {1} -file /home/{0}/keys/dlab.crt \
                     -keystore /home/{0}/keys/dlab.keystore.jks'.format(os_user, keystore_passwd))
                sudo('keytool -importcert -trustcacerts -alias dlab -file /home/{0}/keys/dlab.crt -noprompt \
                     -storepass changeit -keystore {1}/lib/security/cacerts'.format(os_user, java_path))
            except:
                append_result("Unable to generate cert and copy to java keystore")
                sys.exit(1)
            sudo('service supervisor start')
            sudo('service nginx restart')
            sudo('service supervisor restart')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/ss_started')
    except Exception as err:
        traceback.print_exc()
        print('Failed to start Self-service: ', str(err))
        sys.exit(1)


def install_build_dep():
    try:
        if not exists('{}tmp/build_dep_ensured'.format(os.environ['ssn_dlab_path'])):
            maven_version = '3.5.4'
            sudo('apt-get install -y openjdk-8-jdk git wget unzip')
            with cd('/opt/'):
                sudo('wget http://mirrors.sonic.net/apache/maven/maven-{0}/{1}/binaries/apache-maven-{1}-bin.zip'.format(
                    maven_version.split('.')[0], maven_version))
                sudo('unzip apache-maven-{}-bin.zip'.format(maven_version))
                sudo('mv apache-maven-{} maven'.format(maven_version))
            sudo('bash -c "curl --silent --location https://deb.nodesource.com/setup_8.x | bash -"')
            sudo('apt-get install -y nodejs')
            sudo('npm config set unsafe-perm=true')
            sudo('touch {}tmp/build_dep_ensured'.format(os.environ['ssn_dlab_path']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to install build dependencies for UI: ', str(err))
        sys.exit(1)
