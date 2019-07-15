#!/usr/bin/env python
import json
import os
import abc
import argparse

import time
from fabric.api import *
from fabric.contrib.files import exists


class TerraformProviderError(Exception):
    """
    Raises errors while terraform provision
    """
    pass


class Console:
    @staticmethod
    def execute(command):
        """ Execute cli command

        Args:
            command: str cli command
        Returns:
            str: command result
        """
        return os.popen(command).read()

    def connect_to_ssh(self, ip, name, pkey):
        env.hosts = [ip]
        env.user = name
        env.key_filename = pkey


class TerraformProvider:
    def initialize(self):
        """Initialize terraform

        Returns:
             bool: init successful
        Raises:
            TerraformProviderError: if initialization was not succeed
        """
        terraform_success_init = 'Terraform has been successfully initialized!'
        terraform_init_result = Console.execute('terraform init')
        if terraform_success_init not in terraform_init_result:
            raise TerraformProviderError(terraform_init_result)

    def validate(self):
        """Validate terraform

        Returns:
             bool: validation successful
        Raises:
            TerraformProviderError: if validation status was not succeed

        """
        terraform_success_validate = 'Success!'
        terraform_validate_result = Console.execute('terraform validate')
        if terraform_success_validate not in terraform_validate_result:
            raise TerraformProviderError(terraform_validate_result)

    def apply(self, cli_args):
        """Run terraform

        Args:
            target: str
            cli_args: dict of parameters
        Returns:
             None
        """
        args_str = self.get_args_string(cli_args)
        command = 'terraform apply -auto-approve {}'
        Console.execute(command.format(args_str))

    def destroy(self, cli_args):
        """Destroy terraform

        Args:
            target: str
            cli_args: dict of parameters
        Returns:
             None
        """
        args_str = self.get_args_string(cli_args)
        command = 'terraform destroy -auto-approve {}'
        Console.execute(command.format(args_str))

    def output(self, *args):
        """Get terraform output

        Args:
            *args: list of str parameters
        Returns:
            str: terraform output result
        """
        return Console.execute('terraform output {}'.format(' '.join(args)))

    @staticmethod
    def get_args_string(cli_args):
        """Convert dict of cli argument into string

        Args:
            cli_args: dict of cli arguments
        Returns:
            str: string of joined key=values
        """
        args = []
        for key, value in cli_args.items():
            if not value:
                continue
            if type(value) == list:
                quoted_list = ['"{}"'.format(item) for item in value]
                joined_values = ', '.join(quoted_list)
                value = '[{}]'.format(joined_values)
            args.append("-var '{0}={1}'".format(key, value))
        return ' '.join(args)


class AbstractDeployBuilder:

    @property
    @abc.abstractmethod
    def terraform_location(self):
        """ get Terraform location

        Returns:
            str: TF script location
        """
        raise NotImplementedError

    @property
    @abc.abstractmethod
    def cli_args(self):
        """Get cli arguments

        Returns:
            dict: dictionary of client arguments
                  with name as key and props as value
        """
        raise NotImplementedError

    @abc.abstractmethod
    def deploy(self):
        """Post terraform execution

        Returns:
            None
        """
        raise NotImplementedError

    def parse_args(self):
        """Get dict of arguments

        Returns:
            dict: CLI arguments
        """
        terraform_args_parser = argparse.ArgumentParser()
        client_args_parser = argparse.ArgumentParser()
        for argument in self.cli_args:
            parser = (terraform_args_parser
                      if argument.get('is_terraform_param')
                      else client_args_parser)
            parser.add_argument(argument.get('name'), **argument.get('props'))

        return {
            'terraform_args': vars(terraform_args_parser.parse_known_args()[0]),
            'service_args': vars(client_args_parser.parse_known_args()[0]),
        }

    def provision(self):
        """Execute terraform script

        Returns:
            None
        Raises:
            TerraformProviderError: if init or validate fails
        """
        tf_location = self.terraform_location
        cli_args = self.parse_args()
        action = cli_args.get('service_args').get('action')
        terraform_args = cli_args.get('terraform_args')
        terraform = TerraformProvider()

        os.chdir(tf_location)
        try:
            terraform.initialize()
            terraform.validate()
            if action == 'deploy':
                terraform.apply(terraform_args)
            elif action == 'destroy':
                terraform.destroy(terraform_args)
        except TerraformProviderError as ex:
            raise Exception('Error while provisioning {}'.format(ex))

    def get_node_ip(self, output):
        """Extract ip

        Args:
            output: str of terraform output
        Returns:
            str: extracted ip

        """

        ips = json.loads(output)
        if not ips:
            raise TerraformProviderError('no ips')
        return ips[0]


