FROM python:3-buster

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip -qq awscliv2.zip
RUN ./aws/install

RUN apt-get update
RUN apt-get install -y lsb-release
RUN apt-get install -y software-properties-common
RUN curl -fsSL https://apt.releases.hashicorp.com/gpg | apt-key add -
RUN apt-add-repository "deb [arch=amd64] https://apt.releases.hashicorp.com $(lsb_release -cs) main"
RUN apt-get update
RUN apt-get -y install terraform

ENV SERVICE_USER=dataworks
ENV SERVICE_USER_HOME=/home/$SERVICE_USER
ENV INSTALL_DIR=/opt/aws

RUN mkdir -p $SERVICE_USER_HOME $INSTALL_DIR
RUN useradd -d $SERVICE_USER_HOME $SERVICE_USER

WORKDIR $INSTALL_DIR
COPY run.sh ./
COPY terraform.tf ./

RUN chown -R $SERVICE_USER.$SERVICE_USER $SERVICE_USER_HOME
RUN chown -R $SERVICE_USER.$SERVICE_USER $INSTALL_DIR

USER $SERVICE_USER

ENTRYPOINT ["./run.sh"]
