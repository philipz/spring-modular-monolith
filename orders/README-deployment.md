# Orders Service Deployment Guide

This document explains how to containerize and deploy the `orders-service` independently using Docker Compose or Kubernetes. The service image is produced directly by Spring Boot's buildpacks support, so no custom `Dockerfile` is required.

## 1. Build Container Image

From the repository root run:

```bash
./mvnw -pl orders spring-boot:build-image \
  -Dspring-boot.build-image.imageName=philipz/orders-service:latest
```

The command leverages Cloud Native Buildpacks and the configuration already present in `orders/pom.xml`. After the build completes, the image `philipz/orders-service:latest` is available locally (or pushed to the configured registry if you are logged in).

## 2. Run Locally with Docker Compose

The file `orders/docker-compose.yml` provisions the service together with PostgreSQL, RabbitMQ, and Zipkin:

```bash
cd orders
docker compose up
```

Key environment bindings:

- PostgreSQL `ordersdb` exposed on host port `5433`
- RabbitMQ management UI on `http://localhost:15673`
- Zipkin UI on `http://localhost:9412`
- Orders REST API on `http://localhost:8091`
- `PRODUCT_API_BASE_URL` defaults to `http://monolith:8080`; override it if your catalog service is reachable elsewhere.

Stop the stack with `docker compose down` (add `-v` to prune volumes if needed).

## 3. Deploy to Kubernetes

All manifests reside under `orders/k8s/`. Apply them in order:

```bash
kubectl apply -f orders/k8s/namespace.yaml
kubectl apply -f orders/k8s/secret.yaml
kubectl apply -f orders/k8s/configmap.yaml
kubectl apply -f orders/k8s/postgres.yaml
kubectl apply -f orders/k8s/rabbitmq.yaml
kubectl apply -f orders/k8s/zipkin.yaml
kubectl apply -f orders/k8s/orders-service.yaml
```

The deployment expects the same container image tag (`philipz/orders-service:latest`). Adjust the tag if you push to a different registry.

### Accessing the Service

- Inside the cluster: `http://orders-service.orders.svc.cluster.local:8091`
- Zipkin NodePort: `http://<node-ip>:30094`
- RabbitMQ management NodePort: `http://<node-ip>:30093`

## 4. Configuration Overview

- Database credentials and broker passwords are stored in `orders-service-secrets`
- Non-secret environment variables (JDBC URL, Rabbit host, profiling flags) are provided via `orders-service-config`
- Liquibase and Modulith event schemas are assigned (`orders`, `orders_events`)

## 5. Seed Historical Order Data

The service can import legacy orders from the monolith database when it starts.

1. Provide connection details for the source database (defaults to the service database if omitted):

   ```bash
   export ORDERS_BACKFILL_ENABLED=true
   export ORDERS_BACKFILL_LOOKBACK_DAYS=90   # Optional: limit window
   export ORDERS_BACKFILL_RECORD_LIMIT=500   # Maximum rows to migrate per run
   export ORDERS_BACKFILL_SOURCE_URL=jdbc:postgresql://monolith-db:5432/postgres
   export ORDERS_BACKFILL_SOURCE_USERNAME=postgres
   export ORDERS_BACKFILL_SOURCE_PASSWORD=postgres
   ```

2. Start the service (`docker compose up orders-service` or deploy to Kubernetes). A single backfill run executes at startup and records its result in the `orders.backfill_audit` table.
3. Reset `ORDERS_BACKFILL_ENABLED=false` after importing to avoid re-running on subsequent boots.

Audit details (start time, limit, processed count, errors) are persisted in `orders.backfill_audit` for traceability.

## 6. Cleanup

```bash
kubectl delete namespace orders
```

This removes all resources created for the service.
