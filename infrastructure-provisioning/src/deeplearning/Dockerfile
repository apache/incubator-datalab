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

COPY deeplearning/ /root/
COPY general/scripts/${CLOUD}/deeplearning_* /root/scripts/
COPY general/scripts/os/notebook_configure_proxy.py /root/scripts/
COPY general/lib/os/debian/notebook_lib.py /usr/lib/python2.7/dlab/notebook_lib.py
COPY general/lib/os/debian/common_lib.py /usr/lib/python2.7/dlab/common_lib.py
COPY general/templates/os/notebook_spark-defaults_local.conf /root/templates/
COPY general/templates/os/pyspark_local_template.json /root/templates/
COPY general/templates/os/pyspark_emr_template.json /root/templates/
COPY general/files/os/notebook_local_jars.tar.gz /root/files/
COPY general/templates/os/r_emr_template.json /root/templates/
COPY general/templates/os/r_template.json /root/templates/
COPY general/templates/os/run_template.sh /root/templates/
COPY general/templates/os/toree_emr_template.json /root/templates/
COPY general/templates/os/toree_emr_templatev2.json /root/templates/
COPY general/scripts/os/create_configs.py /root/scripts/
COPY general/scripts/${CLOUD}/jupyter_install_emr_kernels.py /root/scripts/
COPY general/files/os/toree-assembly-0.2.0.jar /root/files/
COPY general/files/os/toree_kernel.tar.gz /root/files/

RUN chmod a+x /root/fabfile.py; \
    chmod a+x /root/scripts/*

