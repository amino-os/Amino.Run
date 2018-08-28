#!/bin/sh -x

#TODO getopt
# $1 = oms-app-entry-point

source /root/dcap/common.inc


# TODO: getopt / usage
OMS_APP_MAIN_CLASS=$1
OMS_PORT=22346
get_container_ip
OMS_IP=$container_ip

if [ -z $OMS_IP ]; then
  echo "ERROR: Failed to get container IP address"
  exit 1
fi

echo "Starting OMS for app $OMS_APP_MAIN_CLASS, listening on $OMS_IP:$OMS_PORT .."
java -cp "/root/dcap/jars/*" sapphire.oms.OMSServerImpl $OMS_IP $OMS_PORT $OMS_APP_MAIN_CLASS
