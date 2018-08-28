#!/bin/sh -x

# TODO: getopt
# $1 = app-entry-point
# $2 = oms-service-name
# $3 = oms-service-port

source /root/dcap/common.inc


APP_ENTRY=$1

get_container_ip
APP_PORT=22345
APP_IP=$container_ip

OMS_SERVICE=$2
get_service_ip $OMS_SERVICE
OMS_IP=$service_ip
OMS_PORT=$3

if [ -z $APP_IP ]; then
  echo "ERROR: Failed to get container IP address"
  exit 1
fi
if [ -z $OMS_IP ]; then
  echo "ERROR: Failed to get OMS service IP address"
  exit 1
fi

echo "Starting app $APP_ENTRY on $APP_IP:$APP_PORT, connecting to $OMS_SERVICE at $OMS_IP:$OMS_PORT .."
java -cp "/root/dcap/jars/*" -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=/dcap/client.policy $APP_ENTRY $OMS_IP $OMS_PORT $APP_IP $APP_PORT

