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
import itertools
import json
import logging
import os
import re
import shutil
import subprocess
import sys
import time
from abc import abstractmethod
from deploy.endpoint_fab import start_deploy
from fabric import Connection
from patchwork.transfers import rsync

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
logging.basicConfig(level=logging.INFO, format='%(levelname)s-%(message)s')
INITIAL_LOCATION = os.path.dirname(os.path.abspath(__file__))


class TerraformOutputBase:
    @property
    @abstractmethod
    def output_path(self):
        pass

    @abstractmethod
    def write(self, obj):
        pass

    @abstractmethod
    def extract(self):
        pass


class LocalStorageOutputProcessor(TerraformOutputBase):
    output_path = None

    def __init__(self, path):
        self.output_path = path

    def write(self, obj):
        """Write json string to local file
        :param obj: json string
        """
        existed_data = {}
        if os.path.isfile(self.output_path):
            with open(self.output_path, 'r') as fp:
                output = fp.read()
                if len(output):
                    existed_data = json.loads(output)
        existed_data.update(obj)

        with open(self.output_path, 'w') as fp:
            json.dump(existed_data, fp)
        pass

    def extract(self):
        """Extract data from local file
        :return: dict
        """
        if os.path.isfile(self.output_path):
            with open(self.output_path, 'r') as fp:
                output = fp.read()
                if len(output):
                    return json.loads(output)


def extract_args(cli_args):
    args = []
    for key, value in cli_args.items():
        if not value:
            continue
        if type(value) == list:
            quoted_list = ['"{}"'.format(item) for item in value]
            joined_values = ', '.join(quoted_list)
            value = '[{}]'.format(joined_values)
        args.append((key, value))
    return args


def get_var_args_string(cli_args):
    """Convert dict of cli argument into string

    Args:
        cli_args: dict of cli arguments
    Returns:
        str: string of joined key=values
    """
    args = extract_args(cli_args)
    args_hidden = list()
    args_plain = ["-var '{0}={1}'".format(key, value) for key, value in args]
    for key, value in args:
        if key in ["secret_access_key", "access_key_id", "ldap_host", "ldap_user", "ldap_bind_creds", "mongo_password", "mongo_host"]:
            value = '********'
        args_hidden.append("-var '{0}={1}'".format(key, value))
    return [' '.join(args_plain), ' '.join(args_hidden)]


def get_args_string(cli_args):
    """Convert dict of cli argument into string

    Args:
        cli_args: dict of cli arguments
    Returns:
        str: string of joined key=values
    """

    args = extract_args(cli_args)
    args = ["{0} {1}".format(key, value) for key, value in args]
    return ' '.join(args)


class ParamsBuilder:

    def __init__(self):
        self.__params = []

    def add(self, arg_type, name, desc, **kwargs):
        default_group = ['all_args']
        if isinstance(kwargs.get('group'), str):
            default_group.append(kwargs.get('group'))
        if isinstance(kwargs.get('group'), (list, tuple)):
            default_group.extend(kwargs.get('group'))

        parameter = {
            'group': default_group,
            'name': name,
            'props': {
                'help': desc,
                'type': arg_type,
                'default': kwargs.get('default'),
                'choices': kwargs.get('choices'),
                'nargs': kwargs.get('nargs'),
                'action': kwargs.get('action'),
                'required': kwargs.get('required'),
            }
        }
        self.__params.append(parameter)
        return self

    def add_str(self, name, desc, **kwargs):
        return self.add(str, name, desc, **kwargs)

    def add_bool(self, name, desc, **kwargs):
        return self.add(self.str2bool, name, desc, **kwargs)

    def add_int(self, name, desc, **kwargs):
        return self.add(int, name, desc, **kwargs)

    @staticmethod
    def str2bool(v):
        if isinstance(v, bool):
            return v
        if v.lower() in ('yes', 'true', 't', 'y', '1'):
            return True
        elif v.lower() in ('no', 'false', 'f', 'n', '0'):
            return False
        else:
            raise argparse.ArgumentTypeError('Boolean value expected.')

    def build(self):
        return self.__params


class Console:

    @staticmethod
    def execute_to_command_line(command):
        """ Execute cli command

        Args:
            command: str cli command
        Returns:
            str: command result
        """
        # process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE,
        #                            stderr=subprocess.STDOUT,
        #                            universal_newlines=True)
        subprocess.run(command, shell=True, check=True)

        # while True:
        #     nextline = process.stdout.readline()
        #     print(nextline)
        #     if nextline == '' and process.poll() is not None:
        #         break
        #     if 'error' in nextline.lower():
        #         sys.exit(0)

    @staticmethod
    def execute(command):
        """ Execute cli command

        Args:
            command: str cli command
        Returns:
            str: command result
        """
        return os.popen(command).read()

    @staticmethod
    def ssh(ip, name, pkey):
        attempt = 0
        while attempt < 12:
            logging.info('connection attempt {}'.format(attempt))
            connection = Connection(
                host=ip,
                user=name,
                connect_kwargs={'key_filename': pkey,
                                'allow_agent': False,
                                'look_for_keys': False,
                                })
            try:
                connection.run('ls')
                return connection
            except Exception as ex:
                logging.error(ex)
                attempt += 1
                time.sleep(10)


class TerraformProviderError(Exception):
    """
    Raises errors while terraform provision
    """
    pass


