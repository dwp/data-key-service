#!/usr/bin/env bash

main() {
    make_keystore dks-keystore.jks
    extract_public_certificate dks-keystore.jks dks.crt
    make_truststore dks.crt

    make_keystore integration-tests-keystore.jks
    extract_public_certificate integration-tests-keystore.jks
    make_truststore integration-tests.crt

    import_into_truststore dks-truststore.jks integration-tests.crt
    import_into_truststore integration-tests-truststore.jks dks.crt

    extract_pems ./integration-tests-keystore.jks
    extract_pems ./dks-keystore.jks

    cp -v dks-crt.pem integration-tests-key.pem integration-tests-crt.pem \
       ./images/tests

    cp -v dks-truststore.jks images/dks/truststore.jks
    cp -v dks-keystore.jks images/dks/keystore.jks
}

make_keystore() {
    local keystore=${1:?Usage: $FUNCNAME keystore [common-name]}
    local common_name=${2:-${keystore%-keystore.jks}}

    [[ -f $keystore ]] && rm -v $keystore

    keytool -v \
            -genkeypair \
            -keyalg RSA \
            -alias cid \
            -keystore $keystore \
            -storepass $(password) \
            -validity 365 \
            -keysize 2048 \
            -keypass $(password) \
            -dname "CN=$common_name,OU=DataWorks,O=DWP,L=Leeds,ST=West Yorkshire,C=UK"
}

make_truststore() {
    local certificate=${1:?Usage: $FUNCNAME certificate [truststore]}
    local truststore=${2:-${certificate%.crt}-truststore.jks}
    [[ -f $truststore ]] && rm -v $truststore
    import_into_truststore $truststore $certificate self
}

extract_public_certificate() {
    local keystore=${1:?Usage: $FUNCNAME keystore [certificate]}
    local certificate=${2:-${keystore%-keystore.jks}.crt}

    [[ -f $certificate ]] && rm -v $certificate

    keytool -v \
            -exportcert \
            -keystore $keystore \
            -storepass $(password) \
            -alias cid \
            -file $certificate
}


import_into_truststore() {
    local truststore=${1:?Usage: $FUNCNAME truststore certificate}
    local certificate=${2:?Usage: $FUNCNAME truststore certificate}
    local alias=${3:-${certificate%.crt}}

    keytool -importcert \
            -noprompt \
            -v \
            -trustcacerts \
            -alias $alias \
            -file $certificate \
            -keystore $truststore \
            -storepass $(password)
}

extract_pems() {
    local keystore=${1:-keystore.jks}
    local key=${2:-${keystore%-keystore.jks}-key.pem}
    local certificate=${3:-${keystore%-keystore.jks}-crt.pem}

    local intermediate_store=${keystore/jks/p12}

    local filename=$(basename $keystore)
    local alias=cid

    [[ -f $intermediate_store ]] && rm -v $intermediate_store
    [[ -f $key ]] && rm -v $key

    if keytool -importkeystore \
               -srckeystore $keystore \
               -srcstorepass $(password) \
               -srckeypass $(password) \
               -srcalias $alias \
               -destalias $alias \
               -destkeystore $intermediate_store \
               -deststoretype PKCS12 \
               -deststorepass $(password) \
               -destkeypass $(password); then
        local pwd=$(password)
        export pwd

        openssl pkcs12 \
                -provider legacy \
                -nomacver \
                -in $intermediate_store \
                -nodes \
                -nocerts \
                -password env:pwd \
                -out $key

        openssl pkcs12 \
                -provider legacy \
                -nomacver \
                -in $intermediate_store \
                -nokeys \
                -out $certificate \
                -password env:pwd

        unset pwd
    else
        echo Failed to generate intermediate keystore $intermediate_store >&2
    fi
}

password() {
    echo changeit
}

hammer() {
    local host=${1:-localhost}
    while true; do
        response=$(curl -sS --insecure --cert certificate.pem:changeit --key key.pem https://$host:8443/datakey)
        ciphertext=$(jq -r .ciphertextDataKey <<< $response)
        curl -X POST -H "Content-type: application/json" -sS --insecure \
             --data "$ciphertext" \
             --cert certificate.pem:changeit --key key.pem \
             'https://'$host':8443/datakey/actions/decrypt?keyId=STANDALONE'
        echo
    done
}
