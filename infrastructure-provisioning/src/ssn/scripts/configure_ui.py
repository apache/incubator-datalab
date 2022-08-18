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
import logging
import os
import sys
import traceback
import subprocess
from datalab.fab import *
from datalab.ssn_lib import *
from fabric import *
from patchwork.files import exists
from patchwork import files
import time

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--additional_config', type=str, default='{"empty":"string"}')
parser.add_argument('--datalab_path', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--cloud_provider', type=str, default='')
parser.add_argument('--os_family', type=str, default='')
parser.add_argument('--request_id', type=str, default='')
parser.add_argument('--resource', type=str, default='')
parser.add_argument('--service_base_name', type=str, default='')
parser.add_argument('--billing_dataset_name', type=str, default='')
parser.add_argument('--default_endpoint_name', type=str, default='')
parser.add_argument('--tag_resource_id', type=str, default=None)
parser.add_argument('--billing_tag', type=str, default=None)
parser.add_argument('--account_id', type=str, default=None)
parser.add_argument('--billing_bucket', type=str, default=None)
parser.add_argument('--aws_job_enabled', type=str, default=None)
parser.add_argument('--report_path', type=str, default=None)
parser.add_argument('--authentication_file', type=str, default=None)
parser.add_argument('--offer_number', type=str, default=None)
parser.add_argument('--currency', type=str, default=None)
parser.add_argument('--locale', type=str, default=None)
parser.add_argument('--region_info', type=str, default=None)
parser.add_argument('--billing_enabled', type=str, default=False)
parser.add_argument('--ldap_login', type=str, default=None)
parser.add_argument('--tenant_id', type=str, default=None)
parser.add_argument('--application_id', type=str, default=None)
parser.add_argument('--subscription_id', type=str, default=None)
parser.add_argument('--datalake_store_name', type=str, default=None)
parser.add_argument('--validate_permission_scope', type=str, default=None)
parser.add_argument('--cloud_params', type=str, default='')
parser.add_argument('--datalab_id', type=str, default=None)
parser.add_argument('--usage_date', type=str, default=None)
parser.add_argument('--product', type=str, default=None)
parser.add_argument('--usage_type', type=str, default=None)
parser.add_argument('--usage', type=str, default=None)
parser.add_argument('--cost', type=str, default=None)
parser.add_argument('--resource_id', type=str, default=None)
parser.add_argument('--tags', type=str, default=None)
parser.add_argument('--keycloak_client_id', type=str, default=None)
parser.add_argument('--keycloak_client_secret', type=str, default=None)
parser.add_argument('--keycloak_auth_server_url', type=str, default=None)
parser.add_argument('--keycloak_realm_name', type=str, default=None)
args = parser.parse_args()

datalab_conf_dir = args.datalab_path + 'conf/'
web_path = args.datalab_path + 'webapp/'
local_log_filename = "{}_UI.log".format(args.request_id)
local_log_filepath = "/logs/" + args.resource + "/" + local_log_filename
logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                    level=logging.INFO,
                    filename=local_log_filepath)
mongo_passwd = id_generator()
keystore_passwd = id_generator()


def copy_ssn_libraries():
    try:
        conn.sudo('mkdir -p /usr/lib/python3.8/datalab/')
        conn.run('mkdir -p /tmp/datalab_libs/')
        subprocess.run('rsync -e "ssh -i {}" /usr/lib/python3.8/datalab/*.py {}:/tmp/datalab_libs/'.format(args.keyfile, host_string), shell=True, check=True)
        conn.run('chmod a+x /tmp/datalab_libs/*')
        conn.sudo('mv /tmp/datalab_libs/* /usr/lib/python3.8/datalab/')
        if exists(conn, '/usr/lib64'):
            conn.sudo('mkdir -p /usr/lib64/python3.8')
            conn.sudo('ln -fs /usr/lib/python3.8/datalab /usr/lib64/python3.8/datalab')
    except Exception as err:
        traceback.print_exc()
        print('Failed to copy ssn libraries: ', str(err))
        sys.exit(1)