class TerraformProvider:

    def __init__(self, no_color=False):
        self.no_color = '-no-color' if no_color else ''

    def initialize(self):
        """Initialize terraform

        Returns:
             bool: init successful
        Raises:
            TerraformProviderError: if initialization was not succeed
        """
        logging.info('terraform init')
        terraform_success_init = 'Terraform has been successfully initialized!'
        command = 'terraform init {}'.format(self.no_color)
        terraform_init_result = Console.execute(command)
        logging.info(terraform_init_result)
        if terraform_success_init not in terraform_init_result:
            raise TerraformProviderError(terraform_init_result)

    def validate(self):
        """Validate terraform

        Returns:
             bool: validation successful
        Raises:
            TerraformProviderError: if validation status was not succeed

        """
        logging.info('terraform validate')
        terraform_success_validate = 'Success!'
        terraform_validate_result = Console.execute(
            'terraform validate {}'.format(self.no_color))
        logging.info(terraform_validate_result)
        if terraform_success_validate not in terraform_validate_result:
            raise TerraformProviderError(terraform_validate_result)

    def apply(self, tf_params, cli_args):
        """Run terraform

        Args:
            tf_params: dict of terraform parameters
            cli_args: dict of parameters
        Returns:
             None
        """
        logging.info('terraform apply')
        args_list = get_var_args_string(cli_args)
        params_str = get_args_string(tf_params)
        command = ('terraform apply -auto-approve {} {}'
                   .format(self.no_color, params_str))
        logging.info('{} {}'.format(command, args_list[1]))
        Console.execute_to_command_line('{} {}'.format(command, args_list[0]))

    def destroy(self, tf_params, cli_args, keep_state_file=False):
        """Destroy terraform

        Args:
            tf_params: dict of terraform parameters
            cli_args: dict of parameters
            keep_state_file: Boolean
        Returns:
             None
        """
        logging.info('terraform destroy')
        args_list = get_var_args_string(cli_args)
        params_str = get_args_string(tf_params)
        command = ('terraform destroy -auto-approve {} {}'
                   .format(self.no_color, params_str))
        logging.info('{} {}'.format(command, args_list[1]))
        Console.execute_to_command_line('{} {}'.format(command, args_list[0]))
        if not keep_state_file:
            state_file = tf_params['-state']
            state_file_backup = tf_params['-state'] + '.backup'
            if os.path.isfile(state_file):
                os.remove(state_file)
            if os.path.isfile(state_file_backup):
                os.remove(state_file_backup)

    @staticmethod
    def output(tf_params, *args):
        """Get terraform output

        Args:
            tf_params: dict of terraform parameters
            *args: list of str parameters
        Returns:
            str: terraform output result
        """
        params = get_args_string(tf_params)
        return Console.execute('terraform output {} {}'
                               .format(params, ' '.join(args)))


class AbstractDeployBuilder:
    def __init__(self):

        args = self.parse_args()
        self.service_args = args.get('service')
        self.no_color = self.service_args.get('no_color')
        state_dir = self.service_args.get('state')
        if not state_dir:
            self.output_dir = None
            self.tf_output = os.path.join(INITIAL_LOCATION, 'output.json')
            self.tf_params = {}
        else:
            if os.path.isdir(state_dir) and os.access(state_dir, os.W_OK):
                service_name = (args.get(self.terraform_args_group_name)
                                .get('service_base_name'))
                self.output_dir = (os.path.join(state_dir, service_name))
                self.tf_output = os.path.join(self.output_dir, 'output.json')
                self.tf_params = {
                    '-state': os.path.join(
                        self.output_dir, '{}.tfstate'.format(self.name))
                }
            else:
                sys.stdout.write('path doesn\'t exist')
                sys.exit(1)
        if self.use_tf_output_file:
            self.fill_sys_argv_from_file()
        self.terraform_args = self.parse_args().get(
            self.terraform_args_group_name)

    @property
    @abstractmethod
    def terraform_location(self):
        """ get Terraform location

        Returns:
            str: TF script location
        """
        raise NotImplementedError

    @property
    @abstractmethod
    def name(self):
        """ get Terraform name

        Returns:
            str: TF name
        """
        raise NotImplementedError

    @property
    @abstractmethod
    def terraform_args_group_name(self):
        """ get Terraform location

        Returns:
            str: TF script location
        """
        raise NotImplementedError

    @property
    @abstractmethod
    def cli_args(self):
        """Get cli arguments

        Returns:
            dict: dictionary of client arguments
                  with name as key and props as value
        """
        raise NotImplementedError

    @abstractmethod
    def deploy(self):
        """Post terraform execution

        Returns:
            None
        """
        raise NotImplementedError

    @property
    def use_tf_output_file(self):
        return False

    def apply(self):
        """Apply terraform"""
        terraform = TerraformProvider(self.no_color)
        terraform.apply(self.tf_params, self.terraform_args)

    def destroy(self):
        """Destory terraform"""
        terraform = TerraformProvider(self.no_color)
        terraform.destroy(self.tf_params, self.terraform_args)

    def store_output_to_file(self):
        """Extract terraform output and store to file"""
        terraform = TerraformProvider(self.no_color)
        output = terraform.output(self.tf_params, '-json')
        output = {key: value.get('value')
                  for key, value in json.loads(output).items()}
        output_writer = LocalStorageOutputProcessor(self.tf_output)
        output_writer.write(output)

    def update_extracted_file_data(self, obj):
        """
        :param obj:
        :return:
        Override method if you need to modify extracted from file data
        """
        pass

    def fill_sys_argv_from_file(self):
        """Extract data from file and fill sys args"""
        output_processor = LocalStorageOutputProcessor(self.tf_output)
        output = output_processor.extract()
        if output:
            self.update_extracted_file_data(output)
            for key, value in output.items():
                key = '--' + key
                if key not in sys.argv:
                    sys.argv.extend([key, value])
                else:
                    try:
                        index = sys.argv.index(key)
                        sys.argv[index + 1] = value
                    except:
                        pass

    def parse_args(self):
        """Get dict of arguments

        Returns:
            dict: CLI arguments
        """
        parsers = {}
        args = []

        for arg in self.cli_args:
            group = arg.get('group')
            if isinstance(group, (list, tuple)):
                for item in group:
                    args.append(dict(arg.copy(), **{'group': item}))
            else:
                args.append(arg)

        cli_args = sorted(args, key=lambda x: x.get('group'))
        args_groups = itertools.groupby(cli_args, lambda x: x.get('group'))
        for group, args in args_groups:
            parser = argparse.ArgumentParser()
            for arg in args:
                parser.add_argument(arg.get('name'), **arg.get('props'))
            parsers[group] = parser
        return {
            group: vars(parser.parse_known_args()[0])
            for group, parser in parsers.items()
        }

    def validate_params(self):
        params = self.parse_args()[self.terraform_args_group_name]
        if len(params.get('service_base_name')) > 20:
            sys.stderr.write('service_base_name length should be less then 20')
            sys.exit(1)
        if not re.match("^[a-z0-9\-]+$", params.get('service_base_name')):
            sys.stderr.write('service_base_name should contain only lowercase '
                             'alphanumetic characters and hyphens')
            sys.exit(1)

    def provision(self):
        """Execute terraform script

        Returns:
            None
        Raises:
            TerraformProviderError: if init or validate fails
        """
        self.validate_params()
        tf_location = self.terraform_location
        terraform = TerraformProvider(self.no_color)
        os.chdir(tf_location)
        try:
            terraform.initialize()
            terraform.validate()
        except TerraformProviderError as ex:
            raise Exception('Error while provisioning {}'.format(ex))


