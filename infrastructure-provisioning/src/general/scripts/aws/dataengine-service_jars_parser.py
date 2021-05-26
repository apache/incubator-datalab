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
parser.add_argument('--emr_version', type=str, default='')
parser.add_argument('--region', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    spark_def_path = "/usr/lib/spark/conf/spark-defaults.conf"
    spark_def_path_line1 = subprocess.check_output("cat " + spark_def_path +
                                                   " | grep spark.driver.extraClassPath | awk '{print $2}' | "
                                                   "sed 's/^:// ; s~jar:~jar ~g; s~/\*:~/\* ~g; s~:~/\* ~g'", shell=True).decode('UTF-8')
    spark_def_path_line2 = subprocess.check_output("cat " + spark_def_path +
                                                   " | grep spark.driver.extraLibraryPath | awk '{print $2}' | "
                                                   "sed 's/^:// ; s~jar:~jar ~g; s~/\*:~/\* ~g; s~:\|$~/\* ~g'",shell=True).decode('UTF-8')
    spark_def_path_line1 = spark_def_path_line1.strip('\n')
    spark_def_path_line2 = spark_def_path_line2.strip('\n')
    if args.region == 'us-east-1':
        endpoint = "https://s3.amazonaws.com"
    elif args.region == 'cn-north-1':
        endpoint = "https://s3.{}.amazonaws.com.cn".format(args.region)
    else:
        endpoint = "https://s3-{}.amazonaws.com".format(args.region)
    os.system('touch /tmp/scala_version')
    scala_ver = subprocess.check_output("spark-submit --version 2>&1 | awk '/Scala version / {gsub(/,/, \"\"); print $4}'",
                                        shell=True).decode('UTF-8')
    with open('/tmp/scala_version', 'w') as outfile:
        outfile.write(scala_ver)
    os.system('touch /tmp/r_version')
    r_ver = subprocess.check_output("R --version | awk '/version / {print $3}'", shell=True).decode('UTF-8')
    with open('/tmp/r_version', 'w') as outfile:
        outfile.write(r_ver)
    os.system('touch /tmp/python_version')
    for v in range(4, 8):
        python_ver_checker = "python3.{} -V 2>/dev/null".format(v) + " | awk '{print $2}'"
        python_ver = subprocess.check_output(python_ver_checker, shell=True).decode('UTF-8')
        if python_ver != '':
            with open('/tmp/python_version', 'w') as outfile:
                outfile.write(python_ver)
    os.system('/bin/tar -zhcvf /tmp/jars.tar.gz '
              '--no-recursion '
              '--absolute-names '
              '--ignore-failed-read /usr/lib/hadoop/* {} {} /usr/lib/hadoop/client/*'.
              format(spark_def_path_line1,
                     spark_def_path_line2))
    os.system('/bin/tar -zhcvf /tmp/spark.tar.gz -C /usr/lib/ spark')
    md5sum = subprocess.check_output('md5sum /tmp/jars.tar.gz', shell=True).decode('UTF-8')
    with open('/tmp/jars-checksum.chk', 'w') as outfile:
        outfile.write(md5sum)
    md5sum = subprocess.check_output('md5sum /tmp/spark.tar.gz', shell=True).decode('UTF-8')
    with open('/tmp/spark-checksum.chk', 'w') as outfile:
        outfile.write(md5sum)
    os.system('aws s3 cp /tmp/jars.tar.gz '
              's3://{}/jars/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.emr_version,
                     endpoint,
                     args.region))
    os.system('aws s3 cp /tmp/jars-checksum.chk '
              's3://{}/jars/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.emr_version,
                     endpoint,
                     args.region))
    os.system('aws s3 cp {} '
              's3://{}/{}/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(spark_def_path,
                     args.bucket,
                     args.user_name,
                     args.cluster_name,
                     endpoint,
                     args.region))
    os.system('aws s3 cp /tmp/python_version '
              's3://{}/{}/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.user_name,
                     args.cluster_name,
                     endpoint,
                     args.region))
    os.system('aws s3 cp /tmp/spark.tar.gz '
              's3://{}/{}/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.user_name,
                     args.cluster_name,
                     endpoint,
                     args.region))
    os.system('aws s3 cp /tmp/spark-checksum.chk '
              's3://{}/{}/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.user_name,
                     args.cluster_name,
                     endpoint, args.region))
    os.system('aws s3 cp /tmp/scala_version '
              's3://{}/{}/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.user_name,
                     args.cluster_name,
                     endpoint, args.region))
    os.system('aws s3 cp /tmp/r_version '
              's3://{}/{}/{}/ '
              '--endpoint-url {} '
              '--region {} --sse AES256'.
              format(args.bucket,
                     args.user_name,
                     args.cluster_name,
                     endpoint, args.region))