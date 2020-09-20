# Postgres
kubectl apply -f ./k8/postgres-k8.yml

echo "waiting for postgres..."
kubectl wait --for=condition=Ready --timeout=24h $(kubectl get pods -o=name | grep postgres-deployment)

# Overwrite configurations on keycloak-k8.yml volumes
sed -i '' "s|path:.*|path: ${PWD}/k8/resources|g" k8/keycloak-k8.yml

# Keycloak
kubectl apply -f ./k8/keycloak-k8.yml

echo "waiting for keycloak..."
kubectl wait --for=condition=Ready --timeout=24h $(kubectl get pods -o=name | grep keycloak-deployment)

echo "Updating keycloak database automatically..."
sleep 5
kubectl exec -i -t $(kubectl get pods -o=name | grep postgres-deployment) -- /bin/bash -c "export PGPASSWORD=keycloak && psql -U keycloak -w -d keycloak -c 'ALTER TABLE role_attribute ALTER COLUMN value TYPE TEXT;'"

# Import waterdog realm, create new Administrator user miguel with password miguel
echo "Automatically configuring example keycloak..."
kubectl exec -i -t $(kubectl get pods -o=name | grep keycloak-deployment) -- /bin/bash -c "chmod +x /tmp/resources/keycloak-config.sh && ./tmp/resources/keycloak-config.sh"
echo "Configuration done!"

# Overwrite configurations on auth-service.yml volumes
sed -i '' "s|path:.*#replace-this|path: ${PWD}/keycloak-realms #replace-this|g" k8/auth-service.yml

# Install backend client on auth-service
echo "Install backend client on auth-service"
installFile=$(kubectl exec -i -t $(kubectl get pods -o=name | grep keycloak-deployment) -- /bin/bash -c "chmod +x /tmp/resources/get-backend-install-file.sh && ./tmp/resources/get-backend-install-file.sh")
echo "$installFile"
echo "$installFile" > keycloak-realms/keycloak-waterdog.json

echo "Building and running auth-service"
docker build -t auth-service:dev .

# Auth Service
kubectl apply -f ./k8/auth-service.yml

echo "Waiting for auth service. This might take a while..."
kubectl wait --for=condition=Ready --timeout=24h $(kubectl get pods -o=name | grep auth-service-deployment)

# Ambassador
kubectl apply -f https://www.getambassador.io/yaml/ambassador/ambassador-crds.yaml
kubectl apply -f ./k8/ambassador-rbac.yaml
kubectl apply -f ./k8/ambassador-service.yml

echo "Waiting for ambassador..."
kubectl wait --for=condition=Ready --timeout=24h $(kubectl get pods -o=name | grep ambassador)

echo "Ready!"