class AWSK8sSourceBuilder(AbstractDeployBuilder):

    def __init__(self):
        super(AWSK8sSourceBuilder, self).__init__()
        self._args = self.parse_args()
        self._ip = None
        self._user_name = self.args.get(self.terraform_args_group_name).get(
            'os_user')
        self._pkey_path = self.args.get('service').get('pkey')

    @property
    def name(self):
        return 'ssn-k8s'

    @property
    def args(self):
        return self._args

    @property
    def ip(self):
        return self._ip

    @ip.setter
    def ip(self, ip):
        self._ip = ip

    @property
    def user_name(self):
        return self._user_name

    @property
    def pkey_path(self):
        return self._pkey_path

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/ssn-k8s/main')

    @property
    def terraform_args_group_name(self):
        return 'k8s'

    def validate_params(self):
        super(AWSK8sSourceBuilder, self).validate_params()
        params = self.parse_args()['all_args']
        if params.get('ssn_k8s_masters_count', 1) < 1:
            sys.stderr.write('ssn_k8s_masters_count should be greater then 0')
            sys.exit(1)
        if params.get('ssn_k8s_workers_count', 3) < 3:
            sys.stderr.write('ssn_k8s_masters_count should be minimum 3')
            sys.exit(1)
        # Temporary condition for Jenkins job
        if 'endpoint_id' in params and len(params.get('endpoint_id')) > 12:
            sys.stderr.write('endpoint_id length should be less then 12')
            sys.exit(1)

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_bool('--no_color', 'no color console_output', group='service',
                   default=False)
         .add_str('--state', 'State file path', group='service')
         .add_str('--access_key_id', 'AWS Access Key ID', required=True,
                  group='k8s')
         .add_str('--allowed_cidrs',
                  'CIDR to allow acces to SSN K8S cluster.',
                  default=["0.0.0.0/0"], action='append', group='k8s')
         .add_str('--ami', 'ID of EC2 AMI.', required=True, group='k8s')
         .add_str('--env_os', 'OS type.', default='debian',
                  choices=['debian', 'redhat'], group=('k8s'))
         .add_str('--key_name', 'Name of EC2 Key pair.', required=True,
                  group='k8s')
         .add_str('--os_user', 'Name of DataLab service user.',
                  default='datalab-user', group='k8s')
         .add_str('--pkey', 'path to key', required=True, group='service')
         .add_str('--region', 'Name of AWS region.', default='us-west-2',
                  group=('k8s'))
         .add_str('--secret_access_key', 'AWS Secret Access Key',
                  required=True,
                  group='k8s')
         .add_str('--service_base_name',
                  'Any infrastructure value (should be unique if '
                  'multiple SSN\'s have been deployed before).',
                  default='k8s', group=('k8s', 'helm_charts'))
         .add_int('--ssn_k8s_masters_count', 'Count of K8S masters.',
                  default=3,
                  group='k8s')
         .add_int('--ssn_k8s_workers_count', 'Count of K8S workers', default=2,
                  group=('k8s', 'helm_charts'))
         .add_str('--ssn_k8s_masters_shape', 'Shape for SSN K8S masters.',
                  default='t2.medium', group=('k8s'))
         .add_str('--ssn_k8s_workers_shape', 'Shape for SSN K8S workers.',
                  default='t2.medium', group='k8s')
         .add_int('--ssn_root_volume_size', 'Size of root volume in GB.',
                  default=30, group='k8s')
         .add_str('--subnet_cidr_a',
                  'CIDR for Subnet creation in zone a. Conflicts with  subnet_id_a.',
                  default='172.31.0.0/24', group='k8s')
         .add_str('--subnet_cidr_b',
                  'CIDR for Subnet creation in zone b. Conflicts with  subnet_id_b.',
                  default='172.31.1.0/24', group='k8s')
         .add_str('--subnet_cidr_c',
                  'CIDR for Subnet creation in zone c. Conflicts with  subnet_id_c.',
                  default='172.31.2.0/24', group='k8s')
         .add_str('--subnet_id_a',
                  'ID of AWS Subnet in zone a if you already have subnet created.',
                  group='k8s')
         .add_str('--subnet_id_b',
                  'ID of AWS Subnet in zone b if you already have subnet created.',
                  group='k8s')
         .add_str('--subnet_id_c',
                  'ID of AWS Subnet in zone c if you already have subnet created.',
                  group='k8s')
         .add_str('--vpc_cidr', 'CIDR for VPC creation. Conflicts with vpc_id',
                  default='172.31.0.0/16', group='k8s')
         .add_str('--vpc_id', 'ID of AWS VPC if you already have VPC created.',
                  group='k8s')
         .add_str('--zone', 'Name of AWS zone', default='a',
                  group=('k8s'))
         .add_str('--ldap_host', 'ldap host', required=True,
                  group='helm_charts')
         .add_str('--ldap_dn', 'ldap dn', required=True,
                  group='helm_charts')
         .add_str('--ldap_user', 'ldap user', required=True,
                  group='helm_charts')
         .add_str('--ldap_bind_creds', 'ldap bind creds', required=True,
                  group='helm_charts')
         .add_str('--ldap_users_group', 'ldap users group', required=True,
                  group='helm_charts')
         .add_str('--tag_resource_id', 'Tag resource ID.',
                  default='user:tag', group=('k8s', 'helm_charts'))
         .add_str('--additional_tag', 'Additional tag.',
                  default='product:datalab', group='k8s')
         .add_str('--billing_bucket', 'Billing bucket name',
                  group='helm_charts')
         .add_str('--billing_bucket_path',
                  'The path to billing reports directory in S3 bucket',
                  default='',
                  group='helm_charts')
         .add_str('--billing_aws_job_enabled',
                  'Billing format. Available options: true (aws), false(epam)',
                  default='false',
                  group='helm_charts')
         .add_str('--billing_aws_account_id',
                  'The ID of Amazon account', default='',
                  group='helm_charts')
         .add_str('--billing_datalab_id',
                  'Column name in report file that contains datalab id tag',
                  default='resource_tags_user_user_tag',
                  group='helm_charts')
         .add_str('--billing_usage_date',
                  'Column name in report file that contains usage date tag',
                  default='line_item_usage_start_date',
                  group='helm_charts')
         .add_str('--billing_product',
                  'Column name in report file that contains product name tag',
                  default='product_product_name',
                  group='helm_charts')
         .add_str('--billing_usage_type',
                  'Column name in report file that contains usage type tag',
                  default='line_item_usage_type',
                  group='helm_charts')
         .add_str('--billing_usage',
                  'Column name in report file that contains usage tag',
                  default='line_item_usage_amount',
                  group='helm_charts')
         .add_str('--billing_cost',
                  'Column name in report file that contains cost tag',
                  default='line_item_blended_cost',
                  group='helm_charts')
         .add_str('--billing_resource_id',
                  'Column name in report file that contains datalab resource id tag',
                  default='line_item_resource_id',
                  group='helm_charts')
         .add_str('--billing_tags',
                  'Column name in report file that contains tags',
                  default='line_item_operation,line_item_line_item_description',
                  group='helm_charts')
         .add_str('--billing_tag', 'Billing tag', default='datalab',
                  group='helm_charts')
         .add_bool('--custom_certs_enabled', 'Enable custom certificates',
                   default=False, group=('service', 'helm_charts'))
         .add_str('--custom_cert_path', 'custom_cert_path', default='', group=('service', 'helm_charts'))
         .add_str('--custom_key_path', 'custom_key_path', default='', group=('service', 'helm_charts'))
         .add_str('--custom_certs_host', 'custom certs host', default='', group='helm_charts')
         # Tmp for jenkins job
         .add_str('--endpoint_id', 'Endpoint Id',
                  default='user:tag', group=())
         )
        return params.build()

    def check_k8s_cluster_status(self):
        """ Check for kubernetes status

        Returns:
            None
        Raises:
            TerraformProviderError: if master or kubeDNS is not running

        """
        start_time = time.time()
        while True:
            with Console.ssh(self.ip, self.user_name, self.pkey_path) as c:
                k8c_info_status = c.run(
                    'kubectl cluster-info | '
                    'sed -r "s/\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[mGK]//g"') \
                    .stdout

            kubernetes_success_status = 'Kubernetes master is running'
            kubernetes_dns_success_status = 'KubeDNS is running'

            kubernetes_succeed = kubernetes_success_status in k8c_info_status
            kube_dns_succeed = kubernetes_dns_success_status in k8c_info_status

            if kubernetes_succeed and kube_dns_succeed:
                break
            if (time.time() - start_time) >= 600:
                raise TimeoutError
            time.sleep(60)

    def check_tiller_status(self):
        """ Check tiller status

        Returns:
            None
        Raises:
            TerraformProviderError: if tiller is not running

        """
        start_time = time.time()

        with Console.ssh(self.ip, self.user_name, self.pkey_path) as c:
            while True:
                tiller_status = c.run(
                    "kubectl get pods --all-namespaces "
                    "| grep tiller | awk '{print $4}'").stdout
                tiller_success_status = 'Running'
                if tiller_success_status in tiller_status:
                    break
                if (time.time() - start_time) >= 1200:
                    raise TimeoutError
                time.sleep(60)

    def select_master_ip(self):
        terraform = TerraformProvider(self.no_color)
        output = terraform.output(self.tf_params,
                                  '-json ssn_k8s_masters_ip_addresses')
        ips = json.loads(output)
        if not ips:
            raise TerraformProviderError('no ips')
        self.ip = ips[0]

    def copy_terraform_to_remote(self):
        logging.info('transfer terraform dir to remote')
        tf_dir = os.path.abspath(
            os.path.join(os.getcwd(), os.path.pardir, os.path.pardir))
        source = os.path.join(tf_dir, 'ssn-helm-charts')
        remote_dir = '/home/{}/terraform/'.format(self.user_name)
        with Console.ssh(self.ip, self.user_name, self.pkey_path) as conn:
            conn.run('mkdir -p {}'.format(remote_dir))
            rsync(conn, source, remote_dir, strict_host_keys=False)

    def copy_cert(self):
        logging.info('transfer certificates to remote')
        cert_path = self.service_args.get('custom_cert_path')
        key_path = self.service_args.get('custom_key_path')
        remote_dir = '/tmp/'  # .format(self.user_name)
        with Console.ssh(self.ip, self.user_name, self.pkey_path) as conn:
            conn.run('mkdir -p {}'.format(remote_dir))
            rsync(conn, cert_path, remote_dir, strict_host_keys=False)
            rsync(conn, key_path, remote_dir, strict_host_keys=False)

    def run_remote_terraform(self):
        logging.info('apply helm charts')
        args = self.parse_args()
        # dns_name = json.loads(TerraformProvider(self.no_color)
        #                       .output(self.tf_params,
        #                               '-json ssn_k8s_alb_dns_name'))
        nlb_dns_name = json.loads(TerraformProvider(self.no_color)
                                  .output(self.tf_params,
                                          '-json ssn_k8s_nlb_dns_name'))
        logging.info('apply ssn-helm-charts')
        terraform_args = args.get('helm_charts')
        args_str = get_var_args_string(terraform_args)
        with Console.ssh(self.ip, self.user_name, self.pkey_path) as conn:
            with conn.cd('terraform/ssn-helm-charts/main'):
                init = conn.run('terraform init').stdout.lower()
                validate = conn.run('terraform validate').stdout.lower()
                if 'success' not in init or 'success' not in validate:
                    raise TerraformProviderError
                command = ('terraform apply -auto-approve '
                           '-var \'ssn_k8s_nlb_dns_name={}\''
                           .format(nlb_dns_name))
                logging.info('{} {}'.format(command, args_str[1]))
                conn.run('{} {}'.format(command, args_str[0]))
                output = ' '.join(conn.run('terraform output -json')
                                  .stdout.split())
                self.fill_args_from_dict(json.loads(output))

    def output_terraform_result(self):
        # dns_name = json.loads(
        #     TerraformProvider(self.no_color).output(self.tf_params,
        #                                             '-json nginx_load_balancer_hostname'))
        ssn_k8s_sg_id = json.loads(
            TerraformProvider(self.no_color).output(self.tf_params,
                                                    '-json ssn_k8s_sg_id'))
        ssn_subnet = json.loads(
            TerraformProvider(self.no_color).output(self.tf_params,
                                                    '-json ssn_subnet_id'))
        ssn_vpc_id = json.loads(
            TerraformProvider(self.no_color).output(self.tf_params,
                                                    '-json ssn_vpc_id'))

        logging.info("""
        DataLab SSN K8S cluster has been deployed successfully!
        Summary:
        VPC ID: {}
        Subnet ID:  {}
        SG IDs: {}
        """.format(ssn_vpc_id, ssn_subnet, ssn_k8s_sg_id))

    def fill_args_from_dict(self, output):
        for key, value in output.items():
            value = value.get('value')
            sys.argv.extend(['--' + key, value])

    def fill_remote_terraform_output(self):
        with Console.ssh(self.ip, self.user_name, self.pkey_path) as conn:
            with conn.cd('terraform/ssn-helm-charts/main'):
                output = ' '.join(conn.run('terraform output -json')
                                  .stdout.split())
                self.fill_args_from_dict(json.loads(output))
                output_processor = LocalStorageOutputProcessor(self.tf_output)
                output = {key: value.get('value')
                          for key, value in json.loads(output).items()}
                output_processor.write(output)

    @staticmethod
    def add_ip_to_known_hosts(ip):
        attempt = 0
        while attempt < 10:
            if len(Console.execute('ssh-keygen -H -F {}'.format(ip))) == 0:
                Console.execute(
                    'ssh-keyscan {} >> ~/.ssh/known_hosts'.format(ip))
                attempt += 1
            else:
                break

    def destroy_remote_terraform(self):
        logging.info('destroy helm charts')
        with Console.ssh(self.ip, self.user_name, self.pkey_path) as conn:
            with conn.cd('terraform/ssn-helm-charts/main'):
                init = conn.run('terraform init').stdout.lower()
                validate = conn.run('terraform validate').stdout.lower()
                if 'success' not in init or 'success' not in validate:
                    raise TerraformProviderError
                command = 'terraform destroy -auto-approve'
                logging.info(command)
                conn.run(command)

    def deploy(self):
        logging.info('deploy')
        output = ' '.join(
            TerraformProvider(self.no_color).output(self.tf_params,
                                                    '-json').split())
        self.fill_args_from_dict(json.loads(output))
        self.select_master_ip()
        self.add_ip_to_known_hosts(self.ip)
        self.check_k8s_cluster_status()
        self.check_tiller_status()
        self.copy_terraform_to_remote()
        if self.service_args.get('custom_certs_enabled'):
            self.copy_cert()
        self.run_remote_terraform()
        self.fill_remote_terraform_output()
        self.output_terraform_result()

    def destroy(self):
        self.select_master_ip()
        try:
            self.destroy_remote_terraform()
        except:
            print("Error with destroying helm charts.")
        super(AWSK8sSourceBuilder, self).destroy()
        if self.output_dir is not None:
            shutil.rmtree(self.output_dir)
        elif os.path.isfile(os.path.join(INITIAL_LOCATION, 'output.json')):
            os.remove(os.path.join(INITIAL_LOCATION, 'output.json'))


