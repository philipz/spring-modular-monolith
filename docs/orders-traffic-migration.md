# Orders Service Traffic Migration Playbook

This document describes how to gradually migrate traffic from the monolith to the extracted `orders-service`. The strategy is based on the Docker Compose `webproxy` gateway and can be replicated on any ingress capable of weighted routing.

## 1. Gateway Rollout Controls

The `webproxy` container now renders its Nginx configuration from `proxy.conf.template` at startup. Use the `ORDERS_SERVICE_PERCENT` environment variable to control how many requests matching `/orders`, `/buy`, or `/cart` should be forwarded to the new service.

| Percentage | Behaviour |
| --- | --- |
| `0` (default) | All requests stay on the monolith (safe rollback). |
| `1-99` | Requests are randomly split by client IP and request time. |
| `100` | All traffic goes to `orders-service`. |

Example (Docker Compose):

```bash
ORDERS_SERVICE_PERCENT=25 docker compose up webproxy
```

> The rollout percentage is logged during container start-up and can be changed by restarting the proxy with the new value.

### Forced Routing for QA

- **HTTP header:** `X-Orders-Backend: monolith|orders`
- **Cookie:** `orders_backend=monolith|orders`

These overrides allow targeted verification regardless of the global percentage.

## 2. Monitoring and Observability

The proxy writes access logs with the backend label (`backend=monolith` or `backend=orders-service`). Example:

```bash
docker compose logs -f webproxy | grep backend=
```

The header `X-Orders-Backend` is also attached to responses so browser-based testing can confirm which backend responded. Combine this with existing Zipkin traces and service-specific metrics to compare latency and error rates.

## 3. Kubernetes Adoption

For Kubernetes, mount the same template into an ingress controller or custom Nginx deployment and expose the `ORDERS_SERVICE_PERCENT` environment variable. The header/cookie overrides and logging format work unchanged.

A minimal adaptation plan:

1. Build and push the `webproxy` image (`docker build -t <registry>/bookstore-webproxy webproxy`).
2. Deploy a `Deployment` with the image and set `ORDERS_SERVICE_PERCENT` in the pod spec.
3. Expose port 80 via `Service`/`Ingress` and direct storefront traffic through the proxy.

## 4. Rollout Checklist

1. Start at 0% and validate observability (logs, Zipkin traces, health endpoints).
2. Increase to 5-10% and monitor key metrics (HTTP 5xx, latency, RabbitMQ event throughput).
3. Hold for at least 30 minutes; if stable, continue doubling until 100%.
4. If issues arise, drop back to 0% and investigate with the forced-route header.

## 5. Next Steps

- Automate percentage changes via your CD pipeline (e.g., GitOps update or Compose variable change).
- Stream webproxy access logs into your analytics platform for long-term backend comparison.
- Enable alerting on differential error rates to detect regressions quickly.
