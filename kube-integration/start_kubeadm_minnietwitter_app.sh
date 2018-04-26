#!/bin/bash -xe

docker rmi -f huawei_paas/dcap-sapphire:v0.1.0
docker load -i docker/huawei_paas_dcap_sapphire.tar.gz
~/dind-cluster-v1.9.sh copy-image huawei_paas/dcap-sapphire:v0.1.0

kubectl create -f yaml/oms-minnietwitter.yaml
kubectl create -f yaml/kernelserver-minnietwitter.yaml
kubectl create -f yaml/minnietwitter-app.yaml
