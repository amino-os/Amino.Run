#!/bin/bash -xe

~/dind-cluster-v1.9.sh copy-image huawei_paas/dcap-sapphire:v0.1.0

kubectl create -f yaml/oms-minnietwitter.yaml
kubectl create -f yaml/kernelserver-minnietwitter.yaml
#TODO: Start app

