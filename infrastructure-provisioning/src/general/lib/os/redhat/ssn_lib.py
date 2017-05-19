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

import crypt
import yaml
from dlab.fab import *
from dlab.meta_lib import *
import os


def ensure_docker_daemon(dlab_path, os_user):
    try:
        if not exists('{}tmp/docker_daemon_ensured'.format(dlab_path)):
            sudo('yum update-minimal --security -y')
            sudo('curl -fsSL https://get.docker.com/ | sh')
            sudo('usermod -aG docker {}'.format(os_user))
            sudo('systemctl enable docker.service')
            sudo('systemctl start docker')
            sudo('touch {}tmp/docker_daemon_ensured'.format(dlab_path))
        return True
    except:
        return False


def ensure_nginx(dlab_path):
    try:
        if not exists('{}tmp/nginx_ensured'.format(dlab_path)):
            sudo('yum -y install nginx')
            sudo('systemctl restart nginx.service')
            sudo('chkconfig nginx on')
            sudo('touch {}tmp/nginx_ensured'.format(dlab_path))
        return True
    except:
        return False


def ensure_jenkins(dlab_path):
    try:
        if not exists('{}tmp/jenkins_ensured'.format(dlab_path)):
            sudo('wget -O /etc/yum.repos.d/jenkins.repo http://pkg.jenkins-ci.org/redhat-stable/jenkins.repo')
            sudo('rpm --import https://jenkins-ci.org/redhat/jenkins-ci.org.key')
            sudo('yum -y install java')
            sudo('yum -y install jenkins')
            sudo('touch {}tmp/jenkins_ensured'.format(dlab_path))
        return True
    except:
        return False


def configure_jenkins(dlab_path, os_user, config, tag_resource_id):
    try:
        if not exists('{}tmp/jenkins_configured'.format(dlab_path)):
            sudo('rm -rf /var/lib/jenkins/*')
            sudo('mkdir -p /var/lib/jenkins/jobs/')
            sudo('chown -R {0}:{0} /var/lib/jenkins/'.format(os_user))
            put('/root/templates/jenkins_jobs/*', '/var/lib/jenkins/jobs/')
            #sudo("find /var/lib/jenkins/jobs/ -type f | xargs sed -i \'s/OS_USR/{}/g\'".format(os_user))
            sudo("find /var/lib/jenkins/jobs/ -type f | xargs sed -i \'s/OS_USR/{}/g; s/SBN/{}/g; s/CTUN/{}/g; s/SGI/{}/g; s/VPC/{}/g; s/SNI/{}/g; s/AKEY/{}/g\'".format(os_user, config['service_base_name'], tag_resource_id, config['security_group_id'], config['vpc_id'], config['subnet_id'], config['admin_key']))
            sudo('chown -R jenkins:jenkins /var/lib/jenkins')
            sudo('/etc/init.d/jenkins stop; sleep 5')
            sudo('sed -i \'/JENKINS_PORT/ s/^/#/\' /etc/sysconfig/jenkins; echo \'JENKINS_PORT="8070"\' >> /etc/sysconfig/jenkins')
            sudo('sed -i \'/JENKINS_ARGS/ s|=""|="--prefix=/jenkins"|\' /etc/sysconfig/jenkins')
            sudo('semanage port -a -t http_port_t -p tcp 8070')
            sudo('setsebool -P httpd_can_network_connect 1')
            sudo('chkconfig jenkins on')
            sudo('systemctl start jenkins.service')
            sudo('echo "jenkins ALL = NOPASSWD:ALL" >> /etc/sudoers')
            sudo('touch {}tmp/jenkins_configured'.format(dlab_path))
        return True
    except:
        return False


