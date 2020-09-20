#!/bin/bash

keycloak_server=http://localhost:8080/auth

organization_name=""
if [ -n "$TARGET_REALM" ]; then
    organization_name=$TARGET_REALM
else
  echo "No organization name specified. Exiting..."
  exit 1
fi

# Prompt for admin username
read -p "Keycloak Master Admin Username: " admin_username

# Prompt for admin password
read -s -p "Keycloak Master Admin Password: " admin_password
echo

# Login to keycloak
./opt/jboss/keycloak/bin/kcadm.sh config credentials --server $keycloak_server --realm master --user $admin_username --password $admin_password
[ $? -eq 0 ]  || exit 1

# Create realm and import default template
./opt/jboss/keycloak/bin/kcadm.sh create realms -s realm=$organization_name -s enabled=true
./opt/jboss/keycloak/bin/kcadm.sh create partialImport -r $organization_name -s ifResourceExists=SKIP -o -f /tmp/resources/realm-default-template.json

# Regenerate client secret
backend_client_id=$(./opt/jboss/keycloak/bin/kcadm.sh get clients -r $organization_name --fields id -q clientId=backend | sed -En "s/\"id\" : \"(.*)\"/\1/p" | sed -e 's/^[ \t]*//')
./opt/jboss/keycloak/bin/kcadm.sh create clients/$backend_client_id/client-secret -r $organization_name

# Client root url, redirect and web origins
echo "Configuring Frontend Client..."
read -p "Root Url: " root_url
read -p "Valid redirect URls (separated by commas, no spaces): " redirect_uris_input
read -p "Web Origins (separated by commas, no spaces, * for all): " web_origins_input

redirect_uris=""
IFS=',' read -ra redirect_uris_array <<< "$redirect_uris_input"
for i in "${!redirect_uris_array[@]}"
do
    if [ "$i" == 0 ]; then
      redirect_uris="\"${redirect_uris_array[i]}\""
    else
      redirect_uris="$redirect_uris, \"${redirect_uris_array[i]}\""
    fi
done

web_origins=""
IFS=',' read -ra web_origins_array <<< "$web_origins_input"
for i in "${!web_origins_array[@]}"
do
    if [ "$i" == 0 ]; then
      web_origins="\"${web_origins_array[i]}\""
    else
      web_origins="$web_origins, \"${web_origins_array[i]}\""
    fi
done

# Actually updating frontend client
frontend_client_id=$(./opt/jboss/keycloak/bin/kcadm.sh get clients -r $organization_name --fields id -q clientId=frontend | sed -En "s/\"id\" : \"(.*)\"/\1/p" | sed -e 's/^[ \t]*//')
./opt/jboss/keycloak/bin/kcadm.sh update clients/$frontend_client_id -s rootUrl="$root_url" -s redirectUris=["$redirect_uris"] -s webOrigins=["$web_origins"] -r $organization_name
[ $? -eq 0 ]  || echo "Error configuring frontend client... Please configure it manually after."

echo "Creating $organization_name administrator user..."

# Prompt for admin username
read -p "Username: " new_username

# Prompt for admin password
read -s -p "Password: " new_password
echo

# Is set password temporary (user will be prompt to reset password on first login)
read -p "Temporary password (password reset on first login)(yes/no): " new_username_temporary_password

# Prompt for admin email
read -p "Email: " new_username_email

# Prompt for admin first name
read -p "First Name: " new_username_first_name

# Prompt for admin last name
read -p "Last Name: " new_username_last_name

# TODO
#read -p "Notify by email (yes/no):" new_username_notify_by_email
#
## Create Administrator User
#if [ "$new_username_notify_by_email" == "yes" ]; then
  # TODO
#else

# Actually creating the admin user
./opt/jboss/keycloak/bin/kcadm.sh create users -r $organization_name -s username=$new_username -s enabled=true -s email=$new_username_email -s firstName=$new_username_first_name -s lastName=$new_username_last_name -s emailVerified=true
#fi

# Set password to admin user
if [ "$new_username_temporary_password" == "yes" ]; then
    ./opt/jboss/keycloak/bin/kcadm.sh set-password -r $organization_name --username $new_username --new-password $new_password --temporary
else
    ./opt/jboss/keycloak/bin/kcadm.sh set-password -r $organization_name --username $new_username --new-password $new_password
fi

# Adding Administrator Role
./opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername $new_username --cclientid backend --rolename Administrator -r $organization_name
