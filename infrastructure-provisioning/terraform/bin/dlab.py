import argparse
import itertools
import json
import logging
import os
import subprocess
import sys
import time
from abc import abstractmethod

from fabric import Connection
from patchwork.transfers import rsync
from deploy.endpoint_fab import start_deploy

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
logging.basicConfig(level=logging.INFO, format='%(levelname)s-%(message)s')
INITIAL_LOCATION = os.path.abspath(__file__)


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

    @abstractmethod
    def remove_keys(self, keys):
        pass


class LocalStorageOutputProcessor(TerraformOutputBase):
    @property
    def output_path(self):
        return os.path.abspath(
            os.path.join(os.path.dirname(INITIAL_LOCATION), 'output.json'))

    def write(self, obj):
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
        if os.path.isfile(self.output_path):
            with open(self.output_path, 'r') as fp:
                output = fp.read()
                if len(output):
                    return json.loads(output)

    def remove_keys(self, keys):
        output = self.extract()
        updated = {key: value for key, value in output.items()
                   if key not in keys}
        with open(self.output_path, 'w') as fp:
            json.dump(updated, fp)


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


class ParamsBuilder:

    def __init__(self):
        self.__params = []

    def add(self, arg_type, name, desc, **kwargs):
        parameter = {
            'group': kwargs.get('group'),
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


class Console:

    @staticmethod
    def execute_to_command_line(command):
        """ Execute cli command

        Args:
            command: str cli command
        Returns:
            str: command result
        """
        subprocess.run(command, shell=True)

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
            connection = Connection(host=ip,
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
    def initialize(self):
        """Initialize terraform

        Returns:
             bool: init successful
        Raises:
            TerraformProviderError: if initialization was not succeed
        """
        logging.info('terraform init')
        terraform_success_init = 'Terraform has been successfully initialized!'
        terraform_init_result = Console.execute('terraform init')
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
        terraform_validate_result = Console.execute('terraform validate')
        logging.info(terraform_validate_result)
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
        logging.info('terraform apply')
        args_str = get_args_string(cli_args)
        command = 'terraform apply -auto-approve {}'
        logging.info(command.format(args_str))
        Console.execute_to_command_line(command.format(args_str))

    def destroy(self, cli_args):
        """Destroy terraform

        Args:
            target: str
            cli_args: dict of parameters
        Returns:
             None
        """
        logging.info('terraform destroy')
        args_str = get_args_string(cli_args)
        command = 'terraform destroy -auto-approve {}'
        logging.info(command.format(args_str))
        Console.execute_to_command_line(command.format(args_str))

    def output(self, *args):
        """Get terraform output

        Args:
            *args: list of str parameters
        Returns:
            str: terraform output result
        """
        return Console.execute('terraform output {}'.format(' '.join(args)))


class AbstractDeployBuilder:

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

    def apply(self):
        cli_args = self.parse_args()
        terraform_args = cli_args.get(self.terraform_args_group_name)
        terraform = TerraformProvider()
        terraform.apply(terraform_args)

    def destroy(self):
        cli_args = self.parse_args()
        terraform_args = cli_args.get(self.terraform_args_group_name)
        terraform = TerraformProvider()
        terraform.destroy(terraform_args)

    def store_output_to_file(self):
        terraform = TerraformProvider()
        output = terraform.output('-json')
        output = {key: value.get('value')
                  for key, value in json.loads(output).items()}
        output_writer = LocalStorageOutputProcessor()
        output_writer.write(output)

    def update_extracted_file_data(self, obj):
        '''
        :param obj:
        :return:
        Override method if you need to modufy extracted from file data
        '''
        pass

    def fill_sys_argv_from_file(self):

        output_processor = LocalStorageOutputProcessor()
        output = output_processor.extract()
        if output:
            self.update_extracted_file_data(output)
            for key, value in output.items():
                key = '--' + key
                if key not in sys.argv:
                    sys.argv.extend([key, value])

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

    def provision(self):
        """Execute terraform script

        Returns:
            None
        Raises:
            TerraformProviderError: if init or validate fails
        """
        tf_location = self.terraform_location
        terraform = TerraformProvider()
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

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_str('--access_key_id', 'AWS Access Key ID', required=True,
                  group='k8s')
         .add_str('--allowed_cidrs',
                  'CIDR to allow acces to SSN K8S cluster.',
                  default=["0.0.0.0/0"], action='append', group='k8s')
         .add_str('--ami', 'ID of EC2 AMI.', required=True, group='k8s')
         .add_str('--env_os', 'OS type.', default='debian',
                  choices=['debian', 'redhat'], group=('k8s', 'helm_charts'))
         .add_str('--key_name', 'Name of EC2 Key pair.', required=True,
                  group='k8s')
         .add_str('--os_user', 'Name of DLab service user.',
                  default='dlab-user', group='k8s')
         .add_str('--pkey', 'path to key', required=True, group='service')
         .add_str('--region', 'Name of AWS region.', default='us-west-2',
                  group=('k8s', 'helm_charts'))
         .add_str('--secret_access_key', 'AWS Secret Access Key', required=True,
                  group='k8s')
         .add_str('--service_base_name',
                  'Any infrastructure value (should be unique if '
                  'multiple SSN\'s have been deployed before).',
                  default='k8s', group=('k8s', 'helm_charts'))
         .add_int('--ssn_k8s_masters_count', 'Count of K8S masters.', default=3,
                  group='k8s')
         .add_int('--ssn_k8s_workers_count', 'Count of K8S workers', default=2,
                  group=('k8s', 'helm_charts'))
         .add_str('--ssn_k8s_masters_shape', 'Shape for SSN K8S masters.',
                  default='t2.medium', group=('k8s', 'helm_charts'))
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
                  group=('k8s', 'helm_charts'))
         .add_str('--ssn_keystore_password', 'ssn_keystore_password',
                  group='helm_charts')
         .add_str('--endpoint_keystore_password', 'endpoint_keystore_password',
                  group='helm_charts')
         .add_str('--ssn_bucket_name', 'ssn_bucket_name',
                  group='helm_charts')
         .add_str('--endpoint_eip_address', 'endpoint_eip_address',
                  group='helm_charts')
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
         .add_str('--ssn_subnet', 'ssn subnet id', group='helm_charts')
         .add_str('--ssn_k8s_sg_id', 'ssn sg ids', group='helm_charts')
         .add_str('--ssn_vpc_id', 'ssn vpc id', group='helm_charts')
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
                    "kubectl get pods --all-namespaces | grep tiller | awk '{print $4}'") \
                    .stdout

                tiller_success_status = 'Running'

                if tiller_success_status in tiller_status:
                    break
                if (time.time() - start_time) >= 1200:
                    raise TimeoutError
                time.sleep(60)

    def select_master_ip(self):
        terraform = TerraformProvider()
        output = terraform.output('-json ssn_k8s_masters_ip_addresses')
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

    def run_remote_terraform(self):
        logging.info('apply helm charts')
        args = self.parse_args()
        dns_name = json.loads(TerraformProvider()
                              .output('-json ssn_k8s_alb_dns_name'))
        logging.info('apply ssn-helm-charts')
        terraform_args = args.get('helm_charts')
        args_str = get_args_string(terraform_args)
        with Console.ssh(self.ip, self.user_name, self.pkey_path) as conn:
            with conn.cd('terraform/ssn-helm-charts/main'):
                conn.run('terraform init')
                conn.run('terraform validate')
                command = ('terraform apply -auto-approve {} '
                           '-var \'ssn_k8s_alb_dns_name={}\''
                           .format(args_str, dns_name))
                logging.info(command)
                conn.run(command)
                output = ' '.join(conn.run('terraform output -json')
                                  .stdout.split())
                self.fill_args_from_dict(json.loads(output))

    def output_terraform_result(self):
        dns_name = json.loads(
            TerraformProvider().output('-json ssn_k8s_alb_dns_name'))
        ssn_bucket_name = json.loads(
            TerraformProvider().output('-json ssn_bucket_name'))
        ssn_k8s_sg_id = json.loads(
            TerraformProvider().output('-json ssn_k8s_sg_id'))
        ssn_subnet = json.loads(
            TerraformProvider().output('-json ssn_subnet'))
        ssn_vpc_id = json.loads(TerraformProvider().output('-json ssn_vpc_id'))

        logging.info("""
        DLab SSN K8S cluster has been deployed successfully!
        Summary:
        DNS name: {}
        Bucket name: {}
        VPC ID: {}
        Subnet ID:  {}
        SG IDs: {}
        DLab UI URL: http://{}
        """.format(dns_name, ssn_bucket_name, ssn_vpc_id,
                   ssn_subnet, ssn_k8s_sg_id, dns_name))

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
                output_processor = LocalStorageOutputProcessor()
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

    def deploy(self):
        logging.info('deploy')
        self.select_master_ip()
        self.add_ip_to_known_hosts(self.ip)
        self.check_k8s_cluster_status()
        self.check_tiller_status()
        output = ' '.join(TerraformProvider().output('-json').split())
        self.fill_args_from_dict(json.loads(output))
        self.copy_terraform_to_remote()
        self.run_remote_terraform()
        self.fill_remote_terraform_output()
        self.output_terraform_result()

    def destroy(self):
        logging.info('ssn-k8s destroy')
        output_processor = LocalStorageOutputProcessor()
        endpoint_output_keys = ['keycloak_client_secret']
        endpoint_output_keys.extend(
            json.loads(TerraformProvider().output('-json')).keys())
        output_processor.remove_keys(endpoint_output_keys)
        super(AWSK8sSourceBuilder, self).destroy()


