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

import subprocess
import os
import argparse


parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
parser.add_argument('--dataproc_version', type=str, default='')
parser.add_argument('--nb_user', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    spark_def_path = "/usr/lib/spark/conf/spark-defaults.conf"
    os.system(
        'sudo sed -i "s|secure_path.*$|secure_path=\"/opt/conda/default/bin:/opt/conda/miniconda3/condabin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/snap/bin:/usr/local/bin\"|g" /etc/sudoers')

    os.system('touch /tmp/r_version')
    r_ver = subprocess.check_output("R --version | awk '/version / {print $3}'", shell=True).decode('UTF-8')
    with open('/tmp/r_version', 'w') as outfile:
        outfile.write(r_ver)

    os.system('touch /tmp/python_version')
    for v in range(4, 9):
        python_ver_checker = "python3.{} -V 2>/dev/null".format(v) + " | awk '{print $2}'"
        python_ver = subprocess.check_output(python_ver_checker, shell=True).decode('UTF-8')
        if python_ver != '':
            with open('/tmp/python_version', 'w') as outfile:
                outfile.write(python_ver)
    os.system('touch /tmp/spark_version')
    spark_ver = subprocess.check_output("dpkg -l | grep spark-core | tr -s ' ' '-' | cut -f 4 -d '-'", shell=True).decode('UTF-8')
    with open('/tmp/spark_version', 'w') as outfile:
        outfile.write(spark_ver)
    os.system('touch /tmp/scala_version')
    scala_ver = subprocess.check_output("spark-submit --version 2>&1 | grep -o -P 'Scala version \K.{0,7}'",
                                        shell=True).decode('UTF-8')
    with open('/tmp/scala_version', 'w') as outfile:
        outfile.write(scala_ver)
    os.system('touch /tmp/hadoop_version')
    hadoop_ver = subprocess.check_output("dpkg -l | grep hadoop | head -n 1 | tr -s ' ' '-' | cut -f 3 -d '-'", shell=True).decode('UTF-8')
    with open('/tmp/hadoop_version', 'w') as outfile:
        outfile.write(hadoop_ver)

    os.system('/bin/tar -zhcvf /tmp/jars.tar.gz --no-recursion --absolute-names --ignore-failed-read /usr/lib/hadoop/* /usr/lib/hadoop/client/*')
    os.system('/bin/tar -zhcvf /tmp/spark.tar.gz -C /usr/lib/ spark')
    md5sum = subprocess.check_output('md5sum /tmp/jars.tar.gz', shell=True).decode('UTF-8')
    with open('/tmp/jars-checksum.chk', 'w') as outfile:
        outfile.write(md5sum)
    md5sum = subprocess.check_output('md5sum /tmp/spark.tar.gz', shell=True).decode('UTF-8')
    with open('/tmp/spark-checksum.chk', 'w') as outfile:
        outfile.write(md5sum)

    os.system('gsutil -m cp /etc/hive/conf/hive-site.xml gs://{0}/{1}/{2}/config/hive-site.xml'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /etc/hadoop/conf/* gs://{0}/{1}/{2}/config/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('sudo -u {0} hdfs dfs -mkdir /user/{0}'.format(args.nb_user))
    os.system('sudo -u {0} hdfs dfs -chown -R {0}:{0} /user/{0}'.format(args.nb_user))
    os.system('gsutil -m cp /tmp/jars.tar.gz gs://{0}/jars/{1}/'.format(args.bucket, args.dataproc_version))
    os.system('gsutil -m cp /tmp/jars-checksum.chk gs://{0}/jars/{1}/'.format(args.bucket, args.dataproc_version))
    os.system('gsutil -m cp {0} gs://{1}/{2}/{3}/'.format(spark_def_path, args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/python_version gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/spark_version gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/scala_version gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/r_version gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/hadoop_version gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/spark.tar.gz gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil -m cp /tmp/spark-checksum.chk gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))

