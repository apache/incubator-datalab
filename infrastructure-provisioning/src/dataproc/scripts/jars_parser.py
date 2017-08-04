#!/usr/bin/python

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

import subprocess
import os
import argparse


parser = argparse.ArgumentParser()
parser.add_argument('--bucket', type=str, default='')
parser.add_argument('--user_name', type=str, default='')
parser.add_argument('--cluster_name', type=str, default='')
args = parser.parse_args()


if __name__ == "__main__":
    spark_def_path = "/usr/lib/spark/conf/spark-defaults.conf"
    spark_def_path_line1 = subprocess.check_output("cat " + spark_def_path +
                                                   " | grep spark.driver.extraClassPath | awk '{print $2}' | "
                                                   "sed 's/^:// ; s~jar:~jar ~g; s~/\*:~/\* ~g; s~:~/\* ~g'",
                                                   shell=True)
    spark_def_path_line2 = subprocess.check_output("cat " + spark_def_path +
                                                   " | grep spark.driver.extraLibraryPath | awk '{print $2}' | "
                                                   "sed 's/^:// ; s~jar:~jar ~g; s~/\*:~/\* ~g; s~:\|$~/\* ~g'",
                                                   shell=True)
    spark_def_path_line1 = spark_def_path_line1.strip('\n')
    spark_def_path_line2 = spark_def_path_line2.strip('\n')

    os.system('touch /tmp/python_version')
    python_ver = subprocess.check_output("python3.5 -V 2>/dev/null | awk '{print $2}'", shell=True)
    if python_ver != '':
        with open('/tmp/python_version', 'w') as outfile:
            outfile.write(python_ver)
    else:
        python_ver = subprocess.check_output("python3.4 -V 2>/dev/null | awk '{print $2}'", shell=True)
        with open('/tmp/python_version', 'w') as outfile:
            outfile.write(python_ver)
    os.system('/bin/tar -zhcvf /tmp/jars.tar.gz --no-recursion --absolute-names --ignore-failed-read /usr/lib/hadoop/* {} {} /usr/lib/hadoop/client/*'.format(spark_def_path_line1, spark_def_path_line2))
    os.system('/bin/tar -zhcvf /tmp/spark.tar.gz -C /usr/lib/ spark')
    md5sum = subprocess.check_output('md5sum /tmp/jars.tar.gz', shell=True)
    with open('/tmp/jars-checksum.chk', 'w') as outfile:
        outfile.write(md5sum)
    md5sum = subprocess.check_output('md5sum /tmp/spark.tar.gz', shell=True)
    with open('/tmp/spark-checksum.chk', 'w') as outfile:
        outfile.write(md5sum)

    # TEMPORARY HARCODED VERSION OF DATAPROC IMAGE VERSION
    os.system('gsutil cp /tmp/jars.tar.gz gs://{0}/jars/{1}/'.format(args.bucket, '1.1.37'))
    os.system('gsutil cp /tmp/jars-checksum.chk gs://{0}/jars/{1}/'.format(args.bucket, '1.1.37'))
    os.system('gsutil cp {0} gs://{1}/{2}/{3}/'.format(spark_def_path, args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil cp /tmp/python_version gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil cp /tmp/spark.tar.gz gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
    os.system('gsutil cp /tmp/spark-checksum.chk gs://{0}/{1}/{2}/'.format(args.bucket, args.user_name, args.cluster_name))
