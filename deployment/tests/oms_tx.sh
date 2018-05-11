#!/bin/bash

SODIR=$( cd $(dirname $0)/../.. && pwd )

cp_sapphire=${SODIR}/sapphire/sapphire-core/build/libs/sapphire-core.jar
cp_harmony=${SODIR}/sapphire/dependencies/apache.harmony/build/libs/apache.harmony.jar
cp_app=${SODIR}/sapphire/examples/fundmover/build/libs/fundmover.jar

app_ep=hw.demo.accountant

port_oms=55556

echo 'starting oms on port ${port_oms} ...'
java -Djava.util.logging.config.file=${conf_prop} -cp ${cp_app}:${cp_sapphire}:${cp_harmony} sapphire.oms.OMSServerImpl localhost ${port_oms} ${app_ep} 



