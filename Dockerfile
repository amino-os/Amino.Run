FROM ubuntu:16.04
# Graal vm verison and url taken as build argument
ARG GRAAL_VERSION 
ARG GRAAL_URL 
# Installing the java to support the download of project dependencies
RUN apt-get update &&\
    apt-get install -y wget software-properties-common  && \ 
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \ 
    apt-get install -y oracle-java8-installer && \
    apt-get clean

# Downloading the graalvm 
RUN  wget $GRAAL_URL && \
     tar -xzf $GRAAL_VERSION-linux-amd64.tar.gz && \
     rm $GRAAL_VERSION-linux-amd64.tar.gz

COPY  ./ /AminoRun/run

WORKDIR /AminoRun/run

# Setting Graalvm home 

ENV GRAALVM_HOME=/$GRAAL_VERSION
ENV JAVA_HOME=$GRAALVM_HOME
ENV PATH=$GRAALVM_HOME/bin:$PATH

# Install GraalVM Ruby component package
RUN gu install ruby

# Downloading project related dependencies 
RUN bash gradlew build 

# Removing the Amino code  and java
RUN rm -r  /AminoRun  && \
    apt-get --yes remove oracle-java8-installer && \
    apt-get --yes autoremove 



