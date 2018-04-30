#!/bin/bash -xe

# Master k8s deployment build script

TOP_DIR=$(git rev-parse --show-toplevel)

# Build MinnieTwitter example
pushd $TOP_DIR/sapphire/examples/example-minnietwitter/k8s-deploy && ./build.sh && popd
