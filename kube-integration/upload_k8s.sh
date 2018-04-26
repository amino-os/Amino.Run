#!/bin/bash

#TODO: Setup private docker registry instead of below

source consts.inc

if [ "$1" == "-h" ] || [ "$1" == --help ] || [ $# -ne 3 ]; then
  echo "Usage: This script uploads DCAP k8s docker image, yaml to k8s cluster."
  echo "       This script requires scp, ssh, and sshpass to be installed on local system."
  echo "       This script assumes same username/password for all nodes on the k8s cluster."
  echo "       Run:  './upload_k8s.sh <k8s-master-IP> <username> <password>'"
  exit 0
fi

K8SMASTER=$1
USER=$2
PASS=$3

upload_docker_img() {
  local node=$1
  local homedir=`sshpass -p $PASS ssh -o StrictHostKeyChecking=no $USER@$node 'echo $HOME'`
  echo "Updating docker image on node $node:$homedir .."
  sshpass -p $PASS ssh $USER@$node -- rm $homedir/$DOCKER_IMG_FILE
  sshpass -p $PASS ssh $USER@$node -- docker rmi -f $DOCKER_IMG
  sshpass -p $PASS scp docker/$DOCKER_IMG_FILE $USER@$node:$homedir/
  sshpass -p $PASS ssh $USER@$node -- docker load -i $homedir/$DOCKER_IMG_FILE
}

upload_yaml() {
  local node=$1
  local yml_src=$2
  local yml_dst=$3
  local homedir=`sshpass -p $PASS ssh -o StrictHostKeyChecking=no $USER@$node 'echo $HOME'`
  echo "Copying $yml_dst to $node:$homedir .."
  sshpass -p $PASS ssh $USER@$node -- rm $homedir/$yml_dst
  sshpass -p $PASS scp $yml_src $USER@$node:$homedir/$yml_dst
}

nodes=$(sshpass -p p ssh root@$K8SMASTER -- kubectl get nodes -o jsonpath='{.items[*].status.addresses[?\(@.type==\"InternalIP\"\)].address}')

# Copy MinnieTwitter docker img
for node in $nodes; do
  upload_docker_img $node
done

# Copy MinnieTwitter deployment specs
upload_yaml $K8SMASTER "yaml/oms-minnietwitter.yaml" "oms-minnietwitter.yaml"
upload_yaml $K8SMASTER "yaml/kernelserver-minnietwitter.yaml" "kernelserver-minnietwitter.yaml"

echo " "
echo "Successfully updated MinnieTwitter docker images on k8s cluster."
echo "To deploy, on k8s master run: 'kubectl create -f oms-minnietwitter.yaml -f kernelserver-minnietwitter.yaml'"
