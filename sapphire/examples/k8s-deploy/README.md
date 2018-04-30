
# Deploying DCAP apps on an existing kubernetes cluster

* Build the DCAP-sapphire and apps via Android Studio, and then run ./build.sh to generate the docker image for DCAP app pods.
* Upload DCAP app docker image to k8s cluster, run: './upload_k8s.sh <k8s-master-ip> <username> <password>'
* This deployment approach assumes all nodes in the cluster have the same username / password


# Deploying DCAP apps on a single Linux VM with kubeadm + docker-in-docker based kubernetes cluster

* Recommend Ubuntu 16.04 with docker CE installed.
* Build DCAP-Sapphire using Android Studio, and then run ./build.sh to generate docker image.
  * _Currently, DCAP-Sapphire doesn't build applications both as an apk and as a jar. In order to generate the 'classes.jar' for an application, edit the build.gradle file for the application as follows:_
    * Modify 'apply plugin' from 'com.android.application' to 'com.android.library'
    * Remove applicationId from defaultConfig
    * Rebuild and verify that 'build/intermediates/packaged-classes/debug/classes.jar' has been generated.
* Run ./start_kubeadm_dind_k8s_cluster.sh
* Run ./start_kubeadm_minnietwitter_app.sh
* To view the pod logs: kubectl logs <pod-id>. e.g. kubectl logs oms-minnietwitter-deploy-6bb9979b54-9fvbb
