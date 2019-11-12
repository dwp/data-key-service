#!/bin/bash

stderr() {
    echo $@ >&2
}

undertake() {
    local exe=$1
    stderr Executing \'$exe\'.
    if $@; then
        stderr Succesfully executed \'$exe\'.
    else
        stderr Failed to execute \'$exe\', exiting.
        exit 1
    fi
}

init() {
    if ls ./certificate-*.crt >/dev/null; then
        rm -v ./certificate-*.crt
    else
        return 0
    fi
}

fetch_certificates() {
    local host=${1:?Usage: $FUNCNAME host}
    local port=${2:-443}

    stderr Fetching certificates from host: \'$host\', port: \`$port\`.

    openssl s_client \
            -host $host \
            -port $port \
            -prexit \
            -showcerts 2>/dev/null | \
        ./extract-certificates.pl
}

import_certificates() {

    local options=":a:w:"

    while getopts $options opt; do
        case $opt in
            a)
                local common_alias=$OPTARG
                ;;
            w)
                local keystore_password=$OPTARG
                ;;
            \?)
                stderr Usage: $0 [ $options ] keystore
                ;;
        esac
    done

    shift $((OPTIND - 1))

    local keystore=${1:?Usage $FUNCNAME keystore}
    shift
    local common_alias=${common_alias:-gradle}
    local keystore_password=${keystore_password:-changeit}

    stderr keystore: \'$keystore\'.
    stderr common_alias: \'$common_alias\'.
    stderr keystore_password: \'$keystore_password\'.

    backup_file $keystore

    for certificate in $@; do
        stderr Importing \'$certificate\` into \'$keystore\'.
        local specific_alias=${certificate#*-}
        specific_alias=${common_alias}-${specific_alias%.crt}
        stderr specific_alias: \'$specific_alias\'.
        undertake import_certificate \
                  $keystore \
                  $certificate \
                  $specific_alias \
                  $keystore_password
    done
}

backup_file() {
    local file=${1:?Usage $FUNCNAME file}
    declare -i backup_attempt=1
    local backup=${file}.backup.${backup_attempt}
    while [[ -f $backup ]]; do
        ((backup_attempt++))
        backup=${file}.backup.${backup_attempt}
    done
    stderr Backing up \'$file\' to \'$backup\'.
    cp -v $file $backup
}

import_certificate() {
    local keystore=${1:?Usage $FUNCNAME keystore certificate alias password}
    local certificate=${2:?Usage $FUNCNAME keystore certificate alias password}
    local alias=${3:?Usage $FUNCNAME keystore certificate alias password}
    local password=${4:?Usage $FUNCNAME keystore certificate alias password}

    stderr keystore: \'$keystore\'.
    stderr certificate: \'$certificate\'.
    stderr alias: \'$alias\'.
    stderr password: \'$password\'.
    keytool \
        -import \
        -noprompt \
        -alias $alias \
        -storepass $password \
        -keystore $keystore \
        -file $certificate
}
