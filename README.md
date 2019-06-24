# Data Key Service
DataWorks service to manage the generation and decryption of data keys.

# Build requirements
* Java 8 jdk
* Gradle

# Build instructions
Gradle will fetch required packages and action all of the building. You can start the process by using the gradle wrapper
```
./gradlew build
```

# Running locally
Gradle can run a local webserver to host the api on port 8080
```
SPRING_PROFILES_ACTIVE=AWS,KMS ./gradlew bootRun
```
You can then access it as http://localhost:8080

# Mutual authorisation

Start with the following parameters:

```
server.port
server.ssl.keystore
server.ssl.key-store-password
server.ssl.key-store-type
server.ssl.key-alias
server.http2.enabled
server.ssl.trust-store
server.ssl.trust-store-password
server.ssl.client-auth
```

```
curl --insecure --cert certificate.pem:changeit --key key.pem https://localhost:8443/healthcheck
```
For it to work you will also need some dependencies fulfilled.
* AWS access
* AWS KMS Data Key Encryption Key
* ID of Data Key Encryption Key on AWS Parameter Store

# Swagger UI
The API has been documented in swagger format (OpenAPI) using code annotations on the classes. For your convencience
you can also view them by running the service

```
SPRING_PROFILES_ACTIVE=AWS,KMS ./gradlew bootRun
```

And then navigating to

```
http://localhost:8080/swagger-ui.html#/data-key-controller
```

# Healthcheck endpoint

New GET endpoint on ‘/healthcheck’ reports the following

- all dependencies can be reached,
- the current master key id can be fetched,
- a new key can be generated,
- the new key is encrypted
- the new key can be decrypted

Response code is 200 if everything is OK, 500 otherwise. Example response body:

```
{
"encryptionService": "OK",
"masterKey": "OK",
"dataKeyGenerator": "OK",
"encryption": "OK",
"decryption": “BAD”
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
  * named ```data-key-service.currentKeyId```
  * value set to the full ARN for the KMS master key
* AWS user must have permissions for:
  * ```ssm:GetParameter``` on the Parameter store parameter
  * Create Data Key, Encrypt Data Key, Decrypt Data Key for the KMS CMK
  

# Using Docker

## To build an image
```
docker build --tag javacentos:latest .
```

## To run a container
```
docker run -it -p8080:8080 javacentos:latest
```