def configure_mongo(mongo_passwd, default_endpoint_name):
    try:
        if not exists(conn,"/lib/systemd/system/mongod.service"):
            if os.environ['conf_os_family'] == 'debian':
                subprocess.run('sed -i "s/MONGO_USR/mongodb/g" /root/templates/mongod.service_template', shell=True, check=True)
            elif os.environ['conf_os_family'] == 'redhat':
                subprocess.run('sed -i "s/MONGO_USR/mongod/g" /root/templates/mongod.service_template', shell=True, check=True)
            subprocess.run('rsync -e "ssh -i {}" /root/templates/mongod.service_template {}:/tmp/mongod.service'.format(args.keyfile,
                                                                                                    host_string), shell=True, check=True)
            conn.sudo('mv /tmp/mongod.service /lib/systemd/system/mongod.service')
            conn.sudo('systemctl daemon-reload')
            conn.sudo('systemctl enable mongod.service')
        subprocess.run('sed -i "s|PASSWORD|{}|g" /root/scripts/resource_status.py'.format(mongo_passwd), shell=True, check=True)
        subprocess.run('rsync -e "ssh -i {}" /root/scripts/resource_status.py {}:/tmp/resource_status.py'.format(args.keyfile,
                                                                                             host_string), shell=True, check=True)
        conn.sudo('mv /tmp/resource_status.py ' + os.environ['ssn_datalab_path'] + 'tmp/')
        subprocess.run('sed -i "s|PASSWORD|{}|g" /root/scripts/configure_mongo.py'.format(mongo_passwd), shell=True, check=True)
        subprocess.run('rsync -e "ssh -i {}" /root/scripts/configure_mongo.py {}:/tmp/configure_mongo.py'.format(args.keyfile,
                                                                                             host_string), shell=True, check=True)
        conn.sudo('mv /tmp/configure_mongo.py ' + args.datalab_path + 'tmp/')
        subprocess.run('rsync -e "ssh -i {}" /root/files/{}/mongo_roles.json {}:/tmp/mongo_roles.json'.format(args.keyfile,
                                                                                          args.cloud_provider,
                                                                                          host_string), shell=True, check=True)
        subprocess.run('rsync -e "ssh -i {}" /root/files/local_endpoint.json {}:/tmp/local_endpoint.json'.format(args.keyfile,
                                                                                             host_string), shell=True, check=True)
        conn.sudo('mv /tmp/mongo_roles.json ' + args.datalab_path + 'tmp/')
        conn.sudo('sed -i "s|DEF_ENDPOINT_NAME|{0}|g" /tmp/local_endpoint.json'.format(default_endpoint_name))
        conn.sudo('sed -i "s|CLOUD_PROVIDER|{0}|g" /tmp/local_endpoint.json'.format(
            os.environ['conf_cloud_provider'].upper()))
        conn.sudo('mv /tmp/local_endpoint.json ' + args.datalab_path + 'tmp/')
        conn.sudo('pip3 install -U six==1.15.0 patchwork')
        conn.sudo("python3 " + args.datalab_path + "tmp/configure_mongo.py --datalab_path {} ".format(
            args.datalab_path))
    except Exception as err:
        traceback.print_exc()
        print('Failed to configure MongoDB: ', str(err))
        sys.exit(1)


