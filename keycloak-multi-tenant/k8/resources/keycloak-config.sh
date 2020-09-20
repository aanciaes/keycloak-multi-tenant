#!/bin/bash

# import realm
./opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user admin --password admin
./opt/jboss/keycloak/bin/kcadm.sh create realms -s realm=waterdog -s enabled=true
./opt/jboss/keycloak/bin/kcadm.sh create partialImport -r waterdog -s ifResourceExists=OVERWRITE -o -f /tmp/resources/realm-waterdog.json

# Regenerate client secret
backend_client_id=$(./opt/jboss/keycloak/bin/kcadm.sh get clients -r waterdog --fields id -q clientId=backend | sed -En "s/\"id\" : \"(.*)\"/\1/p" | sed -e 's/^[ \t]*//')
./opt/jboss/keycloak/bin/kcadm.sh create clients/$backend_client_id/client-secret -r waterdog

# Create Administrator User miguel:miguel
./opt/jboss/keycloak/bin/kcadm.sh create users -r waterdog -s username=miguel -s enabled=true -s email=miguel.anciaes@waterdog.mobi -s firstName=Miguel -s lastName=Anciaes -s emailVerified=true
./opt/jboss/keycloak/bin/kcadm.sh set-password -r waterdog --username miguel --new-password miguel
./opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername miguel --cclientid backend --rolename Administrator -r waterdog

# Create BasicUser User bruno:bruno
./opt/jboss/keycloak/bin/kcadm.sh create users -r waterdog -s username=bruno -s enabled=true -s email=bruno.felix@waterdog.mobi -s firstName=Bruno -s lastName=Felix -s emailVerified=true
./opt/jboss/keycloak/bin/kcadm.sh set-password -r waterdog --username bruno --new-password bruno
./opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername bruno --cclientid backend --rolename BasicUser -r waterdog
