#!/bin/bash -xe

docker rmi -f huawei_paas/dcap-sapphire-minnietwitter:v0.1.0
docker load -i huawei_paas_dcap_sapphire_minnietwitter.tar.gz
~/dind-cluster-v1.9.sh copy-image huawei_paas/dcap-sapphire-minnietwitter:v0.1.0

kubectl create -f oms-minnietwitter.yaml
kubectl create -f kernelserver-minnietwitter.yaml
kubectl create -f minnietwitter-app.yaml
