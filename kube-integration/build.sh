#!/bin/bash -xe

source consts.inc

top_dir=$(git rev-parse --show-toplevel)
docker_dir=$top_dir/kube-integration/docker
jar_dir=$docker_dir/jars

required_jars=( sapphire/sapphire-core/build/libs/sapphire-core.jar
                sapphire/dependencies/apache.harmony/build/libs/apache.harmony.jar
                sapphire/dependencies/java.rmi/build/libs/java.rmi.jar
                sapphire/examples/example-minnietwitter/build/intermediates/packaged-classes/debug/classes.jar
              )


build_dcap_sapphire() {
  echo "Building DCAP-sapphire.."
  # TODO: Invoke grade build to generate DCAP-sapphire jars
}

verify_jars_present() {
  for jar in ${required_jars[@]}; do
    if [ ! -f $top_dir/$jar ]; then
      build_dcap_sapphire
      break
    fi
  done
}

copy_jars() {
  rm -rf $jar_dir/*
  mkdir -p $jar_dir
  for jar in ${required_jars[@]}; do
    cp -f $top_dir/$jar $jar_dir/
  done
}

verify_jars_present
copy_jars

# Build MinnieTwitter example
docker rmi -f $DOCKER_IMG
rm -f $docker_dir/$DOCKER_IMG_FILE
pushd docker
docker build --force-rm -t $DOCKER_IMG -f Dockerfile.MinnieTwitter .
docker save -o $docker_dir/$DOCKER_IMG_FILE $DOCKER_IMG
popd

