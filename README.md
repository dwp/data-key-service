# Data Key Service

DataWorks service to manage the generation and decryption of data keys.

# Build requirements

* Java 8 jdk
* Gradle
* CloudHSM sdk (force install ignoring missing dependencies) available from
  https://docs.aws.amazon.com/cloudhsm/latest/userguide/java-library-install.html
  * See versions in See https://docs.aws.amazon.com/cloudhsm/latest/userguide/client-history.html
  * Must match those used in aws infrastructure

# Build instructions
Gradle will fetch required packages and action all of the building. You can start the process by using the gradle wrapper

```bash
./gradlew build
```

# A word about CloudHSM

## Running with CloudHSM enabled

Note that the application can not be run locally if CloudHSM is selected as the
backend. This is because an application can only interact with CloudHSM if it is
running on a EC2 with the CloudHSM client daemon running on it in the same VPC
as an actual initialised CloudHSM cluster.

The best that can be done is to have the client libraries installed locally,
with this in place development can be performed in an IDE with all the benefits
of code completion etc. However to run the code for real a jar must be built,
placed on an EC2 instance that can interact with CloudHSM and then fired up
manually.

## CloudHSM api

The application performs 2 actions - it can respond to request for a new datakey
and it can decrypt the ciphertext of an encrypted datakey. The CloudHSM SDK
comes with the `Cavium` crypto provider libraries and a Util class for CloudHSM
specific operations.

### New datakey

This is a three step process -

* Generate a symmetric data key
* Fetch the public half of the master key pair
* 'Wrap' the datakey, with the public master key playing the role of the
  'wrapping' key

In more detail, first a new ephemeral (i.e. non-persistent), 'extractable'
symmetric key is generated using standard javax.crypto apis (with Cavium as the
provider). This gives the plaintext version of the key. This plaintext version
is then encrypted - which in the CloudHSM argot is termed 'wrapping'. To 'wrap'
the datakey a 'wrapping key' is required - and this is the role played by the
public half of the master key pair. So the public half of the master key pair
must be fetched from the HSM and then the datakey is 'wrapped' with it - this
gives the encrypted version.

The plaintext key, the encrypted ciphertext version of it and the handles (the
CloudHSM ids) of the private and public master key pair are then returned to the
client.

Wrapping is done through the CloudHSM `Util` class which provides the method
`Util.rsaWrapKey` to which you supply the public wrapping key, and the key to
be wrapped.

### Decrypt datakey

When the time comes to decrypt the key the ciphertext and the handles of the
public and private key (in a single compound string)) are sent back to the
application by the client along with the ciphertext. To decrypt, the private key
half of the master key pair must be fetched and then this and the supplied
ciphertext must passed to the `Util.rsaUnwrapKey` method.

# Running locally (non CloudHSM only)


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

* KMS CMK (Master Key), (enabled) for KMS or
* CloudHSM client and sdk available from
  https://docs.aws.amazon.com/cloudhsm/latest/userguide/java-library-install.html
* AWS user must have permissions for:
  * ```ssm:GetParameter``` on the Parameter store parameter
   * Create Data Key, Encrypt Data Key, Decrypt Data Key for the KMS CMK (if in KMS mode)


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