def build_ui():
    try:
        # Building Front-end
        conn.sudo('sed -i "s|CLOUD_PROVIDER|{}|g" ' + args.datalab_path + 'sources/services/self-service/src/main/resources/webapp/src/dictionary/global.dictionary.ts'.format(args.cloud_provider))

        if args.cloud_provider == 'azure' and os.environ['azure_datalake_enable'] == 'true':
            conn.sudo('sed -i "s|\'use_ldap\': true|{}|g" ' + args.datalab_path + 'sources/services/self-service/src/main/resources/webapp/src/dictionary/azure.dictionary.ts'.format(
                    '\'use_ldap\': false'))
        conn.sudo('rm -rf {}sources/services/self-service/src/main/resources/webapp/node_modules'.format(
            args.datalab_path))
        conn.sudo('bash -c "cd {}sources/services/self-service/src/main/resources/webapp/ && echo "N" | npm install"'.format(args.datalab_path))
        manage_npm_pkg('bash -c "cd {}sources/services/self-service/src/main/resources/webapp/ && npm run build.prod"'.format(args.datalab_path))
        conn.sudo('sudo chown -R {} {}/*'.format(args.os_user, args.datalab_path))

        # Building Back-end
        if 'conf_repository_user' in os.environ and 'conf_repository_pass' in os.environ and 'conf_repository_address' in os.environ and os.environ['conf_download_jars'] == 'true':
            conn.sudo(
                'wget -P {0}sources/services/provisioning-service/target/  --user={1} --password={2} https://{3}/repository/packages/{4}/provisioning-service-{4}.jar --no-check-certificate'
                     .format(args.datalab_path, os.environ['conf_repository_user'], os.environ['conf_repository_pass'], os.environ['conf_repository_address'], os.environ['conf_release_tag']))
            conn.sudo(
                'wget -P {0}sources/services/self-service/target/  --user={1} --password={2} https://{3}/repository/packages/{4}/self-service-{4}.jar --no-check-certificate'
                .format(args.datalab_path, os.environ['conf_repository_user'], os.environ['conf_repository_pass'],
                        os.environ['conf_repository_address'], os.environ['conf_release_tag']))
            conn.sudo(
                'wget -P {0}sources/services/billing-{4}/target/  --user={1} --password={2} https://{3}/repository/packages/{5}/billing-{4}-{5}.jar --no-check-certificate'
                .format(args.datalab_path, os.environ['conf_repository_user'], os.environ['conf_repository_pass'],
                        os.environ['conf_repository_address'], args.cloud_provider, os.environ['conf_release_tag']))
        else:
            try:
                conn.sudo('bash -c "cd {}sources/ && /opt/maven/bin/mvn -P{} -DskipTests package 2>&1 > /tmp/maven.log"'.format(args.datalab_path, args.cloud_provider))
            except:
                conn.run('if ! grep -w -E "(ERROR)" /tmp/maven.log > /tmp/maven_error.log; then echo "no_error" > /tmp/maven_error.log;fi')
                conn.run('cat /tmp/maven_error.log')
                print('Failed to build Back-end: ', str(err))
                sys.exit(1)
        conn.sudo('mkdir -p {}webapp/'.format(args.datalab_path))
        for service in ['self-service', 'provisioning-service', 'billing']:
            conn.sudo('mkdir -p {}webapp/{}/lib/'.format(args.datalab_path, service))
            conn.sudo('mkdir -p {}webapp/{}/conf/'.format(args.datalab_path, service))
        conn.sudo('cp {0}sources/services/self-service/self-service.yml {0}webapp/self-service/conf/'.format(
            args.datalab_path))
        conn.sudo('cp {0}sources/services/self-service/target/self-service-*.jar {0}webapp/self-service/lib/'.format(
            args.datalab_path))
        conn.sudo(
            'cp {0}sources/services/provisioning-service/provisioning.yml {0}webapp/provisioning-service/conf/'.format(
                args.datalab_path))
        conn.sudo('cp {0}sources/services/provisioning-service/target/provisioning-service-*.jar '
             '{0}webapp/provisioning-service/lib/'.format(args.datalab_path))

        if args.cloud_provider == 'azure':
            conn.sudo('cp {0}sources/services/billing-azure/billing.yml {0}webapp/billing/conf/'.format(args.datalab_path))
            conn.sudo('cp {0}sources/services/billing-azure/target/billing-azure*.jar {0}webapp/billing/lib/'.format(
                args.datalab_path))
        elif args.cloud_provider == 'aws':
            conn.sudo('cp {0}sources/services/billing-aws/billing.yml {0}webapp/billing/conf/'.format(args.datalab_path))
            conn.sudo('cp {0}sources/services/billing-aws/src/main/resources/application.yml '
                 '{0}webapp/billing/conf/billing_app.yml'.format(args.datalab_path))
            conn.sudo(
                'cp {0}sources/services/billing-aws/target/billing-aws*.jar {0}webapp/billing/lib/'.format(
                    args.datalab_path))
        elif args.cloud_provider == 'gcp':
            conn.sudo('cp {0}sources/services/billing-gcp/billing.yml {0}webapp/billing/conf/'.format(args.datalab_path))
            conn.sudo(
                'cp {0}sources/services/billing-gcp/target/billing-gcp*.jar {0}webapp/billing/lib/'.format(
                    args.datalab_path))
    except Exception as err:
        traceback.print_exc()
        print('Failed to build UI: ', str(err))
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

    logging.info("Installing Supervisor")
    ensure_supervisor()

    print("Installing MongoDB")
    ensure_mongo()

    print("Configuring MongoDB")
    configure_mongo(mongo_passwd, args.default_endpoint_name)

    conn.sudo('bash -c "echo DATALAB_CONF_DIR={} >> /etc/profile"'.format(datalab_conf_dir))
    conn.sudo('bash -c "echo export DATALAB_CONF_DIR >> /etc/profile"')

    print("Installing build dependencies for UI")
    install_build_dep()

    print("Building UI")
    build_ui()

    print("Starting Self-Service(UI)")
    start_ss(args.keyfile, host_string, datalab_conf_dir, web_path,
             args.os_user, mongo_passwd, keystore_passwd, args.cloud_provider,
             args.service_base_name, args.tag_resource_id, args.billing_tag, args.account_id,
             args.billing_bucket, args.aws_job_enabled, args.datalab_path, args.billing_enabled, args.cloud_params,
             args.authentication_file, args.offer_number, args.currency, args.locale,
             args.region_info, args.ldap_login, args.tenant_id, args.application_id,
             args.hostname, args.datalake_store_name, args.subscription_id, args.validate_permission_scope,
             args.datalab_id, args.usage_date, args.product, args.usage_type,
             args.usage, args.cost, args.resource_id, args.tags, args.billing_dataset_name, args.keycloak_client_id,
             args.keycloak_client_secret, args.keycloak_auth_server_url, args.keycloak_realm_name)

    conn.close()
