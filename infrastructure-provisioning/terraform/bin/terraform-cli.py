#!/usr/bin/env python

import os
import abc
import argparse


class TerraformProviderError(Exception):
    """
    Raises errors while terraform provision
    """
    pass


class TerraformProvider:
    def initialize(self):
        """Initialize terraform

        Returns:
             bool: init successful
        """
        terraform_success_init = 'Terraform has been successfully initialized!'
        terraform_init_result = self.console_execute('terraform init')
        if terraform_success_init not in terraform_init_result:
            raise TerraformProviderError(terraform_init_result)

    def validate(self):
        """Validate terraform

        Returns:
             bool: validation successful
        """
        terraform_success_validate = 'Success! The configuration is valid.'
        terraform_validate_result = self.console_execute('terraform validate')
        if terraform_success_validate not in terraform_validate_result:
            raise TerraformProviderError(terraform_validate_result)

    def apply(self, cli_args):
        """Run terraform

        Args:
            cli_args: dict of parameters
        Returns:
             None
        """
        args = ['-var {0}={1}'.format(key, value) for key, value
                in cli_args.items() if value]
        args_str = ' '.join(args)
        print('terraform apply {}'.format(args_str))

    @staticmethod
    def console_execute(command):
        return os.popen(command).read()


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
        parser = argparse.ArgumentParser()
        for argument in self.cli_args:
            parser.add_argument(argument.get('name'), **argument.get('props'))
        return vars(parser.parse_args())

    def provision(self):
        """Execute terraform script

        Returns:
            None
        """
        tf_location = self.terraform_location
        cli_args = self.parse_args()
        terraform = TerraformProvider()

        os.chdir(tf_location)
        try:
            terraform.initialize()
            terraform.validate()
            terraform.apply(cli_args)
        except TerraformProviderError as error:
            print(error)

    def build_str_arg_param(self, name, desc, **kwargs):
        return self.build_arg_param(str, name, desc, **kwargs)

    def build_int_arg_param(self, name, desc, **kwargs):
        return self.build_arg_param(int, name, desc, **kwargs)

    @staticmethod
    def build_arg_param(arg_type, name, desc, **kwargs):
        return {
            'name': name,
            'props': {
                'help': desc,
                'type': arg_type,
                'nargs': kwargs.get('nargs', '?'),
                'default': kwargs.get('default'),
                'choices': kwargs.get('choices'),
            }
        }


class DeployDirector:

    def build(self, builder):
        """ Do build action

        Args:
            builder: AbstractDeployBuilder
        Returns:
            None
        """
        builder.provision()
        builder.deploy()

    def get_status(self):
        """ Get execution status

        Returns:
            int: Execution error status (0 if success)
        """
        return 0


class AWSSourceBuilder(AbstractDeployBuilder):

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/main')

    @property
    def cli_args(self):
        return [
            self.build_str_arg_param('access_key_id',
                                     'AWS Access Key ID.',
                                     nargs=None),
            self.build_str_arg_param('secret_access_key',
                                     'AWS Secret Access Key.',
                                     nargs=None),
            self.build_str_arg_param('service_base_name',
                                     'Any infrastructure value (should be '
                                     'unique if multiple SSN\'s have been '
                                     'deployed before).',
                                     default='dlab-k8s'),
            self.build_str_arg_param('vpc_id',
                                     'ID of AWS VPC if you already have VPC '
                                     'created.'),
            self.build_str_arg_param('vpc_cidr',
                                     'CIDR for VPC creation. '
                                     'Conflicts with vpc_id',
                                     default='172.31.0.0/16'),
            self.build_str_arg_param('subnet_id',
                                     'ID of AWS Subnet if you already have '
                                     'subnet created.'),
            self.build_str_arg_param('subnet_cidr',
                                     'CIDR for Subnet creation. Conflicts with '
                                     'subnet_id.',
                                     default='172.31.0.0/24'),
            self.build_str_arg_param('env_os',
                                     'OS type.',
                                     default='debian',
                                     choices=('debian', 'redhat')),
            self.build_str_arg_param('ami', 'ID of EC2 AMI.',
                                     nargs=None),
            self.build_str_arg_param('key_name', 'Name of EC2 Key pair.',
                                     nargs=None),
            self.build_str_arg_param('region', 'Name of AWS region.',
                                     default='us-west-2'),
            self.build_str_arg_param('zone', 'Name of AWS zone', default='a'),
            self.build_str_arg_param('allowed_cidrs',
                                     'CIDR to allow acces to SSN K8S cluster.',
                                     default='0.0.0.0/0'),
            self.build_str_arg_param('ssn_k8s_masters_shape',
                                     'Shape for SSN K8S masters.',
                                     default='t2.medium'),
            self.build_str_arg_param('ssn_k8s_workers_shape',
                                     'Shape for SSN K8S workers.',
                                     default='t2.medium'),
            self.build_str_arg_param('os_user', 'Name of DLab service user.',
                                     default='dlab-user'),
            self.build_int_arg_param('ssn_k8s_masters_count',
                                     'Count of K8S masters.', default=3),
            self.build_int_arg_param('ssn_k8s_workers_count',
                                     'Count of K8S workers', default=2),
            self.build_int_arg_param('ssn_root_volume_size',
                                     'Size of root volume in GB.', default=30),
        ]

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
