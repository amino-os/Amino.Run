#!/bin/bash

SODIR=$( cd $(dirname $0)/../.. && pwd )

conf_prop="/home/ubuntu/work/DCAP-Sapphire/sapphire/logging.properties"
log_dir=/tmp/ubuntu/sapphire_code/deployment/logs
mkdir -p ${log_dir}

cp_sapphire=${SODIR}/sapphire/sapphire-core/build/libs/sapphire-core.jar
cp_harmony=${SODIR}/sapphire/dependencies/apache.harmony/build/libs/apache.harmony.jar
cp_app=${SODIR}/sapphire/examples/hanksTodo/build/libs/hanksTodo.jar

app_ep=sapphire.appexamples.hankstodo.cloud.TodoStart

port_oms=22343
port_client=22344
port_kernel=22345

echo 'starting oms on port ${port_oms} ...'
java -Djava.util.logging.config.file=${conf_prop} -cp ${cp_app}:${cp_sapphire}:${cp_harmony} sapphire.oms.OMSServerImpl localhost ${port_oms} ${app_ep} > ${log_dir}/oms-log.localhost.${port_oms} 2>&1 &
sleep 2

echo 'starting server on port ${port_kernel} ...'
java -Djava.util.logging.config.file=${conf_prop} -cp ${cp_app}:${cp_sapphire}:${cp_harmony} sapphire.kernel.server.KernelServerImpl  localhost ${port_kernel} localhost ${port_oms}  > ${log_dir}/kernel.localhost.${port_kernel} 2>&1 &
sleep 2

echo 'starting client on port ${port_client} ...'
java -Djava.util.logging.config.file=${conf_prop} -cp ${cp_app}:${cp_sapphire}:${cp_harmony} sapphire.appexamples.hankstodo.device.TodoActivity localhost ${port_oms} loaclhost ${port_client} > ${log_dir}/client.localhost.${port_client}  2>&1 &

echo "please look for log files under ${log_dir} to see the test result"


