#!/bin/bash

source ./environment.sh

OPTIONS=":a:h:p:w:"

while getopts $OPTIONS opt; do
    case $opt in
        a)
            CERTIFICATE_ALIAS=$OPTARG
            ;;
        h)
            REMOTE_HOST=$OPTARG
            ;;
        p)
            REMOTE_PORT=$OPTARG
            ;;
        w)
            KEYSTORE_PASSWORD=$OPTARG
            ;;
        \?)
            stderr Usage: $0 [ $options ] keystore
            ;;
    esac
done

shift $((OPTIND - 1))

KEYSTORE=${1:?Usage: $0 [$OPTIONS] keystore}
REMOTE_HOST=${REMOTE_HOST:-plugins-artifacts.gradle.org}
REMOTE_PORT=${REMOTE_PORT:-443}
KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-changeit}

undertake init
undertake fetch_certificates $REMOTE_HOST $REMOTE_PORT

if [[ -n certificate-*.crt ]]; then
    undertake import_certificates $KEYSTORE certificate-*.crt
fi