class AWSEndpointBuilder(AbstractDeployBuilder):

    def update_extracted_file_data(self, obj):
        if 'ssn_vpc_id' in obj:
            obj['vpc_id'] = obj['ssn_vpc_id']
        if 'ssn_subnet_id' in obj:
            obj['subnet_id'] = obj['ssn_subnet_id']

    @property
    def name(self):
        return 'endpoint'

    @property
    def use_tf_output_file(self):
        return True

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/endpoint/main')

    @property
    def terraform_args_group_name(self):
        return 'endpoint'

    def validate_params(self):
        super(AWSEndpointBuilder, self).validate_params()
        params = self.parse_args()[self.terraform_args_group_name]
        if len(params.get('endpoint_id')) > 12:
            sys.stderr.write('endpoint_id length should be less then 12')
            sys.exit(1)

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_bool('--no_color', 'no color console_output', group='service',
                   default=False)
         .add_str('--state', 'State file path', group='service')
         .add_str('--secret_access_key', 'AWS Secret Access Key',
                  required=True,
                  group='endpoint')
         .add_str('--access_key_id', 'AWS Access Key ID', required=True,
                  group='endpoint')
         .add_str('--pkey', 'path to key', required=True, group='service')
         .add_str('--service_base_name',
                  'Any infrastructure value (should be unique if  multiple '
                  'SSN\'s have been deployed before). Should be  same as on ssn',
                  group='endpoint')
         .add_str('--vpc_id', 'ID of AWS VPC if you already have VPC created.',
                  group='endpoint')
         .add_str('--vpc_cidr',
                  'CIDR for VPC creation. Conflicts with vpc_id.',
                  default='172.31.0.0/16', group='endpoint')
         .add_str('--subnet_id',
                  'ID of Subnet if you already have subnet created.',
                  group='endpoint')
         .add_str('--ssn_k8s_sg_id', 'ID of SSN SG.', group='endpoint')
         .add_str('--subnet_cidr',
                  'CIDR for Subnet creation. Conflicts with subnet_id.',
                  default='172.31.0.0/24', group='endpoint')
         .add_str('--ami', 'ID of AMI.', group='endpoint')
         .add_str('--key_name', 'Name of EC2 Key pair.', required=True,
                  group='endpoint')
         .add_str('--endpoint_id', 'Endpoint id.', required=True,
                  group='endpoint')
         .add_str('--region', 'Name of AWS region.', default='us-west-2',
                  group='endpoint')
         .add_str('--zone', 'Name of AWS zone.', default='a', group='endpoint')
         .add_str('--network_type',
                  'Type of created network (if network is not existed and '
                  'require creation) for endpoint',
                  default='public', group='endpoint')
         .add_str('--endpoint_instance_shape', 'Instance shape of Endpoint.',
                  default='t2.medium', group='endpoint')
         .add_int('--endpoint_volume_size', 'Size of root volume in GB.',
                  default=30, group='endpoint')
         .add_str('--product', 'Product name.', default='datalab',
                  group='endpoint')
         .add_str('--additional_tag', 'Additional tag.',
                  default='product:datalab', group='endpoint')
         .add_str('--ldap_host', 'ldap host', required=True,
                  group='endpoint')
         .add_str('--ldap_dn', 'ldap dn', required=True,
                  group='endpoint')
         .add_str('--ldap_user', 'ldap user', required=True,
                  group='endpoint')
         .add_str('--ldap_bind_creds', 'ldap bind creds', required=True,
                  group='endpoint')
         .add_str('--ldap_users_group', 'ldap users group', required=True,
                  group='endpoint')
         .add_bool('--billing_enable', 'Billing enable', group='endpoint', default=False)
         .add_str('--mongo_password', 'Mongo database password', group='endpoint')
         .add_str('--mongo_host', 'Mongo database host', group='endpoint', default='localhost')
         .add_str('--billing_bucket', 'Billing bucket name', group='endpoint', default='')
         .add_str('--report_path', 'The path to report folder', group='endpoint', default='')
         .add_str('--aws_job_enabled', 'Billing format. Available options: true (aws), false(epam)', group='endpoint',
                  default='false')
         .add_str('--billing_aws_account_id', 'The ID of ASW linked account', group='endpoint', default='')
         .add_str('--billing_tag', 'Billing tag', group='endpoint', default='datalab')
         .add_str('--allowed_ip_cidrs', 'Allowed IP CIDRs for SGs', group='endpoint', default='["0.0.0.0/0"]')
         .add_str('--sg_id', 'SG ID to be added to instance', group='endpoint', default="")
         )
        return params.build()

    def deploy(self):
        self.fill_sys_argv_from_file()
        new_dir = os.path.abspath(
            os.path.join(os.getcwd(), '../../../bin/deploy'))
        os.chdir(new_dir)
        start_deploy()


