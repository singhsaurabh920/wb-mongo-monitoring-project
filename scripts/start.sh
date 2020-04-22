#!/bin/bash
echo Total Number of Argument Passed: "$#"
profile=${1?Error: Please provide profile name}
echo Active profile: $profile
pid=`ps -eaf | grep  gps-monitoring.jar | grep -v "grep" | awk '{print $2}'`
if test -z "$pid" 
then
 echo "Gps monitoring is not running"
else
 echo "Gps monitoring previous Process ID: "$pid
 kill -9 $pid  
fi
nohup java -jar -Dspring.profiles.active=$profile gps-monitoring.jar </dev/null &>/dev/null &
echo "Starting....................."
sleep 2s
pid=`ps -eaf | grep  gps-monitoring.jar | grep -v "grep" | awk '{print $2}'`
echo "Gps monitoring current Process ID: "$pid

