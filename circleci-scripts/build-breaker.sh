#!/bin/bash
SONAR_AUTH_TOKEN='cdf1e99b97a1ce03b090333b0193ca66173f18c7'
SONAR_CE_TASK_URL=`cat target/sonar/report-task.txt|grep -a 'ceTaskUrl'|awk -F '=' '{print $2"="$3}'`
curl -u admin:admin $SONAR_CE_TASK_URL -o ceTask.json
COMPONENT_ID=`cat ceTask.json |awk -F 'componentId' '{print $2}'|awk -F ':' '{print $2}'|awk -F '"' '{print $2}'`
curl -u admin:admin http://10.191.96.46:8080/api/qualitygates/project_status?projectId=$COMPONENT_ID -o qualityGate.json

qualitygate=`cat qualityGate.json |awk -F 'status' '{print $2}'|awk -F ':' '{print $2}'|awk -F '"' '{print $2}'`

echo Sonar Status: $qualitygate

if [ "$qualitygate" == "ERROR"  ]
then
   exit 1
fi
