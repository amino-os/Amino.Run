
# Deploying DCAP apps on a kubernetes cluster

* To build, first build the DCAP-sapphire and apps via Android Studio, and then run ./build.sh
* To upload DCAP app docker image to k8s cluster, run: ./upload_k8s.sh <k8s-master-ip> <username> <password>
* To deploy k8s cluster, recommend using kubeadm.
