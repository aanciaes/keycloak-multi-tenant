#!/bin/bash

echo "Creating new realm and administrator user..."

read -p "New organization name: " organization_name
kubectl exec -i -t $(kubectl get pods -o=name | grep keycloak-deployment) -- /bin/bash -c "chmod +x /tmp/resources/create-new-organization.sh && export TARGET_REALM=$organization_name && ./tmp/resources/create-new-organization.sh"
[ $? -eq 0 ]  || exit 1

# Install backend client on auth-service
echo "Install backend client on auth-service"
installFile=$(kubectl exec -i -t $(kubectl get pods -o=name | grep keycloak-deployment) -- /bin/bash -c "chmod +x /tmp/resources/get-backend-install-file.sh && export TARGET_REALM=$organization_name && ./tmp/resources/get-backend-install-file.sh")
echo "$installFile"
echo "$installFile" > keycloak-realms/keycloak-"$organization_name".json
