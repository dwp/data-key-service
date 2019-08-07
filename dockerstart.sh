#!/bin/sh
docker run -it -p8080:8080 --env SPRING_PROFILES_ACTIVE=STANDALONE,INSECURE javacentos:latest
