#!/bin/bash -x

if [ "`which docker`" == "" ]; then
  echo "ERROR: docker not found. Please install docker CE for your OS."
  exit 1
fi
if [ "`which kubectl`" == "" ]; then
  if [ `awk -F= '/^NAME/{print $2}' /etc/os-release` == "Ubuntu" ]; then
    sudo snap install kubectl --classic
  else
    echo "ERROR: kubectl not found. Please install kubectl for your OS."
    exit 1
  fi
fi

wget -O ~/dind-cluster-v1.9.sh https://raw.githubusercontent.com/Mirantis/kubeadm-dind-cluster/master/fixed/dind-cluster-v1.9.sh
chmod +x ~/dind-cluster-v1.9.sh
~/dind-cluster-v1.9.sh up

