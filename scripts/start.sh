#!/bin/bash
echo Total Number of Argument Passed: "$#"
profile=${1?Error: Please provide profile name}
echo Active profile: $profile
pid=`ps -eaf | grep  wb-mongo-monitoring.jar | grep -v "grep" | awk '{print $2}'`
if test -z "$pid" 
then
 echo "Mongo monitoring project is not running"
else
 echo "Mongo monitoring project previous Process ID: "$pid
 kill -9 $pid  
fi
nohup java -jar -Dspring.profiles.active=$profile wb-mongo-monitoring.jar </dev/null &>/dev/null &
echo "Starting....................."
sleep 2s
pid=`ps -eaf | grep  wb-mongo-monitoring.jar | grep -v "grep" | awk '{print $2}'`
echo "Mongo monitoring project current Process ID: "$pid