class AWSEndpointBuilder(AbstractDeployBuilder):

    def __init__(self):
        super(AWSEndpointBuilder, self).__init__()
        self.fill_sys_argv_from_file()

    def update_extracted_file_data(self, obj):
        if 'ssn_vpc_id' in obj:
            obj['vpc_id'] = obj['ssn_vpc_id']

    @property
    def terraform_location(self):
        tf_dir = os.path.abspath(os.path.join(os.getcwd(), os.path.pardir))
        return os.path.join(tf_dir, 'aws/endpoint/main')

    @property
    def terraform_args_group_name(self):
        return 'endpoint'

    @property
    def cli_args(self):
        params = ParamsBuilder()
        (params
         .add_str('--secret_access_key', 'AWS Secret Access Key', required=True,
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
         .add_str('--vpc_cidr', 'CIDR for VPC creation. Conflicts with vpc_id.',
                  default='172.31.0.0/16', group='endpoint')
         .add_str('--ssn_subnet',
                  'ID of AWS Subnet if you already have subnet created.',
                  group='endpoint')
         .add_str('--subnet_cidr',
                  'CIDR for Subnet creation. Conflicts with subnet_id.',
                  default='172.31.0.0/24', group='endpoint')
         .add_str('--ami', 'ID of EC2 AMI.', required=True, group='endpoint')
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
         .add_str('--endpoint_eip_allocation_id',
                  'Elastic Ip created for Endpoint',
                  group='endpoint')
         .add_str('--product', 'Product name.', default='dlab',
                  group='endpoint')
         )
        return params.build()

    def deploy(self):
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


def get_status(self):
    """ Get execution status

    Returns:
        int: Execution error status (0 if success)
    """

    return 0


def deploy():
    actions = {'deploy', 'destroy'}

    sources_targets = {'aws': ['k8s', 'endpoint']}

    no_args_error = ('usage: ./dlab {} {} {}\n'.format(
        actions,
        set(sources_targets.keys()),
        set(itertools.chain(*sources_targets.values()))))
    no_source_error = (
        lambda x: ('usage: ./dlab {} {} {}\n'.format(
            x,
            set(sources_targets.keys()),
            set(itertools.chain(*sources_targets.values())))))
    no_target_error = (
        lambda x, y: ('usage: ./dlab {} {} {}\n'.format(
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
        }
    }
    builder = builders_dict[source][target]()
    deploy_director = DeployDirector()
    deploy_director.build(action, builder)


if __name__ == '__main__':
    deploy()
