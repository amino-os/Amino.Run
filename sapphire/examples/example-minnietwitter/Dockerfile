FROM alpine:3.7

MAINTAINER Vinay Kulkarni <vinay.kulkarni@huawei.com>

RUN apk --update add openjdk8-jre bind-tools
ENV APP_ROOT=/root/dcap/jars/
RUN mkdir -p $APP_ROOT
COPY ./jars/* /root/dcap/jars/
COPY ./scripts/client.policy /root/dcap/
COPY ./scripts/common.inc /root/dcap/
COPY ./scripts/start-oms.sh /root/dcap/
COPY ./scripts/start-kernelserver.sh /root/dcap/
COPY ./scripts/start-app.sh /root/dcap/

ENTRYPOINT tail -f /dev/null
