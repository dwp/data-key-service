FROM gradle:latest@sha256:1776a1744a6966af96a339c9fad19633e458d33d7b54d1b1551a625ab3276e89 as build

RUN ls /etc
RUN cat /etc/os-release
RUN apt-get -y update
RUN wget https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/Xenial/cloudhsm-client-jce_3.4.2-1_amd64.deb
RUN dpkg --force-depends --install ./cloudhsm-client-jce_3.4.2-1_amd64.deb
RUN mkdir -p /build
COPY build.gradle .
COPY settings.gradle .
COPY src/ ./src
RUN gradle build
RUN ls build/libs
RUN for nam in data-key-service*.jar; do newname=data-key-service.jar; mv $nam $newname; done
RUN cp build/libs/data-key-service.jar /build/

FROM openjdk:16-alpine

ENV USER_NAME=dks
ENV GROUP_NAME=dks
ENV WORKDIR=/dks
ENV LOGDIR=/var/log/dks
RUN addgroup $GROUP_NAME
RUN adduser --system --ingroup $GROUP_NAME $USER_NAME
RUN chown -R $USER_NAME.$GROUP_NAME /etc/ssl/
RUN chown -R $USER_NAME.$GROUP_NAME /usr/local/share/ca-certificates/

RUN mkdir $WORKDIR
RUN mkdir $LOGDIR
WORKDIR $WORKDIR
COPY --from=build /build/data-key-service.jar .
COPY ./images/dks/application.properties .
COPY ./images/dks/logback.xml .
COPY ./images/dks/keystore.jks .
COPY ./images/dks/truststore.jks .
RUN chown -R $USER_NAME.$GROUP_NAME $WORKDIR $LOGDIR
USER $USER_NAME
CMD ["java", "-Xmx2g", "-Dlogging.config=logback.xml", "-jar", "data-key-service.jar"]
