#!/bin/bash
jps -lm | grep spark > /dev/null && spark_status='running' || spark_status='idle'
if [ ${spark_status} = 'running' ]
then
port=4040
while [ ${port} -le 4045 ]
do
	if "nc -z IP_ADRESS ${port}"
	then
		curl http://IP_ADRESS:${port}/jobs/ 2>&1 | grep 'Active Jobs' > /dev/null && result='running' || result='idle'
		if [ ${result} = 'running' ]
		then
		date +%s > /opt/inactivity/inactivity_check
		fi
	fi
((port++))
done
fi