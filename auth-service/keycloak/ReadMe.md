# Keycloak Kubernetes setup

This guide explains how to run Keycloak locally using k8s.
---

## 1. Run following command in terminal.

```bash
kubectl create namespace txnflow
kubectl create -n txnflow -f https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/refs/heads/main/kubernetes/keycloak.yaml
```

> This deploys Keycloak locally using default credentials. username: 'admin' | password: 'admin'

---

## 2. Port forward keycloak svc.