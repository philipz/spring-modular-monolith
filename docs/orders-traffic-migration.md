# Orders Service Traffic Migration Playbook (Updated for `/api/**`)

This playbook describes how to gradually shift order-related HTTP traffic from the monolith to the extracted `orders-service` while the Next.js storefront continues to call `/api/cart/**` and `/api/orders/**`. The current `webproxy/nginx.conf` always routes to the monolith; adapt the template below before running in production.

## 1. Gateway Controls

When using a templated nginx config, introduce an `ORDERS_SERVICE_PERCENT` environment variable to control the split for **API** routes:

```
/api/cart/**      # cart REST endpoints
/api/orders/**    # order REST endpoints
```

| Percentage | Behaviour |
| --- | --- |
| `0` (default) | All traffic stays on the monolith (`bookstore` service). |
| `1-99` | Requests are distributed between monolith and orders-service based on hash of client IP + path. |
| `100` | All traffic goes to `orders-service`. |

Example (Docker Compose):

```bash
ORDERS_SERVICE_PERCENT=25 docker compose up webproxy
```

**QA overrides**

- Header: `X-Orders-Backend: monolith|orders`
- Cookie: `orders_backend=monolith|orders`

These force the proxy decision regardless of the global percentage and are useful for smoke testing.

## 2. Observability

nginx access logs are annotated with `backend=monolith` or `backend=orders-service`. Tail them via:

```bash
docker compose logs -f webproxy | grep backend=
```

The proxy also returns the header `X-Orders-Backend` for quick verification in browser devtools. Combine nginx logs with:

- HyperDX traces (compare latencies between backends).
- RabbitMQ metrics (ensure order events are still published).
- k6 load tests (`k6 run k6.js`) to stress both paths.

## 3. Kubernetes Adaptation

1. Build and push `webproxy` image with the weighted routing template.
2. Deploy an nginx Deployment/Ingress that exposes the same env vars (`ORDERS_SERVICE_PERCENT`, optional overrides).
3. Expose port 80 via Service/Ingress and direct storefront traffic through it.

Forced-route header and cookie logic transfers unchanged.

## 4. Rollout Steps

1. Start at 0% and validate telemetry (logs, HyperDX traces, actuator health).
2. Increase to 5–10% and monitor for HTTP 5xx, gRPC failures, cart/session anomalies.
3. If stable after an agreed soak period, gradually raise the percentage (e.g. 25 → 50 → 75 → 100).
4. If issues arise, revert to 0% (monolith) and inspect using forced-route overrides.

## 5. Requirements & Notes

- The monolith REST controllers already delegate to `OrdersGrpcClient`; verify `BOOKSTORE_SESSION` affinity across both backends.
- Ensure the extracted `orders-service` has access to the same Hazelcast cluster or consistently handles sessions (currently the monolith retains ownership—full extraction would require redistributing session state).
- Keep OpenAPI schemas in sync if the external service evolves.

Following this playbook keeps `/api/cart/**` and `/api/orders/**` stable for the Next.js frontend while you gradually shift execution to the independent orders-service.