class GCPK8sSourceBuilder(AbstractDeployBuilder):

    # def update_extracted_file_data(self, obj):
    #     if 'ssn_vpc_id' in obj:
    #         obj['vpc_id'] = obj['ssn_vpc_id']

    @property
    def name(self):
        return 'k8s'

    @property
    def use_tf_output_file(self):
        return True

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'gcp/ssn-gke/main')

    @property
    def terraform_args_group_name(self):
        return 'k8s'

    def validate_params(self):
        super(GCPK8sSourceBuilder, self).validate_params()
        # params = self.parse_args()[self.terraform_args_group_name]
        # if len(params.get('endpoint_id')) > 12:
        #     sys.stderr.write('endpoint_id length should be less then 12')
        #     sys.exit(1)

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_bool('--no_color', 'no color console_output', group='service',
                   default=False)
         .add_str('--state', 'State file path', group='service')
         .add_str('--namespace', 'Name of namespace', group='k8s')
         .add_str('--credentials_file_path', 'Path to creds file', group='k8s', required=True)
         .add_str('--project_id', 'Project ID', group='k8s', required=True)
         .add_str('--region', 'Region name', group='k8s', required=True)
         .add_str('--zone', 'Zone name', group='k8s', required=True)
         .add_str('--vpc_name', 'VPC name', group='k8s')
         .add_str('--subnet_name', 'Subnet name', group='k8s')
         .add_str('--service_base_name', 'Service base name', group='k8s', required=True)
         .add_str('--subnet_cidr', 'Subnet CIDR', group='k8s')
         .add_str('--additional_tag', 'Additional tag', group='k8s')
         .add_str('--ssn_k8s_workers_count', 'Number of workers per zone', group='k8s')
         .add_str('--gke_cluster_version', 'GKE version', group='k8s')
         .add_str('--ssn_k8s_workers_shape', 'Workers shape', group='k8s')
         .add_str('--service_account_iam_roles', 'Array of roles', group='k8s')
         .add_str('--ssn_k8s_alb_dns_name', 'DNS name', group='k8s')
         .add_str('--keycloak_user', 'Keycloak user name', group='k8s')
         .add_str('--mysql_user', 'MySQL user name', group='k8s')
         .add_str('--mysql_db_name', 'MySQL database name', group='k8s')
         .add_str('--ldap_usernameAttr', 'LDAP username attr', group='k8s', default='uid')
         .add_str('--ldap_rdnAttr', 'LDAP rdn attr', group='k8s', default='uid')
         .add_str('--ldap_uuidAttr', 'LDAP uuid attr', group='k8s', default='uid')
         .add_str('--ldap_users_group', 'LDAP users group', group='k8s', default='ou=People')
         .add_str('--ldap_dn', 'LDAP DN', group='k8s', default='dc=example,dc=com')
         .add_str('--ldap_user', 'LDAP user', group='k8s', default='cn=admin')
         .add_str('--ldap_bind_creds', 'LDAP user password', group='k8s', required=True)
         .add_str('--ldap_host', 'LDAP host', group='k8s', required=True)
         .add_str('--mongo_db_username', 'Mongo user name', group='k8s')
         .add_str('--mongo_dbname', 'Mongo database name', group='k8s')
         .add_str('--mongo_image_tag', 'Mongo image tag', group='k8s')
         .add_str('--mongo_service_port', 'Mongo service port', group='k8s')
         .add_str('--mongo_node_port', 'Mongo node port', group='k8s')
         .add_str('--mongo_service_name', 'Mongo service name', group='k8s')
         .add_str('--env_os', 'Environment Operating system', group='k8s', default='debian')
         .add_str('--big_query_dataset', 'Big query dataset name for billing', group='k8s', default='test')
         .add_str('--custom_certs_enabled', 'If custom certs enabled', group='k8s')
         .add_str('--custom_cert_path', 'Custom cert path', group='k8s')
         .add_str('--custom_key_path', 'Custom key path', group='k8s')
         .add_str('--custom_certs_host', 'Custom cert host ', group='k8s')
         .add_str('--mysql_disk_size', 'MySQL disk size', group='k8s')
         .add_str('--domain', 'Domain name', group='k8s', required=True)
         )
        return params.build()

    def apply(self):
        terraform = TerraformProvider(self.no_color)
        gke_params = self.tf_params.copy()
        helm_charts_params = self.tf_params.copy()

        gke_params['-target'] = 'module.gke_cluster'
        helm_charts_params['-target'] = 'module.helm_charts'

        terraform.apply(gke_params, self.terraform_args)
        terraform.apply(helm_charts_params, self.terraform_args)

    def deploy(self):
        pass

    def destroy(self):
        terraform = TerraformProvider(self.no_color)
        gke_params = self.tf_params.copy()
        helm_charts_params = self.tf_params.copy()

        gke_params['-target'] = 'module.gke_cluster'
        helm_charts_params['-target'] = 'module.helm_charts'

        terraform.destroy(helm_charts_params, self.terraform_args, True)
        time.sleep(60)
        terraform.destroy(gke_params, self.terraform_args)


