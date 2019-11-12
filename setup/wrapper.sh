#! /bin/bash

source ./environment.sh

WRAPPER_PROPERTIES=${1:?Usage: $0 gradle-wrapper-properties-file}
DISTRIBUTION_URL=${2:-https://nexus3.ucds.io/repository/thirdparty/org/gradle/distributions/gradle/5.5.1/gradle-5.5.1-bin.zip}

if [[ -f $WRAPPER_PROPERTIES ]]; then
    backup_file $WRAPPER_PROPERTIES
    TEMP_FILE=$(mktemp)
    (
        cat $WRAPPER_PROPERTIES | grep -v distributionUrl
        echo distributionUrl=$DISTRIBUTION_URL
    ) > $TEMP_FILE && mv -v $TEMP_FILE $WRAPPER_PROPERTIES
else
    stderr Not a file: \'$WRAPPER_PROPERTIES\'.
    exit 1
fi
