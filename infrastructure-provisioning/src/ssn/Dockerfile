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

FROM docker.dlab-base:latest

ARG OS
ARG CLOUD

COPY ssn/ /root/
COPY general/scripts/${CLOUD}/ssn_* /root/scripts/
COPY general/lib/os/${OS}/ssn_lib.py /usr/lib/python2.7/dlab/ssn_lib.py
COPY general/files/${CLOUD}/ssn_instance_shapes.lst /root/files/
COPY general/files/${CLOUD}/ssn_policy.json /root/files/
COPY general/templates/${CLOUD}/jenkins_jobs/ /root/templates/

RUN chmod a+x /root/fabfile.py; \
    chmod a+x /root/scripts/*

RUN mkdir /project_tree
COPY . /project_tree

