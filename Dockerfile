# This is an example of how to run this java app on CentOS, because we will host it as an EC2 running Centos in prod
FROM centos

EXPOSE 8080
CMD ["java", "-jar",  "/opt/data-key-service/data-key-service.jar"]
RUN mkdir /opt/data-key-service

RUN yum -y upgrade
RUN yum install -y java-1.8.0-openjdk
ENV JAVA_HOME /etc/alternatives/jre

COPY build/libs/data-key-service-*.jar /opt/data-key-service/data-key-service.jar