class DeployDirector:

    def build(self, builder):
        """ Do build action

        Args:
            builder: AbstractDeployBuilder
        Returns:
            None
        """
        try:
            builder.provision()
            builder.deploy()
        except Exception as ex:
            print(ex)

    def get_status(self):
        """ Get execution status

        Returns:
            int: Execution error status (0 if success)
        """

        return 0


class ParamsBuilder:

    def __init__(self):
        self.__params = []

    def add(self, arg_type, name, desc, **kwargs):
        parameter = {
            'is_terraform_param': kwargs.get('is_terraform_param', True),
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

    def add_int(self, name, desc, **kwargs):
        return self.add(int, name, desc, **kwargs)

    def build(self):
        return self.__params


class AWSK8sSourceBuilder(AbstractDeployBuilder):

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/ssn-k8s/main')

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_str('--action', 'Action', default='deploy',
                  is_terraform_param=False)
         .add_str('--access_key_id', 'AWS Access Key ID', required=True)
         .add_str('--allowed_cidrs',
                  'CIDR to allow acces to SSN K8S cluster.',
                  default=["0.0.0.0/0"], action='append')
         .add_str('--ami', 'ID of EC2 AMI.', required=True)
         .add_str('--env_os', 'OS type.', default='debian',
                  choices=['debian', 'redhat'])
         .add_str('--key_name', 'Name of EC2 Key pair.', required=True)
         .add_str('--os_user', 'Name of DLab service user.',
                  default='dlab-user')
         .add_str('--pkey', 'path to key',
                  is_terraform_param=False, required=True)
         .add_str('--region', 'Name of AWS region.', default='us-west-2')
         .add_str('--secret_access_key', 'AWS Secret Access Key', required=True)
         .add_str('--service_base_name',
                  'Any infrastructure value (should be unique if '
                  'multiple SSN\'s have been deployed before).',
                  default='dlab-k8s')
         .add_int('--ssn_k8s_masters_count', 'Count of K8S masters.', default=3)
         .add_int('--ssn_k8s_workers_count', 'Count of K8S workers', default=2)
         .add_str('--ssn_k8s_masters_shape', 'Shape for SSN K8S masters.',
                  default='t2.medium')
         .add_str('--ssn_k8s_workers_shape', 'Shape for SSN K8S workers.',
                  default='t2.medium')
         .add_int('--ssn_root_volume_size', 'Size of root volume in GB.',
                  default=30)
         .add_str('--subnet_cidr_a',
                  'CIDR for Subnet creation in zone a. Conflicts with  subnet_id_a.',
                  default='172.31.0.0/24')
         .add_str('--subnet_cidr_b',
                  'CIDR for Subnet creation in zone b. Conflicts with  subnet_id_b.',
                  default='172.31.1.0/24')
         .add_str('--subnet_cidr_c',
                  'CIDR for Subnet creation in zone c. Conflicts with  subnet_id_c.',
                  default='172.31.2.0/24')
         .add_str('--subnet_id_a',
                  'ID of AWS Subnet in zone a if you already have subnet created.')
         .add_str('--subnet_id_b',
                  'ID of AWS Subnet in zone b if you already have subnet created.')
         .add_str('--subnet_id_c',
                  'ID of AWS Subnet in zone c if you already have subnet created.')
         .add_str('--vpc_cidr', 'CIDR for VPC creation. Conflicts with vpc_id',
                  default='172.31.0.0/16')
         .add_str('--vpc_id', 'ID of AWS VPC if you already have VPC created.')
         .add_str('--zone', 'Name of AWS zone', default='a')
         )
        return params.build()

    def check_k8s_cluster_status(self):
        """ Check for kubernetes status

        Returns:
            None
        Raises:
            TerraformProviderError: if master or kubeDNS is not running

        """
        terraform = TerraformProvider()
        output = terraform.output('-json ssn_k8s_masters_ip_addresses')
        args = self.parse_args()

        ip = self.get_node_ip(output)
        user_name = args.get('terraform_args').get('os_user')
        pkey_path = args.get('service_args').get('pkey')

        Console.connect_to_ssh(ip, user_name, pkey_path)
        start_time = time.time()
        while True:
            stdout = run(
                'kubectl cluster-info | '
                'sed -r "s/\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[mGK]//g"')
            outlines = stdout.readlines()
            k8c_info_status = ''.join(outlines)

            kubernetes_success_status = 'Kubernetes master is running'
            kubernetes_dns_success_status = 'KubeDNS is running'

            kubernetes_succeed = kubernetes_success_status in k8c_info_status
            kube_dns_succeed = kubernetes_dns_success_status in k8c_info_status

            if kubernetes_succeed and kube_dns_succeed:
                break
            if (time.time() - start_time) >= 600:
                raise TimeoutError
            time.sleep(60)

    def copy_terraform_to_remote(self):
        args = self.parse_args()
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        source = os.path.join(tf_dir, 'aws/ssn-helm-charts')
        user_name = args.get('terraform_args').get('os_user')
        put(source, '/home/{}/terraform/'.format(user_name))

    def run_remote_terraform(self):
        with cd('terraform/ssn-helm-charts/'):
            run('terraform init')
            run('terraform apply')

    def deploy(self):
        self.check_k8s_cluster_status()
        self.copy_terraform_to_remote()
        self.run_remote_terraform()


