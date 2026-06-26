#!/bin/bash

# Set jar filename
APP_NAME=app.jar

# Usage instructions, to prompt for input parameters
usage() {
echo "Usage: sh service.sh [start|stop|restart|status]"
exit 1
}

# Check if program is running
is_exist(){
pid=`ps -ef|grep $APP_NAME|grep -v grep|awk '{print $2}' `
# If not exists return 1, if exists return 0
if [ -z "${pid}" ]; then
return 1
else
return 0
fi
}

# Start method
start(){
is_exist
if [ $? -eq "0" ]; then
echo "${APP_NAME} is already running. pid=${pid} ."
else
nohup java -jar $APP_NAME > /dev/null 2>&1 &
fi
}

# Stop method
stop(){
is_exist
if [ $? -eq "0" ]; then
kill -9 $pid
else
echo "${APP_NAME} is not running"
fi
}

# Output running status
status(){
is_exist
if [ $? -eq "0" ]; then
echo "${APP_NAME} is running. Pid is ${pid}"
else
echo "${APP_NAME} is NOT running."
fi
}

# Restart
restart(){
stop
start
}

# Select and execute corresponding method based on input parameters; execute usage instructions if empty
case "$1" in
"start")
start
;;
"stop")
stop
;;
"status")
status
;;
"restart")
restart
;;
*)
usage
;;
esac
