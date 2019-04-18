# java
#
# VERSION       Java 8
FROM centos

# Upgrading system
RUN yum -y upgrade

RUN yum install -y \
       java-1.8.0-openjdk

ENV JAVA_HOME /etc/alternatives/jre

RUN mkdir /opt/data-key-service
ADD build/libs/data-key-service-*.jar /opt/data-key-service

EXPOSE 8080
CMD ["java", "-jar",  "/opt/data-key-service/data-key-service-0.0.1-SNAPSHOT.jar"]
