
# Deploying DCAP apps on a single Linux VM with kubeadm + docker-in-docker based kubernetes cluster

* Recommend Ubuntu 16.04 with docker CE, wget, and curl installed.
* Build DCAP-Sapphire by running ```./gradlew build``` from the directory DCAP-Sapphire/sapphire
* Deploy a kubernetes docker-in-docker cluster on a single VM by running: ```./start_kubeadm_dind_k8s_cluster.sh``` which is in DCAP-sapphire/sapphire/examples/k8s-deployment/
* Change the kubernetes environment url as per your environment and also change the dockerhub username and password in gradle.properties.
* Change the image name in yml files of oms, kernelserver and app and also in build.gradle while building, tagging and pushing docker image.
* Run the gradle task "deployApp" using the command ```./gradlew deployApp```
* To view the pod logs: ```kubectl logs <pod-id>```. e.g. kubectl logs oms-minnietwitter-deploy-6bb9979b54-9fvbb
