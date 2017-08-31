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

FROM ubuntu:16.04
ARG OS
ARG CLOUD

# Install any .deb dependecies
RUN	apt-get update && \
    apt-get -y upgrade && \
    apt-get -y install python-pip python-dev groff vim less git wget nano libssl-dev libffi-dev libffi6 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install any python dependencies
RUN pip install -U pip
RUN pip install boto3 fabric fabvenv awscli argparse ujson jupyter google-api-python-client google-cloud-storage pycrypto azure azure-mgmt-authorization

# Configuring ssh for user
RUN mkdir -p /root/.ssh; echo "Host *" > /root/.ssh/config; \
    echo "StrictHostKeyChecking no" >>  /root/.ssh/config; \
    echo "UserKnownHostsFile=/dev/null" >> /root/.ssh/config; \
    echo "GlobalKnownHostsFile=/dev/null" >> /root/.ssh/config; \
    echo "ConnectTimeout=30" >> /root/.ssh/config

# Configuring log directories
RUN mkdir -p /response; chmod a+rwx /response
RUN mkdir -p /logs/ssn; chmod a+rwx /logs/ssn
RUN mkdir -p /logs/edge; chmod a+rwx /logs/edge
RUN mkdir -p /logs/notebook; chmod a+rwx /logs/notebook
RUN mkdir -p /logs/dataengine-service; chmod a+rwx /logs/dataengine-service

# Copying all base scripts to docker
ENV PROVISION_CONFIG_DIR /root/conf/
ENV KEYFILE_DIR /root/keys/
ENV AWS_DIR /root/.aws

RUN mkdir -p /root/conf
RUN mkdir -p /root/scripts
RUN mkdir -p /root/templates
RUN mkdir -p /root/files
RUN mkdir -p /usr/lib/python2.7/dlab
RUN mkdir -p /root/keys/.ssh

COPY base/ /root
COPY general/conf/* /root/conf/
COPY general/api/*.py /bin/
COPY general/scripts/${CLOUD}/common_* /root/scripts/
COPY general/lib/${CLOUD}/* /usr/lib/python2.7/dlab/
COPY general/lib/os/${OS}/common_lib.py /usr/lib/python2.7/dlab/common_lib.py
COPY general/lib/os/fab.py /usr/lib/python2.7/dlab/fab.py
COPY general/files/os/${OS}/sources.list /root/files/

RUN chmod a+x /root/*.py
RUN chmod a+x /root/scripts/*
RUN chmod a+x /bin/*.py

ENTRYPOINT ["/root/entrypoint.py"]
