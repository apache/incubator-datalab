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
import traceback
from datalab.common_lib import *
from datalab.fab import *
from datalab.ssn_lib import *
from fabric import *

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--datalab_path', type=str, default='')
parser.add_argument('--tag_resource_id', type=str, default='')
parser.add_argument('--step_cert_sans', type=str, default='')
args = parser.parse_args()


def set_hostname(subdomain, hosted_zone_name):
    try:
        conn.sudo('hostnamectl set-hostname {0}.{1}'.format(subdomain, hosted_zone_name))
    except Exception as err:
        traceback.print_exc()
        print('Failed to set hostname: ', str(err))
        sys.exit(1)

def set_resolve():
    try:
        conn.sudo('ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf')
    except Exception as err:
        traceback.print_exc()
        print('Failed to set resolve: ', str(err))
        sys.exit(1)

def cp_key(keyfile, host_string, os_user):
    try:
        key_name=keyfile.split("/")
        conn.sudo('mkdir -p /home/' + os_user + '/keys')
        conn.sudo('chown -R ' + os_user + ':' + os_user + ' /home/' + os_user + '/keys')
        conn.local('rsync -r -q -e "ssh -i {0}" {0} {1}:/home/{3}/keys/{2}'.format(keyfile, host_string, key_name[-1], os_user))
        conn.sudo('chmod 600 /home/' + os_user + '/keys/*.pem')
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy key: ', str(err))
        sys.exit(1)


def cp_backup_scripts(datalab_path):
    try:
        conn.put('/root/scripts/backup.py', datalab_path + "tmp/backup.py")
        conn.put('/root/scripts/restore.py', datalab_path + "tmp/restore.py")
        conn.run('chmod +x {0}tmp/backup.py {0}tmp/restore.py'.format(datalab_path))
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy backup scripts: ', str(err))
        sys.exit(1)


def cp_gitlab_scripts(datalab_path):
    try:
        if not exists(conn,'{}tmp/gitlab'.format(datalab_path)):
            conn.run('mkdir -p {}tmp/gitlab'.format(datalab_path))
        conn.put('/root/scripts/gitlab_deploy.py', '{}tmp/gitlab/gitlab_deploy.py'.format(datalab_path))
        conn.put('/root/scripts/configure_gitlab.py', '{}tmp/gitlab/configure_gitlab.py'.format(datalab_path))
        conn.run('cd {}tmp/gitlab && chmod +x gitlab_deploy.py configure_gitlab.py'.format(datalab_path))
        conn.put('/root/templates/gitlab.rb', '{}tmp/gitlab/gitlab.rb'.format(datalab_path))
        conn.put('/root/templates/gitlab.ini', '{}tmp/gitlab/gitlab.ini'.format(datalab_path))
        conn.run('cd {}tmp/gitlab && sed -i "s/CONF_OS_USER/{}/g" gitlab.ini'.format(datalab_path, os.environ['conf_os_user']))
        conn.run('cd {}tmp/gitlab && sed -i "s/CONF_OS_FAMILY/{}/g" gitlab.ini'.format(datalab_path, os.environ['conf_os_family']))
        conn.run('cd {}tmp/gitlab && sed -i "s/CONF_KEY_NAME/{}/g" gitlab.ini'.format(datalab_path, os.environ['conf_key_name']))
        conn.run('cd {}tmp/gitlab && sed -i "s,CONF_DATALAB_PATH,{},g" gitlab.ini'.format(datalab_path, datalab_path))
        conn.run('cd {}tmp/gitlab && sed -i "s/SERVICE_BASE_NAME/{}/g" gitlab.ini'.format(datalab_path, os.environ['conf_service_base_name']))
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy gitlab scripts: ', str(err))
        sys.exit(1)


def creating_service_directories(datalab_path, os_user):
    try:
        if not exists(conn,datalab_path):
            conn.sudo('mkdir -p ' + datalab_path)
            conn.sudo('mkdir -p ' + datalab_path + 'conf')
            conn.sudo('mkdir -p ' + datalab_path + 'webapp/static')
            conn.sudo('mkdir -p ' + datalab_path + 'template')
            conn.sudo('mkdir -p ' + datalab_path + 'tmp')
            conn.sudo('mkdir -p ' + datalab_path + 'tmp/result')
            conn.sudo('mkdir -p ' + datalab_path + 'sources')
            conn.sudo('mkdir -p /var/opt/datalab/log/ssn')
            conn.sudo('mkdir -p /var/opt/datalab/log/edge')
            conn.sudo('mkdir -p /var/opt/datalab/log/notebook')
            conn.sudo('mkdir -p /var/opt/datalab/log/dataengine-service')
            conn.sudo('mkdir -p /var/opt/datalab/log/dataengine')
            conn.sudo('ln -s ' + datalab_path + 'conf /etc/opt/datalab')
            conn.sudo('ln -s /var/opt/datalab/log /var/log/datalab')
            conn.sudo('chown -R ' + os_user + ':' + os_user + ' /var/opt/datalab/log')
            conn.sudo('chown -R ' + os_user + ':' + os_user + ' ' + datalab_path)
    except Exception as err:
        traceback.print_exc()
        print('Failed to create service directories: ', str(err))
        sys.exit(1)


