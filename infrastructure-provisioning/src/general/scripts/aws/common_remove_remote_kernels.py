#!/usr/bin/python3

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

import argparse
import sys
from datalab.actions_lib import remove_dataengine_kernels, remove_kernels
from datalab.fab import init_datalab_connection, find_cluster_kernels
from fabric import *
from datalab.logger import logging

parser = argparse.ArgumentParser()
parser.add_argument('--hostname', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
parser.add_argument('--os_user', type=str, default='')
parser.add_argument('--nb_tag_name', type=str, default='')
parser.add_argument('--nb_tag_value', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    print('Configure connections')
    global conn
    conn = init_datalab_connection(args.hostname, args.os_user, args.keyfile)

    try:
        de_clusters, des_clusters = find_cluster_kernels()
        for cluster in de_clusters:
            remove_dataengine_kernels(args.nb_tag_name, args.nb_tag_value, args.os_user, args.keyfile, cluster)
        for cluster in des_clusters:
            remove_kernels(cluster.split('/')[1], args.nb_tag_name, args.nb_tag_value,
                           args.os_user, args.keyfile, cluster.split('/')[0])
        conn.close()
        sys.exit(0)
    except Exception as err:
        logging.error('Failed to remove cluster kernels.', str(err))
        sys.exit(1)