#!/bin/bash

source ./environment.sh

make_keystore keystore.jks localhost
extract_public_certificate keystore.jks localhost.crt
make_truststore truststore.jks localhost.crt
