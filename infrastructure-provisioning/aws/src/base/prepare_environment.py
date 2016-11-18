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

import os
from ConfigParser import SafeConfigParser

for filename in os.listdir('/root/conf'):
    if filename.endswith('.ini'):
        config = SafeConfigParser()
        config.read(os.path.join('/root/conf', filename))
        for section in config.sections():
            for option in config.options(section):
                varname = "%s_%s" % (section, option)
                if varname not in os.environ:
                    with open('/root/.bashrc', 'a') as bashrc:
                        bashrc.write("export %s=%s\n" % (varname, config.get(section, option)))

# Enforcing overwrite
for filename in os.listdir('/root/conf'):
    if filename.endswith('overwrite.ini'):
        config = SafeConfigParser()
        config.read(os.path.join('/root/conf', filename))
        for section in config.sections():
            for option in config.options(section):
                varname = "%s_%s" % (section, option)
                with open('/root/.bashrc', 'a') as bashrc:
                    bashrc.write("export %s=%s\n" % (varname, config.get(section, option)))
