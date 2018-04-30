#!/bin/bash -xe

# Master k8s deployment artifacts build script

TOP_DIR=$(git rev-parse --show-toplevel)

# Build MinnieTwitter example docker img for k8s pods
pushd $TOP_DIR/sapphire/examples/example-minnietwitter/k8s-deploy && ./build.sh && popd

# TODO: Add deployment for other apps
# TODO: Create common base container (OMS, Kernelserver) and build app specific on top of that
