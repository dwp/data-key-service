# Data Key Service
DataWorks service to manage the generation and decryption of data keys.

# Build requirements
* Java 8 jdk
* Gradle

# Build instructions
Gradle will fetch required packages and action all of the building. You can start the process by using the gradle wrapper

```bash
./gradlew build
```

# Running locally

## Insecure mode

Gradle can run a local webserver to host the api on port 8080

```bash
SPRING_PROFILES_ACTIVE=AWS,KMS,INSECURE ./gradlew bootRun
```

You can then access it for example at http://localhost:8080/datakey


## Secure mode (with Mutual Authentication)

```bash
SPRING_CONFIG_LOCATION=resources/application.properties ./gradlew bootRun
```

And then to hit an endpoint with the provided self signed-certificates (from the
'resources' sub-directory)

```bash
curl --insecure --cert certificate.pem:changeit --key key.pem \
    https://localhost:8443/healthcheck
```

## Generating developer certificates

For local development of the ```SECURE``` mode of operation a script has been
provided to generate to generate a keystore and a truststore containing
self-signed which allow a locally running client (e.g. a local installation of
hbase-to-mongo-export) to operate over a mutually authenticated connection.

To generate the keystores (from the project root directory):

``` bash
cd resources
./generate-developer-certs.sh
```

This will create the files ```keystore.jks``` and ```truststore.jks```. These
should be made available to the data-key-service and to the client (for the
data-key-service see the file resources/application.properties).

Additionally the private key and certificate can be extracted so that the
service can be accessed from using ```curl```.

To extract these files, from the project root directory perform the following:

``` bash
cd resources
. ./environment.sh
extract_pems
```

The result of this is 2 files ```key.pem``` and ```certificate.pem``` which can
be used thus to hit the service:

``` bash
curl --insecure --cert certificate.pem:changeit --key key.pem \
    https://localhost:8443/datakey

```

## Standalone Mode
You can run DKS in a mode that does not require AWS credentials at all. This is
helpful if you essentially want a mock version of DKS that you can develop
against. Encryption and decryption will work (simple byte reversals) and there
is a hard coded encryption key id of STANDALONE.

```bash
SPRING_PROFILES_ACTIVE=STANDALONE,INSECURE ./gradlew bootRun
```

# Running non-locally

For production, or production-like deployments it is expected that you deploy
this service with certificates issued by a Certificate Authority (rather than
self-signed certificates as above), with your server's CA cert and client's CA
cert added to a Java Truststore. Your `config/application.properties` file
should then look like this:

```bash
server.http2.enabled=true
server.port=8443
server.ssl.client-auth=require
server.ssl.key-alias=some_alias
server.ssl.key-store-password=a_suitably_strong_password
server.ssl.key-store-type=JKS
server.ssl.key-store=/opt/dks/keystore.jks
server.ssl.trust-store-password=a_suitably_strong_password
server.ssl.trust-store=/opt/dks/truststore.jks
spring.profiles.active=AWS,KMS,SECURE
```

The Data Key Service can then be run using a command similar to the following:

```bash
java -Ddks.log.directory=/var/log/dks -Ddks.log.level.console=WARN \
-Ddks.log.level.file=INFO -jar /opt/dks/dks.jar`
```

# Swagger UI

The API has been documented in swagger format (OpenAPI) using code annotations
on the classes. For your convencience you can also view them by running the
service and navigating to

```
http://localhost:8080/swagger-ui.html#/data-key-controller
```

or

``` bash
https://localhost:8443/swagger-ui.html#/data-key-controller

```

# Healthcheck endpoint

GET endpoint at ‘/healthcheck’ reports the following

- all dependencies can be reached,
- the current master key id can be fetched,
- a new key can be generated,
- the new key is encrypted
- the new key can be decrypted
- the thumbprint of the certificates of all the trusted clients.

Response code is 200 if everything is OK, 500 otherwise. Example response body:

```json
{
    "encryptionService": "OK",
    "masterKey": "OK",
    "dataKeyGenerator": "OK",
    "encryption": "OK",
    "decryption": "BAD",
    "trustedCertificates": {
        "alice": "AA:11:22 ...",
        "bob": "BB:33:45 ..."
    }
}
```

# Requirements for Data Key Service

## Runtime

* AWS Access via an API Key
* Java 8 JRE
* CentOS

## Infrastructure

* KMS CMK (Master Key), (enabled)
* Parameter Store parameter
  * named ```data_key_service.currentKeyId```
  * value set to the full ARN for the KMS master key
* AWS user must have permissions for:
  * ```ssm:GetParameter``` on the Parameter store parameter
  * Create Data Key, Encrypt Data Key, Decrypt Data Key for the KMS CMK


# Using Docker

## To build an image
```
./gradlew clean assemble
docker build --tag javacentos:latest .
```

## To run a container (as standalone, insecure)
```
docker run -it -p8080:8080 --env SPRING_PROFILES_ACTIVE=STANDALONE,INSECURE javacentos:latest
```