def configure_ssl_certs(hostname, custom_ssl_cert):
    try:
        if custom_ssl_cert:
            conn.put('/root/certs/datalab.crt', 'datalab.crt')
            conn.put('/root/certs/datalab.key', 'datalab.key')
            conn.sudo('mv datalab.crt /etc/ssl/certs/datalab.crt')
            conn.sudo('mv datalab.key /etc/ssl/certs/datalab.key')
        else:
            if os.environ['conf_stepcerts_enabled'] == 'true':
                ensure_step(args.os_user)
                conn.sudo('mkdir -p /home/{0}/keys'.format(args.os_user))
                conn.sudo('''bash -c 'echo "{0}" | base64 --decode > /etc/ssl/certs/root_ca.crt' '''.format(
                     os.environ['conf_stepcerts_root_ca']))
                fingerprint = conn.sudo('step certificate fingerprint /etc/ssl/certs/root_ca.crt').stdout.replace('\n', '')
                conn.sudo('step ca bootstrap --fingerprint {0} --ca-url "{1}"'.format(fingerprint,
                                                                                 os.environ['conf_stepcerts_ca_url']))
                conn.sudo('''bash -c 'echo "{0}" > /home/{1}/keys/provisioner_password' '''.format(
                     os.environ['conf_stepcerts_kid_password'], args.os_user))
                sans = "--san localhost --san 127.0.0.1 {0}".format(args.step_cert_sans)
                cn = hostname
                conn.sudo('step ca token {3} --kid {0} --ca-url "{1}" --root /etc/ssl/certs/root_ca.crt '
                     '--password-file /home/{2}/keys/provisioner_password {4} --output-file /tmp/step_token'.format(
                              os.environ['conf_stepcerts_kid'], os.environ['conf_stepcerts_ca_url'],
                              args.os_user, cn, sans))
                token = conn.sudo('cat /tmp/step_token').stdout
                conn.sudo('step ca certificate "{0}" /etc/ssl/certs/datalab.crt /etc/ssl/certs/datalab.key '
                     '--token "{1}" --kty=RSA --size 2048 --provisioner {2} '.format(cn, token,
                                                                                     os.environ['conf_stepcerts_kid']))
                conn.sudo('touch /var/log/renew_certificates.log')
                conn.put('/root/templates/renew_certificates.sh', '/tmp/renew_certificates.sh')
                conn.sudo('mv /tmp/renew_certificates.sh /usr/local/bin/')
                conn.sudo('chmod +x /usr/local/bin/renew_certificates.sh')
                conn.sudo('sed -i "s/OS_USER/{0}/g" /usr/local/bin/renew_certificates.sh'.format(args.os_user))
                conn.sudo('sed -i "s|JAVA_HOME|{0}|g" /usr/local/bin/renew_certificates.sh'.format(find_java_path_remote()))
                conn.sudo('sed -i "s|RESOURCE_TYPE|ssn|g" /usr/local/bin/renew_certificates.sh')
                conn.sudo('sed -i "s|CONF_FILE|ssn|g" /usr/local/bin/renew_certificates.sh')
                conn.put('/root/templates/manage_step_certs.sh', '/tmp/manage_step_certs.sh')
                conn.sudo('cp /tmp/manage_step_certs.sh /usr/local/bin/manage_step_certs.sh')
                conn.sudo('chmod +x /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_ROOT_CERT_PATH|/etc/ssl/certs/root_ca.crt|g" '
                     '/usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_CERT_PATH|/etc/ssl/certs/datalab.crt|g" /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_KEY_PATH|/etc/ssl/certs/datalab.key|g" /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|STEP_CA_URL|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_ca_url']))
                conn.sudo('sed -i "s|RESOURCE_TYPE|ssn|g" /usr/local/bin/manage_step_certs.sh')
                conn.sudo('sed -i "s|SANS|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(sans))
                conn.sudo('sed -i "s|CN|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(cn))
                conn.sudo('sed -i "s|KID|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(
                    os.environ['conf_stepcerts_kid']))
                conn.sudo('sed -i "s|STEP_PROVISIONER_PASSWORD_PATH|/home/{0}/keys/provisioner_password|g" '
                     '/usr/local/bin/manage_step_certs.sh'.format(args.os_user))
                conn.sudo('bash -c \'echo "0 * * * * root /usr/local/bin/manage_step_certs.sh >> '
                     '/var/log/renew_certificates.log 2>&1" >> /etc/crontab \'')
                conn.put('/root/templates/step-cert-manager.service', '/tmp/step-cert-manager.service')
                conn.sudo('cp /tmp/step-cert-manager.service /etc/systemd/system/step-cert-manager.service')
                conn.sudo('systemctl daemon-reload')
                conn.sudo('systemctl enable step-cert-manager.service')
            else:
                conn.sudo('openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /etc/ssl/certs/datalab.key \
                     -out /etc/ssl/certs/datalab.crt -subj "/C=US/ST=US/L=US/O=datalab/CN={}"'.format(hostname))
        conn.sudo('openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048')
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure SSL certificates: ', str(err))
        sys.exit(1)

def docker_build_script():
    try:
        conn.put('/root/scripts/docker_build.py', 'docker_build')
        conn.sudo('chmod +x docker_build')
        conn.sudo('mv docker_build /usr/bin/docker-build')
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure docker_build script: ', str(err))
        sys.exit(1)


def copy_ssn_libraries():
    try:
        conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
        conn.run('mkdir -p /tmp/datalab_libs/')
        subprocess.run(
            'rsync -e "ssh -i {}" /usr/lib/python3.8/datalab/*.py {}:/tmp/datalab_libs/'.format(args.keyfile, host_string),
            shell=True, check=True)
        conn.run('chmod a+x /tmp/datalab_libs/*')
        conn.sudo('mv /tmp/datalab_libs/* /usr/lib/python3.8/datalab/')
        if exists(conn, '/usr/lib64'):
            conn.sudo('mkdir -p /usr/lib64/python3.8')
            conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')
    except Exception as err:
        traceback.print_exc()
        logging.error('Failed to copy ssn libraries: ', str(err))
        sys.exit(1)

##############
# Run script #
##############


if __name__ == "__main__":
    print("Configure connections")
    try:
        global conn
        conn = datalab.fab.init_datalab_connection(args.hostname, args.os_user, args.keyfile)
        host_string = args.os_user + '@' + args.hostname
        deeper_config = json.loads(args.additional_config)
    except:
        sys.exit(2)
    if 'ssn_hosted_zone_id' in os.environ and 'ssn_hosted_zone_name' in os.environ and 'ssn_subdomain' in os.environ:
        domain_created = True
    else:
        domain_created = False

    if os.path.exists('/root/certs/datalab.crt') and os.path.exists('/root/certs/datalab.key'):
        custom_ssl_cert = True
    else:
        custom_ssl_cert = False

    print('Setting resolve DNS configuration')
    set_resolve()

    print("Creating service directories.")
    creating_service_directories(args.datalab_path, args.os_user)

    if domain_created:
        print("Setting hostname")
        set_hostname(os.environ['ssn_subdomain'], os.environ['ssn_hosted_zone_name'])
        args.hostname = "{0}.{1}".format(os.environ['ssn_subdomain'], os.environ['ssn_hosted_zone_name'])

    print("Installing nginx as frontend.")
    ensure_nginx(args.datalab_path)

    print("Installing Java")
    ensure_java(args.os_user)

    print("Configuring ssl key and cert for nginx.")
    configure_ssl_certs(args.hostname, custom_ssl_cert)

    print("Configuring nginx.")
    configure_nginx(deeper_config, args.datalab_path, args.hostname)

    if os.environ['conf_letsencrypt_enabled'] == 'true':
        print("Configuring letsencrypt certificates.")
        install_certbot(args.os_user)
        if 'conf_letsencrypt_email' in os.environ:
            run_certbot(os.environ['conf_letsencrypt_domain_name'], 'ssn', os.environ['conf_letsencrypt_email'])
        else:
            run_certbot(os.environ['conf_letsencrypt_domain_name'], 'ssn')
        configure_nginx_LE(os.environ['conf_letsencrypt_domain_name'], 'ssn')

    # print("Installing jenkins.")
    # ensure_jenkins(args.datalab_path)

    # print("Configuring jenkins.")
    #configure_jenkins(args.datalab_path, args.os_user, deeper_config, args.tag_resource_id)

    print("Copying key")
    cp_key(args.keyfile, host_string, args.os_user)

    print("Copying backup scripts")
    cp_backup_scripts(args.datalab_path)

    print("Copying gitlab scripts & files")
    cp_gitlab_scripts(args.datalab_path)

    print("Ensuring safest ssh ciphers")
    ensure_ciphers()

    print("Configuring docker_build script")
    docker_build_script()

    logging.info("Copying DataLab libraries to SSN")
    copy_ssn_libraries()

    conn.close()