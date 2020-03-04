from fabric import Connection
from patchwork.files import exists
import logging
import argparse
import sys
import traceback
import time
import string
import random

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
    log_root_dir = "/var/opt/dlab/log"
    supervisor_log_file = "/var/log/application/provision-service.log"
    try:
        if not exists(conn, '/home/' + args.os_user + '/.ensure_dir/logs_ensured'):
            if not exists(conn, args.dlab_path):
                conn.sudo("mkdir -p " + args.dlab_path)
                conn.sudo("chown -R " + args.os_user + ' ' + args.dlab_path)
            if not exists(conn, log_root_dir):
                conn.sudo('mkdir -p ' + log_root_dir + '/provisioning')
                conn.sudo('touch ' + log_root_dir + '/provisioning/provisioning.log')
            if not exists(conn, supervisor_log_file):
                conn.sudo("mkdir -p /var/log/application")
                conn.sudo("touch " + supervisor_log_file)
            conn.sudo("chown -R {0} {1}".format(args.os_user, log_root_dir))
            conn.sudo('touch /home/' + args.os_user + '/.ensure_dir/logs_ensured')
    except Exception as err:
        print('Failed to configure logs and dlab directory: ', str(err))
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


def ensure_step_certs():
    try:
        if not exists(conn, '/home/{}/.ensure_dir/step_ensured'.format(args.os_user)):
            conn.sudo('wget https://github.com/smallstep/cli/releases/download/v0.13.3/step-cli_0.13.3_amd64.deb '
                      '-O /tmp/step-cli_0.13.3_amd64.deb')
            conn.sudo('dpkg -i /tmp/step-cli_0.13.3_amd64.deb')
            conn.sudo('''bash -c 'echo "{0}" | base64 --decode > /etc/ssl/certs/root_ca.crt' '''.format(args.step_root_ca))
            fingerprint = conn.sudo('step certificate fingerprint /etc/ssl/certs/root_ca.crt').stdout.replace('\n', '')
            conn.sudo('step ca bootstrap --fingerprint {0} --ca-url "{1}"'.format(fingerprint,
                                                                                  args.step_ca_url))
            conn.sudo('echo "{0}" > /home/{1}/keys/provisioner_password'.format(args.step_kid_password, args.os_user))
            if args.cloud_provider == 'aws':
                local_ip_address = conn.sudo('curl -s '
                                             'http://169.254.169.254/latest/meta-data/local-ipv4').stdout.replace('\n', '')
                try:
                    public_ip_address = conn.sudo('curl -s http://169.254.169.254/latest/meta-data/'
                                                  'public-ipv4').stdout.replace('\n', '')
                except:
                    public_ip_address = None
            elif args.cloud_provider == 'gcp':
                local_ip_address = conn.sudo('curl -H "Metadata-Flavor: Google" '
                                             'http://metadata/computeMetadata/v1/instance/network-interfaces/0/'
                                             'access-configs/0/external-ip').stdout.replace('\n', '')
                try:
                    public_ip_address = conn.sudo('curl -H "Metadata-Flavor: Google" '
                                                  'http://metadata/computeMetadata/v1/instance/network-interfaces/0/'
                                                  'ip').stdout.replace('\n', '')
                except:
                    public_ip_address = None
            elif args.cloud_provider == 'azure':
                local_ip_address = conn.sudo('curl -s -H Metadata:true "http://169.254.169.254/metadata/'
                                             'instance?api-version=2017-08-01&format=json" | jq -r ".network.'
                                             'interface[].ipv4.ipAddress[].privateIpAddress"').stdout.replace('\n', '')
                try:
                    public_ip_address = conn.sudo('curl -s -H Metadata:true "http://169.254.169.254/metadata/'
                                                  'instance?api-version=2017-08-01&format=json" | jq -r ".network.'
                                                  'interface[].ipv4.ipAddress[].publicIpAddress"').stdout.replace('\n',
                                                                                                                  '')
                except:
                    public_ip_address = None
            else:
                local_ip_address = None
                public_ip_address = None
            sans = "--san localhost --san {0} --san 127.0.0.1 ".format(local_ip_address)
            cn = local_ip_address
            if public_ip_address:
                sans += "--san {0}".format(public_ip_address)
                cn = public_ip_address
            conn.sudo('step ca token {3} --kid {0} --ca-url "{1}" --root /etc/ssl/certs/root_ca.crt '
                      '--password-file /home/{2}/keys/provisioner_password {4} --output-file /tmp/step_token'.format(
                               args.step_kid, args.step_ca_url, args.os_user, cn, sans))
            token = conn.sudo('cat /tmp/step_token').stdout.replace('\n', '')
            conn.sudo('step ca certificate "{0}" /etc/ssl/certs/dlab.crt /etc/ssl/certs/dlab.key '
                      '--token "{1}" --kty=RSA --size 2048 --provisioner {2} '.format(cn, token, args.step_kid))
            conn.put('./renew_certificates.sh', '/tmp/renew_certificates.sh')
            conn.sudo('mv /tmp/renew_certificates.sh /usr/local/bin/')
            conn.sudo('chmod +x /usr/local/bin/renew_certificates.sh')
            conn.sudo('sed -i "s/OS_USER/{0}/g" /usr/local/bin/renew_certificates.sh'.format(args.os_user))
            conn.sudo('sed -i "s|JAVA_HOME|{0}|g" /usr/local/bin/renew_certificates.sh'.format(java_home))
            conn.sudo('sed -i "s|RESOURCE_TYPE|endpoint|g" /usr/local/bin/renew_certificates.sh')
            conn.sudo('sed -i "s|CONF_FILE|provisioning|g" /usr/local/bin/renew_certificates.sh')
            conn.sudo('touch /var/log/renew_certificates.log')
            conn.put('./manage_step_certs.sh', '/tmp/manage_step_certs.sh')
            conn.sudo('mv /tmp/manage_step_certs.sh /usr/local/bin/manage_step_certs.sh')
            conn.sudo('chmod +x /usr/local/bin/manage_step_certs.sh')
            conn.sudo('sed -i "s|STEP_ROOT_CERT_PATH|/etc/ssl/certs/root_ca.crt|g" '
                      '/usr/local/bin/manage_step_certs.sh')
            conn.sudo('sed -i "s|STEP_CERT_PATH|/etc/ssl/certs/dlab.crt|g" /usr/local/bin/manage_step_certs.sh')
            conn.sudo('sed -i "s|STEP_KEY_PATH|/etc/ssl/certs/dlab.key|g" /usr/local/bin/manage_step_certs.sh')
            conn.sudo('sed -i "s|STEP_CA_URL|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(args.step_ca_url))
            conn.sudo('sed -i "s|RESOURCE_TYPE|endpoint|g" /usr/local/bin/manage_step_certs.sh')
            conn.sudo('sed -i "s|SANS|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(sans))
            conn.sudo('sed -i "s|CN|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(cn))
            conn.sudo('sed -i "s|KID|{0}|g" /usr/local/bin/manage_step_certs.sh'.format(args.step_kid))
            conn.sudo('sed -i "s|STEP_PROVISIONER_PASSWORD_PATH|/home/{0}/keys/provisioner_password|g" '
                      '/usr/local/bin/manage_step_certs.sh'.format(args.os_user))
            conn.sudo('bash -c \'echo "0 * * * * root /usr/local/bin/manage_step_certs.sh >> '
                      '/var/log/renew_certificates.log 2>&1" >> /etc/crontab \'')
            conn.put('./step-cert-manager.service', '/tmp/step-cert-manager.service')
            conn.sudo('mv /tmp/step-cert-manager.service /etc/systemd/system/step-cert-manager.service')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable step-cert-manager.service')
            conn.sudo('touch /home/{}/.ensure_dir/step_ensured'
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
            if not exists(conn, '{}/tmp'.format(args.dlab_path)):
                conn.run('mkdir -p {}/tmp'.format(args.dlab_path))
            conn.put('./daemon.json',
                     '{}/tmp/daemon.json'.format(args.dlab_path))
            conn.sudo('sed -i "s|REPOSITORY|{}:{}|g" {}/tmp/daemon.json'
                      .format(args.repository_address,
                              args.repository_port,
                              args.dlab_path))
            if args.cloud_provider == "aws":
                dns_ip_resolve = (conn.run("systemd-resolve --status "
                                           "| grep -A 5 'Current Scopes: DNS' "
                                           "| grep 'DNS Servers:' "
                                           "| awk '{print $3}'")
                                  .stdout.rstrip("\n\r"))
                conn.sudo("sed -i 's|DNS_IP_RESOLVE|\"dns\": [\"{0}\"],|g' {1}/tmp/daemon.json"
                          .format(dns_ip_resolve, args.dlab_path))
            elif args.cloud_provider == "gcp" or args.cloud_provider == "azure":
                dns_ip_resolve = ""
                conn.sudo('sed -i "s|DNS_IP_RESOLVE||g" {1}/tmp/daemon.json'
                          .format(dns_ip_resolve, args.dlab_path))
            conn.sudo('mv {}/tmp/daemon.json /etc/docker'
                      .format(args.dlab_path))
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
            if args.auth_file_path:
                conn.put(args.auth_file_path, '/tmp/azure_auth.json')
                conn.sudo('mv /tmp/azure_auth.json /home/{}/keys/'.format(args.os_user))
                args.auth_file_path = '/home/{}/keys/azure_auth.json'.format(args.os_user)
    except Exception as err:
        logging.error('Failed create keys directory as ~/keys: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_keystore_endpoint(os_user, endpoint_keystore_password):
    try:
        conn.sudo('openssl pkcs12 -export -in /etc/ssl/certs/dlab.crt -inkey '
                  '/etc/ssl/certs/dlab.key -name endpoint -out /home/{0}/keys/endpoint.p12 '
                  '-password pass:{1}'.format(os_user, endpoint_keystore_password))
        conn.sudo('keytool -importkeystore -srckeystore /home/{0}/keys/endpoint.p12 -srcstoretype PKCS12 '
                  '-alias endpoint -destkeystore /home/{0}/keys/endpoint.keystore.jks -deststorepass "{1}" '
                  '-srcstorepass "{1}"'.format(os_user, endpoint_keystore_password))
        conn.sudo('keytool -keystore /home/{0}/keys/endpoint.keystore.jks -alias step-ca -import -file '
                  '/etc/ssl/certs/root_ca.crt  -deststorepass "{1}" -noprompt'.format(
                   os_user, endpoint_keystore_password))
        conn.sudo('keytool -importcert -trustcacerts -alias endpoint -file /etc/ssl/certs/dlab.crt -noprompt '
                  '-storepass changeit -keystore {0}/lib/security/cacerts'.format(java_home))
        conn.sudo('keytool -importcert -trustcacerts -file /etc/ssl/certs/root_ca.crt -noprompt '
                  '-storepass changeit -keystore {0}/lib/security/cacerts'.format(java_home))
        conn.sudo('touch /home/{0}/.ensure_dir/cert_imported'.format(os_user))
        print("Certificates are imported.")
    except Exception as err:
        print('Failed to configure Keystore certificates: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def configure_supervisor_endpoint(endpoint_keystore_password):
    try:
        if not exists(conn,
                      '/home/{}/.ensure_dir/configure_supervisor_ensured'.format(args.os_user)):
            supervisor_conf = '/etc/supervisor/conf.d/supervisor_svc.conf'
            if not exists(conn, '{}/tmp'.format(args.dlab_path)):
                conn.run('mkdir -p {}/tmp'.format(args.dlab_path))
            conn.put('./supervisor_svc.conf',
                     '{}/tmp/supervisor_svc.conf'.format(args.dlab_path))
            dlab_conf_dir = '{}/conf/'.format(args.dlab_path)
            if not exists(conn, dlab_conf_dir):
                conn.run('mkdir -p {}'.format(dlab_conf_dir))
            web_path = '{}/webapp'.format(args.dlab_path)
            if not exists(conn, web_path):
                conn.run('mkdir -p {}'.format(web_path))
            if args.cloud_provider == 'aws':
                interface = conn.sudo('curl -s http://169.254.169.254/latest/meta-data/network/interfaces/macs/').stdout
                args.vpc_id = conn.sudo('curl -s http://169.254.169.254/latest/meta-data/network/interfaces/macs/{}/'
                                        'vpc-id'.format(interface)).stdout
                args.subnet_id = conn.sudo('curl -s http://169.254.169.254/latest/meta-data/network/interfaces/macs/{}/'
                                           'subnet-id'.format(interface)).stdout
                args.vpc2_id = args.vpc_id
                args.subnet2_id = args.subnet_id
            conn.sudo('sed -i "s|OS_USR|{}|g" {}/tmp/supervisor_svc.conf'
                      .format(args.os_user, args.dlab_path))
            conn.sudo('sed -i "s|WEB_CONF|{}|g" {}/tmp/supervisor_svc.conf'
                      .format(dlab_conf_dir, args.dlab_path))
            conn.sudo('sed -i \'s=WEB_APP_DIR={}=\' {}/tmp/supervisor_svc.conf'
                      .format(web_path, args.dlab_path))
            conn.sudo('cp {}/tmp/supervisor_svc.conf {}'
                      .format(args.dlab_path, supervisor_conf))
            conn.put('./provisioning.yml', '{}provisioning.yml'
                     .format(dlab_conf_dir))
            if args.resource_group_name == '':
                args.resource_group_name = '{}-{}-resource-group'.format(args.service_base_name, args.endpoint_id)
            if args.cloud_provider == 'azure':
                args.region = args.region.lower().replace(' ', '')
            cloud_properties = [
                {
                    'key': "OS_USER",
                    'value': args.os_user
                },
                {
                    'key': "KEYNAME",
                    'value': args.key_name
                },
                {
                    'key': "KEYSTORE_PASSWORD",
                    'value': endpoint_keystore_password
                },
                {
                    'key': "JRE_HOME",
                    'value': java_home
                },
                {
                    'key': "CLOUD_PROVIDER",
                    'value': args.cloud_provider
                },
                {
                    'key': "MONGO_HOST",
                    'value': args.mongo_host
                },
                {
                    'key': "MONGO_PORT",
                    'value': args.mongo_port
                },
                {
                    'key': "SSN_UI_HOST",
                    'value': args.ssn_ui_host
                },
                {
                    'key': "KEYCLOAK_CLIENT_ID",
                    'value': args.keycloak_client_id
                },
                {
                    'key': "CLIENT_SECRET",
                    'value': args.keycloak_client_secret
                },
                {
                    'key': "CONF_OS",
                    'value': args.env_os
                },
                {
                    'key': "SERVICE_BASE_NAME",
                    'value': args.service_base_name
                },
                {
                    'key': "EDGE_INSTANCE_SIZE",
                    'value': "" # args.edge_instence_size
                },
                {
                    'key': "SUBNET_ID",
                    'value': args.subnet_id
                },
                {
                    'key': "REGION",
                    'value': args.region
                },
                {
                    'key': "ZONE",
                    'value': args.zone
                },
                {
                    'key': "TAG_RESOURCE_ID",
                    'value': args.tag_resource_id
                },
                {
                    'key': "SG_IDS",
                    'value': args.ssn_k8s_sg_id
                },
                {
                    'key': "SSN_INSTANCE_SIZE",
                    'value': args.ssn_instance_size
                },
                {
                    'key': "VPC2_ID",
                    'value': args.vpc2_id
                },
                {
                    'key': "SUBNET2_ID",
                    'value': args.subnet2_id
                },
                {
                    'key': "CONF_KEY_DIR",
                    'value': args.conf_key_dir
                },
                {
                    'key': "VPC_ID",
                    'value': args.vpc_id
                },
                {
                    'key': "PEERING_ID",
                    'value': args.peering_id
                },
                {
                    'key': "AZURE_RESOURCE_GROUP_NAME",
                    'value': args.resource_group_name
                },
                {
                    'key': "AZURE_SSN_STORAGE_ACCOUNT_TAG",
                    'value': args.azure_ssn_storage_account_tag
                },
                {
                    'key': "AZURE_SHARED_STORAGE_ACCOUNT_TAG",
                    'value': args.azure_shared_storage_account_tag
                },
                {
                    'key': "AZURE_DATALAKE_TAG",
                    'value': args.azure_datalake_tag
                },
                {
                    'key': "AZURE_CLIENT_ID",
                    'value': args.azure_client_id
                },
                {
                    'key': "GCP_PROJECT_ID",
                    'value': args.gcp_project_id
                },
                {
                    'key': "LDAP_HOST",
                    'value': args.ldap_host
                },
                {
                    'key': "LDAP_DN",
                    'value': args.ldap_dn
                },
                {
                    'key': "LDAP_OU",
                    'value': args.ldap_users_group
                },
                {
                    'key': "LDAP_USER_NAME",
                    'value': args.ldap_user
                },
                {
                    'key': "LDAP_USER_PASSWORD",
                    'value': args.ldap_bind_creds
                },
                {
                    'key': "STEP_CERTS_ENABLED",
                    'value': "true"
                },
                {
                    'key': "STEP_ROOT_CA",
                    'value': args.step_root_ca
                },
                {
                    'key': "STEP_KID_ID",
                    'value': args.step_kid
                },
                {
                    'key': "STEP_KID_PASSWORD",
                    'value': args.step_kid_password
                },
                {
                    'key': "STEP_CA_URL",
                    'value': args.step_ca_url
                },
                {
                    'key': "SHARED_IMAGE_ENABLED",
                    'value': args.shared_image_enabled
                },
                {
                    'key': "CONF_IMAGE_ENABLED",
                    'value': args.image_enabled
                },
                {
                    'key': "KEYCLOAK_AUTH_SERVER_URL",
                    'value': args.keycloak_auth_server_url
                },
                {
                    'key': "KEYCLOAK_REALM_NAME",
                    'value': args.keycloak_realm_name
                },
                {
                    'key': "KEYCLOAK_USER_NAME",
                    'value': args.keycloak_user_name
                },
                {
                    'key': "KEYCLOAK_PASSWORD",
                    'value': args.keycloak_user_password
                },
                {
                    'key': "AZURE_AUTH_FILE_PATH",
                    'value': args.auth_file_path
                }
            ]
            for param in cloud_properties:
                conn.sudo('sed -i "s|{0}|{1}|g" {2}provisioning.yml'
                          .format(param['key'], param['value'], dlab_conf_dir))

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
            web_path = '{}/webapp'.format(args.dlab_path)
            if not exists(conn, web_path):
                conn.run('mkdir -p {}'.format(web_path))
            conn.run('wget -P {}  --user={} --password={} '
                     'https://{}/repository/packages/provisioning-service-'
                     '2.2.jar --no-check-certificate'
                     .format(web_path, args.repository_user,
                             args.repository_pass, args.repository_address))
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
        conn.run("git clone https://github.com/apache/incubator-dlab.git {0}/sources".format(args.dlab_path))
        if args.branch_name != "":
            conn.run("cd {0}/sources && git checkout {1} && cd".format(args.dlab_path, args.branch_name))
    except Exception as err:
        logging.error('Failed to download sources: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def pull_docker_images():
    try:
        ensure_file = ('/home/{}/.ensure_dir/docker_images_pulled'
                       .format(args.os_user))
        if not exists(conn, ensure_file):
            list_images = {
                'aws': ['base', 'edge', 'project', 'jupyter', 'rstudio', 'zeppelin', 'tensor', 'tensor-rstudio',
                        'deeplearning', 'jupyterlab', 'dataengine-service', 'dataengine'],
                'gcp': ['base', 'edge', 'project', 'jupyter', 'rstudio', 'zeppelin', 'tensor', 'tensor-rstudio',
                        'deeplearning', 'superset', 'jupyterlab', 'dataengine-service', 'dataengine'],
                'azure': ['base', 'edge', 'project', 'jupyter', 'rstudio', 'zeppelin', 'tensor', 'deeplearning',
                          'dataengine']
            }
            conn.sudo('docker login -u {} -p {} {}:{}'
                      .format(args.repository_user,
                              args.repository_pass,
                              args.repository_address,
                              args.repository_port))
            for image in list_images[args.cloud_provider]:
                conn.sudo('docker pull {0}:{1}/docker.dlab-{3}-{2}'
                          .format(args.repository_address, args.repository_port, args.cloud_provider, image))
                conn.sudo('docker tag {0}:{1}/docker.dlab-{3}-{2} docker.dlab-{3}'
                          .format(args.repository_address, args.repository_port, args.cloud_provider, image))
                conn.sudo('docker rmi {0}:{1}/docker.dlab-{3}-{2}'
                          .format(args.repository_address, args.repository_port, args.cloud_provider, image))
            conn.sudo('chown -R {0}:docker /home/{0}/.docker/'
                      .format(args.os_user))
            conn.sudo('touch {}'.format(ensure_file))
    except Exception as err:
        logging.error('Failed to pull Docker images: ', str(err))
        traceback.print_exc()
        sys.exit(1)


def id_generator(size=10, chars=string.digits + string.ascii_letters):
    return ''.join(random.choice(chars) for _ in range(size))


def configure_guacamole():
    try:
        mysql_pass = id_generator()
        conn.sudo('docker run --name guacd --restart unless-stopped -d -p 4822:4822 guacamole/guacd')
        conn.sudo('docker run --rm guacamole/guacamole /opt/guacamole/bin/initdb.sh --mysql > initdb.sql')
        conn.sudo('mkdir /tmp/scripts')
        conn.sudo('cp initdb.sql /tmp/scripts')
        conn.sudo('mkdir -p /opt/mysql')
        conn.sudo('docker run --name guac-mysql --restart unless-stopped -v /tmp/scripts:/tmp/scripts '
                  ' -v /opt/mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD={} -d mysql:latest'.format(mysql_pass))
        time.sleep(180)
        conn.sudo('touch /opt/mysql/dock-query.sql')
        conn.sudo('chown {0}:{0} /opt/mysql/dock-query.sql'.format(args.os_user))
        conn.sudo("""echo "CREATE DATABASE guacamole; CREATE USER 'guacamole' IDENTIFIED BY '{}';"""
                  """ GRANT SELECT,INSERT,UPDATE,DELETE ON guacamole.* TO 'guacamole';" > /opt/mysql/dock-query.sql"""
                  .format(mysql_pass))
        conn.sudo('docker exec -i guac-mysql /bin/bash -c "mysql -u root -p{} < /var/lib/mysql/dock-query.sql"'
                  .format(mysql_pass))
        conn.sudo('docker exec -i guac-mysql /bin/bash -c "cat /tmp/scripts/initdb.sql | mysql -u root -p{} guacamole"'
                  .format(mysql_pass))
        conn.sudo("docker run --name guacamole --restart unless-stopped --link guacd:guacd --link guac-mysql:mysql"
                  " -e MYSQL_DATABASE='guacamole' -e MYSQL_USER='guacamole' -e MYSQL_PASSWORD='{}'"
                  " -d -p 8080:8080 guacamole/guacamole".format(mysql_pass))
        # create cronjob for run containers on reboot
        conn.sudo('mkdir -p /opt/dlab/cron')
        conn.sudo('touch /opt/dlab/cron/mysql.sh')
        conn.sudo('chmod 755 /opt/dlab/cron/mysql.sh')
        conn.sudo('chown {0}:{0} //opt/dlab/cron/mysql.sh'.format(args.os_user))
        conn.sudo('echo "docker start guacd" >> /opt/dlab/cron/mysql.sh')
        conn.sudo('echo "docker start guac-mysql" >> /opt/dlab/cron/mysql.sh')
        conn.sudo('echo "docker rm guacamole" >> /opt/dlab/cron/mysql.sh')
        conn.sudo("""echo "docker run --name guacamole --restart unless-stopped --link guacd:guacd""" 
                  """ --link guac-mysql:mysql -e MYSQL_DATABASE='guacamole' -e MYSQL_USER='guacamole' """
                  """-e MYSQL_PASSWORD='{}' -d -p 8080:8080 guacamole/guacamole" >> """
                  """/opt/dlab/cron/mysql.sh""".format(mysql_pass))
        conn.sudo('''/bin/bash -c '(crontab -l 2>/dev/null; echo "@reboot sh /opt/dlab/cron/mysql.sh") |''' 
                  ''' crontab - ' ''')
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure guacamole: ', str(err))
        return False


def init_args():
    global args
    parser = argparse.ArgumentParser()
    parser.add_argument('--dlab_path', type=str, default='/opt/dlab')
    parser.add_argument('--key_name', type=str, default='', help='Name of admin key without .pem extension')
    parser.add_argument('--endpoint_eip_address', type=str)
    parser.add_argument('--endpoint_id', type=str, default='')
    parser.add_argument('--pkey', type=str, default='')
    parser.add_argument('--hostname', type=str, default='')
    parser.add_argument('--os_user', type=str, default='dlab-user')
    parser.add_argument('--cloud_provider', type=str, default='')
    parser.add_argument('--mongo_host', type=str, default='MONGO_HOST')
    parser.add_argument('--mongo_port', type=str, default='27017')
    parser.add_argument('--ss_host', type=str, default='')
    parser.add_argument('--ss_port', type=str, default='8443')
    parser.add_argument('--ssn_ui_host', type=str, default='')
    # parser.add_argument('--mongo_password', type=str, default='')
    parser.add_argument('--repository_address', type=str, default='')
    parser.add_argument('--repository_port', type=str, default='')
    parser.add_argument('--repository_user', type=str, default='')
    parser.add_argument('--repository_pass', type=str, default='')
    parser.add_argument('--docker_version', type=str,
                        default='18.06.3~ce~3-0~ubuntu')
    parser.add_argument('--ssn_bucket_name', type=str, default='')
    parser.add_argument('--keycloak_auth_server_url', type=str, default='')
    parser.add_argument('--keycloak_realm_name', type=str, default='')
    parser.add_argument('--keycloak_user_name', type=str, default='')
    parser.add_argument('--keycloak_user_password', type=str, default='')
    parser.add_argument('--keycloak_client_id', type=str, default='')
    parser.add_argument('--keycloak_client_secret', type=str, default='')
    parser.add_argument('--branch_name', type=str, default='master')  # change default
    parser.add_argument('--env_os', type=str, default='debian')
    parser.add_argument('--service_base_name', type=str, default='')
    parser.add_argument('--edge_instence_size', type=str, default='t2.medium')
    parser.add_argument('--subnet_id', type=str, default='')
    parser.add_argument('--region', type=str, default='')
    parser.add_argument('--zone', type=str, default='')
    parser.add_argument('--tag_resource_id', type=str, default='user:tag')
    parser.add_argument('--ssn_k8s_sg_id', type=str, default='')
    parser.add_argument('--ssn_instance_size', type=str, default='t2.large')
    parser.add_argument('--vpc2_id', type=str, default='')
    parser.add_argument('--subnet2_id', type=str, default='')
    parser.add_argument('--conf_key_dir', type=str, default='/root/keys/', help='Should end by symbol /')
    parser.add_argument('--vpc_id', type=str, default='')
    parser.add_argument('--peering_id', type=str, default='')
    parser.add_argument('--resource_group_name', type=str, default='')
    parser.add_argument('--azure_ssn_storage_account_tag', type=str, default='')
    parser.add_argument('--azure_shared_storage_account_tag', type=str, default='')
    parser.add_argument('--azure_datalake_tag', type=str, default='')
    parser.add_argument('--azure_datalake_enabled', type=str, default='')
    parser.add_argument('--azure_client_id', type=str, default='')
    parser.add_argument('--gcp_project_id', type=str, default='')
    parser.add_argument('--ldap_host', type=str, default='')
    parser.add_argument('--ldap_dn', type=str, default='')
    parser.add_argument('--ldap_users_group', type=str, default='')
    parser.add_argument('--ldap_user', type=str, default='')
    parser.add_argument('--ldap_bind_creds', type=str, default='')
    parser.add_argument('--step_root_ca', type=str, default='')
    parser.add_argument('--step_kid', type=str, default='')
    parser.add_argument('--step_kid_password', type=str, default='')
    parser.add_argument('--step_ca_url', type=str, default='')
    parser.add_argument('--shared_image_enabled', type=str, default='true')
    parser.add_argument('--image_enabled', type=str, default='true')
    parser.add_argument('--auth_file_path', type=str, default='')

    # TEMPORARY
    parser.add_argument('--ssn_k8s_nlb_dns_name', type=str, default='')
    parser.add_argument('--ssn_k8s_alb_dns_name', type=str, default='')
    # TEMPORARY

    print(parser.parse_known_args())
    args = parser.parse_known_args()[0]


def update_system():
    conn.sudo('apt-get update')
    conn.sudo('apt-get install -y jq')


def init_dlab_connection(ip=None, user=None,
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
        logging.error('Failed connect as dlab-user: ', str(err))
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
    endpoint_keystore_password = id_generator()

    print("Start provisioning of Endpoint.")
    time.sleep(40)

    print(args)
    logging.info("Creating dlab-user")
    create_user()

    init_dlab_connection()
    update_system()

    logging.info("Configuring ensure dir")
    ensure_dir_endpoint()

    logging.info("Configuring Logs")
    ensure_logs_endpoint()

    logging.info("Installing Java")
    ensure_jre_jdk_endpoint()

    set_java_home()

    logging.info("Creating key directory")
    create_key_dir_endpoint()

    logging.info("Installing Step Certificates")
    ensure_step_certs()

    logging.info("Installing Supervisor")
    ensure_supervisor_endpoint()

    logging.info("Installing Docker")
    ensure_docker_endpoint()

    logging.info("Configuring Supervisor")
    configure_supervisor_endpoint(endpoint_keystore_password)

    logging.info("Copying admin key")
    copy_keys()

    logging.info("Configuring certificates")
    configure_keystore_endpoint(args.os_user, endpoint_keystore_password)

    logging.info("Ensure jar")
    ensure_jar_endpoint()

    logging.info("Downloading sources")
    get_sources()

    logging.info("Pulling docker images")
    pull_docker_images()

    logging.info("Configuring guacamole")
    configure_guacamole()

    logging.info("Starting supervisor")
    start_supervisor_endpoint()

    close_connection()
    print("Done provisioning of Endpoint.")


if __name__ == "__main__":
    start_deploy()
