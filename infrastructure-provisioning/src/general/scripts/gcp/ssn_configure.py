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

    try:
        logging.info('[DERIVING NAMES]')
        print '[DERIVING NAMES]'
        # service_base_name = os.environ['conf_service_base_name']
        # role_name = service_base_name.lower().replace('-', '_') + '-ssn-Role'
        # role_profile_name = service_base_name.lower().replace('-', '_') + '-ssn-Profile'
        # policy_name = service_base_name.lower().replace('-', '_') + '-ssn-Policy'
        # user_bucket_name = (service_base_name + '-ssn-bucket').lower().replace('_', '-')
        # tag_name = service_base_name + '-Tag'
        # instance_name = service_base_name + '-ssn'
        # region = os.environ['region']
        # ssn_ami_name = os.environ['aws_' + os.environ['conf_os_family'] + '_ami_name']
        # ssn_ami_id = get_ami_id(ssn_ami_name)
        # policy_path = '/root/files/ssn_policy.json'
        # vpc_cidr = '172.31.0.0/16'
        # sg_name = instance_name + '-SG'
        pre_defined_vpc = False
        pre_defined_subnet = False
        pre_defined_firewall = False
        billing_enabled = False
        # dlab_ssh_user = os.environ['conf_os_user']

        ssn_conf = dict()
        ssn_conf['service_base_name'] = os.environ['conf_service_base_name']
        ssn_conf['region'] = os.environ['region']
        ssn_conf['zone'] = os.environ['zone']
        ssn_conf['ssn_bucket_name'] = (ssn_conf['service_base_name'] + '-ssn-bucket').lower().replace('_', '-')
        ssn_conf['instance_name'] = ssn_conf['service_base_name'] + '-ssn'
        ssn_conf['instance_size'] = os.environ['ssn_instance_size']
        ssn_conf['vpc_name'] = ssn_conf['service_base_name'] + '-ssn-vpc'
        ssn_conf['subnet_name'] = ssn_conf['service_base_name'] + '-ssn-subnet'
        ssn_conf['subnet_cidr'] = '10.10.1.0/24'
        ssn_conf['firewall_name'] = ssn_conf['service_base_name'] + '-ssn-firewall'
        ssn_conf['ssh_key_path'] = '/root/keys/' + os.environ['conf_key_name'] + '.pem'
        ssn_conf['dlab_ssh_user'] = os.environ['conf_os_user']

        try:
            if os.environ['gcp_vpc_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_vpc = True
        try:
            if os.environ['gcp_subnet_name'] == '':
                raise KeyError
        except KeyError:
            pre_defined_subnet = True
        try:
            if os.environ['gcp_firewall_rules'] == '':
                raise KeyError
        except KeyError:
            pre_defined_firewall = True

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
    except:
        sys.exit(1)

    instance_hostname = GCPMeta().get_instance_public_ip_by_name(ssn_conf['instance_name'])

    try:
        if os.environ['conf_os_family'] == 'debian':
            initial_user = 'ubuntu'
            sudo_group = 'sudo'
        if os.environ['conf_os_family'] == 'redhat':
            initial_user = 'ec2-user'
            sudo_group = 'wheel'

        logging.info('[CREATING DLAB SSH USER]')
        print('[CREATING DLAB SSH USER]')
        params = "--hostname {} --keyfile {} --initial_user {} --os_user {} --sudo_group {}".format\
            (instance_hostname, ssn_conf['ssh_key_path'], initial_user, ssn_conf['dlab_ssh_user'], sudo_group)

        try:
            local("~/scripts/{}.py {}".format('create_ssh_user', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed creating ssh user 'dlab-user'.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'])
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        print('[INSTALLING PREREQUISITES TO SSN INSTANCE]')
        params = "--hostname {} --keyfile {} --pip_packages 'boto3 argparse fabric awscli pymongo pyyaml google-api-python-client google-cloud-storage pycrypto' --user {} --region {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'], ssn_conf['dlab_ssh_user'], ssn_conf['region'])

        try:
            local("~/scripts/{}.py {}".format('install_prerequisites', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed installing software: pip, packages.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'])
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE]')
        print('[CONFIGURE SSN INSTANCE]')
        additional_config = {"nginx_template_dir": "/root/templates/",
                             "service_base_name": ssn_conf['service_base_name'],
                             "security_group_id": ssn_conf['firewall_name'], "vpc_id": ssn_conf['vpc_name'],
                             "subnet_id": ssn_conf['subnet_name'], "admin_key": os.environ['conf_key_name']}
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_user {} --dlab_path {} --tag_resource_id {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'], ssn_conf['service_base_name'])

        try:
            local("~/scripts/{}.py {}".format('configure_ssn_node', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed configuring ssn.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'])
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
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
                             {"name": "emr", "tag": "latest"}]
        params = "--hostname {} --keyfile {} --additional_config '{}' --os_family {} --os_user {} --dlab_path {} --cloud_provider {} --region {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'], json.dumps(additional_config),
                   os.environ['conf_os_family'], ssn_conf['dlab_ssh_user'], os.environ['ssn_dlab_path'],
                   os.environ['conf_cloud_provider'], ssn_conf['region'])

        try:
            local("~/scripts/{}.py {}".format('configure_docker', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Unable to configure docker.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'])
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[CONFIGURE SSN INSTANCE UI]')
        print('[CONFIGURE SSN INSTANCE UI]')
        params = "--hostname {} --keyfile {} --dlab_path {} --os_user {} --os_family {} --request_id {} --resource {} --region {} --service_base_name {} --security_groups_ids {} --vpc_id {} --subnet_id {} --tag_resource_id {} --cloud_provider {} --account_id {} --billing_bucket {} --report_path '{}' --billing_enabled {}". \
            format(instance_hostname, ssn_conf['ssh_key_path'], os.environ['ssn_dlab_path'], ssn_conf['dlab_ssh_user'],
                   os.environ['conf_os_family'], os.environ['request_id'], os.environ['conf_resource'],
                   ssn_conf['region'], ssn_conf['service_base_name'], ssn_conf['firewall_name'], ssn_conf['vpc_name'],
                   ssn_conf['subnet_name'], os.environ['conf_tag_resource_id'], os.environ['conf_cloud_provider'],
                   os.environ['aws_account_id'], os.environ['aws_billing_bucket'], os.environ['aws_report_path'],
                   billing_enabled)

        try:
            local("~/scripts/{}.py {}".format('configure_ui', params))
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Unable to configure UI.", str(err))
        GCPActions().remove_instance(ssn_conf['instance_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'])
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)

    try:
        logging.info('[SUMMARY]')
        print('[SUMMARY]')
        print "Service base name: " + ssn_conf['service_base_name']
        print "SSN Name: " + ssn_conf['instance_name']
        print "SSN Hostname: " + instance_hostname
        #print "Role name: " + role_name
        #print "Role profile name: " + role_profile_name
        #print "Policy name: " + policy_name
        print "Key name: " + os.environ['conf_key_name']
        print "VPC Name: " + ssn_conf['vpc_name']
        print "Subnet Name: " + ssn_conf['subnet_name']
        print "Firewall Names: " + ssn_conf['firewall_name']
        print "SSN instance size: " + os.environ['ssn_instance_size']
        #print "SSN AMI name: " + ssn_ami_name
        print "SSN bucket name: " + ssn_conf['ssn_bucket_name']
        print "Region: " + ssn_conf['region']
        jenkins_url = "http://{}/jenkins".format(instance_hostname)
        jenkins_url_https = "https://{}/jenkins".format(instance_hostname)
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
                   "instance_hostname": instance_hostname,
                   #"role_name": role_name,
                   #"role_profile_name": role_profile_name,
                   #"policy_name": policy_name,
                   "master_keyname": os.environ['conf_key_name'],
                   "vpc_id": ssn_conf['vpc_name'],
                   "subnet_id": ssn_conf['subnet_name'],
                   "security_id": ssn_conf['firewall_name'],
                   "instance_shape": os.environ['ssn_instance_size'],
                   "bucket_name": ssn_conf['ssn_bucket_name'],
                   "region": ssn_conf['region'],
                   "action": "Create SSN instance"}
            f.write(json.dumps(res))

        print 'Upload response file'
        params = "--instance_name {} --local_log_filepath {} --os_user {}".format(ssn_conf['instance_name'],
                                                                                  local_log_filepath,
                                                                                  ssn_conf['dlab_ssh_user'])
        local("~/scripts/{}.py {}".format('upload_response_file', params))
    except:
        GCPActions().remove_instance(ssn_conf['instance_name'])
        GCPActions().remove_bucket(ssn_conf['ssn_bucket_name'])
        if pre_defined_firewall:
            GCPActions().remove_firewall(ssn_conf['firewall_name'])
        if pre_defined_subnet:
            GCPActions().remove_subnet(ssn_conf['subnet_name'], ssn_conf['region'])
        if pre_defined_vpc:
            GCPActions().remove_vpc(ssn_conf['vpc_name'])
        sys.exit(1)
