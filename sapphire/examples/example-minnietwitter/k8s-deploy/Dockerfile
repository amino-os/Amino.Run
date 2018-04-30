FROM alpine:3.7

MAINTAINER Vinay Kulkarni <vinay.kulkarni@huawei.com>

RUN apk --update add openjdk8-jre bind-tools

COPY [ "jars/*", "/dcap/jars/" ]
COPY [ "scripts/client.policy", \
       "scripts/common.inc", \
       "scripts/start-oms.sh", \
       "scripts/start-kernelserver.sh", \
       "scripts/start-app.sh", \
       "/dcap/" ]

ENTRYPOINT tail -f /dev/null
