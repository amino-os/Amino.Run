#!/bin/sh -x

source /dcap/common.inc

check_java

# TODO: getopt / usage
OMS_APP_MAIN_CLASS=$1
OMS_PORT=22346
get_container_ip
OMS_IP=$container_ip

if [ -z $OMS_IP ]; then
  echo "ERROR: Failed to get container IP address"
  exit 1
fi

echo "Starting OMS, listening on $OMS_IP:$OMS_PORT .."
#java -cp "/dcap/jars/*" sapphire.oms.OMSServerImpl $OMS_IP $OMS_PORT sapphire.appexamples.minnietwitter.cloud.MinnieTwitterStart
java -cp "/dcap/jars/*" sapphire.oms.OMSServerImpl $OMS_IP $OMS_PORT $OMS_APP_MAIN_CLASS
