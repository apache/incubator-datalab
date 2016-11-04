#!/usr/bin/env bash
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

BUCKET_NAME=$1
EMR_VERSION=$2
REGION=$3
SPARK_DEF_PATH="/usr/lib/spark/conf/spark-defaults.conf"
SPARK_DEF_PATH_LINE1=`cat $SPARK_DEF_PATH | grep spark.driver.extraClassPath | awk '{print $2}' | sed 's/^:// ; s~jar:~jar ~g; s~/\*:~/\* ~g; s~:~/\* ~g'`
SPARK_DEF_PATH_LINE2=`cat $SPARK_DEF_PATH | grep spark.driver.extraLibraryPath | awk '{print $2}' | sed 's/^:// ; s~jar:~jar ~g; s~/\*:~/\* ~g; s~:\|$~/\* ~g'`
touch /tmp/python_version
PYTHON_VER=`which python3.5 | sed 's/\/usr\/bin\/python//'`
if [ -n "$PYTHON_VER" ]
then
 echo $PYTHON_VER > /tmp/python_version
else
 PYTHON_VER=`which python3.4 | sed 's/\/usr\/bin\/python//'`
 echo $PYTHON_VER > /tmp/python_version
fi
/bin/tar -zhcvf /tmp/jars.tar.gz --no-recursion --absolute-names --ignore-failed-read /usr/lib/hadoop/* $SPARK_DEF_PATH_LINE1 $SPARK_DEF_PATH_LINE2 /usr/lib/hadoop/client/*
aws s3 cp /tmp/jars.tar.gz s3://$BUCKET_NAME/jars/$EMR_VERSION/ --endpoint-url https://s3-$REGION.amazonaws.com --region $REGION
aws s3 cp $SPARK_DEF_PATH s3://$BUCKET_NAME/ --endpoint-url https://s3-$REGION.amazonaws.com --region $REGION
aws s3 cp /tmp/python_version s3://$BUCKET_NAME/ --endpoint-url https://s3-$REGION.amazonaws.com --region $REGION
