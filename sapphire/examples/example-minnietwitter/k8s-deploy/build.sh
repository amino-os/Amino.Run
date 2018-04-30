#!/bin/bash -xe

TOP_DIR=$(git rev-parse --show-toplevel)
DOCKER_IMG=huawei_paas/dcap-sapphire-minnietwitter:v0.1.0
DOCKER_IMG_FILE=huawei_paas_dcap_sapphire_minnietwitter.tar.gz

source $TOP_DIR/sapphire/examples/k8s-deployment/utils.inc

docker_dir=$PWD
jar_dir=$docker_dir/jars
scripts_dir=$docker_dir/scripts

minnietwitter_jars=( sapphire/sapphire-core/build/libs/sapphire-core.jar
                     sapphire/dependencies/apache.harmony/build/libs/apache.harmony.jar
                     sapphire/dependencies/java.rmi/build/libs/java.rmi.jar
                     sapphire/examples/example-minnietwitter/build/intermediates/packaged-classes/debug/classes.jar
                   )


verify_jars_present ${minnietwitter_jars[@]}
copy_jars $jar_dir ${minnietwitter_jars[@]}

mkdir -p $scripts_dir
cp $TOP_DIR/sapphire/examples/k8s-deployment/common/* $scripts_dir


# Build MinnieTwitter example docker image
docker rmi -f $DOCKER_IMG
rm -f $docker_dir/$DOCKER_IMG_FILE
docker build --force-rm -t $DOCKER_IMG .
docker save -o $docker_dir/$DOCKER_IMG_FILE $DOCKER_IMG

# Clean up tmp
rm -f $jar_dir/*
rm -f $scripts_dir/*
rmdir $jar_dir
rmdir $scripts_dir
