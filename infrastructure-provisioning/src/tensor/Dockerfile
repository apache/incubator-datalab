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

COPY tensor/ /root/
COPY general/scripts/${CLOUD}/tensor_* /root/scripts/
COPY general/scripts/os/* /root/scripts/
COPY tensor/scripts/create_configs.py /root/scripts/
COPY general/lib/os/${OS}/notebook_lib.py /usr/lib/python2.7/dlab/notebook_lib.py
COPY general/templates/os/notebook_spark-defaults_local.conf /root/templates/
COPY general/templates/os/${OS}/jupyter-notebook.service /root/templates/
COPY general/templates/os/pyspark_local_template.json /root/templates/
COPY general/templates/os/py3spark_local_template.json /root/templates/
COPY general/templates/os/pyspark_emr_template.json /root/templates/
COPY general/templates/os/${OS}/ungit.service /root/templates/
COPY general/templates/os/git_pre_commit.py /root/templates/
COPY general/files/os/notebook_local_jars.tar.gz /root/files/
COPY general/files/os/cudnn-8.0-linux-x64-v5.1.tgz /root/files/
COPY general/templates/os/tensorboard.service /root/templates/

RUN chmod a+x /root/fabfile.py; \
    chmod a+x /root/scripts/*
