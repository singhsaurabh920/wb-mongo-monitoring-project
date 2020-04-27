#!/bin/bash
echo "-----------------------------------------------------------------------------"
echo "-----------------------------------------------------------------------------"
pid=`ps -eaf | grep  wb-mongo-monitoring.jar | grep -v "grep" | awk '{print $2}'`
if test -z "$pid"
then
 echo "Mongo monitoring project is not running"
else
 echo "Mongo monitoring project previous Process ID: "$pid
 kill -9 $pid
fi
echo "Stopping....................."
sleep 2s
echo "-----------------------------------------------------------------------------"
echo "-----------------------------------------------------------------------------"



