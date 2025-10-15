# Keycloak Kubernetes setup

This guide explains how to run Keycloak locally using Minikube, with external PostgreSQL credentials stored safely in environment variables.

---

## 1. Set up environment variables and Apply Keycloak Kubernetes resources

Apply the ConfigMap, Secret, and StatefulSet YAMLs:

```bash
cd keycloak
export $(cat .env | xargs)
envsubst < keycloak-dev-secret.yaml | kubectl apply -f -
envsubst < keycloak-dev-config.yaml | kubectl apply -f -
kubectl apply -f keycloak-deployment.yaml
```

> This deploys Keycloak locally using your environment variables.

---

## 2. Apply the Ingress for local access

Run the following command to expose Keycloak via a local URL using `nip.io`:

```bash
wget -q -O - https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/refs/heads/main/kubernetes/keycloak-ingress.yaml | \
sed "s/KEYCLOAK_HOST/keycloak.$(minikube ip).nip.io/" | \
kubectl create -f -
```

* This sets up an Ingress so you can access Keycloak at:

```
https://keycloak.<minikube-ip>.nip.io
```
