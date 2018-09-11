#!/bin/sh -x

# TODO: getopt
# $1 = oms-service-name
# $2 = oms-port

source /root/dcap/common.inc


get_container_ip
KERNELSERVER_PORT=22345
KERNELSERVER_IP=$container_ip

OMS_PORT=$2
OMS_SERVICE=$1
get_service_ip $OMS_SERVICE
OMS_IP=$service_ip

if [ -z $KERNELSERVER_IP ]; then
  echo "ERROR: Failed to get container IP address"
  exit 1
fi
if [ -z $OMS_IP ]; then
  echo "ERROR: Failed to get OMS service IP address"
  exit 1
fi

echo "Starting KernelServer on $KERNELSERVER_IP:$KERNELSERVER_PORT, connecting to $OMS_SERVICE at $OMS_IP:$OMS_PORT .."
java -cp "/root/dcap/jars/*" -Djava.rmi.server.useCodebaseOnly=false -Djava.security.policy=/dcap/client.policy sapphire.kernel.server.KernelServerImpl $KERNELSERVER_IP $KERNELSERVER_PORT $OMS_IP $OMS_PORT
