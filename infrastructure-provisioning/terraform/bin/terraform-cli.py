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

    def parse_args(self):
        """Get dict of arguments

        Returns:
            dict: CLI arguments
        """
        parser = argparse.ArgumentParser()
        for argument, props in self.cli_args.items():
            parser.add_argument(argument, **props)
        return vars(parser.parse_args())

    def run_tf(self):
        """Execute terraform script

        Returns:
            None
        """
        # location = self.terraform_location
        args = self.parse_args()
        print(args)

    @abc.abstractmethod
    def install(self):
        """Post terraform execution

        Returns:
            None
        """
        raise NotImplementedError


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


class K8SSourceBuilder(AbstractDeployBuilder):

    @property
    def terraform_location(self):
        # TODO: get terraform location
        return 'terraform location'

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
            'ami': {
                'type': str,
                'help': 'ID of EC2 AMI.',
            },
            'key_name': {
                'type': str,
                'help': 'Name of EC2 Key pair.',
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
    builder = K8SSourceBuilder()
    deploy_director.build(builder)


if __name__ == "__main__":
    main()