class GCPEndpointBuilder(AbstractDeployBuilder):

    def update_extracted_file_data(self, obj):
        if 'ssn_vpc_id' in obj:
            obj['vpc_id'] = obj['ssn_vpc_id']

    @property
    def name(self):
        return 'endpoint'

    @property
    def use_tf_output_file(self):
        return True

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'gcp/endpoint/main')

    @property
    def terraform_args_group_name(self):
        return 'endpoint'

    def validate_params(self):
        super(GCPEndpointBuilder, self).validate_params()
        params = self.parse_args()[self.terraform_args_group_name]
        if len(params.get('endpoint_id')) > 12:
            sys.stderr.write('endpoint_id length should be less then 12')
            sys.exit(1)

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_bool('--no_color', 'no color console_output', group='service',
                   default=False)
         .add_str('--state', 'State file path', group='service')
         .add_str('--gcp_project_id', 'GCP project ID', required=True, group='endpoint')
         .add_str('--creds_file', 'Path to crdes file', required=True, group='endpoint')
         .add_str('--pkey', 'path to key', required=True, group='service')
         .add_str('--service_base_name', 'Service base name', group='endpoint')
         .add_str('--vpc_id', 'ID of VPC if you already have VPC created.', group='endpoint')
         .add_str('--subnet_cidr', 'CIDR for Subnet creation. Conflicts with vpc_id.', default='172.31.0.0/24',
                  group='endpoint')
         .add_str('--ssn_subnet', 'ID of AWS Subnet if you already have subnet created.', group='endpoint')
         .add_str('--subnet_id', 'ID of subnet', group='endpoint')
         .add_str('--ami', 'ID of EC2 AMI.', group='endpoint')
         .add_str('--path_to_pub_key', 'Path to public key', required=True, group='endpoint')
         .add_str('--endpoint_id', 'Endpoint id.', required=True, group='endpoint')
         .add_str('--region', 'Name of region.', group='endpoint')
         .add_str('--zone', 'Name of zone.', group='endpoint')
         .add_str('--endpoint_shape', 'Instance shape of Endpoint.', group='endpoint')
         .add_str('--endpoint_volume_size', 'Endpoint disk size', group='endpoint')
         .add_str('--additional_tag', 'Additional tag.', default='product:datalab', group='endpoint')
         .add_str('--ldap_host', 'ldap host', required=True, group='endpoint')
         .add_str('--ldap_dn', 'ldap dn', required=True, group='endpoint')
         .add_str('--ldap_user', 'ldap user', required=True, group='endpoint')
         .add_str('--ldap_bind_creds', 'ldap bind creds', required=True, group='endpoint')
         .add_str('--ldap_users_group', 'ldap users group', required=True, group='endpoint')
         .add_str('--firewall_ing_cidr_range', 'Ingress range', group='endpoint', default = '["0.0.0.0/0"]')
         .add_str('--firewall_eg_cidr_range', 'Egress range', group='endpoint', default = '["0.0.0.0/0"]')
         .add_str('--endpoint_policies', 'Endpoint policies list', group='endpoint')
         .add_str('--endpoint_roles', 'Endpoint roles list', group='endpoint')
         .add_str('--bucket_region', 'Bucket region', group='endpoint')
         .add_bool('--billing_enable', 'Billing enable', group='endpoint', default=False)
         .add_str('--billing_dataset_name', 'Billing dataset name', group='endpoint')
         .add_str('--mongo_password', 'Mongo database password', group='endpoint')
         .add_str('--mongo_host', 'Mongo database host', group='endpoint', default='localhost')
         )
        return params.build()

    def deploy(self):
        self.fill_sys_argv_from_file()
        new_dir = os.path.abspath(
            os.path.join(os.getcwd(), '../../../bin/deploy'))
        os.chdir(new_dir)
        start_deploy()


