# ******************************************************************************************************
#
# Copyright (c) 2016 EPAM Systems Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including # without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject # to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH # # THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# ****************************************************************************************************/


FROM ubuntu:16.04

# Install any .deb dependecies
RUN	apt-get update && \
    apt-get -y upgrade && \
    apt-get -y install python-pip python-dev groff vim less git wget nano && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install any python dependencies
RUN pip install boto3 fabric awscli boto argparse ujson jupyter

# Configuring ssh for user
RUN mkdir -p /root/.ssh; echo "Host *" > /root/.ssh/config; \
    echo "StrictHostKeyChecking no" >>  /root/.ssh/config; \
    echo "UserKnownHostsFile=/dev/null" >> /root/.ssh/config; \
    echo "GlobalKnownHostsFile=/dev/null" >> /root/.ssh/config; \
    echo "ConnectTimeout=30" >> /root/.ssh/config

RUN mkdir -p /response; chmod a+rwx /response

# Configuring entrypoint script
ENV PROVISION_CONFIG_DIR /root/conf/
ENV KEYFILE_DIR /root/keys/
ENV AWS_DIR /root/.aws
RUN mkdir -p /root/conf
RUN mkdir -p /root/templates
RUN mkdir -p /root/keys/.ssh
COPY . /root
COPY dlab /usr/lib/python2.7/dlab
RUN chmod a+x /root/*.py

# Entrypoint
ENTRYPOINT ["/root/entrypoint.py"]
