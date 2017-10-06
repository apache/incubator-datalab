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

from dlab.fab import *
from dlab.actions_lib import *
from dlab.meta_lib import *
import sys, os
from fabric.api import *
from dlab.ssn_lib import *
import traceback

if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    instance = 'ssn'
    pre_defined_resource_group = False
    pre_defined_vpc = False
    pre_defined_subnet = False
    pre_defined_sg = False
    billing_enabled = False

    try:
        logging.info('[DERIVING NAMES]')
        print '[DERIVING NAMES]'

        ssn_conf = dict()
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name'].replace('_', '-')[:12]
        ssn_conf['region'] = os.environ['azure_region']
        ssn_conf['ssn_storage_account_name'] = ssn_conf['service_base_name'] + '-ssn-storage'
        ssn_conf['ssn_container_name'] = (ssn_conf['service_base_name'] + '-ssn-container').lower()
        ssn_conf['shared_storage_account_name'] = ssn_conf['service_base_name'] + '-shared-storage'
        ssn_conf['shared_container_name'] = (ssn_conf['service_base_name'] + '-shared-container').lower()
        ssn_conf['instance_name'] = ssn_conf['service_base_name'] + '-ssn'
        ssn_conf['vpc_name'] = ssn_conf['service_base_name'] + '-vpc'
        ssn_conf['subnet_name'] = ssn_conf['service_base_name'] + '-ssn-subnet'
        ssn_conf['security_group_name'] = ssn_conf['service_base_name'] + '-ssn-sg'
        ssn_conf['ssh_key_path'] = os.environ['conf_key_dir'] + os.environ['conf_key_name'] + '.pem'
        ssn_conf['dlab_ssh_user'] = os.environ['conf_os_user']
        ssn_conf['instance_dns_name'] = 'host-' + ssn_conf['instance_name'] + '.' + ssn_conf['region'] + '.cloudapp.azure.com'

        try:
            if os.environ['azure_resource_group_name'] == '':
                raise KeyError
        except KeyError:
            os.environ['azure_resource_group_name'] = ssn_conf['service_base_name']
            pre_defined_resource_group = True
        try:
            if os.environ['azure_vpc_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_vpc = True
        try:
            if os.environ['azure_subnet_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_subnet = True
        try:
            if os.environ['aws_account_id'] == '':
                raise KeyError
            if os.environ['aws_billing_bucket'] == '':
                raise KeyError
        except KeyError:
            billing_enabled = False
        if not billing_enabled:
            os.environ['aws_account_id'] = 'None'
            os.environ['aws_billing_bucket'] = 'None'
            os.environ['aws_report_path'] = 'None'
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'
    except:
        print "Failed to generate variables dictionary."
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(os.environ['azure_resource_group_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'],
                                         ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(os.environ['azure_resource_group_name'],
                                                 ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
        AzureActions().remove_instance(os.environ['azure_resource_group_name'], ssn_conf['instance_name'])
        append_result("Failed creating ssh user 'dlab-user'.", str(err))
        sys.exit(1)

    try:
        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (ssn_conf['instance_dns_name'], ssn_conf['ssh_key_path'], initial_user, ssn_conf['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(os.environ['azure_resource_group_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'],
                                         ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(os.environ['azure_resource_group_name'],
                                                 ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
        AzureActions().remove_instance(os.environ['azure_resource_group_name'], ssn_conf['instance_name'])
        append_result("Failed creating ssh user 'dlab-user'.", str(err))
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        print('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages 'argparse fabric pymongo pyyaml pycrypto azure' --user {} --region {}". \
            format(ssn_conf['instance_dns_name'], ssn_conf['ssh_key_path'], ssn_conf['dlab_ssh_user'], ssn_conf['region'])

        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(os.environ['azure_resource_group_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'],
                                         ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(os.environ['azure_resource_group_name'],
                                                 ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
        AzureActions().remove_instance(os.environ['azure_resource_group_name'], ssn_conf['instance_name'])
        append_result("Failed installing software: pip, packages.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        print('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/",
                             "service_base_name": ssn_conf['service_base_name'],
                             "security_group_id": ssn_conf['security_group_name'], "vpc_id": ssn_conf['vpc_name'],
                             "subnet_id": ssn_conf['subnet_name'], "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --dlab_path {} --tag_resource_id {}". \
            format(ssn_conf['instance_dns_name'], ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'], ssn_conf['service_base_name'])

        try:
            local("~/scripts/{}.py {}".format('configure_ssn_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(os.environ['azure_resource_group_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'],
                                         ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(os.environ['azure_resource_group_name'],
                                                 ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
        AzureActions().remove_instance(os.environ['azure_resource_group_name'], ssn_conf['instance_name'])
        append_result("Failed configuring ssn.", str(err))
        sys.exit(1)

    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        print('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "deeplearning", "tag": "latest"},
                             {"name": "dataengine", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --dlab_path {} --cloud_provider {} --region {}". \
            format(ssn_conf['instance_dns_name'], ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   os.environ['conf_os_family'], ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'],
                   os.environ['conf_cloud_provider'], ssn_conf['region'])

        try:
            local("~/scripts/{}.py {}".format('configure_docker', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(os.environ['azure_resource_group_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'],
                                         ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(os.environ['azure_resource_group_name'],
                                                 ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
        AzureActions().remove_instance(os.environ['azure_resource_group_name'], ssn_conf['instance_name'])
        append_result("Unable to configure docker.", str(err))
        sys.exit(1)

    try:
        mongo_parameters = {
            "azure_resource_group_name": os.environ['azure_resource_group_name'],
            "azure_region": ssn_conf['region'],
            "azure_vpc_name": ssn_conf['vpc_name'],
            "azure_subnet_name": ssn_conf['subnet_name'],
            "conf_service_base_name": ssn_conf['service_base_name'],
            "azure_security_group_name": ssn_conf['security_group_name'],
            "conf_os_family": os.environ['conf_os_family'],
            "conf_key_dir": os.environ['conf_key_dir']
        }
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')
        params = "--hostname {} --keyfile {} --dlab_path {} --os_user {} --os_family {} --request_id {} --resource {} --service_base_name {} --tag_resource_id {} --cloud_provider {} --account_id {} --billing_bucket {} --report_path '{}' --billing_enabled {} --mongo_parameters '{}'". \
            format(ssn_conf['instance_dns_name'], ssn_conf['ssh_key_path'], os.environ['ssn_dlab_path'],
                   ssn_conf['dlab_ssh_user'], os.environ['conf_os_family'], os.environ['request_id'],
                   os.environ['conf_resource'], ssn_conf['service_base_name'], os.environ['conf_tag_resource_id'],
                   os.environ['conf_cloud_provider'], os.environ['aws_account_id'], os.environ['aws_billing_bucket'],
                   os.environ['aws_report_path'], billing_enabled, json.dumps(mongo_parameters))

        try:
            local("~/scripts/{}.py {}".format('configure_ui', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        if pre_defined_resource_group:
            AzureActions().remove_resource_group(os.environ['azure_resource_group_name'], ssn_conf['region'])
        if pre_defined_vpc:
            AzureActions().remove_vpc(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'])
            AzureActions().remove_subnet(os.environ['azure_resource_group_name'], ssn_conf['vpc_name'],
                                         ssn_conf['subnet_name'])
        if pre_defined_sg:
            AzureActions().remove_security_group(os.environ['azure_resource_group_name'],
                                                 ssn_conf['security_group_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                AzureActions().remove_storage_account(os.environ['azure_resource_group_name'], storage_account.name)
        AzureActions().remove_instance(os.environ['azure_resource_group_name'], ssn_conf['instance_name'])
        append_result("Unable to configure UI.", str(err))
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        instance_hostname = AzureMeta().get_instance_public_ip_address(os.environ['azure_resource_group_name'],
                                                                       ssn_conf['instance_name'])
        for storage_account in AzureMeta().list_storage_accounts(os.environ['azure_resource_group_name']):
            if ssn_conf['ssn_storage_account_name'] == storage_account.tags["Name"]:
                ssn_storage_account_name = storage_account.name
            if ssn_conf['shared_storage_account_name'] == storage_account.tags["Name"]:
                shared_storage_account_name = storage_account.name
        print('[SUMMARY]')
        print "Service base name: " + ssn_conf['service_base_name']
        print "SSN Name: " + ssn_conf['instance_name']
        print "SSN Public IP address: " + instance_hostname
        print "SSN Hostname: " + ssn_conf['instance_dns_name']
        print "Key name: " + os.environ['conf_key_name']
        print "VPC Name: " + ssn_conf['vpc_name']
        print "Subnet Name: " + ssn_conf['subnet_name']
        print "Firewall Names: " + ssn_conf['security_group_name']
        print "SSN instance size: " + os.environ['azure_ssn_instance_size']
        print "SSN storage account name: " + ssn_storage_account_name
        print "SSN container name: " + ssn_conf['ssn_container_name']
        print "Shared storage account name: " + shared_storage_account_name
        print "Shared container name: " + ssn_conf['shared_container_name']
        print "Region: " + ssn_conf['region']
        jenkins_url = "http://{}/jenkins".format(ssn_conf['instance_dns_name'])
        jenkins_url_https = "https://{}/jenkins".format(ssn_conf['instance_dns_name'])
        print "Jenkins URL: " + jenkins_url
        print "Jenkins URL HTTPS: " + jenkins_url_https
        try:
            with open('jenkins_crids.txt') as f:
                print f.read()
        except:
            print "Jenkins is either configured already or have issues in configuration routine."

        with open("/root/result.json", 'w') as f:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "instance_name": ssn_conf['instance_name'],
                   "instance_hostname": ssn_conf['instance_dns_name'],
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": ssn_conf['vpc_name'],
                   "subnet_id": ssn_conf['subnet_name'],
                   "security_id": ssn_conf['security_group_name'],
                   "instance_shape": os.environ['azure_ssn_instance_size'],
                   "container_name": ssn_conf['ssn_container_name'],
                   "region": ssn_conf['region'],
                   "action": "Create SSN instance"}
            f.write(json.dumps(res))

        print 'Upload response file'
        params = "--instance_name {} --local_log_filepath {} --os_user {} --instance_hostname {}".\
            format(ssn_conf['instance_name'], local_log_filepath, ssn_conf['dlab_ssh_user'], instance_hostname)
        local("~/scripts/{}.py {}".format('upload_response_file', params))
    except:
        sys.exit(1)
