FROM python:3.8

RUN mkdir /tests
WORKDIR /tests
COPY ./features .
COPY ./integration-tests-crt.pem .
COPY ./integration-tests-key.pem .
COPY ./dks-crt.pem .
COPY ./requirements.txt .
RUN pip install -r requirements.txt
ENTRYPOINT ["behave", "--no-capture"]