class AzureEndpointBuilder(AbstractDeployBuilder):

    def update_extracted_file_data(self, obj):
        if 'ssn_vpc_id' in obj:
            obj['vpc_id'] = obj['ssn_vpc_id']

    @property
    def name(self):
        return 'endpoint'

    @property
    def use_tf_output_file(self):
        return True

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'azure/endpoint/main')

    @property
    def terraform_args_group_name(self):
        return 'endpoint'

    def validate_params(self):
        super(AzureEndpointBuilder, self).validate_params()
        params = self.parse_args()[self.terraform_args_group_name]
        if len(params.get('endpoint_id')) > 12:
            sys.stderr.write('endpoint_id length should be less then 12')
            sys.exit(1)

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_bool('--no_color', 'no color console_output', group='service',
                   default=False)
         .add_str('--state', 'State file path', group='service')
         .add_str('--auth_file_path', 'Path to crdes file', required=True, group='endpoint')
         .add_str('--pkey', 'path to key', required=True, group='service')
         .add_str('--service_base_name', 'Service base name', group='endpoint')
         .add_str('--resource_group_name', 'Resource group name', group='endpoint')
         .add_str('--vpc_id', 'ID of VPC if you already have VPC created.', group='endpoint')
         .add_str('--vpc_cidr', 'CIDR for VPC creation. Conflicts with vpc_id.', default='172.31.0.0/16',
                  group='endpoint')
         .add_str('--subnet_cidr', 'CIDR for Subnet creation. Conflicts with vpc_id.', default='172.31.0.0/24',
                  group='endpoint')
         .add_str('--ssn_subnet', 'ID of AWS Subnet if you already have subnet created.', group='endpoint')
         .add_str('--subnet_id', 'ID of subnet', group='endpoint')
         .add_str('--ami', 'ID of EC2 AMI.', group='endpoint')
         .add_str('--key_path', 'Path to public key', required=True, group='endpoint')
         .add_str('--endpoint_id', 'Endpoint id.', required=True, group='endpoint')
         .add_str('--region', 'Name of region.', group='endpoint')
         .add_str('--endpoint_shape', 'Instance shape of Endpoint.', default='Standard_DS2_v2', group='endpoint')
         .add_str('--endpoint_volume_size', 'Endpoint disk size', default='30', group='endpoint')
         .add_str('--additional_tag', 'Additional tag.', default='product:datalab', group='endpoint')
         .add_str('--tenant_id', 'Azure tenant ID', group='endpoint', default='')
         .add_str('--subscription_id', 'Azure subscription ID', group='endpoint', default='')
         .add_str('--offer_number', 'Azure offer number', group='endpoint', default='')
         .add_str('--currency', 'Azure currency for billing', group='endpoint', default='')
         .add_str('--locale', 'Azure locale', group='endpoint', default='')
         .add_str('--region_info', 'Azure region info', group='endpoint', default='')
         .add_str('--mongo_password', 'Mongo database password', group='endpoint')
         .add_str('--mongo_host', 'Mongo database host', group='endpoint', default='localhost')
         .add_str('--allowed_ip_cidrs', 'Allowed IP CIDRs for SGs', group='endpoint', default='["0.0.0.0/0"]')
         .add_bool('--billing_enable', 'Billing enable', group='endpoint', default=False)
         )
        return params.build()

    def deploy(self):
        self.fill_sys_argv_from_file()
        new_dir = os.path.abspath(
            os.path.join(os.getcwd(), '../../../bin/deploy'))
        os.chdir(new_dir)
        start_deploy()


