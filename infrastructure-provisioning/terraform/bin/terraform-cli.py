#!/usr/bin/env python
import json
import os
import abc
import argparse
import paramiko
import time


class TerraformProviderError(Exception):
    """
    Raises errors while terraform provision
    """
    pass


class Console:
    @staticmethod
    def exec_command(command):
        """ Execute cli command

        Args:
            command: str cli command
        Returns:
            str: command result
        """
        return os.popen(command).read()

    @staticmethod
    def remote(ip, user, pkey=None, passwd=None):
        """ Get remote console\

        Args:
            ip: str address
            user: str username
            pkey: str path to pkey
            passwd: str password
        Returns:
            SSHClient: remoter cli
        """
        pkey = paramiko.RSAKey.from_private_key_file('path') if pkey else None
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(ip, username=user, pkey=pkey, password=passwd)
        return ssh


class TerraformProvider:
    def initialize(self):
        """Initialize terraform

        Returns:
             bool: init successful
        Raises:
            TerraformProviderError: if initialization was not succeed
        """
        terraform_success_init = 'Terraform has been successfully initialized!'
        terraform_init_result = Console.exec_command('terraform init')
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
        terraform_validate_result = Console.exec_command('terraform validate')
        if terraform_success_validate not in terraform_validate_result:
            raise TerraformProviderError(terraform_validate_result)

    def apply(self, cli_args):
        """Run terraform

        Args:
            cli_args: dict of parameters
        Returns:
             None
        """
        args_str = self.get_args_string(cli_args)
        command = 'terraform apply -auto-approve -target module.ssn-k8s {}'
        Console.execute(command.format(args_str))

    def destroy(self, cli_args):
        """Destroy terraform

        Args:
            cli_args: dict of parameters
        Returns:
             None
        """
        args_str = self.get_args_string(cli_args)
        command = 'terraform destroy -auto-approve -target module.ssn-k8s {}'
        Console.exec_command(command.format(args_str))

    def output(self, *args):
        """Get terraform output

        Args:
            *args: list of str parameters
        Returns:
            str: terraform output result
        """
        return Console.exec_command('terraform output '.format(' '.join(args)))

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
            'terraform_args': vars(terraform_args_parser.parse_args()),
            'service_args': vars(client_args_parser.parse_args()),
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
                self.check_k8s_cluster_status()
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
        return json.loads(output)

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
        user_name = args.get('terraform').get('os_user')
        pkey_path = args.get('cli').get('pkey')

        console = Console.remote(ip, user_name, pkey=pkey_path)
        start_time = time.time()
        while True:
            stdin, stdout, stderr = console.exec_command('kubectl cluster-info')
            outlines = stdout.readlines()
            k8c_info_status = ''.join(outlines)
            if not k8c_info_status:
                if (time.time() - start_time) >= 600:
                    raise TimeoutError
                time.sleep(120)

            kubernetes_success_status = 'Kubernetes master is running'
            kubernetes_dns_success_status = 'KubeDNS is running'
            if kubernetes_success_status not in k8c_info_status:
                raise TerraformProviderError(
                    'Master issue: {}'.format(k8c_info_status))
            if kubernetes_dns_success_status not in k8c_info_status:
                raise TerraformProviderError(
                    'KubeDNS issue: {}'.format(k8c_info_status))
            break


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


class AWSSourceBuilder(AbstractDeployBuilder):

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/main')

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_str('--pkey', 'path to key', is_terraform_param=False)
         .add_str('--action', 'Action', default='deploy',
                  is_terraform_param=False)
         .add_str('--access_key_id', 'AWS Access Key ID')
         .add_str('--allowed_cidrs',
                  'CIDR to allow acces to SSN K8S cluster.',
                  default=["0.0.0.0/0"], action='append')
         .add_str('--ami', 'ID of EC2 AMI.')
         .add_str('--env_os', 'OS type.', default='debian')
         .add_str('--key_name', 'Name of EC2 Key pair.')
         .add_str('--os_user', 'Name of DLab service user.',
                  default='dlab-user')
         .add_str('--region', 'Name of AWS region.', default='us-west-2')
         .add_str('--secret_access_key', 'AWS Secret Access Key')
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
         .add_str('--subnet_cidr',
                  'CIDR for Subnet creation. Conflicts with  subnet_id.',
                  default='172.31.0.0/24')
         .add_str('--subnet_id',
                  'ID of AWS Subnet if you already have subnet created.')
         .add_str('--vpc_cidr', 'CIDR for VPC creation. Conflicts with vpc_id',
                  default='172.31.0.0/16')
         .add_str('--vpc_id', 'ID of AWS VPC if you already have VPC created.')
         .add_str('--zone', 'Name of AWS zone', default='a'))
        return params.build()

    def deploy(self):
        # os.system('ls -l')
        print('installation process')


def main():
    # TODO switch case depend on TF file name
    deploy_director = DeployDirector()
    builder = AWSSourceBuilder()
    deploy_director.build(builder)


if __name__ == "__main__":
    main()
