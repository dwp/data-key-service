#!/bin/bash


make_keystore() {
    local keystore="${1:?Usage: $FUNCNAME keystore common-name}"
    local common_name="${2:?Usage: $FUNCNAME keystore common-name}"

    [[ -f "${keystore}" ]] && rm -v "${keystore}"

    keytool -v \
            -genkeypair \
            -keyalg RSA \
            -alias $(alias) \
            -keystore "${keystore}" \
            -storepass $(password) \
            -validity 365 \
            -keysize 2048 \
            -keypass $(password) \
            -dname "CN=${common_name},OU=DataWorks,O=DWP,L=Leeds,ST=West Yorkshire,C=UK"
}

extract_public_certificate() {
    local keystore="${1:?Usage: $FUNCNAME keystore certificate}"
    local certificate="${2:?Usage: $FUNCNAME keystore certificate}"

    [[ -f "${certificate}" ]] && rm -v "${certificate}"

    keytool -v \
            -exportcert \
            -keystore "${keystore}" \
            -storepass $(password) \
            -alias $(alias) \
            -file "$certificate"
}

extract_pems() {
    local keystore="${1:?Usage: $FUNCNAME keystore [output_file]}"
    local key="${2:-key.pem}"
    local certificate="${3:-certificate.pem}"

    local intermediate_store="${keystore/jks/p12}"

    [[ -f "${intermediate_store}" ]] && rm -v "${intermediate_store}"
    [[ -f "${key}" ]] && rm -v "${key}"

    keytool -importkeystore \
            -srckeystore "${keystore}" \
            -srcstorepass "$(password)" \
            -srckeypass "$(password)" \
            -srcalias "$(alias)" \
            -destalias "$(alias)" \
            -destkeystore "${intermediate_store}" \
            -deststoretype PKCS12 \
            -deststorepass "$(password)" \
            -destkeypass "$(password)"

    local pwd=$(password)
    export pwd

    openssl pkcs12 \
            -in "${intermediate_store}" \
            -nodes \
            -nocerts \
            -password env:pwd \
            -out "${key}"

    openssl pkcs12 \
            -in "${intermediate_store}" \
            -nokeys \
            -out "${certificate}" \
            -password env:pwd

    unset pwd
}


make_truststore() {
    local truststore="${1:?Usage: $FUNCNAME truststore certificate}"
    local certificate="${2:?Usage: $FUNCNAME truststore certificate}"
    [[ -f ${truststore} ]] && rm -v "${truststore}"
    import_into_truststore ${truststore} ${certificate} self
}

import_into_truststore() {
    local truststore="${1:?Usage: $FUNCNAME truststore certificate}"
    local certificate="${2:?Usage: $FUNCNAME truststore certificate}"
    local alias="${3:-$(alias)}"

    keytool -importcert \
            -noprompt \
            -v \
            -trustcacerts \
            -alias "${alias}" \
            -file "${certificate}" \
            -keystore "${truststore}" \
            -storepass $(password)
}

password() {
    echo changeit
}

alias() {
    echo cid
}