class DeployDirector:

    def build(self, action, builder):
        """ Do build action
        Args:
            builder: AbstractDeployBuilder
        Returns:
            None
        """
        try:
            builder.provision()
            if action == 'deploy':
                builder.apply()
                builder.store_output_to_file()
                builder.deploy()
            if action == 'destroy':
                builder.destroy()

        except Exception as ex:
            print(ex)


def deploy():
    actions = {'deploy', 'destroy'}

    sources_targets = {
        'aws': ['k8s', 'endpoint'],
        'gcp': ['k8s', 'endpoint'],
        'azure': ['endpoint']
    }

    no_args_error = ('usage: ./datalab {} {} {}\n'.format(
        actions,
        set(sources_targets.keys()),
        set(itertools.chain(*sources_targets.values()))))
    no_source_error = (
        lambda x: ('usage: ./datalab {} {} {}\n'.format(
            x,
            set(sources_targets.keys()),
            set(itertools.chain(*sources_targets.values())))))
    no_target_error = (
        lambda x, y: ('usage: ./datalab {} {} {}\n'.format(
            x, y, set(itertools.chain(*sources_targets.values())))))

    if len(sys.argv) == 1 or sys.argv[1] not in actions:
        sys.stderr.write(no_args_error)
        exit(1)
    if len(sys.argv) == 2 or sys.argv[2] not in sources_targets:
        sys.stderr.write(no_source_error(sys.argv[1]))
        exit(1)
    if len(sys.argv) == 3 or sys.argv[3] not in sources_targets[sys.argv[2]]:
        sys.stderr.write(no_target_error(sys.argv[1], sys.argv[2]))

    module, action, source, target = sys.argv[:4]
    builders_dict = {
        'aws': {
            'k8s': AWSK8sSourceBuilder,
            'endpoint': AWSEndpointBuilder
        },
        'gcp': {
            'k8s': GCPK8sSourceBuilder,
            'endpoint': GCPEndpointBuilder
        },
        'azure': {
            'endpoint': AzureEndpointBuilder
        }
    }
    builder = builders_dict[source][target]()
    deploy_director = DeployDirector()
    deploy_director.build(action, builder)


if __name__ == '__main__':
    deploy()
