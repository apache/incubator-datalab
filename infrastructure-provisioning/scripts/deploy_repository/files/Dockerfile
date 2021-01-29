# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

FROM ubuntu:16.04

# Install any .deb dependecies
RUN	apt-get update && \
    apt-get -y upgrade && \
    apt-get -y install python3-pip python3-dev python3-virtualenv groff vim less git wget nano libssl-dev libffi-dev libffi6 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install any python dependencies
RUN pip install -UI pip==9.0.3
RUN pip install boto3 backoff fabric==1.14.0 fabvenv awscli argparse ujson jupyter pycryptodome