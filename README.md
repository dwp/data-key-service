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
./gradlew bootRun
```
You can then access it as http://localhost:8080

For it to work you will also need some dependencies fulfilled.
* AWS access
* AWS KMS Data Key Encryption Key
* ID of Data Key Encryption Key on AWS Parameter Store

# Swagger UI
The API has been documented in swagger format (OpenAPI) using code annotations on the classes. For your convencience
you can also view them by running the service

```
./gradlew bootRun
```

And then navigating to

```
http://localhost:8080/swagger-ui.html#/data-key-controller
```


Requirements [here](./docs/requirements.md)
