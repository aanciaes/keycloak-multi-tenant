#!/bin/bash

realm=waterdog
if [ -n "$TARGET_REALM" ]; then
    realm=$TARGET_REALM
fi

./opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user admin --password admin >/dev/null 2>&1

# Get client id
backend_client_id=$(./opt/jboss/keycloak/bin/kcadm.sh get clients -r $realm --fields id -q clientId=backend | sed -En "s/\"id\" : \"(.*)\"/\1/p" | sed -e 's/^[ \t]*//')

./opt/jboss/keycloak/bin/kcadm.sh get clients/$backend_client_id/installation/providers/keycloak-oidc-keycloak-json -r $realm
