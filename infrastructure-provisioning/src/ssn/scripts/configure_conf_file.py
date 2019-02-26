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


from fabric.api import *
import sys
import argparse
import os
import json
import ConfigParser

parser = argparse.ArgumentParser()
parser.add_argument('--dlab_dir', type=str, default='')
parser.add_argument('--variables_list', type=str, default='')
args = parser.parse_args()


def modify_conf_file():
    try:
        variables_list = json.loads(args.variables_list)
        conf_list = []
        basic_auth_repos = ('apt_bintray_repo', 'apt_ubuntu_security_repo', 'apt_ubuntu_repo', 'docker_repo',
                            'jenkins_repo', 'maven_bintray_repo', 'maven_central_repo', 'mongo_repo', 'pypi_repo',
                            'packages_repo', 'r_repo', 'rrutter_repo')
        conf_file = open('{}sources/infrastructure-provisioning/src/general/conf/dlab.ini'.format(args.dlab_dir), 'r')
        for line in conf_file:
            conf_list.append(line)

        for line in conf_list:
            if line[0:2] == '# ':
                conf_list[conf_list.index(line)] = line.replace('# ', '')

        with open('/tmp/dlab.ini.modified', 'w') as conf_file_modified:
            conf_file_modified.writelines(conf_list)

        config = ConfigParser.RawConfigParser()
        config.read('/tmp/dlab.ini.modified')
        for section in config.sections():
            options = config.options(section)
            for option in options:
                try:
                    print('Trying to put variable {}_{} to conf file'.format(section, option))
                    config.set(section, option, variables_list['{}_{}'.format(section, option)])
                except:
                    print('Such variable doesn`t exist!')
                    config.remove_option(section, option)
            # if section == 'local_repository' and 'repository_user_name' in options and 'repository_user_password' in \
            #         options:
            #         for option in options:
            #             if option in basic_auth_repos:
            #                 repo_url = config.get(section, option)
            #                 updated_repo_url = repo_url.replace('{}://'.format(repo_url.split(':')[0]),
            #                                                     '{0}://{1}:{2}@'.format(
            #                                                         repo_url.split(':')[0],
            #                                                         config.get(section, 'repository_user_name'),
            #                                                         config.get(section,
            #                                                                    'repository_user_password')))
            #                 config.set(section, option, updated_repo_url)

        with open('{}sources/infrastructure-provisioning/src/general/conf/overwrite.ini'.format(
                args.dlab_dir), 'w') as conf_file_final:
            config.write(conf_file_final)
    except Exception as error:
        print('Error with modifying conf files:')
        print(str(error))
        sys.exit(1)


if __name__ == "__main__":
    try:
        modify_conf_file()
    except:
        sys.exit(1)
