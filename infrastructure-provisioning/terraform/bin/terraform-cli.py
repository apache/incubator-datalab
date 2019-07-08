#!/usr/bin/env python

import os
import abc
import argparse


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
    def install(self):
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
        for argument, props in self.cli_args.items():
            parser.add_argument(argument, **props)
        return vars(parser.parse_args())

    def tf_init(self):
        """Initialize terraform

        Returns:
             bool: init successful
        """
        terraform_success_init = 'Terraform has been successfully initialized!'
        terraform_init_result = self.console_execute('terraform init')
        return terraform_success_init in terraform_init_result

    def tf_validate(self):
        """Validate terraform

        Returns:
             bool: validation successful
        """
        terraform_success_validate = 'Success! The configuration is valid.'
        terraform_validate_result = self.console_execute('terraform validate')
        return terraform_success_validate in terraform_validate_result

    def tf_apply(self, cli_args):
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

    def run_tf(self):
        """Execute terraform script

        Returns:
            None
        """
        tf_location = self.terraform_location
        cli_args = self.parse_args()

        os.chdir(tf_location)

        if self.tf_init() and self.tf_validate():
            self.tf_apply(cli_args)


    @staticmethod
    def console_execute(command):
        return os.popen(command).read()


class DeployDirector:

    def build(self, builder):
        """ Do build action

        Args:
            builder: AbstractDeployBuilder
        Returns:
            None
        """
        builder.run_tf()
        builder.install()

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
        return {
            'service_base_name': {
                'type': str,
                'help': 'Any infrastructure value (should be unique '
                        'if multiple SSN’s have been deployed before).',
                'nargs': '?',
                'default': 'dlab-k8s',
            },
            'vpc_id': {
                'type': str,
                'nargs': '?',
                'help': 'ID of AWS VPC if you already have VPC created.',
            },
            'vpc_cidr': {
                'type': str,
                'help': 'CIDR for VPC creation. Conflicts with vpc_id',
                'nargs': '?',
                'default': '172.31.0.0/16',
            },
            'subnet_id': {
                'type': str,
                'nargs': '?',
                'help': 'ID of AWS Subnet if you already have subnet created.',
            },
            'subnet_cidr': {
                'type': str,
                'help': 'CIDR for Subnet creation. Conflicts with subnet_id.',
                'nargs': '?',
                'default': '172.31.0.0/24',
            },
            'env_os': {
                'type': str,
                'help': 'OS type. Available options: debian, redhat.',
                'nargs': '?',
                'default': 'debian',
                'choices': ('debian', 'redhat'),
            },
            'ami': {  # from python
                'type': str,
                'help': 'ID of EC2 AMI.',
                'nargs': '?',
            },
            'key_name': {  # from python
                'type': str,
                'help': 'Name of EC2 Key pair.',
                'nargs': '?',
            },
            'region': {
                'type': str,
                'help': 'Name of AWS region.',
                'nargs': '?',
                'default': 'us-west-2',
            },
            'zone': {
                'type': str,
                'help': 'Name of AWS zone',
                'nargs': '?',
                'default': 'a',
            },
            'ssn_k8s_masters_count': {
                'type': int,
                'help': 'Count of K8S masters.',
                'nargs': '?',
                'default': 3,
            },
            'ssn_k8s_workers_count': {
                'type': int,
                'help': 'Count of K8S workers',
                'nargs': '?',
                'default': 2,
            },
            'ssn_root_volume_size': {
                'type': int,
                'help': 'Size of root volume in GB.',
                'nargs': '?',
                'default': 30,
            },
            'allowed_cidrs': {
                'type': str,
                'help': 'CIDR to allow acces to SSN K8S cluster.',
                'nargs': '?',
                'default': '0.0.0.0/0',
            },
            'ssn_k8s_masters_shape': {
                'type': str,
                'help': 'Shape for SSN K8S masters.',
                'nargs': '?',
                'default': 't2.medium',
            },
            'ssn_k8s_workers_shape': {
                'type': str,
                'help': 'Shape for SSN K8S workers.',
                'nargs': '?',
                'default': 't2.medium',
            },
            'os_user': {
                'type': str,
                'help': 'Name of DLab service user.',
                'nargs': '?',
                'default': 'dlab-user',
            },
        }

    def install(self):
        # os.system('ls -l')
        print('installation process')


def main():
    # TODO switch case depend on TF file name
    deploy_director = DeployDirector()
    builder = AWSSourceBuilder()
    deploy_director.build(builder)


if __name__ == "__main__":
    main()
