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

FROM docker.epmc-bdcc.projects.epam.com/dlab-aws-base:latest

COPY ssn/ /root/
RUN chmod 755 /root/fabfile.py; \
    chmod 755 /root/scripts/*; \
    chmod a+x /root/entrypoint.py; \
    chmod a+x /root/api/*; \
    mv /root/api/*.py /bin/

RUN mkdir /project_tree
COPY . /project_tree