def configure_nginx(config, dlab_path, hostname):
    try:
        random_file_part = id_generator(size=20)
        if not exists("/etc/nginx/conf.d/nginx_proxy.conf"):
            sudo('rm -f /etc/nginx/conf.d/*')
            put(config['nginx_template_dir'] + 'nginx_proxy.conf', '/tmp/nginx_proxy.conf')
            put(config['nginx_template_dir'] + 'ssn_nginx.conf', '/tmp/nginx.conf')
            sudo("sed -i 's|SSN_HOSTNAME|" + hostname + "|' /tmp/nginx_proxy.conf")
            sudo('cat /tmp/nginx.conf > /etc/nginx/nginx.conf')
            sudo('mv /tmp/nginx_proxy.conf ' + dlab_path + 'tmp/')
            sudo('\cp ' + dlab_path + 'tmp/nginx_proxy.conf /etc/nginx/conf.d/')
            sudo('mkdir -p /etc/nginx/locations')
            sudo('rm -f /etc/nginx/sites-enabled/default')
    except:
        return False

    try:
        if not exists("/etc/nginx/locations/proxy_location_jenkins.conf"):
            nginx_password = id_generator()
            template_file = config['nginx_template_dir'] + 'proxy_location_jenkins_template.conf'
            with open("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line)
            put("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, '/tmp/proxy_location_jenkins.conf')
            sudo('\cp /tmp/proxy_location_jenkins.conf /etc/nginx/locations/')
            sudo("echo 'engineer:" + crypt.crypt(nginx_password, id_generator()) + "' > /etc/nginx/htpasswd")
            with open('jenkins_crids.txt', 'w+') as f:
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
        if not exists('{}tmp/superv_ensured'.format(os.environ['ssn_dlab_path'])):
            sudo('yum install -y supervisor')
            #sudo('pip install supervisor')
            sudo('chkconfig supervisord on')
            sudo('systemctl start supervisord')
            sudo('touch {}tmp/superv_ensured'.format(os.environ['ssn_dlab_path']))
        return True
    except:
        return False


def ensure_mongo():
    try:
        if not exists('{}tmp/mongo_ensured'.format(os.environ['ssn_dlab_path'])):
            sudo('echo -e "[MongoDB]\nname=MongoDB Repository\nbaseurl=http://repo.mongodb.org/yum/redhat/7/mongodb-org/3.2/x86_64/\ngpgcheck=0\nenabled=1\n" > /etc/yum.repos.d/mongodb.repo')
            sudo('yum install -y mongodb-org')
            sudo('semanage port -a -t mongod_port_t -p tcp 27017')
            sudo('chkconfig mongod on')
            sudo('systemctl start mongod.service')
            sudo('touch {}tmp/mongo_ensured'.format(os.environ['ssn_dlab_path']))
        return True
    except:
        return False


def start_ss(keyfile, host_string, dlab_conf_dir, web_path, os_user, mongo_passwd, keystore_passwd, cloud_provider,
             service_base_name, tag_resource_id, account_id, billing_bucket, dlab_path, billing_enabled, report_path=''):
    try:
        if not exists('{}tmp/ss_started'.format(os.environ['ssn_dlab_path'])):
            supervisor_conf = '/etc/supervisord.d/supervisor_svc.ini'
            local('sed -i "s|MONGO_PASSWORD|{}|g" /root/templates/ssn.yml'.format(mongo_passwd))
            put('/root/templates/ssn.yml', '/tmp/ssn.yml')
            sudo('mv /tmp/ssn.yml ' + os.environ['ssn_dlab_path'] + 'conf/')
            put('/root/templates/proxy_location_webapp_template.conf', '/tmp/proxy_location_webapp_template.conf')
            sudo('mv /tmp/proxy_location_webapp_template.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            with open('/root/templates/supervisor_svc.conf', 'r') as f:
                text = f.read()
            text = text.replace('WEB_CONF', dlab_conf_dir).replace('OS_USR', os_user)
            with open('/root/templates/supervisor_svc.ini', 'w') as f:
                f.write(text)
            put('/root/templates/supervisor_svc.ini', '/tmp/supervisor_svc.ini')
            sudo('mv /tmp/supervisor_svc.ini ' + os.environ['ssn_dlab_path'] + 'tmp/')
            sudo('cp ' + os.environ['ssn_dlab_path'] + 'tmp/proxy_location_webapp_template.conf /etc/nginx/locations/proxy_location_webapp.conf')
            sudo('cp ' + os.environ['ssn_dlab_path'] + 'tmp/supervisor_svc.ini {}'.format(supervisor_conf))

            sudo('sed -i \'s=WEB_APP_DIR={}=\' {}'.format(web_path, supervisor_conf))

            sudo('mkdir -p /var/log/application')
            sudo('mkdir -p ' + web_path)
            sudo('mkdir -p ' + web_path + 'provisioning-service/')
            sudo('mkdir -p ' + web_path + 'billing/')
            sudo('mkdir -p ' + web_path + 'security-service/')
            sudo('mkdir -p ' + web_path + 'self-service/')
            sudo('chown -R {0}:{0} {1}'.format(os_user, web_path))
            try:
                local('scp -r -i {} /root/web_app/self-service/*.jar {}:'.format(keyfile, host_string) + web_path + 'self-service/')
                local('scp -r -i {} /root/web_app/security-service/*.jar {}:'.format(keyfile, host_string) + web_path + 'security-service/')
                local('scp -r -i {} /root/web_app/provisioning-service/*.jar {}:'.format(keyfile, host_string) + web_path + 'provisioning-service/')
                local('scp -r -i {} /root/web_app/billing/*.jar {}:'.format(keyfile, host_string) + web_path + 'billing/')
                run('mkdir -p /tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/self-service/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/security-service/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/provisioning-service/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/billing/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                sudo('mv /tmp/yml_tmp/* ' + os.environ['ssn_dlab_path'] + 'conf/')
                sudo('rmdir /tmp/yml_tmp/')
            except Exception as err:
                traceback.print_exc()
                append_result("Unable to upload webapp jars. ", str(err))
                sys.exit(1)

            if billing_enabled:
                local('scp -i {} /root/scripts/configure_billing.py {}:/tmp/configure_billing.py'.format(keyfile,
                                                                                                         host_string))
                sudo('python /tmp/configure_billing.py --cloud_provider {} --infrastructure_tag {} --tag_resource_id {} --account_id {} --billing_bucket {} --report_path "{}" --mongo_password {} --dlab_dir {}'.
                     format(cloud_provider, service_base_name, tag_resource_id, account_id, billing_bucket, report_path,
                            mongo_passwd, dlab_path))
            try:
                java_path = sudo("alternatives --list | grep jre_openjdk | awk '{print $3}'")
                sudo('keytool -genkeypair -alias dlab -keyalg RSA -storepass {1} -keypass PASSWORD \
                     -keystore /home/{0}/keys/dlab.keystore.jks -keysize 2048 -dname "CN=localhost"'.format(os_user, keystore_passwd))
                sudo('keytool -exportcert -alias dlab -storepass {1} -file /home/{0}/keys/dlab.crt \
                     -keystore /home/{0}/keys/dlab.keystore.jks'.format(os_user, keystore_passwd))
                sudo('keytool -importcert -trustcacerts -alias dlab -file /home/{0}/keys/dlab.crt -noprompt \
                     -storepass changeit -keystore {1}/lib/security/cacerts'.format(os_user, java_path))
            except:
                append_result("Unable to generate cert and copy to java keystore")
                sys.exit(1)
            sudo('systemctl restart supervisord')
            sudo('service nginx restart')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/ss_started')
        return True
    except:
        return False