class AWSEndpointBuilder(AbstractDeployBuilder):

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/endpoint/main')

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_str('--service_base_name',
                  'Any infrastructure value (should be unique if  multiple '
                  'SSN\'s have been deployed before). Should be  same as on ssn')
         .add_str('--vpc_id', 'ID of AWS VPC if you already have VPC created.')
         .add_str('--vpc_cidr', 'CIDR for VPC creation. Conflicts with vpc_id.',
                  default='172.31.0.0/16')
         .add_str('--subnet_id',
                  'ID of AWS Subnet if you already have subnet created.')
         .add_str('--subnet_cidr',
                  'CIDR for Subnet creation. Conflicts with subnet_id.',
                  default='172.31.0.0/24')
         .add_str('--ami', 'ID of EC2 AMI.', required=True)
         .add_str('--key_name', 'Name of EC2 Key pair.', required=True)
         .add_str('--region', 'Name of AWS region.', default='us-west-2')
         .add_str('--zone', 'Name of AWS zone.', default='a')
         .add_str('--network_type',
                  'Type of created network (if network is not existed and '
                  'require creation) for endpoint',
                  default='public')
         .add_str('--endpoint_instance_shape', 'Instance shape of Endpoint.',
                  default='t2.medium')
         .add_int('--endpoint_volume_size', 'Size of root volume in GB.',
                  default=30)
         .add_str('--request_id', 'Request id', is_terraform_param=False)
         .add_str('--dlab_path', '', is_terraform_param=False)
         .add_str('--resource', '', is_terraform_param=False)
         .add_str('--conf_key_name', '', is_terraform_param=False)
         .add_str('--pkey', '', is_terraform_param=False, required=True)
         .add_str('--hostname', '', is_terraform_param=False)
         .add_str('--jar_url', '', is_terraform_param=False)
         .add_str('--os_user', '', is_terraform_param=False)
         .add_str('--cloud_provider', '', is_terraform_param=False)
         .add_str('--ssn_host', '', is_terraform_param=False)
         .add_str('--mongo_password', '', is_terraform_param=False)
         .add_str('--repository_address', '', is_terraform_param=False)
         .add_str('--repository_user', '', is_terraform_param=False)
         .add_str('--repository_pass', '', is_terraform_param=False)
         .add_str('--docker_version', '', is_terraform_param=False,
                  default='18.06.3~ce~3-0~ubuntu')

         )
        return params.build()

    def deploy(self):
        pass


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', help='Target', choices=['aws'],
                        required=True)
    parser.add_argument('--target', help='Source', choices=['k8s', 'endpoint'],
                        required=True)
    arguments = vars(parser.parse_known_args()[0])

    source = arguments.get('source').lower()
    target = arguments.get('target').lower()

    if source == 'aws':
        if target == 'k8s':
            builder = AWSK8sSourceBuilder()
        elif target == 'endpoint':
            builder = AWSEndpointBuilder()

    deploy_director = DeployDirector()
    deploy_director.build(builder)


if __name__ == "__main__":
    main()
