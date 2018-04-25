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

echo 'starting server on port ${port_kernel} ...'
java -Djava.util.logging.config.file=${conf_prop} -cp ${cp_app}:${cp_sapphire}:${cp_harmony} sapphire.kernel.server.KernelServerImpl  localhost ${port_kernel} localhost ${port_oms}  



